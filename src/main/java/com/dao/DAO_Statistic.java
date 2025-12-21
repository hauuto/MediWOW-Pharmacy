package com.dao;

import com.entities.*;
import com.enums.InvoiceType;
import com.enums.LotStatus;
import com.enums.ShiftStatus;
import com.interfaces.IStatistic;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO Layer cho module Thống kê & Báo cáo
 * Chứa các truy vấn SQL để lấy dữ liệu thống kê
 *
 * @author MediWOW Team
 */
public class DAO_Statistic {
    private final SessionFactory sessionFactory;

    public DAO_Statistic() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    // ========================================
    // Tab 1: Doanh thu & Lợi nhuận
    // ========================================

    /**
     * Lấy dữ liệu doanh thu theo khoảng thời gian
     *
     * SQL Logic:
     * - Gross Revenue: SUM(InvoiceLine.unitPrice * quantity) WHERE type = 'SALE'
     * - Return Amount: SUM(InvoiceLine.unitPrice * quantity) WHERE type = 'RETURN'
     * - COGS: SUM(LotAllocation.quantity * Lot.rawPrice)
     * - Gross Profit: Net Revenue - COGS
     */
    public List<IStatistic.RevenueData> getRevenueData(LocalDate fromDate, LocalDate toDate) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            // Get all invoices in date range with their lines
            List<Invoice> invoices = session.createQuery(
                "SELECT DISTINCT i FROM Invoice i " +
                "LEFT JOIN FETCH i.invoiceLineList " +
                "WHERE i.creationDate >= :startDate AND i.creationDate < :endDate",
                Invoice.class
            )
            .setParameter("startDate", fromDate.atStartOfDay())
            .setParameter("endDate", toDate.plusDays(1).atStartOfDay())
            .list();

