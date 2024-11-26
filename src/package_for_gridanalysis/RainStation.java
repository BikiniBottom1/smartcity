package package_for_gridanalysis;

/**
 * 表示一个雨量站的类，包含雨量站的位置和降水数据。
 */
public class RainStation {
    private double stationX;
    private double stationY;
    private double[] rainfall;
    /**
     * 创建一个雨量站对象。
     *
     * @param x 雨量站的X坐标
     * @param y 雨量站的Y坐标
     * @param rf 雨量站的降水数据数组
     */
    public RainStation(double x, double y, double[] rf){
        stationX=x;
        stationY=y;
        rainfall=rf;
    }
    public double getStationX(){
        return stationX;
    }
    public double getStationY(){
        return stationY;
    }
    public double[] getRainfall(){
        return rainfall;
    }
}
