-- ========================================
-- MediWOW - COMPLETE SAMPLE DATA
-- Period: June 2024 - December 2025 (19 months)
-- Author: Generated for testing purposes
-- ========================================

USE MediWOW;
GO

-- ========================================
-- 1. STAFF DATA
-- ========================================
INSERT INTO dbo.Staff (username, password, fullName, licenseNumber, phoneNumber, email, hireDate, isActive, role, isFirstLogin, mustChangePassword)
VALUES
    (N'admin', N'$2a$12$vVXxXrKyAGhRge.lO0ihZ.0Nl7PghqZLqSpRwvpoDnC8qe3uZC1TK', N'Administrator', NULL, NULL, NULL, N'2024-01-01', 1, N'MANAGER', 0, 0),
    (N'nhanvien250001', N'$2a$12$jdwZLPjlfqJxBT5NBI4hBuyoAeDUHGe7qGlAx13GoDjJBFN18iO/i', N'Tô Thanh Hậu', N'05495/CCHN-D-SYT-HNO', N'0868182546', N'thanhhau670@gmail.com', N'2024-06-01', 1, N'PHARMACIST', 0, 0),
    (N'nhanvien250002', N'$2a$12$jdwZLPjlfqJxBT5NBI4hBuyoAeDUHGe7qGlAx13GoDjJBFN18iO/i', N'Bùi Quốc Trụ', N'02495/CCHN-D-SYT-HNO', N'0912345678', N'nguyenvanan@gmail.com', N'2024-06-15', 1, N'PHARMACIST', 0, 0),
    (N'quanly250001', N'$2a$12$jdwZLPjlfqJxBT5NBI4hBuyoAeDUHGe7qGlAx13GoDjJBFN18iO/i', N'Trần Thị Mai', N'08765/CCHN-D-SYT-HNO', N'0987654321', N'tranthimai@gmail.com', N'2024-05-01', 1, N'MANAGER', 0, 0),
    -- New staff for 2025
    (N'nhanvien250003', N'$2a$12$jdwZLPjlfqJxBT5NBI4hBuyoAeDUHGe7qGlAx13GoDjJBFN18iO/i', N'Nguyễn Thanh Khôi', N'03456/CCHN-D-SYT-HNO', N'0923456789', N'lethihoa@gmail.com', N'2025-01-15', 1, N'PHARMACIST', 0, 0),
    (N'nhanvien250004', N'$2a$12$Rvqmwr26PRsqdRPoQpC4fOH6rR8ygi.KAIx4Lj6gFSSpJsU2TyPye', N'Phạm Văn Minh', N'04567/CCHN-D-SYT-HNO', N'0934567890', N'phamvanminh@gmail.com', N'2025-03-01', 1, N'PHARMACIST', 0, 0),
    (N'quanly250002', N'$2a$12$Rvqmwr26PRsqdRPoQpC4fOH6rR8ygi.KAIx4Lj6gFSSpJsU2TyPye', N'Hoàng Thị Lan', N'09876/CCHN-D-SYT-HNO', N'0945678901', N'hoangthilan@gmail.com', N'2025-02-01', 1, N'MANAGER', 0, 0);


