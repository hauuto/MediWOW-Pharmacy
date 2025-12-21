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
INSERT INTO Staff (role, username, password, fullName, licenseNumber, phoneNumber,
                   email, hireDate, isActive, isFirstLogin, mustChangePassword)
VALUES ( 'MANAGER', 'admin',
        '$2a$12$vVXxXrKyAGhRge.lO0ihZ.0Nl7PghqZLqSpRwvpoDnC8qe3uZC1TK',
        N'Administrator', NULL, NULL, NULL,
        '2025-01-01', 1,0,0);

-- insert into dbo.Staff (id, username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive, role, isFirstLogin, mustChangePassword)
-- values  (N'MAN2025-0005', N'admin', N'$2a$12$vVXxXrKyAGhRge.lO0ihZ.0Nl7PghqZLqSpRwvpoDnC8qe3uZC1TK', N'Administrator', null, null, null, N'2025-01-01', 1, N'MANAGER', 0, 0),
--         (N'PHA2025-0001', N'nhanvien250001', N'$2a$12$jdwZLPjlfqJxBT5NBI4hBuyoAeDUHGe7qGlAx13GoDjJBFN18iO/i', N'Tô Thanh Hậu', N'05495/CCHN-D-SYT-HNO', N'0868182546', N'thanhhau670@gmail.com', N'2025-12-17', 1, N'PHARMACIST', 0, 0),
--         (N'PHA2025-0002', N'nhanvien250002', N'$2a$12$Rvqmwr26PRsqdRPoQpC4fOH6rR8ygi.KAIx4Lj6gFSSpJsU2TyPye', N'Tô Tô', N'02495/CCHN-D-SYT-HNO', N'0234567890', N'hauuto.job@gmail.com', N'2025-12-18', 1, N'PHARMACIST', 0, 0);

-- =====================================================
-- 2. CUSTOMER DATA
-- =====================================================

INSERT INTO Customer (id, name, phoneNumber, address, creationDate)
VALUES ('CUS2025-0001', N'Nguyễn Văn An', '0901234567', N'123 Lê Lợi, Quận 1, TP.HCM', '2025-01-10');

INSERT INTO Customer (id, name, phoneNumber, address, creationDate)
VALUES ('CUS2025-0002', N'Trần Thị Bình', '0912345678', N'456 Nguyễn Huệ, Quận 3, TP.HCM', '2025-01-12');

INSERT INTO Customer (id, name, phoneNumber, address, creationDate)
VALUES ('CUS2025-0003', N'Lê Minh Châu', '0923456789', N'789 Trần Hưng Đạo, Quận 5, TP.HCM', '2025-01-15');

INSERT INTO Customer (id, name, phoneNumber, address, creationDate)
VALUES ('CUS2025-0004', N'Phạm Đức Dũng', '0934567890', N'321 Võ Văn Tần, Quận 10, TP.HCM', '2025-02-01');

INSERT INTO Customer (id, name, phoneNumber, address, creationDate)
VALUES ('CUS2025-0005', N'Hoàng Thị Em', '0945678901', NULL, '2025-02-15');

-- =====================================================
-- 3. PRODUCT DATA (2 OTC, 2 ETC, 1 Supplement)
-- =====================================================

-- Product 1: OTC - Paracetamol
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0001', '8934567890123', 'OTC', 'SOLID', N'Paracetamol 500mg', N'Para 500', N'Pymepharco', N'Paracetamol', 10.00, '500mg', N'Thuốc giảm đau, hạ sốt', N'Viên', '2025-01-15', '2025-01-15');

-- Product 2: OTC - Vitamin C
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0002', '8934567890124', 'OTC', 'LIQUID_DOSAGE', N'Vitamin C 1000mg Syrup', N'Vit C Syrup', N'DHG Pharma', N'Ascorbic Acid', 10.00, '1000mg/5ml', N'Tăng cường sức đề kháng dạng siro', N'ml', '2025-01-16', '2025-01-16');

-- Product 3: ETC - Amoxicillin
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0003', '8934567890125', 'ETC', 'SOLID', N'Amoxicillin 500mg', N'Amoxi 500', N'Traphaco', N'Amoxicillin trihydrate', 10.00, '500mg', N'Kháng sinh điều trị nhiễm khuẩn', N'Viên', '2025-01-17', '2025-01-17');

