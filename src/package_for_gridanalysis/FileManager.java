package package_for_gridanalysis;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.round;

public class FileManager {

    public static FileReader.FileWriter FileWriter;

    public static class FileReader {
        // 文件读取类的成员和方法
        /**
         * 构造函数，创建一个 FileReader 对象，用于读取包含雨量站、降水量和研究区域信息的文件。
         *
         * @param rainstationFile 包含雨量站信息的文件路径
         * @param rainfallFile    包含序列降水量信息的文件路径
         * @param cellFile        包含研究区域信息的文件路径
         */
        private String rainstationFile;
        private String rainfallFile;
        private String cellFile;
        public FileReader(String rainstationFile, String rainfallFile, String cellFile) {
            this.rainstationFile = rainstationFile;
            this.rainfallFile = rainfallFile;
            this.cellFile = cellFile;
        }
        /**
         * 从文件中读取数据，并返回一个包含研究区域信息的 Cell 对象。
         *
         * @return 包含研究区域信息的 Cell 对象
         */
        public Cell readData() {
            int rainStationNum = 0;
            double[] stationX = null;
            double[] stationY = null;
            double[][] rainfall = null;
            double[] rainflow = null;
            double[][] DEM = null;

            int ncols;
            int nrows;
            double xllcorner;
            double yllcorner;
            double cellsize;
            double nodataValue;

            //读取雨量站信息
            try (BufferedReader stationbufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(rainstationFile), "GBK"))) {
                int lineCount = 0;
                String line = stationbufferedReader.readLine(); // 读取第一行
                lineCount++;
                if (line != null) {
                    try {
                        // 将第一行的内容解析为整数
                        rainStationNum = Integer.parseInt(line.trim());
                        stationX = new double[rainStationNum]; // 初始化站点X坐标数组
                        stationY = new double[rainStationNum]; // 初始化站点Y坐标数组

                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse the first line as an integer: " + e.getMessage());
                    }
                } else {
                    System.err.println("The first line is empty.");
                }
                int rainstation_count = 0;
                while ((line = stationbufferedReader.readLine()) != null) {
                    lineCount++;

                    // 当读到第 3 行时执行以下操作
                    if (lineCount >= 3) {
                        String[] tokens = line.split("\\s+");// 使用正则表达式拆分字符串，考虑多个空格分隔
                        stationX[rainstation_count] = Double.parseDouble(tokens[3]);  // 第四个数
                        stationY[rainstation_count] = Double.parseDouble(tokens[4]);   // 第五个数


                        rainstation_count++;
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            //读取序列降水量信息
            List<String[]> lines = new ArrayList<>();
            try (BufferedReader rainfallbufferedReader = new BufferedReader(new java.io.FileReader(rainfallFile))) {
                String line;
                line = rainfallbufferedReader.readLine(); // 读取第一行
                String[] tokens;
                tokens = line.split("\\s+");

                line = rainfallbufferedReader.readLine();       //读取第二行

                while ((line = rainfallbufferedReader.readLine()) != null) {
                    tokens = line.split("\\s+");  // 使用正则表达式拆分字符串，考虑多个空格分隔
                    lines.add(tokens);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            int rainfall_num = lines.size();

            rainfall = new double[rainStationNum][rainfall_num];

            for (int i = 0; i < rainfall_num; i++) {

                for (int j = 0; j < rainStationNum; j++) {
                    rainfall[j][i] = Double.parseDouble(lines.get(i)[j + 2]);

                }
            }


            RainStation[] rainStations = new RainStation[rainStationNum];
            for (int i = 0; i < rainStationNum; i++) {
                rainStations[i] = new RainStation(stationX[i], stationY[i], rainfall[i]);
            }

            //读取研究区域信息
            List<String> celllist = new ArrayList<>();
            List<String> dems = new ArrayList<>();
            try (BufferedReader cellbufferedReader = new BufferedReader(new java.io.FileReader(cellFile))) {
                String line;
                int linecount = 0;

                while ((line = cellbufferedReader.readLine()) != null && linecount <= 5) {
                    linecount++;
                    String[] tokens = line.split("\\s+");  // 使用正则表达式拆分字符串，考虑多个空格分隔
                    celllist.add(tokens[1]);
                }
                dems.add(line);
                while ((line = cellbufferedReader.readLine()) != null) {
                    dems.add(line);
                }

                DEM = new double[Integer.parseInt(celllist.get(1))][Integer.parseInt(celllist.get(0))];
                for (int i = 0; i < Integer.parseInt(celllist.get(1)); i++) {
                    String[] tokens = dems.get(i).trim().split("\\s+");
                    for (int j = 0; j < Integer.parseInt(celllist.get(0)); j++) {
                        //System.out.println(i+"行"+j+"列");
                        DEM[i][j] = Double.parseDouble(tokens[j]);
                    }
                }

            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            //数据赋值

            int distancePower = 2;
            ncols = Integer.parseInt(celllist.get(0));
            nrows = Integer.parseInt(celllist.get(1));
            xllcorner = Double.parseDouble(celllist.get(2));
            yllcorner = Double.parseDouble(celllist.get(3));
            cellsize = Double.parseDouble(celllist.get(4));
            nodataValue = Double.parseDouble(celllist.get(5));

            //返回新的cell
            return new Cell(ncols, nrows, xllcorner, yllcorner, cellsize, nodataValue, rainStationNum, rainStations, DEM);
        }

        public static class FileWriter {
            FileWriter() {}
            public static String Path = "result"; // 输出数据存放的路径
            public static void writeResult(double[][] interpolatedData, String outputFilePath) {
                try {
                    outputFilePath = Path+"\\"+outputFilePath;
                    // 创建文件写入器
                    BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(outputFilePath, false));

                    // 遍历插值结果矩阵，将数据写入文件
                    for (int i = 0; i < interpolatedData.length; i++) {
                        for (int j = 0; j < interpolatedData[i].length; j++) {
                            String formattedNumber = String.format("%.2f", interpolatedData[i][j]);
                            writer.write(formattedNumber);
                            if (j < interpolatedData[i].length - 1) {
                                writer.write("\t"); // 使用制表符分隔数据
                            }
                        }
                        writer.write("\n"); // 换行
                    }

                    // 关闭文件写入器
                    writer.close();

                    System.out.println("插值结果已成功写入文件：" + outputFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public static void WriteASC(Cell MyDem, double[][] OutputData, String OutputFilename) {
        /*
        WriteASC()函数将计算结果数据输出成".asc"格式
        参数解释：
        MyDem为原始栅格数据对象;
        OutputData为计算结果;
        OutputFilename为输出数据的文件名
        */
                try {
                    String OutputFilename_suffix = OutputFilename + ".asc";  // 补充文件名后缀
                    File OutputFile = new File(Path, OutputFilename_suffix);
                    if (!OutputFile.exists()) {      // 如果该文件不存在，则创建新文件
                        OutputFile.createNewFile();
                    } else {                         // 如果该文件存在，则删除原文件后创建新文件
                        OutputFile.delete();
                        OutputFile.createNewFile();
                    }
                    FileOutputStream OutputStream = new FileOutputStream(OutputFile); // 初始化文件输出流
                    String stemp; // 临时字符串 用于存储写入的数据
                    // 写入头文件信息
                    stemp = "nrows         " + MyDem.getNrows() + "\n";
                    OutputStream.write(stemp.getBytes());
                    stemp = "ncols         " + MyDem.getNcols() + "\n";
                    OutputStream.write(stemp.getBytes());
                    stemp = "xllcorner     " + MyDem.getXllcorner() + "\n";
                    OutputStream.write(stemp.getBytes());
                    stemp = "yllcorner     " + MyDem.getYllcorner() + "\n";
                    OutputStream.write(stemp.getBytes());
                    stemp = "cellsize      " + MyDem.getCellsize() + "\n";
                    OutputStream.write(stemp.getBytes());
                    stemp = "NODATA_value  " + MyDem.getNodataValue() + "\n";
                    OutputStream.write(stemp.getBytes());
                    // 写入栅格值
                    for (int i = 0; i < MyDem.getNrows(); i++) {
                        for (int j = 0; j < MyDem.getNcols(); j++) {
                            stemp = String.valueOf(OutputData[i][j]);
                            stemp = stemp + " ";
                            OutputStream.write(stemp.getBytes());
                        }
                        stemp = "\n";
                        OutputStream.write(stemp.getBytes());
                    }
                    OutputStream.close(); // 关闭文件输出流
                } catch (Exception e) {   // 异常捕获
                    e.printStackTrace();
                }
            }

            public static void WriteJPEG(Cell MyDem, double[][] OutputData, String OutputFilename, int UpSampleSize, float quality) {
                /** WriteJPEG()函数将计算结果数据输出成".jpg"格式
                 参数解释：
                 MyDem为原始栅格数据对象;
                 OutputData为计算结果;
                 OutputFilename为输出数据的文件名;
                 UpSampleSize为上采样倍数;
                 quality为图片压缩程度
                 */
                try {
                    String OutputFilename_suffix = OutputFilename + ".jpg";
                    File OutputFileImg = new File(Path, OutputFilename_suffix);
                    if (!OutputFileImg.exists()) {      // 如果该文件不存在，则创建新文件
                        OutputFileImg.createNewFile();
                    } else {                            // 如果该文件存在，则删除原文件后创建新文件
                        OutputFileImg.delete();
                        OutputFileImg.createNewFile();
                    }
                    // 将计算结果从二维数组转为一维数组
                    double[] ImageArray = new double[MyDem.getNrows() * MyDem.getNcols()];
                    for (int i = 0; i < MyDem.getNrows(); i++) {
                        for (int j = 0; j < MyDem.getNcols(); j++) {
                            ImageArray[i * MyDem.getNcols() + j] = OutputData[i][j];
                        }
                    }
                    // 采用"百分比截断"的方式对图像进行拉伸
                    // 将一维数组形式的计算结果转为Integer型列表，便于进行排序等计算
                    List<Double> ImageList = Arrays.stream(ImageArray).boxed().collect(Collectors.toList());
                    Collections.sort(ImageList);                           // 列表排序
                    ImageList.removeIf(s -> s.equals((double) MyDem.getNodataValue())); // 删除NODATA值元素
                    double MinPercent = 0.01;   // 设置百分比下限
                    double MaxPercent = 0.99;   // 设置百分比上限
                    int MinIndex = (int) Math.round((ImageList.size() * MinPercent));
                    int MaxIndex = (int) Math.round((ImageList.size() * MaxPercent));
                    double MinCutBoundary = ImageList.get(MinIndex); // 百分比截断下边界数
                    double MaxCutBoundary = ImageList.get(MaxIndex); // 百分比截断上边界数

                    for (int i = 0; i < ImageArray.length; i++) {  // 对图像进行百分比截断
                        if (ImageArray[i] == MyDem.getNodataValue()) { // NODATA值拉伸为0
                            ImageArray[i] = 0;
                        } else if (ImageArray[i] <= MinCutBoundary) {
                            ImageArray[i] = 0;
                        } else if (ImageArray[i] >= MaxCutBoundary) {
                            ImageArray[i] = 255;
                        } else {     // 有效值范围拉伸值0~255
                            // dtemp = (Double.valueOf(ImageArray[i] - MinCutBoundary) / Double.valueOf(MaxCutBoundart - MinCutBoundary)) * 255;
                            // ImageArray[i] = (int) dtemp;
                            ImageArray[i] = ((ImageArray[i] - MinCutBoundary) / (MaxCutBoundary - MinCutBoundary)) * 255;
                        }
                    }

                    // 直接将二维数组存储为".jpg"格式易出现由像素过少导致的图片模糊问题
                    // 此处使用近似"上采样"的方式扩大输出数据的维度, 例如将1个栅格扩充至4个栅格，即整体行数*2、列数*2，图像扩大4倍，内容形状保持不变
                    // UpSampleSize为上采样的倍数
                    double OutputData_UpSample[][] = new double[MyDem.getNrows() * UpSampleSize][MyDem.getNcols() * UpSampleSize];
                    for (int i = 0; i < MyDem.getNrows(); i++) {
                        for (int j = 0; j < MyDem.getNcols(); j++) {
                            for (int k = 0; k < UpSampleSize; k++) {
                                for (int m = 0; m < UpSampleSize; m++) {
                                    OutputData_UpSample[i * UpSampleSize][j * UpSampleSize] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize + k][j * UpSampleSize + m] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize + k][j * UpSampleSize] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize + k][j * UpSampleSize + k] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize][j * UpSampleSize + k] = ImageArray[i * MyDem.getNcols() + j];
                                }
                            }
                        }
                    }
                    // 将输出数据转为一维数组
                    double ImageArrayUpsample[] = new double[MyDem.getNrows() * UpSampleSize * MyDem.getNcols() * UpSampleSize];
                    for (int i = 0; i < MyDem.getNrows() * UpSampleSize; i++) {
                        for (int j = 0; j < MyDem.getNcols() * UpSampleSize; j++) {
                            ImageArrayUpsample[i * MyDem.getNcols() * UpSampleSize + j] = OutputData_UpSample[i][j];
                        }
                    }
                    // 使用BufferdImage和ImgaeIO对图片进行操作
                    BufferedImage Image = new BufferedImage(MyDem.getNcols() * UpSampleSize, MyDem.getNrows() * UpSampleSize,
                            BufferedImage.TYPE_BYTE_GRAY);      // 初始化BufferedImage对象
                    WritableRaster raster = Image.getRaster();  // 通过WritableRaster初始化图片
                    raster.setSamples(0, 0, MyDem.getNcols() * UpSampleSize, MyDem.getNrows() * UpSampleSize, 0, ImageArrayUpsample);
                    // 指定写图片的方式为 jpg
                    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                    // 先指定Output，才能调用writer.write方法
                    ImageOutputStream ios = ImageIO.createImageOutputStream(OutputFileImg);
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);// 指定压缩方式为MODE_EXPLICIT
                        param.setCompressionQuality(quality);// 参数quality为压缩程度，取值范围0~1(浮点数)，quality=1f则不压缩
                    }
                    // 调用write方法，向输入流写图片
                    writer.write(null, new IIOImage(Image, null, null), param);
                } catch (Exception e) {   // 异常捕获
                    e.printStackTrace();
                }
            }

            public static void WriteColorJPEG(Cell MyDem, double[][] OutputData, String OutputFilename, int UpSampleSize, float quality, String function) {
                /** WriteColorJPEG()函数将计算结果数据输出成彩色".jpg"格式
                 参数解释：
                 MyDem为原始栅格数据对象;
                 OutputData为计算结果;
                 OutputFilename为输出数据的文件名;
                 UpSampleSize为上采样倍数;
                 quality为图片压缩程度;
                 function为功能名称
                 */
                try {
                    String OutputFilename_suffix = OutputFilename + ".jpg";
                    File OutputFileImg = new File(Path, OutputFilename_suffix);
                    if (!OutputFileImg.exists()) {      // 如果该文件不存在，则创建新文件
                        OutputFileImg.createNewFile();
                    } else {                            // 如果该文件存在，则删除原文件后创建新文件
                        OutputFileImg.delete();
                        OutputFileImg.createNewFile();
                    }
                    // 将计算结果从二维数组转为一维数组
                    double[] ImageArray = new double[MyDem.getNrows() * MyDem.getNcols()];
                    for (int i = 0; i < MyDem.getNrows(); i++) {
                        for (int j = 0; j < MyDem.getNcols(); j++) {
                            ImageArray[i * MyDem.getNcols() + j] = OutputData[i][j];
                        }
                    }

                    if (function.equals("FillDem") || function.equals("Accumulation") || function.equals("Kriging") || function.equals("package_for_gridanalysis.Slope")) {
                        // 如果是填洼/累积流/插值/坡度结果，采用"百分比截断"的方式对图像进行拉伸
                        // 将一维数组形式的计算结果转为Integer型列表，便于进行排序等计算
                        List<Double> ImageList = Arrays.stream(ImageArray).boxed().collect(Collectors.toList());
                        Collections.sort(ImageList); // 列表排序
                        ImageList.removeIf(s -> s.equals((double) MyDem.getNodataValue())); // 删除NODATA值元素
                        double MinPercent = 0.01;   // 设置百分比下限
                        double MaxPercent = 0.99;   // 设置百分比上限
                        int MinIndex = (int) Math.round((ImageList.size() * MinPercent));
                        int MaxIndex = (int) Math.round((ImageList.size() * MaxPercent));
                        double MinCutBoundary = ImageList.get(MinIndex); // 百分比截断下边界数
                        double MaxCutBoundary = ImageList.get(MaxIndex); // 百分比截断上边界数
                        for (int i = 0; i < ImageArray.length; i++) {  // 对图像进行百分比截断
                            if (ImageArray[i] == MyDem.getNodataValue()) { // NODATA值拉伸为0
                                ImageArray[i] = MyDem.getNodataValue();
                            } else if (ImageArray[i] <= MinCutBoundary) {
                                ImageArray[i] = 0;
                            } else if (ImageArray[i] >= MaxCutBoundary) {
                                ImageArray[i] = 255;
                            } else {     // 有效值范围拉伸值0~255
                                ImageArray[i] = ((ImageArray[i] - MinCutBoundary) / (MaxCutBoundary - MinCutBoundary)) * 255;
                            }
                        }
                    }
                    // 直接将二维数组存储为".jpg"格式易出现由像素过少导致的图片模糊问题
                    // 此处使用近似"上采样"的方式扩大输出数据的维度, 例如将1个栅格扩充至4个栅格，即整体行数*2、列数*2，图像扩大4倍，内容形状保持不变
                    // UpSampleSize为上采样的倍数
                    double OutputData_UpSample[][] = new double[MyDem.getNrows() * UpSampleSize][MyDem.getNcols() * UpSampleSize];
                    for (int i = 0; i < MyDem.getNrows(); i++) {
                        for (int j = 0; j < MyDem.getNcols(); j++) {
                            for (int k = 0; k < UpSampleSize; k++) {
                                for (int m = 0; m < UpSampleSize; m++) {
                                    OutputData_UpSample[i * UpSampleSize][j * UpSampleSize] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize + k][j * UpSampleSize + m] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize + k][j * UpSampleSize] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize + k][j * UpSampleSize + k] = ImageArray[i * MyDem.getNcols() + j];
                                    OutputData_UpSample[i * UpSampleSize][j * UpSampleSize + k] = ImageArray[i * MyDem.getNcols() + j];
                                }
                            }
                        }
                    }
                    // 将输出数据转为一维数组
                    double ImageArrayUpsample[] = new double[MyDem.getNrows() * UpSampleSize * MyDem.getNcols() * UpSampleSize];
                    for (int i = 0; i < MyDem.getNrows() * UpSampleSize; i++) {
                        for (int j = 0; j < MyDem.getNcols() * UpSampleSize; j++) {
                            ImageArrayUpsample[i * MyDem.getNcols() * UpSampleSize + j] = OutputData_UpSample[i][j];
                        }
                    }
                    // 使用BufferdImage和ImgaeIO对图片进行操作
                    BufferedImage Image = new BufferedImage(MyDem.getNcols() * UpSampleSize, MyDem.getNrows() * UpSampleSize,
                            BufferedImage.TYPE_INT_RGB);      // 初始化BufferedImage对象
                    WritableRaster raster = Image.getRaster();  // 通过WritableRaster初始化图片
                    raster.setSamples(0, 0, MyDem.getNcols() * UpSampleSize, MyDem.getNrows() * UpSampleSize, 0, ImageArrayUpsample);
                    int[] ColorValue = {0x8000000F, 0x808080, 0xFF0000, 0xFFA500, 0xFFFF00, 0x008000, 0x00FFFF, 0x1E90FF, 0x0000FF, 0xFF00FF};
                    if (function.equals("Flowdir")) {  // 每个流向对应一种颜色
                        for (int x = 0; x < MyDem.getNcols() * UpSampleSize; x++) {
                            for (int y = 0; y < MyDem.getNrows() * UpSampleSize; y++) {
                                if (OutputData_UpSample[y][x] == MyDem.getNodataValue()) {
                                    Image.setRGB(x, y, ColorValue[0]);
                                } else if (OutputData_UpSample[y][x] == 1) {
                                    Image.setRGB(x, y, ColorValue[2]);
                                } else if (OutputData_UpSample[y][x] == 2) {
                                    Image.setRGB(x, y, ColorValue[3]);
                                } else if (OutputData_UpSample[y][x] == 4) {
                                    Image.setRGB(x, y, ColorValue[4]);
                                } else if (OutputData_UpSample[y][x] == 8) {
                                    Image.setRGB(x, y, ColorValue[5]);
                                } else if (OutputData_UpSample[y][x] == 16) {
                                    Image.setRGB(x, y, ColorValue[6]);
                                } else if (OutputData_UpSample[y][x] == 32) {
                                    Image.setRGB(x, y, ColorValue[7]);
                                } else if (OutputData_UpSample[y][x] == 64) {
                                    Image.setRGB(x, y, ColorValue[8]);
                                } else if (OutputData_UpSample[y][x] == 128) {
                                    Image.setRGB(x, y, ColorValue[9]);
                                } else {
                                    // System.out.println("FlowDirection calculation error!");
                                    // System.exit(0);
                                }
                            }
                        }
                    }
                    if (function.equals("Aspect")) {
                        // 将罗盘方向值再分为9类：北、东北、东、东南、南、西南、西、西北、平面
                        // 为坡向设置颜色，每一类（每个方向）对应一种颜色
                        for (int x = 0; x < MyDem.getNcols() * UpSampleSize; x++) {
                            for (int y = 0; y < MyDem.getNrows() * UpSampleSize; y++) {
                                if (OutputData_UpSample[y][x] == MyDem.getNodataValue()) {
                                    Image.setRGB(x, y, ColorValue[0]);
                                } else if (OutputData_UpSample[y][x] == -1) {
                                    Image.setRGB(x, y, ColorValue[1]);
                                } else if (OutputData_UpSample[y][x] <= 22.5 || OutputData_UpSample[y][x] > 337.5) {
                                    Image.setRGB(x, y, ColorValue[2]);
                                } else if (OutputData_UpSample[y][x] <= 67.5) {
                                    Image.setRGB(x, y, ColorValue[3]);
                                } else if (OutputData_UpSample[y][x] <= 112.5) {
                                    Image.setRGB(x, y, ColorValue[4]);
                                } else if (OutputData_UpSample[y][x] <= 157.5) {
                                    Image.setRGB(x, y, ColorValue[5]);
                                } else if (OutputData_UpSample[y][x] <= 202.5) {
                                    Image.setRGB(x, y, ColorValue[6]);
                                } else if (OutputData_UpSample[y][x] <= 247.5) {
                                    Image.setRGB(x, y, ColorValue[7]);
                                } else if (OutputData_UpSample[y][x] <= 292.5) {
                                    Image.setRGB(x, y, ColorValue[8]);
                                } else if (OutputData_UpSample[y][x] <= 337.5) {
                                    Image.setRGB(x, y, ColorValue[9]);
                                } else {
                                    System.out.println("CompassDirection calculation error!");
                                    System.exit(0);
                                }
                            }
                        }
                    }
                    if (function.equals("FillDem") || function.equals("package_for_gridanalysis.Slope")) {  //填洼或坡度计算
                        for (int x = 0; x < MyDem.getNcols() * UpSampleSize; x++) {
                            for (int y = 0; y < MyDem.getNrows() * UpSampleSize; y++) {
                                if (OutputData_UpSample[y][x] == MyDem.getNodataValue()) {
                                    Image.setRGB(x, y, ColorValue[0]);
                                } else {
                                    int tmp = (int) round(OutputData_UpSample[y][x]);
                                    if (tmp <= 127) {
                                        Color c = new Color(2 * tmp, 255, 0);
                                        int rgb = c.getRGB();
                                        Image.setRGB(x, y, rgb);
                                    } else {
                                        Color c = new Color(255, 255 - (tmp - 128) * 2, 0);
                                        int rgb = c.getRGB();
                                        Image.setRGB(x, y, rgb);
                                    }
                                }
                            }
                        }
                    }
                    if (function.equals("Accumulation")) {  //累积流
                        for (int x = 0; x < MyDem.getNcols() * UpSampleSize; x++) {
                            for (int y = 0; y < MyDem.getNrows() * UpSampleSize; y++) {
                                if (OutputData_UpSample[y][x] == MyDem.getNodataValue()) {
                                    Image.setRGB(x, y, ColorValue[0]);
                                } else {
                                    int tmp = (int) round(OutputData_UpSample[y][x]);
                                    Color c = new Color(255 - tmp, 255 - tmp, 255);
                                    int rgb = c.getRGB();
                                    Image.setRGB(x, y, rgb);
                                }
                            }
                        }
                    }
                    if (function.equals("Kriging")) {  //克里金降雨插值
                        for (int x = 0; x < MyDem.getNcols() * UpSampleSize; x++) {
                            for (int y = 0; y < MyDem.getNrows() * UpSampleSize; y++) {
                                if (OutputData_UpSample[y][x] == MyDem.getNodataValue()) {
                                    Image.setRGB(x, y, ColorValue[0]);
                                } else {
                                    int tmp = (int) round(OutputData_UpSample[y][x]);
                                    Color c = new Color(0, 255 - tmp, 255);
                                    int rgb = c.getRGB();
                                    Image.setRGB(x, y, rgb);
                                }
                            }
                        }
                    }
                    // 指定写图片的方式为 jpg
                    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                    // 先指定Output，才能调用writer.write方法
                    ImageOutputStream ios = ImageIO.createImageOutputStream(OutputFileImg);
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);// 指定压缩方式为MODE_EXPLICIT
                        param.setCompressionQuality(quality);// 参数quality为压缩程度，取值范围0~1(浮点数)，quality=1f则不压缩
                    }
                    // 调用write方法，向输入流写图片
                    writer.write(null, new IIOImage(Image, null, null), param);
                } catch (Exception e) {   // 异常捕获
                    e.printStackTrace();
                }
            }

        }
    }
}
