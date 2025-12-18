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