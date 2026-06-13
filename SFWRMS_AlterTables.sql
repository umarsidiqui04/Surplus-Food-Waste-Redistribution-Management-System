-- Run this AFTER creating the main tables.
-- Makes driverId nullable so assignments can be created before a driver is selected.

-- 1. Drop FK + NOT NULL on PickupAssignment.driverId
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name LIKE '%PickupAssignment%driverId%' OR parent_object_id = OBJECT_ID('PickupAssignment'))
BEGIN
    DECLARE @fk1 NVARCHAR(200);
    SELECT TOP 1 @fk1 = fk.name FROM sys.foreign_keys fk 
    INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    INNER JOIN sys.columns c ON fkc.parent_column_id = c.column_id AND fkc.parent_object_id = c.object_id
    WHERE fk.parent_object_id = OBJECT_ID('PickupAssignment') AND c.name = 'driverId';
    IF @fk1 IS NOT NULL EXEC('ALTER TABLE PickupAssignment DROP CONSTRAINT [' + @fk1 + ']');
END
GO
ALTER TABLE PickupAssignment ALTER COLUMN driverId INT NULL;
GO
ALTER TABLE PickupAssignment ADD CONSTRAINT FK_PA_Driver FOREIGN KEY (driverId) REFERENCES Driver(driverId);
GO

-- 2. Drop FK + NOT NULL on DeliveryRecord.driverId
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('DeliveryRecord'))
BEGIN
    DECLARE @fk2 NVARCHAR(200);
    SELECT TOP 1 @fk2 = fk.name FROM sys.foreign_keys fk 
    INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    INNER JOIN sys.columns c ON fkc.parent_column_id = c.column_id AND fkc.parent_object_id = c.object_id
    WHERE fk.parent_object_id = OBJECT_ID('DeliveryRecord') AND c.name = 'driverId';
    IF @fk2 IS NOT NULL EXEC('ALTER TABLE DeliveryRecord DROP CONSTRAINT [' + @fk2 + ']');
END
GO
ALTER TABLE DeliveryRecord ALTER COLUMN driverId INT NULL;
GO
ALTER TABLE DeliveryRecord ADD CONSTRAINT FK_DR_Driver FOREIGN KEY (driverId) REFERENCES Driver(driverId);
GO

-- 3. Drop CHECK constraints that might block new status values
DECLARE @ck NVARCHAR(200);
SELECT TOP 1 @ck = cc.name FROM sys.check_constraints cc WHERE cc.parent_object_id = OBJECT_ID('PickupAssignment') AND cc.definition LIKE '%status%';
IF @ck IS NOT NULL EXEC('ALTER TABLE PickupAssignment DROP CONSTRAINT [' + @ck + ']');
GO
DECLARE @ck2 NVARCHAR(200);
SELECT TOP 1 @ck2 = cc.name FROM sys.check_constraints cc WHERE cc.parent_object_id = OBJECT_ID('DeliveryRecord') AND cc.definition LIKE '%deliveryStatus%';
IF @ck2 IS NOT NULL EXEC('ALTER TABLE DeliveryRecord DROP CONSTRAINT [' + @ck2 + ']');
GO

PRINT 'ALTER complete — driverId is now nullable, CHECK constraints removed.';
GO
