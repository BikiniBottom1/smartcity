package package_for_gridanalysis;

import static java.lang.Math.*;

public class Aspect {
    private Cell cell;

    public Aspect(Cell cell) {
        this.cell = cell;
    }

    public static double[][] Aspect(Cell cell, String OutputFileName, int UpSampleSize) {
        /**
         * Aspect()为坡向计算，输出栅格中每个单元格坡面所朝向的罗盘方向值
         * 将按照顺时针方向进行测量，角度范围介于 0（正北）到 360（仍是正北）之间，不具有坡度的平坦区域赋值为-1
         * 参数解释：
         * cell：原始高程栅格
         * OutFileName：输出文件名
         * UpSampleSize:上采样参数
         */
        double nodata = cell.getNodataValue();
        int nrows = cell.getNrows();
        int ncols = cell.getNcols();
        double[][] DEM = cell.getDEM();

        double[][] aspect = new double[nrows][ncols]; // 用于存放坡向值的二维数组

        double AspectClass[][] = new double[nrows][ncols]; // 用于存放坡向分类后编码的二维数组
        for (int i = 1; i < nrows - 1; i++) {   // 从第2行第2列开始遍历，即最外面一圈没有参与循环
            for (int j = 1; j < ncols - 1; j++) {
                if (DEM[i][j] == nodata) {
                    aspect[i][j] = nodata; // 原单元格若为NoData，其坡向值也为NoData
                } else {
                    double NeighborDem[] = new double[8]; // 用于存储周围8邻域像元值的数组
                    int NeighborHasValue[] = new int[8]; // 用于标记周围8邻域是否为NoData的逻辑数组
                    for (int n = 0; n < 8; n++) {  // 将周围8邻域的像元值提取进数组中
                        if (n < 3) {
                            NeighborDem[n] = DEM[i - 1][j - 1 + n];
                        } else if (n > 4) {
                            NeighborDem[n] = DEM[i + 1][j - 6 + n];
                        } else if (n == 3) {
                            NeighborDem[n] = DEM[i][j - 1];
                        } else {
                            NeighborDem[n] = DEM[i][j + 1];
                        }
                        if (NeighborDem[n] != nodata) {  // 判断周围8邻域像元值是否为NoData
                            NeighborHasValue[n] = 1; // 若不是NoData，逻辑数组相应位置为1
                        } else {  // 若是NoData，逻辑数组相应位置为0，同时像元值改为0，便于后续计算
                            NeighborDem[n] = 0;
                            NeighborHasValue[n] = 0;
                        }
                    }
                    double weight[] = new double[4]; // 用于存放有效像元加权计数w的数组
                    weight[0] = NeighborHasValue[2] + NeighborHasValue[4] * 2 + NeighborHasValue[7];
                    weight[1] = NeighborHasValue[0] + NeighborHasValue[3] * 2 + NeighborHasValue[5];
                    weight[2] = NeighborHasValue[5] + NeighborHasValue[6] * 2 + NeighborHasValue[7];
                    weight[3] = NeighborHasValue[0] + NeighborHasValue[1] * 2 + NeighborHasValue[2];
                    if (weight[0] == 0 || weight[1] == 0 || weight[2] == 0 || weight[3] == 0) {  // 判断权重w是否为0
                        aspect[i][j] = nodata; // 权重在计算中将作为分母，因此若4个权重任一为0，该中心像元的坡度为NoData
                    } else {
                        double SlopeX; // X方向的增量dz/dx
                        double SlopeY; // Y方向的增量dz/dy
                        SlopeX = ((NeighborDem[2] + NeighborDem[4] * 2 + NeighborDem[7]) * 4 / weight[0] - (NeighborDem[0] + NeighborDem[3] * 2 + NeighborDem[5]) * 4 / weight[1]) / (8 * cell.getCellsize());
                        SlopeY = ((NeighborDem[5] + NeighborDem[6] * 2 + NeighborDem[7]) * 4 / weight[2] - (NeighborDem[0] + NeighborDem[1] * 2 + NeighborDem[2]) * 4 / weight[3]) / (8 * cell.getCellsize());
                        double AspectValue = atan2(SlopeX, SlopeY); // aspect = atan2 ([dz/dx] / [dz/dy]) * 180 / 𝝅
                        double DirectionValue = CompassDirection(AspectValue); // 将坡向转化为罗盘方向值
                        aspect[i][j] = DirectionValue; // 最终的坡向值
                        double ClassValue = Classification(DirectionValue); // 将坡向值进一步分类
                        AspectClass[i][j] = ClassValue; // 坡向的分类值
                    }
                }
            }
        }
        // 最外圈一层若参与计算，4个w中必定有为0的情况，坡度将输出为NoData，故直接赋值为NoData，不参与上述运算
        for (int i = 0; i < nrows; i++) {
            aspect[i][0] = nodata;
            aspect[i][ncols - 1] = nodata;
        }
        for (int j = 1; j < ncols - 1; j++) {
            aspect[0][j] =nodata;
            aspect[nrows - 1][j] = nodata;
        }
        float quality = 1f;   // 图片压缩程度(详见ExportData类中WriteJPEG()函数)

        // 输出".asc"文件
        FileManager.FileReader.FileWriter.WriteASC(cell, aspect, OutputFileName);
        // 输出坡向值".jpg"文件
        FileManager.FileReader.FileWriter.WriteJPEG(cell, aspect, OutputFileName, UpSampleSize, quality);
        // 输出坡向分类值彩色".jpg"文件
        FileManager.FileReader.FileWriter.WriteColorJPEG(cell, aspect, OutputFileName + "_class_color", UpSampleSize, quality, "Aspect");

        return aspect;
    }

