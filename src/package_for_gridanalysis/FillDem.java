package package_for_gridanalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FillDem {
    private Cell cell;

    //构造函数初始化
    public FillDem(Cell cell) {
        this.cell = cell;
    }

    //填洼计算及输出
    public static double[][] FillDem_MV(Cell cell, String OutputFileName, int UpSampleSize) {
        /** FillDem_MV()为采用M&V算法的填洼计算
         参数解释：
         cell：原始高程栅格
         OutFileName：输出文件名
         UpSampleSize：上采样参数
         */

        // 获取 DEM 数据
        double[][] DEM = cell.getDEM();
        int nrows = cell.getNrows();
        int ncols = cell.getNcols();

        // 为避免原栅格的边界像元无法参与计算，故拓展高程栅格
        int NewRows = nrows + 2;  // 拓展后高程栅格的行数
        int NewCols = ncols + 2;  // 拓展后高程栅格的列数
        double Dem_Expand[][] = new double[NewRows][NewCols];

        double MaxValue = 0;    // 栅格最大值
        double MinValue = 0.1;  // 定义极小值为0.1
        double dtemp = 0;
        for (int i = 0; i <nrows; i++) {
            dtemp = Arrays.stream(DEM[i]).max().getAsDouble(); // stream流求最大值
            if (dtemp > MaxValue) {
                MaxValue = dtemp;
            }
        }

        for (int i = 0; i < NewRows; i++) {
            for (int j = 0; j < NewCols; j++) {
                if (i == 0 || j == 0 || i == NewRows - 1 || j == NewCols - 1) {
                    Dem_Expand[i][j] = cell.getNodataValue(); // 外边界值为NODATA
                } else {
                    Dem_Expand[i][j] = DEM[i - 1][j - 1];     // 用原始高程值填充新高程栅格
                }
            }
        }

        for (int i = 1; i < NewRows - 1; i++) {
            for (int j = 1; j < NewCols - 1; j++) {
                if (Dem_Expand[i + 1][j] == cell.getNodataValue() || Dem_Expand[i][j + 1] == cell.getNodataValue() ||
                        Dem_Expand[i + 1][j + 1] == cell.getNodataValue() || Dem_Expand[i - 1][j] == cell.getNodataValue() ||
                        Dem_Expand[i][j - 1] ==cell.getNodataValue() || Dem_Expand[i - 1][j - 1] == cell.getNodataValue() ||
                        Dem_Expand[i + 1][j - 1] == cell.getNodataValue() || Dem_Expand[i - 1][j + 1] == cell.getNodataValue()) {
                    continue;        // DEM边界处的像元保持原值不变
                } else {
                    Dem_Expand[i][j] = MaxValue + 0.1; // 非边界处的像元"填满水"，即赋值为栅格最大值+1
                }
            }
        }

        double Dem_Filled[][] = new double[nrows][ncols];  // 结果栅格，用于存储填洼计算结果的数组
        for (int i = 1; i < NewRows - 1; i++) {
            for (int j = 1; j < NewCols - 1; j++) {
                Dem_Filled[i - 1][j - 1] = Dem_Expand[i][j];
            }
        }

        List<Double> list = new ArrayList(10);
        boolean LoopFlag;  // While循环的条件
        do {               // M%V算法：不断迭代，让"填满水"的栅格表面逐步逼近真实的无洼地形
            LoopFlag = false;
            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    if (Dem_Filled[i][j] != DEM[i][j]) { // 当结果栅格中的像元值与原值不同时寻找附近的最低值
                        list.add(Dem_Filled[i][j + 1]);            // 添加周围像元的栅格值
                        list.add(Dem_Filled[i + 1][j + 1]);
                        list.add(Dem_Filled[i + 1][j]);
                        list.add(Dem_Filled[i + 1][j - 1]);
                        list.add(Dem_Filled[i][j - 1]);
                        list.add(Dem_Filled[i - 1][j - 1]);
                        list.add(Dem_Filled[i - 1][j]);
                        list.add(Dem_Filled[i - 1][j + 1]);
                        Collections.sort(list);                    // 排序列表便于求周围像元的最小值
                        if (Dem_Filled[i][j] > (list.get(0) + MinValue)) {
                            Dem_Filled[i][j] = list.get(0) + MinValue;
                            LoopFlag = true;                       // 当结果栅格中的像元值发生变化 则迭代继续
                        }
                        if (DEM[i][j] > list.get(0) + MinValue) {
                            Dem_Filled[i][j] = DEM[i][j];
                            LoopFlag = true;
                        }
                        list.clear();
                    }
                }
            }
        } while (LoopFlag);

        // 输出".asc"文件
        FileManager.FileReader.FileWriter.WriteASC(cell, Dem_Filled, OutputFileName);
        // 输出".jpg"文件
        FileManager.FileReader.FileWriter.WriteJPEG(cell, Dem_Filled, OutputFileName, UpSampleSize, 1);
        // 输出".jpg"格式彩色图
        FileManager.FileReader.FileWriter.WriteColorJPEG(cell, Dem_Filled, OutputFileName+"_color", UpSampleSize, 1,"FillDem");

        return Dem_Filled;
    }

}

