package package_for_gridanalysis;

import Jama.Matrix;

public class BasicCal {    // 计算距离
    public static double getDistance(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt(Math.pow(x1-x2,2)+Math.pow(y1-y2,2));
    }
    //寻找最大值行
    private static int getMaxRow(int curRow, double[][] augmentedMatrix) {
        //行扫描查找当前列中的最大值
        int maxRow = curRow;
        for (int i = curRow + 1; i < augmentedMatrix.length; i++) {
            if (augmentedMatrix[i][curRow] > augmentedMatrix[maxRow][curRow]) {
                maxRow = i;
            }
        }
        return maxRow;
    }
    //交换行
    private static double [][] swapRow(int curRow, int maxRow, double[][] matrix) {
        double [] tmp=matrix[curRow];
        matrix[curRow]=matrix[maxRow];
        matrix[maxRow]=tmp;
        return matrix;
    }
    /**
     * 使用高斯-约当消元法解线性方程组或矩阵的增广矩阵。
     *
     * @param augmentedMatrix 增广矩阵，包含系数矩阵和右侧常数向量
     * @return 解线性方程组的结果，或者包含无解或无限解信息的特殊情况
     */
    public static double [][] Gauss_Jordan_Elimination(double[][] augmentedMatrix){
        for(int i=0;i<augmentedMatrix.length-1;i++){
            int maxRow=getMaxRow(i,augmentedMatrix);
            if(maxRow!=i){
                augmentedMatrix=swapRow(i,maxRow,augmentedMatrix);
            }
            double a=augmentedMatrix[i][i];
            for(int j=i+1;j<augmentedMatrix.length;j++){
                double b=augmentedMatrix[j][i];
                for(int k=0;k<augmentedMatrix[0].length;k++){
                    augmentedMatrix[j][k]*=a;
                    augmentedMatrix[j][k]-=augmentedMatrix[i][k]*b;
                }
            }
        }
        return augmentedMatrix;
    }
    /**
     * 解高斯-约当消元法得到的增广矩阵，找到线性方程组的解。
     *
     * @param GJresult 高斯-约当消元法得到的增广矩阵
     * @return 线性方程组的解，或者包含无解或无限解信息的特殊情况
     */
    public static double [] xSolve(double[][] GJresult){
        double [] result=new double[GJresult.length];
        if(GJresult[GJresult.length-1][GJresult[0].length-2]==0){
            if(GJresult[GJresult.length-1][GJresult[0].length-1]!=0){
                System.out.print("该方程组无解！");
            }
            else{
                System.out.print("该方程组有无限个解！");
            }
            for(int i=0;i<GJresult.length;i++){
                result[i]=0;
            }
            System.out.println("插值失败！");
        }
        else{
            result[GJresult.length-1]=GJresult[GJresult.length-1][GJresult[0].length-1]/GJresult[GJresult.length-1][GJresult[0].length-2];
            for(int i=GJresult.length-2;i>=0;i--){
                double temp=GJresult[i][GJresult[0].length-1];
                for(int j=i+1;j<GJresult.length;j++){
                    temp-=GJresult[i][j]*result[j];
                }
                if(GJresult[i][i]==0){
                    System.out.println("插值失败！");
                    for(int k=0;k<GJresult.length;k++){
                        result[k]=0;
                    }
                    break;
                }
                else{
                    result[i]=temp/GJresult[i][i];
                }
            }
        }
        return  result;
    }
    // 计算均值
    public static double mean(double[] data) {
        double sum = 0.0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum / data.length;
    }
    // 计算样本方差
    public static  double var(double[] data) {
        int len = data.length;
        double dMean = mean(data);
        double sum = 0.0;
        for (int i = 0; i < len; i++) {
            sum += Math.pow((data[i] - dMean), 2);
        }
        return sum / len;
    }

    public static class ErrorAnalysis{
        //绝对误差
        public static double absoluteError(double real, double prediction) {
            return Math.abs(real - prediction);
        }
        // 平均绝对误差
        public static double MAE(double[] reals, double[] predictions) {
            if (reals.length != predictions.length) {
                System.out.println("数组长度不匹配！");
                return 0;
            }
            double sum = 0.0;
            for (int i = 0; i < reals.length; i++) {
                sum += absoluteError(reals[i], predictions[i]);
            }
            return sum / reals.length;
        }
        //残差方差
        public static double getResidualVariance(double [] observed,double [] predicted){
            double[] residuals = new double[observed.length];
            for (int i = 0; i < observed.length; i++) {
                residuals[i] = observed[i] - predicted[i];
            }
            return var(residuals);
        }
        //AIC = k*ln(n) + n * ln(σ^2)，n是数据点的数量，σ^2是模型的方差（通常是残差方差），k是模型的参数数量
        public static double AIC(double ResidualVariance,int dataCount,int argsCount){
            return dataCount * Math.log(ResidualVariance) + Math.log(dataCount) * argsCount;
        }


    }
    /**
     * 使用最小二乘法计算线性回归模型的系数。
     *
     * @param X 输入矩阵 X，包含独立变量的数据
     * @param Y 输出矩阵 Y，包含因变量的数据
     * @return 线性回归模型的系数矩阵，或者特殊情况下的零矩阵
     */
    public static Matrix OLS(Matrix X, Matrix Y) {
        Matrix result = null;
        try {
            // 计算线性回归系数矩阵
            result = X.transpose().times(X).inverse().times(X.transpose()).times(Y);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // 处理特殊情况：如果计算失败，返回零矩阵
            double[][] temp = new double[X.getColumnDimension()][1];
            for (int i = 0; i < X.getColumnDimension(); i++) {
                temp[i][0] = 0;
            }
            result = new Matrix(temp);
        }
        return result;
    }
    // * 切片
    // * 从给定数组中提取一部分数据，包括起始索引但不包括结束索引。
    // *
    // * @param data  输入数组，包含要提取的数据
    // * @param start 起始索引（包括在内）
    // * @param end   结束索引（不包括在内）
    // * @return 从原数组中提取的数据部分
    // */
    public static double [] slice(double [] data,int start,int end){
        double [] result=new double[end-start];
        for(int i=0;i<result.length;i++){
            result[i]=data[start+i];
        }
        return result;
    }

}
