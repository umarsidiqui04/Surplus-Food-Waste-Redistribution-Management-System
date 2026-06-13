package com.sfwrms.sfwrms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DriverUser extends User {

    private String vehicleInfo;
    private String phone;
    private int    ngoId;

    @Override
    public String getRole() { return "Driver"; }

    @Override
    public boolean authenticate(String phoneOrEmail, String passwordHash) {
        String sql = "SELECT driverId, name, vehicleInfo, phone, ngoId " +
                     "FROM Driver WHERE phone = ? AND passwordHash = ?";
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneOrEmail);
            ps.setString(2, passwordHash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.id          = rs.getInt("driverId");
                this.name        = rs.getString("name");
                this.email       = phoneOrEmail;
                this.vehicleInfo = rs.getString("vehicleInfo");
                this.phone       = rs.getString("phone");
                this.ngoId       = rs.getInt("ngoId");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        return false;
    }

    public String getVehicleInfo()     { return vehicleInfo; }
    public void   setVehicleInfo(String v) { vehicleInfo = v; }
    public String getPhone()           { return phone; }
    public void   setPhone(String v)   { phone = v; }
    public int    getNgoId()           { return ngoId; }
    public void   setNgoId(int v)      { ngoId = v; }
}
