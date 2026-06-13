package com.sfwrms.sfwrms;

import java.time.LocalDateTime;

/** Represents a PickupAssignment row joined with Donation and Driver data. */
public class AssignmentModel {
    private int           assignmentId;
    private int           donationId;
    private int           driverId;
    private int           repId;
    private String        status;          // Assigned | InProgress | Completed | Cancelled
    private LocalDateTime assignedAt;

    // ── From Donation (joined) ─────────────────────────────────────────────────
    private String        foodType;
    private int           quantity;
    private String        donationLocation;
    private String        donationStatus;
    private LocalDateTime expiryTime;
    private String        donorName;

    // ── From Driver (joined) ──────────────────────────────────────────────────
    private String        driverName;
    private String        vehicleInfo;

    // ── From DeliveryRecord (joined) ──────────────────────────────────────────
    private String        deliveryStatus;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private int           deliveryId;

    public AssignmentModel() {}

    public int           getAssignmentId()       { return assignmentId; }
    public void          setAssignmentId(int v)  { assignmentId = v; }
    public int           getDonationId()         { return donationId; }
    public void          setDonationId(int v)    { donationId = v; }
    public int           getDriverId()           { return driverId; }
    public void          setDriverId(int v)      { driverId = v; }
    public int           getRepId()              { return repId; }
    public void          setRepId(int v)         { repId = v; }
    public String        getStatus()             { return status; }
    public void          setStatus(String v)     { status = v; }
    public LocalDateTime getAssignedAt()         { return assignedAt; }
    public void          setAssignedAt(LocalDateTime v) { assignedAt = v; }

    public String        getFoodType()           { return foodType; }
    public void          setFoodType(String v)   { foodType = v; }
    public int           getQuantity()           { return quantity; }
    public void          setQuantity(int v)      { quantity = v; }
    public String        getDonationLocation()   { return donationLocation; }
    public void          setDonationLocation(String v) { donationLocation = v; }
    public String        getDonationStatus()     { return donationStatus; }
    public void          setDonationStatus(String v)   { donationStatus = v; }
    public LocalDateTime getExpiryTime()         { return expiryTime; }
    public void          setExpiryTime(LocalDateTime v) { expiryTime = v; }
    public String        getDonorName()          { return donorName; }
    public void          setDonorName(String v)  { donorName = v; }

    public String        getDriverName()         { return driverName; }
    public void          setDriverName(String v) { driverName = v; }
    public String        getVehicleInfo()        { return vehicleInfo; }
    public void          setVehicleInfo(String v){ vehicleInfo = v; }

    public String        getDeliveryStatus()     { return deliveryStatus; }
    public void          setDeliveryStatus(String v)   { deliveryStatus = v; }
    public LocalDateTime getPickedUpAt()         { return pickedUpAt; }
    public void          setPickedUpAt(LocalDateTime v){ pickedUpAt = v; }
    public LocalDateTime getDeliveredAt()        { return deliveredAt; }
    public void          setDeliveredAt(LocalDateTime v){ deliveredAt = v; }
    public int           getDeliveryId()         { return deliveryId; }
    public void          setDeliveryId(int v)    { deliveryId = v; }
}
