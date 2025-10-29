package com.gui;

import com.bus.BUS_Invoice;
import com.bus.BUS_Product;
import com.bus.BUS_Promotion;
import com.entities.*;
import com.enums.InvoiceType;
import com.enums.LineType;
import com.enums.PaymentMethod;
import com.enums.ProductCategory;
import com.utils.AppColors;
import com.utils.InvoicePDFGenerator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TAB_Selling extends JFrame {
    JPanel pnlSelling;

    private static final int LEFT_PANEL_MINIMAL_WIDTH = 750;
    private static final int RIGHT_PANEL_MINIMAL_WIDTH = 530;

    private final BUS_Product busProduct;
    private final BUS_Invoice busInvoice;
    private final BUS_Promotion busPromotion;

    private Invoice invoice;
    private List<String> previousPrescriptionCodes;
    private List<Product> products;
    private final List<Promotion> promotions;

    // Map to store product reference for each row (key: product ID)
    private Map<String, Product> productMap;

    // Map to store previous UOM for each row (key: row index)
    private Map<Integer, String> previousUOMMap;

    // Map to store old UOM ID for tracking changes (key: row index)
    private Map<Integer, String> oldUOMIdMap;

    private DefaultTableModel mdlInvoiceLine;
    private JTable tblInvoiceLine;
    private JScrollPane scrInvoiceLine;
    private JButton btnRemoveItem;
    private JButton btnRemoveAllItems;
    private JButton btnBarcodeScan;
    private JTextField txtSearchInput;
    private JSplitPane splitPane;

    // Barcode scanning state
    private boolean barcodeScanningEnabled = false;

    // Field to hold the customer payment text field for cash button updates
    private JFormattedTextField txtCustomerPayment;

    // Fields for prescription code validation
    private JTextField txtPrescriptionCode;
    private JButton btnProcessPayment;

    // Field for VAT display
    private JTextField txtVat;

    // Fields for promotion search and display
    private JTextField txtPromotionSearch;
    private JTextField txtPromotionDetails;
    private JTextField txtTotal;

    // Field for cash options panel
    private JPanel pnlCashOptions;

    // Prescription code regex pattern: xxxxxyyyyyyy-z
    // 5 chars (facility code) + 7 chars (random alphanumeric) + dash + 1 char (type: N/H/C)
    private static final String PRESCRIPTION_CODE_PATTERN = "^[a-zA-Z0-9]{5}[a-zA-Z0-9]{7}-[NHCnhc]$";

    // Fields for search autocomplete
    private JWindow searchWindow;
    private JList<String> searchResultsList;
    private DefaultListModel<String> searchResultsModel;
    private List<Product> currentSearchResults;

    // Fields for promotion search autocomplete
    private JWindow promotionSearchWindow;
    private JList<String> promotionSearchResultsList;
    private DefaultListModel<String> promotionSearchResultsModel;
    private List<Promotion> currentPromotionSearchResults;

    public TAB_Selling(Staff creator) {
        busProduct = new BUS_Product();
        busInvoice = new BUS_Invoice();
        busPromotion = new BUS_Promotion();

        invoice = new Invoice(InvoiceType.SALES, creator);
        invoice.setPaymentMethod(PaymentMethod.CASH); // default payment method

        products = busProduct.getAllProducts();
        previousPrescriptionCodes = busInvoice.getAllPrescriptionCodes();
        promotions = busPromotion.getAllPromotions().stream()
                .filter(Promotion::getIsActive)
                .toList();
        productMap = new HashMap<>();
        previousUOMMap = new HashMap<>();
        oldUOMIdMap = new HashMap<>();

        $$$setupUI$$$();
        createSplitPane();
    }

    private Box createProductSearchBar() {
        Box boxSearchBarVertical = Box.createVerticalBox();
        boxSearchBarVertical.setOpaque(true);
        boxSearchBarVertical.setBackground(Color.WHITE);

        boxSearchBarVertical.add(Box.createVerticalStrut(5));

        Box boxSearchBarHorizontal = Box.createHorizontalBox();
        boxSearchBarVertical.add(boxSearchBarHorizontal);

        boxSearchBarVertical.add(Box.createVerticalStrut(5));

        boxSearchBarHorizontal.add(Box.createHorizontalStrut(5));

        JLabel lblSearch = new JLabel("Tìm kiếm thuốc:");
        txtSearchInput = new JTextField();

        boxSearchBarHorizontal.add(generateLabelAndTextField(lblSearch, txtSearchInput, "Nhập mã/tên/tên rút gọn của thuốc...", "Nhập mã/tên/tên rút gọn của thuốc", 10));

        boxSearchBarHorizontal.add(Box.createHorizontalStrut(5));

        // Create barcode scan button with custom styling to support toggle state
        btnBarcodeScan = new JButton("");
        btnBarcodeScan.setIcon(new ImageIcon("src/main/resources/icons/png_scanner.png"));
        btnBarcodeScan.setMargin(new Insets(10, 10, 10, 10));
        btnBarcodeScan.setBorderPainted(false);
        btnBarcodeScan.setOpaque(true);
        btnBarcodeScan.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBarcodeScan.addActionListener(e -> toggleBarcodeScanning());

        // Add mouse listener that respects the toggle state
        btnBarcodeScan.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!barcodeScanningEnabled) {
                    btnBarcodeScan.setBackground(AppColors.BACKGROUND);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateBarcodeScanButtonAppearance();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!barcodeScanningEnabled) {
                    btnBarcodeScan.setBackground(AppColors.LIGHT);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updateBarcodeScanButtonAppearance();
            }
        });

        updateBarcodeScanButtonAppearance();

        boxSearchBarHorizontal.add(btnBarcodeScan);

        boxSearchBarHorizontal.add(Box.createHorizontalStrut(5));

        // Setup autocomplete for search
        setupSearchAutocomplete(txtSearchInput);

        // Add focus listener to untoggle barcode scanning when focus is lost to another user input field
        txtSearchInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Only untoggle if focus is permanently lost (not temporary like dialogs)
                if (!e.isTemporary() && barcodeScanningEnabled) {
                    Component opposite = e.getOppositeComponent();
                    // Only untoggle if user is focusing on another input component (not dialogs/buttons)
                    if (opposite instanceof JTextField || opposite instanceof JFormattedTextField) {
                        barcodeScanningEnabled = false;
                        updateBarcodeScanButtonAppearance();
                    }
                }
            }
        });

        return boxSearchBarVertical;
    }

    /**
     * Setup autocomplete functionality for product search
     */
    private void setupSearchAutocomplete(JTextField txtSearchInput) {
        // Initialize search results
        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        searchResultsList.setFont(new Font("Arial", Font.PLAIN, 16));
        searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        currentSearchResults = new ArrayList<>();

        // Create a JWindow for dropdown (more stable than JPopupMenu)
        searchWindow = new JWindow(SwingUtilities.getWindowAncestor(pnlSelling));
        JScrollPane scrollPane = new JScrollPane(searchResultsList);
        searchWindow.add(scrollPane);
        searchWindow.setFocusableWindowState(false);

        // Add document listener to search as user types
        txtSearchInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> performSearch(txtSearchInput));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> performSearch(txtSearchInput));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> performSearch(txtSearchInput));
            }
        });

        // Handle mouse click on list
        searchResultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedIndex = searchResultsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    selectProduct(selectedIndex, txtSearchInput);
                }
            }
        });

        // Handle keyboard navigation
        txtSearchInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Handle Enter key for barcode scanning mode
                if (e.getKeyCode() == KeyEvent.VK_ENTER && barcodeScanningEnabled) {
                    processBarcodeInput();
                    e.consume();
                    return;
                }

                // Normal autocomplete navigation
                if (searchWindow.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        int selectedIndex = searchResultsList.getSelectedIndex();
                        if (selectedIndex < searchResultsModel.getSize() - 1) {
                            searchResultsList.setSelectedIndex(selectedIndex + 1);
                            searchResultsList.ensureIndexIsVisible(selectedIndex + 1);
                        } else if (searchResultsModel.getSize() > 0) {
                            searchResultsList.setSelectedIndex(0);
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        int selectedIndex = searchResultsList.getSelectedIndex();
                        if (selectedIndex > 0) {
                            searchResultsList.setSelectedIndex(selectedIndex - 1);
                            searchResultsList.ensureIndexIsVisible(selectedIndex - 1);
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        int selectedIndex = searchResultsList.getSelectedIndex();
                        if (selectedIndex != -1) {
                            selectProduct(selectedIndex, txtSearchInput);
                            e.consume();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        searchWindow.setVisible(false);
                        e.consume();
                    }
                }
            }
        });

        // Hide search window when focus is lost
        txtSearchInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Delay to allow click on list
                javax.swing.Timer timer = new javax.swing.Timer(150, evt -> searchWindow.setVisible(false));
                timer.setRepeats(false);
                timer.start();
            }
        });
    }

    /**
     * Perform search and update results
     */
    private void performSearch(JTextField txtSearchInput) {
        String searchText = txtSearchInput.getText().trim();

        // Check if it's placeholder text
        if (searchText.isEmpty() ||
                searchText.equals("Nhập mã/tên/tên rút gọn của thuốc...") ||
                txtSearchInput.getForeground().equals(Color.GRAY)) {
            searchWindow.setVisible(false);
            return;
        }

        // Clear previous results
        searchResultsModel.clear();
        currentSearchResults.clear();

        // Search products
        String lowerSearch = searchText.toLowerCase();
        for (Product product : products) {
            boolean matches = false;

            if (product.getId() != null && product.getId().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }
            if (product.getName() != null && product.getName().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }
            if (product.getShortName() != null && product.getShortName().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }

            if (matches) {
                currentSearchResults.add(product);
                String displayText = String.format("%s - %s - %s",
                        product.getId(),
                        product.getName(),
                        product.getShortName() != null ? product.getShortName() : "N/A");
                searchResultsModel.addElement(displayText);
            }
        }

        // Show or hide window based on results
        if (!currentSearchResults.isEmpty()) {
            // Position window below text field
            Point location = txtSearchInput.getLocationOnScreen();
            int width = txtSearchInput.getWidth();
            // Show up to 5 items before scrollbar appears (each item ~25px with font 16)
            int maxVisibleItems = 5;
            int itemHeight = 25;
            int maxHeight = maxVisibleItems * itemHeight;
            int height = Math.min(maxHeight, currentSearchResults.size() * itemHeight);

            searchWindow.setLocation(location.x, location.y + txtSearchInput.getHeight());
            searchWindow.setSize(width, height);
            searchWindow.setVisible(true);
        } else {
            searchWindow.setVisible(false);
        }
    }

    /**
     * Handle product selection from search results
     */
    private void selectProduct(int selectedIndex, JTextField txtSearchInput) {
        if (selectedIndex < 0 || selectedIndex >= currentSearchResults.size()) {
            return;
        }

        Product selectedProduct = currentSearchResults.get(selectedIndex);
        addProductToInvoice(selectedProduct);

        // Clear search field completely
        txtSearchInput.setText("");
        txtSearchInput.setForeground(Color.BLACK);

        // Hide search window
        searchWindow.setVisible(false);
    }

    /**
     * Setup autocomplete functionality for promotion search
     */
    private void setupPromotionSearchAutocomplete(JTextField txtPromotionSearch) {
        // Initialize search results
        promotionSearchResultsModel = new DefaultListModel<>();
        promotionSearchResultsList = new JList<>(promotionSearchResultsModel);
        promotionSearchResultsList.setFont(new Font("Arial", Font.PLAIN, 16));
        promotionSearchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        currentPromotionSearchResults = new ArrayList<>();

        // Create a JWindow for dropdown
        promotionSearchWindow = new JWindow(SwingUtilities.getWindowAncestor(pnlSelling));
        JScrollPane scrollPane = new JScrollPane(promotionSearchResultsList);
        promotionSearchWindow.add(scrollPane);
        promotionSearchWindow.setFocusableWindowState(false);

        // Setup autocomplete with generalized method
        setupAutocomplete(
            txtPromotionSearch,
            promotionSearchWindow,
            promotionSearchResultsList,
            promotionSearchResultsModel,
            "Điền mã hoặc tên khuyến mãi (nếu có)...",
            this::performPromotionSearch,
            this::selectPromotion
        );
    }

    /**
     * Generalized autocomplete setup
     */
    private void setupAutocomplete(
            JTextField textField,
            JWindow window,
            JList<String> resultsList,
            DefaultListModel<String> resultsModel,
            String placeholder,
            Runnable searchAction,
            java.util.function.Consumer<Integer> selectionAction) {

        // Add document listener to search as user types
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(searchAction);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(searchAction);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(searchAction);
            }
        });

        // Handle mouse click on list
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedIndex = resultsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    selectionAction.accept(selectedIndex);
                }
            }
        });

        // Handle keyboard navigation
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (window.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        int selectedIndex = resultsList.getSelectedIndex();
                        if (selectedIndex < resultsModel.getSize() - 1) {
                            resultsList.setSelectedIndex(selectedIndex + 1);
                            resultsList.ensureIndexIsVisible(selectedIndex + 1);
                        } else if (resultsModel.getSize() > 0) {
                            resultsList.setSelectedIndex(0);
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        int selectedIndex = resultsList.getSelectedIndex();
                        if (selectedIndex > 0) {
                            resultsList.setSelectedIndex(selectedIndex - 1);
                            resultsList.ensureIndexIsVisible(selectedIndex - 1);
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        int selectedIndex = resultsList.getSelectedIndex();
                        if (selectedIndex != -1) {
                            selectionAction.accept(selectedIndex);
                            e.consume();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        window.setVisible(false);
                        e.consume();
                    }
                }
            }
        });

        // Hide search window when focus is lost
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Delay to allow click on list
                javax.swing.Timer timer = new javax.swing.Timer(150, evt -> window.setVisible(false));
                timer.setRepeats(false);
                timer.start();
            }
        });
    }

    /**
     * Perform promotion search and update results
     */
    private void performPromotionSearch() {
        String searchText = txtPromotionSearch.getText().trim();

        // Check if it's placeholder text
        if (searchText.isEmpty() ||
                searchText.equals("Điền mã hoặc tên khuyến mãi (nếu có)...") ||
                txtPromotionSearch.getForeground().equals(Color.GRAY)) {
            promotionSearchWindow.setVisible(false);
            return;
        }

        // Clear previous results
        promotionSearchResultsModel.clear();
        currentPromotionSearchResults.clear();

        // Search promotions
        String lowerSearch = searchText.toLowerCase();
        for (Promotion promotion : promotions) {
            boolean matches = false;

            if (promotion.getId() != null && promotion.getId().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }
            if (promotion.getName() != null && promotion.getName().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }

            if (matches) {
                currentPromotionSearchResults.add(promotion);
                String displayText = String.format("%s - %s",
                        promotion.getId(),
                        promotion.getName());
                promotionSearchResultsModel.addElement(displayText);
            }
        }

        // Show or hide window based on results
        if (!currentPromotionSearchResults.isEmpty()) {
            // Position window below text field
            Point location = txtPromotionSearch.getLocationOnScreen();
            int width = txtPromotionSearch.getWidth();
            // Show up to 5 items before scrollbar appears
            int maxVisibleItems = 5;
            int itemHeight = 25;
            int visibleItems = Math.min(currentPromotionSearchResults.size(), maxVisibleItems);
            int height = visibleItems * itemHeight + 4;

            promotionSearchWindow.setLocation(location.x, location.y + txtPromotionSearch.getHeight());
            promotionSearchWindow.setSize(width, height);
            promotionSearchWindow.setVisible(true);

            // Select first item by default
            if (promotionSearchResultsModel.getSize() > 0) {
                promotionSearchResultsList.setSelectedIndex(0);
            }
        } else {
            promotionSearchWindow.setVisible(false);
        }
    }

    /**
     * Handle promotion selection from search results
     */
    private void selectPromotion(int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= currentPromotionSearchResults.size()) {
            return;
        }

        Promotion selectedPromotion = currentPromotionSearchResults.get(selectedIndex);

        // Apply promotion to invoice
        invoice.setPromotion(selectedPromotion);

        // Update promotion display text field (if exists)
        // For now, just show the promotion name in search field
        txtPromotionSearch.setText(selectedPromotion.getId() + " - " + selectedPromotion.getName());
        txtPromotionSearch.setForeground(Color.BLACK);

        // Hide search window
        promotionSearchWindow.setVisible(false);

        // TODO: Update invoice calculations with promotion applied
    }

    /**
     * Add product to invoice line table
     */
    private void addProductToInvoice(Product product) {
        // Check if product is ETC and prescription code is required
        if (product.getCategory() == ProductCategory.ETC) {
            if (!isValidPrescriptionCode()) {
                JOptionPane.showMessageDialog(pnlSelling,
                        "Sản phẩm '" + product.getName() + "' là thuốc ETC (thuốc kê đơn).\n" +
                                "Vui lòng nhập mã đơn thuốc hợp lệ trước khi thêm sản phẩm này.",
                        "Yêu cầu mã đơn thuốc",
                        JOptionPane.WARNING_MESSAGE);

                // Set focus to prescription code field
                txtPrescriptionCode.requestFocusInWindow();
                return;
            }
        }

        // Get base unit of measure
        UnitOfMeasure baseUOM = findUnitOfMeasure(product, product.getBaseUnitOfMeasure());

        // Check if product already exists with the same UOM and line type
        for (int i = 0; i < mdlInvoiceLine.getRowCount(); i++) {
            String existingId = (String) mdlInvoiceLine.getValueAt(i, 0);
            String existingUnit = (String) mdlInvoiceLine.getValueAt(i, 2);

            if (existingId.equals(product.getId()) && existingUnit.equals(product.getBaseUnitOfMeasure())) {
                // Increase quantity
                int currentQty = (int) mdlInvoiceLine.getValueAt(i, 3);
                int newQty = currentQty + 1;
                mdlInvoiceLine.setValueAt(newQty, i, 3);

                // Update total - get unit price and handle both double and formatted string
                Object unitPriceObj = mdlInvoiceLine.getValueAt(i, 4);
                double unitPrice;
                if (unitPriceObj instanceof Double) {
                    unitPrice = (Double) unitPriceObj;
                } else if (unitPriceObj instanceof String) {
                    unitPrice = parseCurrencyValue((String) unitPriceObj);
                } else {
                    unitPrice = 0.0;
                }
                mdlInvoiceLine.setValueAt(newQty * unitPrice, i, 5);

                // Update invoice line in the invoice
                InvoiceLine updatedLine = new InvoiceLine(product, invoice, baseUOM, LineType.SALE, newQty);
                invoice.updateInvoiceLine(product.getId(), baseUOM.getId(), updatedLine);

                // Update VAT display
                updateVatDisplay();

                // Update total display
                updateTotalDisplay();

                // Check prescription code requirement after adding
                validatePrescriptionCodeForInvoice();
                return;
            }
        }

        // Add new row
        String unit = "N/A";
        if (product.getBaseUnitOfMeasure() != null && !product.getBaseUnitOfMeasure().isEmpty()) {
            unit = product.getBaseUnitOfMeasure();
        }

        // Get unit price from oldest lot
        double unitPrice = 0.0;
        Lot oldestLot = product.getOldestLotAvailable();
        if (oldestLot != null) {
            unitPrice = oldestLot.getRawPrice();
        }

        Object[] row = {
                product.getId(),
                product.getName(),
                unit,
                1,
                unitPrice,
                unitPrice
        };

        mdlInvoiceLine.addRow(row);

        // Store product reference in map
        productMap.put(product.getId(), product);

        // Store initial UOM for this row
        int newRow = mdlInvoiceLine.getRowCount() - 1;
        previousUOMMap.put(newRow, unit);
        oldUOMIdMap.put(newRow, baseUOM.getId());

        // Create and add invoice line to invoice
        InvoiceLine invoiceLine = new InvoiceLine(product, invoice, baseUOM, LineType.SALE, 1);
        invoice.addInvoiceLine(invoiceLine);

        // Update VAT display
        updateVatDisplay();

        // Update total display
        updateTotalDisplay();

        // Check prescription code requirement after adding
        validatePrescriptionCodeForInvoice();
    }

    /**
     * Find UnitOfMeasure from product by name
     */
    private UnitOfMeasure findUnitOfMeasure(Product product, String uomName) {
        if (product.getUnitOfMeasureList() != null) {
            for (UnitOfMeasure uom : product.getUnitOfMeasureList()) {
                if (uom.getName().equals(uomName)) {
                    return uom;
                }
            }
        }
        // If not found in list, create a base UOM
        return new UnitOfMeasure(product.getId() + "-BASE", product, uomName, 1.0);
    }

    /**
     * Check if prescription code is valid according to the pattern
     */
    private boolean isValidPrescriptionCode() {
        String code = txtPrescriptionCode.getText().trim();

        // Check if it's placeholder text or empty
        if (code.isEmpty() ||
                code.equals("Điền mã đơn kê thuốc (nếu có)...") ||
                txtPrescriptionCode.getForeground().equals(Color.GRAY)) {
            return false;
        }

        // Check against regex pattern
        return code.matches(PRESCRIPTION_CODE_PATTERN);
    }

    /**
     * Validate prescription code format when field loses focus
     */
    private void validatePrescriptionCode() {
        String code = txtPrescriptionCode.getText().trim();

        // Check if empty or placeholder
        boolean isEmpty = code.isEmpty() ||
                code.equals("Điền mã đơn kê thuốc (nếu có)...") ||
                txtPrescriptionCode.getForeground().equals(Color.GRAY);

        if (isEmpty) {
            // Clear prescription code in invoice
            invoice.setPrescriptionCode(null);

            // Check if ETC products exist in invoice
            boolean hasETCProduct = false;
            for (int i = 0; i < mdlInvoiceLine.getRowCount(); i++) {
                String productId = (String) mdlInvoiceLine.getValueAt(i, 0);
                Product product = productMap.get(productId);

                if (product != null && product.getCategory() == ProductCategory.ETC) {
                    hasETCProduct = true;
                    break;
                }
            }

            // If ETC products exist, show warning
            if (hasETCProduct) {
                JOptionPane.showMessageDialog(pnlSelling,
                        "Hóa đơn có chứa thuốc ETC (thuốc kê đơn).\n" +
                                "Vui lòng nhập mã đơn thuốc hợp lệ để tiếp tục.",
                        "Yêu cầu mã đơn thuốc",
                        JOptionPane.WARNING_MESSAGE);

                // Set focus back to the field
                txtPrescriptionCode.requestFocusInWindow();
            }

            validatePrescriptionCodeForInvoice();
            return;
        }

        // Validate format
        if (!code.matches(PRESCRIPTION_CODE_PATTERN)) {
            JOptionPane.showMessageDialog(pnlSelling,
                    "Mã đơn thuốc không hợp lệ!\n\n" +
                            "Định dạng đúng: xxxxxyyyyyyy-z\n" +
                            "- 5 ký tự đầu: Mã cơ sở khám bệnh (chữ/số)\n" +
                            "- 7 ký tự tiếp: Mã đơn thuốc (chữ thường/số)\n" +
                            "- 1 ký tự cuối sau dấu gạch ngang: Loại đơn (N/H/C)\n\n" +
                            "Ví dụ: MW001a3b5c7d-C",
                    "Lỗi định dạng mã đơn thuốc",
                    JOptionPane.WARNING_MESSAGE);

            // Set focus back to the field
            txtPrescriptionCode.requestFocusInWindow();
            btnProcessPayment.setEnabled(false);
            // Clear prescription code in invoice
            invoice.setPrescriptionCode(null);
            return;
        }

        // Check if prescription code has already been used
        if (previousPrescriptionCodes != null && previousPrescriptionCodes.contains(code.toLowerCase())) {
            JOptionPane.showMessageDialog(pnlSelling,
                    "Mã đơn thuốc '" + code + "' đã được sử dụng trước đó!\n" +
                            "Vui lòng nhập mã đơn thuốc khác.",
                    "Mã đơn thuốc đã tồn tại",
                    JOptionPane.WARNING_MESSAGE);

            // Select all text and set focus
            txtPrescriptionCode.selectAll();
            txtPrescriptionCode.requestFocusInWindow();
            btnProcessPayment.setEnabled(false);
            // Clear prescription code in invoice
            invoice.setPrescriptionCode(null);
            return;
        }

        // Valid prescription code - update invoice
        invoice.setPrescriptionCode(code);
        validatePrescriptionCodeForInvoice();
    }

    /**
     * Check if prescription code is required based on invoice contents
     */
    private void validatePrescriptionCodeForInvoice() {
        boolean hasETCProduct = false;

        // Check if any product in the invoice is ETC
        for (int i = 0; i < mdlInvoiceLine.getRowCount(); i++) {
            String productId = (String) mdlInvoiceLine.getValueAt(i, 0);
            Product product = productMap.get(productId);

            if (product != null && product.getCategory() == ProductCategory.ETC) {
                hasETCProduct = true;
                break;
            }
        }

        // If ETC products exist, prescription code is required
        if (hasETCProduct) {
            if (!isValidPrescriptionCode()) {
                btnProcessPayment.setEnabled(false);
            } else {
                btnProcessPayment.setEnabled(true);
            }
        } else {
            // No ETC products, payment button can be enabled
            btnProcessPayment.setEnabled(true);
        }
    }

    /**
     * Update invoice line from table data
     */
    private void updateInvoiceLineFromTable(int row) {
        if (row < 0 || row >= mdlInvoiceLine.getRowCount()) {
            return;
        }

        // Get product information
        String productId = (String) mdlInvoiceLine.getValueAt(row, 0);
        Product product = productMap.get(productId);

        if (product != null) {
            String uomName = (String) mdlInvoiceLine.getValueAt(row, 2);
            int quantity = (int) mdlInvoiceLine.getValueAt(row, 3);

            // Check for duplicate product with same UOM (excluding current row)
            for (int i = 0; i < mdlInvoiceLine.getRowCount(); i++) {
                if (i != row) {
                    String otherProductId = (String) mdlInvoiceLine.getValueAt(i, 0);
                    String otherUomName = (String) mdlInvoiceLine.getValueAt(i, 2);

                    if (productId.equals(otherProductId) && uomName.equals(otherUomName)) {
                        // Duplicate found - show warning and revert
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(pnlSelling,
                                        "Sản phẩm '" + product.getName() + "' với đơn vị '" + uomName + "' đã tồn tại trong hóa đơn!\n" +
                                                "Vui lòng tăng số lượng của sản phẩm hiện có hoặc chọn đơn vị khác.",
                                        "Cảnh báo trùng lặp",
                                        JOptionPane.WARNING_MESSAGE)
                        );

                        // Revert to previous UOM (before the change)
                        String previousUOM = previousUOMMap.get(row);
                        if (previousUOM != null) {
                            mdlInvoiceLine.setValueAt(previousUOM, row, 2);
                        } else {
                            // Fallback to base UOM if no previous value stored
                            String fallbackUOM = product.getBaseUnitOfMeasure();
                            if (fallbackUOM != null && !fallbackUOM.isEmpty()) {
                                mdlInvoiceLine.setValueAt(fallbackUOM, row, 2);
                            }
                        }
                        return;
                    }
                }
            }

            // Find the UnitOfMeasure
            UnitOfMeasure uom = findUnitOfMeasure(product, uomName);

            // Calculate unit price based on UOM
            double unitPrice = 0.0;
            Lot oldestLot = product.getOldestLotAvailable();
            if (oldestLot != null) {
                unitPrice = oldestLot.getRawPrice();
                if (uom != null) {
                    unitPrice *= uom.getBasePriceConversionRate();
                }
            }

            // Update unit price and total in table
            mdlInvoiceLine.setValueAt(unitPrice, row, 4);
            mdlInvoiceLine.setValueAt(unitPrice * quantity, row, 5);

            // Update invoice line in invoice using old UOM ID
            String oldUomId = oldUOMIdMap.get(row);
            if (oldUomId == null) {
                oldUomId = uom.getId(); // First time update
            }

            InvoiceLine updatedLine = new InvoiceLine(product, invoice, uom, LineType.SALE, quantity);
            invoice.updateInvoiceLine(productId, oldUomId, updatedLine);

            // Update the stored old UOM ID for next time
            oldUOMIdMap.put(row, uom.getId());
            previousUOMMap.put(row, uomName);

            // Update VAT display
            updateVatDisplay();

            // Update total display
            updateTotalDisplay();
        }
    }

    /**
     * Utility method to add placeholder text to a JTextField
     *
     * @param textField   The text field to add placeholder to
     * @param placeholder The placeholder text to display
     */
    private void setPlaceholderAndTooltip(JTextField textField, String placeholder, String tooltip) {
        textField.setText(placeholder);
        textField.setForeground(Color.GRAY);

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });

        textField.setToolTipText(tooltip);
    }

    private void createSplitPane() {
        // Create left panel with invoice line table
        JPanel pnlLeft = new JPanel(new BorderLayout());
        pnlLeft.setBackground(Color.WHITE);
        pnlLeft.setMinimumSize(new Dimension(LEFT_PANEL_MINIMAL_WIDTH, 0));

        // Add search bar at the top of left panel
        pnlLeft.add(createProductSearchBar(), BorderLayout.NORTH);

        JPanel pnlTableContainer = new JPanel(new BorderLayout());
        pnlTableContainer.setBackground(Color.WHITE);
        pnlLeft.add(pnlTableContainer, BorderLayout.CENTER);

        Box boxInvoiceHorizontal = Box.createHorizontalBox();

        boxInvoiceHorizontal.add(Box.createHorizontalStrut(10));

        Box boxInvoiceVertical = Box.createVerticalBox();
        boxInvoiceHorizontal.add(boxInvoiceVertical);

        boxInvoiceHorizontal.add(Box.createHorizontalStrut(10));

        // Adding InvoiceLineTable Title
        Box boxInvoiceLineTableTile = Box.createHorizontalBox();
        JLabel lblInvoiceLineTableTitle = new JLabel("CHI TIẾT HÓA ĐƠN BÁN HÀNG");
        lblInvoiceLineTableTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblInvoiceLineTableTitle.setForeground(AppColors.DARK);

        boxInvoiceLineTableTile.add(Box.createHorizontalGlue());

        boxInvoiceLineTableTile.add(lblInvoiceLineTableTitle);

        boxInvoiceLineTableTile.add(Box.createHorizontalGlue());

        boxInvoiceVertical.add(Box.createVerticalStrut(20));

        boxInvoiceVertical.add(boxInvoiceLineTableTile);

        boxInvoiceVertical.add(Box.createVerticalStrut(20));

        pnlTableContainer.add(boxInvoiceHorizontal, BorderLayout.NORTH);

        createInvoiceLineTable();
        pnlTableContainer.add(scrInvoiceLine, BorderLayout.CENTER);

        pnlLeft.add(createInvoiceLineTableButtons(), BorderLayout.SOUTH);

        // Create right panel for invoice
        JPanel pnlRight = new JPanel(new BorderLayout());
        pnlRight.setBackground(AppColors.WHITE);
        pnlRight.setMinimumSize(new Dimension(RIGHT_PANEL_MINIMAL_WIDTH, 0));
        pnlRight.add(createInvoice(), BorderLayout.NORTH);

        // Create split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlLeft, pnlRight);

        pnlSelling.add(splitPane, BorderLayout.CENTER);
    }

    private void createInvoiceLineTable() {
        String[] columnHeaders = {"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"};

        mdlInvoiceLine = new DefaultTableModel(columnHeaders, 0) {
            // Make only "Đơn vị" (column 2) and "Số lượng" (column 3) editable
            @Override
            public boolean isCellEditable(int row, int column) {
                return (column == 2 || column == 3);
            }
        };

        tblInvoiceLine = new JTable(mdlInvoiceLine);
        tblInvoiceLine.setFont(new Font("Arial", Font.PLAIN, 16));
        tblInvoiceLine.getTableHeader().setReorderingAllowed(false);
        tblInvoiceLine.setRowHeight(35); // Increase row height to accommodate spinner

        tblInvoiceLine.getTableHeader().setBackground(AppColors.PRIMARY);
        tblInvoiceLine.getTableHeader().setForeground(Color.WHITE);
        tblInvoiceLine.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        // Set custom cell editor for "Đơn vị" column (column index 2)
        tblInvoiceLine.getColumnModel().getColumn(2).setCellEditor(new UnitOfMeasureCellEditor());

        // Set custom cell editor and renderer for "Số lượng" column (column index 3)
        tblInvoiceLine.getColumnModel().getColumn(3).setCellEditor(new QuantitySpinnerEditor());
        tblInvoiceLine.getColumnModel().getColumn(3).setCellRenderer(new QuantitySpinnerRenderer());

        // Set right-aligned renderer for columns 3-5 (Quantity, Unit Price, Total)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRenderer.setFont(new Font("Arial", Font.PLAIN, 16));
        tblInvoiceLine.getColumnModel().getColumn(3).setCellRenderer(new QuantitySpinnerRenderer()); // Keep spinner renderer for column 3

        // Set currency renderer for columns 4-5 (Unit Price, Total)
        tblInvoiceLine.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());
        tblInvoiceLine.getColumnModel().getColumn(5).setCellRenderer(new CurrencyRenderer());

        // Add focus listener to stop editing when table loses focus
        tblInvoiceLine.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Don't stop editing if focus moved to a child component (like dropdown or spinner)
                Component oppositeComponent = e.getOppositeComponent();
                if (oppositeComponent != null) {
                    // Check if the opposite component is part of the cell editor
                    Container parent = oppositeComponent.getParent();
                    while (parent != null) {
                        if (parent == tblInvoiceLine) {
                            // Focus moved to a component within the table, don't stop editing
                            return;
                        }
                        parent = parent.getParent();
                    }
                }

                // Stop cell editing when table loses focus to external component
                if (tblInvoiceLine.isEditing()) {
                    int editingRow = tblInvoiceLine.getEditingRow();
                    int editingColumn = tblInvoiceLine.getEditingColumn();

                    // Stop the cell editor
                    if (tblInvoiceLine.getCellEditor() != null) {
                        tblInvoiceLine.getCellEditor().stopCellEditing();
                    }

                    // Update the invoice line for the edited row
                    if (editingRow >= 0 && (editingColumn == 2 || editingColumn == 3)) {
                        updateInvoiceLineFromTable(editingRow);
                    }
                }
            }
        });

        // Add property change listener to handle cell editing stop
        tblInvoiceLine.addPropertyChangeListener("tableCellEditor", evt -> {
            // When editing stops, update the invoice line
            if (evt.getOldValue() != null && evt.getNewValue() == null) {
                // Editing has stopped
                int row = tblInvoiceLine.getEditingRow();
                if (row == -1) {
                    row = tblInvoiceLine.getSelectedRow();
                }
                if (row >= 0) {
                    updateInvoiceLineFromTable(row);
                }
            }
        });

        // Add table model listener to handle changes
        mdlInvoiceLine.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();

                if (row >= 0 && (column == 2 || column == 3)) {
                    updateInvoiceLineFromTable(row);
                }
            }
        });

        scrInvoiceLine = new JScrollPane(tblInvoiceLine);
    }

    private JPanel createInvoiceLineTableButtons() {
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.setBackground(Color.WHITE);

        btnRemoveAllItems = createStyledButton("Xóa tất cả");
        btnRemoveAllItems.addActionListener(e -> removeAllItems());
        pnlButtons.add(btnRemoveAllItems);

        btnRemoveItem = createStyledButton("Xóa sản phẩm");
        btnRemoveItem.addActionListener(e -> removeSelectedItems());
        pnlButtons.add(btnRemoveItem);

        return pnlButtons;
    }

    /**
     * Toggle barcode scanning mode on/off
     */
    private void toggleBarcodeScanning() {
        barcodeScanningEnabled = !barcodeScanningEnabled;
        updateBarcodeScanButtonAppearance();

        if (barcodeScanningEnabled) {
            // Clear any previous input and set focus on search text field
            txtSearchInput.setText("");
            txtSearchInput.requestFocusInWindow();

            JOptionPane.showMessageDialog(pnlSelling,
                "Chế độ quét mã vạch đã BẬT!\n" +
                "Sử dụng máy quét để thêm sản phẩm vào hóa đơn.",
                "Quét mã vạch",
                JOptionPane.INFORMATION_MESSAGE);

            // Restore focus after dialog
            SwingUtilities.invokeLater(() -> txtSearchInput.requestFocusInWindow());
        } else {
            JOptionPane.showMessageDialog(pnlSelling,
                "Chế độ quét mã vạch đã TẮT!",
                "Quét mã vạch",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Update barcode scan button appearance based on state
     */
    private void updateBarcodeScanButtonAppearance() {
        if (barcodeScanningEnabled) {
            // ON state - Green background
            btnBarcodeScan.setBackground(new Color(46, 204, 113)); // Green
        } else {
            // OFF state - White background (default style)
            btnBarcodeScan.setBackground(Color.WHITE);
        }
    }

    /**
     * Process barcode input when scanned (triggered by Enter key)
     */
    private void processBarcodeInput() {
        String barcode = txtSearchInput.getText().trim();

        // Clear the input field immediately
        txtSearchInput.setText("");

        if (barcode.isEmpty()) {
            return;
        }

        // Find product by barcode
        Product foundProduct = null;
        for (Product product : products) {
            if (product.getBarcode() != null && product.getBarcode().equals(barcode)) {
                foundProduct = product;
                break;
            }
        }

        if (foundProduct == null) {
            // Product not found - play beep and show brief message
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(pnlSelling,
                "Không tìm thấy sản phẩm với mã vạch: " + barcode,
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);

            // Restore focus to search field
            SwingUtilities.invokeLater(() -> {
                if (barcodeScanningEnabled) {
                    txtSearchInput.requestFocusInWindow();
                }
            });
            return;
        }

        // Product found - add to invoice
        addProductToInvoice(foundProduct);

        // Play success beep
        Toolkit.getDefaultToolkit().beep();

        // Restore focus to search field for next scan
        SwingUtilities.invokeLater(() -> {
            if (barcodeScanningEnabled) {
                txtSearchInput.requestFocusInWindow();
            }
        });
    }

    /**
     * Remove selected items from the table and invoice
     */
    private void removeSelectedItems() {
        int[] selectedRows = tblInvoiceLine.getSelectedRows();

        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(pnlSelling,
                    "Vui lòng chọn sản phẩm cần xóa!",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(pnlSelling,
                "Bạn có chắc chắn muốn xóa " + selectedRows.length + " sản phẩm đã chọn?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Remove from invoice and table (iterate backwards to avoid index issues)
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int row = selectedRows[i];

            // Get product information
            String productId = (String) mdlInvoiceLine.getValueAt(row, 0);
            String uomName = (String) mdlInvoiceLine.getValueAt(row, 2);

            Product product = productMap.get(productId);
            if (product != null) {
                UnitOfMeasure uom = findUnitOfMeasure(product, uomName);

                // Remove from invoice using productId and uomId
                invoice.removeInvoiceLine(productId, uom.getId());
            }

            // Remove from table
            mdlInvoiceLine.removeRow(row);

            // Clean up maps - shift entries after removed row
            Map<Integer, String> updatedPreviousUOMMap = new HashMap<>();
            Map<Integer, String> updatedOldUOMIdMap = new HashMap<>();

            for (Map.Entry<Integer, String> entry : previousUOMMap.entrySet()) {
                int rowIndex = entry.getKey();
                if (rowIndex < row) {
                    updatedPreviousUOMMap.put(rowIndex, entry.getValue());
                } else if (rowIndex > row) {
                    updatedPreviousUOMMap.put(rowIndex - 1, entry.getValue());
                }
            }

            for (Map.Entry<Integer, String> entry : oldUOMIdMap.entrySet()) {
                int rowIndex = entry.getKey();
                if (rowIndex < row) {
                    updatedOldUOMIdMap.put(rowIndex, entry.getValue());
                } else if (rowIndex > row) {
                    updatedOldUOMIdMap.put(rowIndex - 1, entry.getValue());
                }
            }

            previousUOMMap = updatedPreviousUOMMap;
            oldUOMIdMap = updatedOldUOMIdMap;
        }

        // Update VAT display
        updateVatDisplay();

        // Update total display
        updateTotalDisplay();

        // Check prescription code requirement after removal
        validatePrescriptionCodeForInvoice();
    }

    /**
     * Remove all items from the table and invoice
     */
    private void removeAllItems() {
        if (mdlInvoiceLine.getRowCount() == 0) {
            JOptionPane.showMessageDialog(pnlSelling,
                    "Không có sản phẩm nào để xóa!",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(pnlSelling,
                "Bạn có chắc chắn muốn xóa tất cả sản phẩm?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Remove all invoice lines from invoice
        for (int i = mdlInvoiceLine.getRowCount() - 1; i >= 0; i--) {
            // Get product information
            String productId = (String) mdlInvoiceLine.getValueAt(i, 0);
            String uomName = (String) mdlInvoiceLine.getValueAt(i, 2);

            Product product = productMap.get(productId);
            if (product != null) {
                UnitOfMeasure uom = findUnitOfMeasure(product, uomName);

                // Remove from invoice using productId and uomId
                invoice.removeInvoiceLine(productId, uom.getId());
            }
        }

        // Clear table
        mdlInvoiceLine.setRowCount(0);

        // Clear all maps
        previousUOMMap.clear();
        oldUOMIdMap.clear();

        // Update VAT display
        updateVatDisplay();

        // Update total display
        updateTotalDisplay();

        // Check prescription code requirement after removal
        validatePrescriptionCodeForInvoice();
    }

    /**
     * Creates a styled button with rounded corners and hover effects
     *
     * @param text The text to display on the button
     * @return A configured JButton with styling and mouse listeners
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setToolTipText(text);
        button.setMargin(new Insets(10, 10, 10, 10));
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(new Color(11, 110, 217));
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        if (text.equalsIgnoreCase("Thanh toán"))
            button.setBackground(AppColors.WHITE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add mouse listener for hover and click effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(AppColors.WHITE);

                if (text.equalsIgnoreCase("Thanh toán"))
                    button.setBackground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);

                if (text.equalsIgnoreCase("Thanh toán"))
                    button.setBackground(AppColors.WHITE);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(AppColors.LIGHT);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.contains(e.getPoint())) {
                    button.setBackground(AppColors.WHITE);

                    if (text.equalsIgnoreCase("Thanh toán"))
                        button.setBackground(Color.WHITE);
                } else {
                    button.setBackground(Color.WHITE);

                    if (text.equalsIgnoreCase("Thanh toán"))
                        button.setBackground(AppColors.WHITE);
                }
            }
        });

        return button;
    }

    private Box createInvoice() {
        Box boxInvoiceHorizontal = Box.createHorizontalBox();

        boxInvoiceHorizontal.add(Box.createHorizontalStrut(10));

        Box boxInvoiceVertical = Box.createVerticalBox();
        boxInvoiceHorizontal.add(boxInvoiceVertical);

        boxInvoiceHorizontal.add(Box.createHorizontalStrut(10));

        // Adding Invoice Title
        Box boxInvoiceTitle = Box.createHorizontalBox();
        JLabel lblInvoiceTitle = new JLabel("HÓA ĐƠN BÁN HÀNG");
        lblInvoiceTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblInvoiceTitle.setForeground(AppColors.DARK);

        boxInvoiceTitle.add(Box.createHorizontalGlue());

        boxInvoiceTitle.add(lblInvoiceTitle);

        boxInvoiceTitle.add(Box.createHorizontalGlue());

        boxInvoiceVertical.add(Box.createVerticalStrut(57));

        boxInvoiceVertical.add(boxInvoiceTitle);

        boxInvoiceVertical.add(Box.createVerticalStrut(15));


        // PRESCRIPTION DETAILS SECTION


        Box boxPrescriptionDetailsHorizontal = Box.createHorizontalBox();
        TitledBorder prescriptionBorder = BorderFactory.createTitledBorder("Thông tin kê đơn thuốc");
        prescriptionBorder.setTitleFont(new Font("Arial", Font.BOLD, 16));
        prescriptionBorder.setTitleColor(AppColors.PRIMARY);
        boxPrescriptionDetailsHorizontal.setBorder(prescriptionBorder);

        boxInvoiceVertical.add(boxPrescriptionDetailsHorizontal);

        boxInvoiceVertical.add(Box.createVerticalStrut(40));

        Box boxPrescriptionDetailsVertical = Box.createVerticalBox();
        boxPrescriptionDetailsHorizontal.add(boxPrescriptionDetailsVertical);

        // Adding prescription code label and text field
        JLabel lblPrescriptionCode = new JLabel("Mã đơn kê thuốc:");
        txtPrescriptionCode = new JTextField();

        // Add focus listener for prescription code validation
        txtPrescriptionCode.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validatePrescriptionCode();
            }
        });

        boxPrescriptionDetailsVertical.add(generateLabelAndTextField(lblPrescriptionCode, txtPrescriptionCode, "Điền mã đơn kê thuốc (nếu có)...", "Điền mã đơn kê thuốc", 69));

        boxPrescriptionDetailsVertical.add(Box.createVerticalStrut(10));


        // PAYMENT DETAILS SECTION


        Box boxPaymentHorizontal = Box.createHorizontalBox();
        TitledBorder paymentBorder = BorderFactory.createTitledBorder("Thông tin thanh toán");
        paymentBorder.setTitleFont(new Font("Arial", Font.BOLD, 16));
        paymentBorder.setTitleColor(AppColors.PRIMARY);
        boxPaymentHorizontal.setBorder(paymentBorder);

        boxInvoiceVertical.add(boxPaymentHorizontal);

        Box boxPaymentVertical = Box.createVerticalBox();
        boxPaymentHorizontal.add(boxPaymentVertical);

        // Adding promotion search label and text field
        JLabel lblPromotionSearch = new JLabel("Tìm kiếm khuyến mãi:");
        txtPromotionSearch = new JTextField();

        boxPaymentVertical.add(generateLabelAndTextField(lblPromotionSearch, txtPromotionSearch, "Điền mã hoặc tên khuyến mãi (nếu có)...", "Điền mã hoặc tên khuyến mãi", 52));

        // Setup autocomplete for promotion search
        setupPromotionSearchAutocomplete(txtPromotionSearch);

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        // Adding promotion details label and text field
        JLabel lblPromotionDetails = new JLabel("Nội dung khuyến mãi:");
        JTextField txtPromotionDetails = new JTextField();
        txtPromotionDetails.setEditable(false);
        txtPromotionDetails.setFocusable(false);

        boxPaymentVertical.add(generateLabelAndTextField(lblPromotionDetails, txtPromotionDetails, "", "Nội dung khuyến mãi", 53));

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        // Adding Vat label and text field
        JLabel lblVat = new JLabel("Vat:");
        txtVat = new JTextField();
        txtVat.setEditable(false);
        txtVat.setFocusable(false);

        boxPaymentVertical.add(generateLabelAndTextField(lblVat, txtVat, "", "Thuế hóa đơn", 173));

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        // Adding discount amount label and text field
        JLabel lblDiscountAmount = new JLabel("Tiền giảm giá (khuyến mãi):");
        JTextField txtDiscountAmount = new JTextField();
        txtDiscountAmount.setEditable(false);
        txtDiscountAmount.setFocusable(false);

        boxPaymentVertical.add(generateLabelAndTextField(lblDiscountAmount, txtDiscountAmount, "", "Tiền giảm giá", 10));

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        // Adding total label and text field
        JLabel lblTotal = new JLabel("Tổng tiền:");
        txtTotal = new JTextField();
        txtTotal.setEditable(false);
        txtTotal.setFocusable(false);

        boxPaymentVertical.add(generateLabelAndTextField(lblTotal, txtTotal, "", "Tổng tiền", 133));

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        // Create formatted text field for Vietnamese currency
        DecimalFormat currencyFormat = createCurrencyFormat();

        NumberFormatter formatter = new NumberFormatter(currencyFormat);
        formatter.setValueClass(Long.class);
        formatter.setMinimum(0L);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);

        // Wrap the formatter in a DefaultFormatterFactory
        DefaultFormatterFactory formatterFactory = new DefaultFormatterFactory(formatter);

        // Adding customer payment label and text field
        JLabel lblCustomerPayment = new JLabel("Tiền khách đưa:");
        txtCustomerPayment = new JFormattedTextField(formatter);

        boxPaymentVertical.add(generateLabelAndTextField(lblCustomerPayment, txtCustomerPayment, "Nhập hoặc chọn số tiền khách đưa...", "Nhập hoặc chọn số tiền khách đưa", 89));

        txtCustomerPayment.setValue(0L);

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        // Adding customer payment label and text field
        Box boxPaymentMethod = Box.createHorizontalBox();
        boxPaymentVertical.add(boxPaymentMethod);

        JLabel lblPaymentMethod = new JLabel("Phương thức thanh toán:");
        lblPaymentMethod.setFont(new Font("Arial", Font.PLAIN, 16));

        boxPaymentMethod.add(lblPaymentMethod);

        boxPaymentMethod.add(Box.createHorizontalStrut(29));

        ButtonGroup paymentMethodGroup = new ButtonGroup();

        JRadioButton rdoCash = new JRadioButton("Tiền mặt");
        rdoCash.setFont(new Font("Arial", Font.PLAIN, 16));
        rdoCash.setSelected(true);

        boxPaymentMethod.add(rdoCash);

        boxPaymentMethod.add(Box.createHorizontalStrut(10));

        JRadioButton rdoBankOrDigitalWallet = new JRadioButton("Ngân hàng/Ví điện tử");
        rdoBankOrDigitalWallet.setFont(new Font("Arial", Font.PLAIN, 16));

        paymentMethodGroup.add(rdoCash);
        paymentMethodGroup.add(rdoBankOrDigitalWallet);

        boxPaymentMethod.add(rdoBankOrDigitalWallet);

        boxPaymentMethod.add(Box.createHorizontalGlue());

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        Box boxCashOptions = Box.createHorizontalBox();
        boxPaymentVertical.add(boxCashOptions);

        boxCashOptions.add(Box.createHorizontalStrut(203));

        pnlCashOptions = createCashOptionsPanel();
        boxCashOptions.add(pnlCashOptions);

        // Add action listeners to show/hide cash options based on payment method
        rdoCash.addActionListener(e -> {
            boxCashOptions.setVisible(true);
            boxPaymentVertical.revalidate();
            boxPaymentVertical.repaint();

            // Enable txtCustomerPayment
            if (txtCustomerPayment != null) {
                txtCustomerPayment.setEnabled(true);
                txtCustomerPayment.setEditable(true);
            }

            // Update payment method in invoice
            if (invoice != null) {
                invoice.setPaymentMethod(PaymentMethod.CASH);
            }
        });

        rdoBankOrDigitalWallet.addActionListener(e -> {
            boxCashOptions.setVisible(false);
            boxPaymentVertical.revalidate();
            boxPaymentVertical.repaint();

            // Disable txtCustomerPayment and set value to total
            if (txtCustomerPayment != null && txtTotal != null && invoice != null) {
                txtCustomerPayment.setEnabled(false);
                txtCustomerPayment.setEditable(false);

                // Set value to total amount
                long totalAmount = (long) invoice.calculateTotal();
                txtCustomerPayment.setValue(totalAmount);
            }

            // Update payment method in invoice
            if (invoice != null) {
                invoice.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            }
        });

        boxInvoiceVertical.add(Box.createVerticalStrut(20));

        // PAYMENT BUTTON SECTION

        Box boxPaymentButton = Box.createHorizontalBox();
        boxInvoiceVertical.add(boxPaymentButton);

        boxPaymentButton.add(Box.createHorizontalGlue());

        btnProcessPayment = createStyledButton("Thanh toán");
        btnProcessPayment.setEnabled(true); // Initially enabled

        // Add action listener to validate and process payment
        btnProcessPayment.addActionListener(e -> {
            // Validate invoice line list is not empty
            if (invoice.getInvoiceLineList() == null || invoice.getInvoiceLineList().isEmpty()) {
                JOptionPane.showMessageDialog(pnlSelling,
                    "Danh sách sản phẩm trống!\n" +
                    "Vui lòng thêm sản phẩm vào hóa đơn trước khi thanh toán.",
                    "Không thể thanh toán",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Validate customer payment is >= total (only for CASH payment)
            if (invoice.getPaymentMethod() == PaymentMethod.CASH && txtCustomerPayment != null) {
                Object paymentValue = txtCustomerPayment.getValue();
                long customerPayment = 0;
                if (paymentValue instanceof Number) {
                    customerPayment = ((Number) paymentValue).longValue();
                }

                long total = (long) invoice.calculateTotal();

                if (customerPayment < total) {
                    JOptionPane.showMessageDialog(pnlSelling,
                        "Số tiền khách đưa không đủ!\n" +
                        "Tổng tiền: " + String.format("%,d", total).replace(',', '.') + " Đ\n" +
                        "Khách đưa: " + String.format("%,d", customerPayment).replace(',', '.') + " Đ",
                        "Không thể thanh toán",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Generate PDF invoice
            try {
                // Create invoices directory if it doesn't exist
                File invoicesDir = new File("invoices");
                if (!invoicesDir.exists()) {
                    invoicesDir.mkdirs();
                }

                // Generate filename with timestamp
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String timestamp = dateFormat.format(new Date());
                String filename = "invoices/Invoice_" + timestamp + ".pdf";

                // Generate PDF
                File pdfFile = InvoicePDFGenerator.generateInvoicePDF(invoice, filename);

                // Show success dialog with option to open PDF
                int option = JOptionPane.showConfirmDialog(pnlSelling,
                    "Thanh toán thành công!\n" +
                    "Hóa đơn đã được lưu tại: " + pdfFile.getAbsolutePath() + "\n\n" +
                    "Bạn có muốn mở hóa đơn không?",
                    "Thành công",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

                // Open PDF if user chooses Yes
                if (option == JOptionPane.YES_OPTION) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(pdfFile);
                    }
                }

                // TODO: Save invoice to database here
                // busInvoice.addInvoice(invoice);

                System.out.println("========== INVOICE PDF GENERATED ==========");
                System.out.println("File: " + pdfFile.getAbsolutePath());
                System.out.println("Invoice ID: " + invoice.getId());
                System.out.println("Total: " + invoice.calculateTotal());
                System.out.println("==========================================");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(pnlSelling,
                    "Lỗi khi tạo hóa đơn PDF:\n" + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        boxPaymentButton.add(btnProcessPayment);

        return boxInvoiceHorizontal;
    }

    /**
     * Creates a scrollable panel with cash amount buttons
     */
    private JPanel createCashOptionsPanel() {
        JPanel pnlCashOptions = new JPanel();
        pnlCashOptions.setLayout(new GridLayout(0, 3, 10, 10));
        pnlCashOptions.setBackground(Color.WHITE);
        pnlCashOptions.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Initial buttons - will be updated dynamically
        updateCashButtons();

        return pnlCashOptions;
    }

    /**
     * Update cash buttons based on current total
     */
    private void updateCashButtons() {
        if (pnlCashOptions == null || txtTotal == null || invoice == null) {
            return;
        }

        // Clear existing buttons
        pnlCashOptions.removeAll();

        // Get current total
        double total = invoice.calculateTotal();

        // Round up to nearest 1,000
        long minAmount = ((long) Math.ceil(total / 1000)) * 1000;

        // Generate 6 buttons with custom increments: +0, +10k, +20k, +50k, +100k, +200k
        long[] increments = {0, 10000, 20000, 50000, 100000, 200000};
        for (long increment : increments) {
            long amount = minAmount + increment;
            JButton btn = createCashButton(amount);
            pnlCashOptions.add(btn);
        }

        pnlCashOptions.revalidate();
        pnlCashOptions.repaint();
    }

    /**
     * Creates a single cash amount button with styling
     */
    private JButton createCashButton(long amount) {
        // Format amount with dots as thousands separator (e.g., 100.000)
        String text = String.format("%,d", amount).replace(',', '.');

        JButton btn = createStyledButton(text);

        // Add the amount to the payment field when clicked (not replace)
        btn.addActionListener(e -> {
            if (txtCustomerPayment != null) {
                // Get current value
                Object currentValue = txtCustomerPayment.getValue();
                long currentAmount = 0;
                if (currentValue instanceof Number) {
                    currentAmount = ((Number) currentValue).longValue();
                }

                // Add new amount to current
                long newAmount = currentAmount + amount;
                txtCustomerPayment.setValue(newAmount);
                txtCustomerPayment.setForeground(Color.BLACK);
                txtCustomerPayment.requestFocusInWindow();
            }
        });

        return btn;
    }

    private Box generateLabelAndTextField(JLabel lbl, JTextField txt, String placeholderText, String tooltipText, int gap) {
        Box boxHorizontal = Box.createHorizontalBox();
        lbl.setFont(new Font("Arial", Font.PLAIN, 16));

        boxHorizontal.add(lbl);

        boxHorizontal.add(Box.createHorizontalStrut(gap));

        txt.setFont(new Font("Arial", Font.PLAIN, 16));

        if (!(txt instanceof JFormattedTextField))
            setPlaceholderAndTooltip(txt, placeholderText, tooltipText);

        boxHorizontal.add(txt);

        return boxHorizontal;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        pnlSelling = new JPanel();
        pnlSelling.setLayout(new BorderLayout(0, 0));
        pnlSelling.setBackground(new Color(-16777216));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pnlSelling;
    }

    /**
     * Custom cell editor for Unit of Measure column with dropdown
     */
    private class UnitOfMeasureCellEditor extends DefaultCellEditor {
        private JComboBox<String> comboBox;
        private String currentProductId;

        public UnitOfMeasureCellEditor() {
            super(new JComboBox<>());
            comboBox = (JComboBox<String>) getComponent();
            comboBox.setFont(new Font("Arial", Font.PLAIN, 16));

            // Override the UI to control popup positioning
            comboBox.setUI(new BasicComboBoxUI() {
                @Override
                protected ComboPopup createPopup() {
                    return new BasicComboPopup(comboBox) {
                        @Override
                        public void show() {
                            // Get the combo box location on screen
                            Point comboLocation = comboBox.getLocationOnScreen();

                            // Calculate popup dimensions
                            Dimension popupSize = new Dimension(
                                    comboBox.getWidth(),
                                    getPopupHeightForRowCount(comboBox.getMaximumRowCount())
                            );

                            // Position directly below the combo box
                            int x = 0;  // Relative to combo box
                            int y = comboBox.getHeight();  // Below the combo box

                            // Check if popup would go off-screen
                            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                            if (comboLocation.y + comboBox.getHeight() + popupSize.height > screenSize.height) {
                                // Show above if not enough space below
                                y = -popupSize.height;
                            }

                            // Set the popup size
                            scroller.setMaximumSize(popupSize);
                            scroller.setPreferredSize(popupSize);
                            scroller.setMinimumSize(popupSize);

                            // Select the current item
                            int selectedIndex = comboBox.getSelectedIndex();
                            if (selectedIndex == -1) {
                                list.clearSelection();
                            } else {
                                list.setSelectedIndex(selectedIndex);
                                list.ensureIndexIsVisible(selectedIndex);
                            }

                            // Show the popup at the calculated position
                            setLightWeightPopupEnabled(comboBox.isLightWeightPopupEnabled());
                            show(comboBox, x, y);
                        }
                    };
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            // Store the previous UOM value before editing
            if (value != null) {
                previousUOMMap.put(row, value.toString());
            }

            // Get the product ID from the current row
            currentProductId = (String) table.getValueAt(row, 0);

            // Get the product from the map
            Product product = productMap.get(currentProductId);

            // Clear and repopulate combo box
            comboBox.removeAllItems();

            if (product != null) {
                // Add base unit of measure
                if (product.getBaseUnitOfMeasure() != null && !product.getBaseUnitOfMeasure().isEmpty()) {
                    comboBox.addItem(product.getBaseUnitOfMeasure());
                }

                // Add all other units of measure
                if (product.getUnitOfMeasureList() != null) {
                    for (UnitOfMeasure uom : product.getUnitOfMeasureList()) {
                        if (uom.getName() != null && !uom.getName().isEmpty()) {
                            // Don't add duplicate if it's same as base unit
                            if (!uom.getName().equals(product.getBaseUnitOfMeasure())) {
                                comboBox.addItem(uom.getName());
                            }
                        }
                    }
                }
            }

            // Set current value
            if (value != null) {
                comboBox.setSelectedItem(value);
            }

            return comboBox;
        }
    }

    /**
     * Custom cell editor for Quantity column with spinner
     */
    private class QuantitySpinnerEditor extends DefaultCellEditor {
        private JSpinner spinner;
        private SpinnerNumberModel spinnerModel;

        public QuantitySpinnerEditor() {
            super(new JTextField());
            spinnerModel = new SpinnerNumberModel(1, 1, 9999, 1);
            spinner = new JSpinner(spinnerModel);
            spinner.setFont(new Font("Arial", Font.PLAIN, 16));

            // Make the spinner text field center-aligned
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
                textField.setHorizontalAlignment(JTextField.CENTER);
                textField.setFont(new Font("Arial", Font.PLAIN, 16));
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            // Set the current value
            if (value instanceof Integer) {
                spinner.setValue(value);
            } else {
                spinner.setValue(1);
            }
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public boolean stopCellEditing() {
            try {
                spinner.commitEdit();
            } catch (ParseException e) {
                // If commit fails, use the current value
            }
            return super.stopCellEditing();
        }
    }

    /**
     * Custom cell renderer for Quantity column to always show spinner
     */
    private class QuantitySpinnerRenderer implements TableCellRenderer {
        private final JSpinner spinner;

        public QuantitySpinnerRenderer() {
            SpinnerNumberModel model = new SpinnerNumberModel(1, 1, 9999, 1);
            spinner = new JSpinner(model);
            spinner.setFont(new Font("Arial", Font.PLAIN, 16));

            // Make the spinner text field center-aligned
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
                textField.setHorizontalAlignment(JTextField.CENTER);
                textField.setFont(new Font("Arial", Font.PLAIN, 16));
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // Set the current value
            if (value instanceof Integer) {
                spinner.setValue(value);
            } else {
                spinner.setValue(1);
            }

            // Set background color based on selection
            if (isSelected) {
                spinner.setBackground(table.getSelectionBackground());
                JComponent editor = spinner.getEditor();
                if (editor instanceof JSpinner.DefaultEditor) {
                    ((JSpinner.DefaultEditor) editor).getTextField().setBackground(table.getSelectionBackground());
                }
            } else {
                spinner.setBackground(table.getBackground());
                JComponent editor = spinner.getEditor();
                if (editor instanceof JSpinner.DefaultEditor) {
                    ((JSpinner.DefaultEditor) editor).getTextField().setBackground(table.getBackground());
                }
            }

            return spinner;
        }
    }

    /**
     * Custom cell renderer for currency columns (Unit Price and Total)
     */
    private class CurrencyRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat currencyFormat;

        public CurrencyRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(new Font("Arial", Font.PLAIN, 16));

            currencyFormat = createCurrencyFormat();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // Format the value as currency
            if (value instanceof Number) {
                double amount = ((Number) value).doubleValue();
                value = currencyFormat.format(amount);
            }
            // If already a String, keep as is

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Create a DecimalFormat for Vietnamese currency formatting
     * @return DecimalFormat configured with Vietnamese currency settings
     */
    private DecimalFormat createCurrencyFormat() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator('.');
        dfs.setDecimalSeparator(',');
        DecimalFormat currencyFormat = new DecimalFormat("#,##0 'Đ'", dfs);
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setGroupingSize(3);
        return currencyFormat;
    }

    /**
     * Parse currency formatted string back to double value
     *
     * @param formattedValue The formatted currency string (e.g., "10.000 Đ")
     * @return The numeric value as double
     */
    private double parseCurrencyValue(String formattedValue) {
        if (formattedValue == null || formattedValue.trim().isEmpty()) {
            return 0.0;
        }

        // Remove currency symbol and spaces
        String cleaned = formattedValue.replace("Đ", "").trim();

        // Replace grouping separator (.) with empty string
        cleaned = cleaned.replace(".", "");

        // Replace decimal separator (,) with .
        cleaned = cleaned.replace(",", ".");

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Update the VAT display field with the calculated VAT amount from invoice
     */
    private void updateVatDisplay() {
        if (txtVat != null && invoice != null) {
            double vatAmount = invoice.calculateVatAmount();

            // Format VAT amount as currency
            DecimalFormat currencyFormat = createCurrencyFormat();

            txtVat.setText(currencyFormat.format(vatAmount));
        }
    }

    /**
     * Update the total display field with calculated total from invoice
     */
    private void updateTotalDisplay() {
        if (txtTotal != null && invoice != null) {
            double total = invoice.calculateTotal();

            // Format total as Vietnamese currency
            DecimalFormat currencyFormat = createCurrencyFormat();
            txtTotal.setText(currencyFormat.format(total));

            // Update cash buttons based on new total
            updateCashButtons();
        }
    }

    /**
     * @noinspection ALL
     */
}