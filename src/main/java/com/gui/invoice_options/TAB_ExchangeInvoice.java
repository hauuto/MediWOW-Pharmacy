package com.gui.invoice_options;

import com.bus.*;
import com.dao.DAO_Invoice;
import com.entities.*;
import com.enums.*;
import com.gui.DIALOG_MomoQRCode;
import com.interfaces.DataChangeListener;
import com.interfaces.ShiftChangeListener;
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
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.List;

public class TAB_ExchangeInvoice extends JFrame implements ActionListener, MouseListener, FocusListener, KeyListener, DocumentListener, PropertyChangeListener, TableModelListener {
    public JPanel pnlExchangeInvoice, pnlCashOptions;
    private static final int LEFT_MIN = 750, RIGHT_MIN = 530, TOP_MIN = 200, BOTTOM_MIN = 316;
    private static final String INVOICE_ID_PATTERN = "^INV\\d{4}-\\d{4}$";

    // BUS layer
    private final BUS_Product busProduct = new BUS_Product();
    private final BUS_Invoice busInvoice = new BUS_Invoice();
    private final BUS_Shift busShift = new BUS_Shift();
    private final BUS_Customer busCustomer = new BUS_Customer();
    private final DAO_Invoice daoInvoice = new DAO_Invoice();

    // Staff and Shift
    private final Staff currentStaff;
    private Shift currentShift;
    private ShiftChangeListener shiftChangeListener;
    private DataChangeListener dataChangeListener;

    // Invoice objects
    private Invoice exchangeInvoice;
    private Invoice selectedOriginalInvoice;

    // Products
    private final List<Product> products;
    private final Map<String, Product> productMap = new HashMap<>();
    private final Map<Integer, String> previousUOMMap = new HashMap<>();
    private final Map<Integer, String> oldUOMIdMap = new HashMap<>();
    private final Map<Integer, Integer> previousQuantityMap = new HashMap<>();

    // Original invoice line max quantities (row index -> original quantity)
    private final Map<Integer, Integer> originalMaxQuantityMap = new HashMap<>();

    // Tables
    private DefaultTableModel mdlOriginalInvoiceLine, mdlExchangeInvoiceLine;
    private JTable tblOriginalInvoiceLine, tblExchangeInvoiceLine;
    private JScrollPane scrOriginalInvoiceLine, scrExchangeInvoiceLine;

    // Search components
    private JWindow productSearchWindow;
    private JList<String> productSearchResultsList;
    private DefaultListModel<String> productSearchResultsModel;
    private List<Product> currentProductSearchResults = new ArrayList<>();

    // UI components
    private JButton btnBarcodeScan, btnProcessPayment;
    private JRadioButton rdoCash, rdoBank;
    private JWindow barcodeScanOverlay;
    private boolean barcodeScanningEnabled = false;
    private boolean isUpdatingInvoiceLine = false;
    private JTextField txtSubtotal, txtVat, txtTotal, txtShiftId, txtCustomerName, txtInvoiceSearch, txtProductSearch;
    private JFormattedTextField txtCustomerPayment;
    private Window parentWindow;

    public TAB_ExchangeInvoice(Staff creator, ShiftChangeListener shiftChangeListener) {
        this(creator, shiftChangeListener, null);
    }

    public TAB_ExchangeInvoice(Staff creator, ShiftChangeListener shiftChangeListener, DataChangeListener dataChangeListener) {
        this.currentStaff = Objects.requireNonNull(creator, "Staff cannot be null");
        this.shiftChangeListener = shiftChangeListener;
        this.dataChangeListener = dataChangeListener;
        $$$setupUI$$$();
        parentWindow = SwingUtilities.getWindowAncestor(pnlExchangeInvoice);
        currentShift = ensureCurrentShift();
        products = busProduct.getAllProducts();
        createMainLayout();
        setFieldsEnabled(false); // Disable all fields until invoice is selected
    }

    // Legacy constructor for backward compatibility
    public TAB_ExchangeInvoice(Staff creator) {
        this(creator, null, null);
    }

    private Shift ensureCurrentShift() {
        String workstation = busShift.getCurrentWorkstation();
        Shift shift = busShift.getOpenShiftOnWorkstation(workstation);
        while (shift == null) {
            Object[] options = {"Mở ca", "Hủy"};
            int choice = JOptionPane.showOptionDialog(parentWindow,
                "Chưa có ca làm việc nào đang mở trên máy này.",
                "Yêu cầu mở ca", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (choice != JOptionPane.YES_OPTION) {
                throw new IllegalStateException("Người dùng chưa mở ca làm việc");
            }
            shift = promptOpenShiftDialog();
        }
        return shift;
    }

    private Shift promptOpenShiftDialog() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(parentWindow);
        if (parentFrame == null) parentFrame = new JFrame();
        com.gui.DIALOG_OpenShift openShiftDialog = new com.gui.DIALOG_OpenShift(parentFrame, currentStaff);
        openShiftDialog.setVisible(true);
        Shift openedShift = openShiftDialog.getOpenedShift();
        if (openedShift != null && shiftChangeListener != null) {
            shiftChangeListener.onShiftOpened(openedShift);
        }
        return openedShift;
    }

    private void setFieldsEnabled(boolean enabled) {
        // Product search
        if (txtProductSearch != null) { txtProductSearch.setEnabled(enabled); txtProductSearch.setFocusable(enabled); }
        if (btnBarcodeScan != null) btnBarcodeScan.setEnabled(enabled);

        // Invoice panel fields
        if (txtCustomerName != null) { txtCustomerName.setEnabled(enabled); txtCustomerName.setFocusable(enabled); }
        if (txtCustomerPayment != null) { txtCustomerPayment.setEnabled(enabled); txtCustomerPayment.setFocusable(enabled); }
        if (rdoCash != null) rdoCash.setEnabled(enabled);
        if (rdoBank != null) rdoBank.setEnabled(enabled);
        if (pnlCashOptions != null) setComponentsEnabled(pnlCashOptions, enabled);

        // Exchange table
        if (tblExchangeInvoiceLine != null) tblExchangeInvoiceLine.setEnabled(enabled);

        updateProcessPaymentButton();
    }

    private void setComponentsEnabled(Container container, boolean enabled) {
        for (Component c : container.getComponents()) {
            c.setEnabled(enabled);
            if (c instanceof Container) setComponentsEnabled((Container) c, enabled);
        }
    }

    private void updateProcessPaymentButton() {
        if (btnProcessPayment == null) return;
        boolean hasOriginalInvoice = selectedOriginalInvoice != null;
        boolean hasExchangeLines = mdlExchangeInvoiceLine != null && mdlExchangeInvoiceLine.getRowCount() > 0;
        btnProcessPayment.setEnabled(hasOriginalInvoice && hasExchangeLines);
    }

    private void createMainLayout() {
        pnlExchangeInvoice.add(createInvoiceSearchBar(), BorderLayout.NORTH); pnlExchangeInvoice.add(createSplitPane(), BorderLayout.CENTER);
    }

