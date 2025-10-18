--Author: To Thanh Hau--

CREATE DATABASE MediWOW
GO
USE MediWOW

-- Staff Table
CREATE TABLE Staff (
                       id NVARCHAR(50) PRIMARY KEY,
                       username NVARCHAR(255) NOT NULL UNIQUE,
                       password NVARCHAR(255) NOT NULL,
                       fullName NVARCHAR(255) NOT NULL,
                       licenseNumber NVARCHAR(100),
                       phoneNumber NVARCHAR(20),
                       email NVARCHAR(255),
                       hireDate DATE NOT NULL,
                       isActive BIT NOT NULL DEFAULT 1,
                       role NVARCHAR(50) NOT NULL CHECK (role IN ('MANAGER', 'PHARMACIST'))
);

-- PrescribedCustomer Table
CREATE TABLE PrescribedCustomer (
                                    id NVARCHAR(50) PRIMARY KEY,
                                    name NVARCHAR(255) NOT NULL,
                                    phoneNumber NVARCHAR(20),
                                    address NVARCHAR(500),
                                    creationDate DATETIME NOT NULL DEFAULT GETDATE()
);

-- Product Table
CREATE TABLE Product (
                         id NVARCHAR(50) PRIMARY KEY,
                         barcode NVARCHAR(100) UNIQUE,
                         category NVARCHAR(50) NOT NULL CHECK (category IN ('SUPPLEMENT', 'OTC', 'ETC')),
                         form NVARCHAR(50) NOT NULL CHECK (form IN ('SOLID', 'LIQUID_DOSAGE', 'LIQUID_ORAL_DOSAGE')),
                         name NVARCHAR(255) NOT NULL,
                         shortName NVARCHAR(100),
                         manufacturer NVARCHAR(255),
                         activeIngredient NVARCHAR(500),
                         vat FLOAT NOT NULL DEFAULT 0,
                         strength NVARCHAR(100),
                         description NVARCHAR(MAX),
                         baseUnitOfMeasure NVARCHAR(50),
                         creationDate DATETIME NOT NULL DEFAULT GETDATE(),
                         updateDate DATETIME
);

-- UnitOfMeasure Table (Weak Entity - Composition with Product)
-- Primary Key: (id)
CREATE TABLE UnitOfMeasure (
                               id NVARCHAR(50) NOT NULL PRIMARY KEY,
                               product NVARCHAR(50) NOT NULL,
                               name NVARCHAR(100) NOT NULL,
                               baseUnitConversionRate FLOAT NOT NULL,
                               FOREIGN KEY (product) REFERENCES Product(id) ON DELETE CASCADE
);

-- Lot Table (Composition with Product)
CREATE TABLE Lot (
                     batchNumber NVARCHAR(50) PRIMARY KEY,
                     product NVARCHAR(50) NOT NULL,
                     quantity INT NOT NULL,
                     mwPrice FLOAT NOT NULL,
                     expiryDate DATETIME NOT NULL,
                     status NVARCHAR(50) NOT NULL CHECK (status IN ('AVAILABLE', 'EXPIRED', 'FAULTY')),
                     FOREIGN KEY (product) REFERENCES Product(id) ON DELETE CASCADE
);



-- Promotion Table
CREATE TABLE Promotion (
                           id NVARCHAR(50) PRIMARY KEY,
                           name NVARCHAR(255) NOT NULL,
                           description NVARCHAR(MAX),
                           creationDate DATETIME NOT NULL DEFAULT GETDATE(),
                           effectiveDate DATETIME NOT NULL,
                           endDate DATETIME NOT NULL,
                           isActive BIT NOT NULL DEFAULT 1
);

-- PromotionCondition Table (Weak Entity - Composition with Promotion)
-- Primary Key: (id)
CREATE TABLE PromotionCondition (
                                    id NVARCHAR(50) NOT NULL PRIMARY KEY,
                                    promotion NVARCHAR(50) NOT NULL,
                                    type NVARCHAR(50) NOT NULL CHECK (type IN ('PRODUCT_ID', 'PRODUCT_QTY', 'ORDER_SUBTOTAL')),
                                    comparator NVARCHAR(50) NOT NULL CHECK (comparator IN ('GREATER_EQUAL', 'LESS_EQUAL', 'GREATER', 'LESS', 'EQUAL', 'BETWEEN')),
                                    target NVARCHAR(50) NOT NULL CHECK (target IN ('PRODUCT', 'ORDER_SUBTOTAL')),
                                    primaryValue FLOAT NOT NULL,
                                    secondaryValue FLOAT,
                                    product NVARCHAR(50),
                                    FOREIGN KEY (promotion) REFERENCES Promotion(id) ON DELETE CASCADE,
                                    FOREIGN KEY (product) REFERENCES Product(id)
);

