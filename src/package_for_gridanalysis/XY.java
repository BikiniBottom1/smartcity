package package_for_gridanalysis;

//表示具有 x 和 y 坐标的点的类。
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
