package package_for_gridanalysis;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class Interface {
    // 声明并初始化数据库连接配置信息
    public String url = "";
    public String user = "";
    public String password = "";


    // 根据想要文件是否存在进行输入输出，exist=true想要文件存在，否则想要文件不存在
    public static String stringFileInput(String prompt, boolean exist) throws IOException {
        // 输出提示信息
        System.out.print(prompt);
        boolean isContinue = true;
        String fileName = "";
        if (exist) {
            while (isContinue) {
                BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
                fileName = buffer.readLine();
                File file = new File(fileName);
                // 判断文件是否存在
                if (file.exists()) {
                    isContinue = false;
                } else {
                    System.out.print("输入的文件不存在，请重新输入：");
                }
            }
        } else {
            while (isContinue) {
                BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
                fileName = buffer.readLine();
                File file = new File(fileName);
                // 判断文件是否存在
                if (file.exists()) {
                    System.out.print("输入的文件已存在，请重新输入：");
                } else {
                    isContinue = false;
                }
            }
        }
        return fileName;
    }

    public static String input(String prompt) throws IOException {
        // 输出提示信息
        System.out.print(prompt);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        return buffer.readLine();
    }

    // 输入并判断输入是否为整数，若是返回整数，若不是重新输入
    public int intInput() throws IOException {
        boolean isContinue = true;
        String temp = "";
        int result = 0;
        while (isContinue) {
            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            temp = buffer.readLine();
            if (temp.matches("[0-9]+")) {
                result = Integer.parseInt(temp);
                isContinue = false;
            } else {
                System.out.print("输入的不是整数，请重新输入：");
            }
        }
        return result;
    }
    // 检查输入是否越界
    public int intInputIsCross(int low, int up) throws IOException {
        boolean isContinue = true;
        int result = 0;
        while (isContinue) {
            result = intInput();
            if (result >= low && result <= up) {
                isContinue = false;
            } else {
                System.out.print("输入越界，请重新输入：");
            }
        }
        return result;
    }

    //--------------------------降水插值-----------------------------
    private double[][] interpolationProcess(int i, Cell cell, int j) throws IOException, SQLException {
        Interpolation interpolation = new Interpolation(cell);
        Interpolation.Voronoi voronoi = interpolation.new Voronoi();
        Interpolation.IDW idw = interpolation.new IDW();
        Interpolation.Kriging kriging = interpolation.new Kriging();
        Interpolation.Spline spline = interpolation.new Spline();
        Interpolation.TrendSurface trendSurface = interpolation.new TrendSurface();
        Interpolation.NaturalNeighbor naturalNeighbor = interpolation.new NaturalNeighbor();
        Interpolation.RBF rbf = interpolation.new RBF();
        double[][] result = new double[cell.getNrows()][cell.getNcols()];
        System.out.print("请输入拟插值数据的时间序列号（由0开始）：");
        int id = intInputIsCross(0, cell.getRainStation()[0].getRainfall().length - 1);
        System.out.print("请根据计算机配置输入适宜的线程数（由1开始）：");
        int numThreads = intInputIsCross(1, Math.min(cell.getNrows(), cell.getNcols()));
        int distancePower = 0;
        if (i == 2) {
            System.out.print("请输入反距离权重值（正整数）：");
            distancePower = intInputIsCross(1, Integer.MAX_VALUE);
        }
        String ModelClass = "";
        if (i == 3) {
            System.out.print("请输入数字选择半变异函数：1（球状模型）2（指数模型）3（高斯函数）");
            int KrigingModelNum = intInputIsCross(1, 3);
            switch (KrigingModelNum) {
                case (1) -> ModelClass = "Spherical";
                case (2) -> ModelClass = "Exponential";
                case (3) -> ModelClass = "Gaussian";
            }
        }
        int degree = 0;
        if (i == 5) {
            System.out.print("请输入趋势面的阶数（正整数）：");
            degree = intInputIsCross(1, Integer.MAX_VALUE);
        }
        String RBFClass = "";
        double s = 0;
        if (i == 7) {
            System.out.println("请输入数字选择核函数：1（高斯曲面函数）2（多项式函数）3（线性函数）");
            System.out.println("4（分段线性函数）5（立方曲面函数）6（薄板曲面函数）7（Wendland's C2函数）");
            int RBFClassNum = intInputIsCross(1, 7);
            switch (RBFClassNum) {
                case (1) -> RBFClass = "Gaussian";
                case (2) -> RBFClass = "Multiquadrics";
                case (3) -> RBFClass = "Linear";
                case (4) -> RBFClass = "LinearEpsR";
                case (5) -> RBFClass = "Cubic";
                case (6) -> RBFClass = "Thinplate";
                case (7) -> RBFClass = "Wendland";
            }
            if (RBFClassNum == 1 || RBFClassNum == 2 || RBFClassNum == 4) {
                System.out.print("请输入拟合函数的方差：");
                boolean isContinue = true;
                String temp = "";
                while (isContinue) {
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
                    temp = buffer.readLine();
                    if (temp.matches("^[0-9]*\\.?[0-9]*")) {
                        s = Double.parseDouble(temp);
                        if (s == 0) {
                            System.out.print("输入的不是大于0的数字，请重新输入：");
                        } else {
                            isContinue = false;
                        }
                    } else {
                        System.out.print("输入的不是大于0的数字，请重新输入：");
                    }
                }
            }
        }
        long startTime;
        startTime = System.nanoTime();
        switch (i) {
            case (1) -> result = voronoi.Voronoi_interpolation(id, numThreads);
            case (2) -> result = idw.IDW_interpolation(id, numThreads, distancePower);
            case (3) -> result = kriging.Kriging_interpolation(id, numThreads, ModelClass);
            case (4) -> result = spline.Spline_interpolation(id, numThreads);
            case (5) -> result = trendSurface.TrendSurface_interpolation(id, numThreads, degree);
            case (6) -> result = naturalNeighbor.NaturalNeighbor_interpolation(id, numThreads);
            case (7) -> result = rbf.RBF_interpolation(id, numThreads, RBFClass, s);
        }
        if (j == 0) {
            System.out.println("插值花费时间：" + (System.nanoTime() - startTime) / 1e9 + " 秒");
            //结果可视化
            startTime = System.nanoTime();
            double[][] finalResult = result;
            String savePath = "result\\"; // 保存路径
            String resultFileName = stringFileInput("请输入写出的文件名：", false);

            SwingUtilities.invokeLater(() -> {
                Interpolation.InterpolationResultPlot plot = new Interpolation.InterpolationResultPlot(finalResult, "插值结果图", cell.getNodataValue(), 1, 600, numThreads,savePath,resultFileName);
                plot.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            });
            System.out.println("画图花费时间：" + (System.nanoTime() - startTime) / 1e9 + " 秒");

            FileManager.FileReader.FileWriter.writeResult(result, resultFileName);
        }
        return result;
    }

    //--------------------------水文分析-----------------------------
    private void waterAnalysisSubMenu(Cell cell, MySQLOperate sql) throws IOException {
        boolean subMenuContinue = true;
        while (subMenuContinue) {
            System.out.println("=====================================");
            System.out.println("               水文分析               ");
            System.out.println("=====================================");
            System.out.println("| 0. 返回上一级");
            System.out.println("| 1. 坡度计算");
            System.out.println("| 2. 流向计算");
            System.out.println("| 3. 填洼计算");
            System.out.println("| 4. 累积流计算");
            System.out.println("| 5. 山脊线提取");
            System.out.println("| 6. 河网提取计算");
            System.out.println("| 7. 坡向计算");
            System.out.println("=====================================");
            System.out.println("请输入需要计算的功能(0-7)：");
            int subSelection = intInputIsCross(0, 7);
            switch (subSelection) {
                case (0) -> subMenuContinue = false;
                case (1) -> {
                    Slope slopeCalculator = new Slope(cell);
                    try {
                        double[][] slope = slopeCalculator.getSlope(); // 计算坡度
                        if (slope != null) { // 检查是否成功计算出坡度
                            String outputFileName = "slope_output"; // 定义输出文件名
                            int upSampleSize = 8; // 定义上采样大小
                            slopeCalculator.outputResults(outputFileName, upSampleSize); // 输出结果
                            System.out.println("坡度计算和输出完成");
                        } else {
                            System.out.println("坡度计算失败");
                        }
                        //保存结果至数据库
                        sql.insert_Slope(cell, slope);
                    } catch (IOException e) {
                        System.out.println("坡度计算过程中出现错误: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                case (2) -> {
                    double[][] filledDem_dir = FillDem.FillDem_MV(cell, "filled_dem_output", 8); // 调用填洼计算
                    double[][] flowdir = Flowdir.Flowdir(cell, filledDem_dir, "Force", "flowdir_output", 8); // 调用流向计算
                    // 处理计算结果
                    if (flowdir != null) {
                        System.out.println("流向计算和输出完成");
                    } else {
                        System.out.println("流向计算失败");
                    }
                    //保存结果至数据库
                    sql.insert_Flowdir(cell, flowdir);
                }

                case (3) -> {
                    double[][] filledDem = FillDem.FillDem_MV(cell, "filled_dem_output", 8); // 调用填洼计算
                    if (filledDem != null) { // 检查是否成功计算出填洼结果
                        System.out.println("填洼计算和输出完成");
                    } else {
                        System.out.println("填洼计算失败");
                    }
                    //保存结果至数据库
                    sql.insert_DEMFilled(cell, filledDem);

                }
                case (4) -> {
                    double[][] filledDem_acc = FillDem.FillDem_MV(cell, "filled_dem_output", 8); // 调用填洼计算
                    double[][] flowdir = Flowdir.Flowdir(cell, filledDem_acc, "Force", "flowdir_output", 8); // 调用流向计算
                    double[][] accinter = new double[cell.getNrows()][cell.getNcols()];
                    System.out.print("是否使用插值结果作为权重0（否）1（是）：");
                    int Choice  = intInputIsCross(0, 1);
                    boolean useInterpolationAsWeight = (Choice == 0);
                    double[][] acc = Accumulation.acc(cell, flowdir, useInterpolationAsWeight, accinter, "Accumulation", 8);
                    if (acc != null) {
                        System.out.println("累积流计算和输完成");
                    } else {
                        System.out.println("累积流计算失败");
                    }
                    //保存结果至数据库
                    sql.insert_DEMFilled(cell, acc);

                }
                case (5) -> {
                    double[][] filledDem_line = FillDem.FillDem_MV(cell, "filled_dem_output", 8); // 调用填洼计算
                    double[][] flowdir = Flowdir.Flowdir(cell, filledDem_line, "Force", "flowdir_output", 8); // 调用流向计算
                    double[][] ridgeLine = RidgeLine.ridge(cell, flowdir, "ridge_output", 8); // 调用山脊线提取
                    if (ridgeLine != null) {
                        System.out.println("山脊线提取和输出完成");
                    } else {
                        System.out.println("山脊线提取失败");
                    }
                    //保存结果至数据库
                    sql.insert_RidgeLine(cell, ridgeLine);
                }
                case (6) -> {
                    double[][] filledDem_acc = FillDem.FillDem_MV(cell, "filled_dem_output", 8); // 调用填洼计算
                    double[][] flowdir = Flowdir.Flowdir(cell, filledDem_acc, "Force", "flowdir_output", 8); // 调用流向计算
                    double[][] accinter = new double[cell.getNrows()][cell.getNcols()];
                    double[][] acc = Accumulation.acc(cell, flowdir, false, accinter, "Accumulation", 8);
                    double[][] streamNet = StreamNet.StreamNet(cell, acc, 20, "streamnet_output", 8); // 调用河网提取计算
                    if (streamNet != null) {
                        System.out.println("河网提取计算和输出完成");
                    } else {
                        System.out.println("河网提取计算失败");
                    }
                    //保存结果至数据库
                    sql.insert_streamNet(cell, streamNet);
                }
                case (7) -> {
                    double[][] aspectResult = Aspect.Aspect(cell, "aspect_output", 8); // 调用坡向计算
                    if (aspectResult != null) {
                        System.out.println("坡向计算和输出完成");
                    } else {
                        System.out.println("坡向计算失败");
                    }
                    //保存结果至数据库
                    sql.insert_Aspect(cell, aspectResult);
                }
            }
        }
    }

    //-------------------------主菜单------------------------------
    public void cellInterface(Cell cell, MySQLOperate sql) throws IOException, SQLException {
        boolean mainMenuContinue = true;
        int selection = 0;
        while (mainMenuContinue) {
            System.out.println("=====================================");
            System.out.println("           降水分析模型主菜单            ");
            System.out.println("=====================================");
            System.out.println("|  0. 退出程序");
            System.out.println("|  1. 插值计算");
            System.out.println("|  2. 数据库查询");
            System.out.println("|  3. 描述分析");
            System.out.println("|  4. 水文分析");
            System.out.println("=====================================");
            System.out.println("请输入您要进入的板块(0-4)：");
            selection = intInputIsCross(0, 4);

            System.out.println("=====================================");
            switch (selection) {
                //----------退出----------//
                case (0) -> mainMenuContinue = false;
                //------插值计算板块----------//
                case (1) -> {
                    boolean interpolationContinue = true;
                    while (interpolationContinue) {
                        System.out.println("=====================================");
                        System.out.println("               插值计算               ");
                        System.out.println("=====================================");
                        System.out.println("|  0. 返回上一级");
                        System.out.println("|  1. 泰森多边形插值");
                        System.out.println("|  2. 反距离权重插值");
                        System.out.println("|  3. 克里金插值");
                        System.out.println("|  4. 样条函数插值");
                        System.out.println("|  5. 趋势面插值");
                        System.out.println("|  6. 自然领域插值");
                        System.out.println("|  7. 径向基函数插值");
                        System.out.println("=====================================");
                        System.out.println("请输入插值方法(0-7)：");
                        selection = intInputIsCross(0, 7);
                        switch (selection) {
                            case (0) -> interpolationContinue = false;
                            case (1) -> interpolationProcess(1, cell, 0);
                            case (2) -> interpolationProcess(2, cell, 0);
                            case (3) -> interpolationProcess(3, cell, 0);
                            case (4) -> interpolationProcess(4, cell, 0);
                            case (5) -> interpolationProcess(5, cell, 0);
                            case (6) -> interpolationProcess(6, cell, 0);
                            case (7) -> interpolationProcess(7, cell, 0);
                        }
                    }
                }
                //------数据库板块----------//
                case (2) -> {
                    boolean DBContinue = true;
                    while (DBContinue) {
                        System.out.println("=====================================");
                        System.out.println("               数据库查询               ");
                        System.out.println("=====================================");
                        System.out.println("|  0. 返回上一级");
                        System.out.println("|  1. 根据站点、时间查询降雨量");
                        System.out.println("|  2. 根据站点编号查询雨量站");
                        System.out.println("|  3. 根据站点名称统计降雨量");
                        System.out.println("=====================================");
                        System.out.println("请输入查询选择(0-3)：");
                        selection = intInputIsCross(0, 3);
                        switch (selection) {
                            case (0) -> DBContinue = false;
                            case (1) -> {
                                Scanner scanner = new Scanner(System.in);
                                System.out.println("现有雨量站:");
                                sql.PrintStationName();
                                System.out.println("请输入查询站点名称(EnName):");
                                String name_query = scanner.nextLine();
                                System.out.println("数据记录时段:");
                                sql.PrintDuration();
                                System.out.println("请输入查询时刻(eg. 2005-06-25 05:00:00):");
                                String time_query = scanner.nextLine();
                                sql.QueryPrecip(name_query, time_query);
                            }

                            case (2) -> {
                                System.out.println("现有雨量站编号:");
                                sql.PrintSID();
                                Scanner scanner = new Scanner(System.in);
                                System.out.println("请输入查询站点编号(SID)：");
                                String sid_query = scanner.nextLine();
                                sql.QueryGauge(sid_query);
                            }

                            case (3) -> {
                                System.out.println("现有雨量站:");
                                sql.PrintStationName();
                                Scanner scanner = new Scanner(System.in);
                                System.out.println("请输入查询站点名称(enName)：");
                                String name_query = scanner.nextLine();
                                sql.StatisticStation(name_query);
                            }
                        }
                    }
                }
                //------描述分析板块，包括变化检测与预测，统计分析-----//
                case (3) -> {
                    boolean continueOption3 = true;
                    while (continueOption3) {
                        System.out.println("=====================================");
                        System.out.println("               描述分析               ");
                        System.out.println("=====================================");
                        System.out.println("| 0. 返回上一级");
                        System.out.println("| 1. MK变化检测");
                        System.out.println("| 2. 滑动自回归预测");
                        System.out.println("| 3. 统计");
                        System.out.println("=====================================");
                        System.out.println("请输入您的选择(0-3)：");
                        int option3Selection = intInputIsCross(0, 3);
                        switch (option3Selection) {
                            case 0 -> continueOption3 = false;
                            case 1 -> {
                                Mann_Kendall mannKendall = new Mann_Kendall(cell);
                                mannKendall.calculateMannKendallTrendForRainStations();
                            }
                            case 2 -> {
                                boolean ARMAContinue = true;
                                while (ARMAContinue) {
                                    System.out.print("请输入数字选择您要进行的操作：0（返回上一级）1（预测已有数据测试）2（预测未来数据）：");
                                    selection = intInputIsCross(0, 2);
                                    switch (selection) {
                                        case (0) -> ARMAContinue = false;
                                        case (1) -> {
                                            System.out.print("请输入站点编号：");
                                            int stationID = intInputIsCross(0, cell.getRainStationNum() - 1);
                                            double[] data = cell.getRainStation()[stationID].getRainfall();
                                            System.out.print("请输入你要预测的时间：");
                                            int dayNum = intInputIsCross(1, data.length - 5);
                                            double[] dataSlice = BasicCal.slice(data, 0, data.length - dayNum);
                                            ArrayList<Double>[] par_res = ARMA.ARMA_bestAIC(dataSlice);
                                            double[][] result = ARMA.multiPredict(dataSlice, par_res, dayNum);
                                            double[] rainfallPreResult = new double[dayNum];
                                            System.out.println("预测结果为：");
                                            for (int i = 0; i < dayNum; i++) {
                                                rainfallPreResult[i] = result[i][0];
                                                System.out.print(rainfallPreResult[i] + " ");
                                            }
                                            ArrayList<XY>[] lineData = ARMA.makeArray(data, rainfallPreResult, 1);
                                            String[] lineName = new String[]{"observed", "predicted"};
                                            DrawPolyline myDraw = new DrawPolyline(lineData, lineName, "ARMA", "time", "rainfall");
                                            myDraw.draw();
                                            double[] reals = BasicCal.slice(data, data.length - dayNum, data.length);
                                            System.out.printf("平均绝对误差为：%.2f\n", BasicCal.ErrorAnalysis.MAE(reals, rainfallPreResult));
                                        }
                                        case (2) -> {
                                            System.out.print("请输入站点编号：");
                                            int stationID = intInputIsCross(0, cell.getRainStationNum() - 1);
                                            double[] data = cell.getRainStation()[stationID].getRainfall();
                                            System.out.print("请输入你要预测的时间：");
                                            int dayNum = intInputIsCross(1, Integer.MAX_VALUE);
                                            ArrayList<Double>[] par_res = ARMA.ARMA_bestAIC(data);
                                            double[][] result = ARMA.multiPredict(data, par_res, dayNum);
                                            double[] rainfallPreResult = new double[dayNum];
                                            for (int i = 0; i < dayNum; i++) {
                                                rainfallPreResult[i] = result[i][0];
                                            }
                                            ArrayList<XY>[] lineData = ARMA.makeArray(data, rainfallPreResult, 2);
                                            String[] lineName = new String[]{"observed", "predicted"};
                                            DrawPolyline myDraw = new DrawPolyline(lineData, lineName, "ARMA", "time", "rainfall");
                                            myDraw.draw();
                                        }
                                    }
                                }
                            }
                            case 3 -> {
                                boolean statisticsContinue = true;
                                while (statisticsContinue) {
                                    System.out.println("=====================================");
                                    System.out.println("               插值计算               ");
                                    System.out.println("=====================================");
                                    System.out.println("|  0. 返回上一级");
                                    System.out.println("|  1. 泰森多边形插值");
                                    System.out.println("|  2. 反距离权重插值");
                                    System.out.println("|  3. 克里金插值");
                                    System.out.println("|  4. 样条函数插值");
                                    System.out.println("|  5. 趋势面插值");
                                    System.out.println("|  6. 自然领域插值");
                                    System.out.println("|  7. 径向基函数插值");
                                    System.out.println("=====================================");
                                    System.out.println("请输入插值方法(0-7)：");
                                    selection = intInputIsCross(0, 7);
                                    double[][] data;
                                    switch (selection) {
                                        case (0) -> statisticsContinue = false;
                                        case (1) -> {
                                            data = interpolationProcess(1, cell, 1);
                                            StatisticsCalculator.calculateresult(data);
                                        }
                                        case (2) -> {
                                            data = interpolationProcess(2, cell, 1);
                                            StatisticsCalculator.calculateresult(data);
                                        }
                                        case (3) -> {
                                            data = interpolationProcess(3, cell, 1);
                                            StatisticsCalculator.calculateresult(data);
                                        }
                                        case (4) -> {
                                            data = interpolationProcess(4, cell, 1);
                                            StatisticsCalculator.calculateresult(data);
                                        }
                                        case (5) -> {
                                            data = interpolationProcess(5, cell, 1);
                                            StatisticsCalculator.calculateresult(data);
                                        }
                                        case (6) -> {
                                            data = interpolationProcess(6, cell, 1);
                                            StatisticsCalculator.calculateresult(data);
                                        }
                                        case (7) -> {
                                            data = interpolationProcess(7, cell, 1);
                                            StatisticsCalculator.calculateresult(data);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //------水文分析板块-------//
                case (4) -> waterAnalysisSubMenu(cell, sql);
            }
        }
    }

    //-------------------------开始界面---------------------------------
    public void consoleInterface() throws IOException {
        System.out.println("==========================================");
        System.out.println("|                                        |");
        System.out.println("            欢迎使用降雨分析小程序           ");
        System.out.println("|                                        |");
        System.out.println("==========================================");
        boolean projectContinue = true;
        Connection conn;

        //-------------------------read files---------------------------
        String rainStationFile = stringFileInput("请输入雨量站信息文件名称：", true);
        String rainfallFile = stringFileInput("请输入降雨量信息文件名称：", true);
        String cellFile = stringFileInput("请输入网格信息文件名称：", true);
        FileManager.FileReader fileReader = new FileManager.FileReader(rainStationFile, rainfallFile, cellFile);
        Cell cell = fileReader.readData();

        //--------------------------database----------------------------
        url = input("请输入数据库的url：");
        user = input("请输入用户名：");
        password = input("请输入密码：");
        System.out.println("---------------- loading ----------------");
        MySQLOperate DataBase = new MySQLOperate(url, user, password);
        DataBase.createDataBase();
        DataBase.insert_originalData(cell, "./data/gauges.txt","./data/rainfall.txt");

        //--------------------------analysis----------------------------
        try {
            cellInterface(cell, DataBase);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    // 主程序入口
    public static void main(String[] args) throws IOException, SQLException, ParseException, ClassNotFoundException {
        // 创建一个 Interface 类的实例，用于处理用户界面和程序逻辑
        Interface myInterface=new Interface();
        // 调用控制台用户界面方法，启动程序的交互式控制台界面
        myInterface.consoleInterface();
    }
}