    private Box createInvoiceSearchBar() {
        Box v = Box.createVerticalBox(), h = Box.createHorizontalBox();
        v.setOpaque(true); v.setBackground(AppColors.BACKGROUND);
        v.add(Box.createVerticalStrut(5)); v.add(h); v.add(Box.createVerticalStrut(5));
        h.add(Box.createHorizontalStrut(5));
        txtInvoiceSearch = new JTextField();
        h.add(generateLabelAndTextField(new JLabel("Tìm hóa đơn gốc:"), txtInvoiceSearch, "Nhập mã hóa đơn cần đổi...", "Nhập mã hóa đơn cần đổi", 0));
        h.add(Box.createHorizontalStrut(5));
        txtInvoiceSearch.addKeyListener(this);
        txtInvoiceSearch.addFocusListener(this);
        return v;
    }

    private void searchAndLoadInvoice() {
        String inputId = txtInvoiceSearch.getText().trim();
        if (inputId.isEmpty() || inputId.equals("Nhập mã hóa đơn cần đổi...") ||
            txtInvoiceSearch.getForeground().equals(AppColors.PLACEHOLDER_TEXT)) {
            return;
        }
        if (!inputId.toUpperCase().matches(INVOICE_ID_PATTERN)) {
            JOptionPane.showMessageDialog(parentWindow,
                "Định dạng mã hóa đơn không hợp lệ!\n\nĐịnh dạng đúng: INVXXXX-XXXX\n(X là các chữ số)",
                "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            txtInvoiceSearch.selectAll();
            txtInvoiceSearch.requestFocusInWindow();
            return;
        }
        String normalizedId = inputId.toUpperCase();
        Invoice invoice = daoInvoice.getInvoice(normalizedId);
        if (invoice == null) {
            JOptionPane.showMessageDialog(parentWindow,
                "Không tìm thấy hóa đơn với mã: " + normalizedId,
                "Không tìm thấy", JOptionPane.WARNING_MESSAGE);
            txtInvoiceSearch.selectAll();
            txtInvoiceSearch.requestFocusInWindow();
            return;
        }
        if (invoice.getType() == InvoiceType.EXCHANGE) {
            JOptionPane.showMessageDialog(parentWindow,
                "Mã hóa đơn này là hóa đơn đổi hàng!\nKhông thể sử dụng hóa đơn đổi hàng để đổi hàng.",
                "Loại hóa đơn không hợp lệ", JOptionPane.WARNING_MESSAGE);
            txtInvoiceSearch.selectAll();
            txtInvoiceSearch.requestFocusInWindow();
            return;
        }
        if (invoice.getType() == InvoiceType.RETURN) {
            JOptionPane.showMessageDialog(parentWindow,
                "Mã hóa đơn này là hóa đơn trả hàng!\nKhông thể sử dụng hóa đơn trả hàng để đổi hàng.",
                "Loại hóa đơn không hợp lệ", JOptionPane.WARNING_MESSAGE);
            txtInvoiceSearch.selectAll();
            txtInvoiceSearch.requestFocusInWindow();
            return;
        }
        if (daoInvoice.isInvoiceReferenced(normalizedId)) {
            JOptionPane.showMessageDialog(parentWindow,
                "Hóa đơn này đã được sử dụng cho đổi/trả hàng trước đó!\nMỗi hóa đơn chỉ có thể đổi/trả hàng một lần.",
                "Hóa đơn đã được tham chiếu", JOptionPane.WARNING_MESSAGE);
            txtInvoiceSearch.selectAll();
            txtInvoiceSearch.requestFocusInWindow();
            return;
        }
        selectedOriginalInvoice = invoice;

        // Create the exchange invoice with reference to original
        exchangeInvoice = new Invoice(InvoiceType.EXCHANGE, currentStaff, currentShift);
        exchangeInvoice.setReferencedInvoice(selectedOriginalInvoice);
        exchangeInvoice.setPaymentMethod(PaymentMethod.CASH);

        loadOriginalInvoiceLines(invoice);

        // Clear exchange table for new exchange
        if (mdlExchangeInvoiceLine != null) {
            mdlExchangeInvoiceLine.setRowCount(0);
            productMap.clear();
            previousUOMMap.clear();
            oldUOMIdMap.clear();
            previousQuantityMap.clear();
        }

        // Update shift display
        refreshOpenShiftAndUI();

        // Enable all fields now that invoice is selected
        setFieldsEnabled(true);

        // Update displays
        updateSubtotalDisplay();
        updateVatDisplay();
        updateTotalDisplay();

        txtInvoiceSearch.setText("");
        txtInvoiceSearch.setForeground(AppColors.TEXT);
        txtInvoiceSearch.requestFocusInWindow();

        JOptionPane.showMessageDialog(parentWindow,
            "Đã chọn hóa đơn gốc: " + normalizedId + "\nBây giờ bạn có thể thêm sản phẩm đổi.",
            "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
    private void loadOriginalInvoiceLines(Invoice invoice) {
        mdlOriginalInvoiceLine.setRowCount(0);
        originalMaxQuantityMap.clear();
        if (invoice == null || invoice.getInvoiceLineList() == null) return;
        int rowIndex = 0;
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
            Product product = line.getProduct();
            UnitOfMeasure uom = line.getUnitOfMeasure();
            if (product == null || uom == null) continue;
            String productId = product.getId();
            String productName = product.getName();
            String uomName = uom.getName();
            int quantity = line.getQuantity();
            BigDecimal unitPrice = line.getUnitPrice();
            BigDecimal subtotal = line.calculateSubtotal(); // Use the proper calculation from InvoiceLine
            mdlOriginalInvoiceLine.addRow(new Object[]{productId, productName, uomName, quantity, unitPrice, subtotal});
            // Store original quantity as max for this row
            originalMaxQuantityMap.put(rowIndex, quantity);
            rowIndex++;
        }
    }

    private JSplitPane createSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        splitPane.setBackground(AppColors.WHITE); splitPane.setDividerLocation(LEFT_MIN);
        return splitPane;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(AppColors.WHITE); left.setMinimumSize(new Dimension(LEFT_MIN, 0));
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createUpperSection(), createLowerSection());
        verticalSplit.setBackground(AppColors.WHITE);
        verticalSplit.setDividerLocation(TOP_MIN); verticalSplit.setResizeWeight(0.5);
        left.add(verticalSplit, BorderLayout.CENTER);
        return left;
    }

