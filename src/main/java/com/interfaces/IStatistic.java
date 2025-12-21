package com.interfaces;

import com.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Interface cho module Thống kê & Báo cáo
 * @author Tô Thanh Hậu
 */
public interface IStatistic {

    // ===== Tab 1: Doanh thu & Lợi nhuận (Manager Only) =====

    /**
     * DTO cho dữ liệu doanh thu theo ngày
     */
    record RevenueData(
        LocalDate date,
        BigDecimal grossRevenue,      // Tổng doanh thu
        BigDecimal returnAmount,       // Tiền trả hàng
        BigDecimal netRevenue,         // Doanh thu thuần = grossRevenue - returnAmount
        BigDecimal cogs,               // Cost of Goods Sold (Giá vốn hàng bán)
        BigDecimal grossProfit,        // Lợi nhuận gộp = netRevenue - cogs
        int invoiceCount,              // Số hóa đơn
        int returnCount                // Số đơn trả hàng
    ) {}

    List<RevenueData> getRevenueReport(LocalDate fromDate, LocalDate toDate, Role userRole);

    // ===== Tab 2: Hàng hóa =====

    /**
     * DTO cho sản phẩm bán chạy
     */
    record BestSellerData(
        String productId,
        String productName,
        String category,
        int quantitySold,
        BigDecimal revenue,
        BigDecimal rawPrice,  // Giá vốn - chỉ Manager được xem
        BigDecimal profit     // Lợi nhuận - chỉ Manager được xem
    ) {}

    /**
     * DTO cho sản phẩm tồn kho lâu (Dead Stock)
     */
    record DeadStockData(
        String productId,
        String productName,
        String category,
        int currentStock,
        LocalDate lastSoldDate,
        int daysSinceLastSale,
        BigDecimal rawPrice  // Giá vốn - chỉ Manager được xem
    ) {}

    /**
     * DTO cho sản phẩm tăng đột biến (Trending Spikes)
     */
    record TrendingData(
        String productId,
        String productName,
        String category,
        int quantityToday,
        BigDecimal revenueToday,
        BigDecimal avgRevenue7Days,
        BigDecimal percentIncrease
    ) {}

    /**
     * DTO cho lô hàng sắp hết hạn
     */
    record ExpiryAlertData(
        String lotId,
        String batchNumber,
        String productId,
        String productName,
        int quantity,
        LocalDate expiryDate,
        int daysUntilExpiry
    ) {}

    List<BestSellerData> getBestSellers(LocalDate fromDate, LocalDate toDate, int limit, Role userRole);
    List<DeadStockData> getDeadStock(int daysSinceLastSale, Role userRole);
    List<TrendingData> getTrendingProducts(LocalDate date, Role userRole);
    List<ExpiryAlertData> getExpiryAlerts(int daysFromNow, int daysToNow);

    // ===== Tab 3: Kiểm soát & Nhân viên =====

    /**
     * DTO cho đối soát tiền mặt theo ca
     */
    record CashAuditData(
        String shiftId,
        String staffId,
        String staffName,
        LocalDate shiftDate,
        String startTime,
        String endTime,
        BigDecimal startCash,
        BigDecimal systemCash,      // Tiền hệ thống tính toán
        BigDecimal actualCash,      // Tiền thực tế cuối ca
        BigDecimal mismatch,        // Chênh lệch = actualCash - systemCash
        boolean isNegativeMismatch  // true nếu lệch âm (nguy cơ thất thoát)
    ) {}

    /**
     * DTO cho hiệu suất nhân viên
     */
    record StaffPerformanceData(
        String staffId,
        String staffName,
        String role,
        int invoiceCount,
        BigDecimal totalRevenue,
        BigDecimal averageInvoiceValue,
        int rank
    ) {}

    List<CashAuditData> getCashAuditReport(LocalDate fromDate, LocalDate toDate);
    List<StaffPerformanceData> getStaffPerformance(LocalDate fromDate, LocalDate toDate, Role userRole);

    // ===== Tab 4: Hiệu quả Khuyến mãi (Manager Only) =====

    /**
     * DTO cho thống kê khuyến mãi
     */
    record PromotionStatsData(
        String promotionId,
        String promotionName,
        LocalDate effectiveDate,
        LocalDate endDate,
        int usageCount,              // Số lượt sử dụng
        BigDecimal totalDiscount,    // Tổng tiền đã giảm
        BigDecimal revenueFromPromo, // Doanh thu từ đơn có khuyến mãi
        BigDecimal avgOrderValue     // Giá trị đơn hàng trung bình
    ) {}

    List<PromotionStatsData> getPromotionStats(LocalDate fromDate, LocalDate toDate, Role userRole);

    // ===== Tổng hợp =====

    /**
     * DTO cho tổng quan thống kê
     */
    record StatisticSummary(
        BigDecimal totalRevenue,
        BigDecimal totalProfit,
        int totalInvoices,
        int totalReturns,
        BigDecimal totalCashMismatch,
        int expiringLots
    ) {}

    StatisticSummary getSummary(LocalDate fromDate, LocalDate toDate, Role userRole);
}

