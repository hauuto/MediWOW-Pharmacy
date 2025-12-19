package com.gui.invoice_options;

import com.bus.BUS_Invoice;
import com.entities.*;
import com.utils.AppColors;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.*;
import java.util.*;
import java.util.List;

public class TAB_InvoiceList extends JPanel {
    public JPanel pnlInvoiceList;
    private static final int LEFT_MIN = 750, RIGHT_MIN = 530, TOP_MIN = 200, BOTTOM_MIN = 316;
    private final BUS_Invoice busInvoice = new BUS_Invoice();
    private List<Invoice> invoices = new ArrayList<>();
    private DefaultTableModel mdlInvoice, mdlInvoiceLine, mdlLotAllocation;
    private JTable tblInvoice, tblInvoiceLine, tblLotAllocation;
    private Invoice selectedInvoice;
    private InvoiceLine selectedInvoiceLine;

    public TAB_InvoiceList() {
        $$$setupUI$$$();
        createMainLayout();
    }

    private void createMainLayout() {
        pnlInvoiceList.add(createSplitPane(), BorderLayout.CENTER);
    }

    public void refreshData() {
        loadInvoices();
    }

    private void loadInvoices() {
        invoices = busInvoice.getAllInvoices();
        if (invoices == null) invoices = new ArrayList<>();
        mdlInvoice.setRowCount(0);
        for (Invoice inv : invoices) {
            mdlInvoice.addRow(new Object[]{
                inv.getId(),
                inv.getType() != null ? inv.getType().toString() : "",
                inv.getCreationDate() != null ? inv.getCreationDate().toString() : "",
                inv.getCreator() != null ? inv.getCreator().getFullName() : "",
                inv.getPaymentMethod() != null ? inv.getPaymentMethod().toString() : "",
                inv.getPrescriptionCode() != null ? inv.getPrescriptionCode() : "",
                inv.getPromotion() != null ? inv.getPromotion().getName() : ""
            });
        }
        mdlInvoiceLine.setRowCount(0);
        mdlLotAllocation.setRowCount(0);
        selectedInvoice = null;
        selectedInvoiceLine = null;
    }

    private void loadInvoiceLines(Invoice invoice) {
        mdlInvoiceLine.setRowCount(0);
        mdlLotAllocation.setRowCount(0);
        selectedInvoiceLine = null;
        if (invoice == null || invoice.getInvoiceLineList() == null) return;
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
            java.math.BigDecimal unitPrice = line.getUnitPrice();
            int qty = line.getQuantity();
            java.math.BigDecimal lineTotal = (unitPrice != null)
                    ? unitPrice.multiply(java.math.BigDecimal.valueOf(qty))
                    : java.math.BigDecimal.ZERO;

            mdlInvoiceLine.addRow(new Object[]{
                line.getId(),
                line.getProduct() != null ? line.getProduct().getId() : "",
                line.getProduct() != null ? line.getProduct().getName() : "",
                line.getUnitOfMeasure(),
                qty,
                unitPrice,
                lineTotal,
                line.getLineType() != null ? line.getLineType().toString() : ""
            });
        }
    }

    private void loadLotAllocations(InvoiceLine invoiceLine) {
        mdlLotAllocation.setRowCount(0);
        if (invoiceLine == null || invoiceLine.getLotAllocations() == null) return;
        for (LotAllocation alloc : invoiceLine.getLotAllocations()) {
            mdlLotAllocation.addRow(new Object[]{
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
        Box titleBox = Box.createVerticalBox(), titleH = Box.createHorizontalBox();
        JLabel title = new JLabel("DANH SÁCH HÓA ĐƠN");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(AppColors.DARK);
        titleH.add(Box.createHorizontalGlue()); titleH.add(title); titleH.add(Box.createHorizontalGlue());
        titleBox.add(Box.createVerticalStrut(20)); titleBox.add(titleH); titleBox.add(Box.createVerticalStrut(20));
        left.add(titleBox, BorderLayout.NORTH);
        createInvoiceTable();
        left.add(new JScrollPane(tblInvoice), BorderLayout.CENTER);
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
        mdlInvoice = new DefaultTableModel(new String[]{"Mã hóa đơn", "Loại", "Ngày tạo", "Người tạo", "Thanh toán", "Mã đơn thuốc", "Khuyến mãi"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblInvoice = new JTable(mdlInvoice);
        styleTable(tblInvoice);
        tblInvoice.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblInvoice.getSelectedRow() >= 0) {
                int row = tblInvoice.getSelectedRow();
                if (row >= 0 && row < invoices.size()) {
                    selectedInvoice = invoices.get(row);
                    loadInvoiceLines(selectedInvoice);
                }
            }
        });
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