-- Product 4: ETC - Omeprazole
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0004', '8934567890126', 'ETC', 'LIQUID_DOSAGE', N'Omeprazole 20mg Suspension', N'Ome Susp', N'Stella', N'Omeprazole', 10.00, '20mg/5ml', N'Điều trị loét dạ dày dạng hỗn dịch', N'ml', '2025-01-18', '2025-01-18');

-- Product 5: Supplement - Omega 3
INSERT INTO Product (id, barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate)
VALUES ('PRO2025-0005', '8934567890127', 'SUPPLEMENT', 'SOLID', N'Omega 3 Fish Oil', N'Omega 3', N'Blackmores', N'Fish Oil, EPA, DHA', 10.00, '1000mg', N'Bổ sung Omega 3 cho tim mạch', N'Viên', '2025-01-19', '2025-01-19');

-- =====================================================
-- 4. MEASUREMENT NAME DATA (Dictionary for UOM names)
-- =====================================================

INSERT INTO MeasurementName (name)
VALUES (N'Viên'),(N'Vỉ'),(N'Hộp'),(N'ml'),(N'Chai'),(N'Lọ'),(N'Gói'),(N'Ống'),(N'Tuýp');

-- =====================================================
-- 5. UNIT OF MEASURE DATA
-- =====================================================

-- UOMs for Paracetamol (PRO2025-0001) - base unit: Viên
INSERT INTO UnitOfMeasure (product, measurementId, price, baseUnitConversionRate) VALUES
                                                                                      ('PRO2025-0001', 1, 200.00, 1.0000),   -- Viên
                                                                                      ('PRO2025-0001', 2, 2000.00, 0.1000),  -- Vỉ
                                                                                      ('PRO2025-0001', 3, 20000.00, 0.0100); -- Hộp

-- UOMs for Vitamin C (PRO2025-0002) - base unit: ml
INSERT INTO UnitOfMeasure (product, measurementId, price, baseUnitConversionRate) VALUES
                                                                                      ('PRO2025-0002', 4, 100.00, 1.0000),  -- ml
                                                                                      ('PRO2025-0002', 5, 10000.00, 0.0100);-- Chai

-- UOMs for Amoxicillin (PRO2025-0003) - base unit: Viên
INSERT INTO UnitOfMeasure (product, measurementId, price, baseUnitConversionRate) VALUES
                                                                                      ('PRO2025-0003', 1, 500.00, 1.0000),   -- Viên
                                                                                      ('PRO2025-0003', 2, 5000.00, 0.1000),  -- Vỉ
                                                                                      ('PRO2025-0003', 3, 50000.00, 0.0100); -- Hộp

-- UOMs for Omeprazole (PRO2025-0004) - base unit: ml
INSERT INTO UnitOfMeasure (product, measurementId, price, baseUnitConversionRate) VALUES
                                                                                      ('PRO2025-0004', 4, 200.00, 1.0000),   -- ml
                                                                                      ('PRO2025-0004', 5, 24000.00, 0.0083); -- Chai

-- UOMs for Omega 3 (PRO2025-0005) - base unit: Viên
INSERT INTO UnitOfMeasure (product, measurementId, price, baseUnitConversionRate) VALUES
                                                                                      ('PRO2025-0005', 1, 1500.00, 1.0000),   -- Viên
                                                                                      ('PRO2025-0005', 6, 90000.00, 0.0167);  -- Lọ

-- =====================================================
-- 6. LOT DATA (Each product has at least 2 lots, at least 2 lots are unavailable)
-- =====================================================

-- Lots for Paracetamol (PRO2025-0001)
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0001', 'PARA-2025-0001', 'PRO2025-0001', 5000, 120.00, '2026-12-31', 'AVAILABLE');
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0002', 'PARA-2024-0005', 'PRO2025-0001', 0, 110.00, '2025-01-15', 'EXPIRED');
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0003', 'PARA-2025-0002', 'PRO2025-0001', 3000, 125.00, '2027-06-30', 'AVAILABLE');

