package package_for_gridanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Flowdir {
    private Cell cell;
    public Flowdir(Cell cell) {
        this.cell = cell;
    }
    public static double[][] Flowdir(Cell cell, double[][] FillDem, String force_flow, String OutputFileName, int UpSampleSize) {
        /** Flowdir()为流向计算，采用D8算法输出栅格中每个单元格水体流w动的方向
         参数解释：
         cell：原始高程栅格
         FillDem：填洼栅格
         force_flow：是否强制边缘像元外流
         OutFileName：输出文件名
         UpSampleSize：上采样参数
         */
        double nodata = cell.getNodataValue();
        int nrow = cell.getNrows();
        int ncol = cell.getNcols();

        // 创建一个二维数组 flowdir 用于存储流向信息，其维度为 nrow 行 ncol 列。
        // 创建多个列表用于存储不同数据的集合，包括：
        // Z_list：用于存储8个领域的 Z 值
        // steep_list：用于存储8个领域的坡度值
        // Z_order_list：用于存储按 Z 值排序后的集合
        // steep_order_list：用于存储按坡度值排序后的集合
        // origin_Z_list：用于保存最原始的8领域的 Z 值
        // origin_Z_order_list：用于保存最原始的8领域的 Z 值排序后的集合
        // 初始化一个变量 steepest，用于存储坡度值。
        double[][] flowdir = new double[nrow][ncol];
        List<Double> Z_list = new ArrayList(8);
        List<Double> steep_list = new ArrayList(8);
        List<Double> Z_order_list = new ArrayList(8);
        List<Double> steep_order_list = new ArrayList(8);
        List<Double> origin_Z_list = new ArrayList(8); // 用来保存最原始的8领域的z值
        List<Double> origin_Z_order_list = new ArrayList(8); // 用来保存最原始的8领域的z值
        double steepest = 0;

        // 将nodata值从9999变成-9999，同时将背景像元的流向设为nodata
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                if (FillDem[i][j] == nodata) {
                    FillDem[i][j] = -nodata;
                    flowdir[i][j] = nodata;
                }
            }
        }

        // 循环遍历每个像元
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                // 过滤掉背景像元，只对内部像元和边缘像元进行计算
                if (FillDem[i][j] != -nodata) {

                    Z_list.clear();
                    steep_list.clear();
                    Z_order_list.clear();
                    steep_order_list.clear();

                    boolean flag = true;  // 循环标志
                    int[] search_area = new int[8]; // 搜索区域
                    for (int m = 0; m < 8; m++) // 先对搜索区域进行初始化，初始化大小为一个步长
                    {
                        Z_list.add(0.0);
                        search_area[m] = 1;
                    }

                    while (flag) {
                        // 将相邻像元的Z拉伸成一个列表
                        if (Z_list.get(0) != nodata) {
                            Z_list.set(0, j + search_area[0] < ncol ? FillDem[i][j + search_area[0]] : -nodata);
                        }
                        if (Z_list.get(1) != nodata) {
                            Z_list.set(1, i + search_area[1] < nrow && j + search_area[1] < ncol ? FillDem[i + search_area[1]][j + search_area[1]] : -nodata);
                        }
                        if (Z_list.get(2) != nodata) {
                            Z_list.set(2, i + search_area[2] < nrow ? FillDem[i + search_area[2]][j] : -nodata);
                        }
                        if (Z_list.get(3) != nodata) {
                            Z_list.set(3, i + search_area[3] < nrow && j - search_area[3] >= 0 ? FillDem[i + search_area[3]][j - search_area[3]] : -nodata);
                        }
                        if (Z_list.get(4) != nodata) {
                            Z_list.set(4, j - search_area[4] >= 0 ? FillDem[i][j - search_area[4]] : -nodata);
                        }
                        if (Z_list.get(5) != nodata) {
                            Z_list.set(5, i - search_area[5] >= 0 && j - search_area[5] >= 0 ? FillDem[i - search_area[5]][j - search_area[5]] : -nodata);
                        }
                        if (Z_list.get(6) != nodata) {
                            Z_list.set(6, i - search_area[6] >= 0 ? FillDem[i - search_area[6]][j] : -nodata);
                        }
                        if (Z_list.get(7) != nodata) {
                            Z_list.set(7, i - search_area[7] >= 0 && j + search_area[7] < ncol ? FillDem[i - search_area[7]][j + search_area[7]] : -nodata);
                        }

                        // 对所有Z值进行排序
                        for (int k = 0; k < 8; k++) {
                            Z_order_list.add(Z_list.get(k));
                        }
                        Collections.sort(Z_order_list);

                        for (int k = 0; k < 8; k++) {
                            // 如果相邻像元的值为nodata就不必计算降幅了
                            if (Z_list.get(k) == nodata) {
                                steep_list.add(nodata);
                            } else {
                                double dis = k % 2 == 0 ? 1 : Math.sqrt(2);
                                steep_list.add((FillDem[i][j] - Z_list.get(k)) / dis);
                            }
                        }

                        // 找到最大降幅
                        for (int k = 0; k < 8; k++) {
                            steep_order_list.add(steep_list.get(k));
                        }
                        Collections.sort(steep_order_list);
                        steepest = steep_order_list.get(7);

                        // 判断是否有多个最大降幅，是则扩大搜索范围，否则进入下一阶段
                        int n_steepest = 0;
                        for (int k = 0; k < 8; k++) {
                            if (steep_list.get(k) == steepest) {
                                n_steepest++; // 记录最大降幅的个数
                                // 只在最陡的像元方向上扩大搜索范围
                                search_area[k]++;
                            }
                            // 如果不是最大降幅的方向，则将像元值设置为nodata，之后该方向的高程信息将不再更新
                            else {
                                Z_list.set(k, nodata);
                            }
                        }

                        if (n_steepest == 1 || Z_order_list.get(7) == -nodata) {
                            flag = false;
                        }
                    }

                    //  记录最开始的相邻像元的信息，用于判断像元类型
                    origin_Z_list.clear();
                    origin_Z_list.add(FillDem[i][j + 1]);
                    origin_Z_list.add(FillDem[i + 1][j + 1]);
                    origin_Z_list.add(FillDem[i + 1][j]);
                    origin_Z_list.add(FillDem[i + 1][j - 1]);
                    origin_Z_list.add(FillDem[i][j - 1]);
                    origin_Z_list.add(FillDem[i - 1][j - 1]);
                    origin_Z_list.add(FillDem[i - 1][j]);
                    origin_Z_list.add(FillDem[i - 1][j + 1]);
                    origin_Z_order_list.clear();
                    for (int k = 0; k < 8; k++) {
                        origin_Z_order_list.add(origin_Z_list.get(k));
                    }
                    Collections.sort(origin_Z_order_list);

                    // 如果是内部像元或者不强制外流时，像元流向为降幅最大的内部像元，内部像元和边缘像元的算法一致
                    if (force_flow.equals("Normal")  || (force_flow .equals("Force" )&& origin_Z_order_list.get(7) != -nodata)) {
                        // 匹配到最大降幅对应的方向
                        for (int k = 0; k < 8; k++) {
                            if (steep_list.get(k) == steepest) {
                                flowdir[i][j] = Math.pow(2, k);
                            }
                        }
                    }

                    // 强制边缘像元外流时，边缘像元的计算方式，流向为任一外部流向
                    else {
                        for (int k = 0; k < 8; k++) {
                            if (origin_Z_list.get(k) == -nodata) {
                                flowdir[i][j] = Math.pow(2, k);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 输出".asc"文件
        FileManager.FileReader.FileWriter.WriteASC(cell, flowdir, OutputFileName);
        // 输出".jpg"文件
        FileManager.FileReader.FileWriter.WriteJPEG(cell, flowdir, OutputFileName, UpSampleSize, 1);
        // 输出".jpg"格式彩色图
        FileManager.FileReader.FileWriter.WriteColorJPEG(cell, flowdir, OutputFileName + "_color", UpSampleSize, 1, "Flowdir");

        return flowdir;
    }

}
