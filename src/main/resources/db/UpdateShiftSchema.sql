-- Update Shift table to support workstation tracking and close audit
-- Run this script to update existing database schema

PRINT 'Starting Shift table schema update...';
PRINT '';

-- Add workstation column
PRINT '1. Adding workstation column...';
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'Shift') AND name = 'workstation')
BEGIN
    ALTER TABLE Shift ADD workstation NVARCHAR(100) NULL;
    PRINT '   ✓ Column "workstation" added successfully';
END
ELSE
BEGIN
    PRINT '   - Column "workstation" already exists';
END

-- Add closedBy column (foreign key to Staff)
PRINT '2. Adding closedBy column...';
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'Shift') AND name = 'closedBy')
BEGIN
    ALTER TABLE Shift ADD closedBy NVARCHAR(50) NULL;
    PRINT '   ✓ Column "closedBy" added successfully';

    -- Add foreign key constraint
    IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_Shift_ClosedBy')
    BEGIN
        ALTER TABLE Shift ADD CONSTRAINT FK_Shift_ClosedBy FOREIGN KEY (closedBy) REFERENCES Staff(id);
        PRINT '   ✓ Foreign key constraint added';
    END
END
ELSE
BEGIN
    PRINT '   - Column "closedBy" already exists';
END

-- Add closeReason column
PRINT '3. Adding closeReason column...';
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'Shift') AND name = 'closeReason')
BEGIN
    ALTER TABLE Shift ADD closeReason NVARCHAR(500) NULL;
    PRINT '   ✓ Column "closeReason" added successfully';
END
ELSE
BEGIN
    PRINT '   - Column "closeReason" already exists';
END

PRINT '';
PRINT '========================================';
PRINT 'UPDATING TRIGGER';
PRINT '========================================';
PRINT '';

-- Drop existing trigger
PRINT '4. Updating trigger trg_Shift_AutoID...';
IF OBJECT_ID('trg_Shift_AutoID', 'TR') IS NOT NULL
BEGIN
    DROP TRIGGER trg_Shift_AutoID;
    PRINT '   ✓ Dropped existing trigger';
END

GO

-- Recreate trigger with new columns
CREATE TRIGGER trg_Shift_AutoID ON Shift
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO Shift (id, staff, startTime, endTime, startCash, endCash, systemCash, status, notes, workstation, closedBy, closeReason)
    SELECT
        'SHI' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.ShiftSeg AS NVARCHAR(4)), 4),
        staff,
        startTime,
        endTime,
        startCash,
        endCash,
        systemCash,
        status,
        notes,
        workstation,
        closedBy,
        closeReason
    FROM inserted;
END
GO

PRINT '   ✓ Trigger recreated with new columns';
PRINT '';
PRINT '========================================';
PRINT 'UPDATE COMPLETED SUCCESSFULLY!';
PRINT '========================================';
PRINT '';
PRINT 'Summary:';
PRINT '- workstation column: Ready';
PRINT '- closedBy column: Ready';
PRINT '- closeReason column: Ready';
PRINT '- Trigger trg_Shift_AutoID: Updated';
PRINT '';
PRINT 'You can now restart the application.';


