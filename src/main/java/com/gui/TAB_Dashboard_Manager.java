package com.gui;

import com.bus.BUS_Invoice;
import com.bus.BUS_Product;
import com.dao.DAO_Shift;
import com.entities.*;
import com.enums.InvoiceType;
import com.enums.ProductCategory;
import com.interfaces.ShiftChangeListener;
import com.utils.AppColors;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XChartPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard cho Quản lý - công cụ ra quyết định chiến lược.
 * Không hiển thị dữ liệu thô; tập trung KPI, xu hướng, rủi ro và hành động.
 *
 * @author Tô Thanh Hậu
 */
public class TAB_Dashboard_Manager extends JPanel {

    private final Staff currentStaff;
    private ShiftChangeListener shiftChangeListener;

    // Chỉ dùng BUS/DAO của entities
    private final BUS_Invoice busInvoice;
    private final BUS_Product busProduct;
    private final DAO_Shift daoShift;

    // Header controls
    private JLabel lblTitle;
    private JLabel lblDate;
    private JComboBox<String> cbSoSanh;
    private JComboBox<String> cbBieuDo;
    private JButton btnLamMoi;

    // KPI cards
    private KpiCard cardDoanhThuThuan;
    private KpiCard cardLoiNhuan;
    private KpiCard cardTraHang;
    private KpiCard cardChenhLechTienMat;

    // Chart
    private JPanel chartWrap;

    // Tables
    private JTable tblDotBien;
    private DefaultTableModel mdlDotBien;

    private JTable tblDoiSoat;
    private DefaultTableModel mdlDoiSoat;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TAB_Dashboard_Manager() {
        this(null);
    }

    public TAB_Dashboard_Manager(Staff staff) {
        this.currentStaff = staff;
        this.busInvoice = new BUS_Invoice();
        this.busProduct = new BUS_Product();
        this.daoShift = new DAO_Shift();
        initComponents();
        refresh();
    }

    public void setShiftChangeListener(ShiftChangeListener listener) {
        this.shiftChangeListener = listener;
    }