-- ========================================
-- 2. CUSTOMERS DATA (100 customers)
-- ========================================
INSERT INTO dbo.Customer (id, name, phoneNumber, address, creationDate)
VALUES
    -- 2024 Customers (30)
    (N'CUS2024-0001', N'Nguyễn Văn A', N'0901234567', N'123 Nguyễn Huệ, Q.1, TP.HCM', N'2024-06-05'),
    (N'CUS2024-0002', N'Trần Thị B', N'0902345678', N'456 Lê Lợi, Q.1, TP.HCM', N'2024-06-08'),
    (N'CUS2024-0003', N'Lê Văn C', N'0903456789', N'789 Hai Bà Trưng, Q.3, TP.HCM', N'2024-06-12'),
    (N'CUS2024-0004', N'Phạm Thị D', N'0904567890', N'321 Trần Hưng Đạo, Q.5, TP.HCM', N'2024-06-15'),
    (N'CUS2024-0005', N'Hoàng Văn E', N'0905678901', N'654 Võ Văn Tần, Q.3, TP.HCM', N'2024-06-20'),
    (N'CUS2024-0006', N'Vũ Thị F', N'0906789012', N'987 Nguyễn Thị Minh Khai, Q.1, TP.HCM', N'2024-07-01'),
    (N'CUS2024-0007', N'Đỗ Văn G', N'0907890123', N'147 Pasteur, Q.3, TP.HCM', N'2024-07-05'),
    (N'CUS2024-0008', N'Bùi Thị H', N'0908901234', N'258 Cách Mạng Tháng 8, Q.10, TP.HCM', N'2024-07-10'),
    (N'CUS2024-0009', N'Đinh Văn I', N'0909012345', N'369 Lý Thường Kiệt, Q.10, TP.HCM', N'2024-07-15'),
    (N'CUS2024-0010', N'Mai Thị K', N'0910123456', N'741 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM', N'2024-07-20'),
    (N'CUS2024-0011', N'Ngô Văn L', N'0911234567', N'852 Võ Thị Sáu, Q.3, TP.HCM', N'2024-08-01'),
    (N'CUS2024-0012', N'Dương Thị M', N'0912345670', N'963 Nam Kỳ Khởi Nghĩa, Q.1, TP.HCM', N'2024-08-05'),
    (N'CUS2024-0013', N'Lý Văn N', N'0913456781', N'159 Trường Chinh, Q.Tân Bình, TP.HCM', N'2024-08-10'),
    (N'CUS2024-0014', N'Phan Thị O', N'0914567892', N'357 Cộng Hòa, Q.Tân Bình, TP.HCM', N'2024-08-15'),
    (N'CUS2024-0015', N'Tô Văn P', N'0915678903', N'753 Lạc Long Quân, Q.11, TP.HCM', N'2024-08-20'),
    (N'CUS2024-0016', N'Châu Thị Q', N'0916789014', N'951 Âu Cơ, Q.Tân Bình, TP.HCM', N'2024-09-01'),
    (N'CUS2024-0017', N'Thái Văn R', N'0917890125', N'246 Hoàng Văn Thụ, Q.Phú Nhuận, TP.HCM', N'2024-09-05'),
    (N'CUS2024-0018', N'Lâm Thị S', N'0918901236', N'468 Phan Xích Long, Q.Phú Nhuận, TP.HCM', N'2024-09-10'),
    (N'CUS2024-0019', N'Hồ Văn T', N'0919012347', N'579 Nguyễn Văn Trỗi, Q.Phú Nhuận, TP.HCM', N'2024-09-15'),
    (N'CUS2024-0020', N'Kim Thị U', N'0920123458', N'135 Ba Tháng Hai, Q.10, TP.HCM', N'2024-09-20'),
    (N'CUS2024-0021', N'Trịnh Văn V', N'0921234569', N'246 Sư Vạn Hạnh, Q.10, TP.HCM', N'2024-10-01'),
    (N'CUS2024-0022', N'Huỳnh Thị W', N'0922345671', N'357 Nguyễn Chí Thanh, Q.5, TP.HCM', N'2024-10-05'),
    (N'CUS2024-0023', N'Ông Văn X', N'0923456782', N'468 Hùng Vương, Q.5, TP.HCM', N'2024-10-10'),
    (N'CUS2024-0024', N'La Thị Y', N'0924567893', N'579 Tôn Đản, Q.4, TP.HCM', N'2024-10-15'),
    (N'CUS2024-0025', N'Ma Văn Z', N'0925678904', N'681 Bến Vân Đồn, Q.4, TP.HCM', N'2024-10-20'),
    (N'CUS2024-0026', N'Nghiêm Thị AA', N'0926789015', N'792 Khánh Hội, Q.4, TP.HCM', N'2024-11-01'),
    (N'CUS2024-0027', N'Ung Văn BB', N'0927890126', N'803 Xô Viết Nghệ Tĩnh, Q.Bình Thạnh, TP.HCM', N'2024-11-05'),
    (N'CUS2024-0028', N'Văn Thị CC', N'0928901237', N'914 Phan Đăng Lưu, Q.Bình Thạnh, TP.HCM', N'2024-11-10'),
    (N'CUS2024-0029', N'Xa Văn DD', N'0929012348', N'125 Nơ Trang Long, Q.Bình Thạnh, TP.HCM', N'2024-11-15'),
    (N'CUS2024-0030', N'Da Thị EE', N'0930123459', N'236 Đinh Bộ Lĩnh, Q.Bình Thạnh, TP.HCM', N'2024-11-20'),

    -- 2025 Customers (70)
    (N'CUS2025-0001', N'Võ Văn FF', N'0931234560', N'345 Lý Tự Trọng, Q.1, TP.HCM', N'2025-01-05'),
    (N'CUS2025-0002', N'Đặng Thị GG', N'0932345671', N'456 Đồng Khởi, Q.1, TP.HCM', N'2025-01-10'),
    (N'CUS2025-0003', N'Cao Văn HH', N'0933456782', N'567 Hai Bà Trưng, Q.1, TP.HCM', N'2025-01-15'),
    (N'CUS2025-0004', N'Bạch Thị II', N'0934567893', N'678 Nguyễn Du, Q.1, TP.HCM', N'2025-01-20'),
    (N'CUS2025-0005', N'Lưu Văn JJ', N'0935678904', N'789 Phạm Ngọc Thạch, Q.3, TP.HCM', N'2025-01-25'),
    (N'CUS2025-0006', N'Trương Thị KK', N'0936789015', N'890 Võ Thị Sáu, Q.3, TP.HCM', N'2025-02-01'),
    (N'CUS2025-0007', N'Quách Văn LL', N'0937890126', N'901 Trần Quốc Toản, Q.3, TP.HCM', N'2025-02-05'),
    (N'CUS2025-0008', N'Mạc Thị MM', N'0938901237', N'123 Điện Biên Phủ, Q.3, TP.HCM', N'2025-02-10'),
    (N'CUS2025-0009', N'Ân Văn NN', N'0939012348', N'234 Nam Kỳ Khởi Nghĩa, Q.3, TP.HCM', N'2025-02-15'),
    (N'CUS2025-0010', N'Diệp Thị OO', N'0940123459', N'345 Cách Mạng Tháng 8, Q.3, TP.HCM', N'2025-02-20'),
    (N'CUS2025-0011', N'Từ Văn PP', N'0941234560', N'456 Lê Văn Sỹ, Q.3, TP.HCM', N'2025-02-25'),
    (N'CUS2025-0012', N'Phùng Thị QQ', N'0942345671', N'567 Trần Huy Liệu, Q.Phú Nhuận, TP.HCM', N'2025-03-01'),
    (N'CUS2025-0013', N'Ưng Văn RR', N'0943456782', N'678 Phan Đình Phùng, Q.Phú Nhuận, TP.HCM', N'2025-03-05'),
    (N'CUS2025-0014', N'Khúc Thị SS', N'0944567893', N'789 Nguyễn Thị Minh Khai, Q.3, TP.HCM', N'2025-03-10'),
    (N'CUS2025-0015', N'Lục Văn TT', N'0945678904', N'890 Phan Xích Long, Q.Phú Nhuận, TP.HCM', N'2025-03-15'),
    (N'CUS2025-0016', N'Vi Thị UU', N'0946789015', N'901 Hoàng Văn Thụ, Q.Tân Bình, TP.HCM', N'2025-03-20'),
    (N'CUS2025-0017', N'Tề Văn VV', N'0947890126', N'123 Cộng Hòa, Q.Tân Bình, TP.HCM', N'2025-03-25'),
    (N'CUS2025-0018', N'Mã Thị WW', N'0948901237', N'234 Trường Chinh, Q.Tân Bình, TP.HCM', N'2025-04-01'),
    (N'CUS2025-0019', N'Cung Văn XX', N'0949012348', N'345 Lạc Long Quân, Q.Tân Bình, TP.HCM', N'2025-04-05'),
    (N'CUS2025-0020', N'Uông Thị YY', N'0950123459', N'456 Âu Cơ, Q.Tân Bình, TP.HCM', N'2025-04-10'),
    (N'CUS2025-0021', N'Tăng Văn ZZ', N'0951234560', N'567 Nguyễn Văn Linh, Q.7, TP.HCM', N'2025-04-15'),
    (N'CUS2025-0022', N'Bành Thị AAA', N'0952345671', N'678 Huỳnh Tấn Phát, Q.7, TP.HCM', N'2025-04-20'),
    (N'CUS2025-0023', N'Lộc Văn BBB', N'0953456782', N'789 Nguyễn Thị Thập, Q.7, TP.HCM', N'2025-04-25'),
    (N'CUS2025-0024', N'Đường Thị CCC', N'0954567893', N'890 Lê Văn Lương, Q.7, TP.HCM', N'2025-05-01'),
    (N'CUS2025-0025', N'Hồng Văn DDD', N'0955678904', N'901 Nguyễn Hữu Thọ, Q.7, TP.HCM', N'2025-05-05'),
    (N'CUS2025-0026', N'Lan Thị EEE', N'0956789015', N'123 Võ Văn Kiệt, Q.5, TP.HCM', N'2025-05-10'),
    (N'CUS2025-0027', N'Mẫn Văn FFF', N'0957890126', N'234 Trần Hưng Đạo, Q.5, TP.HCM', N'2025-05-15'),
    (N'CUS2025-0028', N'Nhung Thị GGG', N'0958901237', N'345 Nguyễn Trãi, Q.5, TP.HCM', N'2025-05-20'),
    (N'CUS2025-0029', N'Oanh Văn HHH', N'0959012348', N'456 Hùng Vương, Q.5, TP.HCM', N'2025-05-25'),
    (N'CUS2025-0030', N'Phượng Thị III', N'0960123459', N'567 Trần Phú, Q.5, TP.HCM', N'2025-06-01'),
    (N'CUS2025-0031', N'Quân Văn JJJ', N'0961234560', N'678 An Dương Vương, Q.5, TP.HCM', N'2025-06-05'),
    (N'CUS2025-0032', N'Như Thị KKK', N'0962345671', N'789 Châu Văn Liêm, Q.5, TP.HCM', N'2025-06-10'),
    (N'CUS2025-0033', N'Sinh Văn LLL', N'0963456782', N'890 Nguyễn Chí Thanh, Q.5, TP.HCM', N'2025-06-15'),
    (N'CUS2025-0034', N'Tâm Thị MMM', N'0964567893', N'901 Hồng Bàng, Q.5, TP.HCM', N'2025-06-20'),
    (N'CUS2025-0035', N'Út Văn NNN', N'0965678904', N'123 Bến Vân Đồn, Q.4, TP.HCM', N'2025-06-25'),
    (N'CUS2025-0036', N'Vân Thị OOO', N'0966789015', N'234 Tôn Đản, Q.4, TP.HCM', N'2025-07-01'),
    (N'CUS2025-0037', N'Xuân Văn PPP', N'0967890126', N'345 Khánh Hội, Q.4, TP.HCM', N'2025-07-05'),
    (N'CUS2025-0038', N'Yến Thị QQQ', N'0968901237', N'456 Hoàng Diệu, Q.4, TP.HCM', N'2025-07-10'),
    (N'CUS2025-0039', N'An Văn RRR', N'0969012348', N'567 Nguyễn Tất Thành, Q.4, TP.HCM', N'2025-07-15'),
    (N'CUS2025-0040', N'Bình Thị SSS', N'0970123459', N'678 Xô Viết Nghệ Tĩnh, Q.Bình Thạnh, TP.HCM', N'2025-07-20'),
    (N'CUS2025-0041', N'Cảnh Văn TTT', N'0971234560', N'789 Phan Đăng Lưu, Q.Bình Thạnh, TP.HCM', N'2025-07-25'),
    (N'CUS2025-0042', N'Dung Thị UUU', N'0972345671', N'890 Nơ Trang Long, Q.Bình Thạnh, TP.HCM', N'2025-08-01'),
    (N'CUS2025-0043', N'Dũng Văn VVV', N'0973456782', N'901 Đinh Bộ Lĩnh, Q.Bình Thạnh, TP.HCM', N'2025-08-05'),
    (N'CUS2025-0044', N'Giang Thị WWW', N'0974567893', N'123 Bạch Đằng, Q.Bình Thạnh, TP.HCM', N'2025-08-10'),
    (N'CUS2025-0045', N'Hải Văn XXX', N'0975678904', N'234 Ung Văn Khiêm, Q.Bình Thạnh, TP.HCM', N'2025-08-15'),
    (N'CUS2025-0046', N'Hằng Thị YYY', N'0976789015', N'345 D2, Q.Bình Thạnh, TP.HCM', N'2025-08-20'),
    (N'CUS2025-0047', N'Huy Văn ZZZ', N'0977890126', N'456 Nguyễn Xí, Q.Bình Thạnh, TP.HCM', N'2025-08-25'),
    (N'CUS2025-0048', N'Khoa Thị AAAA', N'0978901237', N'567 Lê Quang Định, Q.Bình Thạnh, TP.HCM', N'2025-09-01'),
    (N'CUS2025-0049', N'Kiên Văn BBBB', N'0979012348', N'678 Nguyễn Duy Trinh, Q.2, TP.HCM', N'2025-09-05'),
    (N'CUS2025-0050', N'Linh Thị CCCC', N'0980123459', N'789 Trần Não, Q.2, TP.HCM', N'2025-09-10'),
    (N'CUS2025-0051', N'Long Văn DDDD', N'0981234560', N'890 Mai Chí Thọ, Q.2, TP.HCM', N'2025-09-15'),
    (N'CUS2025-0052', N'Minh Thị EEEE', N'0982345671', N'901 Đỗ Xuân Hợp, Q.9, TP.HCM', N'2025-09-20'),
    (N'CUS2025-0053', N'Nam Văn FFFF', N'0983456782', N'123 Lê Văn Việt, Q.9, TP.HCM', N'2025-09-25'),
    (N'CUS2025-0054', N'Ngọc Thị GGGG', N'0984567893', N'234 Võ Văn Ngân, Q.Thủ Đức, TP.HCM', N'2025-10-01'),
    (N'CUS2025-0055', N'Phát Văn HHHH', N'0985678904', N'345 Kha Vạn Cân, Q.Thủ Đức, TP.HCM', N'2025-10-05'),
    (N'CUS2025-0056', N'Quang Thị IIII', N'0986789015', N'456 Đặng Văn Bi, Q.Thủ Đức, TP.HCM', N'2025-10-10'),
    (N'CUS2025-0057', N'Sơn Văn JJJJ', N'0987890126', N'567 Tô Ngọc Vân, Q.Thủ Đức, TP.HCM', N'2025-10-15'),
    (N'CUS2025-0058', N'Thảo Thị KKKK', N'0988901237', N'678 Phạm Văn Đồng, Q.Thủ Đức, TP.HCM', N'2025-10-20'),
    (N'CUS2025-0059', N'Thiện Văn LLLL', N'0989012348', N'789 Lê Văn Chí, Q.Thủ Đức, TP.HCM', N'2025-10-25'),
    (N'CUS2025-0060', N'Thư Thị MMMM', N'0990123459', N'890 Quốc Lộ 1A, Q.Thủ Đức, TP.HCM', N'2025-11-01'),
    (N'CUS2025-0061', N'Trang Văn NNNN', N'0991234560', N'901 Tạ Quang Bửu, Q.8, TP.HCM', N'2025-11-05'),
    (N'CUS2025-0062', N'Tuấn Thị OOOO', N'0992345671', N'123 Phạm Thế Hiển, Q.8, TP.HCM', N'2025-11-10'),
    (N'CUS2025-0063', N'Tùng Văn PPPP', N'0993456782', N'234 Cao Lỗ, Q.8, TP.HCM', N'2025-11-15'),
    (N'CUS2025-0064', N'Vũ Thị QQQQ', N'0994567893', N'345 Dương Bá Trạc, Q.8, TP.HCM', N'2025-11-20'),
    (N'CUS2025-0065', N'Yên Văn RRRR', N'0995678904', N'456 Hưng Phú, Q.8, TP.HCM', N'2025-11-25'),
    (N'CUS2025-0066', N'Chi Thị SSSS', N'0996789015', N'567 Nguyễn Văn Quá, Q.12, TP.HCM', N'2025-12-01'),
    (N'CUS2025-0067', N'Đạt Văn TTTT', N'0997890126', N'678 Lê Văn Khương, Q.12, TP.HCM', N'2025-12-05'),
    (N'CUS2025-0068', N'Hiền Thị UUUU', N'0998901237', N'789 Quốc Lộ 22, Q.12, TP.HCM', N'2025-12-10'),
    (N'CUS2025-0069', N'Hùng Văn VVVV', N'0999012348', N'890 Tô Ký, Q.12, TP.HCM', N'2025-12-15'),
    (N'CUS2025-0070', N'Thủy Thị WWWW', N'0991123456', N'901 Thạnh Xuân, Q.12, TP.HCM', N'2025-12-20');
