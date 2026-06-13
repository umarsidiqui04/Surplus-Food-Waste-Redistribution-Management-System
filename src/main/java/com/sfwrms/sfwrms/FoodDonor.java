package com.sfwrms.sfwrms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FoodDonor extends User {

    private String phone;
    private String organizationName;

    @Override
    public String getRole() { return "Donor"; }

    @Override
    public boolean authenticate(String email, String passwordHash) {
        String sql = "SELECT donorId, name, email, phone, organizationName " +
                     "FROM FoodDonor WHERE email = ? AND passwordHash = ?";
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.id               = rs.getInt("donorId");
                this.name             = rs.getString("name");
                this.email            = email;
                this.phone            = rs.getString("phone");
                this.organizationName = rs.getString("organizationName");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        return false;
    }

    public String getPhone()            { return phone; }
    public void   setPhone(String p)    { this.phone = p; }
    public String getOrganizationName()      { return organizationName; }
    public void   setOrganizationName(String o) { this.organizationName = o; }
}
