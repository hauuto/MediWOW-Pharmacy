package com.gui.invoice_options;

import com.entities.*;
import com.utils.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.util.List;

public class TAB_ExchangeInvoice extends JFrame implements ActionListener, MouseListener, FocusListener, KeyListener, DocumentListener {
    public JPanel pnlExchangeInvoice;
    private static final int LEFT_MIN = 750, RIGHT_MIN = 530, TOP_MIN = 200, BOTTOM_MIN = 316;
    private JTextField txtInvoiceSearch;
    private JWindow invoiceSearchWindow;
    private JList<String> invoiceSearchResultsList;
    private DefaultListModel<String> invoiceSearchResultsModel;
    private List<Invoice> currentInvoiceSearchResults = new ArrayList<>();
    private DefaultTableModel mdlOriginalInvoiceLine;
    private JTable tblOriginalInvoiceLine;
    private JScrollPane scrOriginalInvoiceLine;
    private DefaultTableModel mdlExchangeInvoiceLine;
    private JTable tblExchangeInvoiceLine;
    private JScrollPane scrExchangeInvoiceLine;
    private JTextField txtProductSearch;
    private JButton btnBarcodeScan;
    private JWindow productSearchWindow;
    private JList<String> productSearchResultsList;
    private DefaultListModel<String> productSearchResultsModel;
    private List<Product> currentProductSearchResults = new ArrayList<>();
    private boolean barcodeScanningEnabled = false;
    private JTextField txtPrescriptionCode, txtVat, txtTotal;
    private JFormattedTextField txtCustomerPayment;
    private JPanel pnlCashOptions;
    private JButton btnProcessPayment;
    private Window parentWindow;

    public TAB_ExchangeInvoice(Staff creator) {
        $$$setupUI$$$();
        parentWindow = SwingUtilities.getWindowAncestor(pnlExchangeInvoice);
        createMainLayout();
    }

    private void createMainLayout() {
        pnlExchangeInvoice.add(createInvoiceSearchBar(), BorderLayout.NORTH); pnlExchangeInvoice.add(createSplitPane(), BorderLayout.CENTER);
    }

    private Box createInvoiceSearchBar() {
        Box v = Box.createVerticalBox(), h = Box.createHorizontalBox();
        v.setOpaque(true); v.setBackground(AppColors.WHITE);
        v.add(Box.createVerticalStrut(5)); v.add(h); v.add(Box.createVerticalStrut(5));
        h.add(Box.createHorizontalStrut(5));
        txtInvoiceSearch = new JTextField();
        h.add(generateLabelAndTextField(new JLabel("Tìm hóa đơn gốc:"), txtInvoiceSearch, "Nhập mã hóa đơn cần đổi...", "Nhập mã hóa đơn cần đổi", 0));
        h.add(Box.createHorizontalStrut(5));
        setupInvoiceSearchAutocomplete();
        return v;
    }

    private void setupInvoiceSearchAutocomplete() {
        invoiceSearchResultsModel = new DefaultListModel<>();
        invoiceSearchResultsList = new JList<>(invoiceSearchResultsModel);
        invoiceSearchResultsList.setFont(new Font("Arial", Font.PLAIN, 16));
        invoiceSearchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        invoiceSearchResultsList.setName("invoiceSearchResultsList");
        invoiceSearchWindow = new JWindow(parentWindow);
        invoiceSearchWindow.add(new JScrollPane(invoiceSearchResultsList));
        invoiceSearchWindow.setFocusableWindowState(false);
        txtInvoiceSearch.getDocument().addDocumentListener(this);
        txtInvoiceSearch.addKeyListener(this); txtInvoiceSearch.addFocusListener(this);
        invoiceSearchResultsList.addMouseListener(this);
    }

