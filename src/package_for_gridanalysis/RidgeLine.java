package package_for_gridanalysis;

public class RidgeLine {
    private Cell cell;

    public RidgeLine(Cell cell) {
        this.cell = cell;
    }

    public static double[][] ridge(Cell cell, double[][] Flowdir, String OutputFileName, int UpSampleSize) {
        /** ridge()函数为提取山脊线
         参数解释：
         cell：原始高程栅格
         Flowdir：流向栅格
         OutputFileName：输出文件名
         UpSampleSize：上采样参数
         */

        double nodata = cell.getNodataValue();
        int nrows = cell.getNrows();
        int ncols = cell.getNcols();

        double[][] line = new double[nrows][ncols];
        double[][] temp = new double[nrows][ncols];
        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                temp[i][j] = 0;
                line[i][j] = 0;
            }
        }
        // 记录每个单元接受水流流入的单元数量
        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                if (Flowdir[i][j] == 1)
                    temp[i][j + 1]++;
                if (Flowdir[i][j] == 2)
                    temp[i + 1][j + 1]++;
                if (Flowdir[i][j] == 4)
                    temp[i + 1][j]++;
                if (Flowdir[i][j] == 8)
                    temp[i + 1][j - 1]++;
                if (Flowdir[i][j] == 16)
                    temp[i][j - 1]++;
                if (Flowdir[i][j] == 32)
                    temp[i - 1][j - 1]++;
                if (Flowdir[i][j] == 64)
                    temp[i - 1][j]++;
                if (Flowdir[i][j] == 128)
                    temp[i - 1][j + 1]++;
                if (Flowdir[i][j] == -9999)
                    temp[i][j] = -9999;
            }
        }
        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                if (Flowdir[i][j] == nodata)
                    line[i][j] = nodata;
                if (Flowdir[i][j] != nodata && temp[i][j] == 0)
                    line[i][j] = 1;
                if (Flowdir[i][j] != nodata && temp[i][j] > 0) {
                    line[i][j] = 0;
                }
            }
        }

        // 输出".asc"文件
        FileManager.FileReader.FileWriter.WriteASC(cell, line, OutputFileName);
        // 输出".jpg"文件
        FileManager.FileReader.FileWriter.WriteJPEG(cell, line, OutputFileName, UpSampleSize, 1);

        return line;
    }
}
