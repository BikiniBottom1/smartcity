package package_for_gridanalysis;

import java.util.ArrayList;

public class Accumulation {
    private Cell cell;

    public Accumulation(Cell cell) {
        this.cell = cell;
    }

    public static double[][] acc(Cell cell, double[][] Flowdir, boolean IsWeight, double[][] Rain_fall, String OutputFileName, int UpSampleSize) {
        /** acc()为累计流计算
         参数解释：
         cell：原始高程栅格
         Flowdir：流向栅格
         IsWeight：是否使用降雨量插值结果作为权重栅格
         Rain_fall：降雨量插值结果
         OutFileName：输出文件名
         UpSampleSize：上采样参数
         */
        double nodata = cell.getNodataValue();
        int nrow = cell.getNrows();
        int ncol = cell.getNcols();
        // 创建三维数组，1表示依赖矩阵，2标记每次循环计算的像元位置，3表示累计流结果
        double[][][] Matrix3 = new double[nrow][ncol][3];
        double[][] isCal = new double[nrow][ncol];

        // 初始化
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                Matrix3[i][j][0] = 0;
                Matrix3[i][j][1] = 0;
                Matrix3[i][j][2] = 0;
            }
        }
        // 计算初始依赖矩阵
        Matrix3 = Dependency(nrow, ncol, Flowdir);
        // 创建队列Q（此处用两个集合表示Q的索引），Q表示D（c）=0的单元队列，即没有水流入的单元
        ArrayList list_x = new ArrayList();
        ArrayList list_y = new ArrayList();
        boolean flag = true;
        // 每次循环待计算像元的位置
        double[][] is_cal = new double[nrow][ncol];
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                is_cal[i][j] = Matrix3[i][j][1];
            }
        }
        while (flag) {
            // 将待计算像元的行列号传入队列
            for (int i = 0; i < nrow; i++) {
                for (int j = 0; j < ncol; j++) {
                    if (Matrix3[i][j][0] == 0 && is_cal[i][j] == 1) {
                        list_x.add(i);
                        list_y.add(j);
                    }
                }
            }
            Matrix3 = accCal(Matrix3, nrow, ncol, list_x, list_y, Flowdir, IsWeight, Rain_fall);
            int sum = 0;
            for (int i = 0; i < nrow; i++) {
                for (int j = 0; j < ncol; j++) {
                    is_cal[i][j] = Matrix3[i][j][1];
                    Matrix3[i][j][1] = 0;
                }
            }
            // 当传入单元的数量为0时结束循环
            if (list_x.size() == 0) {
                flag = false;
            }
            // 清除集合中的记录
            list_x.clear();
            list_y.clear();
        }
        double[][] accumulation = new double[nrow][ncol];
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                // 将计算结果传入结果的二维数组
                accumulation[i][j] = Matrix3[i][j][2];
                if (Flowdir[i][j] == -9999)
                    accumulation[i][j] = nodata;
            }
        }

        // 输出".asc"文件
        FileManager.FileReader.FileWriter.WriteASC(cell, accumulation, OutputFileName);
        // 输出".jpg"文件
        FileManager.FileReader.FileWriter.WriteJPEG(cell, accumulation, OutputFileName, UpSampleSize, 1);
        // 输出".jpg"格式彩色图
        FileManager.FileReader.FileWriter.WriteColorJPEG(cell, accumulation, OutputFileName + "_color", UpSampleSize, 1, "Accumulation");

        return accumulation;
    }

    // 计算依赖矩阵D，判断计算矩阵is_acc,两者组合为一个三维数组(第三层为累积流)
    public static double[][][] Dependency(int nrow, int ncol, double[][] Flowdir) {
        double[][][] temp = new double[nrow][ncol][3];
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                temp[i][j][0] = 0;
                temp[i][j][1] = 0;
                temp[i][j][2] = 0;
            }
        }
        // 记录每个单元接受水流流入的单元数量
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                if (Flowdir[i][j] == 1)
                    temp[i][j + 1][0]++;
                if (Flowdir[i][j] == 2)
                    temp[i + 1][j + 1][0]++;
                if (Flowdir[i][j] == 4)
                    temp[i + 1][j][0]++;
                if (Flowdir[i][j] == 8)
                    temp[i + 1][j - 1][0]++;
                if (Flowdir[i][j] == 16)
                    temp[i][j - 1][0]++;
                if (Flowdir[i][j] == 32)
                    temp[i - 1][j - 1][0]++;
                if (Flowdir[i][j] == 64)
                    temp[i - 1][j][0]++;
                if (Flowdir[i][j] == 128)
                    temp[i - 1][j + 1][0]++;
                if (Flowdir[i][j] == -9999)
                    temp[i][j][0] = -9999;
            }
        }
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                if (Flowdir[i][j] != -9999 && temp[i][j][0] == 0)
                    temp[i][j][1] = 1;
            }
        }
        return temp;
    }

    //  计算累积流
    public static double[][][] accCal(double[][][] com3, int nrow, int ncol, ArrayList list_x, ArrayList list_y, double[][] Flowdir, boolean IsWeight, double weight[][]) {
        double[][][] temp = new double[nrow][ncol][3];
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                temp[i][j][0] = com3[i][j][0];
                temp[i][j][1] = com3[i][j][1];
                temp[i][j][2] = com3[i][j][2];
            }
        }
        // 每次传入的单元的以来矩阵记录值为0，可以理解为该单元的累积流已经计算完毕，可以由它流出
        for (int i = 0; i < list_x.size(); i++) {
            int x = (int) list_x.get(i);
            int y = (int) list_y.get(i);
            if (Flowdir[x][y] == 1) {
                if (IsWeight == false)
                    temp[x][y + 1][2] += temp[x][y][2] + 1;
                else
                    temp[x][y + 1][2] += temp[x][y][2] + weight[x][y];
                temp[x][y + 1][0]--;// 流入单元的累积流累加之后，该单元的依赖矩阵自减
                temp[x][y][1] = 0;// 将上一步待计算的像元的判断矩阵记录值赋为0
                if (temp[x][y + 1][0] == 0)
                    temp[x][y + 1][1] = 1;
            }
            if (Flowdir[x][y] == 2) {
                if (IsWeight == false)
                    temp[x + 1][y + 1][2] += temp[x][y][2] + 1;
                else
                    temp[x + 1][y + 1][2] += temp[x][y][2] + weight[x][y];
                temp[x + 1][y + 1][0]--;
                temp[x][y][1] = 0;
                if (temp[x + 1][y + 1][0] == 0)
                    temp[x + 1][y + 1][1] = 1;
            }
            if (Flowdir[x][y] == 4) {
                if (IsWeight == false)
                    temp[x + 1][y][2] += temp[x][y][2] + 1;
                else
                    temp[x + 1][y][2] += temp[x][y][2] + weight[x][y];
                temp[x + 1][y][0]--;
                temp[x][y][1] = 0;
                if (temp[x + 1][y][0] == 0)
                    temp[x + 1][y][1] = 1;
            }
            if (Flowdir[x][y] == 8) {
                if (IsWeight == false)
                    temp[x + 1][y - 1][2] += temp[x][y][2] + 1;
                else
                    temp[x + 1][y - 1][2] += temp[x][y][2] + weight[x][y];
                temp[x + 1][y - 1][0]--;
                temp[x][y][1] = 0;
                if (temp[x + 1][y - 1][0] == 0)
                    temp[x + 1][y - 1][1] = 1;
            }
            if (Flowdir[x][y] == 16) {
                if (IsWeight == false)
                    temp[x][y - 1][2] += temp[x][y][2] + 1;
                else
                    temp[x][y - 1][2] += temp[x][y][2] + weight[x][y];
                temp[x][y - 1][0]--;
                temp[x][y][1] = 0;
                if (temp[x][y - 1][0] == 0)
                    temp[x][y - 1][1] = 1;
            }
            if (Flowdir[x][y] == 32) {
                if (IsWeight == false)
                    temp[x - 1][y - 1][2] += temp[x][y][2] + 1;
                else
                    temp[x - 1][y - 1][2] += temp[x][y][2] + weight[x][y];
                temp[x - 1][y - 1][0]--;
                temp[x][y][1] = 0;
                if (temp[x - 1][y - 1][0] == 0)
                    temp[x - 1][y - 1][1] = 1;
            }
            if (Flowdir[x][y] == 64) {
                if (IsWeight == false)
                    temp[x - 1][y][2] += temp[x][y][2] + 1;
                else
                    temp[x - 1][y][2] += temp[x][y][2] + weight[x][y];
                temp[x - 1][y][0]--;
                temp[x][y][1] = 0;
                if (temp[x - 1][y][0] == 0)
                    temp[x - 1][y][1] = 1;
            }
            if (Flowdir[x][y] == 128) {
                if (IsWeight == false)
                    temp[x - 1][y + 1][2] += temp[x][y][2] + 1;
                else
                    temp[x - 1][y + 1][2] += temp[x][y][2] + weight[x][y];
                temp[x - 1][y + 1][0]--;
                temp[x][y][1] = 0;
                if (temp[x - 1][y + 1][0] == 0)
                    temp[x - 1][y + 1][1] = 1;
            }
        }
        return temp;
    }
}