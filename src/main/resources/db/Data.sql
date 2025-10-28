-- Generated Data for MediWOW Pharmacy System
-- Generated on: October 28, 2025
-- This data follows the schema and includes:
-- - 5 Products (2 OTC, 2 ETC, 1 Supplement)
-- - Multiple lots per product (some unavailable)
-- - Multiple UOMs per product
-- - 1 Staff (admin)
-- - 2 Promotions with conditions and actions
-- - 5 Invoices (2 with prescription codes)

USE MediWOW
GO

-- =====================================================
-- 1. STAFF DATA (Admin account)
-- =====================================================

-- Admin account (username: admin, password: admin)
-- This account will not be displayed in staff list on UI
INSERT INTO Staff (role, username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive)
VALUES ( 'MANAGER', 'admin', '$2a$12$vVXxXrKyAGhRge.lO0ihZ.0Nl7PghqZLqSpRwvpoDnC8qe3uZC1TK', N'Administrator', NULL, NULL, NULL, '2025-01-01', 1);

-- =====================================================
-- 2. PRODUCT DATA (2 OTC, 2 ETC, 1 Supplement)
-- =====================================================

-- Product 1: OTC - Paracetamol
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0001', '8934567890123', 'OTC', 'SOLID', N'Paracetamol 500mg', N'Para 500', N'Pymepharco', N'Paracetamol', 10, '500mg', N'Thuốc giảm đau, hạ sốt', N'Viên', '2025-01-15', '2025-01-15');

-- Product 2: OTC - Vitamin C
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0002', '8934567890124', 'OTC', 'LIQUID_DOSAGE', N'Vitamin C 1000mg Syrup', N'Vit C Syrup', N'DHG Pharma', N'Ascorbic Acid', 10, '1000mg/5ml', N'Tăng cường sức đề kháng dạng siro', N'ml', '2025-01-16', '2025-01-16');

-- Product 3: ETC - Amoxicillin
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0003', '8934567890125', 'ETC', 'SOLID', N'Amoxicillin 500mg', N'Amoxi 500', N'Traphaco', N'Amoxicillin trihydrate', 10, '500mg', N'Kháng sinh điều trị nhiễm khuẩn', N'Viên', '2025-01-17', '2025-01-17');

-- Product 4: ETC - Omeprazole
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0004', '8934567890126', 'ETC', 'LIQUID_DOSAGE', N'Omeprazole 20mg Suspension', N'Ome Susp', N'Stella', N'Omeprazole', 10, '20mg/5ml', N'Điều trị loét dạ dày dạng hỗn dịch', N'ml', '2025-01-18', '2025-01-18');

-- Product 5: Supplement - Omega 3
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0005', '8934567890127', 'SUPPLEMENT', 'SOLID', N'Omega 3 Fish Oil', N'Omega 3', N'Blackmores', N'Fish Oil, EPA, DHA', 10, '1000mg', N'Bổ sung Omega 3 cho tim mạch', N'Viên', '2025-01-19', '2025-01-19');

-- =====================================================
-- 3. UNIT OF MEASURE DATA
-- =====================================================

-- UOMs for Paracetamol
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0001', 'PRO2025-0001', N'Viên', 1);
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0002', 'PRO2025-0001', N'Vỉ', 0.1);
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0003', 'PRO2025-0001', N'Hộp', 0.01);

-- UOMs for Vitamin C
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0004', 'PRO2025-0002', N'ml', 1);
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0005', 'PRO2025-0002', N'Chai', 0.01);

-- UOMs for Amoxicillin
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0006', 'PRO2025-0003', N'Viên', 1);
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0007', 'PRO2025-0003', N'Vỉ', 0.1);
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0008', 'PRO2025-0003', N'Hộp', 0.01);

-- UOMs for Omeprazole
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0009', 'PRO2025-0004', N'ml', 1);
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0010', 'PRO2025-0004', N'Chai', 0.01);

-- UOMs for Omega 3
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0011', 'PRO2025-0005', N'Viên', 1);
INSERT INTO UnitOfMeasure (id, product, name, baseUnitConversionRate) VALUES ('UOM2025-0012', 'PRO2025-0005', N'Lọ', 0.0166667);

-- =====================================================
-- 4. LOT DATA (Each product has at least 2 lots, at least 2 lots are unavailable)
-- =====================================================

