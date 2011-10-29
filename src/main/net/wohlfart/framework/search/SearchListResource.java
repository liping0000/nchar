package net.wohlfart.framework.search;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;
import net.wohlfart.user.SearchActionBean;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

@Name("searchListResource")
@Scope(ScopeType.EVENT)
// seems like logger doesn't work in event context
public class SearchListResource {

    private final static Logger           LOGGER      = LoggerFactory.getLogger(SearchListResource.class);

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    @In(value = "searchActionBean")
    SearchActionBean                      searchActionBean;

    @Create
    public void createComponent() {
        LOGGER.debug("createComponent called");
    }

    @Destroy
    public void destroyComponent() {
        LOGGER.debug("destroyComponent called");
    }

    @BypassInterceptors
    public String getFileName() {
        return "list.pdf";
    }

    @BypassInterceptors
    public String getContentType() {
        return "application/pdf"; // FIXME: use a constance from a lib here
    }

    @Transactional
    public byte[] getData() {
        try {

            final Font defaultFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(10, 10, 10));
            final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, new Color(10, 10, 10));

            final Document document = new Document(PageSize.A4);
            document.setMargins(50, 50, 50, 50);
            document.addTitle("Charms - List of Workflow Instances");

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
            writer.setPageEvent(new PdfPageEventHelper() {

                protected PdfTemplate total;
                protected PdfTemplate pagetitle;

                @Override
                public void onOpenDocument(final PdfWriter writer, final Document doc) {
                    total = writer.getDirectContent().createTemplate(100, 100);
                    total.setBoundingBox(new Rectangle(-20, -20, 100, 100));

                    pagetitle = writer.getDirectContent().createTemplate(800, 100);
                    pagetitle.setBoundingBox(new Rectangle(-20, -20, 800, 100));
                }

                @Override
                public void onEndPage(final PdfWriter writer, final Document doc) {
                    final float boderAdjust = 30;
                    final String text = writer.getPageNumber() + "/";
                    final PdfContentByte cb = writer.getDirectContent();
                    cb.saveState();

                    final float textBasePageTitle = doc.top() + 2;
                    cb.addTemplate(pagetitle, doc.left(), textBasePageTitle);

                    final float textBase = doc.bottom() - defaultFont.getSize() - 5;
                    final float textSize = defaultFont.getBaseFont().getWidthPoint(text, defaultFont.getSize());
                    cb.beginText();
                    cb.setFontAndSize(defaultFont.getBaseFont(), defaultFont.getSize());
                    cb.setTextMatrix(doc.right() - textSize - boderAdjust, textBase);
                    cb.showText(text);
                    cb.endText();
                    cb.addTemplate(total, doc.right() - boderAdjust, textBase);

                    cb.setColorStroke(new GrayColor(0.2f));
                    cb.moveTo(doc.right(), doc.top());
                    cb.lineTo(doc.left(), doc.top());
                    cb.stroke();

                    cb.setColorStroke(new GrayColor(0.2f));
                    cb.moveTo(doc.right(), doc.bottom());
                    cb.lineTo(doc.left(), doc.bottom());
                    cb.stroke();

                    cb.restoreState();
                }

                @Override
                public void onCloseDocument(final PdfWriter writer, final Document document) {
                    total.beginText();
                    total.setFontAndSize(defaultFont.getBaseFont(), defaultFont.getSize());
                    total.setTextMatrix(0, 0);
                    total.showText(String.valueOf(writer.getPageNumber() - 1));
                    total.endText();

                    pagetitle.beginText();
                    pagetitle.setFontAndSize(defaultFont.getBaseFont(), defaultFont.getSize());
                    pagetitle.setTextMatrix(0, 0);
                    pagetitle.showText("Charms - Suchergebnisse - " + DATE_FORMAT.format(new Date()));
                    pagetitle.endText();
                }
            });
            document.open();

            final List<ChangeRequestData> list = searchActionBean.getChangeRequestDataList();

            // HtmlCleaner cleaner = new HtmlCleaner();
            // CleanerProperties props = cleaner.getProperties();

