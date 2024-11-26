package package_for_gridanalysis;

import Jama.Matrix;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.special.BesselJ;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Interpolation {
    private static Cell cell;
    // 构造函数
    public Interpolation(Cell cell){
        Interpolation.cell =cell;
    }
    // 求栅格中心点坐标
    private static XY[][] getIsolateCell(){
        XY[][] isolateCell=new XY[cell.getNrows()][cell.getNcols()];
        for(int i=0;i<cell.getNrows();i++) {
            for (int j = 0; j < cell.getNcols(); j++) {
                isolateCell[i][j]=new XY(cell.getXllcorner()+cell.getCellsize()*(j+0.5), cell.getYllcorner()+cell.getCellsize()*(cell.getNrows()-i-0.5));
            }
        }
        return isolateCell;
    }
    //插值结果绘图
    public static class InterpolationResultPlot extends JFrame {
        private BufferedImage image;
        private double[][] interpolationResult;
        private double[][] resizedResult;
        private double nodataValue;
        private double minValue;
        private  double maxValue;
        private int width;
        private int height;
        private  String savePath;
        private  String imageFileName;
        public InterpolationResultPlot(double[][] interpolationResult, String title, double nodataValue, int change, int widthset, int numThreads,String savePath,String imageFileName ) {
            this.interpolationResult = interpolationResult;
            this.nodataValue = nodataValue;
            this.savePath = savePath; // 存储保存路径
            this.imageFileName = imageFileName; // 存储图像文件名
            minValue = Double.MAX_VALUE;
            maxValue = -Double.MAX_VALUE;
            width = interpolationResult[0].length;
            height = interpolationResult.length;
            resizedResult = null;
            if(change == 1){
                // 设置新的尺寸
                int newWidth = widthset; // 设置新的宽度
                double scale =  (double)newWidth / width;
                int newHeight = (int)(scale * height); // 根据新宽度计算新高度
                resizedResult = new double[newHeight][newWidth];
                for (int newY = 0; newY < newHeight; newY++) {
                    for (int newX = 0; newX < newWidth; newX++) {
                        // 计算在原始矩阵中的坐标
                        int originalX = (int) (newX / scale);
                        int originalY = (int) (newY / scale);

                        // 将原始矩阵中的值填充到缩放后的矩阵
                        resizedResult[newY][newX] = interpolationResult[originalY][originalX];
                    }
                }
                width = newWidth;
                height = newHeight;
            }
            else {double[][] resizedResult = interpolationResult;}
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures = new Future[numThreads];

            // 计算每个线程的任务大小
            int taskSize = height / numThreads;
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startRow = threadNum * taskSize;
                final int endRow = (threadNum == numThreads - 1) ? height : (threadNum + 1) * taskSize;

                // 提交线程任务
                int finalThreadNum = threadNum;
                futures[threadNum] = executor.submit(() -> {
                    for (int i = startRow; i < endRow; i++) {
                        for (int j = 0; j < width; j++) {
                            double value = resizedResult[i][j];
                            if(resizedResult[i][j] == nodataValue){
                                continue;
                            }
                            minValue = Math.min(minValue, resizedResult[i][j]);
                            maxValue = Math.max(maxValue, resizedResult[i][j]);
                        }
                    }
                });
            }

            // 关闭线程池
            executor.shutdown();

            try {
                // 等待所有线程完成
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 创建线程池
            ExecutorService executor1 = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures1 = new Future[numThreads];

            // 计算每个线程的任务大小
            int taskSize1 = height / numThreads;
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startRow = threadNum * taskSize;
                final int endRow = (threadNum == numThreads - 1) ? height : (threadNum + 1) * taskSize;

                // 提交线程任务
                int finalThreadNum = threadNum;
                futures1[threadNum] = executor1.submit(() -> {
                    for (int i = startRow; i < endRow; i++) {
                        for (int j = 0; j < width; j++) {
                            double value = resizedResult[i][j];
                            if(value == nodataValue){
                                image.setRGB(j, i, Color.black.getRGB());
                                continue;
                            }
                            float hue = (float) (0.02 + 0.66 * (value - minValue) / (maxValue - minValue));
                            Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
                            image.setRGB(j, i, color.getRGB());
                        }
                    }
                });
            }

            // 关闭线程池
            executor1.shutdown();

            try {
                // 等待所有线程完成
                for (Future<?> future : futures1) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            setPreferredSize(new Dimension(width + 20, height + 80));
            pack();
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setTitle(title);
            setVisible(true);
        }

        @Override
        public void paint(Graphics g) {
            // 设置背景为黑色
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            // 绘制插值结果
            g.drawImage(image, 0, 0, this);

            // 计算插值结果图的高度
            int resultHeight = height;

            // 绘制图例
            drawLegend(g, resultHeight, minValue, maxValue);

            g.setColor(Color.WHITE);
            g.drawString("插值结果", 10, 20); // 添加标题标签

            try {
                String imagePath = savePath + imageFileName + ".jpg"; // 指定图像文件路径
                File outputFile = new File(imagePath); // 创建文件对象
                ImageIO.write(image, "jpg", outputFile); // 将图像保存为JPEG格式（可以根据需要选择格式）
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void drawLegend(Graphics g, int resultHeight, double minValue, double maxValue) {
            // 在插值结果图的下方绘制图例
            int legendX = 20; // X 起始坐标
            int legendY = resultHeight + 30; // Y 起始坐标
            int legendWidth = width - 20; // 图例宽度
            int legendHeight = 20; // 图例高度

            g.setColor(Color.BLACK);
            g.fillRect(legendX, legendY, legendWidth, legendHeight);
            int num = width-20;
            int legendStep = 1;
            for (int i = 0; i < num; i++) {
                float hue = (float) (0.02 + 0.66 * i / num);
                Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
                g.setColor(color);
                g.fillRect(legendX + i * legendStep, legendY, legendStep, legendHeight);
            }

            g.setColor(Color.WHITE);
            g.drawString("Legend", legendX, legendY - 5); // 图例标题
            g.setColor(Color.WHITE);
            g.drawLine(legendX, legendY, legendX + legendWidth, legendY);
            for (int i = 0; i <= 5; i++) {
                double value = minValue + i * (maxValue - minValue) / 5;
                String label = String.format("%.0f", value); // 格式化数值
                int labelX = legendX + i * (legendWidth-10) / 5 - 5; // 居中显示标签
                int labelY = legendY + legendHeight + 15; // 在颜色框下方显示标签
                g.drawString(label, labelX, labelY);
            }
        }
    }
    // 反距离权重插值
    public class IDW {
        //求距离的负幂次方
        private double weight(double x1, double y1, double x2, double y2, int distancePower) {
            return Math.pow(BasicCal.getDistance(x1,y1,x2,y2),-distancePower);
        }
        public double[][] IDW_interpolation(int ID, int numThreads, int distancePower) {
            XY[][] isolateCell=getIsolateCell();
            double[][] result = new double[cell.getNrows()][cell.getNcols()];  // 插值结果矩阵
            double[][] weight = new double[numThreads][cell.getRainStationNum()];

            // 初始化插值结果为nodata值
            for (int i = 0; i < cell.getNrows(); i++) {
                Arrays.fill(result[i], cell.getNodataValue());
            }

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures = new Future[numThreads];

            // 计算每个线程的任务大小
            int taskSize = cell.getNrows() / numThreads;
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startRow = threadNum * taskSize;
                final int endRow = (threadNum == numThreads - 1) ? cell.getNrows() : (threadNum + 1) * taskSize;

                // 提交线程任务
                int finalThreadNum = threadNum;
                futures[threadNum] = executor.submit(() -> {
                    // 针对每个子任务，执行插值计算
                    for (int i = startRow; i < endRow; i++) {
                        for (int j = 0; j < cell.getNcols(); j++) {
                            double weightSum = 0;
                            double preResult = 0;
                            for (int k = 0; k < cell.getRainStationNum(); k++) {
                                if (isolateCell[i][j].getX() == cell.getRainStation()[k].getStationX() && isolateCell[i][j].getY() == cell.getRainStation()[k].getStationY()) {
                                    // 如果当前坐标匹配到雨量站坐标，直接使用雨量站数据
                                    result[i][j] = cell.getRainStation()[k].getRainfall()[ID];
                                    break;
                                }
                                weight[finalThreadNum][k] = weight(isolateCell[i][j].getX(), isolateCell[i][j].getY(), cell.getRainStation()[k].getStationX(), cell.getRainStation()[k].getStationY(), distancePower);
                                preResult += weight[finalThreadNum][k] * cell.getRainStation()[k].getRainfall()[ID];
                                weightSum += weight[finalThreadNum][k];
                            }
                            if (weightSum != 0) {
                                result[i][j] = preResult / weightSum;
                            }
                        }
                    }
                });
            }

            // 关闭线程池
            executor.shutdown();

            try {
                // 等待所有线程完成
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 处理DEM中的nodata值
            for (int i = 0; i < cell.getNrows(); i++) {
                for (int j = 0; j < cell.getNcols(); j++) {
                    if (cell.getDEM()[i][j] == cell.getNodataValue()) {
                        result[i][j] = cell.getNodataValue();
                    }
                }
            }

            return result;
        }
    }
    // 泰森多边形插值
    public class Voronoi {
        public double[][] Voronoi_interpolation(int ID, int numThreads) {
            XY[][] isolateCell=getIsolateCell();
            double[][] result = new double[cell.getNrows()][cell.getNcols()];  // 插值结果矩阵
            // 初始化插值结果为nodata值
            for (int i = 0; i < cell.getNrows(); i++) {
                Arrays.fill(result[i], cell.getNodataValue());
            }

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures = new Future[numThreads];

            // 计算每个线程的任务大小
            int taskSize = cell.getNrows() / numThreads;
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startRow = threadNum * taskSize;
                final int endRow = (threadNum == numThreads - 1) ? cell.getNrows() : (threadNum + 1) * taskSize;

                // 提交线程任务
                int finalThreadNum = threadNum;
                futures[threadNum] = executor.submit(() -> {
                    for (int i = startRow; i < endRow; i++) {
                        for (int j = 0; j < cell.getNcols(); j++) {
                            double minDistance = 9999999;
                            for (int k = 0; k < cell.getRainStationNum(); k++) {
                                double distance = BasicCal.getDistance(isolateCell[i][j].getX(), isolateCell[i][j].getY(), cell.getRainStation()[k].getStationX(), cell.getRainStation()[k].getStationY());
                                if (distance < minDistance) {
                                    minDistance = distance;
                                    result[i][j] = cell.getRainStation()[k].getRainfall()[ID];
                                }
                            }
                        }
                    }
                });
            }

            // 关闭线程池
            executor.shutdown();

            try {
                // 等待所有线程完成
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < cell.getNrows(); i++) {
                for (int j = 0; j < cell.getNcols(); j++) {
                    if (cell.getDEM()[i][j] == cell.getNodataValue()) {
                        result[i][j] = cell.getNodataValue();
                    }
                }
            }

            return result;
        }
    }
    // 趋势面插值
    public class TrendSurface {
        public double[][] TrendSurface_interpolation(int ID, int numThreads,int degree) {
            XY[][] isolateCell = getIsolateCell();
            double[][] result = new double[cell.getNrows()][cell.getNcols()]; // 插值结果矩阵
            // 初始化插值结果为nodata值
            for (int i = 0; i < cell.getNrows(); i++) {
                Arrays.fill(result[i], cell.getNodataValue());
            }

            int n = (degree + 1) * (degree + 2) / 2; // 特征数量，一个和阶数有关的方程。


            // 添加截距项
            double[][] X_with_intercept = new double[cell.getRainStationNum()][n];
            int idx = 0;
            for (int i = 0; i <= degree; i++) {
                for (int j = 0; j <= i; j++) {
                    for (int k = 0; k < cell.getRainStationNum(); k++) {
                        X_with_intercept[k][idx] = Math.pow(cell.getRainStation()[k].getStationX(), i - j) * Math.pow(cell.getRainStation()[k].getStationY(), j);
                    }
                    idx++;
                }
            }

            // 计算X的转置和X^T * X
            double[][] X_transpose = new double[n][cell.getRainStationNum()];
            for (int l = 0; l < cell.getRainStationNum(); l++) {
                for (int j = 0; j < n; j++) {
                    X_transpose[j][l] = X_with_intercept[l][j];
                }
            }
            double[][] XTX = new double[n][n];
            double[] XTY = new double[n];
            double[] RF = new double[cell.getRainStationNum()];
            for (int i = 0; i < cell.getRainStationNum(); i++) {
                double rainfall = cell.getRainStation()[i].getRainfall()[ID];
                RF[i] = rainfall; // 计算雨量站数据的平均值
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    XTX[i][j] = 0;
                    for (int k = 0; k < cell.getRainStationNum(); k++) {
                        XTX[i][j] += X_transpose[i][k] * X_with_intercept[k][j];
                    }
                }
                XTY[i] = 0;
                for (int k = 0; k < cell.getRainStationNum(); k++) {
                    XTY[i] += RF[k] * X_with_intercept[k][i];
                }
            }
            double[] beta = solve(XTX, XTY, n);

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures = new Future[numThreads];

            // 计算每个线程的任务大小
            int taskSize = cell.getNcols() / numThreads;
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startCol = threadNum * taskSize;
                final int endCol = (threadNum == numThreads - 1) ? cell.getNcols() : (threadNum + 1) * taskSize;

                // 提交线程任务
                int finalThreadNum = threadNum;
                futures[threadNum] = executor.submit(() -> {
                    for (int i = 0; i < cell.getNrows(); i++) {
                        for (int j = startCol; j < endCol; j++) {
                            result[i][j] = beta[0] + beta[1] * isolateCell[i][j].getX() + beta[2] * isolateCell[i][j].getY();
                        }
                    }
                });
            }

            // 关闭线程池
            executor.shutdown();

            try {
                // 等待所有线程完成
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < cell.getNrows(); i++) {
                for (int j = 0; j < cell.getNcols(); j++) {
                    if (cell.getDEM()[i][j] == cell.getNodataValue()) {
                        result[i][j] = cell.getNodataValue();
                    }
                }
            }

            return result;
        }
        //高斯消元解线性方程
        private double[] solve(double[][] A, double[] B, int n) {
            int nn = A.length;
            for (int k = 0; k < nn - 1; k++) {
                for (int i = k + 1; i < nn; i++) {
                    double factor = A[i][k] / A[k][k];
                    B[i] -= factor * B[k];
                    for (int j = k; j < nn; j++) {
                        A[i][j] -= factor * A[k][j];
                    }
                }
            }
            double [] X=new double[n];
            // 回代求解
            X[nn - 1] = B[nn - 1] / A[nn - 1][nn - 1];
            for (int i = nn - 2; i >= 0; i--) {
                double sum = B[i];
                for (int j = i + 1; j < nn; j++) {
                    sum -= A[i][j] * X[j];
                }
                X[i] = sum / A[i][i];
            }

            return X;
        }
    }
    // 克里金插值
    public class Kriging {

        public double[][] Kriging_interpolation(int ID, int numThreads,String ModelClass) {
            XY[][] isolateCell = getIsolateCell();
            double[][] result = new double[cell.getNrows()][cell.getNcols()]; // 插值结果矩阵

            // 计算半变异函数的协方差矩阵
            double[][] covMatrix_StationToStation = new double[cell.getRainStationNum()][cell.getRainStationNum()];
            for (int i = 0; i < cell.getRainStationNum(); i++) {
                for (int j = 0; j < cell.getRainStationNum(); j++) {
                    double distance = BasicCal.getDistance(cell.getRainStation()[i].getStationX(), cell.getRainStation()[i].getStationY(), cell.getRainStation()[j].getStationX(), cell.getRainStation()[j].getStationY());
                    covMatrix_StationToStation[i][j] = chooseModel(ModelClass, distance);
                }
            }
            // 初始化矩阵
            Matrix covMatrixJama1 = new Matrix(covMatrix_StationToStation);
            // 转置矩阵
            Matrix covMatrixTransposed1 = covMatrixJama1.transpose();
            // 计算协方差矩阵的逆矩阵
            Matrix covMatrixInverse1 = covMatrixJama1.inverse();

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures = new Future[numThreads];

            // 计算每个线程的任务大小
            int taskSize = cell.getNcols() / numThreads;
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startCol = threadNum * taskSize;
                final int endCol = (threadNum == numThreads - 1) ? cell.getNcols() : (threadNum + 1) * taskSize;

                // 提交线程任务
                int finalThreadNum = threadNum;
                futures[threadNum] = executor.submit(() -> {
                    // 对每个网格点进行插值
                    for (int i = 0; i < cell.getNrows(); i++) {
                        for (int j = startCol; j < endCol; j++) {
                            boolean isKnownStation = false;
                            double knownValue = 0.0;

                            // 检查当前网格点是否是已知站点
                            for (int k = 0; k < cell.getRainStationNum(); k++) {
                                if (isolateCell[i][j].getX() == cell.getRainStation()[k].getStationX() && isolateCell[i][j].getY() == cell.getRainStation()[k].getStationY()) {
                                    isKnownStation = true;
                                    knownValue = cell.getRainStation()[k].getRainfall()[ID];
                                    break;
                                }
                            }

                            if (isKnownStation) {
                                // 当前网格点对应于已知站点，直接使用已知站点的值
                                result[i][j] = knownValue;
                            } else {
                                // 对其他网格点进行插值计算
                                double[] distanceToStation = new double[cell.getRainStationNum()];
                                for (int k = 0; k < cell.getRainStationNum(); k++) {
                                    distanceToStation[k] = Math.sqrt(
                                            Math.pow(isolateCell[i][j].getX() - cell.getRainStation()[k].getStationX(), 2) +
                                                    Math.pow(isolateCell[i][j].getY() - cell.getRainStation()[k].getStationY(), 2)
                                    );
                                }
                                double[] covMatrix_GridToStation = new double[cell.getRainStationNum()];
                                for (int k = 0; k < cell.getRainStationNum(); k++){
                                    covMatrix_GridToStation[k] = chooseModel(ModelClass,distanceToStation[k]);
                                }
                                // 初始化矩阵
                                Matrix covMatrixJama2 = new Matrix(covMatrix_GridToStation, cell.getRainStationNum());
//                                // 转置矩阵
//                                Matrix covMatrixTransposed2 = covMatrixJama2.transpose();
//                                // 计算协方差矩阵的逆矩阵
//                                Matrix covMatrixInverse2 = covMatrixJama2.inverse();
// 计算权重
                                Matrix weights = covMatrixInverse1.times(covMatrixJama2);

                                for (int k = 0; k < cell.getRainStationNum(); k++) {
                                    result[i][j] += weights.get(k, 0) * (cell.getRainStation()[k].getRainfall()[ID]);
                                }
                            }
                        }
                    }
                });
            }
            // 关闭线程池
            executor.shutdown();

            try {
                // 等待所有线程完成
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < cell.getNrows(); i++) {
                for (int j = 0; j < cell.getNcols(); j++) {
                    if (cell.getDEM()[i][j] == cell.getNodataValue()) {
                        result[i][j] = cell.getNodataValue();
                    }
                }
            }

            return result;
        }

        // 选择半变异函数
        private double chooseModel(String ModelClass, double d) {
            double r = 0;
            switch (ModelClass) {
                case "Spherical" -> r = Spherical(d);
                case "Exponential" -> r = Exponential(d);
                case "Gaussian" -> r = Gaussian(d);
                default -> System.out.println("暂无此函数！");
            }
            return r;
        }

        // 球面模型的半变异函数
        private double Spherical(double d){
            double a = 1.5 * Math.max(cell.getNrows(), cell.getNrows()) * cell.getCellsize();
            double r = 0;
            if (d == 0) {
                r = 0;
            }
            else if (d > 0 && d <= a) {
                r = 1.5 * ((d / a) - (Math.pow(d, 3) / (3 * Math.pow(a, 3))));
            }
            else {
                r = 1;
            }
            return r;
        }
        // 指数模型的半变异函数
        private double Exponential(double d) {
            double a = 1.5 * Math.max(cell.getNrows(), cell.getNrows()) * cell.getCellsize();
            double r = 0;

            if (d == 0) {
                r = 0;
            } else {
                r = 1 - Math.exp(-d / a);
            }
            return r;
        }
        // 高斯模型的半变异函数
        private double Gaussian(double d) {
            double a = 1 * Math.max(cell.getNrows(), cell.getNrows()) * cell.getCellsize();
            double r = 0;

            if (d == 0) {
                r = 0;
            } else {
                r = 1 - Math.exp(-(Math.pow(d, 2) / Math.pow(a, 2)));
            }
            return r;
        }
    }
    //样条插值
    public class Spline {
        static double c = 0.577215;
        static double tauSquared = 0.01;

        public double[][] Spline_interpolation(int ID, int numThreads){
            // 网格参数
            int gridSizeY = cell.getNcols();
            int gridSizeX = cell.getNrows();
            double cellSize =cell.getCellsize(); // 单元大小
            double minX = cell.getXllcorner(); // 左下角坐标
            double minY = cell.getYllcorner();
            RainStation[] rainStation = cell.getRainStation();
            int rainStationNum = cell.getRainStationNum();

            List<RainStation> stations = new ArrayList<>();
            for (int i = 0; i < rainStationNum; i++) {
                double stationX = rainStation[i].getStationX();
                double stationY = rainStation[i].getStationY();
                double[] rainfall = rainStation[i].getRainfall();

                stations.add(new RainStation(stationX, stationY, rainfall));
            }

            // 创建网格
            double[][] grid = new double[gridSizeX][gridSizeY];
            Coefficients coeffs = get_coefficients(stations);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures = new Future[numThreads];

            int taskSize = gridSizeY / numThreads;

            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startCol = threadNum * taskSize;
                final int endCol = (threadNum == numThreads - 1) ? gridSizeY : (threadNum + 1) * taskSize;

                futures[threadNum] = executor.submit(() -> {
                    for (int i = 0; i < gridSizeX; ++i) {
                        double cellCenterY = cell.getYllcorner() + (i + 0.5) * cell.getCellsize();
                        for (int j = startCol; j < endCol; ++j) {
                            double cellCenterX = cell.getXllcorner() + (j + 0.5) * cell.getCellsize();

                            boolean isStation = false;
                            for (RainStation station : stations) {
                                double stationX = station.getStationX();
                                double stationY = station.getStationY();
                                if (Math.abs(cellCenterX - stationX) < 1e-6 && Math.abs(cellCenterY - stationY) < 1e-6) {
                                    isStation = true;
                                    grid[i][j] = station.getRainfall()[ID];
                                    break;
                                }
                            }

                            if (!isStation) {
                                double interpolation = rainfallInterpolation(cellCenterX, cellCenterY, stations, coeffs);
                                grid[i][j] = interpolation;
                            }
                        }
                    }
                });
            }

            executor.shutdown();

            try {
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for(int i=0;i<grid.length;i++){
                for(int j=0;j<grid[0].length;j++){
                    if(cell.getDEM()[i][j]==cell.getNodataValue()){
                        grid[i][j]=cell.getNodataValue();
                    }
                }
            }
            return grid;
        }

        private static double calculateDistance(RainStation station1, RainStation station2) {
            double dx = station1.getStationX() - station2.getStationX();
            double dy = station1.getStationY() - station2.getStationY();
            return Math.sqrt(dx * dx + dy * dy)/1000;
        }

        private static double R_r(double r) {
            if (r == 0) return 0;  // 当距离为0时直接返回0

            double term1 = (r * r) / 4.0 * (Math.log(r / (2.0 * Math.sqrt(tauSquared))) + c - 1);
            double term2 = tauSquared * (BesselJ.value(0, r / Math.sqrt(tauSquared)) + c + Math.log(r / (2.0 * Math.PI)));

            return (1.0 / (2.0 * Math.PI)) * (term1 + term2);
        }

        private static class Coefficients {
            public double[] ai; // 存储 a1, a2, a3
            public double[] lambda; // 存储 λj
        }

        private static Coefficients get_coefficients(List<RainStation> stations) {
            int n = stations.size();
            RealMatrix matrixA = new Array2DRowRealMatrix(n + 3, n + 3);
            RealVector vectorB = new ArrayRealVector(n + 3);

            // 填充矩阵A和向量B的前n行
            for (int i = 0; i < n; i++) {
                double x = stations.get(i).getStationX();
                double y = stations.get(i).getStationY();
                double[] rainfall = stations.get(i).getRainfall();

                matrixA.setEntry(i, 0, 1);
                matrixA.setEntry(i, 1, x);
                matrixA.setEntry(i, 2, y);
                for (int j = 0; j < n; j++) {
                    double distance = calculateDistance(stations.get(i), stations.get(j));
                    matrixA.setEntry(i, j + 3, R_r(distance));
                }
                double rainfallAverage = Arrays.stream(stations.get(i).getRainfall()).average().orElse(0.0);
                vectorB.setEntry(i, rainfallAverage);
            }

            // 填充矩阵A的最后三行
            for (int i = 0; i < 3; i++) {
                matrixA.setEntry(n + i, i, 1);
            }


            // 使用LU分解求解线性方程组
            DecompositionSolver solver = new LUDecomposition(matrixA).getSolver();
            RealVector solution = solver.solve(vectorB);

            Coefficients coeffs = new Coefficients();
            coeffs.ai = new double[3];
            coeffs.lambda = new double[n];

            // 提取 a1, a2, a3
            for (int i = 0; i < 3; i++) {
                coeffs.ai[i] = solution.getEntry(i);
            }

            // 提取 λj
            for (int i = 0; i < n; i++) {
                coeffs.lambda[i] = solution.getEntry(i + 3);
            }

            return coeffs;
        }

        private static double rainfallInterpolation(double centerX, double centerY, List<RainStation> stations, Coefficients coeffs) {
            int N = stations.size();
            double interpolation = coeffs.ai[0] + coeffs.ai[1] * centerX + coeffs.ai[2] * centerY;

            for (int j = 0; j < N; j++) {
                double distance = calculateDistance(new RainStation(centerX, centerY,new double[]{0}), stations.get(j));
                interpolation += coeffs.lambda[j] * R_r(distance);
            }

            return interpolation;
        }
    }
    // 自然邻域插值
    public class NaturalNeighbor {
        public double[][] NaturalNeighbor_interpolation(int ID, int numThreads) {
            XY[][] isolateCell=getIsolateCell();
            RainStation[] rainStation = cell.getRainStation();
            double[][] result = new double[cell.getNrows()][cell.getNcols()];  //插值结果矩阵
            double[][] station = new double[cell.getRainStationNum()+1][2];
            //生成仅有雨量站点的泰森多边形
            for(int i = 0; i < cell.getRainStationNum(); i++){
                station[i][0] = cell.getRainStation()[i].getStationX();
                station[i][1] = cell.getRainStation()[i].getStationY();
            }
            int[][] label = Voronoi(cell.getRainStationNum(), station, isolateCell);//泰森多边形标签
            //加入邻域计算权重并赋值
            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            Future<?>[] futures = new Future[numThreads];

            // 计算每个线程的任务大小
            int taskSize = cell.getNrows() / numThreads;
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startRow = threadNum * taskSize;
                final int endRow = (threadNum == numThreads - 1) ? cell.getNrows() : (threadNum + 1) * taskSize;

                // 提交线程任务
                int finalThreadNum = threadNum;
                int[][] finalLabel = label;
                futures[threadNum] = executor.submit(() -> {
                    for (int i = startRow; i < endRow; i++) {
                        for (int j = 0; j < cell.getNcols(); j++) {
                            // 多线程下，你需要确保每个线程都有自己的局部变量
                            int newlabelNum = 0;
                            int[] overlapNum = new int[cell.getRainStationNum()];
                            double[][] station_only = new double[cell.getRainStationNum() + 1][2];
                            // 复制数据到局部station_only数组
                            for (int s = 0; s <= cell.getRainStationNum(); s++) {
                                station_only[s][0] = station[s][0];
                                station_only[s][1] = station[s][1];
                            }
                            // 其他代码保持不变
                            station_only[cell.getRainStationNum()][0] = isolateCell[i][j].getX();
                            station_only[cell.getRainStationNum()][1] = isolateCell[i][j].getY();
                            Arrays.fill(overlapNum, 0);
                            int [][] newLabel = Voronoi(cell.getRainStationNum() + 1, station_only, isolateCell);
                            for (int m = 0; m < cell.getNrows(); m++) {
                                for (int n = 0; n < cell.getNcols(); n++) {
                                    if (newLabel[m][n] == cell.getRainStationNum()) {
                                        newlabelNum++;
                                        overlapNum[finalLabel[m][n]]++;
                                    }
                                }
                            }
                            for (int k = 0; k < cell.getRainStationNum(); k++) {
                                result[i][j] += (double) overlapNum[k] / newlabelNum * rainStation[k].getRainfall()[ID];
                            }
                            if (newlabelNum == 0) {
                                result[i][j] = cell.getNodataValue();
                                for (int p = 0; p < cell.getRainStationNum(); p++) {
                                    if (Arrays.equals(station_only[p], station_only[cell.getRainStationNum()])) {
                                        result[i][j] = rainStation[p].getRainfall()[ID];
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });
            }

            // 关闭线程池
            executor.shutdown();

            try {
                // 等待所有线程完成
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for(int i = 0; i < cell.getNrows(); i++){
                for(int j = 0; j < cell.getNcols(); j++){
                    if(cell.getDEM()[i][j] == cell.getNodataValue()){
                        result[i][j] = cell.getNodataValue();
                    }
                }
            }
            return result;
        }
        private static int[][] Voronoi(int stationNum, double[][] station, XY[][] isolateCell){
            int[][] label = new int[cell.getNrows()][cell.getNcols()];
            double minDistance;
            //初始化标签矩阵为-1
            for (int i = 0; i < cell.getNrows(); i++) {
                Arrays.fill(label[i], -1);
            }
            //栅格化泰森多边形
            for (int i = 0; i < cell.getNrows(); i++) {
                for (int j = 0; j < cell.getNcols(); j++) {
                    minDistance = 9999999;
                    for (int k = 0; k < stationNum; k++) {
                        double distance = BasicCal.getDistance(isolateCell[i][j].getX(), isolateCell[i][j].getY(), station[k][0], station[k][1]);
                        if (distance < minDistance) {
                            minDistance = distance;
                            label[i][j] = k;
                        }
                    }
                }
            }
            return label;
        }
    }
    // 径向基函数插值
    public class RBF {
        public double [][] RBF_interpolation(int ID, int numThreads, String RBFClass,double s ){
            //得到雨量增广矩阵
            double [][] augmentedMatrix=getAugmentedMatrix(ID);
            double [][] result=new double[cell.getNrows()][cell.getNcols()];
            //求取径向基函数结果
            for(int i=0;i<augmentedMatrix.length;i++){
                for (int j=0;j<augmentedMatrix.length;j++){
                    augmentedMatrix[i][j]=chooseRBF(RBFClass,augmentedMatrix[i][j],s);
                }
            }
            //获得权重
            double [][] GJresult= BasicCal.Gauss_Jordan_Elimination(augmentedMatrix);
            double [] w= BasicCal.xSolve(GJresult);
            //判断权重是否存在
            int w_0=0;
            for(int i=0;i<w.length;i++){
                if(w[i]==0){
                    w_0++;
                }
            }
            if(w_0==w.length){
                System.out.println("插值失败！");
                for(int i=0;i< cell.getNrows();i++) {
                    for (int j = 0; j < cell.getNcols(); j++) {
                        result[i][j] = -2;
                    }
                }
            }
            else{
                XY[][] isolateCell=getIsolateCell();
                double [][] DEM=cell.getDEM();
                int numRows = DEM.length;
                int numCols = DEM[0].length;
                int taskSize = numRows / numThreads;

                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                Future<?>[] futures = new Future[numThreads];

                for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                    final int startRow = threadNum * taskSize;
                    final int endRow = (threadNum == numThreads - 1) ? numRows : (threadNum + 1) * taskSize;

                    futures[threadNum] = executor.submit(() -> {
                        for (int i = startRow; i < endRow; i++) {
                            for (int j = 0; j < numCols; j++) {
                                if (DEM[i][j] == cell.getNodataValue()) {
                                    result[i][j] = cell.getNodataValue();
                                } else {
                                    double sum = 0;
                                    for (int k = 0; k < cell.getRainStationNum(); k++) {
                                        sum = w[k] * chooseRBF(RBFClass, BasicCal.getDistance(isolateCell[i][j].getX(), isolateCell[i][j].getY(), cell.getRainStation()[k].getStationX(), cell.getRainStation()[k].getStationY()), s);
                                    }
                                    result[i][j] = sum;
                                    for (int p = 0; p < cell.getRainStationNum(); p++) {
                                        if (cell.getRainStation()[p].getStationX() == isolateCell[i][j].getX() && cell.getRainStation()[p].getStationY() == isolateCell[i][j].getY()) {
                                            result[i][j] = cell.getRainStation()[p].getRainfall()[ID];
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
                executor.shutdown();

                try {
                    for (Future<?> future : futures) {
                        future.get();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
        // 获得增广矩阵
        private double [][] getAugmentedMatrix(int ID){
            double [][] result=new double[cell.getRainStationNum()][cell.getRainStationNum()+1];
            for(int i=0;i<cell.getRainStationNum();i++){
                for(int j=0;j<cell.getRainStationNum();j++){
                    result[i][j]= BasicCal.getDistance(cell.getRainStation()[i].getStationX(),cell.getRainStation()[i].getStationY(),cell.getRainStation()[j].getStationX(),cell.getRainStation()[j].getStationY());
                }
                result[i][cell.getRainStationNum()]=cell.getRainStation()[i].getRainfall()[ID];
            }
            return result;
        }
        // 选择径向基函数
        private double chooseRBF(String RBFClass, double x ,double s){
            double result=0;
            switch (RBFClass) {
                //高斯曲面函数
                case "Gaussian" -> result = Math.exp(Math.pow(x, 2) * (-0.5) / Math.pow(s, 2));

                //多项式函数
                case "Multiquadrics" -> result = Math.sqrt(1 + Math.pow(x / s, 2));

                //线性函数
                case "Linear" -> result = x;

                //分段线性函数
                case "LinearEpsR" -> {
                    result = 2 * s - x;
                    if (result < 0) {
                        result = 0;
                    }
                }
                //立方曲面函数
                case "Cubic" -> result = Math.pow(x, 3);

                //薄板曲面函数
                case "Thinplate" -> result = Math.pow(x, 2) * Math.log(x + 1);

                //Wendland's C2函数，网格变形常用
                case "Wendland" -> result = Math.pow((1 - x), 4) * (4 * x + 1);
                default -> System.out.println("暂无此函数！");
            }
            return result;
        }
    }
}
