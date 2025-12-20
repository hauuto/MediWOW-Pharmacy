-- Trigger to auto-generate Staff ID based on role --
-- Author: To Thanh Hau--
-- SEQUENCE to prevent ID conflicts among different roles, using when there are many inserts staff on one time --
CREATE SEQUENCE dbo.ManagerSeg AS INT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.PharmacistSeg AS INT START WITH 1 INCREMENT BY 1;
GO


CREATE TRIGGER trg_Staff_AutoID_ByRole On Staff
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @StaffToInsert TABLE (
                                     id NVARCHAR(50),
                                     username NVARCHAR(255),
                                     password NVARCHAR(255),
                                     fullName NVARCHAR(255),
                                     licenseNumber NVARCHAR(100),
                                     phoneNumber NVARCHAR(20),
                                     email NVARCHAR(255),
                                     hireDate DATE,
                                     isActive BIT,
                                     role NVARCHAR(50),
                                     isFirstLogin BIT,
                                     mustChangePassword BIT

                                 );

    INSERT INTO @StaffToInsert(username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive, role, isFirstLogin, mustChangePassword)
    SELECT
        username,
        password,
        fullName,
        licenseNumber,
        phoneNumber,
        email,
        hireDate,
        isActive,
        role,
        isFirstLogin,
        mustChangePassword

    FROM inserted;

    DECLARE @CurrenYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    -- Generate ID for MANAGER
    UPDATE @StaffToInsert
    SET id = 'MAN' + @CurrenYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.ManagerSeg AS NVARCHAR(4)),4)
    WHERE role = 'MANAGER';

    -- Generate ID for PHARMACIST
    UPDATE @StaffToInsert
    SET id = 'PHA' + @CurrenYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.PharmacistSeg AS NVARCHAR(4)),4)
    WHERE role = 'PHARMACIST';


    UPDATE @StaffToInsert
    SET username = CASE
                       WHEN LEFT(id, 3) = 'MAN' THEN 'quanly' + SUBSTRING(id, 6, 2) + RIGHT(id, 4)
                       WHEN LEFT(id, 3) = 'PHA' THEN 'nhanvien' + SUBSTRING(id, 6, 2) + RIGHT(id, 4)
                       ELSE username
        END
    WHERE username IS NULL OR username = '';

    INSERT INTO Staff (id, username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive, role, isFirstLogin, mustChangePassword)
    SELECT
        id,
        username,
        password,
        fullName,
        licenseNumber,
        phoneNumber,
        email,
        hireDate,
        isActive,
        role,
        isFirstLogin,
        mustChangePassword
    FROM @StaffToInsert;
END
GO


-- Add default ID to auto-generate Customer ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.CustomerSeg AS INT START WITH 1 INCREMENT BY 1;

ALTER TABLE PrescribedCustomer
    ADD CONSTRAINT DF_PrescribedCustomer_ID
        DEFAULT ('CUS' + CAST(YEAR(GETDATE()) AS NVARCHAR(4)) + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.CustomerSeg AS NVARCHAR(4)),4)) FOR id;
GO



-- Trigger to auto-generate Product ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.ProductSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_Product_AutoID ON Product
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, image, creationDate, updateDate)
    SELECT
        'PRO' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.ProductSeg AS NVARCHAR(4)), 4),
        barcode,
        category,
        form,
        name,
        shortName,
        manufacturer,
        activeIngredient,
        vat,
        strength,
        description,
        baseUnitOfMeasure,
        image,
        creationDate,
        updateDate
    FROM inserted;
END
GO


-- Trigger to auto-generate Lot ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.LotSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_Lot_AutoID ON Lot
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
    SELECT
        'LOT' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.LotSeg AS NVARCHAR(4)), 4),
        batchNumber,
        product,
        quantity,
        rawPrice,
        expiryDate,
        status
    FROM inserted;
END
GO


-- Trigger to auto-generate Promotion ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.PromotionSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_Promotion_AutoID ON Promotion
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO Promotion (id, name, description, creationDate, effectiveDate, endDate, isActive)
    SELECT
        'PMO' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.PromotionSeg AS NVARCHAR(4)), 4),
        name,
        description,
        creationDate,
        effectiveDate,
        endDate,
        isActive
    FROM inserted;
END
GO


-- Trigger to auto-generate PromotionCondition ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.PromotionConditionSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_PromotionCondition_AutoID ON PromotionCondition
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO PromotionCondition (id, promotion, type, comparator, target, value, product, unitOfMeasure)
    SELECT
        'PCO' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.PromotionConditionSeg AS NVARCHAR(4)), 4),
        promotion,
        type,
        comparator,
        target,
        value,
        product,
        unitOfMeasure
    FROM inserted;
END
GO


-- Trigger to auto-generate PromotionAction ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.PromotionActionSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_PromotionAction_AutoID ON PromotionAction
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, value, product, unitOfMeasure)
    SELECT
        'PAC' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.PromotionActionSeg AS NVARCHAR(4)), 4),
        promotion,
        actionOrder,
        type,
        target,
        value,
        product,
        unitOfMeasure
    FROM inserted;
END
GO


-- Trigger to auto-generate Invoice ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.InvoiceSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_Invoice_AutoID ON Invoice
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO Invoice (id, type, creationDate, creator, prescribedCustomer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
    SELECT
        'INV' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.InvoiceSeg AS NVARCHAR(4)), 4),
        type,
        creationDate,
        creator,
        prescribedCustomer,
        prescriptionCode,
        referencedInvoice,
        promotion,
        paymentMethod,
        notes,
        shift
    FROM inserted;
END
GO


-- Trigger to auto-generate InvoiceLine ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.InvoiceLineSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_InvoiceLine_AutoID ON InvoiceLine
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
    SELECT
        'ILN' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.InvoiceLineSeg AS NVARCHAR(4)), 4),
        invoice,
        product,
        unitOfMeasure,
        quantity,
        unitPrice,
        lineType
    FROM inserted;
END
GO


-- Trigger to auto-generate LotAllocation ID --
-- Author: To Thanh Hau --
CREATE SEQUENCE dbo.LotAllocationSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_LotAllocation_AutoID ON LotAllocation
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO LotAllocation (id, invoiceLine, lot, quantity)
    SELECT
        'LAL' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.LotAllocationSeg AS NVARCHAR(4)), 4),
        invoiceLine,
        lot,
        quantity
    FROM inserted;
END
GO


CREATE SEQUENCE dbo.ShiftSeg AS INT START WITH 1 INCREMENT BY 1;
GO

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