

-- Step 1: Create and use the database
CREATE DATABASE SFWRMS;
GO
USE SFWRMS;
GO


CREATE TABLE FoodDonor (
    donorId           NVARCHAR(50)  PRIMARY KEY,
    name              NVARCHAR(100) NOT NULL,
    email             NVARCHAR(100) NOT NULL UNIQUE,
    phone             NVARCHAR(20),
    organizationName  NVARCHAR(150),
    passwordHash      NVARCHAR(255) NOT NULL,
    createdAt         DATETIME      DEFAULT GETDATE()
);


CREATE TABLE NGO (
    ngoId           NVARCHAR(50)  PRIMARY KEY,
    name            NVARCHAR(150) NOT NULL,
    location        NVARCHAR(200) NOT NULL,
    capacity        INT           NOT NULL DEFAULT 0,
    operationalArea NVARCHAR(200),
    contactEmail    NVARCHAR(100),
    createdAt       DATETIME      DEFAULT GETDATE()
);

CREATE TABLE NGORepresentative (
    repId        NVARCHAR(50)  PRIMARY KEY,
    name         NVARCHAR(100) NOT NULL,
    email        NVARCHAR(100) NOT NULL UNIQUE,
    phone        NVARCHAR(20),
    passwordHash NVARCHAR(255) NOT NULL,
    ngoId        NVARCHAR(50)  NOT NULL,
    FOREIGN KEY (ngoId) REFERENCES NGO(ngoId)
);

CREATE TABLE Driver (
    driverId     NVARCHAR(50)  PRIMARY KEY,
    name         NVARCHAR(100) NOT NULL,
    vehicleInfo  NVARCHAR(150),
    phone        NVARCHAR(20),
    passwordHash NVARCHAR(255) NOT NULL,
    ngoId        NVARCHAR(50)  NOT NULL,
    FOREIGN KEY (ngoId) REFERENCES NGO(ngoId)
);


CREATE TABLE Donation (
    donationId      NVARCHAR(50)  PRIMARY KEY,
    donorId         NVARCHAR(50)  NOT NULL,
    foodType        NVARCHAR(100) NOT NULL,
    quantity        INT           NOT NULL CHECK (quantity > 0),
    preparationTime DATETIME      NOT NULL,
    expiryTime      DATETIME      NOT NULL,
    location        NVARCHAR(200) NOT NULL,
    status          NVARCHAR(30)  NOT NULL DEFAULT 'Pending'
                    CHECK (status IN ('Pending','Assigned','InTransit','Delivered','Cancelled')),
    createdAt       DATETIME      DEFAULT GETDATE(),
    FOREIGN KEY (donorId) REFERENCES FoodDonor(donorId)
);

CREATE TABLE UrgencyScore (
    scoreId      NVARCHAR(50) PRIMARY KEY,
    donationId   NVARCHAR(50) NOT NULL UNIQUE,  -- UNIQUE = one score per donation
    score        FLOAT        NOT NULL,
    calculatedAt DATETIME     DEFAULT GETDATE(),
    FOREIGN KEY (donationId) REFERENCES Donation(donationId)
);


CREATE TABLE PickupAssignment (
    assignmentId NVARCHAR(50) PRIMARY KEY,
    donationId   NVARCHAR(50) NOT NULL,
    driverId     NVARCHAR(50) NOT NULL,
    repId        NVARCHAR(50) NOT NULL,
    assignedAt   DATETIME     DEFAULT GETDATE(),
    status       NVARCHAR(30) NOT NULL DEFAULT 'Assigned'
                 CHECK (status IN ('Assigned','InProgress','Completed','Cancelled')),
    FOREIGN KEY (donationId) REFERENCES Donation(donationId),
    FOREIGN KEY (driverId)   REFERENCES Driver(driverId),
    FOREIGN KEY (repId)      REFERENCES NGORepresentative(repId)
);


CREATE TABLE DeliveryRecord (
    deliveryId     NVARCHAR(50) PRIMARY KEY,
    assignmentId   NVARCHAR(50) NOT NULL UNIQUE,  -- one record per assignment
    driverId       NVARCHAR(50) NOT NULL,
    pickedUpAt     DATETIME,
    deliveredAt    DATETIME,
    deliveryStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending'
                   CHECK (deliveryStatus IN ('Pending','InTransit','Delivered','Failed')),
    FOREIGN KEY (assignmentId) REFERENCES PickupAssignment(assignmentId),
    FOREIGN KEY (driverId)     REFERENCES Driver(driverId)
);


CREATE TABLE Notification (
    notificationId NVARCHAR(50)  PRIMARY KEY,
    recipientId    NVARCHAR(50)  NOT NULL,
    donationId     NVARCHAR(50),
    message        NVARCHAR(500) NOT NULL,
    recipientType  NVARCHAR(30)  NOT NULL
                   CHECK (recipientType IN ('Donor','NGORep','Driver')),
    sentAt         DATETIME      DEFAULT GETDATE(),
    isRead         BIT           DEFAULT 0,
    FOREIGN KEY (donationId) REFERENCES Donation(donationId)
);


CREATE TABLE SustainabilityReport (
    reportId       NVARCHAR(50)  PRIMARY KEY,
    repId          NVARCHAR(50)  NOT NULL,
    period         NVARCHAR(20)  NOT NULL
                   CHECK (period IN ('Weekly','Monthly','Yearly')),
    totalMealsSaved INT          NOT NULL DEFAULT 0,
    successRate    FLOAT         NOT NULL DEFAULT 0.0,
    generatedAt    DATETIME      DEFAULT GETDATE(),
    FOREIGN KEY (repId) REFERENCES NGORepresentative(repId)
);

SELECT * 
FROM DeliveryRecord

SELECT * 
FROM Donation

SELECT * 
FROM Driver

SELECT * 
FROM FoodDonor

SELECT * 
FROM NGO

SELECT * 
FROM NGORepresentative

SELECT * 
FROM Notification

SELECT * 
FROM PickupAssignment

SELECT * 
FROM SustainabilityReport   

SELECT * 
FROM UrgencyScore

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

SELECT * 
FROM Donation