    // 坡向计算过程中的atan2函数，注：此atan2函数包括将计算出的atan2函数值由弧度转化为度
    public static double atan2(double SlopeX, double SlopeY) {
        double AspectValue = -1; // 初始化函数返回值AspectValue
        if (SlopeX > 0) {  // atan2(x,y)=arctan(y/x), x>0
            AspectValue = toDegrees(atan(SlopeY / SlopeX));
        } else if (SlopeX < 0 && SlopeY >= 0) {  // atan2(x,y)=arctan(y/x)+𝜋, x<0, y>=0
            AspectValue = toDegrees(atan(SlopeY / SlopeX) + PI);
        } else if (SlopeX < 0 && SlopeY < 0) {  // atan2(x,y)=arctan(y/x)-𝜋, x<0, y<0
            AspectValue = toDegrees(atan(SlopeY / SlopeX) - PI);
        } else if (SlopeX == 0 && SlopeY > 0) {  // atan2(x,y)=𝜋/2, x=0, y>0
            AspectValue = 90;
        } else if (SlopeX == 0 && SlopeY < 0) {  // atan2(x,y)=-𝜋/2, x=0, y<0
            AspectValue = -90;
        } else if (SlopeX == 0 && SlopeY == 0) {  // atan2(x,y)=-1, x=0, y=0
            AspectValue = -1;
        } else {
            System.out.println("SlopeX or SlopeY calculation error!");
            System.exit(0);
        }
        return AspectValue;
    }

    // 将计算出的坡向值AspectValue转化为罗盘方向值
    public static double CompassDirection(double AspectValue) {
        double CellValue = -9999; // 初始化罗盘方向值CellValue
        if (AspectValue == -1) {  // 平坦区域AspectValue为-1，罗盘方向值也为-1
            CellValue = -1;
        } else if (AspectValue < 0) {  // AspectValue范围为-180~180，需转换到0~360
            CellValue = 360 - 90 + AspectValue; // 当AspectValue<0时
        } else if (AspectValue >= 90.0) {
            CellValue = AspectValue - 90; // 当AspectValue>90时
        } else {
            CellValue = 360 - 90 + AspectValue; // 当0<=AspectValue<90时
        }
        return CellValue;//
    }

    // 将罗盘方向值再分类，共分为9类：北、东北、东、东南、南、西南、西、西北、平面
    public static double Classification(double CompassDirection) {
        double ClassValue = -1;
        if (CompassDirection == -1) {
            ClassValue = -1; // 平坦区域为-1
        } else if (CompassDirection <= 22.5 || CompassDirection > 337.5) {
            ClassValue = 0; // 北(0~22.5或337.5~360)为0
        } else if (CompassDirection <= 67.5) {
            ClassValue = 45; // 东北(22.5~67.5)为45
        } else if (CompassDirection <= 112.5) {
            ClassValue = 90; // 东(67.5~112.5)为90
        } else if (CompassDirection <= 157.5) {
            ClassValue = 135; // 东南(112.5~157.5)为135
        } else if (CompassDirection <= 202.5) {
            ClassValue = 180; // 南(157.5~202.5)为180
        } else if (CompassDirection <= 247.5) {
            ClassValue = 225; // 西南(202.5~247.5)为225
        } else if (CompassDirection <= 292.5) {
            ClassValue = 270; // 西(247.5~292.5)为270
        } else if (CompassDirection <= 337.5) {
            ClassValue = 315; // 西北(292.5~337.5)为315
        } else {
            System.out.println("CompassDirection calculation error!");
            System.exit(0);
        }
        return ClassValue;
    }
}
