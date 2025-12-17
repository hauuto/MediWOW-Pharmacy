CREATE TABLE Shift (
                       id NVARCHAR(50) PRIMARY KEY,
                       staff NVARCHAR(50) NOT NULL,           -- Nhân viên trực ca
                       startTime DATETIME NOT NULL DEFAULT GETDATE(),
                       endTime DATETIME,                      -- Null nếu đang mở ca
                       startCash DECIMAL(18,2) NOT NULL,      -- Tiền mặt đầu ca (Nhập tay)
                       endCash DECIMAL(18,2),                 -- Tiền mặt thực tế đếm được khi kết ca (Nhập tay)
                       systemCash DECIMAL(18,2),              -- Tiền mặt hệ thống tính toán (Lưu lại để đối chiếu)
                       status NVARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
                       notes NVARCHAR(MAX),

                       FOREIGN KEY (staff) REFERENCES Staff(id)
);

-- 2. Thêm cột shift vào bảng Invoice
-- Lý do: Để biết hóa đơn này thuộc doanh thu của ca nào
ALTER TABLE Invoice
    ADD shift NVARCHAR(50);

ALTER TABLE Invoice
    ADD CONSTRAINT FK_Invoice_Shift
        FOREIGN KEY (shift) REFERENCES Shift(id);




CREATE SEQUENCE dbo.ShiftSeg AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TRIGGER trg_Shift_AutoID ON Shift
    INSTEAD OF INSERT
    AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));

    INSERT INTO Shift (id, staff, startTime, endTime, startCash, endCash, systemCash, status, notes)
    SELECT
        'SHI' + @CurrentYear + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.ShiftSeg AS NVARCHAR(4)), 4),
        staff,
        startTime,
        endTime,
        startCash,
        endCash,
        systemCash,
        status,
        notes
    FROM inserted;
END
GO