    private JPanel createUpperSection() {
        JPanel upper = new JPanel(new BorderLayout());
        upper.setBackground(AppColors.WHITE); upper.setMinimumSize(new Dimension(0, TOP_MIN));
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
        lower.setBackground(AppColors.WHITE); lower.setMinimumSize(new Dimension(0, BOTTOM_MIN));
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(AppColors.WHITE);
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
        v.setOpaque(true); v.setBackground(AppColors.WHITE);
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

        // Search products from BUS_Product
        for (Product p : products) {
            if ((p.getId() != null && p.getId().toLowerCase().contains(search)) ||
                (p.getName() != null && p.getName().toLowerCase().contains(search)) ||
                (p.getShortName() != null && p.getShortName().toLowerCase().contains(search))) {
                currentProductSearchResults.add(p);
                productSearchResultsModel.addElement(p.getId() + " - " + p.getName() + " - " + p.getShortName());
            }
        }

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
        if (selectedOriginalInvoice == null || exchangeInvoice == null) {
            JOptionPane.showMessageDialog(parentWindow, "Vui lòng chọn hóa đơn gốc trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        isUpdatingInvoiceLine = true;
        try {
            UnitOfMeasure baseUOM = findUnitOfMeasure(product, product.getBaseUnitOfMeasure());

            // Check if product already exists in table
            for (int i = 0; i < mdlExchangeInvoiceLine.getRowCount(); i++) {
                if (mdlExchangeInvoiceLine.getValueAt(i, 0).equals(product.getId()) &&
                    mdlExchangeInvoiceLine.getValueAt(i, 2).equals(product.getBaseUnitOfMeasure())) {
                    int qty = (int) mdlExchangeInvoiceLine.getValueAt(i, 3) + 1;

                    // Check inventory
                    Lot lot = product.getOldestLotAvailable();
                    BigDecimal rawPrice = lot != null ? lot.getRawPrice() : BigDecimal.ZERO;
                    BigDecimal basePriceConversion = (baseUOM != null && baseUOM.getBasePriceConversionRate() != null)
                            ? baseUOM.getBasePriceConversionRate() : BigDecimal.ONE;
                    BigDecimal unitPrice = rawPrice.multiply(basePriceConversion);

                    InvoiceLine tempLine = new InvoiceLine(exchangeInvoice, baseUOM, qty, unitPrice, LineType.EXCHANGE_OUT, new ArrayList<>());
                    if (!tempLine.allocateLots()) {
                        int remaining = getRemainingInventoryInUOM(product, product.getBaseUnitOfMeasure());
                        JOptionPane.showMessageDialog(parentWindow,
                            "Số lượng yêu cầu vượt quá tồn kho!\nSản phẩm: " + product.getName() +
                            "\nTồn kho còn lại: " + remaining + " " + product.getBaseUnitOfMeasure(),
                            "Không đủ tồn kho", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    mdlExchangeInvoiceLine.setValueAt(qty, i, 3);
                    BigDecimal price = parseCurrencyValue(mdlExchangeInvoiceLine.getValueAt(i, 4).toString());
                    mdlExchangeInvoiceLine.setValueAt(price.multiply(BigDecimal.valueOf(qty)), i, 5);

                    InvoiceLine updatedLine = new InvoiceLine(exchangeInvoice, baseUOM, qty, unitPrice, LineType.EXCHANGE_OUT, new ArrayList<>());
                    updatedLine.allocateLots();
                    exchangeInvoice.updateInvoiceLine(product.getId(), product.getBaseUnitOfMeasure(), updatedLine);
                    previousQuantityMap.put(i, qty);
                    updateSubtotalDisplay(); updateVatDisplay(); updateTotalDisplay(); updateProcessPaymentButton();
                    return;
                }
            }

            // Add new product
            Lot lot = product.getOldestLotAvailable();
            BigDecimal price = lot != null ? lot.getRawPrice() : BigDecimal.ZERO;

            InvoiceLine newLine = new InvoiceLine(exchangeInvoice, baseUOM, 1, price, LineType.EXCHANGE_OUT, new ArrayList<>());
            if (!newLine.allocateLots()) {
                int remaining = getRemainingInventoryInUOM(product, product.getBaseUnitOfMeasure());
                JOptionPane.showMessageDialog(parentWindow,
                    "Số lượng yêu cầu vượt quá tồn kho!\nSản phẩm: " + product.getName() +
                    "\nTồn kho còn lại: " + remaining + " " + product.getBaseUnitOfMeasure(),
                    "Không đủ tồn kho", JOptionPane.WARNING_MESSAGE);
                return;
            }

            mdlExchangeInvoiceLine.addRow(new Object[]{product.getId(), product.getName(), product.getBaseUnitOfMeasure(), 1, price, price});
            productMap.put(product.getId(), product);
            int row = mdlExchangeInvoiceLine.getRowCount() - 1;
            previousUOMMap.put(row, product.getBaseUnitOfMeasure());
            oldUOMIdMap.put(row, product.getBaseUnitOfMeasure());
            previousQuantityMap.put(row, 1);
            exchangeInvoice.addInvoiceLine(newLine);
            updateSubtotalDisplay(); updateVatDisplay(); updateTotalDisplay(); updateProcessPaymentButton();
        } finally {
            isUpdatingInvoiceLine = false;
        }
    }

    private UnitOfMeasure findUnitOfMeasure(Product product, String name) {
        if (name.equals(product.getBaseUnitOfMeasure())) {
            for (UnitOfMeasure uom : product.getUnitOfMeasureSet())
                if (uom.getName().equals(name)) return uom;
        }
        for (UnitOfMeasure uom : product.getUnitOfMeasureSet())
            if (uom.getName().equals(name)) return uom;
        return null;
    }

    private int getRemainingInventoryInUOM(Product product, String uomName) {
        int totalBaseQuantity = product.getLotSet().stream()
                .filter(lot -> lot.getStatus() == LotStatus.AVAILABLE && lot.getQuantity() > 0)
                .mapToInt(Lot::getQuantity).sum();
        if (uomName.equals(product.getBaseUnitOfMeasure())) return totalBaseQuantity;
        UnitOfMeasure uom = findUnitOfMeasure(product, uomName);
        if (uom == null || uom.getBaseUnitConversionRate() == null || uom.getBaseUnitConversionRate().compareTo(BigDecimal.ZERO) == 0)
            return totalBaseQuantity;
        BigDecimal remaining = BigDecimal.valueOf(totalBaseQuantity).multiply(uom.getBaseUnitConversionRate());
        return remaining.setScale(0, java.math.RoundingMode.FLOOR).intValue();
    }

    private void updateExchangeInvoiceLineFromTable(int row) {
        if (isUpdatingInvoiceLine || row < 0 || row >= mdlExchangeInvoiceLine.getRowCount()) return;
        isUpdatingInvoiceLine = true;
        try {
            String productId = (String) mdlExchangeInvoiceLine.getValueAt(row, 0);
            String uomName = (String) mdlExchangeInvoiceLine.getValueAt(row, 2);
            int quantity = (int) mdlExchangeInvoiceLine.getValueAt(row, 3);
            Product product = productMap.get(productId);
            if (product == null) return;

            // Check for duplicates
            for (int i = 0; i < mdlExchangeInvoiceLine.getRowCount(); i++) {
                if (i != row && mdlExchangeInvoiceLine.getValueAt(i, 0).equals(productId) && mdlExchangeInvoiceLine.getValueAt(i, 2).equals(uomName)) {
                    JOptionPane.showMessageDialog(parentWindow, "Sản phẩm với đơn vị này đã tồn tại!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    String prev = previousUOMMap.get(row);
                    mdlExchangeInvoiceLine.setValueAt(prev != null ? prev : product.getBaseUnitOfMeasure(), row, 2);
                    return;
                }
            }

            UnitOfMeasure uom = findUnitOfMeasure(product, uomName);
            Lot lot = product.getOldestLotAvailable();
            BigDecimal rawPrice = lot != null ? lot.getRawPrice() : BigDecimal.ZERO;
            BigDecimal basePriceConversion = (uom != null && uom.getBasePriceConversionRate() != null)
                    ? uom.getBasePriceConversionRate() : BigDecimal.ONE;
            BigDecimal price = rawPrice.multiply(basePriceConversion);

            String oldUomName = oldUOMIdMap.getOrDefault(row, uomName);
            InvoiceLine newLine = new InvoiceLine(exchangeInvoice, uom, quantity, price, LineType.EXCHANGE_OUT, new ArrayList<>());
            if (!newLine.allocateLots()) {
                int remaining = getRemainingInventoryInUOM(product, uomName);
                JOptionPane.showMessageDialog(parentWindow,
                    "Số lượng yêu cầu vượt quá tồn kho!\nTồn kho còn lại: " + remaining + " " + uomName,
                    "Không đủ tồn kho", JOptionPane.WARNING_MESSAGE);
                String prevUom = previousUOMMap.get(row);
                if (prevUom != null) mdlExchangeInvoiceLine.setValueAt(prevUom, row, 2);
                Integer prevQty = previousQuantityMap.get(row);
                mdlExchangeInvoiceLine.setValueAt(prevQty != null ? prevQty : 1, row, 3);
                return;
            }

            mdlExchangeInvoiceLine.setValueAt(price, row, 4);
            mdlExchangeInvoiceLine.setValueAt(price.multiply(BigDecimal.valueOf(quantity)), row, 5);
            exchangeInvoice.updateInvoiceLine(productId, oldUomName, newLine);
            oldUOMIdMap.put(row, uomName);
            previousUOMMap.put(row, uomName);
            previousQuantityMap.put(row, quantity);
            updateSubtotalDisplay(); updateVatDisplay(); updateTotalDisplay();
        } finally {
            isUpdatingInvoiceLine = false;
        }
    }

    private BigDecimal parseCurrencyValue(String val) {
        String c = val.replace("D", "").replace("Đ", "").trim().replace(".", "").replace(",", ".");
        try {
            if (c.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(c);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void updateBarcodeScanButtonAppearance() {
        btnBarcodeScan.setBackground(barcodeScanningEnabled ? AppColors.PRIMARY : AppColors.WHITE);
    }

    private void toggleBarcodeScanning() {
        barcodeScanningEnabled = !barcodeScanningEnabled;
        updateBarcodeScanButtonAppearance();
        txtProductSearch.setText("");

        if (barcodeScanningEnabled) {
            JOptionPane.showMessageDialog(parentWindow, "Chế độ quét mã vạch đã BẬT!\n\nNhấn chuột bất kỳ để tắt.", "Quét mã vạch", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(this::showBarcodeScanOverlay);
        } else {
            hideBarcodeScanOverlay();
            JOptionPane.showMessageDialog(parentWindow, "Chế độ quét mã vạch đã TẮT!", "Quét mã vạch", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void setupBarcodeScanOverlay() {
        barcodeScanOverlay = new JWindow();
        barcodeScanOverlay.setBackground(new Color(0, 0, 0, 1)); // Nearly transparent

        JPanel overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Semi-transparent overlay to indicate scanning mode
                g.setColor(new Color(0, 0, 0, 30));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlayPanel.setOpaque(false);
        overlayPanel.setLayout(null);

        // Mouse listener to disable barcode scanning on any click
        overlayPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                disableBarcodeScanning();
            }
        });

        barcodeScanOverlay.setContentPane(overlayPanel);
        barcodeScanOverlay.setAlwaysOnTop(true);
    }

    private void showBarcodeScanOverlay() {
        if (barcodeScanOverlay == null) setupBarcodeScanOverlay();

        Window ancestor = SwingUtilities.getWindowAncestor(pnlExchangeInvoice);
        if (ancestor != null) {
            Point loc = ancestor.getLocationOnScreen();
            barcodeScanOverlay.setLocation(loc);
            barcodeScanOverlay.setSize(ancestor.getSize());
        } else {
            // Fallback to screen size
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            barcodeScanOverlay.setLocation(0, 0);
            barcodeScanOverlay.setSize(screenSize);
        }

        barcodeScanOverlay.setVisible(true);
        // Set focus on the product search bar for barcode input
        SwingUtilities.invokeLater(() -> txtProductSearch.requestFocusInWindow());
    }

    private void hideBarcodeScanOverlay() {
        if (barcodeScanOverlay != null) {
            barcodeScanOverlay.setVisible(false);
        }
    }

    private void disableBarcodeScanning() {
        if (barcodeScanningEnabled) {
            barcodeScanningEnabled = false;
            updateBarcodeScanButtonAppearance();
            hideBarcodeScanOverlay();
            JOptionPane.showMessageDialog(parentWindow, "Chế độ quét mã vạch đã TẮT!", "Quét mã vạch", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void createOriginalInvoiceLineTable() {
        mdlOriginalInvoiceLine = new DefaultTableModel(new String[]{"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 3; } // Only quantity column is editable
        };
        tblOriginalInvoiceLine = new JTable(mdlOriginalInvoiceLine);
        tblOriginalInvoiceLine.setBackground(AppColors.WHITE);
        tblOriginalInvoiceLine.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblOriginalInvoiceLine.setFont(new Font("Arial", Font.PLAIN, 16));
        tblOriginalInvoiceLine.getTableHeader().setReorderingAllowed(false);
        tblOriginalInvoiceLine.setRowHeight(35);
        tblOriginalInvoiceLine.getTableHeader().setBackground(AppColors.PRIMARY);
        tblOriginalInvoiceLine.getTableHeader().setForeground(AppColors.WHITE);
        tblOriginalInvoiceLine.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        tblOriginalInvoiceLine.getColumnModel().getColumn(3).setCellEditor(new OriginalQuantitySpinnerEditor());
        tblOriginalInvoiceLine.getColumnModel().getColumn(3).setCellRenderer(new QuantitySpinnerRenderer());
        tblOriginalInvoiceLine.setName("tblOriginalInvoiceLine");
        tblOriginalInvoiceLine.addPropertyChangeListener("tableCellEditor", this);
        mdlOriginalInvoiceLine.addTableModelListener(this);

        // Alternating row color renderer for columns 0-2
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, s, f, row, col);
                c.setBackground(row % 2 == 0 ? AppColors.WHITE : AppColors.BACKGROUND);
                if (s) c.setBackground(t.getSelectionBackground());
                return c;
            }
        };
        for (int i = 0; i < 3; i++) tblOriginalInvoiceLine.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);

        // Currency renderer
        CurrencyRenderer currencyRenderer = new CurrencyRenderer();
        tblOriginalInvoiceLine.getColumnModel().getColumn(4).setCellRenderer(currencyRenderer);
        tblOriginalInvoiceLine.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer);

        scrOriginalInvoiceLine = new JScrollPane(tblOriginalInvoiceLine);
    }

    private void createExchangeInvoiceLineTable() {
        mdlExchangeInvoiceLine = new DefaultTableModel(new String[]{"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 2 || c == 3; }
        };
        tblExchangeInvoiceLine = new JTable(mdlExchangeInvoiceLine);
        tblExchangeInvoiceLine.setBackground(AppColors.WHITE);
        tblExchangeInvoiceLine.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblExchangeInvoiceLine.setFont(new Font("Arial", Font.PLAIN, 16));
        tblExchangeInvoiceLine.getTableHeader().setReorderingAllowed(false);
        tblExchangeInvoiceLine.setRowHeight(35);
        tblExchangeInvoiceLine.getTableHeader().setBackground(AppColors.PRIMARY);
        tblExchangeInvoiceLine.getTableHeader().setForeground(AppColors.WHITE);
        tblExchangeInvoiceLine.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        tblExchangeInvoiceLine.getColumnModel().getColumn(2).setCellEditor(new UnitOfMeasureCellEditor());
        tblExchangeInvoiceLine.getColumnModel().getColumn(3).setCellEditor(new QuantitySpinnerEditor());
        tblExchangeInvoiceLine.getColumnModel().getColumn(3).setCellRenderer(new QuantitySpinnerRenderer());
        tblExchangeInvoiceLine.setName("tblExchangeInvoiceLine");
        tblExchangeInvoiceLine.addFocusListener(this);
        tblExchangeInvoiceLine.addPropertyChangeListener("tableCellEditor", this);
        mdlExchangeInvoiceLine.addTableModelListener(this);

        // Alternating row color renderer for columns 0-3
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, s, f, row, col);
                c.setBackground(row % 2 == 0 ? AppColors.WHITE : AppColors.BACKGROUND);
                if (s) c.setBackground(t.getSelectionBackground());
                return c;
            }
        };
        for (int i = 0; i < 4; i++) tblExchangeInvoiceLine.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);

        // Currency renderer
        CurrencyRenderer currencyRenderer = new CurrencyRenderer();
        tblExchangeInvoiceLine.getColumnModel().getColumn(4).setCellRenderer(currencyRenderer);
        tblExchangeInvoiceLine.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer);

        scrExchangeInvoiceLine = new JScrollPane(tblExchangeInvoiceLine);
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.getTableHeader().setReorderingAllowed(false); table.setBackground(AppColors.WHITE);
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
        CurrencyRenderer currencyRenderer = new CurrencyRenderer();
        if (table.getColumnCount() >= 6) {
            table.getColumnModel().getColumn(4).setCellRenderer(currencyRenderer);
            table.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer);
        }
    }

    private JPanel createExchangeTableButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT)); p.setBackground(AppColors.WHITE);
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
        for (int i = rows.length - 1; i >= 0; i--) {
            int row = rows[i];
            String id = (String) mdlExchangeInvoiceLine.getValueAt(row, 0);
            String uomName = (String) mdlExchangeInvoiceLine.getValueAt(row, 2);
            if (exchangeInvoice != null) exchangeInvoice.removeInvoiceLine(id, uomName);
            mdlExchangeInvoiceLine.removeRow(row);
        }
        previousUOMMap.clear(); oldUOMIdMap.clear(); previousQuantityMap.clear();
        updateSubtotalDisplay(); updateVatDisplay(); updateTotalDisplay(); updateProcessPaymentButton();
    }

