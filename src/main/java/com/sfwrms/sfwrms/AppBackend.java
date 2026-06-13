package com.sfwrms.sfwrms;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  AppBackend  —  The single business-logic and data-access layer.
 *  Architectural Style: 3-Tier Layered Architecture
 *    Tier 1 (Presentation)  : FXML + *Controller.java files
 *    Tier 2 (Business Logic): THIS FILE — AppBackend
 *    Tier 3 (Data Access)   : DBConnection + SQL Server

 *  GOF Singleton — one shared instance via getInstance()
 *  GOF Facade — single entry point; controllers call only AppBackend
 *  GRASP Information Expert — owns all domain operations
 *  GRASP Controller  — mediates between UI events and DB layer
 *  GRASP Low Coupling — UI depends only on this one class
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class AppBackend {

    // ─── Singleton ─────────────────────────────────────────────────────────────

    private static AppBackend instance;

    private AppBackend() {}

    public static synchronized AppBackend getInstance() {
        if (instance == null) instance = new AppBackend();
        return instance;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION
    // ══════════════════════════════════════════════════════════════════════════

    public User login(String emailOrPhone, String password, String role) {
        String hash = PasswordUtil.hash(password);
        User user = switch (role) {
            case "Donor"  -> new FoodDonor();
            case "Driver" -> new DriverUser();
            default       -> new NGORep();
        };
        return user.authenticate(emailOrPhone, hash) ? user : null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC01 — LOG SURPLUS FOOD  (CREATE DONATION)
    // ══════════════════════════════════════════════════════════════════════════


    public boolean createDonation(int donorId, String foodType, int quantity,
                                  String quantityUnit,
                                  LocalDateTime prepTime, LocalDateTime expiryTime,
                                  String location) {
        if (expiryTime.isBefore(LocalDateTime.now()) || quantity <= 0) {
            System.err.println("[UC01] VALIDATION FAILED: expiryTime=" + expiryTime +
                " (now=" + LocalDateTime.now() + "), quantity=" + quantity);
            return false;
        }

        String sql = "INSERT INTO Donation (donorId, foodType, quantity, quantityUnit, preparationTime, " +
                     "expiryTime, location, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending')";
        Connection conn = DBConnection.connect();
        if (conn == null) {
            System.err.println("[UC01] DATABASE CONNECTION FAILED — check DBConnection settings.");
            return false;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, donorId);
            ps.setString(2, foodType);
            ps.setInt(3, quantity);
            ps.setString(4, quantityUnit != null ? quantityUnit : "kg");
            ps.setTimestamp(5, Timestamp.valueOf(prepTime));
            ps.setTimestamp(6, Timestamp.valueOf(expiryTime));
            ps.setString(7, location);
            int rows = ps.executeUpdate();
            System.out.println("[UC01] INSERT executed, rows affected: " + rows);
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int donationId = keys.getInt(1);
                System.out.println("[UC01] Donation created with ID: " + donationId);
                storeUrgencyScore(conn, donationId, expiryTime);
                // Auto-match disabled: donations stay Pending for NGO manual accept/reject (UC09)
                // autoMatchNGO(conn, donationId, quantity, location); // UC08
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[UC01] SQL ERROR during INSERT: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC02 — MODIFY EXISTING DONATION  (UPDATE)
    // ══════════════════════════════════════════════════════════════════════════

    public boolean updateDonation(int donationId, String foodType, int quantity,
                                  String quantityUnit,
                                  LocalDateTime prepTime, LocalDateTime expiryTime,
                                  String location) {
        String sql = "UPDATE Donation SET foodType=?, quantity=?, quantityUnit=?, preparationTime=?, " +
                     "expiryTime=?, location=? WHERE donationId=? AND status='Pending'";
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, foodType);
            ps.setInt(2, quantity);
            ps.setString(3, quantityUnit != null ? quantityUnit : "kg");
            ps.setTimestamp(4, Timestamp.valueOf(prepTime));
            ps.setTimestamp(5, Timestamp.valueOf(expiryTime));
            ps.setString(6, location);
            ps.setInt(7, donationId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                storeUrgencyScore(conn, donationId, expiryTime);
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC03 — DELETE DONATION
    // ══════════════════════════════════════════════════════════════════════════

    public boolean deleteDonation(int donationId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try {
            String checkSql = "SELECT status FROM Donation WHERE donationId=?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, donationId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next() || !"Pending".equals(rs.getString("status"))) return false;
            }
            // Delete DeliveryRecord rows via PickupAssignment
            String delDR = "DELETE FROM DeliveryRecord WHERE assignmentId IN " +
                           "(SELECT assignmentId FROM PickupAssignment WHERE donationId=?)";
            try (PreparedStatement ps = conn.prepareStatement(delDR)) {
                ps.setInt(1, donationId);
                ps.executeUpdate();
            }
            // Delete PickupAssignment rows
            runUpdate(conn, "DELETE FROM PickupAssignment WHERE donationId=?", donationId);
            // Delete UrgencyScore and Notification rows
            runUpdate(conn, "DELETE FROM UrgencyScore WHERE donationId=?", donationId);
            runUpdate(conn, "DELETE FROM Notification WHERE donationId=?", donationId);
            // Delete the Donation itself
            return runUpdate(conn,
                    "DELETE FROM Donation WHERE donationId=?", donationId) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC04 — TRACK DONATION STATUS  (READ)
    // ══════════════════════════════════════════════════════════════════════════

    /** Dynamic urgency score calculated in SQL — no dependency on UrgencyScore table. */
    private static final String URGENCY_EXPR =
        "CASE " +
        "WHEN DATEDIFF(MINUTE, GETDATE(), d.expiryTime) <= 0 THEN 100.0 " +
        "WHEN DATEDIFF(MINUTE, GETDATE(), d.expiryTime) >= 2880 THEN 5.0 " +
        "ELSE ROUND(100.0 - (CAST(DATEDIFF(MINUTE, GETDATE(), d.expiryTime) AS FLOAT) * 95.0 / 2880.0), 1) " +
        "END";

    public List<DonationModel> getDonationsByDonor(int donorId) {
        List<DonationModel> list = new ArrayList<>();
        String sql = "SELECT d.*, " + URGENCY_EXPR + " AS urgencyScore " +
                     "FROM Donation d WHERE d.donorId=? ORDER BY d.createdAt DESC";
        Connection conn = DBConnection.connect();
        if (conn == null) return list;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapDonationRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return list;
    }

    public String getDonationStatus(int donationId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return "Unknown";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM Donation WHERE donationId=?")) {
            ps.setInt(1, donationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("status");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return "Unknown";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC05 — VIEW TOTAL MEALS SAVED  (AGGREGATION)
    // ══════════════════════════════════════════════════════════════════════════

    /** Business rule: 2 kg = 1 meal.  Formula: meals = SUM(quantity) / 2 */
    public int getTotalMealsSaved() {
        Connection conn = DBConnection.connect();
        if (conn == null) return 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ISNULL(SUM(quantity),0)/2 AS meals FROM Donation WHERE status='Delivered'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("meals");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC06 — GENERATE SUSTAINABILITY REPORT  (ANALYTICS)
    // ══════════════════════════════════════════════════════════════════════════

    public ReportData generateReport(int repId, String period) {
        LocalDateTime startDate = switch (period) {
            case "Weekly" -> LocalDateTime.now().minusWeeks(1);
            case "Yearly" -> LocalDateTime.now().minusYears(1);
            default       -> LocalDateTime.now().minusMonths(1);
        };
        
        Connection conn = DBConnection.connect();
        if (conn == null) return null;
        
        try {
            // First get the NGO ID for this rep
            int ngoId = 0;
            String ngoSql = "SELECT ngoId FROM NGORepresentative WHERE repId = ?";
            PreparedStatement psNgo = conn.prepareStatement(ngoSql);
            psNgo.setInt(1, repId);
            ResultSet rsNgo = psNgo.executeQuery();
            if (rsNgo.next()) {
                ngoId = rsNgo.getInt("ngoId");
            }
            rsNgo.close();
            psNgo.close();
            
            if (ngoId == 0) {
                System.err.println("[UC06] Could not find NGO for repId: " + repId);
                return null;
            }
            
            // Main stats query - donations for this NGO
            String sql = "SELECT COUNT(*) AS total, " +
                         "SUM(CASE WHEN d.status='Delivered' THEN 1 ELSE 0 END) AS delivered, " +
                         "ISNULL(SUM(CASE WHEN d.status='Delivered' THEN d.quantity ELSE 0 END),0)/2 AS meals " +
                         "FROM Donation d " +
                         "INNER JOIN PickupAssignment pa ON d.donationId = pa.donationId " +
                         "INNER JOIN NGORepresentative r ON pa.repId = r.repId " +
                         "WHERE d.createdAt >= ? AND r.ngoId = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(startDate));
            ps.setInt(2, ngoId);
            ResultSet rs = ps.executeQuery();
            
            int total = 0, delivered = 0, meals = 0;
            double rate = 0.0;
            
            if (rs.next()) {
                total     = rs.getInt("total");
                delivered = rs.getInt("delivered");
                meals     = rs.getInt("meals");
                rate      = total > 0 ? (double) delivered / total * 100.0 : 0.0;
            }
            rs.close();
            ps.close();
            
            // Top Donors for this NGO (descending by donation count)
            List<Map<String, Object>> topDonors = new ArrayList<>();
            String donorSql = "SELECT TOP 5 fd.name, ISNULL(fd.organizationName,'') AS organizationName, " +
                              "COUNT(d.donationId) AS donationCount, " +
                              "ISNULL(SUM(d.quantity),0) AS totalQuantity " +
                              "FROM FoodDonor fd " +
                              "INNER JOIN Donation d ON fd.donorId = d.donorId " +
                              "INNER JOIN PickupAssignment pa ON d.donationId = pa.donationId " +
                              "INNER JOIN NGORepresentative r ON pa.repId = r.repId " +
                              "WHERE d.createdAt >= ? AND r.ngoId = ? " +
                              "GROUP BY fd.donorId, fd.name, fd.organizationName " +
                              "ORDER BY donationCount DESC, totalQuantity DESC";
            PreparedStatement psDonor = conn.prepareStatement(donorSql);
            psDonor.setTimestamp(1, Timestamp.valueOf(startDate));
            psDonor.setInt(2, ngoId);
            ResultSet rsDonor = psDonor.executeQuery();
            while (rsDonor.next()) {
                Map<String, Object> donor = new HashMap<>();
                donor.put("name", rsDonor.getString("name"));
                donor.put("organization", rsDonor.getString("organizationName"));
                donor.put("donationCount", rsDonor.getInt("donationCount"));
                donor.put("totalQuantity", rsDonor.getInt("totalQuantity"));
                topDonors.add(donor);
            }
            rsDonor.close();
            psDonor.close();
            
            // Top Driver for this NGO (descending by deliveries completed)
            Map<String, Object> topDriver = new HashMap<>();
            String driverSql = "SELECT TOP 1 dr.name, ISNULL(dr.vehicleInfo,'') AS vehicleInfo, " +
                              "COUNT(a.assignmentId) AS assignmentCount, " +
                              "SUM(CASE WHEN d.status = 'Delivered' THEN 1 ELSE 0 END) AS completedDeliveries " +
                              "FROM Driver dr " +
                              "INNER JOIN PickupAssignment a ON dr.driverId = a.driverId " +
                              "INNER JOIN NGORepresentative r ON a.repId = r.repId " +
                              "LEFT JOIN Donation d ON a.donationId = d.donationId " +
                              "WHERE a.assignedAt >= ? AND r.ngoId = ? " +
                              "GROUP BY dr.driverId, dr.name, dr.vehicleInfo " +
                              "ORDER BY completedDeliveries DESC, assignmentCount DESC";
            PreparedStatement psDriver = conn.prepareStatement(driverSql);
            psDriver.setTimestamp(1, Timestamp.valueOf(startDate));
            psDriver.setInt(2, ngoId);
            ResultSet rsDriver = psDriver.executeQuery();
            if (rsDriver.next()) {
                topDriver.put("name", rsDriver.getString("name"));
                topDriver.put("vehicleInfo", rsDriver.getString("vehicleInfo"));
                topDriver.put("assignmentCount", rsDriver.getInt("assignmentCount"));
                topDriver.put("completedDeliveries", rsDriver.getInt("completedDeliveries"));
            }
            rsDriver.close();
            psDriver.close();
            
            // Save report to database
            String insert = "INSERT INTO SustainabilityReport (repId,period,totalMealsSaved,successRate)" +
                            " VALUES (?,?,?,?)";
            PreparedStatement ins = conn.prepareStatement(insert);
            ins.setInt(1, repId);
            ins.setString(2, period);
            ins.setInt(3, meals);
            ins.setDouble(4, rate);
            ins.executeUpdate();
            ins.close();
            
            return new ReportData(total, delivered, meals, rate, topDonors, topDriver, period);
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC07 — VIEW AVAILABLE DONATIONS  (READ + FILTER)
    // ══════════════════════════════════════════════════════════════════════════


    public List<DonationModel> getAllPendingDonations() {
        return getPendingDonationsFiltered("", "", "urgency");
    }

    /**
     * Returns Pending donations with optional filtering.
     * @param locationFilter  substring match on location (empty = no filter)
     * @param foodTypeFilter  substring match on foodType (empty = no filter)
     * @param sortBy          "urgency" (default) or "expiry"
     */
    public List<DonationModel> getPendingDonationsFiltered(
            String locationFilter, String foodTypeFilter, String sortBy) {

        List<DonationModel> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT d.*, " + URGENCY_EXPR + " AS urgencyScore " +
            "FROM Donation d " +
            "WHERE d.status='Pending'");

        boolean filterLoc  = locationFilter != null && !locationFilter.isBlank();
        boolean filterFood = foodTypeFilter != null && !foodTypeFilter.isBlank();
        if (filterLoc)  sql.append(" AND d.location  LIKE ?");
        if (filterFood) sql.append(" AND d.foodType  LIKE ?");

        if ("expiry".equalsIgnoreCase(sortBy))
            sql.append(" ORDER BY d.expiryTime ASC");
        else
            sql.append(" ORDER BY urgencyScore DESC");

        Connection conn = DBConnection.connect();
        if (conn == null) return list;
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (filterLoc)  ps.setString(idx++, "%" + locationFilter + "%");
            if (filterFood) ps.setString(idx++, "%" + foodTypeFilter + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapDonationRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return list;
    }

    /** Returns full details for a single donation (UC07 drill-down). */
    public DonationModel getDonationDetails(int donationId) {
        String sql = "SELECT d.*, " + URGENCY_EXPR + " AS urgencyScore " +
                     "FROM Donation d WHERE d.donationId=?";
        Connection conn = DBConnection.connect();
        if (conn == null) return null;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapDonationRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC08 — AUTO MATCH DONATION WITH NGO  (CORE MATCHING ENGINE)
    // ══════════════════════════════════════════════════════════════════════════

    private void autoMatchNGO(Connection conn, int donationId, int quantity, String donationLocation) {
        String sql =
            "SELECT TOP 1 " +
            "  n.ngoId, n.name AS ngoName, n.operationalArea, n.capacity, " +
            "  r.repId " +
            "FROM NGO n " +
            "INNER JOIN NGORepresentative r ON r.ngoId = n.ngoId " +
            "WHERE n.capacity >= ? " +
            "ORDER BY " +
            "  CASE WHEN n.operationalArea LIKE ? THEN 0 ELSE 1 END ASC, " +
            "  ABS(n.capacity - ?) ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            String locToken = "%" + donationLocation.split("[,\\s]+")[0].trim() + "%";
            ps.setString(2, locToken);
            ps.setInt(3, quantity);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("[UC08] No matching NGO found for donation #" + donationId);
                return;
            }

            int repId = rs.getInt("repId");

            // Create PickupAssignment with NO driver (driverId = NULL)
            String assignSql =
                "INSERT INTO PickupAssignment (donationId, driverId, repId, status) " +
                "VALUES (?, NULL, ?, 'Assigned')";
            int assignmentId = -1;
            try (PreparedStatement pa = conn.prepareStatement(assignSql, Statement.RETURN_GENERATED_KEYS)) {
                pa.setInt(1, donationId);
                pa.setInt(2, repId);
                pa.executeUpdate();
                ResultSet keys = pa.getGeneratedKeys();
                if (keys.next()) assignmentId = keys.getInt(1);
            }

            // Create DeliveryRecord (driverId=NULL, starts as Pending)
            if (assignmentId > 0) {
                String drSql =
                    "INSERT INTO DeliveryRecord (assignmentId, driverId, deliveryStatus) " +
                    "VALUES (?, NULL, 'Pending')";
                try (PreparedStatement dr = conn.prepareStatement(drSql)) {
                    dr.setInt(1, assignmentId);
                    dr.executeUpdate();
                }
            }

            // Update donation status → Assigned
            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE Donation SET status='Assigned' WHERE donationId=?")) {
                upd.setInt(1, donationId);
                upd.executeUpdate();
            }

            // Notify NGO rep
            String ngoName = rs.getString("ngoName");
            sendNotification(conn, repId, donationId,
                "New donation auto-matched to " + ngoName + ". Please review, accept and assign a driver.",
                "NGORep");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC09 — ACCEPT / REJECT ASSIGNMENT  (NGO DECISION)
    // ══════════════════════════════════════════════════════════════════════════

    public boolean acceptDonation(int donationId, int repId, int ngoId) {
        Connection conn = DBConnection.connect();
        if (conn == null) { System.err.println("[acceptDonation] No DB connection"); return false; }
        try {
            // Verify donation is still Pending
            String checkSql = "SELECT status FROM Donation WHERE donationId=?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, donationId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { System.err.println("[acceptDonation] Donation not found: " + donationId); return false; }
                String st = rs.getString("status");
                if (!"Pending".equals(st)) { System.err.println("[acceptDonation] Donation not Pending, is: " + st); return false; }
            }

            // Create PickupAssignment with NULL driver
            String assignSql =
                "INSERT INTO PickupAssignment (donationId, driverId, repId, status) " +
                "VALUES (?, NULL, ?, 'Assigned')";
            int assignmentId = -1;
            try (PreparedStatement pa = conn.prepareStatement(assignSql, Statement.RETURN_GENERATED_KEYS)) {
                pa.setInt(1, donationId);
                pa.setInt(2, repId);
                pa.executeUpdate();
                ResultSet keys = pa.getGeneratedKeys();
                if (keys.next()) assignmentId = keys.getInt(1);
                System.out.println("[acceptDonation] Created assignment #" + assignmentId);
            }

            // Create DeliveryRecord (driverId=NULL, status=Pending)
            if (assignmentId > 0) {
                String drSql =
                    "INSERT INTO DeliveryRecord (assignmentId, driverId, deliveryStatus) " +
                    "VALUES (?, NULL, 'Pending')";
                try (PreparedStatement dr = conn.prepareStatement(drSql)) {
                    dr.setInt(1, assignmentId);
                    dr.executeUpdate();
                }
            }

            // Update donation status -> Assigned
            runUpdate(conn, "UPDATE Donation SET status='Assigned' WHERE donationId=?", donationId);
            System.out.println("[acceptDonation] Donation #" + donationId + " accepted successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("[acceptDonation] SQL ERROR: " + e.getMessage());
            System.err.println("HINT: Run SFWRMS_AlterTables.sql to allow NULL driverId.");
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    public boolean rejectDonation(int donationId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try {
            // If there's an existing assignment, cancel it
            String cancelSql = "UPDATE PickupAssignment SET status='Cancelled' " +
                               "WHERE donationId=? AND status='Assigned'";
            try (PreparedStatement ps = conn.prepareStatement(cancelSql)) {
                ps.setInt(1, donationId);
                ps.executeUpdate();
            }
            // Keep donation as Pending for another NGO to pick up
            runUpdate(conn, "UPDATE Donation SET status='Pending' WHERE donationId=?", donationId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    /** Returns all active assignments belonging to this NGO (via repId). */
    public List<AssignmentModel> getAssignmentsForNGO(int ngoId) {
        List<AssignmentModel> list = new ArrayList<>();
        String sql =
            "SELECT pa.assignmentId, pa.donationId, ISNULL(pa.driverId,0) AS driverId, pa.repId, pa.status, pa.assignedAt, " +
            "       d.foodType, d.quantity, d.location AS donationLocation, " +
            "       d.status AS donationStatus, d.expiryTime, " +
            "       fd.name AS donorName, " +
            "       ISNULL(dr.name,'Not Assigned') AS driverName, ISNULL(dr.vehicleInfo,'') AS vehicleInfo, " +
            "       ISNULL(dlr.deliveryStatus,'Pending') AS deliveryStatus, " +
            "       dlr.pickedUpAt, dlr.deliveredAt, ISNULL(dlr.deliveryId,0) AS deliveryId " +
            "FROM PickupAssignment pa " +
            "INNER JOIN Donation   d   ON d.donationId   = pa.donationId " +
            "INNER JOIN FoodDonor  fd  ON fd.donorId     = d.donorId " +
            "LEFT  JOIN Driver     dr  ON dr.driverId    = pa.driverId " +
            "INNER JOIN NGORepresentative r ON r.repId   = pa.repId " +
            "LEFT  JOIN DeliveryRecord dlr ON dlr.assignmentId = pa.assignmentId " +
            "WHERE r.ngoId = ? AND pa.status IN ('Assigned','InProgress') " +
            "ORDER BY pa.assignedAt DESC";
        Connection conn = DBConnection.connect();
        if (conn == null) return list;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ngoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAssignmentRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return list;
    }

    /** Returns cancelled (rejected) assignments for this NGO — used to show 'Rejected' in donation view. */
    public List<AssignmentModel> getCancelledAssignmentsForNGO(int ngoId) {
        List<AssignmentModel> list = new ArrayList<>();
        String sql =
            "SELECT pa.assignmentId, pa.donationId, ISNULL(pa.driverId,0) AS driverId, pa.repId, pa.status, pa.assignedAt, " +
            "       d.foodType, d.quantity, d.location AS donationLocation, " +
            "       d.status AS donationStatus, d.expiryTime, " +
            "       fd.name AS donorName, " +
            "       ISNULL(dr.name,'') AS driverName, ISNULL(dr.vehicleInfo,'') AS vehicleInfo, " +
            "       'Cancelled' AS deliveryStatus, " +
            "       NULL AS pickedUpAt, NULL AS deliveredAt, 0 AS deliveryId " +
            "FROM PickupAssignment pa " +
            "INNER JOIN Donation   d   ON d.donationId   = pa.donationId " +
            "INNER JOIN FoodDonor  fd  ON fd.donorId     = d.donorId " +
            "LEFT  JOIN Driver     dr  ON dr.driverId    = pa.driverId " +
            "INNER JOIN NGORepresentative r ON r.repId   = pa.repId " +
            "WHERE r.ngoId = ? AND pa.status = 'Cancelled'";
        Connection conn = DBConnection.connect();
        if (conn == null) return list;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ngoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAssignmentRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return list;
    }

    public boolean rejectAssignment(int assignmentId, int donationId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try {
            // Cancel this assignment
            runUpdate(conn, "UPDATE PickupAssignment SET status='Cancelled' WHERE assignmentId=?",
                    assignmentId);
            // Reset donation to Pending
            runUpdate(conn, "UPDATE Donation SET status='Pending' WHERE donationId=?", donationId);

            // Fetch quantity + location to re-run matching
            String sql = "SELECT quantity, location FROM Donation WHERE donationId=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, donationId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    autoMatchNGO(conn, donationId, rs.getInt("quantity"), rs.getString("location"));
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC10 — ASSIGN / REASSIGN DRIVER  (NGO → DRIVER LINK)
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns drivers belonging to this NGO who are not on active assignments. */
    public List<DriverInfo> getAvailableDrivers(int ngoId) {
        List<DriverInfo> list = new ArrayList<>();
        String sql =
            "SELECT driverId, name, vehicleInfo, phone FROM Driver " +
            "WHERE ngoId = ? " +
            "AND driverId NOT IN (" +
            "    SELECT ISNULL(driverId,0) FROM PickupAssignment WHERE status IN ('Assigned','InProgress')" +
            ")";
        Connection conn = DBConnection.connect();
        if (conn == null) return list;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ngoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new DriverInfo(
                    rs.getInt("driverId"), rs.getString("name"),
                    rs.getString("vehicleInfo"), rs.getString("phone")));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return list;
    }

    public boolean assignDriver(int assignmentId, int driverId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try {
            // Update assignment driver
            String updAssign =
                "UPDATE PickupAssignment SET driverId=? WHERE assignmentId=? AND status='Assigned'";
            try (PreparedStatement ps = conn.prepareStatement(updAssign)) {
                ps.setInt(1, driverId);
                ps.setInt(2, assignmentId);
                if (ps.executeUpdate() == 0) return false;
            }
            // Update delivery record driver
            String updDR =
                "UPDATE DeliveryRecord SET driverId=? WHERE assignmentId=?";
            try (PreparedStatement ps = conn.prepareStatement(updDR)) {
                ps.setInt(1, driverId);
                ps.setInt(2, assignmentId);
                ps.executeUpdate();
            }
            // Fetch NGO name and donation details to send notification
            String fetchSql =
                "SELECT pa.donationId, n.name AS ngoName, d2.foodType, d2.quantity " +
                "FROM PickupAssignment pa " +
                "INNER JOIN NGORepresentative r ON r.repId = pa.repId " +
                "INNER JOIN NGO n ON n.ngoId = r.ngoId " +
                "INNER JOIN Donation d2 ON d2.donationId = pa.donationId " +
                "WHERE pa.assignmentId=?";
            try (PreparedStatement ps = conn.prepareStatement(fetchSql)) {
                ps.setInt(1, assignmentId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String ngoName = rs.getString("ngoName");
                    String foodType = rs.getString("foodType");
                    int qty = rs.getInt("quantity");
                    sendNotification(conn, driverId, rs.getInt("donationId"),
                        ngoName + " has requested you for pickup assignment: " +
                        foodType + " (" + qty + " kg). Please mark pickup when collected.",
                        "Driver");
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    public boolean acceptAssignmentByDriver(int assignmentId, int driverId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try {
            // Verify this assignment belongs to this driver and is Assigned
            String checkSql = "SELECT pa.donationId, pa.repId FROM PickupAssignment pa " +
                              "WHERE pa.assignmentId=? AND pa.driverId=? AND pa.status='Assigned'";
            int donationId = -1, repId = -1;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, assignmentId);
                ps.setInt(2, driverId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return false;
                donationId = rs.getInt("donationId");
                repId = rs.getInt("repId");
            }
            // Notify NGO rep that driver accepted
            sendNotification(conn, repId, donationId,
                "Driver has accepted the pickup assignment #" + assignmentId + ". Pickup in progress.",
                "NGORep");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC11 — UPDATE PICKUP STATUS  (DRIVER MARKS PICKED UP → InTransit)
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns all active assignments for a specific driver. */
    public List<AssignmentModel> getDriverAssignments(int driverId) {
        List<AssignmentModel> list = new ArrayList<>();
        String sql =
            "SELECT pa.assignmentId, pa.donationId, ISNULL(pa.driverId,0) AS driverId, pa.repId, pa.status, pa.assignedAt, " +
            "       d.foodType, d.quantity, d.location AS donationLocation, " +
            "       d.status AS donationStatus, d.expiryTime, " +
            "       fd.name AS donorName, " +
            "       ISNULL(dr.name,'') AS driverName, ISNULL(dr.vehicleInfo,'') AS vehicleInfo, " +
            "       ISNULL(dlr.deliveryStatus,'Pending') AS deliveryStatus, " +
            "       dlr.pickedUpAt, dlr.deliveredAt, ISNULL(dlr.deliveryId,0) AS deliveryId " +
            "FROM PickupAssignment pa " +
            "INNER JOIN Donation   d   ON d.donationId  = pa.donationId " +
            "INNER JOIN FoodDonor  fd  ON fd.donorId    = d.donorId " +
            "LEFT  JOIN Driver     dr  ON dr.driverId   = pa.driverId " +
            "LEFT  JOIN DeliveryRecord dlr ON dlr.assignmentId = pa.assignmentId " +
            "WHERE pa.driverId=? AND pa.status IN ('Assigned','InProgress') " +
            "ORDER BY pa.assignedAt DESC";
        Connection conn = DBConnection.connect();
        if (conn == null) return list;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAssignmentRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return list;
    }

    public boolean markPickedUp(int assignmentId, int donationId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try {
            runUpdate(conn,
                "UPDATE Donation SET status='InTransit' WHERE donationId=?", donationId);
            runUpdate(conn,
                "UPDATE PickupAssignment SET status='InProgress' WHERE assignmentId=?", assignmentId);
            // Update DeliveryRecord
            String updDR =
                "UPDATE DeliveryRecord SET deliveryStatus='InTransit', pickedUpAt=GETDATE() " +
                "WHERE assignmentId=?";
            runUpdate(conn, updDR, assignmentId);

            // Notify NGO rep
            String repSql =
                "SELECT repId FROM PickupAssignment WHERE assignmentId=?";
            try (PreparedStatement ps = conn.prepareStatement(repSql)) {
                ps.setInt(1, assignmentId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    sendNotification(conn, rs.getInt("repId"), donationId,
                        "Pickup confirmed by driver. Donation is now InTransit.", "NGORep");
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UC12 — CONFIRM DELIVERY  (FINAL STEP)
    // ══════════════════════════════════════════════════════════════════════════

    public boolean confirmDelivery(int assignmentId, int donationId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return false;
        try {
            // Validate: assignment must be InProgress
            String checkSql = "SELECT pa.repId, d.donorId " +
                               "FROM PickupAssignment pa " +
                               "INNER JOIN Donation d ON d.donationId=pa.donationId " +
                               "WHERE pa.assignmentId=? AND pa.status='InProgress'";
            int repId = -1, donorId = -1;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, assignmentId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return false;
                repId   = rs.getInt("repId");
                donorId = rs.getInt("donorId");
            }

            // Update all statuses
            runUpdate(conn,
                "UPDATE Donation SET status='Delivered' WHERE donationId=?", donationId);
            runUpdate(conn,
                "UPDATE PickupAssignment SET status='Completed' WHERE assignmentId=?", assignmentId);
            String updDR =
                "UPDATE DeliveryRecord SET deliveryStatus='Delivered', deliveredAt=GETDATE() " +
                "WHERE assignmentId=?";
            runUpdate(conn, updDR, assignmentId);

            // Notify NGO rep + donor (UC12 post-processing)
            sendNotification(conn, repId, donationId,
                "Delivery confirmed! Food has arrived at the NGO.", "NGORep");
            sendNotification(conn, donorId, donationId,
                "Your donation has been delivered successfully. Thank you!", "Donor");

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════════════════

    public List<NotificationModel> getNotifications(int recipientId, String recipientType) {
        List<NotificationModel> list = new ArrayList<>();
        String sql = "SELECT * FROM Notification " +
                     "WHERE recipientId=? AND recipientType=? " +
                     "ORDER BY sentAt DESC";
        Connection conn = DBConnection.connect();
        if (conn == null) return list;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            ps.setString(2, recipientType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NotificationModel n = new NotificationModel();
                n.setNotificationId(rs.getInt("notificationId"));
                n.setRecipientId(rs.getInt("recipientId"));
                n.setDonationId(rs.getInt("donationId"));
                n.setMessage(rs.getString("message"));
                n.setRecipientType(rs.getString("recipientType"));
                Timestamp ts = rs.getTimestamp("sentAt");
                if (ts != null) n.setSentAt(ts.toLocalDateTime());
                n.setRead(rs.getBoolean("isRead"));
                list.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
        return list;
    }

    public void markNotificationRead(int notificationId) {
        Connection conn = DBConnection.connect();
        if (conn == null) return;
        try {
            runUpdate(conn, "UPDATE Notification SET isRead=1 WHERE notificationId=?", notificationId);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private double calculateUrgencyScore(LocalDateTime expiryTime) {
        long minutes = Duration.between(LocalDateTime.now(), expiryTime).toMinutes();
        if (minutes <= 0) return 100.0;
        if (minutes >= 2880) return 5.0; // 48 hours
        return Math.round((100.0 - (minutes * 95.0 / 2880.0)) * 10.0) / 10.0;
    }

    private void storeUrgencyScore(Connection conn, int donationId, LocalDateTime expiryTime) {
        double score = calculateUrgencyScore(expiryTime);
        try {
            // Try update first, then insert if no row exists
            String upd = "UPDATE UrgencyScore SET score=?, calculatedAt=GETDATE() WHERE donationId=?";
            try (PreparedStatement ps = conn.prepareStatement(upd)) {
                ps.setDouble(1, score);
                ps.setInt(2, donationId);
                if (ps.executeUpdate() == 0) {
                    String ins = "INSERT INTO UrgencyScore (donationId, score) VALUES (?, ?)";
                    try (PreparedStatement pi = conn.prepareStatement(ins)) {
                        pi.setInt(1, donationId);
                        pi.setDouble(2, score);
                        pi.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            // UrgencyScore table may not exist — not critical since we compute dynamically
            System.err.println("[storeUrgencyScore] " + e.getMessage());
        }
    }

    private void sendNotification(Connection conn, int recipientId, int donationId,
                                  String message, String recipientType) {
        String sql = "INSERT INTO Notification (recipientId,donationId,message,recipientType) " +
                     "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            ps.setInt(2, donationId);
            ps.setString(3, message);
            ps.setString(4, recipientType);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int runUpdate(Connection conn, String sql, int param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            return ps.executeUpdate();
        }
    }

    private DonationModel mapDonationRow(ResultSet rs) throws SQLException {
        DonationModel d = new DonationModel();
        d.setDonationId(rs.getInt("donationId"));
        d.setDonorId(rs.getInt("donorId"));
        d.setFoodType(rs.getString("foodType"));
        d.setQuantity(rs.getInt("quantity"));
        // Read quantityUnit safely — column may not exist in older schemas
        try { d.setQuantityUnit(rs.getString("quantityUnit")); } catch (SQLException ignored) {}
        Timestamp prep = rs.getTimestamp("preparationTime");
        if (prep != null) d.setPreparationTime(prep.toLocalDateTime());
        Timestamp exp = rs.getTimestamp("expiryTime");
        if (exp != null) d.setExpiryTime(exp.toLocalDateTime());
        d.setLocation(rs.getString("location"));
        d.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("createdAt");
        if (created != null) d.setCreatedAt(created.toLocalDateTime());
        d.setUrgencyScore(rs.getDouble("urgencyScore"));
        return d;
    }

    private AssignmentModel mapAssignmentRow(ResultSet rs) throws SQLException {
        AssignmentModel a = new AssignmentModel();
        a.setAssignmentId(rs.getInt("assignmentId"));
        a.setDonationId(rs.getInt("donationId"));
        a.setDriverId(rs.getInt("driverId"));
        a.setRepId(rs.getInt("repId"));
        a.setStatus(rs.getString("status"));
        Timestamp at = rs.getTimestamp("assignedAt");
        if (at != null) a.setAssignedAt(at.toLocalDateTime());
        a.setFoodType(rs.getString("foodType"));
        a.setQuantity(rs.getInt("quantity"));
        a.setDonationLocation(rs.getString("donationLocation"));
        a.setDonationStatus(rs.getString("donationStatus"));
        Timestamp exp = rs.getTimestamp("expiryTime");
        if (exp != null) a.setExpiryTime(exp.toLocalDateTime());
        a.setDonorName(rs.getString("donorName"));
        a.setDriverName(rs.getString("driverName"));
        a.setVehicleInfo(rs.getString("vehicleInfo"));
        a.setDeliveryStatus(rs.getString("deliveryStatus"));
        Timestamp pu = rs.getTimestamp("pickedUpAt");
        if (pu != null) a.setPickedUpAt(pu.toLocalDateTime());
        Timestamp da = rs.getTimestamp("deliveredAt");
        if (da != null) a.setDeliveredAt(da.toLocalDateTime());
        a.setDeliveryId(rs.getInt("deliveryId"));
        return a;
    }

    private void closeQuietly(Connection conn) {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }
}
