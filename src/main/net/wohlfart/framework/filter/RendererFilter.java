package net.wohlfart.framework.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.wohlfart.framework.RuntimeEnvDataProvider;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.web.AbstractFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.resource.FSEntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.lowagie.text.DocumentException;

/**
 * This filter adds the flying sourcer html to pdf converter
 * 
 * 
 * see:
 * http://today.java.net/pub/a/today/2006/10/31/combine-facelets-and-flying-
 * saucer-renderer.html http://seamframework.org/Community/HowCanIPrintMyPage
 * 
 * disabled because this filter messes up the utf-8 encodeing!
 */
// @Startup
// @Scope(ScopeType.APPLICATION)
// @Name("rendererFilter")
// @BypassInterceptors
// @Filter(around ="org.jboss.seam.web.ajax4jsfFilter")
public class RendererFilter extends AbstractFilter {

    private final static Logger LOGGER      = LoggerFactory.getLogger(RendererFilter.class);

    @SuppressWarnings("unused")
    private FilterConfig        config;
    private DocumentBuilder     documentBuilder;
    private URL                 basePath    = null;

    private static final String APPLICATION = "application/pdf";
    private static final String RENDER_TYPE = "RenderOutputType";
    private static final String MIME_TYPE   = "pdf";

    @Override
    public void init(final FilterConfig config) throws ServletException {
        this.config = config;
        System.setProperty("xr.util-logging.loggingEnabled", "false");

        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            documentBuilder = factory.newDocumentBuilder();
            // use local DTDs instead of fetching them from the net
            documentBuilder.setEntityResolver(FSEntityResolver.instance());
        } catch (final ParserConfigurationException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException,
            ServletException {

        // only filter http requests
        if (servletRequest instanceof HttpServletRequest) {

            final HttpServletRequest request = (HttpServletRequest) servletRequest;
            final HttpServletResponse response = (HttpServletResponse) servletResponse;

            if (basePath == null) {
                final ServletContext servletContext = RuntimeEnvDataProvider.getServletContext();
                final String contextPath = servletContext.getContextPath();
                final String baseUrl = servletContext.getInitParameter("charms.baseUrl");
                basePath = new URL(baseUrl + request.getContextPath() + "/");
            }

            // Check to see if this filter should apply.
            // just attach &RenderOutputType=pdf to the url
            // ?RenderOutputType=pdf
            final String renderType = request.getParameter(RENDER_TYPE);
            if (StringUtils.isEmpty(renderType)) {
                // Normal processing
                filterChain.doFilter(servletRequest, servletResponse);

            } else if (renderType.equals(MIME_TYPE)) {
                // Capture the content for this request
                final ContentCaptureServletResponse capContent = new ContentCaptureServletResponse(response);
                filterChain.doFilter(request, capContent);

                // Transform the XHTML content to a document
                // readable by the renderer.
                // Parse the XHTML content to a document that is readable by the
                // XHTML renderer.
                StringReader contentReader = null;
                try {

                    contentReader = new StringReader(capContent.getContent());
                    final InputSource source = new InputSource(contentReader);

                    final Document xhtmlContent = documentBuilder.parse(source);

                    final ITextRenderer renderer = new ITextRenderer();
                    // basePath for css
                    renderer.setDocument(xhtmlContent, basePath.toString());
                    renderer.layout();

                    response.setContentType(APPLICATION);
                    final OutputStream browserStream = response.getOutputStream();

                    renderer.createPDF(browserStream);

                    return;

                } catch (final SAXException e) {
                    e.printStackTrace();
                } catch (final DocumentException e) {
                    e.printStackTrace();
                } catch (final TransformerException e) {
                    e.printStackTrace();
                }

            } else {
                LOGGER.warn("unknown render type {}", renderType);
            }

        } else {
            LOGGER.warn("unknown request type");
        }

    }
}