    private void removeAllExchangeItems() {
        if (mdlExchangeInvoiceLine.getRowCount() == 0) { JOptionPane.showMessageDialog(parentWindow, "Không có sản phẩm nào để xóa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(parentWindow, "Bạn có chắc chắn muốn xóa tất cả sản phẩm?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        for (int i = mdlExchangeInvoiceLine.getRowCount() - 1; i >= 0; i--) {
            String id = (String) mdlExchangeInvoiceLine.getValueAt(i, 0);
            String uomName = (String) mdlExchangeInvoiceLine.getValueAt(i, 2);
            if (exchangeInvoice != null) exchangeInvoice.removeInvoiceLine(id, uomName);
        }
        mdlExchangeInvoiceLine.setRowCount(0);
        previousUOMMap.clear(); oldUOMIdMap.clear(); previousQuantityMap.clear(); productMap.clear();
        updateSubtotalDisplay(); updateVatDisplay(); updateTotalDisplay(); updateProcessPaymentButton();
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(AppColors.BACKGROUND);
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

        Box presc = Box.createHorizontalBox();
        TitledBorder pb = BorderFactory.createTitledBorder("Thông tin chung");
        pb.setTitleFont(new Font("Arial", Font.BOLD, 16)); pb.setTitleColor(AppColors.PRIMARY);
        presc.setBorder(pb);
        v.add(presc); v.add(Box.createVerticalStrut(40));
        Box pv = Box.createVerticalBox(); presc.add(pv);

        txtShiftId = new JTextField(); txtShiftId.setEditable(false); txtShiftId.setFocusable(false);
        pv.add(generateLabelAndTextField(new JLabel("Mã ca:"), txtShiftId, "", "Mã ca làm việc", 112));
        pv.add(Box.createVerticalStrut(10));

        txtCustomerName = new JTextField(); txtCustomerName.setName("txtCustomerName");
        pv.add(generateLabelAndTextField(new JLabel("Tên khách hàng:"), txtCustomerName, "Điền tên khách hàng (nếu có)...", "Điền tên khách hàng", 46));
        pv.add(Box.createVerticalStrut(10));

        Box pay = Box.createHorizontalBox();
        TitledBorder payb = BorderFactory.createTitledBorder("Thông tin thanh toán");
        payb.setTitleFont(new Font("Arial", Font.BOLD, 16)); payb.setTitleColor(AppColors.PRIMARY);
        pay.setBorder(payb);
        v.add(pay);
        Box payv = Box.createVerticalBox(); pay.add(payv);

        // Subtotal of exchange items
        txtSubtotal = new JTextField(); txtSubtotal.setEditable(false); txtSubtotal.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("Tiền hàng đổi:"), txtSubtotal, "", "Tiền hàng đổi", 61));
        payv.add(Box.createVerticalStrut(10));

        txtVat = new JTextField(); txtVat.setEditable(false); txtVat.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("VAT:"), txtVat, "", "Thuế hóa đơn", 124));
        payv.add(Box.createVerticalStrut(10));

        txtTotal = new JTextField(); txtTotal.setEditable(false); txtTotal.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("Chênh lệch:"), txtTotal, "", "Tiền chênh lệch (phải trả thêm hoặc hoàn lại)", 78));
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
        rdoCash = new JRadioButton("Tiền mặt"); rdoBank = new JRadioButton("Ngân hàng/Ví điện tử");
        rdoCash.setFont(new Font("Arial", Font.PLAIN, 16)); rdoCash.setSelected(true); rdoCash.setName("rdoCash"); rdoCash.addActionListener(this);
        rdoBank.setFont(new Font("Arial", Font.PLAIN, 16)); rdoBank.setName("rdoBankOrDigitalWallet"); rdoBank.addActionListener(this);
        bg.add(rdoCash); bg.add(rdoBank);
        pm.add(rdoCash); pm.add(Box.createHorizontalStrut(10)); pm.add(rdoBank); pm.add(Box.createHorizontalGlue());
        payv.add(pm); payv.add(Box.createVerticalStrut(10));

        Box co = Box.createHorizontalBox(); co.add(Box.createHorizontalStrut(203));
        pnlCashOptions = createCashOptionsPanel(); co.add(pnlCashOptions);
        payv.add(co);

        v.add(Box.createVerticalStrut(20));
        Box pb1 = Box.createHorizontalBox(); pb1.add(Box.createHorizontalGlue());
        btnProcessPayment = createStyledButton("Thanh toán");
        btnProcessPayment.setEnabled(false); btnProcessPayment.setName("btnProcessPayment"); btnProcessPayment.addActionListener(this);
        pb1.add(btnProcessPayment);
        v.add(pb1);
        return h;
    }

    private JPanel createCashOptionsPanel() {
        JPanel p = new JPanel(new GridLayout(0, 3, 10, 10));
        p.setBackground(AppColors.WHITE); p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        // Initial buttons will be added by updateCashButtons
        return p;
    }

    private void updateCashButtons() {
        if (pnlCashOptions == null || exchangeInvoice == null) return;
        pnlCashOptions.removeAll();

        BigDecimal total = exchangeInvoice.calculateExchangeTotal();
        // Only show cash buttons if total > 0 (customer needs to pay)
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            pnlCashOptions.revalidate();
            pnlCashOptions.repaint();
            return;
        }

        BigDecimal thousand = BigDecimal.valueOf(1000);
        long roundedToThousand = total.divide(thousand, 0, java.math.RoundingMode.CEILING).multiply(thousand).longValue();

        long[] amounts = {
            1000L, 2000L, 5000L, 10000L, 20000L, 50000L, 100000L, 200000L, 500000L,
            roundedToThousand
        };
        for (long inc : amounts) {
            pnlCashOptions.add(createCashButton(inc));
            if (inc == 500000L)
                pnlCashOptions.add(new JPanel()); // Placeholder for alignment
        }
        pnlCashOptions.revalidate();
        pnlCashOptions.repaint();
    }

    private JButton createCashButton(long amt) {
        JButton b = createStyledButton(String.format("%,d", amt).replace(',', '.'));
        b.setName("cashBtn_" + amt); b.addActionListener(this);
        return b;
    }

    private JButton createStyledButton(String text) {
        int arc = 12;
        boolean isPaymentBtn = text.equalsIgnoreCase("Thanh toán");
        Color defaultBg = isPaymentBtn ? AppColors.BACKGROUND : AppColors.WHITE;
        Color rolloverBg = isPaymentBtn ? AppColors.WHITE : AppColors.BACKGROUND;
        Color pressedBg = AppColors.LIGHT;
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = getModel();
                Color fill = model.isPressed() ? pressedBg : (model.isRollover() ? rolloverBg : getBackground());
                g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc); g2.dispose(); super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false); b.setOpaque(false); b.setBorderPainted(false); b.setFocusPainted(false); b.setRolloverEnabled(true);
        b.setMargin(new Insets(10, 10, 10, 10)); b.setFont(new Font("Arial", Font.BOLD, 16));
        b.setForeground(AppColors.PRIMARY); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setName(text); b.setBackground(defaultBg);
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
        DecimalFormat f = new DecimalFormat("#,##0 'Đ'", s);
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
            if (v instanceof java.math.BigDecimal bd) {
                v = fmt.format(bd);
            } else if (v instanceof Number) {
                v = fmt.format(((Number) v).doubleValue());
            }
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            comp.setBackground(r % 2 == 0 ? AppColors.WHITE : AppColors.BACKGROUND);
            if (s) comp.setBackground(t.getSelectionBackground());
            return comp;
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
        if (txtCustomerPayment != null) {
            txtCustomerPayment.setEnabled(true);
            txtCustomerPayment.setEditable(true);
        }
        if (exchangeInvoice != null) exchangeInvoice.setPaymentMethod(PaymentMethod.CASH);
        updateCashButtons();
        updatePaymentOptionsVisibility();
    }

    private void handleBankPaymentMethod() {
        pnlCashOptions.setVisible(false); pnlCashOptions.getParent().revalidate(); pnlCashOptions.getParent().repaint();
        if (txtCustomerPayment != null && exchangeInvoice != null) {
            txtCustomerPayment.setEnabled(false); txtCustomerPayment.setEditable(false);
            BigDecimal exchangeTotal = exchangeInvoice.calculateExchangeTotal();
            if (exchangeTotal.compareTo(BigDecimal.ZERO) > 0) {
                txtCustomerPayment.setValue(exchangeTotal.longValue());
            }
        }
        if (exchangeInvoice != null) exchangeInvoice.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
    }

    private void updatePaymentOptionsVisibility() {
        if (exchangeInvoice == null || pnlCashOptions == null) return;
        BigDecimal exchangeTotal = exchangeInvoice.calculateExchangeTotal();
        boolean needsPayment = exchangeTotal.compareTo(BigDecimal.ZERO) > 0;
        boolean hasExchangeLines = mdlExchangeInvoiceLine != null && mdlExchangeInvoiceLine.getRowCount() > 0;

        // Cash options only visible if: cash is selected, needs payment, and has exchange lines
        boolean showCashOptions = rdoCash != null && rdoCash.isSelected() && needsPayment && hasExchangeLines;
        pnlCashOptions.setVisible(showCashOptions);

        // If exchange total <= 0, disable payment selection
        if (rdoCash != null) rdoCash.setEnabled(needsPayment);
        if (rdoBank != null) rdoBank.setEnabled(needsPayment);
        if (txtCustomerPayment != null) {
            txtCustomerPayment.setEnabled(needsPayment && rdoCash != null && rdoCash.isSelected());
            txtCustomerPayment.setEditable(needsPayment && rdoCash != null && rdoCash.isSelected());
        }
        if (pnlCashOptions != null) {
            pnlCashOptions.getParent().revalidate();
            pnlCashOptions.getParent().repaint();
        }
    }

    private void updateSubtotalDisplay() {
        if (txtSubtotal != null && exchangeInvoice != null) {
            txtSubtotal.setText(createCurrencyFormat().format(exchangeInvoice.calculateSubtotal()));
        }
    }

    private void updateVatDisplay() {
        if (txtVat != null && exchangeInvoice != null) {
            txtVat.setText(createCurrencyFormat().format(exchangeInvoice.calculateVatAmount()));
        }
    }

    private void updateTotalDisplay() {
        if (txtTotal != null && exchangeInvoice != null) {
            BigDecimal exchangeTotal = exchangeInvoice.calculateExchangeTotal();
            String prefix = exchangeTotal.compareTo(BigDecimal.ZERO) >= 0 ? "" : "-";
            txtTotal.setText(prefix + createCurrencyFormat().format(exchangeTotal.abs()));
        }
        updateCashButtons();
        updatePaymentOptionsVisibility();
        updateProcessPaymentButton();
    }

    private void refreshOpenShiftAndUI() {
        try {
            String workstation = busShift.getCurrentWorkstation();
            Shift openShift = busShift.getOpenShiftOnWorkstation(workstation);
            if (openShift != null) {
                currentShift = openShift;
                if (exchangeInvoice != null) exchangeInvoice.setShift(openShift);
                if (txtShiftId != null) txtShiftId.setText(openShift.getId());
            } else {
                if (txtShiftId != null) txtShiftId.setText(currentShift != null ? currentShift.getId() : "N/A");
            }
        } catch (Exception ex) {
            if (txtShiftId != null) txtShiftId.setText(currentShift != null ? currentShift.getId() : "N/A");
        }
    }

    private String getCustomerNameValue() {
        if (txtCustomerName == null) return null;
        String text = txtCustomerName.getText().trim();
        if (text.isEmpty() || text.equals("Điền tên khách hàng (nếu có)...") ||
            txtCustomerName.getForeground().equals(AppColors.PLACEHOLDER_TEXT)) {
            return null;
        }
        return text;
    }

    private void processPayment() {
        // Remove original invoice lines with quantity 0 before processing
        if (selectedOriginalInvoice != null && selectedOriginalInvoice.getInvoiceLineList() != null) {
            selectedOriginalInvoice.getInvoiceLineList().removeIf(line -> line.getQuantity() == 0);
        }

        if (exchangeInvoice == null || exchangeInvoice.getInvoiceLineList() == null || exchangeInvoice.getInvoiceLineList().isEmpty()) {
            JOptionPane.showMessageDialog(parentWindow, "Danh sách sản phẩm đổi trống!", "Không thể thanh toán", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Add original invoice lines as EXCHANGE_IN to the exchange invoice
        if (selectedOriginalInvoice != null && selectedOriginalInvoice.getInvoiceLineList() != null) {
            for (InvoiceLine origLine : selectedOriginalInvoice.getInvoiceLineList()) {
                if (origLine.getQuantity() > 0) {
                    // Create a new invoice line for EXCHANGE_IN (returned products)
                    InvoiceLine exchangeInLine = new InvoiceLine(
                        exchangeInvoice,
                        origLine.getUnitOfMeasure(),
                        origLine.getQuantity(),
                        origLine.getUnitPrice(),
                        LineType.EXCHANGE_IN,
                        new ArrayList<>()
                    );
                    exchangeInvoice.addInvoiceLine(exchangeInLine);
                }
            }
        }

        BigDecimal exchangeTotal = exchangeInvoice.calculateExchangeTotal();

        // If customer needs to pay (exchange total > 0)
        if (exchangeTotal.compareTo(BigDecimal.ZERO) > 0) {
            if (exchangeInvoice.getPaymentMethod() == PaymentMethod.CASH) {
                if (txtCustomerPayment != null) {
                    long pay = txtCustomerPayment.getValue() instanceof Number ? ((Number) txtCustomerPayment.getValue()).longValue() : 0;
                    if (pay < exchangeTotal.longValue()) {
                        JOptionPane.showMessageDialog(parentWindow, "Số tiền không đủ!", "Không thể thanh toán", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                completeExchangeAndGenerateInvoice();
            } else if (exchangeInvoice.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
                String orderInfo = "Thanh toán hóa đơn đổi hàng MediWOW Pharmacy";
                Window owner = SwingUtilities.getWindowAncestor(pnlExchangeInvoice);
                boolean paymentSuccess = DIALOG_MomoQRCode.showAndPay(owner, exchangeTotal.longValue(), orderInfo);
                if (paymentSuccess) {
                    completeExchangeAndGenerateInvoice();
                } else {
                    JOptionPane.showMessageDialog(parentWindow, "Thanh toán qua MoMo đã bị hủy hoặc thất bại.", "Thanh toán không thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } else {
            // Customer gets refund or no payment needed - complete directly
            completeExchangeAndGenerateInvoice();
        }
    }

    private void completeExchangeAndGenerateInvoice() {
        try {
            refreshOpenShiftAndUI();

            // Process lot quantities based on line type
            for (InvoiceLine line : exchangeInvoice.getInvoiceLineList()) {
                if (line.getLineType() == LineType.EXCHANGE_OUT) {
                    // Deduct lot quantities for EXCHANGE_OUT items (products going out)
                    for (LotAllocation allocation : line.getLotAllocations()) {
                        boolean success = busProduct.deductLotQuantity(allocation.getLot().getId(), allocation.getQuantity());
                        if (!success) {
                            throw new RuntimeException("Không thể cập nhật số lượng lô: " + allocation.getLot().getId());
                        }
                    }
                } else if (line.getLineType() == LineType.EXCHANGE_IN) {
                    // Add lot quantities for EXCHANGE_IN items (products coming back)
                    // Get the original invoice line to find the lot allocations
                    if (selectedOriginalInvoice != null) {
                        for (InvoiceLine origLine : selectedOriginalInvoice.getInvoiceLineList()) {
                            if (origLine.getProduct().getId().equals(line.getProduct().getId()) &&
                                origLine.getUnitOfMeasure().getName().equals(line.getUnitOfMeasure().getName())) {
                                // Add back the quantity from original lot allocations
                                for (LotAllocation allocation : origLine.getLotAllocations()) {
                                    // Calculate the proportion of quantity being returned
                                    int returnQty = line.getQuantity();
                                    int origQty = origLine.getQuantity();
                                    if (origQty > 0 && returnQty > 0) {
                                        // Add the quantity back to the lot
                                        boolean success = busProduct.addLotQuantity(allocation.getLot().getId(), allocation.getQuantity());
                                        if (!success) {
                                            throw new RuntimeException("Không thể cập nhật số lượng lô: " + allocation.getLot().getId());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }

            // Create customer if provided
            String customerName = getCustomerNameValue();
            if (customerName != null && !customerName.isEmpty()) {
                try {
                    Customer customer = new Customer(customerName);
                    busCustomer.addCustomer(customer);
                    exchangeInvoice.setCustomer(customer);
                } catch (Exception e) {
                    System.err.println("Warning: Could not save customer: " + e.getMessage());
                }
            }

            // Save invoice to database
            String generatedInvoiceId = busInvoice.saveInvoice(exchangeInvoice);

            // Get customer payment
            BigDecimal customerPayment = BigDecimal.ZERO;
            if (txtCustomerPayment != null && txtCustomerPayment.getValue() instanceof Number) {
                customerPayment = BigDecimal.valueOf(((Number) txtCustomerPayment.getValue()).longValue());
            }

            // Notify dashboard
            if (dataChangeListener != null) {
                dataChangeListener.onInvoiceCreated();
            }

            // Ask to print receipt
            int printChoice = JOptionPane.showConfirmDialog(parentWindow,
                "Thanh toán thành công!\nBạn có muốn in hóa đơn không?",
                "In hóa đơn", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (printChoice == JOptionPane.YES_OPTION) {
                printExchangeReceiptDirectly(generatedInvoiceId, customerPayment);
            }

            // Refresh products
            products.clear();
            products.addAll(busProduct.getAllProducts());

            // Reset form
            resetExchangeForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentWindow, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void printExchangeReceiptDirectly(String invoiceId, BigDecimal customerPayment) {
        String[] printers = ReceiptThermalPrinter.getAvailablePrinters();
        if (printers.length == 0) {
            JOptionPane.showMessageDialog(parentWindow, "Không tìm thấy máy in nào!", "Lỗi máy in", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String selectedPrinter = (String) JOptionPane.showInputDialog(parentWindow, "Chọn máy in:", "Chọn máy in",
            JOptionPane.QUESTION_MESSAGE, null, printers, printers[0]);
        if (selectedPrinter == null) return;
        try {
            ReceiptThermalPrinter.printExchangeReceipt(exchangeInvoice, invoiceId, customerPayment, selectedPrinter);
            JOptionPane.showMessageDialog(parentWindow, "In hóa đơn thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parentWindow, "Lỗi in hóa đơn: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void resetExchangeForm() {
        // Clear exchange table
        mdlExchangeInvoiceLine.setRowCount(0);
        productMap.clear();
        previousUOMMap.clear();
        oldUOMIdMap.clear();
        previousQuantityMap.clear();

        // Clear original invoice table
        mdlOriginalInvoiceLine.setRowCount(0);
        originalMaxQuantityMap.clear();

        // Reset selected invoice
        selectedOriginalInvoice = null;
        exchangeInvoice = null;

        // Reset text fields
        if (txtCustomerName != null) {
            txtCustomerName.setText("Điền tên khách hàng (nếu có)...");
            txtCustomerName.setForeground(AppColors.PLACEHOLDER_TEXT);
        }
        if (txtCustomerPayment != null) txtCustomerPayment.setValue(0L);
        if (txtSubtotal != null) txtSubtotal.setText("");
        if (txtVat != null) txtVat.setText("");
        if (txtTotal != null) txtTotal.setText("");

        // Disable fields
        setFieldsEnabled(false);
    }

    @Override public void mouseClicked(MouseEvent e) {
        if (e.getSource() == productSearchResultsList && productSearchResultsList.getSelectedIndex() != -1) selectProduct(productSearchResultsList.getSelectedIndex());
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

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
        } else if (src == txtProductSearch) {
            new javax.swing.Timer(150, evt -> productSearchWindow.setVisible(false)) {{ setRepeats(false); start(); }};
        } else if (src instanceof JTextField) {
            JTextField t = (JTextField) src;
            if (t.getName() != null && t.getName().startsWith("placeholder_") && t.getText().isEmpty()) { t.setText(t.getToolTipText()); t.setForeground(AppColors.PLACEHOLDER_TEXT); }
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getSource() == txtInvoiceSearch && e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume(); searchAndLoadInvoice();
        } else if (e.getSource() == txtProductSearch) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && barcodeScanningEnabled) { processBarcodeInput(); e.consume(); }
            else if (productSearchWindow.isVisible()) handleSearchNavigation(e, productSearchResultsList, productSearchResultsModel);
        }
    }

    private void handleSearchNavigation(KeyEvent e, JList<String> list, DefaultListModel<String> model) {
        int i = list.getSelectedIndex();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN: list.setSelectedIndex((i + 1) % model.getSize()); list.ensureIndexIsVisible(list.getSelectedIndex()); e.consume(); break;
            case KeyEvent.VK_UP: list.setSelectedIndex((i + model.getSize() - 1) % model.getSize()); list.ensureIndexIsVisible(list.getSelectedIndex()); e.consume(); break;
            case KeyEvent.VK_ENTER: if (i != -1) { selectProduct(i); } e.consume(); break;
            case KeyEvent.VK_ESCAPE: productSearchWindow.setVisible(false); e.consume(); break;
        }
    }

    private void processBarcodeInput() {
        String code = txtProductSearch.getText().trim(); txtProductSearch.setText("");
        if (code.isEmpty()) return;
        Product p = products.stream().filter(pr -> code.equals(pr.getBarcode())).findFirst().orElse(null);
        Toolkit.getDefaultToolkit().beep();
        if (p == null)
            JOptionPane.showMessageDialog(parentWindow, "Không tìm thấy sản phẩm với mã vạch: " + code + "\nBẤM ENTER ĐỂ TẮT THÔNG BÁO!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        else
            addProductToExchangeTable(p);
        SwingUtilities.invokeLater(() -> { if (barcodeScanningEnabled) txtProductSearch.requestFocusInWindow(); });
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void insertUpdate(DocumentEvent e) { handleDocumentChange(e); }
    @Override public void removeUpdate(DocumentEvent e) { handleDocumentChange(e); }
    @Override public void changedUpdate(DocumentEvent e) { handleDocumentChange(e); }

    private void handleDocumentChange(DocumentEvent e) {
        if (txtProductSearch != null && e.getDocument() == txtProductSearch.getDocument()) SwingUtilities.invokeLater(this::performProductSearch);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("tableCellEditor".equals(evt.getPropertyName()) && evt.getOldValue() != null && evt.getNewValue() == null) {
            // Handle exchange invoice table
            if (evt.getSource() == tblExchangeInvoiceLine) {
                int r = tblExchangeInvoiceLine.getEditingRow();
                if (r == -1) r = tblExchangeInvoiceLine.getSelectedRow();
                if (r >= 0) updateExchangeInvoiceLineFromTable(r);
            }
            // Handle original invoice table
            if (evt.getSource() == tblOriginalInvoiceLine) {
                int r = tblOriginalInvoiceLine.getEditingRow();
                if (r == -1) r = tblOriginalInvoiceLine.getSelectedRow();
                if (r >= 0) updateOriginalInvoiceLineFromTable(r);
            }
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getSource() == mdlExchangeInvoiceLine && e.getType() == TableModelEvent.UPDATE && e.getFirstRow() >= 0 && (e.getColumn() == 2 || e.getColumn() == 3)) {
            updateExchangeInvoiceLineFromTable(e.getFirstRow());
        }
        if (e.getSource() == mdlOriginalInvoiceLine && e.getType() == TableModelEvent.UPDATE && e.getFirstRow() >= 0 && e.getColumn() == 3) {
            updateOriginalInvoiceLineFromTable(e.getFirstRow());
        }
    }

    private void updateOriginalInvoiceLineFromTable(int row) {
        if (isUpdatingInvoiceLine || row < 0 || row >= mdlOriginalInvoiceLine.getRowCount()) return;
        if (selectedOriginalInvoice == null) return;

        isUpdatingInvoiceLine = true;
        try {
            int newQuantity = (int) mdlOriginalInvoiceLine.getValueAt(row, 3);

            // Check if all quantities would be zero
            boolean allZero = true;
            for (int i = 0; i < mdlOriginalInvoiceLine.getRowCount(); i++) {
                int qty = (i == row) ? newQuantity : (int) mdlOriginalInvoiceLine.getValueAt(i, 3);
                if (qty > 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                JOptionPane.showMessageDialog(parentWindow,
                    "Phải có ít nhất 1 sản phẩm trong hóa đơn gốc!\nKhông thể đặt tất cả số lượng về 0.",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                // Revert to 1
                mdlOriginalInvoiceLine.setValueAt(1, row, 3);
                return;
            }

            // Find the corresponding invoice line and update
            String productId = (String) mdlOriginalInvoiceLine.getValueAt(row, 0);
            String uomName = (String) mdlOriginalInvoiceLine.getValueAt(row, 2);

            for (InvoiceLine line : selectedOriginalInvoice.getInvoiceLineList()) {
                if (line.getProduct().getId().equals(productId) && line.getUnitOfMeasure().getName().equals(uomName)) {
                    line.setQuantity(newQuantity);
                    // Use the invoice line's unit price for accurate subtotal calculation
                    BigDecimal newSubtotal = line.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity));
                    mdlOriginalInvoiceLine.setValueAt(newSubtotal, row, 5);
                    break;
                }
            }

            // Update exchange invoice reference and recalculate totals
            if (exchangeInvoice != null) {
                exchangeInvoice.setReferencedInvoice(selectedOriginalInvoice);
            }
            updateSubtotalDisplay();
            updateVatDisplay();
            updateTotalDisplay();
        } finally {
            isUpdatingInvoiceLine = false;
        }
    }

    // ================ CELL EDITORS ================

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
                            if (editingRow >= 0 && tblExchangeInvoiceLine != null) {
                                Rectangle cellRect = tblExchangeInvoiceLine.getCellRect(editingRow, 2, true);
                                Point tableScreenLoc = tblExchangeInvoiceLine.getLocationOnScreen();
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
                for (UnitOfMeasure u : p.getUnitOfMeasureSet())
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

    // Quantity spinner for original invoice line (min 0, max = original quantity)
    private class OriginalQuantitySpinnerEditor extends DefaultCellEditor {
        private JSpinner spinner;
        private JSpinner.DefaultEditor editor;

        public OriginalQuantitySpinnerEditor() {
            super(new JTextField());
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
            spinner.setFont(new Font("Arial", Font.PLAIN, 16));
            editor = (JSpinner.DefaultEditor) spinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
            setClickCountToStart(1);
        }

        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            // Get the original max quantity for this row
            int maxQty = originalMaxQuantityMap.getOrDefault(r, 9999);
            int currentValue = (v instanceof Integer) ? (Integer) v : 0;

            // Update spinner model with correct max
            spinner.setModel(new SpinnerNumberModel(currentValue, 0, maxQty, 1));

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
}