GO -- Ngắt batch

-- ========================================
-- 3. PRODUCTS DATA (30 common medicines)
-- ========================================
INSERT INTO dbo.Product (barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate)
VALUES
    -- SUPPLEMENT (5 products)
    (N'8936079260019', N'SUPPLEMENT', N'SOLID', N'Vitamin C 1000mg', N'Vit C 1000', N'DHG Pharma', N'Ascorbic Acid', 10.00, N'1000mg', N'Bổ sung vitamin C', N'Viên', N'2024-06-01'),
    (N'8936079260026', N'SUPPLEMENT', N'SOLID', N'Calcium + Vitamin D3', N'Ca + D3', N'Pymepharco', N'Calcium Carbonate, Cholecalciferol', 10.00, N'600mg + 400IU', N'Bổ sung canxi và vitamin D', N'Viên', N'2024-06-01'),
    (N'8936079260033', N'SUPPLEMENT', N'SOLID', N'Omega 3 Fish Oil', N'Omega 3', N'Blackmores', N'EPA, DHA', 10.00, N'1000mg', N'Bổ sung omega 3', N'Viên', N'2024-06-01'),
    (N'8936079260040', N'SUPPLEMENT', N'SOLID', N'Multivitamin', N'Multivit', N'Centrum', N'Various Vitamins & Minerals', 10.00, N'N/A', N'Vitamin tổng hợp', N'Viên', N'2024-06-01'),
    (N'8936079260057', N'SUPPLEMENT', N'LIQUID_ORAL_DOSAGE', N'Siro tăng sức đề kháng', N'Siro ĐK', N'Traphaco', N'Various', 10.00, N'100ml', N'Tăng đề kháng cho trẻ', N'Chai', N'2024-06-01'),

    -- OTC (9 products)
    (N'8936079260064', N'OTC', N'SOLID', N'Paracetamol 500mg', N'Para 500', N'DHG Pharma', N'Paracetamol', 5.00, N'500mg', N'Hạ sốt, giảm đau', N'Viên', N'2024-06-01'),
    (N'8936079260071', N'OTC', N'SOLID', N'Ibuprofen 400mg', N'Ibu 400', N'Pymepharco', N'Ibuprofen', 5.00, N'400mg', N'Chống viêm, giảm đau', N'Viên', N'2024-06-01'),
    (N'8936079260088', N'OTC', N'SOLID', N'Cetirizine 10mg', N'Ceti 10', N'Traphaco', N'Cetirizine HCl', 5.00, N'10mg', N'Điều trị dị ứng', N'Viên', N'2024-06-01'),
    (N'8936079260095', N'OTC', N'SOLID', N'Loratadine 10mg', N'Lora 10', N'DHG Pharma', N'Loratadine', 5.00, N'10mg', N'Chống dị ứng', N'Viên', N'2024-06-01'),
    (N'8936079260101', N'OTC', N'LIQUID_ORAL_DOSAGE', N'Siro ho Prospan', N'Prospan', N'Engelhard', N'Hedera helix extract', 5.00, N'100ml', N'Điều trị ho', N'Chai', N'2024-06-01'),
    (N'8936079260118', N'OTC', N'SOLID', N'Vitamin B Complex', N'Vit B', N'DHG Pharma', N'B1, B6, B12', 5.00, N'N/A', N'Bổ sung vitamin B', N'Viên', N'2024-06-01'),
    (N'8936079260125', N'OTC', N'LIQUID_DOSAGE', N'Dung dịch súc miệng', N'Nước súc', N'Listerine', N'Various', 5.00, N'250ml', N'Vệ sinh răng miệng', N'Chai', N'2024-06-01'),
    (N'8936079260132', N'OTC', N'SOLID', N'Viên ngậm ho', N'Ngậm ho', N'Strepsils', N'Various', 5.00, N'N/A', N'Giảm đau họng', N'Viên', N'2024-06-01'),
    (N'8936079260149', N'OTC', N'LIQUID_DOSAGE', N'Dầu gió', N'Dầu gió', N'Con Ó', N'Menthol, Camphor', 5.00, N'12ml', N'Xoa bóp giảm đau', N'Chai', N'2024-06-01'),

    -- ETC (16 products)
    (N'8936079260156', N'ETC', N'SOLID', N'Amoxicillin 500mg', N'Amoxi 500', N'DHG Pharma', N'Amoxicillin', 5.00, N'500mg', N'Kháng sinh', N'Viên', N'2024-06-01'),
    (N'8936079260163', N'ETC', N'SOLID', N'Cefixime 200mg', N'Cefi 200', N'Pymepharco', N'Cefixime', 5.00, N'200mg', N'Kháng sinh cephalosporin', N'Viên', N'2024-06-01'),
    (N'8936079260170', N'ETC', N'SOLID', N'Azithromycin 250mg', N'Azithro 250', N'Traphaco', N'Azithromycin', 5.00, N'250mg', N'Kháng sinh macrolide', N'Viên', N'2024-06-01'),
    (N'8936079260187', N'ETC', N'SOLID', N'Metformin 500mg', N'Metfor 500', N'DHG Pharma', N'Metformin HCl', 5.00, N'500mg', N'Điều trị đái tháo đường', N'Viên', N'2024-06-01'),
    (N'8936079260194', N'ETC', N'SOLID', N'Amlodipine 5mg', N'Amlo 5', N'Pymepharco', N'Amlodipine', 5.00, N'5mg', N'Hạ huyết áp', N'Viên', N'2024-06-01'),
    (N'8936079260200', N'ETC', N'SOLID', N'Atorvastatin 10mg', N'Ator 10', N'Traphaco', N'Atorvastatin', 5.00, N'10mg', N'Hạ mỡ máu', N'Viên', N'2024-06-01'),
    (N'8936079260217', N'ETC', N'SOLID', N'Omeprazole 20mg', N'Ome 20', N'DHG Pharma', N'Omeprazole', 5.00, N'20mg', N'Ức chế bơm proton', N'Viên', N'2024-06-01'),
    (N'8936079260224', N'ETC', N'SOLID', N'Esomeprazole 40mg', N'Eso 40', N'Pymepharco', N'Esomeprazole', 5.00, N'40mg', N'Điều trị dạ dày', N'Viên', N'2024-06-01'),
    (N'8936079260231', N'ETC', N'SOLID', N'Losartan 50mg', N'Losar 50', N'Traphaco', N'Losartan', 5.00, N'50mg', N'Hạ huyết áp', N'Viên', N'2024-06-01'),
    (N'8936079260248', N'ETC', N'SOLID', N'Bisoprolol 5mg', N'Biso 5', N'DHG Pharma', N'Bisoprolol', 5.00, N'5mg', N'Điều trị tim mạch', N'Viên', N'2024-06-01'),
    (N'8936079260255', N'ETC', N'SOLID', N'Clopidogrel 75mg', N'Clopi 75', N'Pymepharco', N'Clopidogrel', 5.00, N'75mg', N'Chống đông máu', N'Viên', N'2024-06-01'),
    (N'8936079260262', N'ETC', N'SOLID', N'Glimepiride 2mg', N'Glime 2', N'Traphaco', N'Glimepiride', 5.00, N'2mg', N'Điều trị đái tháo đường', N'Viên', N'2024-06-01'),
    (N'8936079260279', N'ETC', N'SOLID', N'Gliclazide 80mg', N'Glicla 80', N'DHG Pharma', N'Gliclazide', 5.00, N'80mg', N'Điều trị đái tháo đường', N'Viên', N'2024-06-01'),
    (N'8936079260286', N'ETC', N'LIQUID_DOSAGE', N'Insulin Glargine', N'Insulin', N'Sanofi', N'Insulin Glargine', 5.00, N'100IU/ml', N'Điều trị đái tháo đường', N'Lọ', N'2024-06-01'),
    (N'8936079260293', N'ETC', N'SOLID', N'Levothyroxine 100mcg', N'Levo 100', N'Pymepharco', N'Levothyroxine', 5.00, N'100mcg', N'Điều trị suy giáp', N'Viên', N'2024-06-01'),
    (N'8936079260309', N'ETC', N'SOLID', N'Prednisone 5mg', N'Pred 5', N'Traphaco', N'Prednisone', 5.00, N'5mg', N'Corticosteroid', N'Viên', N'2024-06-01');
