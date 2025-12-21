-- Author: To Thanh Hau --

--DROP DATABASE IF EXISTS MediWOW--

CREATE DATABASE MediWOW
GO
USE MediWOW

-- 1. Staff Table
CREATE TABLE Staff (
                       id NVARCHAR(50) PRIMARY KEY,
                       username NVARCHAR(255) NOT NULL UNIQUE,
                       password NVARCHAR(255) NOT NULL,
                       fullName NVARCHAR(255) NOT NULL,
                       licenseNumber NVARCHAR(100),
                       phoneNumber NVARCHAR(10),
                       email NVARCHAR(255),
                       hireDate DATE NOT NULL,
                       isActive BIT NOT NULL DEFAULT 1,
                       role NVARCHAR(50) NOT NULL CHECK (role IN ('MANAGER', 'PHARMACIST')),
                       isFirstLogin BIT NOT NULL DEFAULT 1,
                       mustChangePassword BIT NOT NULL DEFAULT 1
);

-- 2. Shift Table
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
                       workstation NVARCHAR(100),
                       closedBy NVARCHAR(50),
                       closeReason NVARCHAR(500),

                       FOREIGN KEY (staff) REFERENCES Staff(id),
                       FOREIGN KEY (closedBy) REFERENCES Staff(id)
);

-- 3. Customer Table
CREATE TABLE PrescribedCustomer (
                                    id NVARCHAR(50) PRIMARY KEY,
                                    name NVARCHAR(255) NOT NULL,
                                    phoneNumber NVARCHAR(20),
                                    address NVARCHAR(500),
                                    creationDate DATETIME NOT NULL DEFAULT GETDATE()
);

-- 4. Product Table
CREATE TABLE Product (
                         id NVARCHAR(50) PRIMARY KEY,
                         barcode NVARCHAR(100) UNIQUE,
                         category NVARCHAR(50) NOT NULL CHECK (category IN ('SUPPLEMENT', 'OTC', 'ETC')),
                         form NVARCHAR(50) NOT NULL CHECK (form IN ('SOLID', 'LIQUID_DOSAGE', 'LIQUID_ORAL_DOSAGE')),
                         name NVARCHAR(255) NOT NULL,
                         shortName NVARCHAR(100),
                         manufacturer NVARCHAR(255),
                         activeIngredient NVARCHAR(500),
                         vat DECIMAL(5, 2) NOT NULL DEFAULT 0, -- Sửa thành Decimal (VD: 10.00)
                         strength NVARCHAR(100),
                         description NVARCHAR(MAX),
                         baseUnitOfMeasure NVARCHAR(50),
                         image NVARCHAR(500),
                         creationDate DATETIME NOT NULL DEFAULT GETDATE(),
                         updateDate DATETIME
);

-- 5. MeasurementName Table (Dictionary for UOM names)
CREATE TABLE MeasurementName (
                                 id INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
                                 name NVARCHAR(100) NOT NULL UNIQUE
);

-- 6. UnitOfMeasure Table
-- Primary Key: (product, name) -> Composite Key
CREATE TABLE UnitOfMeasure (
                               product NVARCHAR(50) NOT NULL,
                               measurementId INT NOT NULL,
                               price DECIMAL(18,2) NOT NULL,
                               baseUnitConversionRate DECIMAL(18,4) NOT NULL,

                               FOREIGN KEY (product) REFERENCES Product(id) ON DELETE CASCADE,
                               FOREIGN KEY (measurementId) REFERENCES MeasurementName(id) ON DELETE CASCADE,
                               PRIMARY KEY (product, measurementId)
);

-- 7. Lot Table
CREATE TABLE Lot (
                     id NVARCHAR(50) PRIMARY KEY,
                     batchNumber NVARCHAR(50),
                     product NVARCHAR(50) NOT NULL,
                     quantity INT NOT NULL,
                     rawPrice DECIMAL(18,2) NOT NULL, -- Giá vốn (Cost)
                     expiryDate DATE NOT NULL,
                     status NVARCHAR(50) NOT NULL CHECK (status IN ('AVAILABLE', 'EXPIRED', 'FAULTY')),

                     FOREIGN KEY (product) REFERENCES Product(id) ON DELETE CASCADE
);

-- 8. Promotion Table
CREATE TABLE Promotion (
                           id NVARCHAR(50) PRIMARY KEY,
                           name NVARCHAR(255) NOT NULL,
                           description NVARCHAR(MAX),
                           creationDate DATETIME NOT NULL DEFAULT GETDATE(),
                           effectiveDate DATETIME NOT NULL,
                           endDate DATETIME NOT NULL,
                           isActive BIT NOT NULL DEFAULT 1
);

-- 9. PromotionCondition Table
CREATE TABLE PromotionCondition (
                                    id NVARCHAR(50) PRIMARY KEY,
                                    promotion NVARCHAR(50) NOT NULL,
                                    type NVARCHAR(50) NOT NULL,
                                    comparator NVARCHAR(50) NOT NULL,
                                    target NVARCHAR(50) NOT NULL,
                                    value DECIMAL(18,2),
                                    product NVARCHAR(50),
                                    unitOfMeasure INT,

                                    FOREIGN KEY (promotion) REFERENCES Promotion(id),
                                    FOREIGN KEY (product, unitOfMeasure)
                                        REFERENCES UnitOfMeasure(product, measurementId)
);