    public void refresh() {
        // tránh block EDT lâu: do dữ liệu có thể truy vấn DB
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private ManagerKpis kpis;
            private List<TimePoint> series;
            private List<TrendingProductRow> dotBien;
            private List<ShiftReconciliationRow> doiSoat;

            @Override
            protected Void doInBackground() {
                ComparisonPeriod cp = getComparisonPeriodLocal();
                TimeGranularity tg = getTimeGranularityLocal();

                kpis = buildKpisHomNay(cp);
                series = buildDoanhThuLoiNhuanSeries(tg);
                dotBien = buildSanPhamDotBienTop(5);
                doiSoat = buildDoiSoatCaHomNay();
                return null;
            }

            @Override
            protected void done() {
                applyKpis(kpis);
                renderChart(series);
                loadDotBien(dotBien);
                loadDoiSoat(doiSoat);
            }
        };
        worker.execute();
    }

    // ================= UI =================

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(AppColors.WHITE);

        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setOpaque(false);

        content.add(createKpiPanel(), BorderLayout.NORTH);
        content.add(createCenterPanel(), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Left: title + date
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        lblTitle = new JLabel("Dashboard Quản lý - Điều hành chiến lược");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(AppColors.PRIMARY);

        lblDate = new JLabel("Ngày: " + LocalDate.now().format(dateFmt));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(AppColors.DARK);

        left.add(lblTitle);
        left.add(Box.createVerticalStrut(4));
        left.add(lblDate);

        // Right: filters
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        cbSoSanh = new JComboBox<>(new String[]{"So với hôm qua", "So với 7 ngày trước"});
        cbSoSanh.setPreferredSize(new Dimension(170, 32));

        cbBieuDo = new JComboBox<>(new String[]{"Biểu đồ 7 ngày gần nhất", "Biểu đồ theo giờ hôm nay"});
        cbBieuDo.setPreferredSize(new Dimension(190, 32));

        btnLamMoi = new JButton("Làm mới");
        btnLamMoi.setPreferredSize(new Dimension(110, 32));
        btnLamMoi.setBackground(AppColors.PRIMARY);
        btnLamMoi.setForeground(Color.WHITE);
        btnLamMoi.setFocusPainted(false);
        btnLamMoi.setBorderPainted(false);
        btnLamMoi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLamMoi.addActionListener(e -> refresh());

        cbSoSanh.addActionListener(e -> refresh());
        cbBieuDo.addActionListener(e -> refresh());

        right.add(cbSoSanh);
        right.add(cbBieuDo);
        right.add(btnLamMoi);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createKpiPanel() {
        JPanel wrap = new JPanel(new GridLayout(1, 4, 12, 0));
        wrap.setOpaque(false);

        cardDoanhThuThuan = new KpiCard("Doanh thu thuần", "Doanh thu bán − Doanh thu trả", KpiTone.TICH_CUC);
        cardLoiNhuan = new KpiCard("Lợi nhuận / Lỗ", "Doanh thu hóa đơn − Giá vốn", KpiTone.TU_DONG);
        cardTraHang = new KpiCard("Tổng đơn trả hàng", "Chỉ báo vấn đề kinh doanh", KpiTone.CANH_BAO);
        cardChenhLechTienMat = new KpiCard("Chênh lệch tiền mặt", "Thực tế − Hệ thống", KpiTone.NGUY_CO);

        wrap.add(cardDoanhThuThuan);
        wrap.add(cardLoiNhuan);
        wrap.add(cardTraHang);
        wrap.add(cardChenhLechTienMat);

        return wrap;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setOpaque(false);

        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setOpaque(false);
        left.add(createChartPanel(), BorderLayout.NORTH);
        left.add(createDotBienPanel(), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setOpaque(false);
        right.add(createActionHintPanel(), BorderLayout.NORTH);
        right.add(createDoiSoatPanel(), BorderLayout.CENTER);

        center.add(left);
        center.add(right);
        return center;
    }

    private JPanel createChartPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Doanh thu & Lợi nhuận theo thời gian"
        ));

        chartWrap = new JPanel(new BorderLayout());
        chartWrap.setBackground(Color.WHITE);
        chartWrap.setPreferredSize(new Dimension(0, 300));

        JLabel note = new JLabel("Mục tiêu: nhận diện giờ/ngày cao điểm để điều chỉnh phân ca hoặc khuyến mãi.");
        note.setBorder(new EmptyBorder(6, 8, 6, 8));
        note.setForeground(AppColors.DARK);
        note.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        p.add(chartWrap, BorderLayout.CENTER);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createDotBienPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Sản phẩm có doanh số tăng đột biến"
        ));

        JLabel note = new JLabel("Hành động: cân nhắc nhập hàng gấp và theo dõi xu hướng nhu cầu bất thường.");
        note.setBorder(new EmptyBorder(6, 8, 6, 8));
        note.setForeground(AppColors.DARK);
        note.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        mdlDotBien = new DefaultTableModel(new Object[]{"Mã", "Tên sản phẩm", "Nhóm", "Doanh thu hôm nay", "TB 7 ngày", "% tăng"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblDotBien = new JTable(mdlDotBien);
        styleTable(tblDotBien);
        tblDotBien.getColumnModel().getColumn(5).setCellRenderer(new PercentCellRenderer());

        p.add(new JScrollPane(tblDotBien), BorderLayout.CENTER);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createActionHintPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Nhà thuốc đang làm ăn ra sao?"
        ));

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBackground(Color.WHITE);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ta.setForeground(AppColors.TEXT);
        ta.setText(
            "• Xem Doanh thu thuần và Lợi nhuận/Lỗ để đánh giá hiệu quả kinh doanh.\n" +
            "• Nếu Tổng đơn trả hàng tăng: rà soát chất lượng tư vấn, sản phẩm và quy trình đổi/trả.\n" +
            "• Nếu Chênh lệch tiền mặt âm: ưu tiên kiểm tra ca có rủi ro thất thoát.\n" +
            "• Dựa vào biểu đồ theo giờ/ngày để quyết định phân ca hoặc chạy khuyến mãi đúng thời điểm."
        );
        ta.setBorder(new EmptyBorder(8, 10, 8, 10));

        p.add(ta, BorderLayout.CENTER);
        return p;
    }

    private JPanel createDoiSoatPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Kiểm soát tiền mặt & rủi ro thất thoát (đối soát ca)"
        ));

        JLabel note = new JLabel("Màu đỏ: tiền thực tế thấp hơn hệ thống (nguy cơ thất thoát)." );
        note.setBorder(new EmptyBorder(6, 8, 6, 8));
        note.setForeground(AppColors.DARK);
        note.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        mdlDoiSoat = new DefaultTableModel(new Object[]{"Mã ca", "Nhân viên", "Mở ca", "Tiền đầu ca", "Tiền cuối ca", "Doanh thu hệ thống", "Tiền thực tế", "Chênh lệch"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblDoiSoat = new JTable(mdlDoiSoat);
        styleTable(tblDoiSoat);
        tblDoiSoat.getColumnModel().getColumn(7).setCellRenderer(new CashMismatchRenderer());

        p.add(new JScrollPane(tblDoiSoat), BorderLayout.CENTER);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    // ================= Data binding =================

    // Đổi các hàm binding để dùng enum nội bộ
    private ComparisonPeriod getComparisonPeriodLocal() {
        int idx = cbSoSanh != null ? cbSoSanh.getSelectedIndex() : 0;
        return idx == 0 ? ComparisonPeriod.HOM_QUA : ComparisonPeriod.TUAN_TRUOC_CUNG_KY;
    }

    private TimeGranularity getTimeGranularityLocal() {
        int idx = cbBieuDo != null ? cbBieuDo.getSelectedIndex() : 0;
        return idx == 0 ? TimeGranularity.THEO_NGAY_7_NGAY : TimeGranularity.THEO_GIO_HOM_NAY;
    }

    // ================== Tổng hợp số liệu (không tạo BUS/DAO mới) ==================

    private static class PeriodAgg {
        BigDecimal doanhThuBan = BigDecimal.ZERO;
        BigDecimal doanhThuTra = BigDecimal.ZERO;
        BigDecimal doanhThuThuan = BigDecimal.ZERO;
        BigDecimal loiNhuan = BigDecimal.ZERO;
        int soDonTraHang = 0;
    }

    private ManagerKpis buildKpisHomNay(ComparisonPeriod compare) {
        LocalDate today = LocalDate.now();
        LocalDate prev = (compare == ComparisonPeriod.HOM_QUA) ? today.minusDays(1) : today.minusDays(7);

        PeriodAgg cur = aggregateInvoicesForDay(today);
        PeriodAgg pre = aggregateInvoicesForDay(prev);

        BigDecimal mismatchCur = calculateCashMismatchForDay(today);
        BigDecimal mismatchPre = calculateCashMismatchForDay(prev);

        return new ManagerKpis(
            new KpiTrend(cur.doanhThuThuan, pre.doanhThuThuan),
            new KpiTrend(cur.loiNhuan, pre.loiNhuan),
            new KpiTrend(BigDecimal.valueOf(cur.soDonTraHang), BigDecimal.valueOf(pre.soDonTraHang)),
            new KpiTrend(mismatchCur, mismatchPre)
        );
    }

    private List<TimePoint> buildDoanhThuLoiNhuanSeries(TimeGranularity g) {
        return (g == TimeGranularity.THEO_NGAY_7_NGAY) ? buildSeries7Ngay() : buildSeriesTheoGioHomNay();
    }

    private PeriodAgg aggregateInvoicesForDay(LocalDate day) {
        PeriodAgg agg = new PeriodAgg();
        if (day == null) return agg;

        List<Invoice> invoices = safeInvoices().stream()
            .filter(i -> i != null && i.getCreationDate() != null && i.getCreationDate().toLocalDate().equals(day))
            .toList();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalReturn = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int returnCount = 0;

        for (Invoice inv : invoices) {
            BigDecimal invTotal = nz(inv.calculateTotal());
            if (inv.getType() == InvoiceType.RETURN) {
                totalReturn = totalReturn.add(invTotal);
                returnCount++;
            } else {
                totalRevenue = totalRevenue.add(invTotal);
                totalCost = totalCost.add(calculateInvoiceCost(inv));
            }
        }

        agg.doanhThuBan = totalRevenue;
        agg.doanhThuTra = totalReturn;
        agg.doanhThuThuan = totalRevenue.subtract(totalReturn);
        agg.loiNhuan = totalRevenue.subtract(totalCost);
        agg.soDonTraHang = returnCount;
        return agg;
    }

    private List<TimePoint> buildSeries7Ngay() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        java.util.ArrayList<TimePoint> out = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            PeriodAgg a = aggregateInvoicesForDay(d);
            out.add(new TimePoint(d.format(f), a.doanhThuThuan, a.loiNhuan));
        }
        return out;
    }

    private List<TimePoint> buildSeriesTheoGioHomNay() {
        LocalDate today = LocalDate.now();
        List<Invoice> invoices = safeInvoices().stream()
            .filter(i -> i != null && i.getCreationDate() != null && i.getCreationDate().toLocalDate().equals(today))
            .toList();

        java.util.Map<Integer, java.util.List<Invoice>> byHour = invoices.stream()
            .collect(java.util.stream.Collectors.groupingBy(i -> i.getCreationDate().getHour()));

        java.util.ArrayList<TimePoint> out = new java.util.ArrayList<>();
        for (int h = 0; h < 24; h++) {
            List<Invoice> bucket = byHour.getOrDefault(h, java.util.List.of());

            BigDecimal rev = BigDecimal.ZERO;
            BigDecimal ret = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;

            for (Invoice inv : bucket) {
                BigDecimal invTotal = nz(inv.calculateTotal());
                if (inv.getType() == InvoiceType.RETURN) {
                    ret = ret.add(invTotal);
                } else {
                    rev = rev.add(invTotal);
                    cost = cost.add(calculateInvoiceCost(inv));
                }
            }

            out.add(new TimePoint(String.format("%02d:00", h), rev.subtract(ret), rev.subtract(cost)));
        }
        return out;
    }

    private BigDecimal calculateInvoiceCost(Invoice inv) {
        if (inv == null || inv.getInvoiceLineList() == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine line : inv.getInvoiceLineList()) {
            if (line == null) continue;
            total = total.add(calculateLineCost(line));
        }
        return total;
    }

    private BigDecimal calculateLineCost(InvoiceLine line) {
        BigDecimal totalCost = BigDecimal.ZERO;
        if (line.getLotAllocations() != null) {
            for (LotAllocation allocation : line.getLotAllocations()) {
                if (allocation == null || allocation.getLot() == null) continue;
                BigDecimal raw = allocation.getLot().getRawPrice();
                if (raw == null) continue;
                totalCost = totalCost.add(raw.multiply(BigDecimal.valueOf(allocation.getQuantity())));
            }
        }
        return totalCost;
    }

    private BigDecimal calculateCashMismatchForDay(LocalDate day) {
        if (day == null) return BigDecimal.ZERO;
        List<Shift> shifts = daoShift.listShiftsOpenedOn(day);
        if (shifts == null || shifts.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = BigDecimal.ZERO;
        for (Shift s : shifts) {
            BigDecimal startCash = nz(s.getStartCash());
            BigDecimal systemCash = s.getSystemCash();
            BigDecimal endCash = s.getEndCash();

            if (systemCash == null && s.getId() != null) {
                try {
                    systemCash = daoShift.calculateSystemCashForShift(s.getId());
                } catch (Exception ignored) {
                    systemCash = BigDecimal.ZERO;
                }
            }

            BigDecimal expected = nz(systemCash).subtract(startCash); // doanh thu hệ thống
            BigDecimal actual = nz(endCash);
            sum = sum.add(actual.subtract(expected));
        }
        return sum;
    }

    private List<TrendingProductRow> buildSanPhamDotBienTop(int topN) {
        LocalDate today = LocalDate.now();
        LocalDate from7 = today.minusDays(7);

        List<Invoice> invoices = safeInvoices();

        List<Invoice> todayInvoices = invoices.stream()
            .filter(i -> i != null && i.getCreationDate() != null && i.getCreationDate().toLocalDate().equals(today))
            .toList();

        List<Invoice> sevenDayInvoices = invoices.stream()
            .filter(i -> i != null && i.getCreationDate() != null)
            .filter(i -> {
                LocalDate d = i.getCreationDate().toLocalDate();
                return (!d.isAfter(today.minusDays(1))) && (!d.isBefore(from7));
            })
            .toList();

        java.util.Map<String, BigDecimal> todayRevByProduct = revenueByProduct(todayInvoices);
        java.util.Map<String, java.util.List<BigDecimal>> dailyRevs = revenueByProductByDay(sevenDayInvoices);

        java.util.ArrayList<TrendingProductRow> rows = new java.util.ArrayList<>();
        for (var e : todayRevByProduct.entrySet()) {
            String productId = e.getKey();
            BigDecimal revToday = e.getValue();

            BigDecimal avg7 = average(dailyRevs.getOrDefault(productId, java.util.List.of()));
            if (avg7.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (revToday.compareTo(avg7.multiply(BigDecimal.valueOf(2))) <= 0) continue;

            BigDecimal pct = revToday.subtract(avg7)
                .divide(avg7, 6, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            Product p = busProduct.getProductById(productId);
            String ten = p != null ? p.getName() : productId;
            String nhom = p != null ? mapNhomSanPham(p.getCategory()) : "";

            rows.add(new TrendingProductRow(productId, ten, nhom, revToday, avg7, pct));
        }

        rows.sort(java.util.Comparator.comparing(TrendingProductRow::getPhanTramTang).reversed());
        if (rows.size() > topN) return rows.subList(0, topN);
        return rows;
    }

    private java.util.Map<String, BigDecimal> revenueByProduct(List<Invoice> invoices) {
        java.util.Map<String, BigDecimal> map = new java.util.HashMap<>();
        for (Invoice inv : invoices) {
            if (inv == null || inv.getInvoiceLineList() == null) continue;
            if (inv.getType() == InvoiceType.RETURN) continue;
            for (InvoiceLine line : inv.getInvoiceLineList()) {
                if (line == null || line.getProduct() == null) continue;
                String id = line.getProduct().getId();
                map.put(id, map.getOrDefault(id, BigDecimal.ZERO).add(nz(line.calculateTotalAmount())));
            }
        }
        return map;
    }

    private java.util.Map<String, java.util.List<BigDecimal>> revenueByProductByDay(List<Invoice> invoices) {
        java.util.Map<java.time.LocalDate, java.util.List<Invoice>> byDay = invoices.stream()
            .filter(i -> i != null && i.getCreationDate() != null)
            .collect(java.util.stream.Collectors.groupingBy(i -> i.getCreationDate().toLocalDate()));

        java.util.Map<String, java.util.List<BigDecimal>> out = new java.util.HashMap<>();
        for (var e : byDay.entrySet()) {
            java.util.Map<String, BigDecimal> rev = revenueByProduct(e.getValue());
            for (var r : rev.entrySet()) {
                out.computeIfAbsent(r.getKey(), k -> new java.util.ArrayList<>()).add(r.getValue());
            }
        }
        return out;
    }

    private List<ShiftReconciliationRow> buildDoiSoatCaHomNay() {
        LocalDate today = LocalDate.now();
        List<Shift> shifts = daoShift.listShiftsOpenedOn(today);
        if (shifts == null || shifts.isEmpty()) return java.util.List.of();

        java.time.format.DateTimeFormatter dtfLocal = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        java.util.ArrayList<ShiftReconciliationRow> out = new java.util.ArrayList<>();
        for (Shift s : shifts) {
            BigDecimal start = s.getStartCash();
            BigDecimal end = s.getEndCash();
            BigDecimal systemCash = s.getSystemCash();

            if (systemCash == null && s.getId() != null) {
                try {
                    systemCash = daoShift.calculateSystemCashForShift(s.getId());
                } catch (Exception ignored) {
                    systemCash = BigDecimal.ZERO;
                }
            }

            BigDecimal doanhThuHeThong = nz(systemCash).subtract(nz(start));
            BigDecimal tienThucTe = nz(end);
            BigDecimal chenh = tienThucTe.subtract(doanhThuHeThong);

            String tenNv = (s.getStaff() != null) ? s.getStaff().getFullName() : "";
            String tg = (s.getStartTime() != null) ? s.getStartTime().format(dtfLocal) : "";

            out.add(new ShiftReconciliationRow(s.getId(), tenNv, tg, start, end, doanhThuHeThong, tienThucTe, chenh));
        }

        return out;
    }

    private List<Invoice> safeInvoices() {
        try {
            List<Invoice> inv = busInvoice.getAllInvoices();
            return inv != null ? inv : java.util.List.of();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (BigDecimal v : values) {
            if (v == null) continue;
            sum = sum.add(v);
            n++;
        }
        if (n == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private static String mapNhomSanPham(ProductCategory c) {
        if (c == null) return "Thuốc không kê đơn";
        return switch (c) {
            case ETC -> "Thuốc kê đơn";
            case OTC -> "Thuốc không kê đơn";
            case SUPPLEMENT -> "Sản phẩm chức năng";
        };
    }

    // ================== Components ==================

    private enum KpiTone {
        TICH_CUC, // xanh
        NGUY_CO,  // đỏ
        CANH_BAO, // vàng
        TU_DONG   // tùy theo giá trị
    }

    private static class KpiCard extends JPanel {
        private final JLabel lblName;
        private final JLabel lblDesc;
        private final JLabel lblValue;
        private final JLabel lblTrend;

        private KpiTone tone;

        public KpiCard(String name, String desc, KpiTone tone) {
            setLayout(new BorderLayout(6, 4));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1),
                new EmptyBorder(10, 12, 10, 12)
            ));

            this.tone = tone;

            lblName = new JLabel(name);
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));

            lblDesc = new JLabel(desc);
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDesc.setForeground(new Color(90, 90, 90));

            lblValue = new JLabel("--");
            lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));

            lblTrend = new JLabel("So sánh: --");
            lblTrend.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(lblName);
            top.add(lblDesc);

            add(top, BorderLayout.NORTH);
            add(lblValue, BorderLayout.CENTER);
            add(lblTrend, BorderLayout.SOUTH);

            applyToneColor(Color.BLACK);
        }

        public void setValue(String value) {
            lblValue.setText(value != null ? value : "--");
        }

        public void setTrend(BigDecimal percent, BigDecimal delta) {
            BigDecimal pct = percent != null ? percent : BigDecimal.ZERO;
            BigDecimal d = delta != null ? delta : BigDecimal.ZERO;

            String sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            String t = String.format("So sánh: %s%.1f%% (Δ %s)", sign, pct.doubleValue(), formatSignedMoney(d));
            lblTrend.setText(t);

            // Màu trend theo hướng (xanh tăng, đỏ giảm) - ngoại trừ KPI rủi ro sẽ setTone bên ngoài.
            if (pct.compareTo(BigDecimal.ZERO) >= 0) {
                lblTrend.setForeground(AppColors.SUCCESS);
            } else {
                lblTrend.setForeground(AppColors.DANGER);
            }
        }

        public void setTone(KpiTone tone) {
            this.tone = tone;
            Color c = switch (tone) {
                case TICH_CUC -> AppColors.SUCCESS;
                case NGUY_CO -> AppColors.DANGER;
                case CANH_BAO -> AppColors.WARNING;
                default -> AppColors.TEXT;
            };
            applyToneColor(c);
        }

        private void applyToneColor(Color c) {
            lblName.setForeground(c);
            lblValue.setForeground(c);
        }

        private String formatSignedMoney(BigDecimal value) {
            if (value == null) value = BigDecimal.ZERO;
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
            String s = nf.format(value.abs());
            if (value.compareTo(BigDecimal.ZERO) > 0) return "+" + s;
            if (value.compareTo(BigDecimal.ZERO) < 0) return "-" + s;
            return s;
        }
    }

    private class PercentCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof BigDecimal bd) {
                setText(String.format("%+.0f%%", bd.doubleValue()));
                setForeground(bd.compareTo(BigDecimal.ZERO) >= 0 ? AppColors.SUCCESS : AppColors.DANGER);
            } else {
                setText(value != null ? value.toString() : "");
                setForeground(AppColors.TEXT);
            }
            return this;
        }
    }

    private class CashMismatchRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);

            if (value instanceof BigDecimal bd) {
                setText(currency.format(bd));
                if (bd.compareTo(BigDecimal.ZERO) < 0) {
                    setForeground(AppColors.DANGER);
                } else if (bd.compareTo(BigDecimal.ZERO) > 0) {
                    setForeground(AppColors.WARNING);
                } else {
                    setForeground(AppColors.SUCCESS);
                }
            } else {
                setText(value != null ? value.toString() : "");
                setForeground(AppColors.TEXT);
            }
            return this;
        }
    }

    // ====== DTO/enum nội bộ cho dashboard (không tạo BUS/DAO mới) ======
    private enum ComparisonPeriod {
        HOM_QUA,
        TUAN_TRUOC_CUNG_KY
    }

    private enum TimeGranularity {
        THEO_NGAY_7_NGAY,
        THEO_GIO_HOM_NAY
    }

    private static class KpiTrend {
        private final BigDecimal current;
        private final BigDecimal previous;

        public KpiTrend(BigDecimal current, BigDecimal previous) {
            this.current = nz(current);
            this.previous = nz(previous);
        }

        public BigDecimal getCurrent() { return current; }
        public BigDecimal getDelta() { return current.subtract(previous); }

        public BigDecimal getPercentChange() {
            if (previous.compareTo(BigDecimal.ZERO) == 0) {
                if (current.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
                return BigDecimal.valueOf(100);
            }
            return current.subtract(previous)
                .divide(previous.abs(), 6, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }

    private static class ManagerKpis {
        private final KpiTrend doanhThuThuan;
        private final KpiTrend loiNhuan;
        private final KpiTrend tongDonTraHang;
        private final KpiTrend chenhLechTienMat;

        public ManagerKpis(KpiTrend doanhThuThuan, KpiTrend loiNhuan, KpiTrend tongDonTraHang, KpiTrend chenhLechTienMat) {
            this.doanhThuThuan = doanhThuThuan;
            this.loiNhuan = loiNhuan;
            this.tongDonTraHang = tongDonTraHang;
            this.chenhLechTienMat = chenhLechTienMat;
        }

        public KpiTrend getDoanhThuThuan() { return doanhThuThuan; }
        public KpiTrend getLoiNhuan() { return loiNhuan; }
        public KpiTrend getTongDonTraHang() { return tongDonTraHang; }
        public KpiTrend getChenhLechTienMat() { return chenhLechTienMat; }
    }

    private static class TimePoint {
        private final String nhan;
        private final BigDecimal doanhThuThuan;
        private final BigDecimal loiNhuan;

        public TimePoint(String nhan, BigDecimal doanhThuThuan, BigDecimal loiNhuan) {
            this.nhan = nhan;
            this.doanhThuThuan = nz(doanhThuThuan);
            this.loiNhuan = nz(loiNhuan);
        }

        public String getNhan() { return nhan; }
        public BigDecimal getDoanhThuThuan() { return doanhThuThuan; }
        public BigDecimal getLoiNhuan() { return loiNhuan; }
    }

    private static class TrendingProductRow {
        private final String ma;
        private final String ten;
        private final String nhom;
        private final BigDecimal doanhThuHomNay;
        private final BigDecimal doanhThuTrungBinh7Ngay;
        private final BigDecimal phanTramTang;

        public TrendingProductRow(String ma, String ten, String nhom, BigDecimal doanhThuHomNay, BigDecimal doanhThuTrungBinh7Ngay, BigDecimal phanTramTang) {
            this.ma = ma;
            this.ten = ten;
            this.nhom = nhom;
            this.doanhThuHomNay = nz(doanhThuHomNay);
            this.doanhThuTrungBinh7Ngay = nz(doanhThuTrungBinh7Ngay);
            this.phanTramTang = nz(phanTramTang);
        }

        public String getMa() { return ma; }
        public String getTen() { return ten; }
        public String getNhom() { return nhom; }
        public BigDecimal getDoanhThuHomNay() { return doanhThuHomNay; }
        public BigDecimal getDoanhThuTrungBinh7Ngay() { return doanhThuTrungBinh7Ngay; }
        public BigDecimal getPhanTramTang() { return phanTramTang; }
    }

    private static class ShiftReconciliationRow {
        private final String maCa;
        private final String nhanVien;
        private final String thoiGianMo;
        private final BigDecimal tienDauCa;
        private final BigDecimal tienCuoiCa;
        private final BigDecimal doanhThuHeThong;
        private final BigDecimal tienThucTe;
        private final BigDecimal chenhlech;

        public ShiftReconciliationRow(String maCa, String nhanVien, String thoiGianMo, BigDecimal tienDauCa, BigDecimal tienCuoiCa,
                                     BigDecimal doanhThuHeThong, BigDecimal tienThucTe, BigDecimal chenhlech) {
            this.maCa = maCa;
            this.nhanVien = nhanVien;
            this.thoiGianMo = thoiGianMo;
            this.tienDauCa = nz(tienDauCa);
            this.tienCuoiCa = nz(tienCuoiCa);
            this.doanhThuHeThong = nz(doanhThuHeThong);
            this.tienThucTe = nz(tienThucTe);
            this.chenhlech = nz(chenhlech);
        }

        public String getMaCa() { return maCa; }
        public String getNhanVien() { return nhanVien; }
        public String getThoiGianMo() { return thoiGianMo; }
        public BigDecimal getTienDauCa() { return tienDauCa; }
        public BigDecimal getTienCuoiCa() { return tienCuoiCa; }
        public BigDecimal getDoanhThuHeThong() { return doanhThuHeThong; }
        public BigDecimal getTienThucTe() { return tienThucTe; }
        public BigDecimal getChenhlech() { return chenhlech; }
    }

    // Cập nhật applyKpis / renderChart / loadDotBien / loadDoiSoat dùng DTO nội bộ
    private void applyKpis(ManagerKpis k) {
        if (k == null) return;

        cardDoanhThuThuan.setValue(currency.format(k.getDoanhThuThuan().getCurrent()));
        cardDoanhThuThuan.setTrend(k.getDoanhThuThuan().getPercentChange(), k.getDoanhThuThuan().getDelta());
        cardDoanhThuThuan.setTone(KpiTone.TICH_CUC);

        // Lợi nhuận: xanh nếu dương, đỏ nếu âm
        BigDecimal profit = k.getLoiNhuan().getCurrent();
        cardLoiNhuan.setValue(currency.format(profit));
        cardLoiNhuan.setTrend(k.getLoiNhuan().getPercentChange(), k.getLoiNhuan().getDelta());
        cardLoiNhuan.setTone(profit.compareTo(BigDecimal.ZERO) >= 0 ? KpiTone.TICH_CUC : KpiTone.NGUY_CO);

        cardTraHang.setValue(formatInt(k.getTongDonTraHang().getCurrent()));
        cardTraHang.setTrend(k.getTongDonTraHang().getPercentChange(), k.getTongDonTraHang().getDelta());
        cardTraHang.setTone(KpiTone.CANH_BAO);

        BigDecimal mismatch = k.getChenhLechTienMat().getCurrent();
        cardChenhLechTienMat.setValue(currency.format(mismatch));
        cardChenhLechTienMat.setTrend(k.getChenhLechTienMat().getPercentChange(), k.getChenhLechTienMat().getDelta());
        cardChenhLechTienMat.setTone(mismatch.compareTo(BigDecimal.ZERO) == 0 ? KpiTone.TICH_CUC : KpiTone.NGUY_CO);
    }

    private void renderChart(List<TimePoint> points) {
        chartWrap.removeAll();

        if (points == null || points.isEmpty()) {
            JLabel empty = new JLabel("Chưa có dữ liệu để vẽ biểu đồ.");
            empty.setBorder(new EmptyBorder(12, 12, 12, 12));
            chartWrap.add(empty, BorderLayout.CENTER);
            chartWrap.revalidate();
            chartWrap.repaint();
            return;
        }

        List<String> x = points.stream().map(TimePoint::getNhan).toList();
        List<Double> doanhThu = points.stream().map(p -> p.getDoanhThuThuan().doubleValue()).toList();
        List<Double> loiNhuan = points.stream().map(p -> p.getLoiNhuan().doubleValue()).toList();

        CategoryChart chart = new CategoryChartBuilder()
            .width(600)
            .height(280)
            .title("Doanh thu thuần và Lợi nhuận")
            .xAxisTitle("Thời gian")
            .yAxisTitle("Giá trị (₫)")
            .build();

        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setAxisTickLabelsFont(new Font("Segoe UI", Font.PLAIN, 11));
        chart.getStyler().setLegendFont(new Font("Segoe UI", Font.PLAIN, 12));
        chart.getStyler().setChartTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        chart.getStyler().setAvailableSpaceFill(.8);
        chart.getStyler().setOverlapped(true);

        chart.addSeries("Doanh thu thuần", x, doanhThu).setFillColor(AppColors.PRIMARY);
        chart.addSeries("Lợi nhuận", x, loiNhuan).setFillColor(AppColors.SUCCESS);

        chartWrap.add(new XChartPanel<>(chart), BorderLayout.CENTER);
        chartWrap.revalidate();
        chartWrap.repaint();
    }

    private void loadDotBien(List<TrendingProductRow> rows) {
        mdlDotBien.setRowCount(0);
        if (rows == null || rows.isEmpty()) return;
        for (TrendingProductRow r : rows) {
            mdlDotBien.addRow(new Object[]{
                r.getMa(),
                r.getTen(),
                r.getNhom(),
                currency.format(r.getDoanhThuHomNay()),
                currency.format(r.getDoanhThuTrungBinh7Ngay()),
                r.getPhanTramTang()
            });
        }
    }

    private void loadDoiSoat(List<ShiftReconciliationRow> rows) {
        mdlDoiSoat.setRowCount(0);
        if (rows == null || rows.isEmpty()) return;
        for (ShiftReconciliationRow r : rows) {
            mdlDoiSoat.addRow(new Object[]{
                r.getMaCa(),
                r.getNhanVien(),
                r.getThoiGianMo(),
                currency.format(r.getTienDauCa()),
                currency.format(r.getTienCuoiCa()),
                currency.format(r.getDoanhThuHeThong()),
                currency.format(r.getTienThucTe()),
                r.getChenhlech()
            });
        }
    }

    // ================= UI helpers =================

    private void styleTable(JTable tbl) {
        tbl.setRowHeight(28);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tbl.setGridColor(new Color(220, 220, 220));
        tbl.setShowGrid(true);
        tbl.setFillsViewportHeight(true);

        JTableHeader header = tbl.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(AppColors.LIGHT);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tbl.setDefaultRenderer(Object.class, left);
    }

    private String formatInt(BigDecimal bd) {
        if (bd == null) return "0";
        return String.valueOf(bd.setScale(0, RoundingMode.HALF_UP).intValue());
    }
}