GO -- Ngắt batch

-- ========================================
-- 4. MEASUREMENT NAMES
-- ========================================
INSERT INTO dbo.MeasurementName (name) VALUES
                                           (N'Viên'), (N'Vỉ'), (N'Hộp'), (N'Chai'), (N'Lọ'), (N'Tuýp'), (N'Gói'), (N'Ống');
GO -- Ngắt batch

-- ========================================
-- 5. UNIT OF MEASURE (Complete pricing)
-- ========================================
INSERT INTO dbo.UnitOfMeasure (product, measurementId, price, baseUnitConversionRate)
SELECT p.id, m.id, price, rate
FROM dbo.Product p, dbo.MeasurementName m,
     (VALUES
          (N'Vitamin C 1000mg', N'Viên', 3000, 1),
          (N'Vitamin C 1000mg', N'Vỉ', 28000, 0.1),
          (N'Vitamin C 1000mg', N'Hộp', 260000, 0.01),
          (N'Calcium + Vitamin D3', N'Viên', 2500, 1),
          (N'Calcium + Vitamin D3', N'Vỉ', 24000, 0.1),
          (N'Calcium + Vitamin D3', N'Hộp', 230000, 0.01),
          (N'Omega 3 Fish Oil', N'Viên', 5000, 1),
          (N'Omega 3 Fish Oil', N'Hộp', 450000, 0.01),
          (N'Multivitamin', N'Viên', 4000, 1),
          (N'Multivitamin', N'Hộp', 360000, 0.01),
          (N'Siro tăng sức đề kháng', N'Chai', 85000, 1),
          (N'Siro tăng sức đề kháng', N'Hộp', 160000, 2), -- Lưu ý: Giá trị 2 giữ nguyên (không phải 10 hay 100)
          (N'Paracetamol 500mg', N'Viên', 500, 1),
          (N'Paracetamol 500mg', N'Vỉ', 4500, 0.1),
          (N'Paracetamol 500mg', N'Hộp', 42000, 0.01),
          (N'Ibuprofen 400mg', N'Viên', 800, 1),
          (N'Ibuprofen 400mg', N'Vỉ', 7500, 0.1),
          (N'Ibuprofen 400mg', N'Hộp', 72000, 0.01),
          (N'Cetirizine 10mg', N'Viên', 700, 1),
          (N'Cetirizine 10mg', N'Vỉ', 6500, 0.1),
          (N'Cetirizine 10mg', N'Hộp', 62000, 0.01),
          (N'Loratadine 10mg', N'Viên', 900, 1),
          (N'Loratadine 10mg', N'Vỉ', 8500, 0.1),
          (N'Loratadine 10mg', N'Hộp', 82000, 0.01),
          (N'Siro ho Prospan', N'Chai', 95000, 1),
          (N'Vitamin B Complex', N'Viên', 600, 1),
          (N'Vitamin B Complex', N'Vỉ', 5500, 0.1),
          (N'Vitamin B Complex', N'Hộp', 52000, 0.01),
          (N'Dung dịch súc miệng', N'Chai', 65000, 1),
          (N'Viên ngậm ho', N'Viên', 1200, 1),
          (N'Viên ngậm ho', N'Vỉ', 11000, 0.1),
          (N'Dầu gió', N'Chai', 25000, 1),
          (N'Amoxicillin 500mg', N'Viên', 1500, 1),
          (N'Amoxicillin 500mg', N'Vỉ', 14000, 0.1),
          (N'Amoxicillin 500mg', N'Hộp', 135000, 0.01),
          (N'Cefixime 200mg', N'Viên', 3500, 1),
          (N'Cefixime 200mg', N'Vỉ', 33000, 0.1),
          (N'Cefixime 200mg', N'Hộp', 320000, 0.01),
          (N'Azithromycin 250mg', N'Viên', 4000, 1),
          (N'Azithromycin 250mg', N'Vỉ', 38000, 0.1),
          (N'Azithromycin 250mg', N'Hộp', 360000, 0.01),
          (N'Metformin 500mg', N'Viên', 800, 1),
          (N'Metformin 500mg', N'Vỉ', 7500, 0.1),
          (N'Metformin 500mg', N'Hộp', 72000, 0.01),
          (N'Amlodipine 5mg', N'Viên', 1200, 1),
          (N'Amlodipine 5mg', N'Vỉ', 11000, 0.1),
          (N'Amlodipine 5mg', N'Hộp', 105000, 0.01),
          (N'Atorvastatin 10mg', N'Viên', 2500, 1),
          (N'Atorvastatin 10mg', N'Vỉ', 24000, 0.1),
          (N'Atorvastatin 10mg', N'Hộp', 230000, 0.01),
          (N'Omeprazole 20mg', N'Viên', 1500, 1),
          (N'Omeprazole 20mg', N'Vỉ', 14000, 0.1),
          (N'Omeprazole 20mg', N'Hộp', 135000, 0.01),
          (N'Esomeprazole 40mg', N'Viên', 3000, 1),
          (N'Esomeprazole 40mg', N'Vỉ', 28000, 0.1),
          (N'Esomeprazole 40mg', N'Hộp', 270000, 0.01),
          (N'Losartan 50mg', N'Viên', 1800, 1),
          (N'Losartan 50mg', N'Vỉ', 17000, 0.1),
          (N'Losartan 50mg', N'Hộp', 165000, 0.01),
          (N'Bisoprolol 5mg', N'Viên', 2000, 1),
          (N'Bisoprolol 5mg', N'Vỉ', 19000, 0.1),
          (N'Bisoprolol 5mg', N'Hộp', 185000, 0.01),
          (N'Clopidogrel 75mg', N'Viên', 3500, 1),
          (N'Clopidogrel 75mg', N'Vỉ', 33000, 0.1),
          (N'Clopidogrel 75mg', N'Hộp', 320000, 0.01),
          (N'Glimepiride 2mg', N'Viên', 1600, 1),
          (N'Glimepiride 2mg', N'Vỉ', 15000, 0.1),
          (N'Glimepiride 2mg', N'Hộp', 145000, 0.01),
          (N'Gliclazide 80mg', N'Viên', 1400, 1),
          (N'Gliclazide 80mg', N'Vỉ', 13000, 0.1),
          (N'Gliclazide 80mg', N'Hộp', 125000, 0.01),
          (N'Insulin Glargine', N'Lọ', 450000, 1),
          (N'Levothyroxine 100mcg', N'Viên', 1100, 1),
          (N'Levothyroxine 100mcg', N'Vỉ', 10500, 0.1),
          (N'Levothyroxine 100mcg', N'Hộp', 100000, 0.01),
          (N'Prednisone 5mg', N'Viên', 900, 1),
          (N'Prednisone 5mg', N'Vỉ', 8500, 0.1),
          (N'Prednisone 5mg', N'Hộp', 82000, 0.01)
     ) AS v(prod, unit, price, rate)
