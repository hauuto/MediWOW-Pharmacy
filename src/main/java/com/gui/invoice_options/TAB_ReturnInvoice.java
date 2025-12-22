package com.gui.invoice_options;

import com.bus.*;
import com.dao.DAO_Invoice;
import com.entities.*;
import com.enums.*;
import com.interfaces.DataChangeListener;
import com.interfaces.ShiftChangeListener;
import com.utils.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.List;

public class TAB_ReturnInvoice extends JFrame implements ActionListener, MouseListener, FocusListener, KeyListener, PropertyChangeListener, TableModelListener {
    public JPanel pnlReturnInvoice;
    private static final int LEFT_MIN = 750, RIGHT_MIN = 530;
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
    private Invoice returnInvoice;
    private Invoice selectedOriginalInvoice;

    // Original invoice line max quantities (row index -> original quantity)
    private final Map<Integer, Integer> originalMaxQuantityMap = new HashMap<>();

    // Tables
    private DefaultTableModel mdlOriginalInvoiceLine;
    private JTable tblOriginalInvoiceLine;
    private JScrollPane scrOriginalInvoiceLine;

    // UI components
    private JButton btnProcessPayment;
    private boolean isUpdatingInvoiceLine = false;
    private JTextField txtOriginalTotal, txtRefundAmount, txtShiftId, txtCustomerName, txtInvoiceSearch;
    private Window parentWindow;

    public TAB_ReturnInvoice(Staff creator, ShiftChangeListener shiftChangeListener) {
        this(creator, shiftChangeListener, null);
    }

    public TAB_ReturnInvoice(Staff creator, ShiftChangeListener shiftChangeListener, DataChangeListener dataChangeListener) {
        this.currentStaff = Objects.requireNonNull(creator, "Staff cannot be null");
        this.shiftChangeListener = shiftChangeListener;
        this.dataChangeListener = dataChangeListener;
        $$$setupUI$$$();
        parentWindow = SwingUtilities.getWindowAncestor(pnlReturnInvoice);
        currentShift = ensureCurrentShift();
        createMainLayout();
        setFieldsEnabled(false);
    }

    public TAB_ReturnInvoice(Staff creator) {
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
        if (txtCustomerName != null) { txtCustomerName.setEnabled(enabled); txtCustomerName.setFocusable(enabled); }
        if (tblOriginalInvoiceLine != null) tblOriginalInvoiceLine.setEnabled(enabled);
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
        boolean hasReturnItems = hasValidReturnItems();
        btnProcessPayment.setEnabled(hasOriginalInvoice && hasReturnItems);
    }

    private boolean hasValidReturnItems() {
        if (mdlOriginalInvoiceLine == null) return false;
        for (int i = 0; i < mdlOriginalInvoiceLine.getRowCount(); i++) {
            int qty = (int) mdlOriginalInvoiceLine.getValueAt(i, 3);
            if (qty > 0) return true;
        }
        return false;
    }

    private void createMainLayout() {
        pnlReturnInvoice.add(createInvoiceSearchBar(), BorderLayout.NORTH);
        pnlReturnInvoice.add(createSplitPane(), BorderLayout.CENTER);
    }

    private Box createInvoiceSearchBar() {
        Box v = Box.createVerticalBox(), h = Box.createHorizontalBox();
        v.setOpaque(true); v.setBackground(AppColors.BACKGROUND);
        v.add(Box.createVerticalStrut(5)); v.add(h); v.add(Box.createVerticalStrut(5));
        h.add(Box.createHorizontalStrut(5));
        txtInvoiceSearch = new JTextField();
        h.add(generateLabelAndTextField(new JLabel("Tìm hóa đơn gốc:"), txtInvoiceSearch, "Nhập mã hóa đơn cần trả...", "Nhập mã hóa đơn cần trả", 0));
        h.add(Box.createHorizontalStrut(5));
        txtInvoiceSearch.addKeyListener(this);
        txtInvoiceSearch.addFocusListener(this);
        return v;
    }

