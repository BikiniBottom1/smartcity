package package_for_gridanalysis;

/**
 * 表示读取的地理单元格，包括其位置和相关信息。
 */
public class Cell {
    public String NODATA_value;
    private int ncols;
    private int nrows;
    private double xllcorner;
    private double yllcorner;
    private double cellsize;
    private double nodataValue;
    private int rainStationNum;
    private RainStation[] rainStation;
    private double[][] DEM;
    /**
     * 构造函数，用于初始化地理单元格对象。
     *
     * @param nc      单元格列数
     * @param nr      单元格行数
     * @param xc      单元格左下角 X 坐标
     * @param yc      单元格左下角 Y 坐标
     * @param cs      单元格大小
     * @param nodata  无数据值
     * @param rsNum   雨量站数量
     * @param rs      雨量站数组
     * @param dem     数值高程模型数据
     */
    public Cell(int nc, int nr, double xc, double yc, double cs, double nodata, int rsNum, RainStation[] rs, double[][] dem) {
        ncols = nc;
        nrows = nr;
        xllcorner = xc;
        yllcorner = yc;
        cellsize = cs;
        nodataValue = nodata;
        rainStationNum = rsNum;
        rainStation = rs;
        DEM = dem;
    }
    /**
     * 获取单元格的信息。
     */
    public int getNcols() {
        return ncols;
    }

    public int getNrows() {
        return nrows;
    }

    public double getXllcorner() {
        return xllcorner;
    }

    public double getYllcorner() {
        return yllcorner;
    }

    public double getCellsize() {
        return cellsize;
    }

    public double getNodataValue() {
        return nodataValue;
    }

    public int getRainStationNum() {
        return rainStationNum;
    }

    public RainStation[] getRainStation() {
        return rainStation;
    }

    public double[][] getDEM() {
        return DEM;
    }
}