-- Lots for Vitamin C (PRO2025-0002)
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0004', 'VITC-2025-0001', 'PRO2025-0002', 20000, 70.00, '2026-10-31', 'AVAILABLE');
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0005', 'VITC-2025-0002', 'PRO2025-0002', 15000, 65.00, '2027-03-31', 'AVAILABLE');

-- Lots for Amoxicillin (PRO2025-0003)
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0006', 'AMOX-2025-0001', 'PRO2025-0003', 4000, 350.00, '2026-08-31', 'AVAILABLE');
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0007', 'AMOX-2024-012', 'PRO2025-0003', 100, 340.00, '2025-12-31', 'FAULTY');
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0008', 'AMOX-2025-0002', 'PRO2025-0003', 3500, 360.00, '2027-01-31', 'AVAILABLE');

-- Lots for Omeprazole (PRO2025-0004)
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0009', 'OMEP-2025-0001', 'PRO2025-0004', 30000, 150.00, '2026-11-30', 'AVAILABLE');
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0010', 'OMEP-2025-0002', 'PRO2025-0004', 25000, 145.00, '2027-05-31', 'AVAILABLE');

-- Lots for Omega 3 (PRO2025-0005)
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0011', 'OMEG-2025-0001', 'PRO2025-0005', 1000, 1000.00, '2026-09-30', 'AVAILABLE');
INSERT INTO Lot (id, batchNumber, product, quantity, rawPrice, expiryDate, status)
VALUES ('LOT2025-0012', 'OMEG-2025-0002', 'PRO2025-0005', 800, 980.00, '2027-02-28', 'AVAILABLE');

-- =====================================================
-- 7. PROMOTION DATA
-- =====================================================

-- Insert all Promotions first
INSERT INTO Promotion (id, name, description, creationDate, effectiveDate, endDate, isActive)
VALUES ('PMO2025-0001', N'Khuyến mãi Paracetamol - Giảm giá kép', N'Mua từ 2 hộp Paracetamol, giảm 10% tổng đơn và giảm thêm 20,000đ', '2025-01-20', '2025-01-20', '2025-12-31', 1);

INSERT INTO Promotion (id, name, description, creationDate, effectiveDate, endDate, isActive)
VALUES ('PMO2025-0002', N'Khuyến mãi hóa đơn - Tặng quà', N'Tặng 1 chai Vitamin C và giảm 5% tổng đơn cho hóa đơn trên 200.000 VNĐ', '2025-01-21', '2025-01-21', '2025-12-31', 1);

INSERT INTO Promotion (id, name, description, creationDate, effectiveDate, endDate, isActive)
VALUES ('PMO2025-0003', N'Khuyến mãi Amoxicillin - Tặng Omega 3', N'Mua từ 5 hộp Amoxicillin, tặng 1 lọ Omega 3', '2025-01-22', '2025-01-22', '2025-12-31', 1);

-- Insert all PromotionConditions
-- PCO2025-0001: Buy Paracetamol (PRO2025-0001) with Hộp unit (measurementId=3) >= 2
INSERT INTO PromotionCondition (id, promotion, type, comparator, target, value, product, unitOfMeasure)
VALUES ('PCO2025-0001', 'PMO2025-0001', 'PRODUCT_QTY', 'GREATER_EQUAL', 'PRODUCT', 2, 'PRO2025-0001', 3);

-- PCO2025-0002: Order subtotal >= 200000 (no product/UOM needed)
INSERT INTO PromotionCondition (id, promotion, type, comparator, target, value, product, unitOfMeasure)
VALUES ('PCO2025-0002', 'PMO2025-0002', 'ORDER_SUBTOTAL', 'GREATER_EQUAL', 'ORDER_SUBTOTAL', 200000, NULL, NULL);

-- PCO2025-0003: Buy Amoxicillin (PRO2025-0003) with Hộp unit (measurementId=3) >= 5
INSERT INTO PromotionCondition (id, promotion, type, comparator, target, value, product, unitOfMeasure)
VALUES ('PCO2025-0003', 'PMO2025-0003', 'PRODUCT_QTY', 'GREATER_EQUAL', 'PRODUCT', 5, 'PRO2025-0003', 3);