            // Fetch lot allocations
            if (!invoices.isEmpty()) {
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.lotAllocations la " +
                    "LEFT JOIN FETCH la.lot " +
                    "WHERE il.invoice IN :invoices",
                    InvoiceLine.class
                ).setParameter("invoices", invoices).list();
            }

            // Group by date and calculate
            Map<LocalDate, List<Invoice>> invoicesByDate = new HashMap<>();
            for (Invoice inv : invoices) {
                LocalDate date = inv.getCreationDate().toLocalDate();
                invoicesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(inv);
            }

            List<IStatistic.RevenueData> result = new ArrayList<>();
            for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
                List<Invoice> dayInvoices = invoicesByDate.getOrDefault(date, new ArrayList<>());

                BigDecimal grossRevenue = BigDecimal.ZERO;
                BigDecimal returnAmount = BigDecimal.ZERO;
                BigDecimal cogs = BigDecimal.ZERO;
                int invoiceCount = 0;
                int returnCount = 0;

                for (Invoice inv : dayInvoices) {
                    BigDecimal invTotal = calculateInvoiceTotal(inv);

                    if (inv.getType() == InvoiceType.RETURN) {
                        returnAmount = returnAmount.add(invTotal);
                        returnCount++;
                    } else {
                        grossRevenue = grossRevenue.add(invTotal);
                        cogs = cogs.add(calculateInvoiceCOGS(inv));
                        invoiceCount++;
                    }
                }

                BigDecimal netRevenue = grossRevenue.subtract(returnAmount);
                BigDecimal grossProfit = netRevenue.subtract(cogs);

                result.add(new IStatistic.RevenueData(
                    date, grossRevenue, returnAmount, netRevenue,
                    cogs, grossProfit, invoiceCount, returnCount
                ));
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // ========================================
    // Tab 2: Hàng hóa
    // ========================================

    /**
     * Lấy danh sách sản phẩm bán chạy
     *
     * SQL:
     * SELECT p.id, p.name, SUM(il.quantity), SUM(il.unitPrice * il.quantity),
     *        SUM(la.quantity * l.rawPrice) as cogs
     * FROM InvoiceLine il
     * JOIN Product p ON il.product = p.id
     * LEFT JOIN LotAllocation la ON la.invoiceLine = il.id
     * LEFT JOIN Lot l ON la.lot = l.id
     * JOIN Invoice i ON il.invoice = i.id
     * WHERE i.type = 'SALE' AND i.creationDate BETWEEN :from AND :to
     * GROUP BY p.id, p.name
     * ORDER BY SUM(il.unitPrice * il.quantity) DESC
     */
    public List<IStatistic.BestSellerData> getBestSellers(LocalDate fromDate, LocalDate toDate, int limit) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            List<Object[]> results = session.createQuery(
                "SELECT il.unitOfMeasure.product.id, il.unitOfMeasure.product.name, il.unitOfMeasure.product.category, " +
                "SUM(il.quantity), SUM(il.unitPrice * il.quantity) " +
                "FROM InvoiceLine il " +
                "JOIN il.invoice i " +
                "WHERE i.type = :saleType AND i.creationDate >= :startDate AND i.creationDate < :endDate " +
                "GROUP BY il.unitOfMeasure.product.id, il.unitOfMeasure.product.name, il.unitOfMeasure.product.category " +
                "ORDER BY SUM(il.unitPrice * il.quantity) DESC",
                Object[].class
            )
            .setParameter("saleType", InvoiceType.SALES)
            .setParameter("startDate", fromDate.atStartOfDay())
            .setParameter("endDate", toDate.plusDays(1).atStartOfDay())
            .setMaxResults(limit)
            .list();

            List<IStatistic.BestSellerData> data = new ArrayList<>();
            for (Object[] row : results) {
                String productId = (String) row[0];
                String productName = (String) row[1];
                String category = row[2] != null ? row[2].toString() : "";
                int quantitySold = ((Number) row[3]).intValue();
                BigDecimal revenue = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;

                // Get COGS for this product
                BigDecimal cogs = getProductCOGS(session, productId, fromDate, toDate);
                BigDecimal profit = revenue.subtract(cogs);

                data.add(new IStatistic.BestSellerData(
                    productId, productName, category, quantitySold,
                    revenue, cogs, profit
                ));
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Lấy danh sách sản phẩm tồn kho lâu (Dead Stock)
     *
     * Query Logic:
     * - Lấy các sản phẩm không có trong InvoiceLine trong N ngày qua
     * - Hoặc lần bán cuối cùng cách đây > N ngày
     */
    public List<IStatistic.DeadStockData> getDeadStock(int daysSinceLastSale) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            LocalDate cutoffDate = LocalDate.now().minusDays(daysSinceLastSale);

            // Find products with last sale before cutoff date or never sold
            String hql = """
                SELECT p.id, p.name, p.category,
                       (SELECT COALESCE(SUM(l.quantity), 0) FROM Lot l WHERE l.product = p AND l.status = :activeStatus),
                       (SELECT MAX(i.creationDate) FROM Invoice i
                        JOIN InvoiceLine il ON il.invoice = i
                        WHERE il.unitOfMeasure.product = p AND i.type = :saleType)
                FROM Product p
                WHERE NOT EXISTS (
                    SELECT 1 FROM InvoiceLine il2
                    JOIN il2.invoice i2
                    WHERE il2.unitOfMeasure.product = p AND i2.type = :saleType
                    AND i2.creationDate >= :cutoffDateTime
                )
                """;

            List<Object[]> results = session.createQuery(hql, Object[].class)
                .setParameter("activeStatus", LotStatus.AVAILABLE)
                .setParameter("saleType", InvoiceType.SALES)
                .setParameter("cutoffDateTime", cutoffDate.atStartOfDay())
                .list();

            List<IStatistic.DeadStockData> data = new ArrayList<>();
            for (Object[] row : results) {
                String productId = (String) row[0];
                String productName = (String) row[1];
                String category = row[2] != null ? row[2].toString() : "";
                int currentStock = row[3] != null ? ((Number) row[3]).intValue() : 0;
                LocalDateTime lastSoldDateTime = (LocalDateTime) row[4];
                LocalDate lastSoldDate = lastSoldDateTime != null ? lastSoldDateTime.toLocalDate() : null;

                int daysSince = lastSoldDate != null
                    ? (int) java.time.temporal.ChronoUnit.DAYS.between(lastSoldDate, LocalDate.now())
                    : Integer.MAX_VALUE;

                // Get average raw price
                BigDecimal avgRawPrice = getAverageRawPrice(session, productId);

                data.add(new IStatistic.DeadStockData(
                    productId, productName, category, currentStock,
                    lastSoldDate, daysSince, avgRawPrice
                ));
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Lấy danh sách sản phẩm tăng đột biến (Trending Spikes)
     *
     * Logic:
     * - So sánh doanh số hôm nay với trung bình 7 ngày trước
     * - Nếu tăng > 150% thì coi là trending spike
     *
     * SQL Pattern:
     * WITH Today AS (...), Avg7Days AS (...)
     * SELECT ... WHERE today_revenue > avg7_revenue * 1.5
     */
    public List<IStatistic.TrendingData> getTrendingProducts(LocalDate date) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            LocalDate sevenDaysAgo = date.minusDays(7);

            // Get today's sales by product
            Map<String, Object[]> todaySales = new HashMap<>();
            List<Object[]> todayResults = session.createQuery(
                "SELECT il.unitOfMeasure.product.id, il.unitOfMeasure.product.name, il.unitOfMeasure.product.category, " +
                "SUM(il.quantity), SUM(il.unitPrice * il.quantity) " +
                "FROM InvoiceLine il " +
                "JOIN il.invoice i " +
                "WHERE i.type = :saleType " +
                "AND i.creationDate >= :startDate AND i.creationDate < :endDate " +
                "GROUP BY il.unitOfMeasure.product.id, il.unitOfMeasure.product.name, il.unitOfMeasure.product.category",
                Object[].class
            )
            .setParameter("saleType", InvoiceType.SALES)
            .setParameter("startDate", date.atStartOfDay())
            .setParameter("endDate", date.plusDays(1).atStartOfDay())
            .list();

            for (Object[] row : todayResults) {
                todaySales.put((String) row[0], row);
            }

            // Get 7-day average by product
            Map<String, BigDecimal> avgRevenue7Days = new HashMap<>();
            List<Object[]> avg7Results = session.createQuery(
                "SELECT il.unitOfMeasure.product.id, SUM(il.unitPrice * il.quantity) / 7.0 " +
                "FROM InvoiceLine il " +
                "JOIN il.invoice i " +
                "WHERE i.type = :saleType " +
                "AND i.creationDate >= :startDate AND i.creationDate < :endDate " +
                "GROUP BY il.unitOfMeasure.product.id",
                Object[].class
            )
            .setParameter("saleType", InvoiceType.SALES)
            .setParameter("startDate", sevenDaysAgo.atStartOfDay())
            .setParameter("endDate", date.atStartOfDay())
            .list();

            for (Object[] row : avg7Results) {
                String productId = (String) row[0];
                BigDecimal avg = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                avgRevenue7Days.put(productId, avg);
            }

            // Find trending products (> 150% increase)
            List<IStatistic.TrendingData> data = new ArrayList<>();
            for (Map.Entry<String, Object[]> entry : todaySales.entrySet()) {
                String productId = entry.getKey();
                Object[] row = entry.getValue();

                BigDecimal revenueToday = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;
                BigDecimal avg7 = avgRevenue7Days.getOrDefault(productId, BigDecimal.ZERO);

                if (avg7.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentIncrease = revenueToday.subtract(avg7)
                        .divide(avg7, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                    // Only include if > 150% increase
                    if (percentIncrease.compareTo(BigDecimal.valueOf(150)) > 0) {
                        data.add(new IStatistic.TrendingData(
                            productId,
                            (String) row[1],
                            row[2] != null ? row[2].toString() : "",
                            ((Number) row[3]).intValue(),
                            revenueToday,
                            avg7,
                            percentIncrease
                        ));
                    }
                }
            }

            // Sort by percent increase descending
            data.sort((a, b) -> b.percentIncrease().compareTo(a.percentIncrease()));

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Lấy danh sách lô hàng sắp hết hạn
     *
     * SQL:
     * SELECT l.*, p.name
     * FROM Lot l JOIN Product p ON l.product = p.id
     * WHERE l.expiryDate BETWEEN DATEADD(DAY, :daysFrom, GETDATE())
     *                        AND DATEADD(DAY, :daysTo, GETDATE())
     *   AND l.status = 'ACTIVE' AND l.quantity > 0
     * ORDER BY l.expiryDate ASC
     */
    public List<IStatistic.ExpiryAlertData> getExpiryAlerts(int daysFromNow, int daysToNow) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            LocalDateTime fromDate = LocalDate.now().plusDays(daysFromNow).atStartOfDay();
            LocalDateTime toDate = LocalDate.now().plusDays(daysToNow).atTime(LocalTime.MAX);

            List<Lot> lots = session.createQuery(
                "SELECT l FROM Lot l " +
                "JOIN FETCH l.product " +
                "WHERE l.expiryDate >= :fromDate AND l.expiryDate <= :toDate " +
                "AND l.status = :status AND l.quantity > 0 " +
                "ORDER BY l.expiryDate ASC",
                Lot.class
            )
            .setParameter("fromDate", fromDate)
            .setParameter("toDate", toDate)
            .setParameter("status", LotStatus.AVAILABLE)
            .list();

            List<IStatistic.ExpiryAlertData> data = new ArrayList<>();
            for (Lot lot : lots) {
                int daysUntilExpiry = (int) java.time.temporal.ChronoUnit.DAYS
                    .between(LocalDate.now(), lot.getExpiryDate().toLocalDate());

                data.add(new IStatistic.ExpiryAlertData(
                    lot.getId(),
                    lot.getBatchNumber(),
                    lot.getProduct().getId(),
                    lot.getProduct().getName(),
                    lot.getQuantity(),
                    lot.getExpiryDate().toLocalDate(),
                    daysUntilExpiry
                ));
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // ========================================
    // Tab 3: Kiểm soát & Nhân viên
    // ========================================

    /**
     * Lấy dữ liệu đối soát tiền mặt theo ca
     */
    public List<IStatistic.CashAuditData> getCashAuditData(LocalDate fromDate, LocalDate toDate) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            List<Shift> shifts = session.createQuery(
                "SELECT s FROM Shift s " +
                "JOIN FETCH s.staff " +
                "WHERE s.startTime >= :startDate AND s.startTime < :endDate " +
                "ORDER BY s.startTime DESC",
                Shift.class
            )
            .setParameter("startDate", fromDate.atStartOfDay())
            .setParameter("endDate", toDate.plusDays(1).atStartOfDay())
            .list();

            List<IStatistic.CashAuditData> data = new ArrayList<>();
            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

            for (Shift shift : shifts) {
                BigDecimal startCash = shift.getStartCash() != null ? shift.getStartCash() : BigDecimal.ZERO;
                BigDecimal systemCash = shift.getSystemCash() != null ? shift.getSystemCash() : BigDecimal.ZERO;
                BigDecimal actualCash = shift.getEndCash() != null ? shift.getEndCash() : BigDecimal.ZERO;
                BigDecimal mismatch = actualCash.subtract(systemCash);

                data.add(new IStatistic.CashAuditData(
                    shift.getId(),
                    shift.getStaff().getId(),
                    shift.getStaff().getFullName(),
                    shift.getStartTime().toLocalDate(),
                    shift.getStartTime().format(timeFmt),
                    shift.getEndTime() != null ? shift.getEndTime().format(timeFmt) : "Đang mở",
                    startCash,
                    systemCash,
                    actualCash,
                    mismatch,
                    mismatch.compareTo(BigDecimal.ZERO) < 0
                ));
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Lấy dữ liệu hiệu suất nhân viên
     */
    public List<IStatistic.StaffPerformanceData> getStaffPerformance(LocalDate fromDate, LocalDate toDate) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            List<Object[]> results = session.createQuery(
                "SELECT i.creator.id, i.creator.fullName, i.creator.role, " +
                "COUNT(i), SUM(il.unitPrice * il.quantity) " +
                "FROM Invoice i " +
                "JOIN i.invoiceLineList il " +
                "WHERE i.type = :saleType " +
                "AND i.creationDate >= :startDate AND i.creationDate < :endDate " +
                "GROUP BY i.creator.id, i.creator.fullName, i.creator.role " +
                "ORDER BY SUM(il.unitPrice * il.quantity) DESC",
                Object[].class
            )
            .setParameter("saleType", InvoiceType.SALES)
            .setParameter("startDate", fromDate.atStartOfDay())
            .setParameter("endDate", toDate.plusDays(1).atStartOfDay())
            .list();

            List<IStatistic.StaffPerformanceData> data = new ArrayList<>();
            int rank = 1;
            for (Object[] row : results) {
                String staffId = (String) row[0];
                String staffName = (String) row[1];
                String role = row[2] != null ? row[2].toString() : "";
                int invoiceCount = ((Number) row[3]).intValue();
                BigDecimal totalRevenue = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;
                BigDecimal avgValue = invoiceCount > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(invoiceCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                data.add(new IStatistic.StaffPerformanceData(
                    staffId, staffName, role, invoiceCount, totalRevenue, avgValue, rank++
                ));
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // ========================================
    // Tab 4: Hiệu quả Khuyến mãi
    // ========================================

    /**
     * Lấy thống kê khuyến mãi
     */
    public List<IStatistic.PromotionStatsData> getPromotionStats(LocalDate fromDate, LocalDate toDate) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            List<Object[]> results = session.createQuery(
                "SELECT i.promotion.id, i.promotion.name, " +
                "i.promotion.effectiveDate, i.promotion.endDate, " +
                "COUNT(i), SUM(il.unitPrice * il.quantity) " +
                "FROM Invoice i " +
                "JOIN i.invoiceLineList il " +
                "WHERE i.promotion IS NOT NULL " +
                "AND i.creationDate >= :startDate AND i.creationDate < :endDate " +
                "GROUP BY i.promotion.id, i.promotion.name, i.promotion.effectiveDate, i.promotion.endDate " +
                "ORDER BY COUNT(i) DESC",
                Object[].class
            )
            .setParameter("startDate", fromDate.atStartOfDay())
            .setParameter("endDate", toDate.plusDays(1).atStartOfDay())
            .list();

            List<IStatistic.PromotionStatsData> data = new ArrayList<>();
            for (Object[] row : results) {
                String promoId = (String) row[0];
                String promoName = (String) row[1];
                LocalDate effectiveDate = (LocalDate) row[2];
                LocalDate endDate = (LocalDate) row[3];
                int usageCount = ((Number) row[4]).intValue();
                BigDecimal revenue = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;

                // Calculate total discount (simplified - actual discount calculation may be more complex)
                BigDecimal totalDiscount = calculatePromotionDiscount(session, promoId, fromDate, toDate);
                BigDecimal avgOrderValue = usageCount > 0
                    ? revenue.divide(BigDecimal.valueOf(usageCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                data.add(new IStatistic.PromotionStatsData(
                    promoId, promoName, effectiveDate, endDate,
                    usageCount, totalDiscount, revenue, avgOrderValue
                ));
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private BigDecimal calculateInvoiceTotal(Invoice invoice) {
        if (invoice == null || invoice.getInvoiceLineList() == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
            if (line != null && line.getUnitPrice() != null) {
                total = total.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            }
        }
        return total;
    }

    private BigDecimal calculateInvoiceCOGS(Invoice invoice) {
        if (invoice == null || invoice.getInvoiceLineList() == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
            if (line == null || line.getLotAllocations() == null) continue;
            for (LotAllocation allocation : line.getLotAllocations()) {
                if (allocation != null && allocation.getLot() != null && allocation.getLot().getRawPrice() != null) {
                    total = total.add(allocation.getLot().getRawPrice()
                        .multiply(BigDecimal.valueOf(allocation.getQuantity())));
                }
            }
        }
        return total;
    }

    private BigDecimal getProductCOGS(Session session, String productId, LocalDate fromDate, LocalDate toDate) {
        try {
            Object result = session.createQuery(
                "SELECT SUM(la.quantity * la.lot.rawPrice) " +
                "FROM LotAllocation la " +
                "JOIN la.invoiceLine il " +
                "JOIN il.invoice i " +
                "WHERE il.unitOfMeasure.product.id = :productId " +
                "AND i.type = :saleType " +
                "AND i.creationDate >= :startDate AND i.creationDate < :endDate",
                BigDecimal.class
            )
            .setParameter("productId", productId)
            .setParameter("saleType", InvoiceType.SALES)
            .setParameter("startDate", fromDate.atStartOfDay())
            .setParameter("endDate", toDate.plusDays(1).atStartOfDay())
            .uniqueResult();

            return result != null ? (BigDecimal) result : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getAverageRawPrice(Session session, String productId) {
        try {
            Object result = session.createQuery(
                "SELECT AVG(l.rawPrice) FROM Lot l WHERE l.product.id = :productId AND l.status = :status"
            )
            .setParameter("productId", productId)
            .setParameter("status", LotStatus.AVAILABLE)
            .uniqueResult();

            return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculatePromotionDiscount(Session session, String promotionId,
                                                   LocalDate fromDate, LocalDate toDate) {
        try {
            // Simplified: estimate discount as 10% of revenue for promotional invoices
            // In real implementation, calculate based on PromotionAction rules
            Object result = session.createQuery(
                "SELECT SUM(il.unitPrice * il.quantity) * 0.1 " +
                "FROM Invoice i JOIN i.invoiceLineList il " +
                "WHERE i.promotion.id = :promoId " +
                "AND i.creationDate >= :startDate AND i.creationDate < :endDate"
            )
            .setParameter("promoId", promotionId)
            .setParameter("startDate", fromDate.atStartOfDay())
            .setParameter("endDate", toDate.plusDays(1).atStartOfDay())
            .uniqueResult();

            return result != null ? new BigDecimal(result.toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}