WHERE p.name = v.prod AND m.name = v.unit;
GO -- Ngắt batch

-- ========================================
-- 6. LOTS DATA (2024 + 2025 inventory)
-- ========================================
INSERT INTO dbo.Lot (batchNumber, product, quantity, rawPrice, expiryDate, status)
SELECT v.batch, p.id, v.qty, v.cost, v.expiry, v.status
FROM dbo.Product p,
     (VALUES
          -- 2024 Batches
          (N'VTC001-2024', N'Vitamin C 1000mg', 5000, 1800, N'2026-12-31', N'AVAILABLE'),
          (N'VTC002-2024', N'Vitamin C 1000mg', 3000, 1800, N'2027-03-31', N'AVAILABLE'),
          (N'CAD001-2024', N'Calcium + Vitamin D3', 4000, 1500, N'2026-11-30', N'AVAILABLE'),
          (N'CAD002-2024', N'Calcium + Vitamin D3', 2500, 1500, N'2027-02-28', N'AVAILABLE'),
          (N'OMG001-2024', N'Omega 3 Fish Oil', 2000, 3000, N'2026-10-31', N'AVAILABLE'),
          (N'MUL001-2024', N'Multivitamin', 3000, 2400, N'2026-12-31', N'AVAILABLE'),
          (N'SIR001-2024', N'Siro tăng sức đề kháng', 1000, 51000, N'2026-06-30', N'AVAILABLE'),
          (N'PAR001-2024', N'Paracetamol 500mg', 10000, 300, N'2027-06-30', N'AVAILABLE'),
          (N'PAR002-2024', N'Paracetamol 500mg', 8000, 300, N'2027-09-30', N'AVAILABLE'),
          (N'IBU001-2024', N'Ibuprofen 400mg', 5000, 480, N'2027-05-31', N'AVAILABLE'),
          (N'CET001-2024', N'Cetirizine 10mg', 4000, 420, N'2027-04-30', N'AVAILABLE'),
          (N'LOR001-2024', N'Loratadine 10mg', 3500, 540, N'2027-03-31', N'AVAILABLE'),
          (N'PRO001-2024', N'Siro ho Prospan', 800, 57000, N'2026-08-31', N'AVAILABLE'),
          (N'VTB001-2024', N'Vitamin B Complex', 4000, 360, N'2027-02-28', N'AVAILABLE'),
          (N'MOU001-2024', N'Dung dịch súc miệng', 1500, 39000, N'2026-12-31', N'AVAILABLE'),
          (N'THR001-2024', N'Viên ngậm ho', 3000, 720, N'2027-01-31', N'AVAILABLE'),
          (N'OIL001-2024', N'Dầu gió', 2000, 15000, N'2027-12-31', N'AVAILABLE'),
          (N'AMO001-2024', N'Amoxicillin 500mg', 6000, 900, N'2026-09-30', N'AVAILABLE'),
          (N'CEF001-2024', N'Cefixime 200mg', 3000, 2100, N'2026-10-31', N'AVAILABLE'),
          (N'AZI001-2024', N'Azithromycin 250mg', 2500, 2400, N'2026-11-30', N'AVAILABLE'),
          (N'MET001-2024', N'Metformin 500mg', 8000, 480, N'2027-03-31', N'AVAILABLE'),
          (N'AML001-2024', N'Amlodipine 5mg', 5000, 720, N'2027-04-30', N'AVAILABLE'),
          (N'ATO001-2024', N'Atorvastatin 10mg', 4000, 1500, N'2027-02-28', N'AVAILABLE'),
          (N'OME001-2024', N'Omeprazole 20mg', 5000, 900, N'2027-01-31', N'AVAILABLE'),
          (N'ESO001-2024', N'Esomeprazole 40mg', 3000, 1800, N'2027-03-31', N'AVAILABLE'),
          (N'LOS001-2024', N'Losartan 50mg', 4500, 1080, N'2027-02-28', N'AVAILABLE'),
          (N'BIS001-2024', N'Bisoprolol 5mg', 3500, 1200, N'2027-04-30', N'AVAILABLE'),
          (N'CLO001-2024', N'Clopidogrel 75mg', 3000, 2100, N'2027-03-31', N'AVAILABLE'),
          (N'GLM001-2024', N'Glimepiride 2mg', 3500, 960, N'2027-05-31', N'AVAILABLE'),
          (N'GLC001-2024', N'Gliclazide 80mg', 4000, 840, N'2027-04-30', N'AVAILABLE'),
          (N'INS001-2024', N'Insulin Glargine', 300, 270000, N'2026-06-30', N'AVAILABLE'),
          (N'LEV001-2024', N'Levothyroxine 100mcg', 4000, 660, N'2027-06-30', N'AVAILABLE'),
          (N'PRE001-2024', N'Prednisone 5mg', 3000, 540, N'2027-05-31', N'AVAILABLE'),

          -- 2025 New Batches (Fresh stock)
          (N'VTC001-2025', N'Vitamin C 1000mg', 6000, 1900, N'2027-06-30', N'AVAILABLE'),
          (N'VTC002-2025', N'Vitamin C 1000mg', 5000, 1900, N'2027-12-31', N'AVAILABLE'),
          (N'CAD001-2025', N'Calcium + Vitamin D3', 5000, 1600, N'2027-08-31', N'AVAILABLE'),
          (N'CAD002-2025', N'Calcium + Vitamin D3', 4000, 1600, N'2028-01-31', N'AVAILABLE'),
          (N'OMG001-2025', N'Omega 3 Fish Oil', 3000, 3200, N'2027-07-31', N'AVAILABLE'),
          (N'OMG002-2025', N'Omega 3 Fish Oil', 2500, 3200, N'2027-11-30', N'AVAILABLE'),
          (N'MUL001-2025', N'Multivitamin', 4000, 2500, N'2027-09-30', N'AVAILABLE'),
          (N'MUL002-2025', N'Multivitamin', 3500, 2500, N'2028-02-28', N'AVAILABLE'),
          (N'SIR001-2025', N'Siro tăng sức đề kháng', 1500, 53000, N'2027-03-31', N'AVAILABLE'),
          (N'SIR002-2025', N'Siro tăng sức đề kháng', 1200, 53000, N'2027-08-31', N'AVAILABLE'),
          (N'PAR001-2025', N'Paracetamol 500mg', 12000, 320, N'2028-01-31', N'AVAILABLE'),
          (N'PAR002-2025', N'Paracetamol 500mg', 10000, 320, N'2028-06-30', N'AVAILABLE'),
          (N'IBU001-2025', N'Ibuprofen 400mg', 6000, 500, N'2028-02-28', N'AVAILABLE'),
          (N'IBU002-2025', N'Ibuprofen 400mg', 5000, 500, N'2028-07-31', N'AVAILABLE'),
          (N'CET001-2025', N'Cetirizine 10mg', 5000, 440, N'2028-01-31', N'AVAILABLE'),
          (N'CET002-2025', N'Cetirizine 10mg', 4500, 440, N'2028-06-30', N'AVAILABLE'),
          (N'LOR001-2025', N'Loratadine 10mg', 4000, 560, N'2028-03-31', N'AVAILABLE'),
          (N'LOR002-2025', N'Loratadine 10mg', 3500, 560, N'2028-08-31', N'AVAILABLE'),
          (N'PRO001-2025', N'Siro ho Prospan', 1000, 59000, N'2027-05-31', N'AVAILABLE'),
          (N'PRO002-2025', N'Siro ho Prospan', 900, 59000, N'2027-10-31', N'AVAILABLE'),
          (N'VTB001-2025', N'Vitamin B Complex', 5000, 380, N'2028-01-31', N'AVAILABLE'),
          (N'VTB002-2025', N'Vitamin B Complex', 4000, 380, N'2028-06-30', N'AVAILABLE'),
          (N'MOU001-2025', N'Dung dịch súc miệng', 2000, 40000, N'2027-09-30', N'AVAILABLE'),
          (N'MOU002-2025', N'Dung dịch súc miệng', 1800, 40000, N'2028-02-28', N'AVAILABLE'),
          (N'THR001-2025', N'Viên ngậm ho', 4000, 750, N'2027-12-31', N'AVAILABLE'),
          (N'THR002-2025', N'Viên ngậm ho', 3500, 750, N'2028-05-31', N'AVAILABLE'),
          (N'OIL001-2025', N'Dầu gió', 2500, 16000, N'2028-06-30', N'AVAILABLE'),
          (N'OIL002-2025', N'Dầu gió', 2000, 16000, N'2028-11-30', N'AVAILABLE'),
          (N'AMO001-2025', N'Amoxicillin 500mg', 7000, 950, N'2027-06-30', N'AVAILABLE'),
          (N'AMO002-2025', N'Amoxicillin 500mg', 6500, 950, N'2027-11-30', N'AVAILABLE'),
          (N'CEF001-2025', N'Cefixime 200mg', 4000, 2200, N'2027-07-31', N'AVAILABLE'),
          (N'CEF002-2025', N'Cefixime 200mg', 3500, 2200, N'2028-01-31', N'AVAILABLE'),
          (N'AZI001-2025', N'Azithromycin 250mg', 3000, 2500, N'2027-08-31', N'AVAILABLE'),
          (N'AZI002-2025', N'Azithromycin 250mg', 2800, 2500, N'2028-02-28', N'AVAILABLE'),
          (N'MET001-2025', N'Metformin 500mg', 9000, 500, N'2028-01-31', N'AVAILABLE'),
          (N'MET002-2025', N'Metformin 500mg', 8500, 500, N'2028-06-30', N'AVAILABLE'),
          (N'AML001-2025', N'Amlodipine 5mg', 6000, 750, N'2028-02-28', N'AVAILABLE'),
          (N'AML002-2025', N'Amlodipine 5mg', 5500, 750, N'2028-07-31', N'AVAILABLE'),
          (N'ATO001-2025', N'Atorvastatin 10mg', 5000, 1600, N'2028-01-31', N'AVAILABLE'),
          (N'ATO002-2025', N'Atorvastatin 10mg', 4500, 1600, N'2028-06-30', N'AVAILABLE'),
          (N'OME001-2025', N'Omeprazole 20mg', 6000, 950, N'2027-12-31', N'AVAILABLE'),
          (N'OME002-2025', N'Omeprazole 20mg', 5500, 950, N'2028-05-31', N'AVAILABLE'),
          (N'ESO001-2025', N'Esomeprazole 40mg', 4000, 1900, N'2028-01-31', N'AVAILABLE'),
          (N'ESO002-2025', N'Esomeprazole 40mg', 3500, 1900, N'2028-06-30', N'AVAILABLE'),
          (N'LOS001-2025', N'Losartan 50mg', 5500, 1100, N'2028-01-31', N'AVAILABLE'),
          (N'LOS002-2025', N'Losartan 50mg', 5000, 1100, N'2028-06-30', N'AVAILABLE'),
          (N'BIS001-2025', N'Bisoprolol 5mg', 4500, 1250, N'2028-02-28', N'AVAILABLE'),
          (N'BIS002-2025', N'Bisoprolol 5mg', 4000, 1250, N'2028-07-31', N'AVAILABLE'),
          (N'CLO001-2025', N'Clopidogrel 75mg', 4000, 2200, N'2028-01-31', N'AVAILABLE'),
          (N'CLO002-2025', N'Clopidogrel 75mg', 3500, 2200, N'2028-06-30', N'AVAILABLE'),
          (N'GLM001-2025', N'Glimepiride 2mg', 4500, 1000, N'2028-03-31', N'AVAILABLE'),
          (N'GLM002-2025', N'Glimepiride 2mg', 4000, 1000, N'2028-08-31', N'AVAILABLE'),
          (N'GLC001-2025', N'Gliclazide 80mg', 5000, 880, N'2028-02-28', N'AVAILABLE'),
          (N'GLC002-2025', N'Gliclazide 80mg', 4500, 880, N'2028-07-31', N'AVAILABLE'),
          (N'INS001-2025', N'Insulin Glargine', 400, 280000, N'2027-03-31', N'AVAILABLE'),
          (N'INS002-2025', N'Insulin Glargine', 350, 280000, N'2027-08-31', N'AVAILABLE'),
          (N'LEV001-2025', N'Levothyroxine 100mcg', 5000, 700, N'2028-03-31', N'AVAILABLE'),
          (N'LEV002-2025', N'Levothyroxine 100mcg', 4500, 700, N'2028-08-31', N'AVAILABLE'),
          (N'PRE001-2025', N'Prednisone 5mg', 4000, 570, N'2028-02-28', N'AVAILABLE'),
          (N'PRE002-2025', N'Prednisone 5mg', 3500, 570, N'2028-07-31', N'AVAILABLE'),
          (N'PRE003-2025', N'Prednisone 5mg', 3400, 570, N'2025-12-31', N'AVAILABLE')
     ) AS v(batch, prod, qty, cost, expiry, status)