-- PromotionAction Table (Weak Entity - Composition with Promotion)
-- Primary Key: (id)
CREATE TABLE PromotionAction (
                                 id NVARCHAR(50) NOT NULL PRIMARY KEY,
                                 promotion NVARCHAR(50) NOT NULL,
                                 actionOrder INT NOT NULL,
                                 type NVARCHAR(50) NOT NULL CHECK (type IN ('PERCENT_DISCOUNT', 'FIXED_DISCOUNT', 'PRODUCT_GIFT')),
                                 target NVARCHAR(50) NOT NULL CHECK (target IN ('PRODUCT', 'ORDER_SUBTOTAL')),
                                 primaryValue FLOAT NOT NULL,
                                 secondaryValue FLOAT,
                                 product NVARCHAR(50),
                                 FOREIGN KEY (promotion) REFERENCES Promotion(id) ON DELETE CASCADE,
                                 FOREIGN KEY (product) REFERENCES Product(id)
);
-- Invoice Table (contains both product and payment information)
CREATE TABLE Invoice (
                         id NVARCHAR(50) PRIMARY KEY,
                         type NVARCHAR(50) NOT NULL CHECK (type IN ('SALES', 'RETURN', 'EXCHANGE')),
                         creationDate DATETIME NOT NULL DEFAULT GETDATE(),
                         creator NVARCHAR(50) NOT NULL,
                         prescribedCustomer NVARCHAR(50),
                         prescriptionCode NVARCHAR(100),
                         referencedInvoice NVARCHAR(50), -- For RETURN/EXCHANGE to reference original invoice
                         promotion NVARCHAR(50),
                         paymentMethod NVARCHAR(50) NOT NULL CHECK (paymentMethod IN ('CASH', 'BANK_TRANSFER')),
                         notes NVARCHAR(MAX),
                         FOREIGN KEY (creator) REFERENCES Staff(id),
                         FOREIGN KEY (prescribedCustomer) REFERENCES PrescribedCustomer(id),
                         FOREIGN KEY (referencedInvoice) REFERENCES Invoice(id),
                         FOREIGN KEY (promotion) REFERENCES Promotion(id)
);

-- InvoiceLine Table (Weak Entity - Composition with Invoice)
-- Primary Key: (invoice, product, unitOfMeasure, lineType)
CREATE TABLE InvoiceLine (
                             invoice NVARCHAR(50) NOT NULL,
                             product NVARCHAR(50) NOT NULL,
                             quantity INT NOT NULL,
                             unitOfMeasure NVARCHAR(50) NOT NULL,
                             unitPrice DECIMAL(18,2) NOT NULL,
                             lineType NVARCHAR(50) NOT NULL CHECK (lineType IN ('SALE', 'RETURN', 'EXCHANGE_OUT', 'EXCHANGE_IN')),
                             PRIMARY KEY (invoice, product, unitOfMeasure, lineType),
                             FOREIGN KEY (invoice) REFERENCES Invoice(id) ON DELETE CASCADE,
                             FOREIGN KEY (product) REFERENCES Product(id),
                             FOREIGN KEY (unitOfMeasure) REFERENCES UnitOfMeasure(id)
);
-- Indexes for better performance
CREATE INDEX idx_staff_username ON Staff(username);
CREATE INDEX idx_staff_role ON Staff(role);
CREATE INDEX idx_product_barcode ON Product(barcode);
CREATE INDEX idx_product_category ON Product(category);
CREATE INDEX idx_product_name ON Product(name);
CREATE INDEX idx_lot_product ON Lot(product);
CREATE INDEX idx_lot_status ON Lot(status);
CREATE INDEX idx_invoice_type ON Invoice(type);
CREATE INDEX idx_invoice_creator ON Invoice(creator);
CREATE INDEX idx_invoice_creationDate ON Invoice(creationDate);
CREATE INDEX idx_invoice_prescribedCustomer ON Invoice(prescribedCustomer);
CREATE INDEX idx_invoice_referencedInvoice ON Invoice(referencedInvoice);
CREATE INDEX idx_invoiceline_invoice ON InvoiceLine(invoice);
CREATE INDEX idx_invoiceline_product ON InvoiceLine(product);
CREATE INDEX idx_invoiceline_lineType ON InvoiceLine(lineType);
CREATE INDEX idx_unitofmeasure_product ON UnitOfMeasure(product);
CREATE INDEX idx_promotioncondition_promotion ON PromotionCondition(promotion);
CREATE INDEX idx_promotionaction_promotion ON PromotionAction(promotion);
CREATE INDEX idx_promotion_effectiveDate ON Promotion(effectiveDate);
CREATE INDEX idx_promotion_endDate ON Promotion(endDate);
CREATE INDEX idx_promotion_isActive ON Promotion(isActive);



