package com.gui.invoice_options;

import com.bus.*;
import com.entities.*;
import com.enums.*;
import com.utils.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.text.*;
import java.util.*;
import java.util.List;

public class TAB_SalesInvoice extends JFrame implements ActionListener, MouseListener, FocusListener, KeyListener, DocumentListener, PropertyChangeListener, TableModelListener {
    public JPanel pnlSalesInvoice;
    private static final int LEFT_MIN = 750, RIGHT_MIN = 530;
    private final BUS_Product busProduct = new BUS_Product();
    private final BUS_Invoice busInvoice = new BUS_Invoice();
    private final BUS_Promotion busPromotion = new BUS_Promotion();
    private final Invoice invoice;
    private final List<String> previousPrescriptionCodes;
    private final List<Product> products;
    private final List<Promotion> promotions;
    private final Map<String, Product> productMap = new HashMap<>();
    private final Map<Integer, String> previousUOMMap = new HashMap<>();
    private final Map<Integer, String> oldUOMIdMap = new HashMap<>();
    private DefaultTableModel mdlInvoiceLine;
    private JTable tblInvoiceLine;
    private JScrollPane scrInvoiceLine;
    private JButton btnBarcodeScan, btnProcessPayment;
    private JTextField txtSearchInput, txtPrescriptionCode, txtVat, txtPromotionSearch, txtTotal;
    private JFormattedTextField txtCustomerPayment;
    private JPanel pnlCashOptions;
    private boolean barcodeScanningEnabled = false;
    private JWindow searchWindow, promotionSearchWindow;
    private JList<String> searchResultsList, promotionSearchResultsList;
    private DefaultListModel<String> searchResultsModel, promotionSearchResultsModel;
    private List<Product> currentSearchResults = new ArrayList<>();
    private List<Promotion> currentPromotionSearchResults = new ArrayList<>();
    private static final String PRESCRIPTION_PATTERN = "^[a-zA-Z0-9]{5}[a-zA-Z0-9]{7}-[NHCnhc]$";
    private final Window parentWindow;

    public TAB_SalesInvoice(Staff creator) {
        $$$setupUI$$$();
        invoice = new Invoice(InvoiceType.SALES, creator);
        invoice.setPaymentMethod(PaymentMethod.CASH);
        products = busProduct.getAllProducts();
        previousPrescriptionCodes = busInvoice.getAllPrescriptionCodes();
        promotions = busPromotion.getAllPromotions().stream().filter(Promotion::getIsActive).toList();
        parentWindow = SwingUtilities.getWindowAncestor(pnlSalesInvoice);
        createSplitPane();
    }

    private Box createProductSearchBar() {
        Box v = Box.createVerticalBox(), h = Box.createHorizontalBox();
        v.setOpaque(true); v.setBackground(Color.WHITE);
        v.add(Box.createVerticalStrut(5)); v.add(h); v.add(Box.createVerticalStrut(5));
        h.add(Box.createHorizontalStrut(5));
        txtSearchInput = new JTextField();
        h.add(generateLabelAndTextField(new JLabel("Tìm kiếm thuốc:"), txtSearchInput, "Nhập mã/tên/tên rút gọn của thuốc...", "Nhập mã/tên/tên rút gọn của thuốc", 0));
        h.add(Box.createHorizontalStrut(5));
        btnBarcodeScan = new JButton(new ImageIcon("src/main/resources/icons/png_scanner.png"));
        btnBarcodeScan.setMargin(new Insets(10,10,10,10)); btnBarcodeScan.setBorderPainted(false);
        btnBarcodeScan.setOpaque(true); btnBarcodeScan.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBarcodeScan.setName("btnBarcodeScan"); btnBarcodeScan.addActionListener(this); btnBarcodeScan.addMouseListener(this);
        updateBarcodeScanButtonAppearance();
        h.add(btnBarcodeScan); h.add(Box.createHorizontalStrut(5));
        setupSearchAutocomplete(txtSearchInput);
        return v;
    }

    private void setupSearchAutocomplete(JTextField txt) {
        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        searchResultsList.setFont(new Font("Arial", Font.PLAIN, 16));
        searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultsList.setName("searchResultsList");
        searchWindow = new JWindow(SwingUtilities.getWindowAncestor(pnlSalesInvoice));
        searchWindow.add(new JScrollPane(searchResultsList));
        searchWindow.setFocusableWindowState(false);
        txt.getDocument().addDocumentListener(this);
        txt.addKeyListener(this); txt.addFocusListener(this);
        searchResultsList.addMouseListener(this);
    }

