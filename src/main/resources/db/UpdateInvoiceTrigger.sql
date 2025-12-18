-- Update Invoice trigger to include shift column
-- Run this script to fix the trigger

DROP TRIGGER IF EXISTS trg_Invoice_AutoID;
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

