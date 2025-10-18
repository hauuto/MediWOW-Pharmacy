--Author: To Thanh Hau--

-- Trigger to auto-generate Staff ID based on role --
CREATE SEQUENCE dbo.ManagerSeg AS INT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.PharmacistSeg AS INT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.OtherStaffSeg AS INT START WITH 1 INCREMENT BY 1;

CREATE TRIGGER trg_Staff_AutoID_ByRole On Staff
INSTEAD OF INSERT
    AS
    BEGIN
        SET NOCOUNT ON;
        DECLARE @CurrenYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

        INSERT INTO Staff(id, username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive, role)
        SELECT
            CASE i.role
                WHEN 'MANAGER' THEN 'MAN' + @CurrenYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.ManagerSeg AS NVARCHAR(4)),4)
                WHEN 'PHARMACIST' THEN 'PHA' + @CurrenYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.PharmacistSeg AS NVARCHAR(4)),4)
                ELSE 'OTH' + @CurrenYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.OtherStaffSeg AS NVARCHAR(4)),4)
            END,
            i.username,
            i.password,
            i.fullName,
            i.licenseNumber,
            i.phoneNumber,
            i.email,
            i.hireDate,
            i.isActive,
            i.role
        FROM inserted i;
    END;



