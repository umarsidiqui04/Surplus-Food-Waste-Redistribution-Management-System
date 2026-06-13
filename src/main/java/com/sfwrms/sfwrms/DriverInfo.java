package com.sfwrms.sfwrms;

/** Lightweight DTO for driver selection table. All IDs are int. */
public class DriverInfo {
    private int    driverId;
    private String name;
    private String vehicleInfo;
    private String phone;

    public DriverInfo() {}

    public DriverInfo(int driverId, String name, String vehicleInfo, String phone) {
        this.driverId    = driverId;
        this.name        = name;
        this.vehicleInfo = vehicleInfo;
        this.phone       = phone;
    }

    public int    getDriverId()        { return driverId; }
    public void   setDriverId(int v)   { driverId = v; }
    public String getName()            { return name; }
    public void   setName(String v)    { name = v; }
    public String getVehicleInfo()     { return vehicleInfo; }
    public void   setVehicleInfo(String v) { vehicleInfo = v; }
    public String getPhone()           { return phone; }
    public void   setPhone(String v)   { phone = v; }

    @Override
    public String toString() {
        return name + " — " + vehicleInfo + " (" + phone + ")";
    }
}
