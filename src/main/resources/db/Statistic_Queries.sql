-- =====================================================
-- SQL Queries cho Module Thống kê & Báo cáo (MediWOW)
-- Author: MediWOW Team
-- =====================================================

-- =====================================================
-- 1. QUERY TÍNH LỢI NHUẬN (Profit Calculation)
-- =====================================================

-- Tính doanh thu, COGS (giá vốn), và lợi nhuận theo ngày
-- Yêu cầu: JOIN Invoice -> InvoiceLine -> LotAllocation -> Lot

/*
Logic tính toán:
- Gross Revenue = Tổng (unitPrice * quantity) của các hóa đơn bán
- Return Amount = Tổng giá trị đơn trả hàng
- Net Revenue = Gross Revenue - Return Amount
- COGS (Cost of Goods Sold) = Tổng (rawPrice * quantity) từ LotAllocation
- Gross Profit = Net Revenue - COGS
*/

DECLARE @fromDate DATE = '2025-12-01';
DECLARE @toDate DATE = '2025-12-21';

-- Bảng tạm để lưu dữ liệu tính toán
WITH SalesData AS (
    SELECT
        CAST(i.creationDate AS DATE) AS salesDate,
        i.type AS invoiceType,
        i.id AS invoiceId,
        SUM(il.unitPrice * il.quantity) AS lineTotal
    FROM Invoice i
    INNER JOIN InvoiceLine il ON il.invoice = i.id
    WHERE i.creationDate >= @fromDate
      AND i.creationDate < DATEADD(DAY, 1, @toDate)
    GROUP BY CAST(i.creationDate AS DATE), i.type, i.id
),
COGSData AS (
    SELECT
        CAST(i.creationDate AS DATE) AS salesDate,
        SUM(la.quantity * l.rawPrice) AS totalCOGS
    FROM Invoice i
    INNER JOIN InvoiceLine il ON il.invoice = i.id
    INNER JOIN LotAllocation la ON la.invoiceLine = il.id
    INNER JOIN Lot l ON la.lot = l.id
    WHERE i.type = 'SALE'
      AND i.creationDate >= @fromDate
      AND i.creationDate < DATEADD(DAY, 1, @toDate)
    GROUP BY CAST(i.creationDate AS DATE)
)
SELECT
    d.dt AS [Ngày],
    ISNULL(SUM(CASE WHEN sd.invoiceType = 'SALE' THEN sd.lineTotal ELSE 0 END), 0) AS [Doanh thu gộp],
    ISNULL(SUM(CASE WHEN sd.invoiceType = 'RETURN' THEN sd.lineTotal ELSE 0 END), 0) AS [Tiền trả hàng],
    ISNULL(SUM(CASE WHEN sd.invoiceType = 'SALE' THEN sd.lineTotal ELSE 0 END), 0)
        - ISNULL(SUM(CASE WHEN sd.invoiceType = 'RETURN' THEN sd.lineTotal ELSE 0 END), 0) AS [Doanh thu thuần],
    ISNULL(c.totalCOGS, 0) AS [Giá vốn (COGS)],
    ISNULL(SUM(CASE WHEN sd.invoiceType = 'SALE' THEN sd.lineTotal ELSE 0 END), 0)
        - ISNULL(SUM(CASE WHEN sd.invoiceType = 'RETURN' THEN sd.lineTotal ELSE 0 END), 0)
        - ISNULL(c.totalCOGS, 0) AS [Lợi nhuận gộp],
    COUNT(DISTINCT CASE WHEN sd.invoiceType = 'SALE' THEN sd.invoiceId END) AS [Số hóa đơn],
    COUNT(DISTINCT CASE WHEN sd.invoiceType = 'RETURN' THEN sd.invoiceId END) AS [Số đơn trả]
FROM (
    -- Generate date series
    SELECT DATEADD(DAY, number, @fromDate) AS dt
    FROM master..spt_values
    WHERE type = 'P'
      AND DATEADD(DAY, number, @fromDate) <= @toDate
) d
LEFT JOIN SalesData sd ON d.dt = sd.salesDate
LEFT JOIN COGSData c ON d.dt = c.salesDate
GROUP BY d.dt, c.totalCOGS
ORDER BY d.dt;


-- =====================================================
-- 2. QUERY TÌM SẢN PHẨM "TRENDING SPIKES"
-- =====================================================

/*
Logic:
- Sức bán hôm nay (revenueToday) > 150% so với trung bình 7 ngày trước (avgRevenue7Days)
- Công thức: percentIncrease = ((revenueToday - avgRevenue7Days) / avgRevenue7Days) * 100 > 150
*/