-- Lots for Paracetamol
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('PARA-2025-001', 'PRO2025-0001', 5000, 150, '2026-12-31', 'AVAILABLE');
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('PARA-2024-005', 'PRO2025-0001', 0, 140, '2025-01-15', 'EXPIRED');
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('PARA-2025-002', 'PRO2025-0001', 3000, 155, '2027-06-30', 'AVAILABLE');

-- Lots for Vitamin C
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('VITC-2025-001', 'PRO2025-0002', 2000, 300, '2026-10-31', 'AVAILABLE');
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('VITC-2025-002', 'PRO2025-0002', 1500, 280, '2027-03-31', 'AVAILABLE');

-- Lots for Amoxicillin
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('AMOX-2025-001', 'PRO2025-0003', 4000, 250, '2026-08-31', 'AVAILABLE');
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('AMOX-2024-012', 'PRO2025-0003', 100, 240, '2025-12-31', 'FAULTY');
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('AMOX-2025-002', 'PRO2025-0003', 3500, 260, '2027-01-31', 'AVAILABLE');

-- Lots for Omeprazole
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('OMEP-2025-001', 'PRO2025-0004', 3000, 400, '2026-11-30', 'AVAILABLE');
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('OMEP-2025-002', 'PRO2025-0004', 2500, 390, '2027-05-31', 'AVAILABLE');

-- Lots for Omega 3
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('OMEG-2025-001', 'PRO2025-0005', 1000, 500, '2026-09-30', 'AVAILABLE');
INSERT INTO Lot (batchNumber, product, quantity, mwPrice, expiryDate, status) VALUES ('OMEG-2025-002', 'PRO2025-0005', 800, 480, '2027-02-28', 'AVAILABLE');

-- =====================================================
-- 5. PROMOTION DATA
-- =====================================================

-- Promotion 1: Fixed and Percent Discount (Buy 2 Paracetamol boxes, get 10% off order and 20,000 VND fixed discount)
INSERT INTO Promotion (id, name, description, creationDate, effectiveDate, endDate, isActive)
VALUES ('PROM2025-001', N'Khuyến mãi Paracetamol - Giảm giá kép', N'Mua từ 2 hộp Paracetamol, giảm 10% tổng đơn và giảm thêm 20,000đ', '2025-01-20', '2025-01-20', '2025-12-31', 1);

-- Conditions for Promotion 1
INSERT INTO PromotionCondition (id, promotion, type, comparator, target, primaryValue, secondaryValue, product)
VALUES ('PRMC2025-001', 'PROM2025-001', 'PRODUCT_QTY', 'GREATER_EQUAL', 'PRODUCT', 2, NULL, 'PRO2025-0001');

-- Actions for Promotion 1
INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, primaryValue, secondaryValue, product)
VALUES ('PRMA2025-001', 'PROM2025-001', 1, 'PERCENT_DISCOUNT', 'ORDER_SUBTOTAL', 10, NULL, NULL);

INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, primaryValue, secondaryValue, product)
VALUES ('PRMA2025-002', 'PROM2025-001', 2, 'FIXED_DISCOUNT', 'ORDER_SUBTOTAL', 20000, NULL, NULL);

-- Promotion 2: Product Gift and Percent Discount (Get 1 Vitamin C free and 5% off for orders over 200.000 VND)
INSERT INTO Promotion (id, name, description, creationDate, effectiveDate, endDate, isActive)
VALUES ('PROM2025-002', N'Khuyến mãi hóa đơn - Tặng quà', N'Tặng 1 hộp Vitamin C và giảm 5% tổng đơn cho hóa đơn trên 200.000 VNĐ', '2025-01-21', '2025-01-21', '2025-12-31', 1);

-- Conditions for Promotion 2
INSERT INTO PromotionCondition (id, promotion, type, comparator, target, primaryValue, secondaryValue, product)
VALUES ('PRMC2025-003', 'PROM2025-002', 'ORDER_SUBTOTAL', 'GREATER_EQUAL', 'ORDER_SUBTOTAL', 200000, NULL, NULL);

-- Actions for Promotion 2
INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, primaryValue, secondaryValue, product)
VALUES ('PRMA2025-003', 'PROM2025-002', 1, 'PRODUCT_GIFT', 'PRODUCT', 1, NULL, 'PRO2025-0002');

INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, primaryValue, secondaryValue, product)
VALUES ('PRMA2025-004', 'PROM2025-002', 2, 'PERCENT_DISCOUNT', 'ORDER_SUBTOTAL', 5, NULL, NULL);

-- =====================================================
-- 6. INVOICE DATA (5 invoices, 2 with prescription codes)
-- =====================================================

-- Invoice 1: Sales with Promotion 1 and Prescription Code
INSERT INTO Invoice (id, type, creationDate, creator, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes)
VALUES ('INV2025-0001', 'SALES', '2025-01-22 09:30:00', 'MAN2025-0001', 'MW001a3b5c7d-C', NULL, 'PROM2025-001', 'CASH', N'Bán thuốc theo đơn, áp dụng khuyến mãi');

-- Invoice Lines for Invoice 1
INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0001', 'PRO2025-0001', 3, 'UOM2025-0003', 15000, 'SALE');

INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0001', 'PRO2025-0003', 20, 'UOM2025-0006', 250, 'SALE');

-- Invoice 2: Sales without Promotion
INSERT INTO Invoice (id, type, creationDate, creator, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes)
VALUES ('INV2025-0002', 'SALES', '2025-01-23 10:15:00', 'MAN2025-0001', NULL, NULL, NULL, 'BANK_TRANSFER', N'Khách hàng thanh toán chuyển khoản');

-- Invoice Lines for Invoice 2
INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0002', 'PRO2025-0002', 2, 'UOM2025-0005', 9000, 'SALE');

INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0002', 'PRO2025-0004', 14, 'UOM2025-0010', 5600, 'SALE');

-- Invoice 3: Sales without Promotion but with Prescription Code
INSERT INTO Invoice (id, type, creationDate, creator, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes)
VALUES ('INV2025-0003', 'SALES', '2025-01-24 14:20:00', 'MAN2025-0001', 'MW0019k2m4p6-C', NULL, NULL, 'CASH', N'Đơn thuốc bổ sung');

-- Invoice Lines for Invoice 3
INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0003', 'PRO2025-0005', 2, 'UOM2025-0012', 30000, 'SALE');

INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0003', 'PRO2025-0001', 50, 'UOM2025-0001', 150, 'SALE');

-- Invoice 4: Sales without Promotion
INSERT INTO Invoice (id, type, creationDate, creator, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes)
VALUES ('INV2025-0004', 'SALES', '2025-01-25 11:45:00', 'MAN2025-0001', NULL, NULL, NULL, 'CASH', N'Mua thuốc thông thường');

-- Invoice Lines for Invoice 4
INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0004', 'PRO2025-0003', 10, 'UOM2025-0007', 2500, 'SALE');

INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0004', 'PRO2025-0002', 1, 'UOM2025-0005', 9000, 'SALE');

INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0004', 'PRO2025-0005', 30, 'UOM2025-0011', 500, 'SALE');

-- Invoice 5: Sales without Promotion
INSERT INTO Invoice (id, type, creationDate, creator, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes)
VALUES ('INV2025-0005', 'SALES', '2025-01-26 16:00:00', 'MAN2025-0001', NULL, NULL, NULL, 'CASH', N'Khách hàng mua lẻ');

-- Invoice Lines for Invoice 5
INSERT INTO InvoiceLine (invoice, product, quantity, unitOfMeasure, unitPrice, lineType)
VALUES ('INV2025-0005', 'PRO2025-0001', 20, 'UOM2025-0001', 150, 'SALE');

GO

-- =====================================================
-- DATA GENERATION COMPLETE
-- =====================================================
PRINT 'Data generation completed successfully!';
PRINT '- 1 Staff (admin) created';
PRINT '- 5 Products created (2 OTC [1 LIQUID], 2 ETC [1 LIQUID], 1 Supplement)';
PRINT '- 12 UnitOfMeasures created (with reciprocal baseUnitConversionRate)';
PRINT '- 12 Lots created (2 unavailable: 1 EXPIRED, 1 FAULTY)';
PRINT '- 2 Promotions created (1 condition each)';
PRINT '- 5 SALES Invoices created (2 with prescription codes, 1 with promotion)';
GO
