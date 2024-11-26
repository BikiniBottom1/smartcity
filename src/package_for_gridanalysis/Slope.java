package package_for_gridanalysis;

import java.io.IOException;
public class Slope {

    private Cell cell;
    private double[][] slope; // 将slope声明为类的成员变量


    public Slope(Cell cell) {
        this.cell = cell;
    }

    // getSlope函数：获取各栅格的坡度信息 //
    public double[][] getSlope() throws IOException {
        double[][] Dem = cell.getDEM(); // 栅格中获取DEM
        int nrows = cell.getNrows();
        int ncols = cell.getNcols();
        double Nodata = cell.getNodataValue();
        double cellsize = cell.getCellsize();

        this.slope = new double[nrows][ncols]; // 使用成员变量而不是局部变量

        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                double dz_dx, dz_dy; // 初始化变化率

                if (i == 0 || j == 0 || i == nrows - 1 || j == ncols - 1 || Dem[i][j] == Nodata) {
                    slope[i][j] = Nodata; // 栅格边界和Nodata像元赋值Nodata
                } else {
                    int validCount = countValidNeighbours(Dem, i, j, Nodata);

                    if (validCount == 8) {
                        dz_dx = calculateDzDx(Dem, i, j, cellsize);
                        dz_dy = calculateDzDy(Dem, i, j, cellsize);

                        this.slope[i][j] = Math.atan(Math.sqrt(Math.pow(dz_dx, 2.0) + Math.pow(dz_dy, 2.0))) * 180 / Math.PI;
                    } else {
                        this.slope[i][j] = Nodata; // 小于8个有效像元像元赋值Nodata
                    }
                }
            }
        }
        return this.slope;
    }
    /**
     * 计算指定位置（i，j）周围有效邻居的数量。
     *
     * @param dem     DEM数据矩阵
     * @param i       行索引
     * @param j       列索引
     * @param nodata  无数据标志值
     * @return 有效邻居的数量
     */
    private int countValidNeighbours(double[][] dem, int i, int j, double nodata) {
        int count = 0;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (di == 0 && dj == 0) continue;
                int ni = i + di, nj = j + dj;
                if (ni >= 0 && ni < dem.length && nj >= 0 && nj < dem[0].length && dem[ni][nj] != nodata) {
                    count++;
                }
            }
        }
        return count;
    }
    //计算指定位置（i，j）处DEM数据的梯度值（沿x方向）。
    private double calculateDzDx(double[][] dem, int i, int j, double cellsize) {
        return ((dem[i-1][j+1] + 2 * dem[i][j+1] + dem[i+1][j+1]) - (dem[i-1][j-1] + 2 * dem[i][j-1] + dem[i+1][j-1])) / (8 * cellsize);
    }
    //计算指定位置（i，j）处DEM数据的梯度值（沿y方向）。
    private double calculateDzDy(double[][] dem, int i, int j, double cellsize) {
        return ((dem[i+1][j-1] + 2 * dem[i+1][j] + dem[i+1][j+1]) - (dem[i-1][j-1] + 2 * dem[i-1][j] + dem[i-1][j+1])) / (8 * cellsize);
    }
    // outputResults函数：处理输出结果
    public void outputResults(String outputFileName, int upSampleSize) {
        float quality = 1f; // 图片压缩程度

        // 输出".asc"文件
        FileManager.FileReader.FileWriter.WriteASC(cell, slope, outputFileName);
        // 输出".jpg"文件
        FileManager.FileReader.FileWriter.WriteJPEG(cell, slope, outputFileName, upSampleSize, quality);
        // 输出".jpg"格式彩色图
        FileManager.FileReader.FileWriter.WriteColorJPEG(cell, slope, outputFileName + "_color", upSampleSize, quality, "package_for_gridanalysis.Slope");

}
}
