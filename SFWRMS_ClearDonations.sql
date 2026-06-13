-- ══════════════════════════════════════════════════════════════════════════════
--  SFWRMS — Clear All Previous Donation Records
--  Run this in SSMS to wipe donation data while keeping user accounts intact.
-- ══════════════════════════════════════════════════════════════════════════════

USE SFWRMS;
GO

-- ── Delete child tables first (respects foreign key order) ──────────────────
DELETE FROM Notification;
DELETE FROM SustainabilityReport;
DELETE FROM DeliveryRecord;
DELETE FROM PickupAssignment;
DELETE FROM UrgencyScore;
DELETE FROM Donation;

-- ── Reset identity counters back to 1 ──────────────────────────────────────
DBCC CHECKIDENT ('Notification', RESEED, 0);
DBCC CHECKIDENT ('SustainabilityReport', RESEED, 0);
DBCC CHECKIDENT ('DeliveryRecord', RESEED, 0);
DBCC CHECKIDENT ('PickupAssignment', RESEED, 0);
DBCC CHECKIDENT ('UrgencyScore', RESEED, 0);
DBCC CHECKIDENT ('Donation', RESEED, 0);

PRINT '═══════════════════════════════════════════════════════';
PRINT '  All donation records cleared successfully!';
PRINT '  User accounts (Donors, NGOs, Drivers) are untouched.';
PRINT '═══════════════════════════════════════════════════════';
GO
