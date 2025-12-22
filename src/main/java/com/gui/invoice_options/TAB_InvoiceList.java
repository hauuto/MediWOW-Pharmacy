package com.gui.invoice_options;

import com.bus.BUS_Invoice;
import com.bus.BUS_Shift;
import com.bus.BUS_Staff;
import com.entities.*;
import com.enums.PaymentMethod;
import com.enums.Role;
import com.utils.AppColors;
import com.gui.DIALOG_DatePicker;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class TAB_InvoiceList extends JPanel {
    public JPanel pnlInvoiceList;
    private static final int LEFT_MIN = 750, RIGHT_MIN = 530, TOP_MIN = 200, BOTTOM_MIN = 316;
    private final BUS_Invoice busInvoice = new BUS_Invoice();
    // new helpers
    private final BUS_Shift busShift = new BUS_Shift();
    private final BUS_Staff busStaff = new BUS_Staff();

    private final Staff currentStaff;

    private List<Invoice> invoices = new ArrayList<>();
    private DefaultTableModel mdlInvoice, mdlInvoiceLine, mdlLotAllocation;
    private JTable tblInvoice, tblInvoiceLine, tblLotAllocation;
    // Card view components for invoice list
    private JPanel pnlInvoiceCards;
    private JScrollPane scrInvoiceCards;
    private JPanel selectedCardPanel;
    private Invoice selectedInvoice;
    private InvoiceLine selectedInvoiceLine;

    // Manager filter controls
    private JComboBox<Object> cmbStaffFilter;
    private JComboBox<Object> cmbPaymentFilter;
    private JButton btnView; // unified 'Xem' / 'Áp dụng' button
    private JLabel lblFilterInfo;
    private DIALOG_DatePicker dateFrom;
    private DIALOG_DatePicker dateTo;
    private JComboBox<Object> cmbShiftFilter;

    // Date preset options (same as statistics)
    private static final String[] DATE_PRESETS = {
        "Hôm nay",
        "3 ngày qua",
        "7 ngày qua",
        "Tháng này",
        "Quý này",
        "Năm nay",
        "Tùy chọn"
    };

    private JComboBox<String> cbDatePreset;

    // Formatter for display of creation date/time on invoice cards
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Constructor now requires current staff (can be null for backward compatibility)
    public TAB_InvoiceList(Staff currentStaff) {
        this.currentStaff = currentStaff;
        $$$setupUI$$$();
        createMainLayout();
    }

    // Backward-compatible no-arg constructor
    public TAB_InvoiceList() {
        this(null);
    }

    private void createMainLayout() {
        pnlInvoiceList.add(createSplitPane(), BorderLayout.CENTER);
    }

    // Enable/disable the date pickers (visual feedback)
    private void setDatePickersEnabled(boolean enabled) {
        if (dateFrom != null) {
            dateFrom.setEnabled(enabled);
            dateFrom.setBackground(enabled ? Color.WHITE : new Color(240, 240, 240));
        }
        if (dateTo != null) {
            dateTo.setEnabled(enabled);
            dateTo.setBackground(enabled ? Color.WHITE : new Color(240, 240, 240));
        }
    }

    // Update date pickers values based on preset selection (same semantics as statistics)
    private void updateDatePickersFromPreset() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today;
        LocalDate toDate = today;

        if (cbDatePreset == null) return;
        int selectedIndex = cbDatePreset.getSelectedIndex();
        switch (selectedIndex) {
            case 0: // Hôm nay
                fromDate = today;
                break;
            case 1: // 3 ngày qua
                fromDate = today.minusDays(2);
                break;
            case 2: // 7 ngày qua
                fromDate = today.minusDays(6);
                break;
            case 3: // Tháng này
                fromDate = today.with(TemporalAdjusters.firstDayOfMonth());
                toDate = today.with(TemporalAdjusters.lastDayOfMonth());
                if (toDate.isAfter(today)) toDate = today;
                break;
            case 4: // Quý này
                int currentQuarter = (today.getMonthValue() - 1) / 3;
                fromDate = today.withMonth(currentQuarter * 3 + 1).with(TemporalAdjusters.firstDayOfMonth());
                toDate = fromDate.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
                if (toDate.isAfter(today)) toDate = today;
                break;
            case 5: // Năm nay
                fromDate = today.with(TemporalAdjusters.firstDayOfYear());
                break;
            default: // Tùy chọn
                return; // don't change pickers
        }

        // Update date pickers
        if (dateFrom != null) dateFrom.setDate(java.sql.Date.valueOf(fromDate));
        if (dateTo != null) dateTo.setDate(java.sql.Date.valueOf(toDate));
    }

    public void refreshData() {
        loadInvoices();
    }

    // Populate shift combo for a given day and set renderer
    private void populateShiftCombo(LocalDate day) {
        if (cmbShiftFilter == null) return;
        cmbShiftFilter.removeAllItems();
        cmbShiftFilter.addItem("Tất cả");
        try {
            java.util.List<Shift> shifts = busShift.listShiftsOpenedOn(day != null ? day : LocalDate.now());
            if (shifts != null) for (Shift sh : shifts) cmbShiftFilter.addItem(sh);
        } catch (Exception ignored) {}

        cmbShiftFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Shift) {
                    Shift sh = (Shift) value;
                    String label = (sh.getStartTime() != null ? sh.getStartTime().toString() : sh.getId()) + " - " + (sh.getStaff() != null ? sh.getStaff().getFullName() : "(No staff)");
                    setText(label);
                }
                return c;
            }
        });
    }

    private void loadInvoices() {
        // Role-aware loading:
        try {
            if (currentStaff != null && currentStaff.getRole() == Role.PHARMACIST) {
                // Cashier view: show invoices for current open shift only
                refreshForCashier();
                return;
            }

            // Default / Manager view: show today's invoices
            LocalDateTime from = LocalDate.now().atStartOfDay();
            LocalDateTime to = from.plusDays(1).minusNanos(1);
            List<Invoice> result = busInvoice.getInvoicesByDateRange(from, to);
            if (result == null) result = new ArrayList<>();
            invoices = result;
            populateInvoiceTable(invoices);
        } catch (Exception e) {
            e.printStackTrace();
            invoices = new ArrayList<>();
            populateInvoiceTable(invoices);
        }
    }

    private void refreshForCashier() {
        // Load invoices for the current staff's open shift
        if (currentStaff == null) {
            invoices = new ArrayList<>();
            populateInvoiceTable(invoices);
            return;
        }
        Shift s = busShift.getCurrentOpenShiftForStaff(currentStaff);
        if (s == null) {
            invoices = new ArrayList<>();
            populateInvoiceTable(invoices);
            if (lblFilterInfo != null) lblFilterInfo.setText("Không có ca làm việc đang mở cho nhân viên hiện tại");
            return;
        }
        List<Invoice> result = busInvoice.getInvoicesByShiftId(s.getId());
        if (result == null) result = new ArrayList<>();
        invoices = result;
        populateInvoiceTable(invoices);
        if (lblFilterInfo != null) lblFilterInfo.setText("Hiển thị hóa đơn ca hiện tại: " + (s.getStartTime() != null ? s.getStartTime().toString() : s.getId()));
    }

    private void applyManagerFilters() {
        // If a specific shift is selected, prefer server-side retrieval by shift
        Object shiftSel = cmbShiftFilter != null ? cmbShiftFilter.getSelectedItem() : null;
        if (shiftSel instanceof Shift) {
            Shift sh = (Shift) shiftSel;
            List<Invoice> result = busInvoice.getInvoicesByShiftId(sh.getId());
            if (result == null) result = new ArrayList<>();
            invoices = result;
            // apply staff/payment filters client-side on the shift result
            Object staffSel = cmbStaffFilter != null ? cmbStaffFilter.getSelectedItem() : null;
            Object paySel = cmbPaymentFilter != null ? cmbPaymentFilter.getSelectedItem() : null;
            Stream<Invoice> stream = invoices.stream();
            if (staffSel instanceof Staff) {
                Staff s = (Staff) staffSel;
                stream = stream.filter(inv -> inv.getCreator() != null && s.getId().equals(inv.getCreator().getId()));
            }
            if (paySel instanceof PaymentMethod) {
                PaymentMethod pm = (PaymentMethod) paySel;
                stream = stream.filter(inv -> inv.getPaymentMethod() != null && inv.getPaymentMethod() == pm);
            }
            invoices = stream.toList();
            if (invoices == null || invoices.isEmpty()) {
                invoices = new ArrayList<>();
                populateInvoiceTable(invoices);
                if (lblFilterInfo != null) lblFilterInfo.setText("Không tìm thấy hóa đơn nào cho ca được chọn");
                return;
            }
            populateInvoiceTable(invoices);
            if (lblFilterInfo != null) lblFilterInfo.setText("Hiển thị ca: " + (sh.getStartTime() != null ? sh.getStartTime().toString() : sh.getId()) + " => " + invoices.size() + " hóa đơn");
            return;
        }

        // Otherwise, use date range (from/to pickers)
        LocalDate fromDate = dateFrom != null && dateFrom.getDate() != null ? new java.sql.Date(dateFrom.getDate().getTime()).toLocalDate() : LocalDate.now();
        LocalDate toDate = dateTo != null && dateTo.getDate() != null ? new java.sql.Date(dateTo.getDate().getTime()).toLocalDate() : LocalDate.now();
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(java.time.LocalTime.MAX);
        List<Invoice> result = busInvoice.getInvoicesByDateRange(from, to);
        if (result == null) result = new ArrayList<>();

        Object staffSel = cmbStaffFilter != null ? cmbStaffFilter.getSelectedItem() : null;
        Object paySel = cmbPaymentFilter != null ? cmbPaymentFilter.getSelectedItem() : null;

        Stream<Invoice> stream = result.stream();
        if (staffSel instanceof Staff) {
            Staff s = (Staff) staffSel;
            stream = stream.filter(inv -> inv.getCreator() != null && s.getId().equals(inv.getCreator().getId()));
        }
        if (paySel instanceof PaymentMethod) {
            PaymentMethod pm = (PaymentMethod) paySel;
            stream = stream.filter(inv -> inv.getPaymentMethod() != null && inv.getPaymentMethod() == pm);
        }

        invoices = stream.toList();
        if (invoices == null || invoices.isEmpty()) {
            invoices = new ArrayList<>();
            populateInvoiceTable(invoices);
            if (lblFilterInfo != null) lblFilterInfo.setText("Không tìm thấy hóa đơn nào");
            return;
        }
        populateInvoiceTable(invoices);
        if (lblFilterInfo != null) lblFilterInfo.setText("Hiển thị: " + invoices.size() + " hóa đơn (" + fromDate + " - " + toDate + ")");
    }

    // helper to fill table from invoices list
    private void populateInvoiceTable(List<Invoice> list) {
        // Convert the invoice list into vertical cards instead of table rows
        if (list == null) list = new ArrayList<>();
        // Ensure card panel exists
        if (pnlInvoiceCards == null) createInvoiceTable();
        pnlInvoiceCards.removeAll();
        pnlInvoiceCards.setLayout(new BoxLayout(pnlInvoiceCards, BoxLayout.Y_AXIS));

        DecimalFormat currencyFmt = createCurrencyFormat();

        for (Invoice inv : list) {
            // Use vertical BoxLayout for predictable, compact vertical spacing
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            // Fix card size: width based on LEFT_MIN minus margins, height fixed
            int fixedWidth = Math.max(200, LEFT_MIN - 40); // ensure reasonable minimum
            int fixedHeight = 84; // compact fixed height for each card
            Dimension fixedSize = new Dimension(fixedWidth, fixedHeight);
            card.setPreferredSize(fixedSize);
            card.setMaximumSize(fixedSize);
            card.setMinimumSize(fixedSize);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
             // Light border + inner padding for a card look
             card.setBorder(BorderFactory.createCompoundBorder(
                     BorderFactory.createLineBorder(new Color(220, 220, 220)),
                     // reduce bottom padding from 8 -> 4 to make card bottom tighter
                     BorderFactory.createEmptyBorder(8, 12, 4, 12)
             ));
             card.setBackground(AppColors.WHITE);
             card.setOpaque(true);

            // Header: ID (left, bold) + Creation date (right, small gray)
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            JLabel lblId = new JLabel(inv.getId() != null ? inv.getId() : "");
            lblId.setFont(new Font("Arial", Font.BOLD, 14));
            header.add(lblId, BorderLayout.WEST);
            // Format creation date/time to a friendly format
            String formattedDate = "";
            try {
                Object cd = inv.getCreationDate();
                if (cd instanceof java.time.LocalDateTime ldt) formattedDate = ldt.format(DATE_TIME_FMT);
                else if (cd instanceof java.util.Date d) formattedDate = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(d);
                else formattedDate = cd != null ? cd.toString() : "";
            } catch (Exception ignored) {}
            JLabel lblDate = new JLabel(formattedDate);
            lblDate.setFont(new Font("Arial", Font.PLAIN, 12));
            lblDate.setForeground(Color.GRAY);
            header.add(lblDate, BorderLayout.EAST);
            // Make header occupy a fixed height slice
            int headerH = Math.max(18, fixedHeight / 3); // allocate roughly a third
            header.setPreferredSize(new Dimension(fixedWidth, headerH));
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerH));
            card.add(header);
            // Small gap between header and body (reduced to 2 for tighter layout)
            card.add(Box.createVerticalStrut(2));

            // Body: Creator/Staff with small icon
            JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            body.setOpaque(false);
            JLabel lblCreator = new JLabel();
            lblCreator.setText((inv.getCreator() != null ? inv.getCreator().getFullName() : "(Không xác định)"));
            lblCreator.setFont(new Font("Arial", Font.PLAIN, 13));
            lblCreator.setIconTextGap(6);
            lblCreator.setIcon(new JLabel("\uD83D\uDC64").getIcon());
            body.add(lblCreator);
            body.setMaximumSize(new Dimension(Integer.MAX_VALUE, body.getPreferredSize().height));
            card.add(body);
            // Small gap between body and footer (reduced to match header->body)
            card.add(Box.createVerticalStrut(2));

            // Footer: Type + Total (highlight)
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            JLabel lblType = new JLabel(inv.getType() != null ? inv.getType().toString() : "");
            lblType.setFont(new Font("Arial", Font.PLAIN, 13));
            footer.add(lblType, BorderLayout.WEST);
            String totalText = "";
            try {
                java.math.BigDecimal total = inv.calculateTotal();
                if (total != null) totalText = currencyFmt.format(total);
            } catch (Exception ignored) {}
            JLabel lblTotal = new JLabel(totalText);
            lblTotal.setFont(new Font("Arial", Font.BOLD, 14));
            lblTotal.setForeground(AppColors.PRIMARY);
            footer.add(lblTotal, BorderLayout.EAST);
            footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, footer.getPreferredSize().height));
            card.add(footer);

            // Card mouse behavior: selection and highlighting
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // Deselect previous
                    if (selectedCardPanel != null) selectedCardPanel.setBackground(AppColors.WHITE);
                    // Select this
                    card.setBackground(new Color(220, 240, 255));
                    selectedCardPanel = card;
                    selectedInvoice = inv;
                    // load details into right panel
                    loadInvoiceLines(selectedInvoice);
                }
            });

            // Add wrapper with small spacing and lock wrapper size to card size
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.setPreferredSize(new Dimension(fixedWidth + 4, fixedHeight + 4));
            wrapper.setMaximumSize(new Dimension(fixedWidth + 4, fixedHeight + 4));
            wrapper.add(card, BorderLayout.CENTER);
             wrapper.add(Box.createVerticalStrut(2), BorderLayout.SOUTH);
             pnlInvoiceCards.add(wrapper);
         }

        // Clear detail tables and selection
        if (mdlInvoiceLine != null) mdlInvoiceLine.setRowCount(0);
        if (mdlLotAllocation != null) mdlLotAllocation.setRowCount(0);
        selectedInvoice = null;
        selectedInvoiceLine = null;

        pnlInvoiceCards.revalidate();
        pnlInvoiceCards.repaint();
    }

    private void loadInvoiceLines(Invoice invoice) {
        if (mdlInvoiceLine != null) mdlInvoiceLine.setRowCount(0);
        if (mdlLotAllocation != null) mdlLotAllocation.setRowCount(0);
        selectedInvoiceLine = null;
        if (invoice == null || invoice.getInvoiceLineList() == null) return;
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
            java.math.BigDecimal unitPrice = line.getUnitPrice();
            int qty = line.getQuantity();
            java.math.BigDecimal lineTotal = (unitPrice != null)
                    ? unitPrice.multiply(java.math.BigDecimal.valueOf(qty))
                    : java.math.BigDecimal.ZERO;

            if (mdlInvoiceLine != null) mdlInvoiceLine.addRow(new Object[]{
                    line.getId(),
                    line.getProduct() != null ? line.getProduct().getId() : "",
                    line.getProduct() != null ? line.getProduct().getName() : "",
                    line.getUnitOfMeasure() != null ? line.getUnitOfMeasure().getName() : "",
                    qty,
                    unitPrice,
                    lineTotal,
                    line.getLineType() != null ? line.getLineType().toString() : ""
            });
        }
    }

    private void loadLotAllocations(InvoiceLine invoiceLine) {
        if (mdlLotAllocation != null) mdlLotAllocation.setRowCount(0);
        if (invoiceLine == null || invoiceLine.getLotAllocations() == null) return;
        for (LotAllocation alloc : invoiceLine.getLotAllocations()) {
            if (mdlLotAllocation != null) mdlLotAllocation.addRow(new Object[]{
                    alloc.getId(),
                    alloc.getLot() != null ? alloc.getLot().getId() : "",
                    alloc.getLot() != null ? alloc.getLot().getBatchNumber() : "",
                    alloc.getQuantity(),
                    alloc.getLot() != null && alloc.getLot().getExpiryDate() != null ? alloc.getLot().getExpiryDate().toString() : "",
                    alloc.getLot() != null && alloc.getLot().getStatus() != null ? alloc.getLot().getStatus().toString() : ""
            });
        }
    }

    private JSplitPane createSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        splitPane.setBackground(AppColors.WHITE);
        splitPane.setDividerLocation(LEFT_MIN);
        return splitPane;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(AppColors.WHITE);
        left.setMinimumSize(new Dimension(LEFT_MIN, 0));

        // Filter panel (role-aware) split into two rows so buttons remain visible
        JPanel pnlFilterTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        pnlFilterTop.setBackground(AppColors.WHITE);
        JPanel pnlFilterBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        pnlFilterBottom.setBackground(AppColors.WHITE);

        lblFilterInfo = new JLabel("");
        lblFilterInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblFilterInfo.setForeground(AppColors.TEXT);

        if (currentStaff != null && currentStaff.getRole() == Role.PHARMACIST) {
            // Cashier: show current shift info + refresh button on bottom row
            JButton btnRefresh = new JButton("Làm mới (Ca hiện tại)");
            btnRefresh.addActionListener(e -> refreshForCashier());
            pnlFilterBottom.add(btnRefresh);
            pnlFilterBottom.add(lblFilterInfo);
        } else {
            // Manager: top row = preset + date pickers + shift; bottom row = staff + payment + buttons
            pnlFilterTop.add(new JLabel("Khoảng:"));
            cbDatePreset = new JComboBox<>(DATE_PRESETS);
            cbDatePreset.setPreferredSize(new Dimension(140, 32));
            cbDatePreset.setSelectedIndex(0); // default Hôm nay
            pnlFilterTop.add(cbDatePreset);

            pnlFilterTop.add(new JLabel("Từ ngày:"));
            dateFrom = new DIALOG_DatePicker(java.sql.Date.valueOf(LocalDate.now()));
            dateFrom.setPreferredSize(new Dimension(140, 32));
            pnlFilterTop.add(dateFrom);

            pnlFilterTop.add(new JLabel("Đến:"));
            dateTo = new DIALOG_DatePicker(java.sql.Date.valueOf(LocalDate.now()));
            dateTo.setPreferredSize(new Dimension(140, 32));
            pnlFilterTop.add(dateTo);

            // Initialize date pickers state like statistics
            setDatePickersEnabled(false);
            updateDatePickersFromPreset();

            // When preset changes, enable/disable pickers and update values
            cbDatePreset.addActionListener(e -> {
                int idx = cbDatePreset.getSelectedIndex();
                boolean isCustom = idx == DATE_PRESETS.length - 1; // "Tùy chọn"
                setDatePickersEnabled(isCustom);
                if (!isCustom) updateDatePickersFromPreset();
                try {
                    java.util.Date d = dateFrom.getDate();
                    java.time.LocalDate ld = d != null ? new java.sql.Date(d.getTime()).toLocalDate() : LocalDate.now();
                    populateShiftCombo(ld);
                } catch (Exception ignored) {}
            });

            // when date pickers change, refresh shifts
            java.beans.PropertyChangeListener dateListener = evt -> {
                if ("date".equals(evt.getPropertyName())) {
                    try {
                        java.util.Date d = dateFrom.getDate();
                        java.time.LocalDate ld = d != null ? new java.sql.Date(d.getTime()).toLocalDate() : LocalDate.now();
                        populateShiftCombo(ld);
                    } catch (Exception ignored2) {}
                }
            };
            dateFrom.addPropertyChangeListener(dateListener);
            dateTo.addPropertyChangeListener(dateListener);

            // Add validation listener to ensure dateTo >= dateFrom
            java.beans.PropertyChangeListener validationListener = evt -> {
                if ("date".equals(evt.getPropertyName())) {
                    try {
                        java.util.Date from = dateFrom.getDate();
                        java.util.Date to = dateTo.getDate();
                        if (from != null) {
                            dateTo.setMinDate(from);
                            if (to != null && to.before(from)) {
                                dateTo.setDate(from);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            };
            dateFrom.addPropertyChangeListener(validationListener);
            dateTo.addPropertyChangeListener(validationListener);

            // Shift dropdown on top row
            pnlFilterTop.add(new JLabel("Ca:"));
            cmbShiftFilter = new JComboBox<>();
            cmbShiftFilter.addItem("Tất cả");
            pnlFilterTop.add(cmbShiftFilter);

            // Bottom row: staff, payment, actions
            pnlFilterBottom.add(new JLabel("Nhân viên:"));
            cmbStaffFilter = new JComboBox<>();
            cmbStaffFilter.addItem("Tất cả");
            try {
                List<Staff> staffs = busStaff.getAllStaffs();
                if (staffs != null) for (Staff s : staffs) cmbStaffFilter.addItem(s);
            } catch (Exception ignored) {}
            pnlFilterBottom.add(cmbStaffFilter);

            pnlFilterBottom.add(new JLabel("Thanh toán:"));
            cmbPaymentFilter = new JComboBox<>();
            cmbPaymentFilter.addItem("Tất cả");
            for (PaymentMethod pm : PaymentMethod.values()) cmbPaymentFilter.addItem(pm);
            pnlFilterBottom.add(cmbPaymentFilter);

            // Single action button (Xem / Áp dụng)
            btnView = new JButton("Xem");
            btnView.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnView.setBackground(AppColors.PRIMARY);
            btnView.setForeground(Color.WHITE);
            btnView.setFocusPainted(false);
            btnView.setBorderPainted(false);
            btnView.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnView.setPreferredSize(new Dimension(100, 32));
            btnView.addActionListener(e -> applyManagerFilters());
            pnlFilterBottom.add(btnView);

            pnlFilterBottom.add(lblFilterInfo);

            // default info
            lblFilterInfo.setText("Hiển thị: Hôm nay");
        }

        // populate shifts for today by default
        populateShiftCombo(LocalDate.now());

        // combine two rows into a single vertical container
        JPanel pnlFilter = new JPanel(new BorderLayout());
        pnlFilter.setBackground(AppColors.WHITE);
        pnlFilter.add(pnlFilterTop, BorderLayout.NORTH);
        pnlFilter.add(pnlFilterBottom, BorderLayout.SOUTH);

        Box titleBox = Box.createVerticalBox(), titleH = Box.createHorizontalBox();
        JLabel title = new JLabel("DANH SÁCH HÓA ĐƠN");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(AppColors.DARK);

        // Actions panel placed on the title row so buttons are always visible
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionsPanel.setOpaque(false);
        if (btnView != null) actionsPanel.add(btnView);
        // For cashier show refresh on actions panel

        titleH.add(Box.createHorizontalGlue()); titleH.add(title); titleH.add(Box.createHorizontalStrut(12)); titleH.add(actionsPanel); titleH.add(Box.createHorizontalGlue());
        titleBox.add(Box.createVerticalStrut(20)); titleBox.add(titleH); titleBox.add(Box.createVerticalStrut(20));

        // Put title and filter into a single top container so both are visible
        Box top = Box.createVerticalBox();
        top.add(titleBox);
        top.add(pnlFilter);
        left.add(top, BorderLayout.NORTH);

        createInvoiceTable();
        // Add the card scroll pane (created in createInvoiceTable) instead of the old table
        left.add(scrInvoiceCards != null ? scrInvoiceCards : new JScrollPane(), BorderLayout.CENTER);
        return left;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(AppColors.BACKGROUND);
        right.setMinimumSize(new Dimension(RIGHT_MIN, 0));
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createUpperSection(), createLowerSection());
        verticalSplit.setBackground(AppColors.BACKGROUND);
        verticalSplit.setDividerLocation(TOP_MIN);
        verticalSplit.setResizeWeight(0.5);
        right.add(verticalSplit, BorderLayout.CENTER);
        return right;
    }

    private JPanel createUpperSection() {
        JPanel upper = new JPanel(new BorderLayout());
        upper.setBackground(AppColors.BACKGROUND);
        upper.setMinimumSize(new Dimension(0, TOP_MIN));
        Box titleBox = Box.createVerticalBox(), titleH = Box.createHorizontalBox();
        JLabel title = new JLabel("CHI TIẾT DÒNG HÓA ĐƠN");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(AppColors.DARK);
        titleH.add(Box.createHorizontalGlue()); titleH.add(title); titleH.add(Box.createHorizontalGlue());
        titleBox.add(Box.createVerticalStrut(20)); titleBox.add(titleH); titleBox.add(Box.createVerticalStrut(20));
        upper.add(titleBox, BorderLayout.NORTH);
        createInvoiceLineTable();
        upper.add(new JScrollPane(tblInvoiceLine), BorderLayout.CENTER);
        return upper;
    }

    private JPanel createLowerSection() {
        JPanel lower = new JPanel(new BorderLayout());
        lower.setBackground(AppColors.BACKGROUND);
        lower.setMinimumSize(new Dimension(0, BOTTOM_MIN));
        Box titleBox = Box.createVerticalBox(), titleH = Box.createHorizontalBox();
        JLabel title = new JLabel("PHÂN BỔ LÔ HÀNG");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(AppColors.DARK);
        titleH.add(Box.createHorizontalGlue()); titleH.add(title); titleH.add(Box.createHorizontalGlue());
        titleBox.add(Box.createVerticalStrut(20)); titleBox.add(titleH); titleBox.add(Box.createVerticalStrut(20));
        lower.add(titleBox, BorderLayout.NORTH);
        createLotAllocationTable();
        lower.add(new JScrollPane(tblLotAllocation), BorderLayout.CENTER);
        return lower;
    }

    private void createInvoiceTable() {
        // Create a vertical panel to hold invoice cards instead of a table
        pnlInvoiceCards = new JPanel();
        pnlInvoiceCards.setBackground(AppColors.WHITE);
        pnlInvoiceCards.setLayout(new BoxLayout(pnlInvoiceCards, BoxLayout.Y_AXIS));
        scrInvoiceCards = new JScrollPane(pnlInvoiceCards);
        scrInvoiceCards.getVerticalScrollBar().setUnitIncrement(16);
    }

    private void createInvoiceLineTable() {
        mdlInvoiceLine = new DefaultTableModel(new String[]{"Mã dòng", "Mã sản phẩm", "Tên sản phẩm", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền", "Loại"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblInvoiceLine = new JTable(mdlInvoiceLine);
        styleTable(tblInvoiceLine); tblInvoiceLine.setBackground(AppColors.BACKGROUND);
        CurrencyRenderer currencyRenderer = new CurrencyRenderer();
        tblInvoiceLine.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer);
        tblInvoiceLine.getColumnModel().getColumn(6).setCellRenderer(currencyRenderer);
        tblInvoiceLine.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblInvoiceLine.getSelectedRow() >= 0) {
                int row = tblInvoiceLine.getSelectedRow();
                if (selectedInvoice != null && selectedInvoice.getInvoiceLineList() != null && row >= 0 && row < selectedInvoice.getInvoiceLineList().size()) {
                    selectedInvoiceLine = selectedInvoice.getInvoiceLineList().get(row);
                    loadLotAllocations(selectedInvoiceLine);
                }
            }
        });
    }

    private void createLotAllocationTable() {
        mdlLotAllocation = new DefaultTableModel(new String[]{"Mã phân bổ", "Mã lô", "Số lô", "Số lượng", "Ngày hết hạn", "Trạng thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLotAllocation = new JTable(mdlLotAllocation);
        styleTable(tblLotAllocation); tblLotAllocation.setBackground(AppColors.BACKGROUND);
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.getTableHeader().setReorderingAllowed(false);
        table.setBackground(AppColors.WHITE);
        table.setRowHeight(35);
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(AppColors.WHITE);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, s, f, row, col);
                c.setBackground(row % 2 == 0 ? AppColors.WHITE : AppColors.BACKGROUND);
                if (s) c.setBackground(t.getSelectionBackground());
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(renderer);
    }

    private DecimalFormat createCurrencyFormat() {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.'); s.setDecimalSeparator(',');
        DecimalFormat f = new DecimalFormat("#,000 'Đ'", s);
        f.setGroupingUsed(true); f.setGroupingSize(3);
        return f;
    }

    private void $$$setupUI$$$() {
        pnlInvoiceList = new JPanel();
        pnlInvoiceList.setLayout(new BorderLayout(0, 0));
        pnlInvoiceList.setBackground(AppColors.WHITE);
    }

    public JComponent $$$getRootComponent$$$() { return pnlInvoiceList; }

    private class CurrencyRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat fmt = createCurrencyFormat();
        public CurrencyRenderer() { setHorizontalAlignment(SwingConstants.RIGHT); setFont(new Font("Arial", Font.PLAIN, 16)); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            comp.setBackground(r % 2 == 0 ? AppColors.WHITE : AppColors.BACKGROUND);
            if (s) comp.setBackground(t.getSelectionBackground());
            if (v instanceof java.math.BigDecimal bd) {
                setText(fmt.format(bd));
                return this;
            }
            if (v instanceof Number) setText(fmt.format(((Number) v).doubleValue()));
            else setText(v == null ? "" : v.toString());
            return this;
        }
    }
}
