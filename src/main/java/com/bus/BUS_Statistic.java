package com.bus;

import com.dao.DAO_Statistic;
import com.enums.Role;
import com.interfaces.IStatistic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Business Logic Layer cho module Thống kê & Báo cáo
 * Thực hiện kiểm tra phân quyền (Role-based access control)
 *
 * @author MediWOW Team
 */
public class BUS_Statistic implements IStatistic {

    private final DAO_Statistic daoStatistic;

    public BUS_Statistic() {
        this.daoStatistic = new DAO_Statistic();
    }

    // ========================================
    // Tab 1: Doanh thu & Lợi nhuận (Manager Only)
    // ========================================

    @Override
    public List<RevenueData> getRevenueReport(LocalDate fromDate, LocalDate toDate, Role userRole) {
        // Security Check: Chỉ Manager mới được xem báo cáo doanh thu/lợi nhuận
        if (userRole != Role.MANAGER) {
            throw new SecurityException("Chỉ Quản lý mới được phép xem báo cáo doanh thu và lợi nhuận.");
        }

        validateDateRange(fromDate, toDate);
        return daoStatistic.getRevenueData(fromDate, toDate);
    }

    // ========================================
    // Tab 2: Hàng hóa
    // ========================================

    @Override
    public List<BestSellerData> getBestSellers(LocalDate fromDate, LocalDate toDate, int limit, Role userRole) {
        validateDateRange(fromDate, toDate);

        List<BestSellerData> data = daoStatistic.getBestSellers(fromDate, toDate, limit);

        // Security Filter: Pharmacist không được xem giá vốn và lợi nhuận
        if (userRole != Role.MANAGER) {
            return filterBestSellerForPharmacist(data);
        }

        return data;
    }

    /**
     * Lọc dữ liệu BestSeller cho Pharmacist - ẩn giá vốn và lợi nhuận
     */
    private List<BestSellerData> filterBestSellerForPharmacist(List<BestSellerData> data) {
        List<BestSellerData> filtered = new ArrayList<>();
        for (BestSellerData item : data) {
            filtered.add(new BestSellerData(
                item.productId(),
                item.productName(),
                item.category(),
                item.quantitySold(),
                item.revenue(),
                null,  // rawPrice - HIDDEN for Pharmacist
                null   // profit - HIDDEN for Pharmacist
            ));
        }
        return filtered;
    }

    @Override
    public List<DeadStockData> getDeadStock(int daysSinceLastSale, Role userRole) {
        if (daysSinceLastSale < 0) {
            throw new IllegalArgumentException("Số ngày phải >= 0");
        }

        List<DeadStockData> data = daoStatistic.getDeadStock(daysSinceLastSale);

        // Security Filter: Pharmacist không được xem giá vốn
        if (userRole != Role.MANAGER) {
            return filterDeadStockForPharmacist(data);
        }

        return data;
    }

    /**
     * Lọc dữ liệu DeadStock cho Pharmacist - ẩn giá vốn
     */
    private List<DeadStockData> filterDeadStockForPharmacist(List<DeadStockData> data) {
        List<DeadStockData> filtered = new ArrayList<>();
        for (DeadStockData item : data) {
            filtered.add(new DeadStockData(
                item.productId(),
                item.productName(),
                item.category(),
                item.currentStock(),
                item.lastSoldDate(),
                item.daysSinceLastSale(),
                null  // rawPrice - HIDDEN for Pharmacist
            ));
        }
        return filtered;
    }

    @Override
    public List<TrendingData> getTrendingProducts(LocalDate date, Role userRole) {
        if (date == null) {
            date = LocalDate.now();
        }
        // Trending data không chứa thông tin tài chính nhạy cảm
        // Cả Manager và Pharmacist đều có thể xem
        return daoStatistic.getTrendingProducts(date);
    }

    @Override
    public List<ExpiryAlertData> getExpiryAlerts(int daysFromNow, int daysToNow) {
        if (daysFromNow < 0 || daysToNow < 0) {
            throw new IllegalArgumentException("Số ngày phải >= 0");
        }
        if (daysFromNow > daysToNow) {
            throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn ngày kết thúc");
        }
        // Expiry alerts không chứa thông tin tài chính nhạy cảm
        // Cả Manager và Pharmacist đều có thể xem
        return daoStatistic.getExpiryAlerts(daysFromNow, daysToNow);
    }

