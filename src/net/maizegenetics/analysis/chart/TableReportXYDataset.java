package net.maizegenetics.analysis.chart;

import net.maizegenetics.util.TableReport;
import org.jfree.data.xy.DefaultTableXYDataset;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: USDA-ARS</p>
 * @author Ed Buckler
 * @version 1.0
 */

public class TableReportXYDataset extends DefaultTableXYDataset {
  double[][] theData;
  String[] seriesNames;
  String xName;
  int numberYAxes=1;

  public TableReportXYDataset(TableReport theTable, int seriesX, int seriesY) {
    numberYAxes=1;
    setTableReport(theTable, seriesX, seriesY, -1);
  }
  public TableReportXYDataset(TableReport theTable, int seriesX, int seriesY1, int seriesY2) {
    numberYAxes=2;
    setTableReport(theTable, seriesX, seriesY1, seriesY2);
}


  public int getItemCount(int parm1) {
    return theData.length;
    //throw new java.lang.UnsupportedOperationException("Method getItemCount() not yet implemented.");
  }


    public Number getX(int series, int item) {
    Double x = new Double(theData[item][0]);
    return x;
//    throw new java.lang.UnsupportedOperationException("Method getXValue() not yet implemented.");
  }

  public int getSeriesCount() {
    return numberYAxes;
    //throw new java.lang.UnsupportedOperationException("Method getSeriesCount() not yet implemented.");
  }


    public Number getY(int series, int item) {
    Double y = new Double(theData[item][1+series]);
    return y;
//    throw new java.lang.UnsupportedOperationException("Method getYValue() not yet implemented.");
  }
  public String getSeriesName(int series) {
    /**current*/
    return seriesNames[series];
//    throw new java.lang.UnsupportedOperationException("Method getSeriesName() not yet implemented.");
  }

      public String getSeriesKey(int series) {
    /**current*/
    return seriesNames[series];
//    throw new java.lang.UnsupportedOperationException("Method getSeriesName() not yet implemented.");
  }

  public String getXName() {
  /**current*/
  return xName;
//    throw new java.lang.UnsupportedOperationException("Method getSeriesName() not yet implemented.");
}


  public boolean setTableReport(TableReport theTable, int seriesX, int seriesY1, int seriesY2) {
    int countGood = 0;
    double[][] tempData = new double[theTable.getRowCount()][numberYAxes+1];
    for (int i = 0; i < theTable.getRowCount(); i++) {
        Object[] theRawData=theTable.getRow(i);
      try {
        tempData[countGood][0] = Double.valueOf(theRawData[seriesX].toString()).doubleValue();
        tempData[countGood][1] = Double.valueOf(theRawData[seriesY1].toString()).doubleValue();
        if (Double.isNaN(tempData[countGood][0])||Double.isNaN(tempData[countGood][1]))
            {throw new NumberFormatException();}
        if(numberYAxes==2) {
          tempData[countGood][2] = Double.valueOf(theRawData[seriesY2].toString()).doubleValue();
          if (Double.isNaN(tempData[countGood][2]))
            {throw new NumberFormatException();}
        }
        countGood++;
      }
      catch (NumberFormatException ex)
        {System.out.println("throw new NumberFormatException();");}
    }
    theData = new double[countGood][numberYAxes+1];
    for (int i = 0; i < countGood; i++) {
      for(int j=0; j<theData[0].length; j++) theData[i][j] = tempData[i][j];
    }
    seriesNames=new String[numberYAxes];
    Object[] theNames=theTable.getTableColumnNames();
    xName=(String)theNames[seriesX];
    seriesNames[0]=(String)theNames[seriesY1];
    if(numberYAxes==2) seriesNames[1]=(String)theNames[seriesY2];
    return true;
  }
}