WHERE p.name = v.prod;
GO -- Ngắt batch

-- ========================================
-- 7. PROMOTIONS DATA (2024-2025)
-- ========================================
INSERT INTO dbo.Promotion (name, description, creationDate, effectiveDate, endDate, isActive)
VALUES
    -- 2024 Promotions
    (N'Khuyến mãi mùa hè 2024', N'Giảm 10% cho hóa đơn từ 500k', N'2024-05-25', N'2024-06-01', N'2024-08-31', 0),
    (N'Ưu đãi tháng 10/2024', N'Mua 2 tặng 1 cho các sản phẩm TPCN', N'2024-09-25', N'2024-10-01', N'2024-10-31', 0),
    (N'Black Friday 2024', N'Giảm 15% toàn bộ đơn hàng trên 1 triệu', N'2024-11-20', N'2024-11-25', N'2024-11-30', 0),
    (N'Chào đón năm mới 2025', N'Giảm 20% cho hóa đơn từ 2 triệu', N'2024-12-15', N'2024-12-20', N'2025-01-05', 0),

    -- 2025 Promotions
    (N'Tết Nguyên Đán 2025', N'Giảm 25% toàn bộ hóa đơn từ 1.5 triệu', N'2025-01-15', N'2025-01-20', N'2025-02-15', 0),
    (N'Khuyến mãi 8/3', N'Giảm 15% cho khách hàng nữ', N'2025-02-25', N'2025-03-05', N'2025-03-10', 0),
    (N'Ưu đãi mùa xuân 2025', N'Mua 3 tặng 1 vitamin C', N'2025-03-15', N'2025-03-20', N'2025-04-30', 0),
    (N'Khuyến mãi giữa năm', N'Giảm 12% đơn hàng từ 800k', N'2025-05-25', N'2025-06-01', N'2025-07-31', 0),
    (N'Ưu đãi sinh viên', N'Giảm 10% cho sinh viên có thẻ', N'2025-08-25', N'2025-09-01', N'2025-09-30', 0),
    (N'Khuyến mãi Quốc Khánh', N'Giảm 20% toàn bộ đơn hàng', N'2025-08-28', N'2025-09-01', N'2025-09-05', 0),
    (N'Ưu đãi mùa thu', N'Mua 2 tặng 1 thuốc cảm cúm', N'2025-09-20', N'2025-10-01', N'2025-10-31', 0),
    (N'Black Friday 2025', N'Giảm 18% đơn hàng trên 1.2 triệu', N'2025-11-20', N'2025-11-25', N'2025-11-30', 0),
    (N'Giáng sinh 2025', N'Giảm 15% toàn bộ sản phẩm', N'2025-12-15', N'2025-12-20', N'2025-12-26', 1),
    (N'Chào năm mới 2026', N'Giảm 22% cho hóa đơn từ 2 triệu', N'2025-12-20', N'2025-12-28', N'2026-01-10', 1);
