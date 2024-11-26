package package_for_gridanalysis;

public class StreamNet {
    private Cell cell;

    public StreamNet(Cell cell) {
        this.cell = cell;
    }
    public static double[][] StreamNet(Cell cell, double[][] acc, int n, String OutputFileName, int UpSampleSize) {
        /**
         * package_for_gridanalysis.StreamNet()为河网提取计算，可输出研究区内河网。
         * 参数解释：
         * cell为原始高程栅格
         * acc为累积流栅格
         * n为提取河网的阈值。n越大，提取河网越稀疏；n越小，提取河网越密集。
         * OutFileName为输出文件名
         * UpSampleSize为上采样参数
         */
        double nodata = cell.getNodataValue();
        int nrows = cell.getNrows();
        int ncols = cell.getNcols();
        double[][] streamNet = new double[nrows][ncols]; // 用于存放河网的二维数组

        for (int i = 0; i < nrows - 1; i++) {
            for (int j = 0; j < ncols - 1; j++) {
                if (acc[i][j] != nodata) {
                    if (acc[i][j] > n)
                        streamNet[i][j] = 1; // 累积流大于指定阈值处，认为存在河网
                    else
                        streamNet[i][j] = 0;  // 累积流小于指定阈值处，认为不存在河网
                } else {
                    streamNet[i][j] = nodata;
                }
            }
        }

        float quality = 1f;

        // 输出".asc"文件
        FileManager.FileReader.FileWriter.WriteASC(cell, streamNet, OutputFileName);
        // 输出".jpg"文件
        FileManager.FileReader.FileWriter.WriteJPEG(cell, streamNet, OutputFileName, UpSampleSize, quality);

        return streamNet;
    }
}
