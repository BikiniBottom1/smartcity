package package_for_gridanalysis;

import java.util.HashMap;
import java.util.Map;

public class Mann_Kendall {
    private Cell cell;
    private RainStation[] rainStations;

    public Mann_Kendall(Cell cell) {
        this.cell = cell;
        this.rainStations = cell.getRainStation();
    }
    /**
     * 计算每个雨量站的Mann-Kendall趋势。
     */
    public void calculateMannKendallTrendForRainStations() {
        for (int i =0;i<cell.getRainStationNum();i++) {
            double[] rainfall = cell.getRainStation()[i].getRainfall();
            int n = rainfall.length;
            System.out.println("-------------------------------------------------------------------");
            System.out.println("雨量站"+i+"：");
            MannKendallResult result = calculateMannKendallTrend(n, rainfall);
            System.out.println("package_for_gridanalysis.RainStation Trend: " + result.trend);

        }
    }

    public static MannKendallResult calculateMannKendallTrend(int n, double[] data) {
        // 显著性检验
        double nor = 1.96;

        int sgn = 0;    // 检验函数
        int S = 0;      // 差值函数

        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                double diff = data[j] - data[i];
                if (diff == 0) {
                    sgn = 0;
                } else if (diff > 0) {
                    sgn = 1;
                } else {
                    sgn = -1;
                }
                S = S + sgn;
            }
        }

        // 计算方差 var
        double var;
        Map<Double, Integer> countMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            countMap.put(data[i], countMap.getOrDefault(data[i], 0) + 1);
        }
        if (countMap.size() == n) {
            var = n * (n - 1) * (2 * n + 5) / 18.0;
        } else {
            var = 0;
            for (Map.Entry<Double, Integer> entry : countMap.entrySet()) {
                int FQ = entry.getValue();
                var = var + FQ * (FQ - 1) * (2 * FQ + 5);
            }
            var = (n * (n - 1) * (2 * n + 5) - var) / 18.0;
            System.out.println("方差Var：" + var);
        }

        // 双边检验 H0:没有单调趋势
        double Z;
        if (S > 0) {
            Z = (S - 1) / Math.sqrt(var);
        } else if (S == 0) {
            Z = 0;
        } else {
            Z = (S + 1) / Math.sqrt(var);
        }

        System.out.println("标准正态分布统计量Z：" + Z);

        if (Math.abs(Z) > nor) {
            System.out.print("趋势分析：通过95%显著性检验");

            if (Z > 0) {
                return new MannKendallResult("降雨量在该时段内有上升趋势");
            } else if (Z < 0) {
                return new MannKendallResult("降雨量在该时段内有下降趋势");
            } else {
                return new MannKendallResult("降雨量在该时段内无明显趋势");
            }

        } else {
            System.out.println("趋势分析：未通过95%显著性检验,降雨量在该时段内没有明显变化趋势");
            return new MannKendallResult("无趋势");
        }
    }

    static class MannKendallResult {
        String trend;

        MannKendallResult(String trend) {
            this.trend = trend;
        }
    }
}