GO -- Ngắt batch

-- ========================================
-- 8. PROMOTION CONDITIONS & ACTIONS (UPDATED ENUMS)
-- ========================================

-- Summer 2024
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 500000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi mùa hè 2024';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 10, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi mùa hè 2024';

-- Black Friday 2024
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 1000000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Black Friday 2024';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 15, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Black Friday 2024';

-- New Year 2025
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 2000000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Chào đón năm mới 2025';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 20, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Chào đón năm mới 2025';

-- Tet 2025
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 1500000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Tết Nguyên Đán 2025';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 25, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Tết Nguyên Đán 2025';

-- Women's Day 2025
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 300000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi 8/3';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 15, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi 8/3';

-- Mid-year 2025
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 800000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi giữa năm';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 12, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi giữa năm';

-- National Day 2025
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 500000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi Quốc Khánh';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 20, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Khuyến mãi Quốc Khánh';

-- Black Friday 2025
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 1200000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Black Friday 2025';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 18, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Black Friday 2025';

-- Christmas 2025
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 400000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Giáng sinh 2025';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 15, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Giáng sinh 2025';

-- New Year 2026
INSERT INTO dbo.PromotionCondition (promotion, type, comparator, target, value, product, unitOfMeasure)
SELECT p.id, N'ORDER_SUBTOTAL', N'GREATER_EQUAL', N'ORDER_SUBTOTAL', 2000000, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Chào năm mới 2026';

INSERT INTO dbo.PromotionAction (promotion, actionOrder, type, target, value, product, unitOfMeasure)
SELECT p.id, 1, N'PERCENT_DISCOUNT', N'ORDER_SUBTOTAL', 22, NULL, NULL
FROM dbo.Promotion p WHERE p.name = N'Chào năm mới 2026';
GO -- Ngắt batch

-- ========================================
-- 9. SHIFTS DATA (June 2024 - December 2025)
-- ========================================
DECLARE @ShiftDate DATE = '2024-06-01';
DECLARE @EndDate DATE = '2025-12-22';
DECLARE @StaffList TABLE (StaffID NVARCHAR(50), StaffName NVARCHAR(255));

-- Get pharmacist staff
INSERT INTO @StaffList
SELECT id, fullName FROM dbo.Staff WHERE role = 'PHARMACIST' AND isActive = 1;

WHILE @ShiftDate <= @EndDate
    BEGIN
        -- Morning shift (8:00-16:00)
        INSERT INTO dbo.Shift (staff, startTime, endTime, startCash, endCash, systemCash, status, notes, workstation, closedBy, closeReason)
        SELECT TOP 1
            StaffID,
            CAST(@ShiftDate AS DATETIME) + CAST('08:00:00' AS DATETIME),
            CAST(@ShiftDate AS DATETIME) + CAST('16:00:00' AS DATETIME),
            5000000,
            5000000 + (ABS(CHECKSUM(NEWID())) % 8000000) + 1000000, -- 6M-13M VND sales
            5000000 + (ABS(CHECKSUM(NEWID())) % 8000000) + 1000000,
            'CLOSED',
            N'Ca sáng',
            N'POS-01',
            NULL,
            NULL
        FROM @StaffList
        ORDER BY NEWID();

        -- Evening shift (16:00-23:59)
        INSERT INTO dbo.Shift (staff, startTime, endTime, startCash, endCash, systemCash, status, notes, workstation, closedBy, closeReason)
        SELECT TOP 1
            StaffID,
            CAST(@ShiftDate AS DATETIME) + CAST('16:00:00' AS DATETIME),
            CAST(@ShiftDate AS DATETIME) + CAST('23:59:59' AS DATETIME),
            5000000,
            5000000 + (ABS(CHECKSUM(NEWID())) % 6000000) + 500000, -- 5.5M-11.5M VND sales
            5000000 + (ABS(CHECKSUM(NEWID())) % 6000000) + 500000,
            'CLOSED',
            N'Ca tối',
            N'POS-01',
            NULL,
            NULL
        FROM @StaffList
        ORDER BY NEWID();

        SET @ShiftDate = DATEADD(DAY, 1, @ShiftDate);
    END;