-- Insert all PromotionActions
-- PAC2025-0001: 10% discount on order subtotal (no product/UOM needed)
INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, value, product, unitOfMeasure)
VALUES ('PAC2025-0001', 'PMO2025-0001', 1, 'PERCENT_DISCOUNT', 'ORDER_SUBTOTAL', 10, NULL, NULL);

-- PAC2025-0002: Fixed 20000 discount on order subtotal (no product/UOM needed)
INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, value, product, unitOfMeasure)
VALUES ('PAC2025-0002', 'PMO2025-0001', 2, 'FIXED_DISCOUNT', 'ORDER_SUBTOTAL', 20000, NULL, NULL);

-- PAC2025-0003: Gift 1 Vitamin C Chai (PRO2025-0002, measurementId=5)
INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, value, product, unitOfMeasure)
VALUES ('PAC2025-0003', 'PMO2025-0002', 1, 'PRODUCT_GIFT', 'PRODUCT', 1, 'PRO2025-0002', 5);

-- PAC2025-0004: 5% discount on order subtotal (no product/UOM needed)
INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, value, product, unitOfMeasure)
VALUES ('PAC2025-0004', 'PMO2025-0002', 2, 'PERCENT_DISCOUNT', 'ORDER_SUBTOTAL', 5, NULL, NULL);

-- PAC2025-0005: Gift 1 Omega 3 Lọ (PRO2025-0005, measurementId=6)
INSERT INTO PromotionAction (id, promotion, actionOrder, type, target, value, product, unitOfMeasure)
VALUES ('PAC2025-0005', 'PMO2025-0003', 1, 'PRODUCT_GIFT', 'PRODUCT', 1, 'PRO2025-0005', 6);

-- =====================================================
-- 8. SHIFT DATA
-- =====================================================

-- Shift 1: Completed morning shift
INSERT INTO Shift (id, staff, startTime, endTime, startCash, endCash, systemCash, status, notes)
VALUES ('SHI2025-0001', 'MAN2025-0001', '2025-01-22 08:00:00', '2025-01-22 14:00:00', 1000000.00, 1850000.00, 1845000.00, 'CLOSED', N'Ca sáng - Chênh lệch 5,000đ do tiền lẻ');

-- Shift 2: Completed afternoon shift
INSERT INTO Shift (id, staff, startTime, endTime, startCash, endCash, systemCash, status, notes)
VALUES ('SHI2025-0002', 'MAN2025-0001', '2025-01-22 14:00:00', '2025-01-22 22:00:00', 500000.00, 1250000.00, 1250000.00, 'CLOSED', N'Ca chiều - Khớp tiền');

-- Shift 3: Completed full day shift
INSERT INTO Shift (id, staff, startTime, endTime, startCash, endCash, systemCash, status, notes)
VALUES ('SHI2025-0003', 'MAN2025-0001', '2025-01-23 08:00:00', '2025-01-23 20:00:00', 2000000.00, 3500000.00, 3480000.00, 'CLOSED', N'Ca cả ngày');

-- Shift 4: Open shift (currently working)
INSERT INTO Shift (id, staff, startTime, endTime, startCash, endCash, systemCash, status, notes)
VALUES ('SHI2025-0004', 'MAN2025-0001', '2025-01-26 08:00:00', NULL, 1500000.00, NULL, NULL, 'OPEN', N'Ca đang mở');

-- =====================================================
-- 9. INVOICE DATA (Various types for different test cases)
-- =====================================================

-- Invoice 1: SALES with Promotion 1 and Prescription Code (ETC product)
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0001', 'SALES', '2025-01-22 09:30:00', 'MAN2025-0001', 'CUS2025-0001', 'MW001a3b5c7d-C', NULL, 'PMO2025-0001', 'CASH', N'Bán thuốc theo đơn, áp dụng khuyến mãi', 'SHI2025-0001');

-- Invoice Lines for Invoice 1
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0001', 'INV2025-0001', 'PRO2025-0001', 3, 3, 20000.00, 'SALE');

INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0002', 'INV2025-0001', 'PRO2025-0003', 1, 20, 500.00, 'SALE');

