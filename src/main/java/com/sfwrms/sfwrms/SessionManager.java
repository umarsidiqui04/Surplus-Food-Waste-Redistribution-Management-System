package com.sfwrms.sfwrms;


public class SessionManager {

    private static User currentUser;

    private SessionManager() {}

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void clear() { currentUser = null; }

    public static boolean isLoggedIn() { return currentUser != null; }

    public static int getUserId() {
        return currentUser != null ? currentUser.getId() : 0;
    }

    public static String getUserName() {
        return currentUser != null ? currentUser.getName() : "";
    }

    public static String getUserRole() {
        return currentUser != null ? currentUser.getRole() : "";
    }

    /** Returns ngoId for NGORep or DriverUser, 0 otherwise. */
    public static int getNgoId() {
        if (currentUser instanceof NGORep rep) return rep.getNgoId();
        if (currentUser instanceof DriverUser d) return d.getNgoId();
        return 0;
    }

    /** Returns repId for NGORep users, 0 otherwise. */
    public static int getRepId() {
        return (currentUser instanceof NGORep) ? currentUser.getId() : 0;
    }

    /** Returns driverId for DriverUser, 0 otherwise. */
    public static int getDriverId() {
        return (currentUser instanceof DriverUser) ? currentUser.getId() : 0;
    }
}