DECLARE @targetDate DATE = CAST(GETDATE() AS DATE);
DECLARE @sevenDaysAgo DATE = DATEADD(DAY, -7, @targetDate);

WITH TodaySales AS (
    -- Doanh số hôm nay theo sản phẩm
    SELECT
        il.product AS productId,
        SUM(il.quantity) AS quantityToday,
        SUM(il.unitPrice * il.quantity) AS revenueToday
    FROM Invoice i
    INNER JOIN InvoiceLine il ON il.invoice = i.id
    WHERE i.type = 'SALE'
      AND CAST(i.creationDate AS DATE) = @targetDate
    GROUP BY il.product
),
Last7DaysSales AS (
    -- Trung bình doanh thu 7 ngày trước (không tính hôm nay)
    SELECT
        il.product AS productId,
        SUM(il.unitPrice * il.quantity) / 7.0 AS avgRevenue7Days
    FROM Invoice i
    INNER JOIN InvoiceLine il ON il.invoice = i.id
    WHERE i.type = 'SALE'
      AND CAST(i.creationDate AS DATE) >= @sevenDaysAgo
      AND CAST(i.creationDate AS DATE) < @targetDate
    GROUP BY il.product
)
SELECT
    p.id AS [Mã SP],
    p.name AS [Tên sản phẩm],
    p.category AS [Nhóm],
    ts.quantityToday AS [SL hôm nay],
    ts.revenueToday AS [DT hôm nay],
    l7d.avgRevenue7Days AS [TB 7 ngày],
    ROUND(((ts.revenueToday - l7d.avgRevenue7Days) / l7d.avgRevenue7Days) * 100, 2) AS [% Tăng]
FROM TodaySales ts
INNER JOIN Last7DaysSales l7d ON ts.productId = l7d.productId
INNER JOIN Product p ON ts.productId = p.id
WHERE l7d.avgRevenue7Days > 0  -- Tránh chia cho 0
  AND ts.revenueToday > l7d.avgRevenue7Days * 1.5  -- Tăng > 150%
ORDER BY ((ts.revenueToday - l7d.avgRevenue7Days) / l7d.avgRevenue7Days) DESC;


-- =====================================================
-- 3. QUERY TÌM DEAD STOCK (Sản phẩm tồn kho lâu)
-- =====================================================

/*
Logic: Sản phẩm không bán được trong N ngày qua (mặc định 90 ngày)
*/

DECLARE @cutoffDays INT = 90;
DECLARE @cutoffDate DATE = DATEADD(DAY, -@cutoffDays, GETDATE());

SELECT
    p.id AS [Mã SP],
    p.name AS [Tên sản phẩm],
    p.category AS [Nhóm],
    ISNULL(SUM(l.quantity), 0) AS [Tồn kho hiện tại],
    MAX(lastSale.lastSoldDate) AS [Lần bán cuối],
    CASE
        WHEN MAX(lastSale.lastSoldDate) IS NULL THEN 'Chưa bán'
        ELSE CAST(DATEDIFF(DAY, MAX(lastSale.lastSoldDate), GETDATE()) AS NVARCHAR(10))
    END AS [Số ngày không bán],
    AVG(l.rawPrice) AS [Giá vốn TB]
FROM Product p
LEFT JOIN Lot l ON l.product = p.id AND l.status = 'ACTIVE'
LEFT JOIN (
    SELECT
        il.product AS productId,
        MAX(CAST(i.creationDate AS DATE)) AS lastSoldDate
    FROM Invoice i
    INNER JOIN InvoiceLine il ON il.invoice = i.id
    WHERE i.type = 'SALE'
    GROUP BY il.product
) lastSale ON lastSale.productId = p.id
WHERE NOT EXISTS (
    SELECT 1
    FROM Invoice i2
    INNER JOIN InvoiceLine il2 ON il2.invoice = i2.id
    WHERE il2.product = p.id
      AND i2.type = 'SALE'
      AND i2.creationDate >= @cutoffDate
)
GROUP BY p.id, p.name, p.category
ORDER BY [Số ngày không bán] DESC;


-- =====================================================
-- 4. QUERY LÔ HÀNG SẮP HẾT HẠN (Expiry Alert)
-- =====================================================

DECLARE @daysFrom INT = 0;   -- Từ hôm nay
DECLARE @daysTo INT = 90;    -- Đến 90 ngày tới

