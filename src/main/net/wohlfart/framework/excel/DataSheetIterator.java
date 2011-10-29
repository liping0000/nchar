package net.wohlfart.framework.excel;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* package private */
class DataSheetIterator implements Iterator<HashMap<String, String>> {

    private final static Logger        LOGGER        = LoggerFactory.getLogger(DataSheetIterator.class);

    // format for numeric cells, we don't need decimal fractions here
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#");

    // all the parameters should be set after the constructor finished
    // successfully

    private final HSSFSheet            hSSFSheet;
    private final int                  firstRow;
    private final int                  lastRow;

    // the labels for the column headers
    private ArrayList<String>          labelList;

    private int                        headerRow;
    private HashMap<Short, String>     propertyColumnNumbers;
    // the iterator count and next data
    private int                        rowCounter;
    private HashMap<String, String>    counterRowValues;

    public DataSheetIterator(final HSSFSheet hSSFSheet, final String[] labels) {
        this(hSSFSheet);
        initializeLabels(labels);
    }

    /**
     * the constructor to initialize with a excel sheet
     * 
     * @param hSSFSheet
     */
    protected DataSheetIterator(final HSSFSheet hSSFSheet) {
        assert (hSSFSheet != null) : "hSSFSheet can not be null in constructor";
        this.hSSFSheet = hSSFSheet;

        firstRow = hSSFSheet.getFirstRowNum();
        lastRow = hSSFSheet.getLastRowNum();
        assert (firstRow < lastRow) : "firstRow > lastRow;  firstRow is " + firstRow + " lastRow is " + lastRow;
    }

    protected void initializeLabels(final String[] labels) {
        if ((labels == null) || (labels.length == 0)) {
            LOGGER.error("need labels to look for in constructor, labels are: {}", labels);
            return;
        }
        labelList = new ArrayList<String>();
        for (final String string : labels) {
            labelList.add(string);
        }

        headerRow = findHeaderRowWithLabels(labelList);
        if (headerRow <= 0) {
            throw new InvalidParameterException("can't find header line in sheet");
        }
        rowCounter = headerRow; // start of the values is the line after the
                                // header

        propertyColumnNumbers = findPropertyColumns(headerRow);

        LOGGER.debug("initialized AbstractDataSheet firstRow is " + firstRow + " lastRow is " + lastRow + " header found in row " + headerRow);
    }

    /**
     * return the row with all the labels in the string array returns -1 if the
     * labels can't be found in one row
     * 
     * @param labels
     * @return
     */
    private int findHeaderRowWithLabels(final ArrayList<String> labels) {
        for (int currentRow = firstRow; currentRow < lastRow; currentRow++) {
            LOGGER.debug("scanning line " + currentRow);
            final HSSFRow row = hSSFSheet.getRow(currentRow);
            // try to find all the labels in this row
            boolean foundAll = true;
            for (final String string : labels) {
                foundAll = isLabelInRow(row, string) && foundAll;
            }
            if (foundAll) {
                // return the current row since it contains all the labels
                return currentRow;
            }
        } // loop for the next row

        // nothing found
        return -1;
    }

    /**
     * check if the string is in the row as label, skip all number-type cells
     * case is ignored for userfriendlyness
     * 
     * @param row
     * @param string
     * @return
     */
    private boolean isLabelInRow(final HSSFRow row, final String string) {
        if (row != null) {
            final short first = row.getFirstCellNum();
            final short last = row.getLastCellNum();
            for (short column = first; column <= last; column++) {
                final HSSFCell cell = row.getCell((int) column);
                // check if we got something valid from the excel API
                if ((cell != null) && (cell.getCellType() == Cell.CELL_TYPE_STRING)) {
                    final String content = readCellContent(cell);
                    if ((content != null) && (string.equalsIgnoreCase(content))) {
                        return true;
                    }
                }
            }
        }
        // we checked all the cells
        return false;
    }

    /**
     * read a cell and return its value as cell return null if the type is
     * unknonw or the cell parameter is null
     * 
     * @param cell
     * @return
     */
    private String readCellContent(final HSSFCell cell) {
        if (cell != null) {
            final int type = cell.getCellType();
            switch (type) {
                case Cell.CELL_TYPE_STRING:
                    return cell.getRichStringCellValue().getString().trim();
                case Cell.CELL_TYPE_NUMERIC:
                    return NUMBER_FORMAT.format(cell.getNumericCellValue());
                default:
                    LOGGER.trace("unknown cell type found, cell is neither string nor numeric, returning null");
                    return null;
            }
        } else {
            LOGGER.debug("cell is null, returning null");
            return null;
        }
    }

    protected HashMap<Short, String> findPropertyColumns(final int headerRowId) {
        final HashMap<Short, String> propertyMap = new HashMap<Short, String>();

        final HSSFRow row = hSSFSheet.getRow(headerRowId);
        final short first = hSSFSheet.getRow(headerRowId).getFirstCellNum();
        final short last = hSSFSheet.getRow(headerRowId).getLastCellNum();
        for (short cellnum = first; cellnum <= last; cellnum++) {
            final HSSFCell cell = row.getCell((int) cellnum);
            final String headerLabel = readCellContent(cell);
            if ((headerLabel != null) && (headerLabel.length() > 0)) {
                LOGGER.debug("found a valid label: " + headerLabel + " in column " + cellnum);
                propertyMap.put(cellnum, headerLabel);
            }
        }
        return propertyMap;
    }

    protected HashMap<String, String> readRowValues(final int rowId) {
        final HashMap<String, String> result = new HashMap<String, String>();
        for (final Short column : propertyColumnNumbers.keySet()) {
            final HSSFRow row = hSSFSheet.getRow(rowId);
            if (row != null) {
                final HSSFCell cell = row.getCell((int) column);
                if (cell != null) {
                    final String content = readCellContent(cell);
                    if (content != null) {
                        result.put(propertyColumnNumbers.get(column), content);
                    } // content not null
                } // cell not null
            } // row not null
        }
        return result;
    }

    // iterator methods the iterator variable is count

    @Override
    public boolean hasNext() {
        while (rowCounter <= lastRow) {
            rowCounter = rowCounter + 1;
            counterRowValues = readRowValues(rowCounter);
            // found some data ?
            if (counterRowValues.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HashMap<String, String> next() {
        return counterRowValues;
    }

    @Override
    public void remove() {
        // this is not supported
        assert false : "remove not supported";
    }

}
