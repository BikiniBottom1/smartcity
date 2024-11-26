package package_for_gridanalysis;

/**
 * ��ʾһ������վ���࣬��������վ��λ�úͽ�ˮ���ݡ�
 */
public class RainStation {
    private double stationX;
    private double stationY;
    private double[] rainfall;
    /**
     * ����һ������վ����
     *
     * @param x ����վ��X����
     * @param y ����վ��Y����
     * @param rf ����վ�Ľ�ˮ��������
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