    private void searchAndLoadInvoice() {
        String inputId = txtInvoiceSearch.getText().trim();
        if (inputId.isEmpty() || inputId.equals("Nhập mã hóa đơn cần trả...") ||
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
                "Mã hóa đơn này là hóa đơn đổi hàng!\nKhông thể sử dụng hóa đơn đổi hàng để trả hàng.",
                "Loại hóa đơn không hợp lệ", JOptionPane.WARNING_MESSAGE);
            txtInvoiceSearch.selectAll();
            txtInvoiceSearch.requestFocusInWindow();
            return;
        }
        if (invoice.getType() == InvoiceType.RETURN) {
            JOptionPane.showMessageDialog(parentWindow,
                "Mã hóa đơn này là hóa đơn trả hàng!\nKhông thể sử dụng hóa đơn trả hàng để trả hàng.",
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
        returnInvoice = new Invoice(InvoiceType.RETURN, currentStaff, currentShift);
        returnInvoice.setReferencedInvoice(selectedOriginalInvoice);
        returnInvoice.setPaymentMethod(PaymentMethod.CASH);
        loadOriginalInvoiceLines(invoice);
        refreshOpenShiftAndUI();
        setFieldsEnabled(true);
        updateDisplays();
        txtInvoiceSearch.setText("");
        txtInvoiceSearch.setForeground(AppColors.TEXT);
        txtInvoiceSearch.requestFocusInWindow();
        JOptionPane.showMessageDialog(parentWindow,
            "Đã chọn hóa đơn gốc: " + normalizedId + "\nBạn có thể điều chỉnh số lượng sản phẩm cần trả.",
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
            BigDecimal subtotal = line.calculateSubtotal();
            mdlOriginalInvoiceLine.addRow(new Object[]{productId, productName, uomName, quantity, unitPrice, subtotal});
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
        Box titleBox = Box.createVerticalBox(), titleH = Box.createHorizontalBox();
        JLabel title = new JLabel("CHI TIẾT HÓA ĐƠN GỐC");
        title.setFont(new Font("Arial", Font.BOLD, 20)); title.setForeground(AppColors.DARK);
        titleH.add(Box.createHorizontalGlue()); titleH.add(title); titleH.add(Box.createHorizontalGlue());
        titleBox.add(Box.createVerticalStrut(20)); titleBox.add(titleH); titleBox.add(Box.createVerticalStrut(20));
        left.add(titleBox, BorderLayout.NORTH);
        createOriginalInvoiceLineTable();
        left.add(scrOriginalInvoiceLine, BorderLayout.CENTER);
        return left;
    }

    private void createOriginalInvoiceLineTable() {
        mdlOriginalInvoiceLine = new DefaultTableModel(new String[]{"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 3; }
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
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, s, f, row, col);
                c.setBackground(row % 2 == 0 ? AppColors.WHITE : AppColors.BACKGROUND);
                if (s) c.setBackground(t.getSelectionBackground());
                return c;
            }
        };
        for (int i = 0; i < 3; i++) tblOriginalInvoiceLine.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
        CurrencyRenderer currencyRenderer = new CurrencyRenderer();
        tblOriginalInvoiceLine.getColumnModel().getColumn(4).setCellRenderer(currencyRenderer);
        tblOriginalInvoiceLine.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer);
        scrOriginalInvoiceLine = new JScrollPane(tblOriginalInvoiceLine);
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
        JLabel title = new JLabel("HÓA ĐƠN TRẢ HÀNG");
        title.setFont(new Font("Arial", Font.BOLD, 20)); title.setForeground(AppColors.DARK);
        Box th = Box.createHorizontalBox(); th.add(Box.createHorizontalGlue()); th.add(title); th.add(Box.createHorizontalGlue());
        v.add(Box.createVerticalStrut(20)); v.add(th); v.add(Box.createVerticalStrut(20));

        // General Information
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

        // Refund Information
        Box pay = Box.createHorizontalBox();
        TitledBorder payb = BorderFactory.createTitledBorder("Thông tin hoàn tiền");
        payb.setTitleFont(new Font("Arial", Font.BOLD, 16)); payb.setTitleColor(AppColors.PRIMARY);
        pay.setBorder(payb);
        v.add(pay);
        Box payv = Box.createVerticalBox(); pay.add(payv);

        txtOriginalTotal = new JTextField(); txtOriginalTotal.setEditable(false); txtOriginalTotal.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("Tổng HĐ gốc:"), txtOriginalTotal, "", "Tổng tiền hóa đơn gốc", 54));
        payv.add(Box.createVerticalStrut(10));

        txtRefundAmount = new JTextField(); txtRefundAmount.setEditable(false); txtRefundAmount.setFocusable(false);
        payv.add(generateLabelAndTextField(new JLabel("Tiền hoàn trả:"), txtRefundAmount, "", "Số tiền cần hoàn trả cho khách", 50));
        payv.add(Box.createVerticalStrut(10));

        v.add(Box.createVerticalStrut(20));
        Box pb1 = Box.createHorizontalBox(); pb1.add(Box.createHorizontalGlue());
        btnProcessPayment = createStyledButton("Hoàn tiền");
        btnProcessPayment.setEnabled(false); btnProcessPayment.setName("btnProcessPayment"); btnProcessPayment.addActionListener(this);
        pb1.add(btnProcessPayment);
        v.add(pb1);
        return h;
    }

    private JButton createStyledButton(String text) {
        int arc = 12;
        boolean isPaymentBtn = text.equalsIgnoreCase("Hoàn tiền");
        Color defaultBg = isPaymentBtn ? AppColors.BACKGROUND : AppColors.WHITE;
        Color rolloverBg = isPaymentBtn ? AppColors.WHITE : AppColors.BACKGROUND;
        Color pressedBg = AppColors.LIGHT;
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
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
        setPlaceholderAndTooltip(txt, ph, tt);
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

    private BigDecimal calculateRefundAmount() {
        BigDecimal refund = BigDecimal.ZERO;
        if (selectedOriginalInvoice == null) return refund;
        for (InvoiceLine line : selectedOriginalInvoice.getInvoiceLineList()) {
            refund = refund.add(line.calculateTotalAmount());
        }
        return refund;
    }

    private void updateDisplays() {
        if (txtOriginalTotal != null && selectedOriginalInvoice != null) {
            // Show the original total before any quantity adjustments
            BigDecimal originalTotal = BigDecimal.ZERO;
            for (int i = 0; i < mdlOriginalInvoiceLine.getRowCount(); i++) {
                int origQty = originalMaxQuantityMap.getOrDefault(i, 0);
                BigDecimal unitPrice = (BigDecimal) mdlOriginalInvoiceLine.getValueAt(i, 4);
                originalTotal = originalTotal.add(unitPrice.multiply(BigDecimal.valueOf(origQty)));
            }
            txtOriginalTotal.setText(createCurrencyFormat().format(originalTotal));
        } else if (txtOriginalTotal != null) {
            txtOriginalTotal.setText("");
        }
        if (txtRefundAmount != null && selectedOriginalInvoice != null) {
            BigDecimal refundAmount = calculateRefundAmount();
            txtRefundAmount.setText(createCurrencyFormat().format(refundAmount));
        } else if (txtRefundAmount != null) {
            txtRefundAmount.setText("");
        }
        updateProcessPaymentButton();
    }

    private void refreshOpenShiftAndUI() {
        try {
            String workstation = busShift.getCurrentWorkstation();
            Shift openShift = busShift.getOpenShiftOnWorkstation(workstation);
            if (openShift != null) {
                currentShift = openShift;
                if (returnInvoice != null) returnInvoice.setShift(openShift);
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
        if (selectedOriginalInvoice != null && selectedOriginalInvoice.getInvoiceLineList() != null) {
            selectedOriginalInvoice.getInvoiceLineList().removeIf(line -> line.getQuantity() == 0);
        }
        if (!hasValidReturnItems()) {
            JOptionPane.showMessageDialog(parentWindow, "Không có sản phẩm nào để trả!", "Không thể hoàn tiền", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedOriginalInvoice != null && selectedOriginalInvoice.getInvoiceLineList() != null) {
            for (InvoiceLine origLine : selectedOriginalInvoice.getInvoiceLineList()) {
                if (origLine.getQuantity() > 0) {
                    InvoiceLine returnLine = new InvoiceLine(returnInvoice, origLine.getUnitOfMeasure(),
                        origLine.getQuantity(), origLine.getUnitPrice(), LineType.RETURN, new ArrayList<>());
                    returnInvoice.addInvoiceLine(returnLine);
                }
            }
        }
        completeReturnAndGenerateInvoice();
    }

    private void completeReturnAndGenerateInvoice() {
        try {
            refreshOpenShiftAndUI();
            if (selectedOriginalInvoice != null) {
                for (InvoiceLine origLine : selectedOriginalInvoice.getInvoiceLineList()) {
                    if (origLine.getQuantity() > 0) {
                        for (LotAllocation allocation : origLine.getLotAllocations()) {
                            boolean success = busProduct.addLotQuantity(allocation.getLot().getId(), allocation.getQuantity());
                            if (!success) {
                                throw new RuntimeException("Không thể cập nhật số lượng lô: " + allocation.getLot().getId());
                            }
                        }
                    }
                }
            }
            String customerName = getCustomerNameValue();
            if (customerName != null && !customerName.isEmpty()) {
                try {
                    Customer customer = new Customer(customerName);
                    busCustomer.addCustomer(customer);
                    returnInvoice.setCustomer(customer);
                } catch (Exception e) {
                    System.err.println("Warning: Could not save customer: " + e.getMessage());
                }
            }
            String generatedInvoiceId = busInvoice.saveInvoice(returnInvoice);
            BigDecimal refundAmount = calculateRefundAmount();
            if (dataChangeListener != null) {
                dataChangeListener.onInvoiceCreated();
            }
            int printChoice = JOptionPane.showConfirmDialog(parentWindow,
                "Hoàn tiền thành công!\nSố tiền hoàn trả: " + createCurrencyFormat().format(refundAmount) + "\n\nBạn có muốn in hóa đơn không?",
                "In hóa đơn", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (printChoice == JOptionPane.YES_OPTION) {
                printReturnReceiptDirectly(generatedInvoiceId, refundAmount);
            }
            resetReturnForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentWindow, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void printReturnReceiptDirectly(String invoiceId, BigDecimal refundAmount) {
        String[] printers = ReceiptThermalPrinter.getAvailablePrinters();
        if (printers.length == 0) {
            JOptionPane.showMessageDialog(parentWindow, "Không tìm thấy máy in nào!", "Lỗi máy in", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String selectedPrinter = (String) JOptionPane.showInputDialog(parentWindow, "Chọn máy in:", "Chọn máy in",
            JOptionPane.QUESTION_MESSAGE, null, printers, printers[0]);
        if (selectedPrinter == null) return;
        try {
            ReceiptThermalPrinter.printReturnReceipt(returnInvoice, invoiceId, refundAmount, selectedPrinter);
            JOptionPane.showMessageDialog(parentWindow, "In hóa đơn thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parentWindow, "Lỗi in hóa đơn: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void resetReturnForm() {
        mdlOriginalInvoiceLine.setRowCount(0);
        originalMaxQuantityMap.clear();
        selectedOriginalInvoice = null;
        returnInvoice = null;
        if (txtCustomerName != null) {
            txtCustomerName.setText("Điền tên khách hàng (nếu có)...");
            txtCustomerName.setForeground(AppColors.PLACEHOLDER_TEXT);
        }
        if (txtOriginalTotal != null) txtOriginalTotal.setText("");
        if (txtRefundAmount != null) txtRefundAmount.setText("");
        setFieldsEnabled(false);
    }

    private void $$$setupUI$$$() {
        pnlReturnInvoice = new JPanel(); pnlReturnInvoice.setLayout(new BorderLayout(0, 0));
        pnlReturnInvoice.setBackground(AppColors.WHITE);
    }

    public JComponent $$$getRootComponent$$$() { return pnlReturnInvoice; }

    private class CurrencyRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat fmt = createCurrencyFormat();
        public CurrencyRenderer() { setHorizontalAlignment(SwingConstants.RIGHT); setFont(new Font("Arial", Font.PLAIN, 16)); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof java.math.BigDecimal bd) { v = fmt.format(bd); }
            else if (v instanceof Number) { v = fmt.format(((Number) v).doubleValue()); }
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
        if (n.equals("btnProcessPayment")) { processPayment(); }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override public void focusGained(FocusEvent e) {
        if (e.getSource() instanceof JTextField t) {
            if (t.getName() != null && t.getName().startsWith("placeholder_") && t.getForeground().equals(AppColors.PLACEHOLDER_TEXT)) {
                t.setText(""); t.setForeground(AppColors.TEXT);
            }
        }
    }

    @Override public void focusLost(FocusEvent e) {
        Object src = e.getSource();
        if (src instanceof JTextField t) {
            if (t.getName() != null && t.getName().startsWith("placeholder_") && t.getText().isEmpty()) {
                t.setText(t.getToolTipText()); t.setForeground(AppColors.PLACEHOLDER_TEXT);
            }
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getSource() == txtInvoiceSearch && e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume(); searchAndLoadInvoice();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void propertyChange(PropertyChangeEvent evt) {
        if (isUpdatingInvoiceLine) return;
        if ("tableCellEditor".equals(evt.getPropertyName()) && evt.getOldValue() != null && evt.getNewValue() == null) {
            if (evt.getSource() == tblOriginalInvoiceLine) {
                int r = tblOriginalInvoiceLine.getEditingRow();
                if (r == -1) r = tblOriginalInvoiceLine.getSelectedRow();
                if (r >= 0) updateOriginalInvoiceLineFromTable(r);
            }
        }
    }

    @Override public void tableChanged(TableModelEvent e) {
        if (isUpdatingInvoiceLine) return;
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
            String productId = (String) mdlOriginalInvoiceLine.getValueAt(row, 0);
            String uomName = (String) mdlOriginalInvoiceLine.getValueAt(row, 2);
            for (InvoiceLine line : selectedOriginalInvoice.getInvoiceLineList()) {
                if (line.getProduct().getId().equals(productId) && line.getUnitOfMeasure().getName().equals(uomName)) {
                    line.setQuantity(newQuantity);
                    BigDecimal newSubtotal = line.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity));
                    mdlOriginalInvoiceLine.setValueAt(newSubtotal, row, 5);
                    break;
                }
            }
            updateDisplays();
        } finally {
            isUpdatingInvoiceLine = false;
        }
    }

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
            int maxQty = originalMaxQuantityMap.getOrDefault(r, 9999);
            int currentValue = (v instanceof Integer) ? (Integer) v : 0;
            spinner.setModel(new SpinnerNumberModel(currentValue, 0, maxQty, 1));
            SwingUtilities.invokeLater(() -> { editor.getTextField().requestFocusInWindow(); editor.getTextField().selectAll(); });
            return spinner;
        }
        public Object getCellEditorValue() { return spinner.getValue(); }
        public boolean stopCellEditing() {
            try { spinner.commitEdit(); } catch (java.text.ParseException e) {}
            return super.stopCellEditing();
        }
    }

    private class QuantitySpinnerRenderer implements TableCellRenderer {
        private JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        public QuantitySpinnerRenderer() { spinner.setFont(new Font("Arial", Font.PLAIN, 16)); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof Integer) spinner.setValue(v);
            return spinner;
        }
    }
}