            for (final ChangeRequestData data : list) {
                final Paragraph paragraph = new Paragraph();
                paragraph.setKeepTogether(true);

                // / title of the request data
                final Paragraph titleParagraph = new Paragraph(data.getTitle(), headerFont);
                paragraph.add(titleParagraph);

                // Kennung:
                final String businessKey = data.getBusinessKey();
                if (!StringUtils.isEmpty(businessKey)) {
                    final Paragraph businessKeyParagraph = new Paragraph("Kennung: " + businessKey, defaultFont);
                    paragraph.add(businessKeyParagraph);
                }

                // Maschinentyp:
                final ChangeRequestProduct changeRequestProduct = data.getChangeRequestProduct();
                if (changeRequestProduct != null) {
                    final Paragraph submitUserParagraph = new Paragraph("Maschinentyp: " + changeRequestProduct.getDefaultName(), defaultFont);
                    paragraph.add(submitUserParagraph);
                }

                // Bereich:
                final ChangeRequestUnit changeRequestUnit = data.getChangeRequestUnit();
                if (changeRequestUnit != null) {
                    final Paragraph submitUserParagraph = new Paragraph("Bereich: " + changeRequestUnit.getDefaultName(), defaultFont);
                    paragraph.add(submitUserParagraph);
                }

                // Codierung:
                final ChangeRequestCode changeRequestCode = data.getChangeRequestCode();
                if (changeRequestCode != null) {
                    final Paragraph submitUserParagraph = new Paragraph("Codierung: " + changeRequestCode.getDefaultName(), defaultFont);
                    paragraph.add(submitUserParagraph);
                }

                // Eingereicht:
                final Date submitDate = data.getSubmitDate();
                if (submitDate != null) {
                    final Paragraph submitDateParagraph = new Paragraph("Eingereicht: " + DATE_FORMAT.format(submitDate), defaultFont);
                    paragraph.add(submitDateParagraph);
                }

                // Einreicher:
                final CharmsUser submitUser = data.getSubmitUser();
                if (submitUser != null) {
                    final Paragraph submitUserParagraph = new Paragraph("Einreicher: " + submitUser.getLabel(), defaultFont);
                    paragraph.add(submitUserParagraph);
                }

                // PE:
                final CharmsUser processUser = data.getProcessUser();
                if (processUser != null) {
                    final Paragraph processUserParagraph = new Paragraph("PE: " + processUser.getLabel(), defaultFont);
                    paragraph.add(processUserParagraph);
                }

                // Eingereicht:
                final Date finishDate = data.getFinishDate();
                if (finishDate != null) {
                    final Paragraph finishDateParagraph = new Paragraph("Abgeschlossen: " + DATE_FORMAT.format(finishDate), defaultFont);
                    paragraph.add(finishDateParagraph);
                }

                // / _________________________
                // /
                paragraph.add(new Chunk(new LineSeparator(0.0f, 90, Color.BLUE, Element.ALIGN_CENTER, 3.5f)));

                // String before = data.getProblemDescription();
                // System.err.println("------------------------ before parsing ------------------");
                // System.err.println(before);
                // TagNode node = cleaner.clean(new StringReader(before));
                // ByteArrayOutputStream textPart = new ByteArrayOutputStream();
                // new SimpleXmlSerializer(props).writeXmlToStream(node,
                // textPart);

                // HtmlParser.parse(document, new
                // StringReader(textPart.toString()));

                // org.w3c.dom.Document myDom = new DomSerializer(null,
                // true).createDOM(cleaner.clean(new StringReader(before)));
                // cleaner.clean(new StringReader(before)).serialize(arg0, arg1)
                // String after = cleaner.clean(new
                // StringReader(before)).getText().toString();
                // System.err.println("------------------------- after parsing ------------------");
                // System.err.println(after);

                // HtmlParser.parse(document, new StringReader(after)
                // );

                document.add(paragraph);
            }

            document.close();
            return byteArrayOutputStream.toByteArray();
        } catch (final DocumentException e) {
            e.printStackTrace();
        } // catch (IOException e) {
          // e.printStackTrace();
          // }
        return null;
    }
}