GO -- Ngắt batch quan trọng (Fix lỗi biến @ShiftDate)

-- ========================================
-- 10. SAMPLE INVOICES WITH LINES & LOT ALLOCATIONS
-- Creating realistic sales data
-- ========================================

-- Helper: Get first available staff and shift
PRINT N'Generating new realistic invoices...';

DECLARE @CurrentShiftID NVARCHAR(50);
DECLARE @ShiftDate DATE;
DECLARE @ShiftStaff NVARCHAR(50);
DECLARE @InvoiceCount INT;
DECLARE @i INT;
DECLARE @j INT;
DECLARE @NumProducts INT;

-- Cursor duyệt qua tất cả các ca làm việc đã đóng
DECLARE shift_cursor CURSOR FOR
    SELECT id, CAST(startTime AS DATE), staff
    FROM dbo.Shift
    WHERE status = 'CLOSED'
    ORDER BY startTime;

OPEN shift_cursor;
FETCH NEXT FROM shift_cursor INTO @CurrentShiftID, @ShiftDate, @ShiftStaff;

WHILE @@FETCH_STATUS = 0
    BEGIN
        -- Random số lượng hóa đơn trong ca (3 - 8 hóa đơn)
        SET @InvoiceCount = 3 + (ABS(CHECKSUM(NEWID())) % 6);
        SET @i = 0;

        WHILE @i < @InvoiceCount
            BEGIN
                DECLARE @NewInvoiceID NVARCHAR(50) = NEWID();
                DECLARE @CusID NVARCHAR(50) = (SELECT TOP 1 id FROM dbo.Customer ORDER BY NEWID());

                -- Random thời gian tạo hóa đơn trong khung giờ ca làm việc
                DECLARE @CreationTime DATETIME = DATEADD(MINUTE, 30 + (ABS(CHECKSUM(NEWID())) % 400), CAST(@ShiftDate AS DATETIME));

                -- Tạo Hóa đơn (Chỉ các cột cơ bản, bỏ totalAmount/finalAmount/discountAmount)
                INSERT INTO dbo.Invoice (id, type, creationDate, creator, customer, paymentMethod, notes, shift)
                VALUES (@NewInvoiceID, N'SALES', @CreationTime, @ShiftStaff, @CusID, N'CASH', N'Khách lẻ', @CurrentShiftID);

                -- Random số lượng sản phẩm trong hóa đơn (1 - 4 sản phẩm)
                SET @NumProducts = 1 + (ABS(CHECKSUM(NEWID())) % 4);
                SET @j = 0;

                WHILE @j < @NumProducts
                    BEGIN
                        DECLARE @ProdID NVARCHAR(50);
                        DECLARE @UOMID INT;
                        DECLARE @UnitPrice DECIMAL(18, 2);
                        DECLARE @BaseRate FLOAT;

                        -- Lấy ngẫu nhiên 1 sản phẩm và đơn vị tính
                        SELECT TOP 1
                            @ProdID = p.id,
                            @UOMID = u.measurementId,
                            @UnitPrice = u.price,
                            @BaseRate = u.baseUnitConversionRate
                        FROM dbo.Product p
                                 JOIN dbo.UnitOfMeasure u ON p.id = u.product
                        ORDER BY NEWID();

                        DECLARE @BuyQty INT = 1 + (ABS(CHECKSUM(NEWID())) % 2); -- Mua 1 hoặc 2 đơn vị
                        DECLARE @LineID NVARCHAR(50) = NEWID();

                        -- Tạo dòng chi tiết hóa đơn
                        INSERT INTO dbo.InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType)
                        VALUES (@LineID, @NewInvoiceID, @ProdID, @UOMID, @BuyQty, @UnitPrice, N'SALE');

                        -- Xử lý Lot Allocation (Trừ kho lô hàng)
                        DECLARE @LotID NVARCHAR(50);
                        SELECT TOP 1 @LotID = batchNumber
                        FROM dbo.Lot
                        WHERE product = @ProdID AND status = 'AVAILABLE'
                        ORDER BY expiryDate DESC; -- Lấy lô hạn xa nhất

                        IF @LotID IS NOT NULL
                            BEGIN
                                -- Tính số lượng quy đổi ra đơn vị cơ bản (Base Unit)
                                DECLARE @BaseQty INT;

                                -- Logic xử lý rate: Nếu rate < 1 (ví dụ Vỉ = 0.1) thì chia, ngược lại thì nhân
                                -- (Hoặc tùy thuộc vào logic hệ thống của bạn, đây là logic phổ biến)
                                IF @BaseRate < 1 AND @BaseRate > 0
                                    SET @BaseQty = @BuyQty / @BaseRate; -- Ví dụ: 1 Vỉ (rate 0.1) -> 10 viên
                                ELSE
                                    SET @BaseQty = @BuyQty * @BaseRate; -- Ví dụ: 1 Hộp (rate 10 - nếu có) -> 10 viên, hoặc rate 1 -> 1

                                -- Tạo allocation
                                INSERT INTO dbo.LotAllocation (invoiceLine, lot, quantity)
                                VALUES (@LineID, @LotID, @BaseQty);
                            END

                        SET @j = @j + 1;
                    END

                SET @i = @i + 1;
            END

        FETCH NEXT FROM shift_cursor INTO @CurrentShiftID, @ShiftDate, @ShiftStaff;
    END

CLOSE shift_cursor;
DEALLOCATE shift_cursor;

PRINT N'Database updated successfully (Calculated columns removed)!';
GO

-- ========================================
-- COMPLETION MESSAGE
-- ========================================
PRINT N'';
PRINT N'========================================';
PRINT N'DATABASE POPULATION COMPLETED!';
PRINT N'========================================';
PRINT N'Period: June 2024 - December 2025 (19 months)';
PRINT N'';
PRINT N'Data Summary:';
PRINT N'----------------------------------------';
SELECT 'Staff' AS [Table], COUNT(*) AS [Records] FROM dbo.Staff
UNION ALL SELECT 'Customers', COUNT(*) FROM dbo.Customer
UNION ALL SELECT 'Products', COUNT(*) FROM dbo.Product
UNION ALL SELECT 'Measurement Names', COUNT(*) FROM dbo.MeasurementName
UNION ALL SELECT 'Unit of Measure', COUNT(*) FROM dbo.UnitOfMeasure
UNION ALL SELECT 'Lots', COUNT(*) FROM dbo.Lot
UNION ALL SELECT 'Promotions', COUNT(*) FROM dbo.Promotion
UNION ALL SELECT 'Promotion Conditions', COUNT(*) FROM dbo.PromotionCondition
UNION ALL SELECT 'Promotion Actions', COUNT(*) FROM dbo.PromotionAction
UNION ALL SELECT 'Shifts', COUNT(*) FROM dbo.Shift
UNION ALL SELECT 'Invoices', COUNT(*) FROM dbo.Invoice
UNION ALL SELECT 'Invoice Lines', COUNT(*) FROM dbo.InvoiceLine
UNION ALL SELECT 'Lot Allocations', COUNT(*) FROM dbo.LotAllocation;
GO