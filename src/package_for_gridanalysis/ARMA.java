package package_for_gridanalysis;

import Jama.Matrix;

import java.util.ArrayList;

public class ARMA {
    // 计算一次差分
    private static double[] diff(double[] data, int start) {
        double[] diffOne = new double[data.length-start-1];
        for (int i = 0; i < data.length-start-1; i++) {
            diffOne[i] = data[start + i + 1] - data[start + i];
        }
        return diffOne;
    }
    private static double [] reDiff(double [] diff,double firstData){
        double [] result=new double[diff.length+1];
        result[0]=firstData;
        for(int i=1;i<result.length;i++){
            result[i]=result[i-1]+diff[i-1];
        }
        return  result;
    }
    // 计算滞后k自协方差
    private static double autoCovariance(double[] data, int delay) {
        int len = data.length;
        if(len<=delay){
            System.out.println("数据长度小于滞后间隔！");
            return 0;
        }
        double dMean = BasicCal.mean(data);
        double dAutoCorr = 0.0;
        for (int i = 0; i < len - delay; i++) {
            dAutoCorr += (data[i] - dMean) * (data[i + delay] - dMean);
        }
        return dAutoCorr / (len-delay);
    }
    // 计算滞后自相关系数
    private static double autoCorr(double[] data, int delay) {
        double dAutoCov = autoCovariance(data, delay);
        double dVar = BasicCal.var(data);
        return dAutoCov / dVar;
    }

