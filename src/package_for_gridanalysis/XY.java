package package_for_gridanalysis;

//��ʾ���� x �� y ����ĵ���ࡣ
public class XY {
    private double x;
    private double y;
    public XY(double x,double y){
        this.x=x;
        this.y=y;
    }
    public void set(double x,double y){
        this.x=x;
        this.y=y;
    }
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
}