    private void performSearch(JTextField txt) {
        String search = txt.getText().trim().toLowerCase();
        if (search.isEmpty() || search.equals("nhập mã/tên/tên rút gọn của thuốc...") || txt.getForeground().equals(Color.GRAY)) {
            searchWindow.setVisible(false); return;
        }
        searchResultsModel.clear(); currentSearchResults.clear();
        for (Product p : products) {
            if ((p.getId() != null && p.getId().toLowerCase().contains(search)) ||
                (p.getName() != null && p.getName().toLowerCase().contains(search)) ||
                (p.getShortName() != null && p.getShortName().toLowerCase().contains(search))) {
                currentSearchResults.add(p);
                searchResultsModel.addElement(p.getId() + " - " + p.getName() + " - " + p.getShortName());
            }
        }
        if (!currentSearchResults.isEmpty()) {
            Point loc = txt.getLocationOnScreen();
            int h = Math.min(currentSearchResults.size(), 5) * 25 + 4;
            searchWindow.setLocation(loc.x, loc.y + txt.getHeight());
            searchWindow.setSize(txt.getWidth(), h);
            searchWindow.setVisible(true);
            if (searchResultsModel.getSize() > 0) searchResultsList.setSelectedIndex(0);
        } else searchWindow.setVisible(false);
    }

    private void selectProduct(int idx, JTextField txt) {
        if (idx < 0 || idx >= currentSearchResults.size()) return;
        addProductToInvoice(currentSearchResults.get(idx));
        txt.setText(""); txt.setForeground(Color.BLACK);
        searchWindow.setVisible(false);
    }

    private void setupPromotionSearchAutocomplete(JTextField txt) {
        promotionSearchResultsModel = new DefaultListModel<>();
        promotionSearchResultsList = new JList<>(promotionSearchResultsModel);
        promotionSearchResultsList.setFont(new Font("Arial", Font.PLAIN, 16));
        promotionSearchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        promotionSearchResultsList.setName("promotionSearchResultsList");
        promotionSearchWindow = new JWindow(SwingUtilities.getWindowAncestor(pnlSalesInvoice));
        promotionSearchWindow.add(new JScrollPane(promotionSearchResultsList));
        promotionSearchWindow.setFocusableWindowState(false);
        txt.getDocument().addDocumentListener(this);
        txt.addKeyListener(this); txt.addFocusListener(this);
        promotionSearchResultsList.addMouseListener(this);
    }

    private void performPromotionSearch() {
        String search = txtPromotionSearch.getText().trim().toLowerCase();
        if (search.isEmpty() || txtPromotionSearch.getForeground().equals(Color.GRAY)) {
            promotionSearchWindow.setVisible(false); return;
        }
        promotionSearchResultsModel.clear(); currentPromotionSearchResults.clear();
        for (Promotion p : promotions) {
            if ((p.getId() != null && p.getId().toLowerCase().contains(search)) ||
                (p.getName() != null && p.getName().toLowerCase().contains(search))) {
                currentPromotionSearchResults.add(p);
                promotionSearchResultsModel.addElement(p.getId() + " - " + p.getName());
            }
        }
        if (!currentPromotionSearchResults.isEmpty()) {
            Point loc = txtPromotionSearch.getLocationOnScreen();
            int h = Math.min(currentPromotionSearchResults.size(), 5) * 25 + 4;
            promotionSearchWindow.setLocation(loc.x, loc.y + txtPromotionSearch.getHeight());
            promotionSearchWindow.setSize(txtPromotionSearch.getWidth(), h);
            promotionSearchWindow.setVisible(true);
            if (promotionSearchResultsModel.getSize() > 0) promotionSearchResultsList.setSelectedIndex(0);
        } else promotionSearchWindow.setVisible(false);
    }

    private void selectPromotion(int idx) {
        if (idx < 0 || idx >= currentPromotionSearchResults.size()) return;
        Promotion p = currentPromotionSearchResults.get(idx);
        invoice.setPromotion(p);
        txtPromotionSearch.setText(p.getId() + " - " + p.getName());
        txtPromotionSearch.setForeground(Color.BLACK);
        promotionSearchWindow.setVisible(false);
    }

    private void addProductToInvoice(Product product) {
        if (product.getCategory() == ProductCategory.ETC && !isValidPrescriptionCode()) {
            JOptionPane.showMessageDialog(parentWindow, "Sản phẩm '" + product.getName() + "' là thuốc ETC.\nVui lòng nhập mã đơn thuốc hợp lệ.", "Yêu cầu mã đơn thuốc", JOptionPane.WARNING_MESSAGE);
            txtPrescriptionCode.requestFocusInWindow(); return;
        }
        UnitOfMeasure baseUOM = findUnitOfMeasure(product, product.getBaseUnitOfMeasure());
        for (int i = 0; i < mdlInvoiceLine.getRowCount(); i++) {
            if (mdlInvoiceLine.getValueAt(i, 0).equals(product.getId()) && mdlInvoiceLine.getValueAt(i, 2).equals(product.getBaseUnitOfMeasure())) {
                int qty = (int) mdlInvoiceLine.getValueAt(i, 3) + 1;
                mdlInvoiceLine.setValueAt(qty, i, 3);
                double price = parseCurrencyValue(mdlInvoiceLine.getValueAt(i, 4).toString());
                mdlInvoiceLine.setValueAt(qty * price, i, 5);
                // Calculate unit price
                Lot lot = product.getOldestLotAvailable();
                double unitPrice = lot != null ? lot.getRawPrice() * (baseUOM != null ? baseUOM.getBasePriceConversionRate() : 1) : 0.0;
                invoice.updateInvoiceLine(product.getId(), product.getBaseUnitOfMeasure(),
                    new InvoiceLine(java.util.UUID.randomUUID().toString(), product, invoice, product.getBaseUnitOfMeasure(), LineType.SALE, qty, unitPrice));
                updateVatDisplay(); updateTotalDisplay(); validatePrescriptionCodeForInvoice(); return;
            }
        }
        Lot lot = product.getOldestLotAvailable();
        double price = lot != null ? lot.getRawPrice() : 0.0;
        mdlInvoiceLine.addRow(new Object[]{product.getId(), product.getName(), product.getBaseUnitOfMeasure(), 1, price, price});
        productMap.put(product.getId(), product);
        int row = mdlInvoiceLine.getRowCount() - 1;
        previousUOMMap.put(row, product.getBaseUnitOfMeasure());
        oldUOMIdMap.put(row, product.getBaseUnitOfMeasure());
        invoice.addInvoiceLine(new InvoiceLine(java.util.UUID.randomUUID().toString(), product, invoice, product.getBaseUnitOfMeasure(), LineType.SALE, 1, price));
        updateVatDisplay(); updateTotalDisplay(); validatePrescriptionCodeForInvoice();
    }

