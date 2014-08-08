// TableReport.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)
package net.maizegenetics.util;

/**
 * Interface for classes with data that can be presented in tables
 *
 * @author Ed Buckler
 */
public interface TableReport {

    /**
     * Get the names of the columns
     *
     * @return columns names
     */
    public Object[] getTableColumnNames();

    /**
     * Get the title of the table
     *
     * @return title
     */
    public String getTableTitle();

    /**
     * Get the number of the columns
     *
     * @return number of columns
     */
    public int getColumnCount();

    /**
     * Get the number of rows
     *
     * @return number of rows
     */
    public long getRowCount();

    /**
     * Get the total number of elements in the dataset. Elements = rowCount *
     * columnCount;
     *
     * @return number of elements
     */
    public long getElementCount();

    /**
     * Returns specified row.
     *
     * @param row row number
     *
     * @return row
     */
    public Object[] getRow(long row);

    /**
     * Returns value at given row and column.
     *
     * @param row row number
     * @param col column number
     *
     * @return data
     */
    public Object getValueAt(long row, int col);

}
