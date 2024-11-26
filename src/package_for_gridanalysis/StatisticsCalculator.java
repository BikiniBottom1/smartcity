package package_for_gridanalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class StatisticsCalculator {

    // 计算平均值
    public static double calculateMean(double[] data) {
        double sum = 0;
        for (double value : data) {
            sum += value;
        }
        return sum / data.length;
    }

    // 计算中位数
    public static double calculateMedian(double[] data) {
        double[] sortedData = Arrays.copyOf(data, data.length);
        Arrays.sort(sortedData);
        int middle = sortedData.length / 2;
        if (sortedData.length % 2 == 0) {
            return (sortedData[middle - 1] + sortedData[middle]) / 2;
        } else {
            return sortedData[middle];
        }
    }

    // 计算距平
    public static double[] calculateAnomaly(double[] data) {
        double mean = calculateMean(data);
        double[]anomaly=new double[data.length];
        for(int i=0;i< data.length;i++){
            anomaly[i]=data[i]-mean;
        }
        return anomaly;
    }

    // 计算均方差
    public static double calculateVariance(double[] data) {
        double mean = calculateMean(data);
        double variance = 0;
        for (double value : data) {
            variance += Math.pow(value - mean, 2);
        }
        return variance / data.length;
    }

    // 计算离差
    public static double calculateRange(double[] data) {
        double max = data[0];
        double min = data[0];
        for (double value : data) {
            if (value > max) {
                max = value;
            }
            if (value < min) {
                min = value;
            }
        }
        return max - min;
    }

    // 计算偏度
    public static double calculateSkewness(double[] data) {
        double mean = calculateMean(data);
        double variance = calculateVariance(data);
        double skewness = 0;
        for (double value : data) {
            skewness += Math.pow((value - mean) / Math.sqrt(variance), 3);
        }
        return skewness / data.length;
    }

    // 计算丰度
    public static double calculateKurtosis(double[] data) {
        double mean = calculateMean(data);
        double variance = calculateVariance(data);
        double kurtosis = 0;
        for (double value : data) {
            kurtosis += Math.pow((value - mean) / Math.sqrt(variance), 4);
        }
        return kurtosis / data.length;
    }

    // 计算自相关系数
    public static double calculateAutocorrelation(double[] data) {
        int n = data.length;
        double mean = calculateMean(data);
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < n - 1; i++) {
            numerator += (data[i] - mean) * (data[i + 1] - mean);
            denominator += Math.pow(data[i] - mean, 2);
        }
        double autocorrelation = numerator / denominator;
        return autocorrelation;
    }

    // 计算Pearson相关系数
    public static double calculatePearsonCorrelation(double[] data1, double[] data2) {
        if (data1.length != data2.length) {
            throw new IllegalArgumentException("两个数据数组的长度必须相同");
        }

        double sumXY = 0;
        double sumX = 0;
        double sumY = 0;
        double sumXSquare = 0;
        double sumYSquare = 0;
        int n = data1.length;

        for (int i = 0; i < n; i++) {
            sumXY += data1[i] * data2[i];
            sumX += data1[i];
            sumY += data2[i];
            sumXSquare += Math.pow(data1[i], 2);
            sumYSquare += Math.pow(data2[i], 2);
        }

        return (n * sumXY - sumX * sumY) / (Math.sqrt((n * sumXSquare - Math.pow(sumX, 2)) * (n * sumYSquare - Math.pow(sumY, 2))));
    }

    // 计算灰色关联度
    public static double calculateCorrelation(double[] data1, double[] data2) {
        if (data1.length != data2.length) {
            throw new IllegalArgumentException("两个数据数组的长度必须相同");
        }

        int length = data1.length;
        // Step 1: 标准化数据
        double[] normalizeddata1 = normalizeData(data1);
        double[] normalizeddata2 = normalizeData(data2);

        // Step 2: 建立关联系数矩阵
        double[] relationCoefficients = new double[length - 1];
        for (int i = 0; i < length - 1; i++) {
            relationCoefficients[i] = calculateRelationCoefficient(
                    normalizeddata1[i], normalizeddata1[i + 1],
                    normalizeddata2[i], normalizeddata2[i + 1]
            );
        }
        // Step 3: 计算关联度
        double sum = 0;
        for (double coefficient : relationCoefficients) {
            sum += coefficient;
        }
        return sum / (length - 1);
    }
    private static double[] normalizeData(double[] sequence) {
        double max = getMax(sequence);
        double min = getMin(sequence);
        double range = max - min;
        if (range == 0) {
            // 防止除以零
            return sequence;
        }
        double[] normalizedSequence = new double[sequence.length];
        for (int i = 0; i < sequence.length; i++) {
            normalizedSequence[i] = (sequence[i] - min) / range;
        }
        return normalizedSequence;
    }
    /**
     * 计算两个序列之间的相关系数。
     *
     * @param x0j 第一个序列的第j个元素
     * @param x1j 第二个序列的第j个元素
     * @param x1i 第二个序列的第i个元素
     * @param x2i 第三个序列的第i个元素
     * @return 相关系数
     */
    private static double calculateRelationCoefficient(double x0j, double x1j, double x1i, double x2i) {
        double min1 = Math.min(Math.abs(x0j - x1i), Math.abs(x1j - x2i));
        double min2 = Math.min(Math.abs(x1j - x1i), Math.abs(x2i - x2i));
        double max1 = Math.max(Math.abs(x0j - x1i), Math.abs(x1j - x2i));
        double max2 = Math.max(Math.abs(x1j - x1i), Math.abs(x2i - x2i));
        double denominator = max1 + max2;
        if (denominator == 0) {
            // 防止除以零
            return 0;
        }

        return (min1 + min2) / denominator;

    }
    private static double getMax(double[] array) {
        double max = array[0];
        for (double value : array) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private static double getMin(double[] array) {
        double min = array[0];
        for (double value : array) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }
    /**
     * 计算并输出给定数据的统计指标，包括平均值、中位数、最大值、最小值、均方差、离差、偏度、丰度、自相关性系数。
     *
     * @param data 二维数据数组
     * @throws IOException 如果发生文件操作异常
     */
    public static void calculateresult(double[][] data) throws IOException {
        // 将数据展平并过滤掉 nodata 即 -9999
        double[] flattenedData = Arrays.stream(data)
                .flatMapToDouble(Arrays::stream)
                .filter(value -> value != -9999) // 过滤掉 nodata即-9999
                .toArray();
        System.out.println("-------------------------------------------------------------------");
        System.out.println("插值结果统计如下：");
        System.out.println("平均值: " + String.format("%.2f", StatisticsCalculator.calculateMean(flattenedData)));
        System.out.println("中位数: " + String.format("%.2f", StatisticsCalculator.calculateMedian(flattenedData)));
        System.out.println("最大值: " + String.format("%.2f", StatisticsCalculator.getMax(flattenedData)));
        System.out.println("最小值: " + String.format("%.2f", StatisticsCalculator.getMin(flattenedData)));
        System.out.println("均方差: " + String.format("%.2f", StatisticsCalculator.calculateVariance(flattenedData)));
        System.out.println("离差: " + String.format("%.2f", StatisticsCalculator.calculateRange(flattenedData)));
        System.out.println("偏度: " + String.format("%.2f", StatisticsCalculator.calculateSkewness(flattenedData)));
        System.out.println("丰度: " + String.format("%.2f", StatisticsCalculator.calculateKurtosis(flattenedData)));
        System.out.println("自相关性系数: " + String.format("%.2f", StatisticsCalculator.calculateAutocorrelation(flattenedData)));
        System.out.println("-------------------------------------------------------------------");
        // 询问用户是否输出结果到文件
        String isWrite = Interface.input("是否将结果输出，若是请输入y：");
        if (isWrite.equals("y")) {
            String resultFileName = Interface.stringFileInput("请输入输出的文件名：", false);
            File StatisticsDocPath = new File(resultFileName);
            BufferedWriter dataWrite = new BufferedWriter(new FileWriter(StatisticsDocPath));
            dataWrite.write("各项统计指标如下：");
            dataWrite.write("\n平均值: " + String.format("%.2f", StatisticsCalculator.calculateMean(flattenedData)));
            dataWrite.write("\n中位数: " + String.format("%.2f", StatisticsCalculator.calculateMedian(flattenedData)));
            dataWrite.write("\n最大值: " + String.format("%.2f", StatisticsCalculator.getMax(flattenedData)));
            dataWrite.write("\n最小值: " + String.format("%.2f", StatisticsCalculator.getMin(flattenedData)));
            dataWrite.write("\n均方差: " + String.format("%.2f", StatisticsCalculator.calculateVariance(flattenedData)));
            dataWrite.write("\n离差: " + String.format("%.2f", StatisticsCalculator.calculateRange(flattenedData)));
            dataWrite.write("\n偏度: " + String.format("%.2f", StatisticsCalculator.calculateSkewness(flattenedData)));
            dataWrite.write("\n丰度: " + String.format("%.2f", StatisticsCalculator.calculateKurtosis(flattenedData)));
            dataWrite.write("\n自相关性系数: " + String.format("%.2f", StatisticsCalculator.calculateAutocorrelation(flattenedData)));
            dataWrite.close();
            System.out.println("已成功将统计结果输出");
        }
    }
}