    private UnitOfMeasure findUnitOfMeasure(Product product, String name) {
        if (name.equals(product.getBaseUnitOfMeasure())) {
            for (UnitOfMeasure uom : product.getUnitOfMeasureList())
                if (uom.getName().equals(name)) return uom;
        }
        for (UnitOfMeasure uom : product.getUnitOfMeasureList())
            if (uom.getName().equals(name)) return uom;
        return null;
    }

    private void updateInvoiceLineFromTable(int row) {
        if (row < 0 || row >= mdlInvoiceLine.getRowCount()) return;
        String productId = (String) mdlInvoiceLine.getValueAt(row, 0);
        String uomName = (String) mdlInvoiceLine.getValueAt(row, 2);
        int quantity = (int) mdlInvoiceLine.getValueAt(row, 3);
        Product product = productMap.get(productId);
        if (product == null) return;
        for (int i = 0; i < mdlInvoiceLine.getRowCount(); i++) {
            if (i != row && mdlInvoiceLine.getValueAt(i, 0).equals(productId) && mdlInvoiceLine.getValueAt(i, 2).equals(uomName)) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentWindow, "Sản phẩm '" + product.getName() + "' với đơn vị '" + uomName + "' đã tồn tại!", "Cảnh báo trùng lặp", JOptionPane.WARNING_MESSAGE));
                String prev = previousUOMMap.get(row);
                mdlInvoiceLine.setValueAt(prev != null ? prev : product.getBaseUnitOfMeasure(), row, 2);
                return;
            }
        }
        UnitOfMeasure uom = findUnitOfMeasure(product, uomName);
        Lot lot = product.getOldestLotAvailable();
        double price = lot != null ? lot.getRawPrice() * (uom != null ? uom.getBasePriceConversionRate() : 1) : 0.0;
        mdlInvoiceLine.setValueAt(price, row, 4);
        mdlInvoiceLine.setValueAt(price * quantity, row, 5);
        String oldUomName = oldUOMIdMap.getOrDefault(row, uomName);
        invoice.updateInvoiceLine(productId, oldUomName,
            new InvoiceLine(java.util.UUID.randomUUID().toString(), product, invoice, uomName, LineType.SALE, quantity, price));
        oldUOMIdMap.put(row, uomName);
        previousUOMMap.put(row, uomName);
        updateVatDisplay(); updateTotalDisplay();
    }

    private void setPlaceholderAndTooltip(JTextField txt, String placeholder, String tooltip) {
        txt.setText(placeholder); txt.setForeground(Color.GRAY);
        txt.setName("placeholder_" + placeholder);
        txt.addFocusListener(this); txt.setToolTipText(tooltip);
    }

    private void createSplitPane() {
        JPanel left = new JPanel(new BorderLayout()), right = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE); left.setMinimumSize(new Dimension(LEFT_MIN, 0));
        right.setBackground(AppColors.WHITE); right.setMinimumSize(new Dimension(RIGHT_MIN, 0));
        left.add(createProductSearchBar(), BorderLayout.NORTH);
        JPanel cont = new JPanel(new BorderLayout()); cont.setBackground(Color.WHITE);
        Box tv = Box.createVerticalBox(), th = Box.createHorizontalBox();
        JLabel title = new JLabel("CHI TIẾT HÓA ĐƠN BÁN HÀNG");
        title.setFont(new Font("Arial", Font.BOLD, 20)); title.setForeground(AppColors.DARK);
        th.add(Box.createHorizontalGlue()); th.add(title); th.add(Box.createHorizontalGlue());
        tv.add(Box.createVerticalStrut(20)); tv.add(th); tv.add(Box.createVerticalStrut(20));
        cont.add(tv, BorderLayout.NORTH); createInvoiceLineTable(); cont.add(scrInvoiceLine, BorderLayout.CENTER);
        left.add(cont, BorderLayout.CENTER); left.add(createInvoiceLineTableButtons(), BorderLayout.SOUTH);
        right.add(createInvoice(), BorderLayout.NORTH);
        pnlSalesInvoice.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right), BorderLayout.CENTER);
    }

    private void createInvoiceLineTable() {
        mdlInvoiceLine = new DefaultTableModel(new String[]{"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 2 || c == 3; }
        };
        tblInvoiceLine = new JTable(mdlInvoiceLine);
        tblInvoiceLine.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblInvoiceLine.setFont(new Font("Arial", Font.PLAIN, 16));
        tblInvoiceLine.getTableHeader().setReorderingAllowed(false);
        tblInvoiceLine.setRowHeight(35);
        tblInvoiceLine.getTableHeader().setBackground(AppColors.PRIMARY);
        tblInvoiceLine.getTableHeader().setForeground(Color.WHITE);
        tblInvoiceLine.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        tblInvoiceLine.getColumnModel().getColumn(2).setCellEditor(new UnitOfMeasureCellEditor());
        tblInvoiceLine.getColumnModel().getColumn(3).setCellEditor(new QuantitySpinnerEditor());
        tblInvoiceLine.getColumnModel().getColumn(3).setCellRenderer(new QuantitySpinnerRenderer());
        tblInvoiceLine.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());
        tblInvoiceLine.getColumnModel().getColumn(5).setCellRenderer(new CurrencyRenderer());
        tblInvoiceLine.setName("tblInvoiceLine");
        tblInvoiceLine.addFocusListener(this);
        tblInvoiceLine.addPropertyChangeListener("tableCellEditor", this);
        mdlInvoiceLine.addTableModelListener(this);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, s, f, row, col);
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                if (s) c.setBackground(t.getSelectionBackground());
                return c;
            }
        };
        for (int i = 0; i < 6; i++) tblInvoiceLine.getColumnModel().getColumn(i).setCellRenderer(r);
        scrInvoiceLine = new JScrollPane(tblInvoiceLine);
    }

    private JPanel createInvoiceLineTableButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT)); p.setBackground(Color.WHITE);
        JButton removeAll = createStyledButton("Xóa tất cả"), remove = createStyledButton("Xóa sản phẩm");
        removeAll.setName("btnRemoveAllItems"); removeAll.addActionListener(this);
        remove.setName("btnRemoveItem"); remove.addActionListener(this);
        p.add(removeAll); p.add(remove);
        return p;
    }

    private void toggleBarcodeScanning() {
        barcodeScanningEnabled = !barcodeScanningEnabled;
        updateBarcodeScanButtonAppearance();
        txtSearchInput.setText(""); txtSearchInput.requestFocusInWindow();
        JOptionPane.showMessageDialog(parentWindow, "Chế độ quét mã vạch đã " + (barcodeScanningEnabled ? "BẬT" : "TẮT") + "!", "Quét mã vạch", JOptionPane.INFORMATION_MESSAGE);
        SwingUtilities.invokeLater(() -> txtSearchInput.requestFocusInWindow());
    }

    private void updateBarcodeScanButtonAppearance() {
        btnBarcodeScan.setBackground(barcodeScanningEnabled ? new Color(46, 204, 113) : Color.WHITE);
    }

    private void processBarcodeInput() {
        String code = txtSearchInput.getText().trim(); txtSearchInput.setText("");
        if (code.isEmpty()) return;
        Product p = products.stream().filter(pr -> code.equals(pr.getBarcode())).findFirst().orElse(null);
        Toolkit.getDefaultToolkit().beep();
        if (p == null)
            JOptionPane.showMessageDialog(parentWindow, "Không tìm thấy sản phẩm với mã vạch: " + code, "Lỗi", JOptionPane.ERROR_MESSAGE);
        else
            addProductToInvoice(p);
        SwingUtilities.invokeLater(() -> { if (barcodeScanningEnabled) txtSearchInput.requestFocusInWindow(); });
    }

    private void removeSelectedItems() {
        int[] rows = tblInvoiceLine.getSelectedRows();
        if (rows.length == 0) { JOptionPane.showMessageDialog(parentWindow, "Vui lòng chọn sản phẩm cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(parentWindow, "Bạn có chắc chắn muốn xóa " + rows.length + " sản phẩm?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        for (int i = rows.length - 1; i >= 0; i--) {
            int row = rows[i];
            String id = (String) mdlInvoiceLine.getValueAt(row, 0);
            Product p = productMap.get(id);
            if (p != null) {
                String uomName = (String) mdlInvoiceLine.getValueAt(row, 2);
                invoice.removeInvoiceLine(id, uomName);
            }
            mdlInvoiceLine.removeRow(row);
        }
        previousUOMMap.clear(); oldUOMIdMap.clear();
        updateVatDisplay(); updateTotalDisplay(); validatePrescriptionCodeForInvoice();
    }

    private void removeAllItems() {
        if (mdlInvoiceLine.getRowCount() == 0) { JOptionPane.showMessageDialog(parentWindow, "Không có sản phẩm nào để xóa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(parentWindow, "Bạn có chắc chắn muốn xóa tất cả sản phẩm?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        for (int i = mdlInvoiceLine.getRowCount() - 1; i >= 0; i--) {
            String id = (String) mdlInvoiceLine.getValueAt(i, 0);
            Product p = productMap.get(id);
            String uomName = (String) mdlInvoiceLine.getValueAt(i, 2);
            if (p != null) invoice.removeInvoiceLine(id, uomName);
        }
        mdlInvoiceLine.setRowCount(0);
        previousUOMMap.clear(); oldUOMIdMap.clear(); productMap.clear();
        updateVatDisplay(); updateTotalDisplay(); validatePrescriptionCodeForInvoice();
    }

    private JButton createStyledButton(String text) {
        JButton b = new JButton(text);
        b.setMargin(new Insets(10,10,10,10)); b.setBorderPainted(false);
        b.setFont(new Font("Arial", Font.BOLD, 16)); b.setForeground(new Color(11, 110, 217));
        b.setOpaque(true); b.setBackground(text.equalsIgnoreCase("Thanh toán") ? AppColors.WHITE : Color.WHITE);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.setName(text); b.addMouseListener(this);
        return b;
    }

    private Box createInvoice() {
        Box h = Box.createHorizontalBox(), v = Box.createVerticalBox();
        h.add(Box.createHorizontalStrut(10)); h.add(v); h.add(Box.createHorizontalStrut(10));
        JLabel title = new JLabel("HÓA ĐƠN BÁN HÀNG");
        title.setFont(new Font("Arial", Font.BOLD, 20)); title.setForeground(AppColors.DARK);
        Box th = Box.createHorizontalBox(); th.add(Box.createHorizontalGlue()); th.add(title); th.add(Box.createHorizontalGlue());
        v.add(Box.createVerticalStrut(82)); v.add(th); v.add(Box.createVerticalStrut(20));

        Box presc = Box.createHorizontalBox();
        TitledBorder pb = BorderFactory.createTitledBorder("Thông tin kê đơn thuốc");
        pb.setTitleFont(new Font("Arial", Font.BOLD, 16)); pb.setTitleColor(AppColors.PRIMARY);
        presc.setBorder(pb);
        v.add(presc); v.add(Box.createVerticalStrut(40));
        Box pv = Box.createVerticalBox(); presc.add(pv);
        txtPrescriptionCode = new JTextField(); txtPrescriptionCode.setName("txtPrescriptionCode"); txtPrescriptionCode.addFocusListener(this);
        pv.add(generateLabelAndTextField(new JLabel("Mã đơn kê thuốc:"), txtPrescriptionCode, "Điền mã đơn kê thuốc (nếu có)...", "Điền mã đơn kê thuốc", 39));
        pv.add(Box.createVerticalStrut(10));

        Box pay = Box.createHorizontalBox();
        TitledBorder payb = BorderFactory.createTitledBorder("Thông tin thanh toán");
        payb.setTitleFont(new Font("Arial", Font.BOLD, 16)); payb.setTitleColor(AppColors.PRIMARY);
        pay.setBorder(payb);
        v.add(pay);
        Box payv = Box.createVerticalBox(); pay.add(payv);
        txtPromotionSearch = new JTextField();
        payv.add(generateLabelAndTextField(new JLabel("Tìm kiếm khuyến mãi:"), txtPromotionSearch, "Điền mã hoặc tên khuyến mãi...", "Điền mã hoặc tên khuyến mãi", 10));
        setupPromotionSearchAutocomplete(txtPromotionSearch);
        payv.add(Box.createVerticalStrut(10));

        txtVat = new JTextField(); txtVat.setEditable(false); txtVat.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("VAT:"), txtVat, "", "Thuế hóa đơn", 124));
        payv.add(Box.createVerticalStrut(10));

        JTextField txtDiscount = new JTextField(); txtDiscount.setEditable(false); txtDiscount.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("Tiền giảm giá:"), txtDiscount, "", "Tiền giảm giá", 60));
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
        updateCashButtons();
        return p;
    }

    private void updateCashButtons() {
        if (pnlCashOptions == null || txtTotal == null || invoice == null) return;
        pnlCashOptions.removeAll();
        long[] amounts = {
            1000L, 2000L, 5000L, 10000L, 20000L, 50000L, 100000L, 200000L, 500000L,
            ((long) Math.ceil(invoice.calculateTotal() / 1000) * 1000)
        };
        for (long inc : amounts) {
            pnlCashOptions.add(createCashButton(inc));
            if (inc == 500000L)
                pnlCashOptions.add(new JPanel()); // Placeholder for alignment
        }
        pnlCashOptions.revalidate(); pnlCashOptions.repaint();
    }

    private JButton createCashButton(long amt) {
        JButton b = createStyledButton(String.format("%,d", amt).replace(',', '.'));
        b.setName("cashBtn_" + amt); b.addActionListener(this);
        return b;
    }

    private void $$$setupUI$$$() {
        pnlSalesInvoice = new JPanel(); pnlSalesInvoice.setLayout(new BorderLayout(0, 0));
        pnlSalesInvoice.setBackground(AppColors.WHITE);
    }

    public JComponent $$$getRootComponent$$$() { return pnlSalesInvoice; }

    private class UnitOfMeasureCellEditor extends DefaultCellEditor {
        private JComboBox<String> comboBox;
        private int editingRow = -1;
        public UnitOfMeasureCellEditor() {
            super(new JComboBox<>());
            comboBox = (JComboBox<String>) getComponent();
            comboBox.setFont(new Font("Arial", Font.PLAIN, 16));
            comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            setClickCountToStart(1);
            comboBox.setUI(new BasicComboBoxUI() {
                @Override
                protected ComboPopup createPopup() {
                    return new BasicComboPopup(comboBox) {
                        @Override
                        public void show() {
                            if (editingRow >= 0 && tblInvoiceLine != null) {
                                Rectangle cellRect = tblInvoiceLine.getCellRect(editingRow, 2, true);
                                Point tableScreenLoc = tblInvoiceLine.getLocationOnScreen();
                                int popupX = tableScreenLoc.x + cellRect.x;
                                int popupY = tableScreenLoc.y + cellRect.y + cellRect.height;
                                list.setFont(new Font("Arial", Font.PLAIN, 16));
                                setPopupSize(cellRect.width, getPreferredSize().height);
                                setLocation(popupX, popupY);
                                setVisible(true);
                            } else {
                                super.show();
                            }
                        }
                    };
                }
            });
        }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            editingRow = r;
            String id = (String) t.getValueAt(r, 0);
            Product p = productMap.get(id);
            comboBox.removeAllItems();
            if (p != null) {
                comboBox.addItem(p.getBaseUnitOfMeasure());
                for (UnitOfMeasure u : p.getUnitOfMeasureList())
                    if (!u.getName().equals(p.getBaseUnitOfMeasure())) comboBox.addItem(u.getName());
            }
            if (v != null) comboBox.setSelectedItem(v);
            return comboBox;
        }
        public boolean stopCellEditing() {
            editingRow = -1;
            return super.stopCellEditing();
        }
    }

    private class QuantitySpinnerEditor extends DefaultCellEditor {
        private JSpinner spinner;
        private JSpinner.DefaultEditor editor;
        public QuantitySpinnerEditor() {
            super(new JTextField());
            spinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
            spinner.setFont(new Font("Arial", Font.PLAIN, 16));
            editor = (JSpinner.DefaultEditor) spinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
            setClickCountToStart(1);
        }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            if (v instanceof Integer) spinner.setValue(v);
            SwingUtilities.invokeLater(() -> {
                editor.getTextField().requestFocusInWindow();
                editor.getTextField().selectAll();
            });
            return spinner;
        }
        public Object getCellEditorValue() { return spinner.getValue(); }
        public boolean stopCellEditing() {
            try { spinner.commitEdit(); } catch (java.text.ParseException e) {}
            return super.stopCellEditing();
        }
    }

    private class QuantitySpinnerRenderer implements TableCellRenderer {
        private JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        public QuantitySpinnerRenderer() { spinner.setFont(new Font("Arial", Font.PLAIN, 16)); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof Integer) spinner.setValue(v);
            return spinner;
        }
    }

    private class CurrencyRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat fmt = createCurrencyFormat();
        public CurrencyRenderer() { setHorizontalAlignment(SwingConstants.RIGHT); setFont(new Font("Arial", Font.PLAIN, 16)); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof Number) v = fmt.format(((Number) v).doubleValue());
            return super.getTableCellRendererComponent(t, v, s, f, r, c);
        }
    }

    private Box generateLabelAndTextField(JLabel lbl, JTextField txt, String ph, String tt, int gap) {
        Box h = Box.createHorizontalBox();
        lbl.setFont(new Font("Arial", Font.PLAIN, 16)); h.add(lbl); h.add(Box.createHorizontalStrut(gap));
        txt.setFont(new Font("Arial", Font.PLAIN, 16));
        if (!(txt instanceof JFormattedTextField)) setPlaceholderAndTooltip(txt, ph, tt);
        h.add(txt);
        return h;
    }

    private DecimalFormat createCurrencyFormat() {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.'); s.setDecimalSeparator(',');
        DecimalFormat f = new DecimalFormat("#,000 'Đ'", s);
        f.setGroupingUsed(true); f.setGroupingSize(3);
        return f;
    }

    private boolean isValidPrescriptionCode() {
        if (txtPrescriptionCode == null) return false;
        String txt = txtPrescriptionCode.getText();
        return txt != null && txt.matches(PRESCRIPTION_PATTERN);
    }

    private void validatePrescriptionCode() {
        String txt = txtPrescriptionCode.getText().trim();
        if (txt.isEmpty() || txt.equals("Điền mã đơn kê thuốc (nếu có)...") || txtPrescriptionCode.getForeground().equals(Color.GRAY)) {
            boolean hasETC = invoice.getInvoiceLineList().stream().anyMatch(l -> l.getProduct().getCategory() == ProductCategory.ETC);
            if (hasETC) {
                JOptionPane.showMessageDialog(parentWindow, "Hóa đơn có thuốc ETC. Vui lòng nhập mã đơn thuốc!", "Yêu cầu mã đơn thuốc", JOptionPane.WARNING_MESSAGE);
                txtPrescriptionCode.requestFocusInWindow(); btnProcessPayment.setEnabled(false);
            } else {
                invoice.setPrescriptionCode(null); btnProcessPayment.setEnabled(true);
            }
            return;
        }
        if (!txt.matches(PRESCRIPTION_PATTERN)) {
            JOptionPane.showMessageDialog(parentWindow, "Mã đơn thuốc không hợp lệ!\n\nĐịnh dạng: xxxxxyyyyyyy-z\n- 5 ký tự đầu: mã cơ sở\n- 7 ký tự tiếp: mã đơn thuốc\n- 1 ký tự cuối: loại (N/H/C)", "Mã đơn thuốc không hợp lệ", JOptionPane.ERROR_MESSAGE);
            txtPrescriptionCode.selectAll(); txtPrescriptionCode.requestFocusInWindow(); btnProcessPayment.setEnabled(false);
            return;
        }
        if (previousPrescriptionCodes.contains(txt.toLowerCase())) {
            JOptionPane.showMessageDialog(parentWindow, "Mã đơn thuốc đã được sử dụng!", "Mã trùng lặp", JOptionPane.ERROR_MESSAGE);
            txtPrescriptionCode.selectAll(); txtPrescriptionCode.requestFocusInWindow(); btnProcessPayment.setEnabled(false);
            return;
        }
        invoice.setPrescriptionCode(txt); btnProcessPayment.setEnabled(true);
    }

    private void validatePrescriptionCodeForInvoice() {
        boolean hasETC = invoice.getInvoiceLineList().stream().anyMatch(l -> l.getProduct().getCategory() == ProductCategory.ETC);
        btnProcessPayment.setEnabled(!hasETC || isValidPrescriptionCode());
    }

    private double parseCurrencyValue(String val) {
        String c = val.replace("Đ", "").trim().replace(".", "").replace(",", ".");
        try { return Double.parseDouble(c); } catch (NumberFormatException e) { return 0.0; }
    }

    private void updateVatDisplay() {
        if (txtVat != null && invoice != null) txtVat.setText(createCurrencyFormat().format(invoice.calculateVatAmount()));
    }

    private void updateTotalDisplay() {
        if (txtTotal != null && invoice != null) {
            txtTotal.setText(createCurrencyFormat().format(invoice.calculateTotal()));
            updateCashButtons();
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (!(src instanceof JComponent)) return;
        String n = ((JComponent) src).getName();
        if (n == null) return;
        switch (n) {
            case "btnBarcodeScan": toggleBarcodeScanning(); break;
            case "btnRemoveAllItems": removeAllItems(); break;
            case "btnRemoveItem": removeSelectedItems(); break;
            case "btnProcessPayment": processPayment(); break;
            case "rdoCash": handleCashPaymentMethod(); break;
            case "rdoBankOrDigitalWallet": handleBankPaymentMethod(); break;
            default: if (n.startsWith("cashBtn_")) try { handleCashButtonClick(Long.parseLong(n.substring(8))); } catch (Exception ex) {}
        }
    }

    @Override public void mouseClicked(MouseEvent e) {
        if (e.getSource() == searchResultsList && searchResultsList.getSelectedIndex() != -1) selectProduct(searchResultsList.getSelectedIndex(), txtSearchInput);
        else if (e.getSource() == promotionSearchResultsList && promotionSearchResultsList.getSelectedIndex() != -1) selectPromotion(promotionSearchResultsList.getSelectedIndex());
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
            if (t.getName() != null && t.getName().startsWith("placeholder_") && t.getForeground().equals(Color.GRAY)) {
                t.setText(""); t.setForeground(Color.BLACK);
            }
        }
    }

    @Override public void focusLost(FocusEvent e) {
        Object src = e.getSource();
        if (src == txtSearchInput && !e.isTemporary() && barcodeScanningEnabled) {
            Component o = e.getOppositeComponent();
            if (o instanceof JTextField || o instanceof JFormattedTextField) { barcodeScanningEnabled = false; updateBarcodeScanButtonAppearance(); }
        } else if (src == txtPrescriptionCode) validatePrescriptionCode();
        else if (src == txtPromotionSearch) new javax.swing.Timer(150, evt -> promotionSearchWindow.setVisible(false)) {{ setRepeats(false); start(); }};
        else if (src instanceof JTextField) {
            JTextField t = (JTextField) src;
            if (t.getName() != null && t.getName().startsWith("placeholder_") && t.getText().isEmpty()) { t.setText(t.getToolTipText()); t.setForeground(Color.GRAY); }
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getSource() == txtSearchInput) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && barcodeScanningEnabled) { processBarcodeInput(); e.consume(); }
            else if (searchWindow.isVisible()) handleNav(e, searchResultsList, searchResultsModel, true);
        } else if (e.getSource() == txtPromotionSearch && promotionSearchWindow.isVisible()) handleNav(e, promotionSearchResultsList, promotionSearchResultsModel, false);
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void insertUpdate(DocumentEvent e) { handleDoc(e); }
    @Override public void removeUpdate(DocumentEvent e) { handleDoc(e); }
    @Override public void changedUpdate(DocumentEvent e) { handleDoc(e); }

    private void handleDoc(DocumentEvent e) {
        if (e.getDocument() == txtSearchInput.getDocument()) SwingUtilities.invokeLater(() -> performSearch(txtSearchInput));
        else if (e.getDocument() == txtPromotionSearch.getDocument()) SwingUtilities.invokeLater(this::performPromotionSearch);
    }

    @Override public void propertyChange(PropertyChangeEvent evt) {
        if ("tableCellEditor".equals(evt.getPropertyName()) && evt.getOldValue() != null && evt.getNewValue() == null) {
            int r = tblInvoiceLine.getEditingRow();
            if (r == -1) r = tblInvoiceLine.getSelectedRow();
            if (r >= 0) updateInvoiceLineFromTable(r);
        }
    }

    @Override public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() >= 0 && (e.getColumn() == 2 || e.getColumn() == 3))
            updateInvoiceLineFromTable(e.getFirstRow());
    }

    private void handleNav(KeyEvent e, JList<String> l, DefaultListModel<String> m, boolean isProd) {
        int i = l.getSelectedIndex();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN: l.setSelectedIndex((i + 1) % m.getSize()); l.ensureIndexIsVisible(l.getSelectedIndex()); e.consume(); break;
            case KeyEvent.VK_UP: l.setSelectedIndex((i + m.getSize() - 1) % m.getSize()); l.ensureIndexIsVisible(l.getSelectedIndex()); e.consume(); break;
            case KeyEvent.VK_ENTER: if (i != -1) { if (isProd) selectProduct(i, txtSearchInput); else selectPromotion(i); } e.consume(); break;
            case KeyEvent.VK_ESCAPE: (isProd ? searchWindow : promotionSearchWindow).setVisible(false); e.consume(); break;
        }
    }

    private void handleCashButtonClick(long amt) {
        if (txtCustomerPayment != null) {
            Object v = txtCustomerPayment.getValue();
            long c = v instanceof Number ? ((Number) v).longValue() : 0;
            txtCustomerPayment.setValue(c + amt); txtCustomerPayment.setForeground(Color.BLACK); txtCustomerPayment.requestFocusInWindow();
        }
    }

    private void handleCashPaymentMethod() {
        pnlCashOptions.setVisible(true); pnlCashOptions.getParent().revalidate(); pnlCashOptions.getParent().repaint();
        if (txtCustomerPayment != null) { txtCustomerPayment.setEnabled(true); txtCustomerPayment.setEditable(true); }
        if (invoice != null) invoice.setPaymentMethod(PaymentMethod.CASH);
    }

    private void handleBankPaymentMethod() {
        pnlCashOptions.setVisible(false); pnlCashOptions.getParent().revalidate(); pnlCashOptions.getParent().repaint();
        if (txtCustomerPayment != null && invoice != null) {
            txtCustomerPayment.setEnabled(false); txtCustomerPayment.setEditable(false); txtCustomerPayment.setValue((long) invoice.calculateTotal());
        }
        if (invoice != null) invoice.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
    }

    private void processPayment() {
        if (invoice.getInvoiceLineList() == null || invoice.getInvoiceLineList().isEmpty()) {
            JOptionPane.showMessageDialog(parentWindow, "Danh sách sản phẩm trống!", "Không thể thanh toán", JOptionPane.WARNING_MESSAGE); return;
        }
        if (invoice.getPaymentMethod() == PaymentMethod.CASH && txtCustomerPayment != null) {
            long pay = txtCustomerPayment.getValue() instanceof Number ? ((Number) txtCustomerPayment.getValue()).longValue() : 0;
            long tot = (long) invoice.calculateTotal();
            if (pay < tot) {
                JOptionPane.showMessageDialog(parentWindow, "Số tiền không đủ!", "Không thể thanh toán", JOptionPane.WARNING_MESSAGE); return;
            }
        }
        try {
            File d = new File("invoices"); if (!d.exists()) d.mkdirs();
            String fn = "invoices/Invoice_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf";
            File f = InvoicePDFGenerator.generateInvoicePDF(invoice, fn);
            int o = JOptionPane.showConfirmDialog(parentWindow, "Thanh toán thành công!\nBạn có muốn mở hóa đơn không?", "Thành công", JOptionPane.YES_NO_OPTION);
            if (o == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
        } catch (Exception ex) { JOptionPane.showMessageDialog(parentWindow, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
    }
}

