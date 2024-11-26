package package_for_gridanalysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.util.ArrayList;


public class DrawPolyline {
    /**
     * 创建绘制折线图的对象。
     *
     * @param data     折线图的数据，是一个包含多组数据的列表数组，每组数据是一个包含 (X, Y) 坐标点的列表
     * @param lineName 折线的名称数组，用于标识每组数据的名称
     * @param title    折线图的标题
     * @param xLabel   X 轴标签
     * @param yLabel   Y 轴标签
     */
    private ArrayList<XY> [] data;//每一个ArrayList<package_for_gridanalysis.XY>为一条线
    private String [] lineName;
    private String title;
    private String xLabel;
    private String yLabel;
    public DrawPolyline(ArrayList<XY> [] data, String [] lineName, String title, String xLabel, String yLabel){
        this.data=data;
        this.lineName=lineName;
        this.title=title;
        this.xLabel=xLabel;
        this.yLabel=yLabel;
    }
    public void draw(){
        //创建曲线的数据
        XYSeries [] myData = new XYSeries[data.length];
        for(int i=0;i<data.length;i++){
            myData[i]=new XYSeries(lineName[i]);
            for(int j=0;j<data[i].size();j++){
                myData[i].add(data[i].get(j).getX(),data[i].get(j).getY());
            }
        }
        // 创建数据集
        XYSeriesCollection dataset = new XYSeriesCollection();
        for(int i=0;i<data.length;i++){
            dataset.addSeries(myData[i]);
        }
        // 创建JFreeChart对象
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,  // 包括图例
                true,  // 生成工具提示
                false  // 生成URL链接
        );
        chart.getTitle().setFont(new Font("Time New Romans", Font.PLAIN, 18));
        chart.setBackgroundPaint(Color.WHITE);
        // 获取图表的绘图区域
        XYPlot plot = chart.getXYPlot();
        // 更改绘图区背景颜色
        plot.setBackgroundPaint(Color.white);
        // 利用awt进行显示
        ChartFrame chartFrame = new ChartFrame("Test", chart);

        chartFrame.pack();
        chartFrame.setVisible(true);
    }
}
