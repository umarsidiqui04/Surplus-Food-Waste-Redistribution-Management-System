package com.sfwrms.sfwrms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class DataSetup {

    public static void main(String[] args) {
        insertAll();
    }

    public static void insertAll() {
        String hash = PasswordUtil.hash("pass123");

        Connection conn = DBConnection.connect();
        if (conn == null) {
            System.err.println("[DataSetup] ERROR: Could not connect to database. " +
                               "Make sure the schema tables have been created first.");
            return;
        }

        try {
            // ── Insert NGO ────────────────────────────────────────────────────
            int ngoId = -1;
            String ngoSql = "INSERT INTO NGO (name, location, capacity, operationalArea, contactEmail) " +
                            "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ngoSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "Edhi Foundation");
                ps.setString(2, "Karachi");
                ps.setInt(3, 500);
                ps.setString(4, "Karachi South");
                ps.setString(5, "edhi@ngo.com");
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) ngoId = keys.getInt(1);
            }
            System.out.println("[DataSetup] NGO inserted. ngoId = " + ngoId);

            // ── Insert NGO Representative ─────────────────────────────────────
            String repSql = "INSERT INTO NGORepresentative (name, email, phone, passwordHash, ngoId) " +
                            "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(repSql)) {
                ps.setString(1, "Sara Khan");
                ps.setString(2, "sara@ngo.com");
                ps.setString(3, "0321-9876543");
                ps.setString(4, hash);
                ps.setInt(5, ngoId);
                ps.executeUpdate();
            }
            System.out.println("[DataSetup] NGO Rep inserted. Email: sara@ngo.com");

            // ── Insert Driver (optional, needed for pickup assignments later) ──
            String driverSql = "INSERT INTO Driver (name, vehicleInfo, phone, passwordHash, ngoId) " +
                               "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(driverSql)) {
                ps.setString(1, "Hassan Raza");
                ps.setString(2, "Toyota Hilux - ABC-123");
                ps.setString(3, "0333-1112222");
                ps.setString(4, hash);
                ps.setInt(5, ngoId);
                ps.executeUpdate();
            }
            System.out.println("[DataSetup] Driver 1 inserted. Phone: 0333-1112222");

            // ── Insert second Driver ──────────────────────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(driverSql)) {
                ps.setString(1, "Imran Sheikh");
                ps.setString(2, "Suzuki Bolan - XYZ-456");
                ps.setString(3, "0345-9998888");
                ps.setString(4, hash);
                ps.setInt(5, ngoId);
                ps.executeUpdate();
            }
            System.out.println("[DataSetup] Driver 2 inserted. Phone: 0345-9998888");

            // ── Insert Food Donor ─────────────────────────────────────────────
            String donorSql = "INSERT INTO FoodDonor (name, email, phone, organizationName, passwordHash) " +
                              "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(donorSql)) {
                ps.setString(1, "Ahmed Ali");
                ps.setString(2, "ahmed@donor.com");
                ps.setString(3, "0300-1234567");
                ps.setString(4, "Ahmed Foods Pvt Ltd");
                ps.setString(5, hash);
                ps.executeUpdate();
            }
            System.out.println("[DataSetup] Donor inserted. Email: ahmed@donor.com");

            System.out.println("\n[DataSetup] SUCCESS — test data ready.");
            System.out.println("  Donor login   : ahmed@donor.com  / pass123");
            System.out.println("  NGO Rep login : sara@ngo.com     / pass123");
            System.out.println("  Driver 1 login: 0333-1112222     / pass123");
            System.out.println("  Driver 2 login: 0345-9998888     / pass123");

        } catch (SQLException e) {
            System.err.println("[DataSetup] SQL error: " + e.getMessage());
            System.err.println("Hint: if you see 'duplicate key', test data may already exist.");
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}
