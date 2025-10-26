-- Script to remove PrescribedCustomer table and all its foreign key references
-- Author: AI Assistant
-- Date: October 26, 2025

USE MediWOW
GO

-- Step 1: Drop the foreign key constraint in Invoice table that references PrescribedCustomer
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK__Invoice__prescri__5EBF139D' OR parent_object_id = OBJECT_ID('Invoice') AND referenced_object_id = OBJECT_ID('PrescribedCustomer'))
BEGIN
    -- Find the actual foreign key name dynamically
    DECLARE @fkName NVARCHAR(255)
    SELECT @fkName = name
    FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID('Invoice')
    AND referenced_object_id = OBJECT_ID('PrescribedCustomer')

    IF @fkName IS NOT NULL
    BEGIN
        DECLARE @sql NVARCHAR(MAX) = 'ALTER TABLE Invoice DROP CONSTRAINT ' + @fkName
        EXEC sp_executesql @sql
        PRINT 'Dropped foreign key constraint: ' + @fkName
    END
END
GO

-- Step 2: Drop the index on prescribedCustomer column in Invoice table
IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_invoice_prescribedCustomer' AND object_id = OBJECT_ID('Invoice'))
BEGIN
    DROP INDEX idx_invoice_prescribedCustomer ON Invoice
    PRINT 'Dropped index: idx_invoice_prescribedCustomer'
END
GO

-- Step 3: Set the prescribedCustomer column to NULL for all existing records (optional, for data preservation)
-- You can skip this step if you want to just drop the column
UPDATE Invoice SET prescribedCustomer = NULL WHERE prescribedCustomer IS NOT NULL
PRINT 'Set all prescribedCustomer values to NULL in Invoice table'
GO

-- Step 4: Drop the prescribedCustomer column from Invoice table
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Invoice') AND name = 'prescribedCustomer')
BEGIN
    ALTER TABLE Invoice DROP COLUMN prescribedCustomer
    PRINT 'Dropped column: prescribedCustomer from Invoice table'
END
GO

-- Step 5: Drop the PrescribedCustomer table
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'PrescribedCustomer')
BEGIN
    DROP TABLE PrescribedCustomer
    PRINT 'Dropped table: PrescribedCustomer'
END
GO

PRINT 'Successfully removed PrescribedCustomer table and all its references'
GO

