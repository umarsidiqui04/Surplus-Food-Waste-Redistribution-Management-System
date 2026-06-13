package com.sfwrms.sfwrms;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection connect() {
        try {
            String url =
                    "jdbc:sqlserver://localhost:1433;databaseName=SFWRMS;encrypt=true;trustServerCertificate=true";

            String user = "sa";
            String password = "1234";

            Connection conn = DriverManager.getConnection(url, user, password);

            System.out.println("Database Connected!");
            return conn;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}