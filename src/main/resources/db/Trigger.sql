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
            role NVARCHAR(50)
                                     );

        INSERT INTO @StaffToInsert(username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive, role)
        SELECT
            username,
            password,
            fullName,
            licenseNumber,
            phoneNumber,
            email,
            hireDate,
            isActive,
            role
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

        INSERT INTO Staff (id, username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive, role)
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
            role
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



-- Trigger/Function to auto-generate Product ID --
-- Trigger/Function to auto-generate Unit ID --
-- Trigger/Function to auto-generate Lot ID --
-- Trigger/Function to auto-generate Promotion ID --
-- Trigger/Function to auto-generate PromotionCondition ID --
-- Trigger/Function to auto-generate PromotionAction ID --
-- Trigger/Function to auto-generate Invoice ID --



--
