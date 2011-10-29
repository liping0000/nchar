package net.wohlfart.framework.filter;

// import org.w3c.tidy.Tidy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ContentCaptureServletResponse extends HttpServletResponseWrapper {

    private ByteArrayOutputStream contentBuffer;
    private PrintWriter           writer;

    public ContentCaptureServletResponse(final HttpServletResponse resp) {
        super(resp);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            contentBuffer = new ByteArrayOutputStream();
            writer = new PrintWriter(contentBuffer);
        }
        return writer;
    }

    /*
     * public String getContent(){ // writer might be null here for redirect on
     * not logged in check writer.flush(); String xhtmlContent = new
     * String(contentBuffer.toByteArray()); // xhtmlContent =
     * xhtmlContent.replaceAll("<thead>|</thead>|"+ // "<tbody>|</tbody>","");
     * return xhtmlContent; }
     */

    /*
     * -------------------------------------------------- private
     * ByteArrayOutputStream contentBuffer; private PrintWriter writer;
     * 
     * public ContentCaptureServletResponse(HttpServletResponse
     * originalResponse) { super(originalResponse); }
     * 
     * @Override public PrintWriter getWriter() throws IOException { if (writer
     * == null) { contentBuffer = new ByteArrayOutputStream(); writer = new
     * PrintWriter(contentBuffer); } return writer; }
     */
    public String getContent() throws IOException, SAXException, TransformerException {
        getWriter();
        writer.flush();
        String xhtmlContent = new String(contentBuffer.toByteArray());
        xhtmlContent = xhtmlContent.replaceAll("<thead>|</thead>|" + "<tbody>|</tbody>", "");

        final DOMParser parser = new DOMParser();

        parser.setFeature("http://cyberneko.org/html/features/balance-tags", true);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        parser.setFeature("http://cyberneko.org/html/features/override-namespaces", true);
        parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
        parser.setProperty("http://cyberneko.org/html/properties/namespaces-uri", "http://www.w3.org/1999/xhtml");

        parser.parse(new InputSource(new StringReader(xhtmlContent)));

        final Document node = parser.getDocument();

        final StringWriter sw = new StringWriter();
        final Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));
        xhtmlContent = sw.toString();
        return xhtmlContent;
    }

}