    // 计算滞后自相关系数序列
    private static double[] autoCorr(double[] data, int startDelay, int len) {
        double[] dAutoCorrs = new double[len];
        double dVar = BasicCal.var(data);
        for (int i = 0; i < len; i++) {
            dAutoCorrs[i] = autoCovariance(data, startDelay+i)/dVar;
        }
        return dAutoCorrs;
    }
    //构建AR模型,返回参数和预测
    private static ArrayList<Double> [] buildAR(double [] data,int p) {
        double[] result_p = new double[data.length - p];
        double[][] X_array = new double[result_p.length][p + 1];
        //构造定义矩阵
        for (int i = 0; i < result_p.length; i++) {
            X_array[i][0] = 1;
            for (int j = 1; j < p + 1; j++) {
                X_array[i][j] = data[p - j + i];
            }
        }
        Matrix X_mat = new Matrix(X_array);
        //构造目标矩阵
        double[][] Y_array = new double[result_p.length][1];
        for (int i = 0; i < result_p.length; i++) {
            Y_array[i][0] = data[i + p];
        }
        Matrix Y_mat = new Matrix(Y_array);
        //最小二乘法求参数
        Matrix p_par = BasicCal.OLS(X_mat, Y_mat);
        //得到预测结果
        for (int i = 0; i < result_p.length; i++) {
            result_p[i] = 0;
            for (int j = 0; j < p+1; j++) {
                result_p[i] += X_array[i][j] * p_par.get(j, 0);
            }
        }
        //存储参数
        ArrayList<Double> p_par_arrayList = new ArrayList<>();
        for (int i = 0; i < p + 1; i++) {
            p_par_arrayList.add(p_par.get(i, 0));
        }
        //存储预测结果
        ArrayList<Double> result_p_arrayList = new ArrayList<>();
        for (int i = 0; i < result_p.length; i++) {
            result_p_arrayList.add(result_p[i]);
        }
        return new ArrayList[]{p_par_arrayList, result_p_arrayList};
    }
    //构建MR模型，返回参数
    private static Matrix buildMA(double[] residuals,int q) {
        double [] result_q=new double[residuals.length-q];
        double [][] X_array=new double[result_q.length][q+1];
        for(int i=0;i<result_q.length;i++){
            X_array[i][0]=1;
            for (int j=1;j<q+1;j++){
                X_array[i][j]=residuals[q-j+i];
            }
        }
        Matrix X_mat=new Matrix(X_array);
        double [][] Y_array=new double[result_q.length][1];
        for(int i=0;i<result_q.length;i++){
            Y_array[i][0]=residuals[i+q];
        }
        Matrix Y_mat=new Matrix(Y_array);
        return BasicCal.OLS(X_mat, Y_mat);
    }
    //构建ARMA模型，返回AR参数、MA参数和残差
    private static ArrayList<Double> [] buildARMA(double [] data,int p,int q){
        ArrayList<Double> [] result_p=buildAR(data,p);
        //计算残差作为MA的白噪声
        double[] residuals = new double[result_p[1].size()];
        for (int i = 0; i < result_p[1].size(); i++) {
            residuals[i] = data[p + i] - result_p[1].get(i);
        }
        Matrix q_par=null;
        if(q==0){
            double [][] temp=new double[q+1][1];
            for (int i=0;i<q+1;i++) {
                temp[i][0] = 0;
            }
            q_par=new Matrix(temp);
        }
        else{
            q_par=buildMA(residuals,q);
        }
        //存储AR参数
        ArrayList<Double> p_par_arrayList = new ArrayList<>();
        for(int i=0;i<p+1;i++){
            p_par_arrayList.add(result_p[0].get(i));
        }
        //存储MA参数
        ArrayList<Double> q_par_arrayList = new ArrayList<>();
        for(int i=0;i<q+1;i++){
            q_par_arrayList.add(q_par.get(i,0));
        }
        //存储残差
        ArrayList<Double> residuals_arrayList = new ArrayList<>();
        for (int i = 0; i < residuals.length; i++) {
            residuals_arrayList.add(residuals[i]);
        }
        return new ArrayList[]{p_par_arrayList,q_par_arrayList,residuals_arrayList};
    }
    //利用AIC选取AIC最小的阶数，阶数限定在5之内，
    public static ArrayList<Double> [] ARMA_bestAIC(double [] data){
        ArrayList<Double>[] temp=new ArrayList[3];
        ArrayList<Double>[] result=new ArrayList[3];
        double aic_min=Double.MAX_VALUE;
        double tempAic=Double.MAX_VALUE;
        int maxP= Math.min(data.length / 5, 7);
        int maxQ= maxP-1;
        for(int p=1;p<maxP;p++){
            for(int q=1;q<maxQ;q++){
                temp=buildARMA(data,p,q);
                double [] residuals=new double[temp[2].size()];
                for(int i=0;i<residuals.length;i++){
                    residuals[i]=temp[2].get(i);
                }
                tempAic= BasicCal.ErrorAnalysis.AIC(BasicCal.var(residuals),residuals.length,p+q+2);
                System.out.printf("p=%d,q=%d,aic=%.2f\n",p,q,tempAic);
                if(tempAic<aic_min){
                    aic_min=tempAic;
                    result=temp;
                }
            }
        }
        System.out.printf("最终选择：p=%d,q=%d,aic=%.2f\n",result[0].size()-1,result[1].size()-1,aic_min);
        return result;
    }
    //单步预测
    private static double[] onePredict(double [] historyDays,double [] historyRes,double [] p_par,double [] q_par){
        //预测结果=AR预测+MA预测
        double result=0;
        double res=0;
        result+=p_par[0];
        for(int i=1;i<p_par.length;i++){
            result+=historyDays[i-1]*p_par[i];
        }
        res+=q_par[0];
        for(int i=1;i<q_par.length;i++){
            res+=historyRes[i-1]*q_par[i];
        }
        result+=res;
        return new double[]{result,res};
    }
    //多步预测
    public static double [][] multiPredict(double [] data,ArrayList<Double> [] par_res,int num){
        //提取参数
        int p=par_res[0].size()-1;
        int q=par_res[1].size()-1;
        double [] p_par=new double[p+1];
        for(int i=0;i<p+1;i++){
            p_par[i]=par_res[0].get(i);
        }
        double [] q_par=new double[q+1];
        for(int i=0;i<q+1;i++){
            q_par[i]=par_res[1].get(i);
        }
        //历史数据
        double [] historyDays= BasicCal.slice(data,data.length-p,data.length);
        //历史残差
        double [] historyRes=new double[q];
        for(int i=0;i<q;i++){
            historyRes[i]=par_res[2].get(par_res[2].size()-q+i);
        }
        double [][] result=new double[num][2];
        for(int i=0;i<num;i++) {
            result[i][0]=onePredict(historyDays,historyRes,p_par,q_par)[0];
            result[i][1]=onePredict(historyDays,historyRes,p_par,q_par)[1];
            //更新历史数据
            for(int k=0;k<historyDays.length-1;k++){
                historyDays[k]=historyDays[k+1];
            }
            historyDays[historyDays.length-1]=result[i][0];
            for(int k=0;k<historyRes.length-1;k++){
                historyRes[k]=historyRes[k+1];
            }
            historyRes[historyRes.length-1]=result[i][1];
        }
        return result;
    }
    // 装填画图变量（第一条线为观测，第二条线为预测）
    public static ArrayList <XY> [] makeArray(double[] observed, double[] predicted, int selection) {
        ArrayList<XY> arrayList1 = new ArrayList<>();
        double [] x1=new double[observed.length];
        for (int i=0;i< x1.length;i++){
            x1[i]=i+1;
        }
        for (int i = 0; i < x1.length; i++) {
            XY xy = new XY(x1[i], observed[i]);
            arrayList1.add(xy);
        }
        ArrayList<XY> arrayList2 = new ArrayList<>();
        double [] x2=new double[predicted.length];
        switch (selection){
            case (1) ->{
                for (int i=0;i< x2.length;i++){
                    x2[i]=i+1+observed.length-predicted.length;
                }
                for (int i = 0; i < x2.length; i++) {
                    XY xy = new XY(x2[i], predicted[i]);
                    arrayList2.add(xy);
                }
            }
            case (2) -> {
                for (int i=0;i< x2.length;i++){
                    x2[i]=i+1+observed.length;
                }
                for (int i = 0; i < x2.length; i++) {
                    XY xy = new XY(x2[i], predicted[i]);
                    arrayList2.add(xy);
                }
            }
        }
        return new ArrayList[]{arrayList1, arrayList2};
    }
}