    private void performInvoiceSearch() {
        String search = txtInvoiceSearch.getText().trim().toLowerCase();
        if (search.isEmpty() || search.equals("nhập mã hóa đơn cần đổi...") || txtInvoiceSearch.getForeground().equals(AppColors.PLACEHOLDER_TEXT)) {
            invoiceSearchWindow.setVisible(false); return;
        }
        invoiceSearchResultsModel.clear(); currentInvoiceSearchResults.clear();
        // TODO: Replace with actual invoice search from BUS_Invoice
        if (!currentInvoiceSearchResults.isEmpty()) {
            Point loc = txtInvoiceSearch.getLocationOnScreen();
            int h = Math.min(currentInvoiceSearchResults.size(), 5) * 25 + 4;
            invoiceSearchWindow.setLocation(loc.x, loc.y + txtInvoiceSearch.getHeight());
            invoiceSearchWindow.setSize(txtInvoiceSearch.getWidth(), h);
            invoiceSearchWindow.setVisible(true);
            if (invoiceSearchResultsModel.getSize() > 0) invoiceSearchResultsList.setSelectedIndex(0);
        } else invoiceSearchWindow.setVisible(false);
    }

    private void selectInvoice(int idx) {
        if (idx < 0 || idx >= currentInvoiceSearchResults.size()) return;
        Invoice invoice = currentInvoiceSearchResults.get(idx);
        loadOriginalInvoiceLines(invoice);
        txtInvoiceSearch.setText(invoice.getId());
        txtInvoiceSearch.setForeground(AppColors.TEXT);
        invoiceSearchWindow.setVisible(false);
    }

    private void loadOriginalInvoiceLines(Invoice invoice) {
        mdlOriginalInvoiceLine.setRowCount(0);
        // TODO: Load invoice lines from the selected invoice
    }

