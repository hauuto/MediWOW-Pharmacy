package com.gui;

import com.bus.BUS_Invoice;
import com.bus.BUS_Product;
import com.bus.BUS_Shift;
import com.dao.DAO_Shift;
import com.entities.*;
import com.enums.InvoiceType;
import com.enums.ProductCategory;
import com.interfaces.DataChangeListener;
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
public class TAB_Dashboard_Manager extends JPanel implements DataChangeListener {

    private final Staff currentStaff;
    private ShiftChangeListener shiftChangeListener;

    // Chỉ dùng BUS/DAO của entities
    private final BUS_Invoice busInvoice;
    private final BUS_Product busProduct;
    private final BUS_Shift busShift;
    private final DAO_Shift daoShift;

    // Current shift
    private Shift currentShift;

    // Header controls
    private JLabel lblTitle;
    private JLabel lblDate;
    private JComboBox<String> cbComparison;
    private JComboBox<String> cbChart;

    // Shift management labels
    private JLabel lblShiftId;
    private JLabel lblCurrentCash;
    private JButton btnShift;

    // KPI cards
    private KpiCard cardNetRevenue;
    private KpiCard cardProfit;
    private KpiCard cardReturns;
    private KpiCard cardCashMismatch;

    // Chart
    private JPanel chartWrap;

    // Tables
    private JTable tblTrending;
    private DefaultTableModel mdlTrending;

    private JTable tblReconciliation;
    private DefaultTableModel mdlReconciliation;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TAB_Dashboard_Manager() {
        this(null);
    }

    public TAB_Dashboard_Manager(Staff staff) {
        this.currentStaff = staff;
        this.busInvoice = new BUS_Invoice();
        this.busProduct = new BUS_Product();
        this.busShift = new BUS_Shift();
        this.daoShift = new DAO_Shift();
        initComponents();
        loadShiftData();
        refresh();
    }

    public void setShiftChangeListener(ShiftChangeListener listener) {
        this.shiftChangeListener = listener;
    }

    public void refresh() {
        // Cập nhật thông tin ca làm việc
        loadShiftData();

        // tránh block EDT lâu: do dữ liệu có thể truy vấn DB
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private ManagerKpis kpis;
            private List<TimePoint> series;
            private List<TrendingProductRow> trending;
            private List<ShiftReconciliationRow> reconciliation;

            @Override
            protected Void doInBackground() {
                ComparisonPeriod cp = getComparisonPeriodLocal();
                TimeGranularity tg = getTimeGranularityLocal();

                kpis = buildKpisToday(cp);
                series = buildRevenueProfitSeries(tg);
                trending = buildTrendingProductsTop(5);
                reconciliation = buildTodayShiftReconciliation();
                return null;
            }

            @Override
            protected void done() {
                applyKpis(kpis);
                renderChart(series);
                loadTrending(trending);
                loadReconciliation(reconciliation);
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

        // Right section: Filters + Shift Widget + Shift Button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        // Filters (before shift widget)
        cbComparison = new JComboBox<>(new String[]{"So với hôm qua", "So với 7 ngày trước"});
        cbComparison.setPreferredSize(new Dimension(170, 32));

        cbChart = new JComboBox<>(new String[]{"Biểu đồ 7 ngày gần nhất", "Biểu đồ theo giờ hôm nay"});
        cbChart.setPreferredSize(new Dimension(190, 32));

        cbComparison.addActionListener(e -> refresh());
        cbChart.addActionListener(e -> refresh());

        right.add(cbComparison);
        right.add(cbChart);

        // Separator
        right.add(Box.createHorizontalStrut(15));

        // Shift Info Widget
        JPanel shiftWidget = createShiftWidget();
        right.add(shiftWidget);

        // Shift Button (Open/Close)
        btnShift = new JButton("Mở ca");
        btnShift.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnShift.setBackground(AppColors.SUCCESS);
        btnShift.setForeground(Color.WHITE);
        btnShift.setFocusPainted(false);
        btnShift.setBorderPainted(false);
        btnShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnShift.setPreferredSize(new Dimension(100, 40));
        btnShift.addActionListener(e -> handleShiftButtonClick());
        right.add(btnShift);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createShiftWidget() {
        JPanel widget = new JPanel();
        widget.setLayout(new BoxLayout(widget, BoxLayout.Y_AXIS));
        widget.setBackground(AppColors.WHITE);
        widget.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppColors.SECONDARY, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));

        // Shift ID
        JPanel shiftIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        shiftIdPanel.setBackground(AppColors.WHITE);
        JLabel lblShiftIdLabel = new JLabel("Mã Ca:");
        lblShiftIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblShiftIdLabel.setForeground(AppColors.DARK);
        lblShiftId = new JLabel("---");
        lblShiftId.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblShiftId.setForeground(AppColors.PRIMARY);
        shiftIdPanel.add(lblShiftIdLabel);
        shiftIdPanel.add(lblShiftId);

        // Current Cash
        JPanel cashPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cashPanel.setBackground(AppColors.WHITE);
        JLabel lblCashLabel = new JLabel("Tiền mặt:");
        lblCashLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCashLabel.setForeground(AppColors.DARK);
        lblCurrentCash = new JLabel("0 ₫");
        lblCurrentCash.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCurrentCash.setForeground(AppColors.SUCCESS);
        cashPanel.add(lblCashLabel);
        cashPanel.add(lblCurrentCash);

        widget.add(shiftIdPanel);
        widget.add(cashPanel);

        return widget;
    }