-- LotAllocation for Invoice 1 Lines
-- ILN2025-0001: 3 Hộp Paracetamol = 3 / 0.01 = 300 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0001', 'ILN2025-0001', 'LOT2025-0001', 300);
-- ILN2025-0002: 20 Viên Amoxicillin = 20 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0002', 'ILN2025-0002', 'LOT2025-0006', 20);

-- Invoice 2: SALES with Bank Transfer
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0002', 'SALES', '2025-01-22 10:15:00', 'MAN2025-0001', NULL, NULL, NULL, NULL, 'BANK_TRANSFER', N'Khách hàng thanh toán chuyển khoản', 'SHI2025-0001');

-- Invoice Lines for Invoice 2
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0003', 'INV2025-0002', 'PRO2025-0002', 5, 2, 10000.00, 'SALE');

INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0004', 'INV2025-0002', 'PRO2025-0005', 1, 30, 1500.00, 'SALE');

-- LotAllocation for Invoice 2 Lines
-- ILN2025-0003: 2 Chai Vitamin C = 2 / 0.01 = 200 ml (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0003', 'ILN2025-0003', 'LOT2025-0004', 200);
-- ILN2025-0004: 30 Viên Omega 3 = 30 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0004', 'ILN2025-0004', 'LOT2025-0011', 30);

-- Invoice 3: SALES with Prescription Code (ETC) - Different customer
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0003', 'SALES', '2025-01-22 14:20:00', 'MAN2025-0001', 'CUS2025-0002', 'MW0019k2m4p6-H', NULL, NULL, 'CASH', N'Đơn thuốc hướng thần', 'SHI2025-0002');

-- Invoice Lines for Invoice 3
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0005', 'INV2025-0003', 'PRO2025-0004', 5, 2, 24000.00, 'SALE');

INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0006', 'INV2025-0003', 'PRO2025-0001', 2, 5, 2000.00, 'SALE');

-- LotAllocation for Invoice 3 Lines
-- ILN2025-0005: 2 Chai Omeprazole = 2 / 0.0083 ≈ 241 ml (base UOM, rounded)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0005', 'ILN2025-0005', 'LOT2025-0009', 241);
-- ILN2025-0006: 5 Vỉ Paracetamol = 5 / 0.1 = 50 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0006', 'ILN2025-0006', 'LOT2025-0001', 50);

-- Invoice 4: SALES - Large order
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0004', 'SALES', '2025-01-23 11:45:00', 'MAN2025-0001', 'CUS2025-0003', NULL, NULL, NULL, 'CASH', N'Đơn hàng lớn', 'SHI2025-0003');

-- Invoice Lines for Invoice 4
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0007', 'INV2025-0004', 'PRO2025-0001', 3, 10, 20000.00, 'SALE');

INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0008', 'INV2025-0004', 'PRO2025-0002', 5, 5, 10000.00, 'SALE');

INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0009', 'INV2025-0004', 'PRO2025-0005', 6, 3, 90000.00, 'SALE');

-- LotAllocation for Invoice 4 Lines
-- ILN2025-0007: 10 Hộp Paracetamol = 10 / 0.01 = 1000 Viên (base UOM)
-- Split between LOT2025-0001 (800) and LOT2025-0003 (200) to demo FIFO
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0007', 'ILN2025-0007', 'LOT2025-0001', 800);
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0008', 'ILN2025-0007', 'LOT2025-0003', 200);
-- ILN2025-0008: 5 Chai Vitamin C = 5 / 0.01 = 500 ml (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0009', 'ILN2025-0008', 'LOT2025-0004', 500);
-- ILN2025-0009: 3 Lọ Omega 3 = 3 / 0.0167 ≈ 180 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0010', 'ILN2025-0009', 'LOT2025-0011', 180);

-- Invoice 5: SALES - Simple cash transaction
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0005', 'SALES', '2025-01-26 16:00:00', 'MAN2025-0001', NULL, NULL, NULL, NULL, 'CASH', N'Khách hàng mua lẻ', 'SHI2025-0004');

-- Invoice Lines for Invoice 5
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0010', 'INV2025-0005', 'PRO2025-0001', 1, 20, 200.00, 'SALE');

-- LotAllocation for Invoice 5 Lines
-- ILN2025-0010: 20 Viên Paracetamol = 20 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0011', 'ILN2025-0010', 'LOT2025-0001', 20);

-- Invoice 6: RETURN - Return from Invoice 2
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0006', 'RETURN', '2025-01-23 15:30:00', 'MAN2025-0001', NULL, NULL, 'INV2025-0002', NULL, 'CASH', N'Khách hàng đổi trả do sản phẩm hư', 'SHI2025-0003');

-- Invoice Lines for Invoice 6 (Return)
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0011', 'INV2025-0006', 'PRO2025-0005', 1, 10, 1500.00, 'RETURN');

-- LotAllocation for Invoice 6 (Return - returning to lot)
-- ILN2025-0011: 10 Viên Omega 3 = 10 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0012', 'ILN2025-0011', 'LOT2025-0011', 10);

-- Invoice 7: EXCHANGE - Exchange from Invoice 4
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0007', 'EXCHANGE', '2025-01-24 10:00:00', 'MAN2025-0001', 'CUS2025-0003', NULL, 'INV2025-0004', NULL, 'CASH', N'Đổi sản phẩm khác loại', 'SHI2025-0003');

-- Invoice Lines for Invoice 7 (Exchange)
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0012', 'INV2025-0007', 'PRO2025-0001', 3, 2, 20000.00, 'EXCHANGE_OUT');

INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0013', 'INV2025-0007', 'PRO2025-0003', 3, 1, 50000.00, 'EXCHANGE_IN');

-- LotAllocation for Invoice 7 (Exchange)
-- ILN2025-0012: 2 Hộp Paracetamol (EXCHANGE_OUT) = 2 / 0.01 = 200 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0013', 'ILN2025-0012', 'LOT2025-0001', 200);
-- ILN2025-0013: 1 Hộp Amoxicillin (EXCHANGE_IN) = 1 / 0.01 = 100 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0014', 'ILN2025-0013', 'LOT2025-0006', 100);

-- Invoice 8: SALES with Promotion 2 applied
INSERT INTO Invoice (id, type, creationDate, creator, customer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift)
VALUES ('INV2025-0008', 'SALES', '2025-01-26 17:30:00', 'MAN2025-0001', 'CUS2025-0004', NULL, NULL, 'PMO2025-0002', 'BANK_TRANSFER', N'Áp dụng khuyến mãi tặng quà', 'SHI2025-0004');

-- Invoice Lines for Invoice 8
INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0014', 'INV2025-0008', 'PRO2025-0005', 6, 3, 90000.00, 'SALE');

INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
VALUES ('ILN2025-0015', 'INV2025-0008', 'PRO2025-0002', 5, 1, 0.00, 'SALE');

-- LotAllocation for Invoice 8
-- ILN2025-0014: 3 Lọ Omega 3 = 3 / 0.0167 ≈ 180 Viên (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0015', 'ILN2025-0014', 'LOT2025-0012', 180);
-- ILN2025-0015: 1 Chai Vitamin C (gift) = 1 / 0.01 = 100 ml (base UOM)
INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) VALUES ('LAL2025-0016', 'ILN2025-0015', 'LOT2025-0004', 100);

GO

-- =====================================================
-- DATA GENERATION COMPLETE
-- =====================================================
PRINT 'Data generation completed successfully!';
PRINT '- 1 Staff (admin) created';
PRINT '- 5 Customers created';
PRINT '- 5 Products created (2 OTC [1 LIQUID], 2 ETC [1 LIQUID], 1 Supplement)';
PRINT '- 9 MeasurementNames created';
PRINT '- 12 UnitOfMeasures created (with price and baseUnitConversionRate)';
PRINT '- 12 Lots created (2 unavailable: 1 EXPIRED, 1 FAULTY)';
PRINT '- 3 Promotions created (1 condition each, various actions with unitOfMeasure)';
PRINT '- 4 Shifts created (3 CLOSED, 1 OPEN)';
PRINT '- 8 Invoices created (5 SALES, 1 RETURN, 1 EXCHANGE with different test cases)';
PRINT '- 15 InvoiceLines created';
PRINT '- 16 LotAllocations created (quantities in base UOM)';
GO
