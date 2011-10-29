package net.wohlfart.framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * copy pasted code from Seams ExcelExporter, which is in the excel.jar not used
 * in this application see: https://jira.jboss.org/jira/browse/RF-6279
 * 
 * @author Michael Wohlfart
 * 
 */
@Name(value = "tableExporter")
@Scope(ScopeType.EVENT)
public class TableExporter {

    private final static Logger LOGGER = LoggerFactory.getLogger(TableExporter.class);

    @SuppressWarnings("rawtypes")
    private AbstractTableQuery  abstractTableQuery;

    @SuppressWarnings("rawtypes")
    public void findResource(final String tableName, final String type) {
        LOGGER.info("tableName is: {} of type: {}", tableName, tableName.getClass());
        LOGGER.info("type is: {} of type: {}", type, type.getClass());

        final Object object = Contexts.getConversationContext().get(tableName);
        LOGGER.info("object is: {} of type: {}", object, object.getClass());
        if (object instanceof AbstractTableQuery) {
            LOGGER.info("setting abstractTableQuery in {}", hashCode());
            abstractTableQuery = (AbstractTableQuery) object;
        } else {
            LOGGER.info("setting abstractTableQuery anyways", hashCode());
            abstractTableQuery = (AbstractTableQuery) object;
        }
    }

    @Transactional
    public byte[] getData() {
        LOGGER.info("getData called for {}", hashCode());
        return createSpreadsheetDocument();
    }

    @BypassInterceptors
    public String getContentType() {
        return "application/excel";
    }

    @BypassInterceptors
    public String getFileName() {
        return "test.xls";
    }

    @SuppressWarnings("rawtypes")
    private byte[] createSpreadsheetDocument() {
        // create a new workbook object; note that the workbook
        // and the file are two separate things until the very
        // end, when the workbook is written to the file.
        final HSSFWorkbook wb = new HSSFWorkbook();

        // create a new worksheet
        final HSSFSheet ws = wb.createSheet();

        // create a row object reference for later use
        HSSFRow r = null;

        // create a cell object reference
        HSSFCell c = null;

        // create two cell styles - formats
        // need to be defined before they are used

        final HSSFCellStyle cs1 = wb.createCellStyle();
        final HSSFCellStyle cs2 = wb.createCellStyle();
        final HSSFDataFormat df = wb.createDataFormat();

        // create two font objects for formatting
        final HSSFFont f1 = wb.createFont();
        final HSSFFont f2 = wb.createFont();

        // set font 1 to 10 point bold type
        f1.setFontHeightInPoints((short) 10);
        f1.setBoldweight(Font.BOLDWEIGHT_BOLD);

        // set font 2 to 10 point red type
        f2.setFontHeightInPoints((short) 10);
        f2.setColor(Font.COLOR_RED);

        // for cell style 1, use font 1 and set data format
        cs1.setFont(f1);
        cs1.setDataFormat(df.getFormat("#,##0.0"));

        // for cell style 2, use font 2, set a thin border, text format
        cs2.setBorderBottom(CellStyle.BORDER_THIN);
        cs2.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
        cs2.setFont(f2);

        // set the sheet name
        wb.setSheetName(0, "Test sheet");

        // remember old values
        final Integer first = abstractTableQuery.getFirstResult();
        final Integer max = abstractTableQuery.getMaxResults();

        // we want all
        abstractTableQuery.setFirstResult(0);
        abstractTableQuery.setMaxResults(null);
        final List list = abstractTableQuery.getResultList();

        // reset to old values
        abstractTableQuery.setFirstResult(first);
        abstractTableQuery.setMaxResults(max);

        short rownum = (short) 0;
        for (final Object object : list) {
            // create a row
            r = ws.createRow(rownum);
            c = r.createCell(0);
            c.setCellValue(rownum);
            c = r.createCell(1);
            c.setCellValue(object.toString());
            rownum++;
        }

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            wb.write(buffer);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        return buffer.toByteArray();
    }

    // public void export(Object data, String type) {
    // LOGGER.warn("data is: {} of type: {}", data, data.getClass());
    // if (data instanceof AbstractTableQuery) {
    // AbstractTableQuery abstractTableQuery = (AbstractTableQuery) data;
    // List list = abstractTableQuery.list();
    // LOGGER.warn("list is: {} of type: {} enumerating result list", list,
    // list.getClass());
    // for (Object object:list) {
    // LOGGER.warn("  element is: {} of type: {}", object, object.getClass());
    // }
    // }
    //
    // }

    // public void export(String dataTableId, String type) {
    // // Gets the datatable
    // UIData dataTable = (UIData)
    // FacesContext.getCurrentInstance().getViewRoot().findComponent(dataTableId);
    // if (dataTable == null) {
    // throw new IllegalArgumentException("Could not find dataTableId with id: "
    // + dataTableId);
    // }
    //
    // if (dataTable instanceof HtmlDataTable) {
    // HtmlDataTable htmlDataTable = (HtmlDataTable)dataTable;
    // Object value = htmlDataTable.getValue();
    // htmlDataTable.
    // }
    // String dataTableVar = dataTable.getVar();
    //
    // LOGGER.warn("dataTableVar is: {}", dataTableVar);
    //
    // }

}
