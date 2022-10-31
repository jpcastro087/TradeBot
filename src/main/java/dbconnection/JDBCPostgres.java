package dbconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import system.ConfigSetup;

public class JDBCPostgres {
    public static Connection conn;

    public static Connection getConnection() {
        String url = "jdbc:postgresql://"+ConfigSetup.HOST_DATABASE+"/binance?user="+ConfigSetup.USER_DATABASE+"&password="+ConfigSetup.PASS_DATABASE;
        try {
            if (conn == null)
                conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static void update(String sql, Object... objects) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(sql);
            setearParametrosStatment(ps, objects);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void create(String sql, Object... objects) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(sql);
            setearParametrosStatment(ps, objects);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ResultSet getResultSet(String sql, Object... objects) {
        ResultSet rs = null;
        try {
            PreparedStatement ps = getConnection().prepareStatement(sql);
            setearParametrosStatment(ps, objects);
            rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    private static void setearParametrosStatment(PreparedStatement ps, Object[] objects) {
        try {
            if (objects != null && objects.length > 0)
                for (int i = 0; i < objects.length; i++)
                    ps.setObject(i + 1, objects[i]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