    private void loadShiftData() {
        // Check if there's any open shift on this workstation
        String workstation = busShift.getCurrentWorkstation();
        Shift workstationShift = busShift.getOpenShiftOnWorkstation(workstation);

        // Then check staff's personal shift
        Shift staffShift = null;
        if (currentStaff != null) {
            staffShift = busShift.getCurrentOpenShiftForStaff(currentStaff);
        }

        // Use workstation shift if it exists, otherwise use staff shift
        currentShift = workstationShift != null ? workstationShift : staffShift;

        // Set common button properties
        btnShift.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnShift.setForeground(Color.WHITE);
        btnShift.setBorderPainted(false);
        btnShift.setFocusPainted(false);
        btnShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnShift.setPreferredSize(new Dimension(100, 40));

        if (currentShift != null) {
            // Shift is open - show shift info and "Đóng ca"
            lblShiftId.setText(currentShift.getId());
            BigDecimal currentCash = busShift.calculateSystemCashForShift(currentShift);
            lblCurrentCash.setText(currency.format(currentCash));

            // Check if this is the staff's own shift
            boolean isOwnShift = currentStaff != null &&
                currentShift.getStaff() != null &&
                currentShift.getStaff().getId().equals(currentStaff.getId());

            btnShift.setText("Đóng ca");
            btnShift.setToolTipText(isOwnShift ?
                "Nhấn để đóng ca làm việc" :
                "Đóng ca của: " + currentShift.getStaff().getFullName());
            btnShift.setBackground(AppColors.DANGER);
            btnShift.setEnabled(true);
        } else {
            // No open shift - show "Mở ca"
            lblShiftId.setText("Chưa mở ca");
            lblCurrentCash.setText("---");

            btnShift.setText("Mở ca");
            btnShift.setToolTipText("Nhấn để mở ca làm việc");
            btnShift.setBackground(AppColors.SUCCESS);
            btnShift.setEnabled(true);
        }
    }