-- 10. PromotionAction Table
CREATE TABLE PromotionAction (
                                 id NVARCHAR(50) PRIMARY KEY,
                                 promotion NVARCHAR(50) NOT NULL,
                                 actionOrder INT NOT NULL,
                                 type NVARCHAR(50) NOT NULL,
                                 target NVARCHAR(50) NOT NULL,
                                 value DECIMAL(18,2),
                                 product NVARCHAR(50),
                                 unitOfMeasure INT,

                                 FOREIGN KEY (promotion) REFERENCES Promotion(id),
                                 FOREIGN KEY (product, unitOfMeasure)
                                     REFERENCES UnitOfMeasure(product, measurementId)
);

-- 11. Invoice Table
CREATE TABLE Invoice (
                         id NVARCHAR(50) PRIMARY KEY,
                         type NVARCHAR(50) NOT NULL CHECK (type IN ('SALES', 'RETURN', 'EXCHANGE')),
                         creationDate DATETIME NOT NULL DEFAULT GETDATE(),
                         creator NVARCHAR(50) NOT NULL,
                         prescribedCustomer NVARCHAR(50),
                         prescriptionCode NVARCHAR(100),
                         referencedInvoice NVARCHAR(50),
                         promotion NVARCHAR(50),
                         paymentMethod NVARCHAR(50) NOT NULL CHECK (paymentMethod IN ('CASH', 'BANK_TRANSFER')),
                         notes NVARCHAR(MAX),
                         shift NVARCHAR(50),

                         FOREIGN KEY (creator) REFERENCES Staff(id),
                         FOREIGN KEY (prescribedCustomer) REFERENCES PrescribedCustomer(id),
                         FOREIGN KEY (referencedInvoice) REFERENCES Invoice(id),
                         FOREIGN KEY (promotion) REFERENCES Promotion(id),
                         FOREIGN KEY (shift) REFERENCES Shift(id)
);

-- 12. InvoiceLine Table (Display & Pricing Info)
-- SỬA ĐỔI LỚN: Thêm ID và bỏ Composite PK để LotAllocation tham chiếu được
CREATE TABLE InvoiceLine (
                             id NVARCHAR(50) PRIMARY KEY, -- ID riêng cho dòng này
                             invoice NVARCHAR(50) NOT NULL,
                             product NVARCHAR(50) NOT NULL,
                             unitOfMeasure NVARCHAR(100) NOT NULL, -- Chỉ lưu tên (phần 'name' của UOM)
                             quantity INT NOT NULL,
                             unitPrice DECIMAL(18,2) NOT NULL, -- Giá snapshot tại thời điểm bán
                             lineType NVARCHAR(50) NOT NULL CHECK (lineType IN ('SALE', 'RETURN', 'EXCHANGE_OUT', 'EXCHANGE_IN')),

                             FOREIGN KEY (invoice) REFERENCES Invoice(id) ON DELETE CASCADE,
                             FOREIGN KEY (product) REFERENCES Product(id),

    -- Khóa ngoại phức hợp trỏ về bảng UnitOfMeasure(product, measurementId)
                             FOREIGN KEY (product, unitOfMeasure) REFERENCES UnitOfMeasure(product, measurementId)
);

-- 13. LotAllocation Table (Inventory Control) -> MỚI HOÀN TOÀN
CREATE TABLE LotAllocation (
                               id NVARCHAR(50) PRIMARY KEY,
                               invoiceLine NVARCHAR(50) NOT NULL, -- Tham chiếu tới dòng hóa đơn
                               lot NVARCHAR(50) NOT NULL,         -- Tham chiếu tới Lô
                               quantity INT NOT NULL,             -- Số lượng lấy từ lô này

                               FOREIGN KEY (invoiceLine) REFERENCES InvoiceLine(id) ON DELETE CASCADE,
                               FOREIGN KEY (lot) REFERENCES Lot(id)
);

-- Indexes for performance
CREATE INDEX idx_staff_username ON Staff(username);
CREATE INDEX idx_staff_role ON Staff(role);
CREATE INDEX idx_product_barcode ON Product(barcode);
CREATE INDEX idx_product_category ON Product(category);
CREATE INDEX idx_lot_product ON Lot(product);
CREATE INDEX idx_lot_expiry ON Lot(expiryDate); -- Index date để tìm lô FIFO nhanh
CREATE INDEX idx_invoice_type ON Invoice(type);
CREATE INDEX idx_invoice_creationDate ON Invoice(creationDate);
CREATE INDEX idx_invoiceline_invoice ON InvoiceLine(invoice);
CREATE INDEX idx_allocation_invoiceline ON LotAllocation(invoiceLine);
CREATE INDEX idx_allocation_lot ON LotAllocation(lot);