SELECT
    l.id AS [Mã Lô],
    l.batchNumber AS [Số lô],
    p.id AS [Mã SP],
    p.name AS [Tên sản phẩm],
    l.quantity AS [Số lượng],
    CAST(l.expiryDate AS DATE) AS [Ngày hết hạn],
    DATEDIFF(DAY, GETDATE(), l.expiryDate) AS [Còn lại (ngày)]
FROM Lot l
INNER JOIN Product p ON l.product = p.id
WHERE l.status = 'ACTIVE'
  AND l.quantity > 0
  AND l.expiryDate >= DATEADD(DAY, @daysFrom, CAST(GETDATE() AS DATE))
  AND l.expiryDate <= DATEADD(DAY, @daysTo, CAST(GETDATE() AS DATE))
ORDER BY l.expiryDate ASC;


-- =====================================================
-- 5. QUERY ĐỐI SOÁT TIỀN MẶT THEO CA (Cash Audit)
-- =====================================================

DECLARE @auditFromDate DATE = '2025-12-01';
DECLARE @auditToDate DATE = '2025-12-21';

SELECT
    s.id AS [Mã ca],
    st.fullName AS [Nhân viên],
    CAST(s.startTime AS DATE) AS [Ngày],
    FORMAT(s.startTime, 'HH:mm') AS [Giờ mở],
    CASE WHEN s.endTime IS NOT NULL THEN FORMAT(s.endTime, 'HH:mm') ELSE N'Đang mở' END AS [Giờ đóng],
    s.startCash AS [Tiền đầu ca],
    ISNULL(s.systemCash, 0) AS [Tiền hệ thống],
    ISNULL(s.endCash, 0) AS [Tiền thực tế],
    ISNULL(s.endCash, 0) - ISNULL(s.systemCash, 0) AS [Chênh lệch]
FROM Shift s
INNER JOIN Staff st ON s.staff = st.id
WHERE s.startTime >= @auditFromDate
  AND s.startTime < DATEADD(DAY, 1, @auditToDate)
ORDER BY s.startTime DESC;


-- =====================================================
-- 6. QUERY HIỆU SUẤT NHÂN VIÊN (Staff Performance)
-- =====================================================

DECLARE @perfFromDate DATE = '2025-12-01';
DECLARE @perfToDate DATE = '2025-12-21';

SELECT
    ROW_NUMBER() OVER (ORDER BY SUM(il.unitPrice * il.quantity) DESC) AS [Hạng],
    st.id AS [Mã NV],
    st.fullName AS [Tên nhân viên],
    st.role AS [Vai trò],
    COUNT(DISTINCT i.id) AS [Số hóa đơn],
    SUM(il.unitPrice * il.quantity) AS [Tổng doanh thu],
    SUM(il.unitPrice * il.quantity) / NULLIF(COUNT(DISTINCT i.id), 0) AS [TB/Hóa đơn]
FROM Invoice i
INNER JOIN InvoiceLine il ON il.invoice = i.id
INNER JOIN Staff st ON i.creator = st.id
WHERE i.type = 'SALE'
  AND i.creationDate >= @perfFromDate
  AND i.creationDate < DATEADD(DAY, 1, @perfToDate)
GROUP BY st.id, st.fullName, st.role
ORDER BY [Tổng doanh thu] DESC;


-- =====================================================
-- 7. QUERY THỐNG KÊ KHUYẾN MÃI (Promotion Stats)
-- =====================================================

DECLARE @promoFromDate DATE = '2025-12-01';
DECLARE @promoToDate DATE = '2025-12-21';

SELECT
    p.id AS [Mã KM],
    p.name AS [Tên khuyến mãi],
    p.effectiveDate AS [Ngày hiệu lực],
    p.endDate AS [Ngày kết thúc],
    COUNT(DISTINCT i.id) AS [Số lượt dùng],
    -- Ước tính tổng tiền đã giảm (10% doanh thu - có thể điều chỉnh)
    SUM(il.unitPrice * il.quantity) * 0.1 AS [Tổng tiền giảm (ước tính)],
    SUM(il.unitPrice * il.quantity) AS [Doanh thu KM],
    SUM(il.unitPrice * il.quantity) / NULLIF(COUNT(DISTINCT i.id), 0) AS [TB/Đơn hàng]
FROM Invoice i
INNER JOIN InvoiceLine il ON il.invoice = i.id
INNER JOIN Promotion p ON i.promotion = p.id
WHERE i.creationDate >= @promoFromDate
  AND i.creationDate < DATEADD(DAY, 1, @promoToDate)
GROUP BY p.id, p.name, p.effectiveDate, p.endDate
ORDER BY COUNT(DISTINCT i.id) DESC;