    private JSplitPane createSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        splitPane.setBackground(AppColors.WHITE);
        splitPane.setDividerLocation(LEFT_MIN);
        return splitPane;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE); left.setMinimumSize(new Dimension(LEFT_MIN, 0));
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createUpperSection(), createLowerSection());
        verticalSplit.setBackground(AppColors.WHITE);
        verticalSplit.setDividerLocation(300); verticalSplit.setResizeWeight(0.5);
        left.add(verticalSplit, BorderLayout.CENTER);
        return left;
    }

    private JPanel createUpperSection() {
        JPanel upper = new JPanel(new BorderLayout());
        upper.setBackground(Color.WHITE); upper.setMinimumSize(new Dimension(0, TOP_MIN));
        Box titleBox = Box.createVerticalBox(), titleH = Box.createHorizontalBox();
        JLabel title = new JLabel("CHI TIẾT HÓA ĐƠN GỐC");
        title.setFont(new Font("Arial", Font.BOLD, 20)); title.setForeground(AppColors.DARK);
        titleH.add(Box.createHorizontalGlue()); titleH.add(title); titleH.add(Box.createHorizontalGlue());
        titleBox.add(Box.createVerticalStrut(20)); titleBox.add(titleH); titleBox.add(Box.createVerticalStrut(20));
        upper.add(titleBox, BorderLayout.NORTH);
        createOriginalInvoiceLineTable();
        upper.add(scrOriginalInvoiceLine, BorderLayout.CENTER);
        return upper;
    }

    private JPanel createLowerSection() {
        JPanel lower = new JPanel(new BorderLayout());
        lower.setBackground(Color.WHITE); lower.setMinimumSize(new Dimension(0, BOTTOM_MIN));
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(Color.WHITE);
        Box titleBox = Box.createVerticalBox(), titleH = Box.createHorizontalBox();
        JLabel title = new JLabel("CHI TIẾT HÓA ĐƠN ĐỔI HÀNG");
        title.setFont(new Font("Arial", Font.BOLD, 20)); title.setForeground(AppColors.DARK);
        titleH.add(Box.createHorizontalGlue()); titleH.add(title); titleH.add(Box.createHorizontalGlue());
        titleBox.add(Box.createVerticalStrut(20)); titleBox.add(titleH); titleBox.add(Box.createVerticalStrut(20));
        topSection.add(titleBox, BorderLayout.NORTH);
        topSection.add(createProductSearchBar(), BorderLayout.SOUTH);
        lower.add(topSection, BorderLayout.NORTH);
        createExchangeInvoiceLineTable();
        lower.add(scrExchangeInvoiceLine, BorderLayout.CENTER);
        lower.add(createExchangeTableButtons(), BorderLayout.SOUTH);
        return lower;
    }

    private Box createProductSearchBar() {
        Box v = Box.createVerticalBox(), h = Box.createHorizontalBox();
        v.setOpaque(true); v.setBackground(Color.WHITE);
        v.add(Box.createVerticalStrut(5)); v.add(h); v.add(Box.createVerticalStrut(5));
        h.add(Box.createHorizontalStrut(5));
        txtProductSearch = new JTextField();
        h.add(generateLabelAndTextField(new JLabel("Tìm kiếm thuốc:"), txtProductSearch, "Nhập mã/tên/tên rút gọn của thuốc...", "Nhập mã/tên/tên rút gọn của thuốc", 0));
        h.add(Box.createHorizontalStrut(5));
        btnBarcodeScan = new JButton(new ImageIcon("src/main/resources/icons/png_scanner.png"));
        btnBarcodeScan.setMargin(new Insets(10,10,10,10)); btnBarcodeScan.setBorderPainted(false);
        btnBarcodeScan.setOpaque(true); btnBarcodeScan.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBarcodeScan.setName("btnBarcodeScan"); btnBarcodeScan.addActionListener(this); btnBarcodeScan.addMouseListener(this);
        updateBarcodeScanButtonAppearance();
        h.add(btnBarcodeScan); h.add(Box.createHorizontalStrut(5));
        setupProductSearchAutocomplete();
        return v;
    }

    private void setupProductSearchAutocomplete() {
        productSearchResultsModel = new DefaultListModel<>();
        productSearchResultsList = new JList<>(productSearchResultsModel);
        productSearchResultsList.setFont(new Font("Arial", Font.PLAIN, 16));
        productSearchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productSearchResultsList.setName("productSearchResultsList");
        productSearchWindow = new JWindow(parentWindow);
        productSearchWindow.add(new JScrollPane(productSearchResultsList));
        productSearchWindow.setFocusableWindowState(false);
        txtProductSearch.getDocument().addDocumentListener(this);
        txtProductSearch.addKeyListener(this); txtProductSearch.addFocusListener(this);
        productSearchResultsList.addMouseListener(this);
    }

    private void performProductSearch() {
        String search = txtProductSearch.getText().trim().toLowerCase();
        if (search.isEmpty() || search.equals("nhập mã/tên/tên rút gọn của thuốc...") || txtProductSearch.getForeground().equals(AppColors.PLACEHOLDER_TEXT)) {
            productSearchWindow.setVisible(false); return;
        }
        productSearchResultsModel.clear(); currentProductSearchResults.clear();
        // TODO: Replace with actual product search from BUS_Product
        if (!currentProductSearchResults.isEmpty()) {
            Point loc = txtProductSearch.getLocationOnScreen();
            int h = Math.min(currentProductSearchResults.size(), 5) * 25 + 4;
            productSearchWindow.setLocation(loc.x, loc.y + txtProductSearch.getHeight());
            productSearchWindow.setSize(txtProductSearch.getWidth(), h);
            productSearchWindow.setVisible(true);
            if (productSearchResultsModel.getSize() > 0) productSearchResultsList.setSelectedIndex(0);
        } else productSearchWindow.setVisible(false);
    }

    private void selectProduct(int idx) {
        if (idx < 0 || idx >= currentProductSearchResults.size()) return;
        Product product = currentProductSearchResults.get(idx);
        addProductToExchangeTable(product);
        txtProductSearch.setText(""); txtProductSearch.setForeground(AppColors.TEXT);
        productSearchWindow.setVisible(false);
    }

    private void addProductToExchangeTable(Product product) {
        // TODO: Add product to exchange invoice line table
    }

    private void updateBarcodeScanButtonAppearance() {
        btnBarcodeScan.setBackground(barcodeScanningEnabled ? new Color(46, 204, 113) : Color.WHITE);
    }

    private void toggleBarcodeScanning() {
        barcodeScanningEnabled = !barcodeScanningEnabled;
        updateBarcodeScanButtonAppearance();
        txtProductSearch.setText(""); txtProductSearch.requestFocusInWindow();
        JOptionPane.showMessageDialog(parentWindow, "Chế độ quét mã vạch đã " + (barcodeScanningEnabled ? "BẬT" : "TẮT") + "!", "Quét mã vạch", JOptionPane.INFORMATION_MESSAGE);
        SwingUtilities.invokeLater(() -> txtProductSearch.requestFocusInWindow());
    }

    private void createOriginalInvoiceLineTable() {
        mdlOriginalInvoiceLine = new DefaultTableModel(new String[]{"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblOriginalInvoiceLine = new JTable(mdlOriginalInvoiceLine);
        styleTable(tblOriginalInvoiceLine);
        scrOriginalInvoiceLine = new JScrollPane(tblOriginalInvoiceLine);
    }

    private void createExchangeInvoiceLineTable() {
        mdlExchangeInvoiceLine = new DefaultTableModel(new String[]{"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 2 || c == 3; }
        };
        tblExchangeInvoiceLine = new JTable(mdlExchangeInvoiceLine);
        styleTable(tblExchangeInvoiceLine);
        scrExchangeInvoiceLine = new JScrollPane(tblExchangeInvoiceLine);
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(35);
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, s, f, row, col);
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                if (s) c.setBackground(t.getSelectionBackground());
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        CurrencyRenderer currencyRenderer = new CurrencyRenderer();
        if (table.getColumnCount() >= 6) {
            table.getColumnModel().getColumn(4).setCellRenderer(currencyRenderer);
            table.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer);
        }
    }

    private JPanel createExchangeTableButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT)); p.setBackground(Color.WHITE);
        JButton removeAll = createStyledButton("Xóa tất cả"), remove = createStyledButton("Xóa sản phẩm");
        removeAll.setName("btnRemoveAllExchangeItems"); removeAll.addActionListener(this);
        remove.setName("btnRemoveExchangeItem"); remove.addActionListener(this);
        p.add(removeAll); p.add(remove);
        return p;
    }

    private void removeSelectedExchangeItems() {
        int[] rows = tblExchangeInvoiceLine.getSelectedRows();
        if (rows.length == 0) { JOptionPane.showMessageDialog(parentWindow, "Vui lòng chọn sản phẩm cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(parentWindow, "Bạn có chắc chắn muốn xóa " + rows.length + " sản phẩm?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        for (int i = rows.length - 1; i >= 0; i--) mdlExchangeInvoiceLine.removeRow(rows[i]);
    }

    private void removeAllExchangeItems() {
        if (mdlExchangeInvoiceLine.getRowCount() == 0) { JOptionPane.showMessageDialog(parentWindow, "Không có sản phẩm nào để xóa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(parentWindow, "Bạn có chắc chắn muốn xóa tất cả sản phẩm?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        mdlExchangeInvoiceLine.setRowCount(0);
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(AppColors.WHITE);
        right.setMinimumSize(new Dimension(RIGHT_MIN, 0));
        right.add(createInvoice(), BorderLayout.NORTH);
        return right;
    }

    private Box createInvoice() {
        Box h = Box.createHorizontalBox(), v = Box.createVerticalBox();
        h.add(Box.createHorizontalStrut(10)); h.add(v); h.add(Box.createHorizontalStrut(10));
        JLabel title = new JLabel("HÓA ĐƠN ĐỔI HÀNG");
        title.setFont(new Font("Arial", Font.BOLD, 20)); title.setForeground(AppColors.DARK);
        Box th = Box.createHorizontalBox(); th.add(Box.createHorizontalGlue()); th.add(title); th.add(Box.createHorizontalGlue());
        v.add(Box.createVerticalStrut(20)); v.add(th); v.add(Box.createVerticalStrut(20));

        Box pay = Box.createHorizontalBox();
        TitledBorder payb = BorderFactory.createTitledBorder("Thông tin thanh toán");
        payb.setTitleFont(new Font("Arial", Font.BOLD, 16)); payb.setTitleColor(AppColors.PRIMARY);
        pay.setBorder(payb);
        v.add(pay);
        Box payv = Box.createVerticalBox(); pay.add(payv);
        txtVat = new JTextField(); txtVat.setEditable(false); txtVat.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("VAT:"), txtVat, "", "Thuế hóa đơn", 124));
        payv.add(Box.createVerticalStrut(10));

        txtTotal = new JTextField(); txtTotal.setEditable(false); txtTotal.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("Tổng tiền:"), txtTotal, "", "Tổng tiền", 91));
        payv.add(Box.createVerticalStrut(10));

        NumberFormatter fmt = new NumberFormatter(createCurrencyFormat());
        fmt.setValueClass(Long.class); fmt.setMinimum(0L); fmt.setAllowsInvalid(false); fmt.setCommitsOnValidEdit(true);
        txtCustomerPayment = new JFormattedTextField(fmt);
        payv.add(generateLabelAndTextField(new JLabel("Tiền khách đưa:"), txtCustomerPayment, "Nhập số tiền...", "Nhập số tiền", 47));
        txtCustomerPayment.setValue(0L);
        payv.add(Box.createVerticalStrut(10));

        Box pm = Box.createHorizontalBox();
        JLabel lpm = new JLabel("Phương thức thanh toán:"); lpm.setFont(new Font("Arial", Font.PLAIN, 16));
        pm.add(lpm); pm.add(Box.createHorizontalStrut(29));
        ButtonGroup bg = new ButtonGroup();
        JRadioButton cash = new JRadioButton("Tiền mặt"), bank = new JRadioButton("Ngân hàng/Ví điện tử");
        cash.setFont(new Font("Arial", Font.PLAIN, 16)); cash.setSelected(true); cash.setName("rdoCash"); cash.addActionListener(this);
        bank.setFont(new Font("Arial", Font.PLAIN, 16)); bank.setName("rdoBankOrDigitalWallet"); bank.addActionListener(this);
        bg.add(cash); bg.add(bank);
        pm.add(cash); pm.add(Box.createHorizontalStrut(10)); pm.add(bank); pm.add(Box.createHorizontalGlue());
        payv.add(pm); payv.add(Box.createVerticalStrut(10));

        Box co = Box.createHorizontalBox(); co.add(Box.createHorizontalStrut(203));
        pnlCashOptions = createCashOptionsPanel(); co.add(pnlCashOptions);
        payv.add(co);

        v.add(Box.createVerticalStrut(20));
        Box pb1 = Box.createHorizontalBox(); pb1.add(Box.createHorizontalGlue());
        btnProcessPayment = createStyledButton("Thanh toán");
        btnProcessPayment.setEnabled(true); btnProcessPayment.setName("btnProcessPayment"); btnProcessPayment.addActionListener(this);
        pb1.add(btnProcessPayment);
        v.add(pb1);
        return h;
    }

    private JPanel createCashOptionsPanel() {
        JPanel p = new JPanel(new GridLayout(0, 3, 10, 10));
        p.setBackground(Color.WHITE); p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        for (long inc : new long[]{1000L, 2000L, 5000L, 10000L, 20000L, 50000L, 100000L, 200000L, 500000L}) p.add(createCashButton(inc));
        return p;
    }

    private JButton createCashButton(long amt) {
        JButton b = createStyledButton(String.format("%,d", amt).replace(',', '.'));
        b.setName("cashBtn_" + amt); b.addActionListener(this);
        return b;
    }

    private JButton createStyledButton(String text) {
        JButton b = new JButton(text);
        b.setMargin(new Insets(10,10,10,10)); b.setBorderPainted(false);
        b.setFont(new Font("Arial", Font.BOLD, 16)); b.setForeground(new Color(11, 110, 217));
        b.setOpaque(true); b.setBackground(text.equalsIgnoreCase("Thanh toán") ? AppColors.WHITE : Color.WHITE);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.setName(text); b.addMouseListener(this);
        return b;
    }

    private Box generateLabelAndTextField(JLabel lbl, JTextField txt, String ph, String tt, int gap) {
        Box h = Box.createHorizontalBox();
        lbl.setFont(new Font("Arial", Font.PLAIN, 16)); h.add(lbl); h.add(Box.createHorizontalStrut(gap));
        txt.setFont(new Font("Arial", Font.PLAIN, 16));
        if (!(txt instanceof JFormattedTextField)) setPlaceholderAndTooltip(txt, ph, tt);
        h.add(txt);
        return h;
    }

    private void setPlaceholderAndTooltip(JTextField txt, String placeholder, String tooltip) {
        txt.setText(placeholder); txt.setForeground(AppColors.PLACEHOLDER_TEXT);
        txt.setName("placeholder_" + placeholder);
        txt.addFocusListener(this); txt.setToolTipText(tooltip);
    }

    private DecimalFormat createCurrencyFormat() {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.'); s.setDecimalSeparator(',');
        DecimalFormat f = new DecimalFormat("#,000 'Đ'", s);
        f.setGroupingUsed(true); f.setGroupingSize(3);
        return f;
    }

    private void $$$setupUI$$$() {
        pnlExchangeInvoice = new JPanel(); pnlExchangeInvoice.setLayout(new BorderLayout(0, 0));
        pnlExchangeInvoice.setBackground(AppColors.WHITE);
    }

    public JComponent $$$getRootComponent$$$() { return pnlExchangeInvoice; }

    private class CurrencyRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat fmt = createCurrencyFormat();
        public CurrencyRenderer() { setHorizontalAlignment(SwingConstants.RIGHT); setFont(new Font("Arial", Font.PLAIN, 16)); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof Number) v = fmt.format(((Number) v).doubleValue());
            return super.getTableCellRendererComponent(t, v, s, f, r, c);
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (!(src instanceof JComponent)) return;
        String n = ((JComponent) src).getName();
        if (n == null) return;
        switch (n) {
            case "btnBarcodeScan": toggleBarcodeScanning(); break;
            case "btnRemoveAllExchangeItems": removeAllExchangeItems(); break;
            case "btnRemoveExchangeItem": removeSelectedExchangeItems(); break;
            case "btnProcessPayment": processPayment(); break;
            case "rdoCash": handleCashPaymentMethod(); break;
            case "rdoBankOrDigitalWallet": handleBankPaymentMethod(); break;
            default: if (n.startsWith("cashBtn_")) try { handleCashButtonClick(Long.parseLong(n.substring(8))); } catch (Exception ex) {}
        }
    }

    private void handleCashButtonClick(long amt) {
        if (txtCustomerPayment != null) {
            Object v = txtCustomerPayment.getValue();
            long c = v instanceof Number ? ((Number) v).longValue() : 0;
            txtCustomerPayment.setValue(c + amt); txtCustomerPayment.setForeground(AppColors.TEXT); txtCustomerPayment.requestFocusInWindow();
        }
    }

    private void handleCashPaymentMethod() {
        pnlCashOptions.setVisible(true); pnlCashOptions.getParent().revalidate(); pnlCashOptions.getParent().repaint();
        if (txtCustomerPayment != null) { txtCustomerPayment.setEnabled(true); txtCustomerPayment.setEditable(true); }
    }

    private void handleBankPaymentMethod() {
        pnlCashOptions.setVisible(false); pnlCashOptions.getParent().revalidate(); pnlCashOptions.getParent().repaint();
        if (txtCustomerPayment != null) { txtCustomerPayment.setEnabled(false); txtCustomerPayment.setEditable(false); }
    }

    private void processPayment() {
        JOptionPane.showMessageDialog(parentWindow, "Chức năng thanh toán đang được phát triển!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override public void mouseClicked(MouseEvent e) {
        if (e.getSource() == invoiceSearchResultsList && invoiceSearchResultsList.getSelectedIndex() != -1) selectInvoice(invoiceSearchResultsList.getSelectedIndex());
        else if (e.getSource() == productSearchResultsList && productSearchResultsList.getSelectedIndex() != -1) selectProduct(productSearchResultsList.getSelectedIndex());
    }

    @Override public void mousePressed(MouseEvent e) { if (e.getSource() instanceof JButton) ((JButton) e.getSource()).setBackground(AppColors.LIGHT); }

    @Override public void mouseReleased(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton b = (JButton) e.getSource();
            if ("btnBarcodeScan".equals(b.getName())) updateBarcodeScanButtonAppearance();
            else b.setBackground(b.getText().equalsIgnoreCase("Thanh toán") ? AppColors.WHITE : Color.WHITE);
        }
    }

    @Override public void mouseEntered(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton b = (JButton) e.getSource();
            if (!"btnBarcodeScan".equals(b.getName()) || !barcodeScanningEnabled)
                b.setBackground(b.getText().equalsIgnoreCase("Thanh toán") ? Color.WHITE : AppColors.WHITE);
        }
    }

    @Override public void mouseExited(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton b = (JButton) e.getSource();
            if ("btnBarcodeScan".equals(b.getName())) updateBarcodeScanButtonAppearance();
            else b.setBackground(b.getText().equalsIgnoreCase("Thanh toán") ? AppColors.WHITE : Color.WHITE);
        }
    }

    @Override public void focusGained(FocusEvent e) {
        if (e.getSource() instanceof JTextField) {
            JTextField t = (JTextField) e.getSource();
            if (t.getName() != null && t.getName().startsWith("placeholder_") && t.getForeground().equals(AppColors.PLACEHOLDER_TEXT)) {
                t.setText(""); t.setForeground(AppColors.TEXT);
            }
        }
    }

    @Override public void focusLost(FocusEvent e) {
        Object src = e.getSource();
        if (src == txtProductSearch && !e.isTemporary() && barcodeScanningEnabled) {
            Component o = e.getOppositeComponent();
            if (o instanceof JTextField || o instanceof JFormattedTextField) { barcodeScanningEnabled = false; updateBarcodeScanButtonAppearance(); }
        } else if (src == txtInvoiceSearch) {
            new javax.swing.Timer(150, evt -> invoiceSearchWindow.setVisible(false)) {{ setRepeats(false); start(); }};
        } else if (src == txtProductSearch) {
            new javax.swing.Timer(150, evt -> productSearchWindow.setVisible(false)) {{ setRepeats(false); start(); }};
        } else if (src instanceof JTextField) {
            JTextField t = (JTextField) src;
            if (t.getName() != null && t.getName().startsWith("placeholder_") && t.getText().isEmpty()) { t.setText(t.getToolTipText()); t.setForeground(AppColors.PLACEHOLDER_TEXT); }
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getSource() == txtInvoiceSearch && invoiceSearchWindow.isVisible()) {
            handleSearchNavigation(e, invoiceSearchResultsList, invoiceSearchResultsModel, true);
        } else if (e.getSource() == txtProductSearch) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && barcodeScanningEnabled) { processBarcodeInput(); e.consume(); }
            else if (productSearchWindow.isVisible()) handleSearchNavigation(e, productSearchResultsList, productSearchResultsModel, false);
        }
    }

    private void handleSearchNavigation(KeyEvent e, JList<String> list, DefaultListModel<String> model, boolean isInvoice) {
        int i = list.getSelectedIndex();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN: list.setSelectedIndex((i + 1) % model.getSize()); list.ensureIndexIsVisible(list.getSelectedIndex()); e.consume(); break;
            case KeyEvent.VK_UP: list.setSelectedIndex((i + model.getSize() - 1) % model.getSize()); list.ensureIndexIsVisible(list.getSelectedIndex()); e.consume(); break;
            case KeyEvent.VK_ENTER: if (i != -1) { if (isInvoice) selectInvoice(i); else selectProduct(i); } e.consume(); break;
            case KeyEvent.VK_ESCAPE: (isInvoice ? invoiceSearchWindow : productSearchWindow).setVisible(false); e.consume(); break;
        }
    }

    private void processBarcodeInput() {
        String code = txtProductSearch.getText().trim(); txtProductSearch.setText("");
        if (code.isEmpty()) return;
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(parentWindow, "Quét mã vạch: " + code, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        SwingUtilities.invokeLater(() -> { if (barcodeScanningEnabled) txtProductSearch.requestFocusInWindow(); });
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void insertUpdate(DocumentEvent e) { handleDocumentChange(e); }
    @Override public void removeUpdate(DocumentEvent e) { handleDocumentChange(e); }
    @Override public void changedUpdate(DocumentEvent e) { handleDocumentChange(e); }

    private void handleDocumentChange(DocumentEvent e) {
        if (txtInvoiceSearch != null && e.getDocument() == txtInvoiceSearch.getDocument()) SwingUtilities.invokeLater(this::performInvoiceSearch);
        else if (txtProductSearch != null && e.getDocument() == txtProductSearch.getDocument()) SwingUtilities.invokeLater(this::performProductSearch);
    }
}