    // ========================================
    // Tab 3: Kiểm soát & Nhân viên
    // ========================================

    @Override
    public List<CashAuditData> getCashAuditReport(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        // Cash audit có thể xem bởi cả Manager và Pharmacist
        // Nhưng chỉ hiển thị đầy đủ cho Manager
        return daoStatistic.getCashAuditData(fromDate, toDate);
    }

    @Override
    public List<StaffPerformanceData> getStaffPerformance(LocalDate fromDate, LocalDate toDate, Role userRole) {
        validateDateRange(fromDate, toDate);

        List<StaffPerformanceData> data = daoStatistic.getStaffPerformance(fromDate, toDate);

        // Security: Pharmacist chỉ xem được hiệu suất của chính mình
        // Logic này nên được implement ở GUI level để filter theo staffId
        // Ở đây ta chỉ ẩn revenue nếu không phải Manager
        if (userRole != Role.MANAGER) {
            return filterStaffPerformanceForPharmacist(data);
        }

        return data;
    }

    /**
     * Lọc dữ liệu StaffPerformance cho Pharmacist - ẩn tổng doanh thu
     */
    private List<StaffPerformanceData> filterStaffPerformanceForPharmacist(List<StaffPerformanceData> data) {
        List<StaffPerformanceData> filtered = new ArrayList<>();
        for (StaffPerformanceData item : data) {
            filtered.add(new StaffPerformanceData(
                item.staffId(),
                item.staffName(),
                item.role(),
                item.invoiceCount(),
                null,  // totalRevenue - HIDDEN for Pharmacist
                null,  // averageInvoiceValue - HIDDEN for Pharmacist
                item.rank()
            ));
        }
        return filtered;
    }

    // ========================================
    // Tab 4: Hiệu quả Khuyến mãi (Manager Only)
    // ========================================

    @Override
    public List<PromotionStatsData> getPromotionStats(LocalDate fromDate, LocalDate toDate, Role userRole) {
        // Security Check: Chỉ Manager mới được xem thống kê khuyến mãi
        if (userRole != Role.MANAGER) {
            throw new SecurityException("Chỉ Quản lý mới được phép xem thống kê khuyến mãi.");
        }

        validateDateRange(fromDate, toDate);
        return daoStatistic.getPromotionStats(fromDate, toDate);
    }

    // ========================================
    // Tổng hợp
    // ========================================

    @Override
    public StatisticSummary getSummary(LocalDate fromDate, LocalDate toDate, Role userRole) {
        validateDateRange(fromDate, toDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int totalInvoices = 0;
        int totalReturns = 0;
        BigDecimal totalCashMismatch = BigDecimal.ZERO;

        // Get revenue data (only for Manager)
        if (userRole == Role.MANAGER) {
            List<RevenueData> revenueData = daoStatistic.getRevenueData(fromDate, toDate);
            for (RevenueData rd : revenueData) {
                totalRevenue = totalRevenue.add(rd.netRevenue());
                totalProfit = totalProfit.add(rd.grossProfit());
                totalInvoices += rd.invoiceCount();
                totalReturns += rd.returnCount();
            }
        }

        // Get cash mismatch (visible for all but sensitive info)
        List<CashAuditData> cashData = daoStatistic.getCashAuditData(fromDate, toDate);
        for (CashAuditData cd : cashData) {
            totalCashMismatch = totalCashMismatch.add(cd.mismatch());
        }

        // Get expiring lots count
        List<ExpiryAlertData> expiryData = daoStatistic.getExpiryAlerts(0, 90);
        int expiringLots = expiryData.size();

        // Hide financial data for Pharmacist
        if (userRole != Role.MANAGER) {
            totalRevenue = null;
            totalProfit = null;
        }

        return new StatisticSummary(
            totalRevenue, totalProfit, totalInvoices, totalReturns,
            totalCashMismatch, expiringLots
        );
    }

    // ========================================
    // Validation Helpers
    // ========================================

    /**
     * Validate date range
     */
    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống.");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
        if (toDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày kết thúc không được lớn hơn ngày hiện tại.");
        }
    }

    /**
     * Check if user has Manager role
     */
    public boolean hasManagerAccess(Role userRole) {
        return userRole == Role.MANAGER;
    }
}

