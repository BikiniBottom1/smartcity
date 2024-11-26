package package_for_gridanalysis;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class MySQLOperate {
    private String url;
    private String user;
    private String password;
    public Connection conn;
    public Cell cell;
    public List<String> enName = new ArrayList<>();
    public void Initial(Cell cell) {
        this.cell = cell;
    }
    public String startT;
    public String endT;

    //-------------------------connect database���ݿ�����-------------------------
    public MySQLOperate(String newUrl, String newUser, String newPassword) {
        this.url = newUrl;
        this.user = newUser;
        this.password = newPassword;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("DataBase connected");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    //-------------------create database and table����---------------------
    public void createDataBase() {
        try {
            Statement stat = conn.createStatement();
            //create database PADB ��short for Precipitation Analysis Database)
            stat.executeUpdate("CREATE DATABASE IF NOT EXISTS PADB");

            //create table CellProperty
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS CellProperty");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `CellProperty`(" +
                    "`Column` INT PRIMARY KEY," +
                    "`Row` INT," +
                    "`CellSize` DOUBLE," +
                    "`XLLCorner` DOUBLE," +
                    "`YLLCorner` DOUBLE," +
                    "`NODATA_VALUE` INT)ENGINE=InnoDB DEFAULT CHARSET=UTF8;");

            //create table DEM
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS DEM");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `DEM`(" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE)ENGINE=InnoDB DEFAULT CHARSET=UTF8;");

            //create table Gauge
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS Gauge");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `Gauge` (" +
                    "`ID` INT PRIMARY KEY," +
                    "`SID` CHAR(20)," +
                    "`StationName` CHAR(20)," +
                    "`X` DOUBLE," +
                    "`Y` DOUBLE," +
                    "`EnName` CHAR(20))ENGINE=InnoDB DEFAULT CHARSET=UTF8;");

            //create table Rainfall
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS Rainfall");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `Rainfall` (" +
                    "`RID` INT PRIMARY KEY," +
                    "`Gauge` CHAR(20)," +
                    "`Rainfall` DOUBLE," +
                    "`Time` TIMESTAMP)ENGINE=InnoDB DEFAULT CHARSET=UTF8;");

            //create table DEM_FILLED
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS DEM_FILLED");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `DEM_FILLED` (" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE);");


            //create table Accumulation
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS AccumulationD");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `Accumulation` (" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE);");

            //create table package_for_gridanalysis.Slope
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS Slope");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `Slope` (" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE);");

            //create table Flowdir
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS Flowdir");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `Flowdir` (" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE);");

            //create table Aspect
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS Aspect");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `Aspect` (" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE);");

            //create table package_for_gridanalysis.StreamNet
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS StreamNet");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `StreamNet` (" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE);");

            //create table package_for_gridanalysis.StreamNet
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("DROP TABLE if EXISTS RidgeLine");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS `RidgeLine` (" +
                    "`row` INT," +
                    "`col` INT," +
                    "`dem` DOUBLE);");


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //-------------------------insert data���ݲ���-------------------------
    //Gauge
    public void insert_Gauge(String gaugesFile) {
        //��ȡ����վ����
        try (BufferedReader reader1 = new BufferedReader(new FileReader(gaugesFile))) {
            // ��һ�л�ȡ����վ����
            String[] columns1 = reader1.readLine().split("\\s+");
            int N = Integer.parseInt(columns1[0]);
            double[][] gaugeInfo = new double[N][2];
            List<String> sidList = new ArrayList<>();
            List<String> stationName = new ArrayList<>();
           // List<String> enName = new ArrayList<>();

            Statement stat = conn.createStatement();
            stat.executeUpdate("USE PADB");
            String insertQuery = "INSERT INTO Gauge (ID, SID, StationName, X, Y,EnName) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStatement = conn.prepareStatement(insertQuery);

            for (int i = 0; i < N; i++) {
                try (BufferedReader reader = new BufferedReader(new FileReader(gaugesFile))) {
                    // ����ǰ����
                    reader.readLine();
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] columns = line.split("\\s+"); // ������֮��ʹ�ÿո�ָ�
                        if (columns.length >= 3) {
                            //sidList.add(Integer.parseInt(columns[2]));
                            sidList.add(columns[2]);
                            stationName.add(columns[1]);
                            this.enName.add(columns[5]);
                            gaugeInfo[i][0] = Double.parseDouble(columns[3]);
                            gaugeInfo[i][1] = Double.parseDouble(columns[4]);
                            insertStatement.setInt(1, i + 1);
                            insertStatement.setString(2, sidList.get(i));
                            insertStatement.setString(3, stationName.get(i)); // ����stationName
                            insertStatement.setDouble(4, gaugeInfo[i][0]);  // ����X
                            insertStatement.setDouble(5, gaugeInfo[i][1]);  // ����Y
                            insertStatement.setString(6, enName.get(i));  // ����EnName
                        }
                    }
                }
                insertStatement.executeUpdate();
            }
            System.out.println("save table Gauge successfully");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //DEM
    public void insert_DEM(Cell cell) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        double[][] dem = cell.getDEM();
        String insertQuery = "INSERT INTO DEM (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, dem[i][j]);
                }
            }
            System.out.println("save table DEM successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Rainfall
    public void insert_Rainfall(String rainFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(rainFile))) {
            String line;

            // ��ȡ�ļ��ĵ�һ�У���ȡ����������
            line = reader.readLine();
            String[] headers = line.split("\\s+");

            int rowCount = Integer.parseInt(headers[3]);
            int columnCount = Integer.parseInt(headers[4]);

            // վ����
            String line2 = reader.readLine();
            String[] data = line2.split("\\s+");
            // ��ʼ�� gaugeEnName ����
            String[] gaugeIDs = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                gaugeIDs[i] = enName.get(i%3);
            }

            // ���ڱ��潵ˮ�����ݵĶ�ά����
            double[][] rainfallData = new double[rowCount][columnCount];

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            // �������飬���ݽ�ˮ����������������
            String[] dates = new String[rowCount];
            headers[0] = headers[0] + "0000";
            headers[1] = headers[1] + "0000";
            startT=headers[0];
            endT=headers[1];

            Date firstDate = dateFormat.parse(headers[0]);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(firstDate);

            // ������������
            for (int i = 0; i < rowCount; i++) {
                dates[i] = dateFormat.format(calendar.getTime());
                calendar.add(Calendar.HOUR, 1); // ÿ������һ��Сʱ
            }

            // ��ȡ�ļ���ʣ���У���������ˮ������
            int rowIndex = 0;
            while ((line = reader.readLine()) != null) {
                String[] rowData = line.split("\\s+");

                // ������ǰ�еĽ�ˮ������
                for (int i = 2; i < columnCount+2; i++) {
                    rainfallData[rowIndex][i - 2] = Double.parseDouble(rowData[i]); // �ӵ�����Ԫ�ؿ�ʼ
                }
                rowIndex++;
            }

            // ʹ�ò�������ѯ�����ݲ��뵽����
            Statement stat = conn.createStatement();
            stat.executeUpdate("USE PADB");
            stat.executeUpdate("ALTER TABLE Rainfall MODIFY COLUMN RID INT AUTO_INCREMENT");
            String insertQuery = "INSERT INTO Rainfall (RID, Gauge, Rainfall, TIME) VALUES (DEFAULT, ?, ?, ?)";
            try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
                // ������ά���飬��������
                for (int row = 0; row < rowCount; row++) {
                    for (int column = 0; column < columnCount; column++) {
                        double rainfall = rainfallData[row][column];

                        // ���ò�����ִ�в������
                        statement.setString(1, gaugeIDs[column]);
                        statement.setDouble(2, rainfall);
                        statement.setString(3, dates[row]);
                        statement.executeUpdate();
                    }
                }
            }
            System.out.println("save table Rainfall successfully");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    //CellProperty
    public void insert_CellProperty(Cell cell) {

        int ROW = cell.getNrows();
        int COLUMN = cell.getNcols();
        double CELL_CORNER_X = cell.getXllcorner();
        double CELL_CORNER_Y = cell.getYllcorner();
        double CELL_SIZE = cell.getCellsize();
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("USE PADB");

            stat.executeUpdate("INSERT INTO CellProperty ( `Column`,`Row`,  CellSize, " +
                    "XLLCorner, YLLCorner, NODATA_VALUE) " + "VALUES ('" + COLUMN + "', '" + ROW + "', " +
                    "'" + CELL_SIZE + "', '" + CELL_CORNER_X + "', '" + CELL_CORNER_Y + "', -9999)");
            System.out.println("save table CellProperty successfully");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void insert_originalData(Cell cell, String gaugesFile, String rainFile) {
        insert_Gauge(gaugesFile);
        insert_DEM(cell);
        insert_Rainfall(rainFile);
        insert_CellProperty(cell);
    }


    //���ݽ����
    public void insert_DEMFilled(Cell cell, double[][] DEMFilled) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        String insertQuery = "INSERT INTO DEM_FILLED (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, DEMFilled[i][j]);
                }
            }
            System.out.println("save table DEM_FILLED successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //��������
    public void insert_Flowdir(Cell cell, double[][] DEM) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        String insertQuery = "INSERT INTO Flowdir (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, DEM[i][j]);
                }
            }
            System.out.println("save table Flowdir successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //�¶Ƚ����
    public void insert_Slope(Cell cell, double[][] DEM) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        String insertQuery = "INSERT INTO Slope (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, DEM[i][j]);
                }
            }
            System.out.println("save table package_for_gridanalysis.Slope successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //�ۼ��������
    public void insert_accumulation(Cell cell, double[][] DEM) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        String insertQuery = "INSERT INTO Accumulation (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, DEM[i][j]);
                }
            }
            System.out.println("save table Accumulation successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //��������
    public void insert_Aspect(Cell cell, double[][] DEM) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        String insertQuery = "INSERT INTO Aspect (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, DEM[i][j]);
                }
            }
            System.out.println("save table Aspect successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //���������
    public void insert_streamNet(Cell cell, double[][] DEM) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        String insertQuery = "INSERT INTO StreamNet (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, DEM[i][j]);
                }
            }
            System.out.println("save table package_for_gridanalysis.StreamNet successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //ɽ���߽����
    public void insert_RidgeLine(Cell cell, double[][] DEM) {
        int ncols = cell.getNcols();
        int nrows = cell.getNrows();
        String insertQuery = "INSERT INTO RidgeLine (row, col, dem) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            Statement stat = conn.createStatement();

            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    statement.setInt(1, i + 1);
                    statement.setInt(2, j + 1);
                    statement.setDouble(3, DEM[i][j]);
                }
            }
            System.out.println("save table package_for_gridanalysis.RidgeLine successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    //---------------------------------query--------------------------------------
    public void QueryPrecip(String newGauge, String newTime) {
        //���Ӳ���ѯ
        try {
            String sqlPrecip = "select* from Rainfall where Gauge=? and Time=?";
            PreparedStatement statPrecip = conn.prepareStatement(sqlPrecip);  //PreparedStatement�ӿ�

            statPrecip.executeUpdate("use PADB");
            statPrecip.setString(1, newGauge);  //GaugeID
            statPrecip.setString(2, newTime);

            ResultSet rs4 = statPrecip.executeQuery();  //��ѯ
            if (rs4.next()) {

                String GaugeID = rs4.getString("Gauge");
                Timestamp Time2 = rs4.getTimestamp("Time");
                double Rainfall = rs4.getDouble("Rainfall");
                System.out.println("\n-------Rainfall Query Result-------");
                System.out.println("GaugeID:" + GaugeID + "\nTime:" + Time2 + "\nRainfall:" + Rainfall);
                System.out.println("-----------------------------------");
            } else {
                System.out.println("\n-------Rainfall Query Result-------");
                System.out.println("Unable to find relevant data");
                System.out.println("-----------------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void QueryGauge(String newGaugeSID) {
        //���Ӳ���ѯ
        try {
            String sqlGauge = "select ID,SID,EnName,X,Y from Gauge where SID=? ";
            PreparedStatement statGauge = conn.prepareStatement(sqlGauge);  //PreparedStatement�ӿ�
            statGauge.executeUpdate("use PADB");
            statGauge.setString(1 ,newGaugeSID);

            //��ѯGauge
            ResultSet rs3 = statGauge.executeQuery();
            if (rs3.next()) {

                int GaugeID = rs3.getInt("ID");
                String SID = rs3.getString("SID");
                String EnName = rs3.getString("EnName");
                double X = rs3.getDouble("X");
                double Y = rs3.getDouble("Y");
                System.out.println("\n--------Gauge Query Result--------");
                System.out.println("GaugeID:" + GaugeID + "\nSID:" + SID + "\nGaugeName:" + EnName + "\nCoordinate:(" + X + "," + Y + ")");
                System.out.println("-----------------------------------");
            } else {
                System.out.println("\n--------Gauge Query Result--------");
                System.out.println("Unable to find relevant data");
                System.out.println("-----------------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //-------------------------------statistic-------------------------------------
    //ͳ��һ������վ
    public void StatisticStation(String newS) {
        try {
            //PreparedStatement�ӿ�
            String sqlGauge = "select * from Rainfall where Gauge=?";
            PreparedStatement statStatistics = conn.prepareStatement(sqlGauge);

            statStatistics.executeUpdate("use PADB");
            statStatistics.setString(1, newS);

            ResultSet rs3 = statStatistics.executeQuery();
            List<Double> list = new ArrayList<>();

            while (rs3.next()) {
                double Rainfall = rs3.getDouble("Rainfall");
                list.add(Rainfall);
            }

            if (list.size() == 0) {
                System.out.println("\n----------Rainfall Statistics ---------");
                System.out.println("Unable to find relevant data");
                System.out.println("----------------------------------------");
            }

            // �������ֵ
            double max = Collections.max(list);
            // ������Сֵ
            double min = Collections.min(list);

            // ����ƽ��ֵ��������
            double sum = 0;
            for (int i = 0; i < list.size(); ++i) {
                sum += list.get(i);
            }
            int lag = 1;
            double average = sum / list.size();
            DecimalFormat df = new DecimalFormat("#.##");
            double dsum = 0;
            for (int i = 0; i < list.size(); ++i) {
                double s = list.get(i) - average;
                dsum += Math.pow(s, 2);
            }
            double standardDeviation = Math.sqrt(dsum / (list.size() - 1));
            System.out.println("\n----------Rainfall Statistics ---------");
            System.out.println("Statistical results of gauge "+newS+":");
            System.out.println("Mean value: " + df.format(average));
            System.out.println("Maximum value: " + max);
            System.out.println("Minimum value: " + min);
            System.out.println("Mean square error: " + df.format(standardDeviation));

            System.out.println("--------------------------------------");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void PrintStationName() {
        try {
            // ����Statement����
            Statement statement = conn.createStatement();

            // ִ��SQL��ѯ
            String sql = "SELECT EnName FROM gauge";
            ResultSet resultSet = statement.executeQuery(sql);

            // �������������ӡ����е�����
            while (resultSet.next()) {
                System.out.println(resultSet.getString("EnName"));
            }
            // �ر���Դ
            resultSet.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void PrintSID()
    {
        try {
            // ����Statement����
            Statement statement = conn.createStatement();

            // ִ��SQL��ѯ
            String sql = "SELECT SID FROM gauge";
            ResultSet resultSet = statement.executeQuery(sql);

            // �������������ӡ����е�����
            while (resultSet.next()) {
                System.out.println(resultSet.getString("SID"));}
            // �ر���Դ
            resultSet.close();
            statement.close();} catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void PrintDuration() {
        SimpleDateFormat St = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat Et = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat outputFormats = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat outputFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date startdate = St.parse(startT);
            Date enddate = Et.parse(endT);
            String formattedDates = outputFormats.format(startdate);
            String formattedDatee = outputFormate.format(enddate);
            System.out.println("Start:"+formattedDates);
            System.out.println("End:  "+formattedDatee);
        } catch (ParseException e) {
            e.printStackTrace();
        }
            }

}