    private void handleShiftButtonClick() {
        if (currentShift != null) {
            // Close shift
            DIALOG_CloseShift closeShiftDialog = new DIALOG_CloseShift(
                (Frame) SwingUtilities.getWindowAncestor(this),
                currentShift,
                currentStaff
            );
            closeShiftDialog.setVisible(true);

            // Update button and shift info if shift was closed
            if (closeShiftDialog.isConfirmed()) {
                Shift closedShift = currentShift;
                currentShift = null;
                loadShiftData();
                refresh();

                // Notify listener that shift was closed
                if (shiftChangeListener != null) {
                    shiftChangeListener.onShiftClosed(closedShift);
                }

                JOptionPane.showMessageDialog(this,
                    "Ca làm việc đã được đóng thành công!",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // Open shift - check for existing shift on workstation
            String workstation = busShift.getCurrentWorkstation();
            Shift existingShift = busShift.getOpenShiftOnWorkstation(workstation);

            if (existingShift != null && currentStaff != null &&
                !existingShift.getStaff().getId().equals(currentStaff.getId())) {
                // Another staff has an open shift - show takeover dialog
                handleExistingShift(existingShift);
            } else {
                // No existing shift or same staff - open new shift
                openNewShift();
            }
        }
    }

    private void handleExistingShift(Shift existingShift) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String message = String.format(
            "Hiện đang có ca do nhân viên %s mở từ %s.\n\n" +
            "Bạn có muốn tiếp tục ca này không?\n\n" +
            "Lưu ý: Nếu chọn 'Có', bạn sẽ không thể bán hàng cho đến khi đóng ca này.",
            existingShift.getStaff().getFullName(),
            existingShift.getStartTime().format(dtf)
        );

        int choice = JOptionPane.showConfirmDialog(
            this,
            message,
            "Ca làm việc đang mở",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            currentShift = existingShift;
            loadShiftData();
            refresh();

            JOptionPane.showMessageDialog(
                this,
                "Bạn đã chọn tiếp tục ca hiện tại.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void openNewShift() {
        DIALOG_OpenShift openShiftDialog = new DIALOG_OpenShift(
            (Frame) SwingUtilities.getWindowAncestor(this),
            currentStaff
        );
        openShiftDialog.setVisible(true);

        // Update button and shift info if shift was opened
        if (openShiftDialog.getOpenedShift() != null) {
            currentShift = openShiftDialog.getOpenedShift();
            loadShiftData();
            refresh();

            // Notify listener that shift was opened
            if (shiftChangeListener != null) {
                shiftChangeListener.onShiftOpened(currentShift);
            }

            JOptionPane.showMessageDialog(this,
                "Ca làm việc đã được mở thành công!",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel createKpiPanel() {
        JPanel wrap = new JPanel(new GridLayout(1, 4, 12, 0));
        wrap.setOpaque(false);

        cardNetRevenue = new KpiCard("Doanh thu thuần", "Doanh thu bán − Doanh thu trả", KpiTone.POSITIVE);
        cardProfit = new KpiCard("Lợi nhuận / Lỗ", "Doanh thu hóa đơn − Giá vốn", KpiTone.AUTO);
        cardReturns = new KpiCard("Tổng đơn trả hàng", "Chỉ báo vấn đề kinh doanh", KpiTone.WARNING);
        cardCashMismatch = new KpiCard("Chênh lệch tiền mặt", "Thực tế − Hệ thống", KpiTone.RISK);

        wrap.add(cardNetRevenue);
        wrap.add(cardProfit);
        wrap.add(cardReturns);
        wrap.add(cardCashMismatch);

        return wrap;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setOpaque(false);

        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setOpaque(false);
        left.add(createChartPanel(), BorderLayout.NORTH);
        left.add(createTrendingPanel(), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setOpaque(false);
        right.add(createActionHintPanel(), BorderLayout.NORTH);
        right.add(createReconciliationPanel(), BorderLayout.CENTER);

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

    private JPanel createTrendingPanel() {
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

        mdlTrending = new DefaultTableModel(new Object[]{"Mã", "Tên sản phẩm", "Nhóm", "Doanh thu hôm nay", "TB 7 ngày", "% tăng"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblTrending = new JTable(mdlTrending);
        styleTable(tblTrending);
        tblTrending.getColumnModel().getColumn(5).setCellRenderer(new PercentCellRenderer());

        p.add(new JScrollPane(tblTrending), BorderLayout.CENTER);
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

    private JPanel createReconciliationPanel() {
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

        mdlReconciliation = new DefaultTableModel(new Object[]{"Mã ca", "Nhân viên", "Mở ca", "Tiền đầu ca", "Tiền cuối ca", "Doanh thu hệ thống", "Tiền thực tế", "Chênh lệch"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblReconciliation = new JTable(mdlReconciliation);
        styleTable(tblReconciliation);
        tblReconciliation.getColumnModel().getColumn(7).setCellRenderer(new CashMismatchRenderer());

        p.add(new JScrollPane(tblReconciliation), BorderLayout.CENTER);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    // ================= Data binding =================

    // Đổi các hàm binding để dùng enum nội bộ
    private ComparisonPeriod getComparisonPeriodLocal() {
        int idx = cbComparison != null ? cbComparison.getSelectedIndex() : 0;
        return idx == 0 ? ComparisonPeriod.YESTERDAY : ComparisonPeriod.LAST_WEEK_SAME_DAY;
    }

    private TimeGranularity getTimeGranularityLocal() {
        int idx = cbChart != null ? cbChart.getSelectedIndex() : 0;
        return idx == 0 ? TimeGranularity.DAILY_LAST_7_DAYS : TimeGranularity.HOURLY_TODAY;
    }

    // ================== Tổng hợp số liệu (không tạo BUS/DAO mới) ==================

    private static class PeriodAgg {
        BigDecimal salesRevenue = BigDecimal.ZERO;
        BigDecimal returnRevenue = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        int returnOrderCount = 0;
    }

    private ManagerKpis buildKpisToday(ComparisonPeriod compare) {
        LocalDate today = LocalDate.now();
        LocalDate prev = (compare == ComparisonPeriod.YESTERDAY) ? today.minusDays(1) : today.minusDays(7);

        PeriodAgg cur = aggregateInvoicesForDay(today);
        PeriodAgg pre = aggregateInvoicesForDay(prev);

        BigDecimal mismatchCur = calculateCashMismatchForDay(today);
        BigDecimal mismatchPre = calculateCashMismatchForDay(prev);

        return new ManagerKpis(
            new KpiTrend(cur.netRevenue, pre.netRevenue),
            new KpiTrend(cur.profit, pre.profit),
            new KpiTrend(BigDecimal.valueOf(cur.returnOrderCount), BigDecimal.valueOf(pre.returnOrderCount)),
            new KpiTrend(mismatchCur, mismatchPre)
        );
    }

    private List<TimePoint> buildRevenueProfitSeries(TimeGranularity g) {
        return (g == TimeGranularity.DAILY_LAST_7_DAYS) ? buildSeriesLast7Days() : buildSeriesTodayByHour();
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

        agg.salesRevenue = totalRevenue;
        agg.returnRevenue = totalReturn;
        agg.netRevenue = totalRevenue.subtract(totalReturn);
        agg.profit = totalRevenue.subtract(totalCost);
        agg.returnOrderCount = returnCount;
        return agg;
    }

    private List<TimePoint> buildSeriesLast7Days() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        java.util.ArrayList<TimePoint> out = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            PeriodAgg a = aggregateInvoicesForDay(d);
            out.add(new TimePoint(d.format(f), a.netRevenue, a.profit));
        }
        return out;
    }

    private List<TimePoint> buildSeriesTodayByHour() {
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
            // Only calculate for closed shifts (endCash != null)
            BigDecimal endCash = s.getEndCash();
            if (endCash == null) continue; // Skip open shifts

            BigDecimal systemCash = s.getSystemCash();
            if (systemCash == null && s.getId() != null) {
                try {
                    systemCash = daoShift.calculateSystemCashForShift(s.getId());
                } catch (Exception ignored) {
                    systemCash = BigDecimal.ZERO;
                }
            }

            // mismatch = actual end cash - expected system cash
            // negative = shortage (potential loss), positive = surplus
            BigDecimal mismatch = endCash.subtract(nz(systemCash));
            sum = sum.add(mismatch);
        }
        return sum;
    }

    private List<TrendingProductRow> buildTrendingProductsTop(int topN) {
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
            String name = p != null ? p.getName() : productId;
            String category = p != null ? mapProductCategory(p.getCategory()) : "";

            rows.add(new TrendingProductRow(productId, name, category, revToday, avg7, pct));
        }

        rows.sort(java.util.Comparator.comparing(TrendingProductRow::getPercentIncrease).reversed());
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

    private List<ShiftReconciliationRow> buildTodayShiftReconciliation() {
        LocalDate today = LocalDate.now();
        List<Shift> shifts = daoShift.listShiftsOpenedOn(today);
        if (shifts == null || shifts.isEmpty()) return java.util.List.of();

        java.time.format.DateTimeFormatter dtfLocal = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        java.util.ArrayList<ShiftReconciliationRow> out = new java.util.ArrayList<>();
        for (Shift s : shifts) {
            BigDecimal startCash = nz(s.getStartCash());
            BigDecimal endCash = s.getEndCash();
            BigDecimal systemCash = s.getSystemCash();

            if (systemCash == null && s.getId() != null) {
                try {
                    systemCash = daoShift.calculateSystemCashForShift(s.getId());
                } catch (Exception ignored) {
                    systemCash = BigDecimal.ZERO;
                }
            }

            // systemCash = startCash + sales - returns + exchanges (total expected cash at end)
            // systemRevenue = systemCash - startCash (net revenue from transactions)
            BigDecimal systemCashTotal = nz(systemCash);
            BigDecimal systemRevenue = systemCashTotal.subtract(startCash);

            // For display: actualCash is endCash (or current system cash if shift still open)
            BigDecimal actualCash = endCash != null ? endCash : systemCashTotal;

            // mismatch = actual - expected (only meaningful for closed shifts)
            BigDecimal mismatch = endCash != null ? endCash.subtract(systemCashTotal) : BigDecimal.ZERO;

            String staffName = (s.getStaff() != null) ? s.getStaff().getFullName() : "";
            String startTime = (s.getStartTime() != null) ? s.getStartTime().format(dtfLocal) : "";

            out.add(new ShiftReconciliationRow(s.getId(), staffName, startTime, startCash,
                endCash != null ? endCash : systemCashTotal, systemRevenue, actualCash, mismatch));
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

    private static String mapProductCategory(ProductCategory c) {
        if (c == null) return "Thuốc không kê đơn";
        return switch (c) {
            case ETC -> "Thuốc kê đơn";
            case OTC -> "Thuốc không kê đơn";
            case SUPPLEMENT -> "Sản phẩm chức năng";
        };
    }

    // ================== Components ==================

    private enum KpiTone {
        POSITIVE, // xanh
        RISK,     // đỏ
        WARNING,  // vàng
        AUTO      // tùy theo giá trị
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
                case POSITIVE -> AppColors.SUCCESS;
                case RISK -> AppColors.DANGER;
                case WARNING -> AppColors.WARNING;
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
        YESTERDAY,
        LAST_WEEK_SAME_DAY
    }

    private enum TimeGranularity {
        DAILY_LAST_7_DAYS,
        HOURLY_TODAY
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
        private final KpiTrend netRevenue;
        private final KpiTrend profit;
        private final KpiTrend totalReturnOrders;
        private final KpiTrend cashMismatch;

        public ManagerKpis(KpiTrend netRevenue, KpiTrend profit, KpiTrend totalReturnOrders, KpiTrend cashMismatch) {
            this.netRevenue = netRevenue;
            this.profit = profit;
            this.totalReturnOrders = totalReturnOrders;
            this.cashMismatch = cashMismatch;
        }

        public KpiTrend getNetRevenue() { return netRevenue; }
        public KpiTrend getProfit() { return profit; }
        public KpiTrend getTotalReturnOrders() { return totalReturnOrders; }
        public KpiTrend getCashMismatch() { return cashMismatch; }
    }

    private static class TimePoint {
        private final String label;
        private final BigDecimal netRevenue;
        private final BigDecimal profit;

        public TimePoint(String label, BigDecimal netRevenue, BigDecimal profit) {
            this.label = label;
            this.netRevenue = nz(netRevenue);
            this.profit = nz(profit);
        }

        public String getLabel() { return label; }
        public BigDecimal getNetRevenue() { return netRevenue; }
        public BigDecimal getProfit() { return profit; }
    }

    private static class TrendingProductRow {
        private final String id;
        private final String name;
        private final String category;
        private final BigDecimal todayRevenue;
        private final BigDecimal avg7DayRevenue;
        private final BigDecimal percentIncrease;

        public TrendingProductRow(String id, String name, String category, BigDecimal todayRevenue, BigDecimal avg7DayRevenue, BigDecimal percentIncrease) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.todayRevenue = nz(todayRevenue);
            this.avg7DayRevenue = nz(avg7DayRevenue);
            this.percentIncrease = nz(percentIncrease);
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public BigDecimal getAvg7DayRevenue() { return avg7DayRevenue; }
        public BigDecimal getPercentIncrease() { return percentIncrease; }
    }

    private static class ShiftReconciliationRow {
        private final String shiftId;
        private final String staffName;
        private final String openTime;
        private final BigDecimal startCash;
        private final BigDecimal endCash;
        private final BigDecimal systemRevenue;
        private final BigDecimal actualCash;
        private final BigDecimal mismatch;

        public ShiftReconciliationRow(String shiftId, String staffName, String openTime, BigDecimal startCash, BigDecimal endCash,
                                     BigDecimal systemRevenue, BigDecimal actualCash, BigDecimal mismatch) {
            this.shiftId = shiftId;
            this.staffName = staffName;
            this.openTime = openTime;
            this.startCash = nz(startCash);
            this.endCash = nz(endCash);
            this.systemRevenue = nz(systemRevenue);
            this.actualCash = nz(actualCash);
            this.mismatch = nz(mismatch);
        }

        public String getShiftId() { return shiftId; }
        public String getStaffName() { return staffName; }
        public String getOpenTime() { return openTime; }
        public BigDecimal getStartCash() { return startCash; }
        public BigDecimal getEndCash() { return endCash; }
        public BigDecimal getSystemRevenue() { return systemRevenue; }
        public BigDecimal getActualCash() { return actualCash; }
        public BigDecimal getMismatch() { return mismatch; }
    }

    // Cập nhật applyKpis / renderChart / loadTrending / loadReconciliation dùng DTO nội bộ
    private void applyKpis(ManagerKpis k) {
        if (k == null) return;

        cardNetRevenue.setValue(currency.format(k.getNetRevenue().getCurrent()));
        cardNetRevenue.setTrend(k.getNetRevenue().getPercentChange(), k.getNetRevenue().getDelta());
        cardNetRevenue.setTone(KpiTone.POSITIVE);

        // Lợi nhuận: xanh nếu dương, đỏ nếu âm
        BigDecimal profit = k.getProfit().getCurrent();
        cardProfit.setValue(currency.format(profit));
        cardProfit.setTrend(k.getProfit().getPercentChange(), k.getProfit().getDelta());
        cardProfit.setTone(profit.compareTo(BigDecimal.ZERO) >= 0 ? KpiTone.POSITIVE : KpiTone.RISK);

        cardReturns.setValue(formatInt(k.getTotalReturnOrders().getCurrent()));
        cardReturns.setTrend(k.getTotalReturnOrders().getPercentChange(), k.getTotalReturnOrders().getDelta());
        cardReturns.setTone(KpiTone.WARNING);

        BigDecimal mismatch = k.getCashMismatch().getCurrent();
        cardCashMismatch.setValue(currency.format(mismatch));
        cardCashMismatch.setTrend(k.getCashMismatch().getPercentChange(), k.getCashMismatch().getDelta());
        cardCashMismatch.setTone(mismatch.compareTo(BigDecimal.ZERO) == 0 ? KpiTone.POSITIVE : KpiTone.RISK);
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

        boolean isHourlyView = points.size() == 24;

        // For hourly view, show labels only for key hours (0, 6, 12, 18) but keep all data points
        List<String> x = new java.util.ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (isHourlyView) {
                // Show label only every 6 hours: 0, 6, 12, 18
                if (i % 6 == 0) {
                    x.add(points.get(i).getLabel());
                } else {
                    x.add(" "); // Use space instead of empty string to avoid XChart error
                }
            } else {
                x.add(points.get(i).getLabel());
            }
        }

        List<Double> revenue = points.stream().map(p -> p.getNetRevenue().doubleValue()).toList();
        List<Double> profitData = points.stream().map(p -> p.getProfit().doubleValue()).toList();

        CategoryChart chart = new CategoryChartBuilder()
            .width(600)
            .height(280)
            .title(isHourlyView ? "Doanh thu & Lợi nhuận theo giờ (hôm nay)" : "Doanh thu & Lợi nhuận (7 ngày)")
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
        chart.getStyler().setAvailableSpaceFill(isHourlyView ? 0.95 : 0.8);
        chart.getStyler().setOverlapped(true);

        // Custom Y-axis formatter to avoid E notation (scientific notation)
        chart.getStyler().setYAxisDecimalPattern("#,###");

        chart.addSeries("Doanh thu thuần", x, revenue).setFillColor(AppColors.PRIMARY);
        chart.addSeries("Lợi nhuận", x, profitData).setFillColor(AppColors.SUCCESS);

        chartWrap.add(new XChartPanel<>(chart), BorderLayout.CENTER);
        chartWrap.revalidate();
        chartWrap.repaint();
    }

    private void loadTrending(List<TrendingProductRow> rows) {
        mdlTrending.setRowCount(0);
        if (rows == null || rows.isEmpty()) return;
        for (TrendingProductRow r : rows) {
            mdlTrending.addRow(new Object[]{
                r.getId(),
                r.getName(),
                r.getCategory(),
                currency.format(r.getTodayRevenue()),
                currency.format(r.getAvg7DayRevenue()),
                r.getPercentIncrease()
            });
        }
    }

    private void loadReconciliation(List<ShiftReconciliationRow> rows) {
        mdlReconciliation.setRowCount(0);
        if (rows == null || rows.isEmpty()) return;
        for (ShiftReconciliationRow r : rows) {
            mdlReconciliation.addRow(new Object[]{
                r.getShiftId(),
                r.getStaffName(),
                r.getOpenTime(),
                currency.format(r.getStartCash()),
                currency.format(r.getEndCash()),
                currency.format(r.getSystemRevenue()),
                currency.format(r.getActualCash()),
                r.getMismatch()
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

    // ================= DataChangeListener implementation =================

    @Override
    public void onInvoiceCreated() {
        // Immediately refresh dashboard when a new invoice is created
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override
    public void onProductChanged() {
        // Immediately refresh dashboard when products change
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override
    public void onPromotionChanged() {
        // Immediately refresh dashboard when promotions change
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override
    public void onDataChanged() {
        // General data change - refresh everything
        SwingUtilities.invokeLater(this::refresh);
    }
}
