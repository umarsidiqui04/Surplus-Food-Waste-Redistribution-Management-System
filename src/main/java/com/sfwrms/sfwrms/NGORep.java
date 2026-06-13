package com.sfwrms.sfwrms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class NGORep extends User {

    private String phone;
    private int    ngoId;

    @Override
    public String getRole() { return "NGORep"; }

    @Override
    public boolean authenticate(String email, String passwordHash) {
        String sql = "SELECT repId, name, ngoId, phone " +
                     "FROM NGORepresentative WHERE email = ? AND passwordHash = ?";
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.id    = rs.getInt("repId");
                this.name  = rs.getString("name");
                this.email = email;
                this.ngoId = rs.getInt("ngoId");
                this.phone = rs.getString("phone");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        return false;
    }

    public int    getNgoId()         { return ngoId; }
    public void   setNgoId(int n)    { this.ngoId = n; }
    public String getPhone()         { return phone; }
    public void   setPhone(String p) { this.phone = p; }
}
