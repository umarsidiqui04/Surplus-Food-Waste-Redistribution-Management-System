-- ═══════════════════════════════════════════════════════════════
--  Add quantityUnit column to Donation table
--  Run this ONCE against your SFWRMS database.
--  This column stores whether quantity is in 'kg' or 'pieces'.
-- ═══════════════════════════════════════════════════════════════

IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Donation' AND COLUMN_NAME = 'quantityUnit'
)
BEGIN
    ALTER TABLE Donation ADD quantityUnit VARCHAR(20) NOT NULL DEFAULT 'kg';
    PRINT 'Column quantityUnit added to Donation table.';
END
ELSE
BEGIN
    PRINT 'Column quantityUnit already exists.';
END
