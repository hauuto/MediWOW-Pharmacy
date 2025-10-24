package com.gui;

import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

public class TAB_Selling extends JFrame {
    JPanel pnlSelling;

    private static final Color LIGHT_GRAY = new Color(210, 210, 210);
    private static final int LEFT_PANEL_MINIMAL_WIDTH = 750;
    private static final int RIGHT_PANEL_MINIMAL_WIDTH = 600;

    private DefaultTableModel mdlInvoiceLine;
    private JTable tblInvoiceLine;
    private JScrollPane scrInvoiceLine;
    private JButton btnRemoveItem;
    private JButton btnRemoveAllItems;
    private JSplitPane splitPane;

    // Field to hold the customer payment text field for cash button updates
    private JFormattedTextField txtCustomerPayment;

    public TAB_Selling() {
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
        JTextField txtSearchInput = new JTextField();

        boxSearchBarHorizontal.add(generateLabelAndTextField(lblSearch, txtSearchInput, "Nhập mã/tên/tên rút gọn của thuốc...", "Nhập mã/tên/tên rút gọn của thuốc", 10));

        boxSearchBarHorizontal.add(Box.createHorizontalStrut(5));

        return boxSearchBarVertical;
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
        pnlRight.setBackground(LIGHT_GRAY);
        pnlRight.setMinimumSize(new Dimension(RIGHT_PANEL_MINIMAL_WIDTH, 0));
        pnlRight.add(createInvoice(), BorderLayout.NORTH);

        // Create split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlLeft, pnlRight);

        pnlSelling.add(splitPane, BorderLayout.CENTER);
    }

    private void createInvoiceLineTable() {
        String[] columnHeaders = {"Mã thuốc", "Tên thuốc", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"};

        mdlInvoiceLine = new DefaultTableModel(columnHeaders, 0) {
            // Make only "Số lượng" (column 3) and "Đơn giá" (column 4) editable
            @Override
            public boolean isCellEditable(int row, int column) {
                return (column == 3 || column == 4);
            }
        };

        tblInvoiceLine = new JTable(mdlInvoiceLine);
        tblInvoiceLine.setFont(new Font("Arial", Font.PLAIN, 16));
        tblInvoiceLine.getTableHeader().setReorderingAllowed(false);

        tblInvoiceLine.getTableHeader().setBackground(AppColors.PRIMARY);
        tblInvoiceLine.getTableHeader().setForeground(Color.WHITE);
        tblInvoiceLine.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        scrInvoiceLine = new JScrollPane(tblInvoiceLine);
    }

    private JPanel createInvoiceLineTableButtons() {
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.setBackground(Color.WHITE);

        btnRemoveAllItems = createStyledButton("Xóa tất cả");
        pnlButtons.add(btnRemoveAllItems);

        btnRemoveItem = createStyledButton("Xóa sản phẩm");
        pnlButtons.add(btnRemoveItem);

        return pnlButtons;
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
        button.setMargin(new Insets(10, 20, 10, 20));
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(new Color(11, 110, 217));
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        if (text.equalsIgnoreCase("Thanh toán"))
            button.setBackground(LIGHT_GRAY);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add mouse listener for hover and click effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(AppColors.BACKGROUND);

                if (text.equalsIgnoreCase("Thanh toán"))
                    button.setBackground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);

                if (text.equalsIgnoreCase("Thanh toán"))
                    button.setBackground(LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(AppColors.LIGHT);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.contains(e.getPoint())) {
                    button.setBackground(LIGHT_GRAY);

                    if (text.equalsIgnoreCase("Thanh toán"))
                        button.setBackground(Color.WHITE);
                } else {
                    button.setBackground(Color.WHITE);

                    if (text.equalsIgnoreCase("Thanh toán"))
                        button.setBackground(LIGHT_GRAY);
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

        boxInvoiceVertical.add(Box.createVerticalStrut(20));

        boxInvoiceVertical.add(boxInvoiceTitle);

        boxInvoiceVertical.add(Box.createVerticalStrut(20));


        // PRESCRIPTION DETAILS SECTION


        Box boxPrescriptionDetailsHorizontal = Box.createHorizontalBox();
        TitledBorder prescriptionBorder = BorderFactory.createTitledBorder("Thông tin kê đơn thuốc");
        prescriptionBorder.setTitleFont(new Font("Arial", Font.BOLD, 16));
        prescriptionBorder.setTitleColor(AppColors.PRIMARY);
        boxPrescriptionDetailsHorizontal.setBorder(prescriptionBorder);

        boxInvoiceVertical.add(boxPrescriptionDetailsHorizontal);

        boxInvoiceVertical.add(Box.createVerticalStrut(20));

        Box boxPrescriptionDetailsVertical = Box.createVerticalBox();
        boxPrescriptionDetailsHorizontal.add(boxPrescriptionDetailsVertical);

        // Adding prescription code label and text field
        JLabel lblPrescriptionCode = new JLabel("Mã đơn kê thuốc:");
        JTextField txtPrescriptionCode = new JTextField();

        boxPrescriptionDetailsVertical.add(generateLabelAndTextField(lblPrescriptionCode, txtPrescriptionCode, "Điền mã đơn kê thuốc (nếu có)...", "Điền mã đơn kê thuốc", 69));

        boxPrescriptionDetailsVertical.add(Box.createVerticalStrut(10));

        // Adding customer name label and text field
        JLabel lblCustomerName = new JLabel("Tên khách hàng:");
        JTextField txtCustomerName = new JTextField();

        boxPrescriptionDetailsVertical.add(generateLabelAndTextField(lblCustomerName, txtCustomerName, "Điền tên khách hàng (nếu có)...", "Điền tên khách hàng", 76));

        boxPrescriptionDetailsVertical.add(Box.createVerticalStrut(10));

        // Adding customer phone number label and text field
        JLabel lblCustomerPhoneNumber = new JLabel("Số điện thoại khách hàng:");
        JTextField txtCustomerPhoneNumber = new JTextField();

        boxPrescriptionDetailsVertical.add(generateLabelAndTextField(lblCustomerPhoneNumber, txtCustomerPhoneNumber, "Điền số điện thoại khách hàng (nếu có)...", "Điền số điện thoại khách hàng", 10));

        boxPrescriptionDetailsVertical.add(Box.createVerticalStrut(10));

        // Adding customer address label and text field
        JLabel lblCustomerAddress = new JLabel("Địa chỉ khách hàng:");
        JTextField txtCustomerAddress = new JTextField();

        boxPrescriptionDetailsVertical.add(generateLabelAndTextField(lblCustomerAddress, txtCustomerAddress, "Điền địa chỉ khách hàng (nếu có)...", "Điền địa chỉ khách hàng", 53));


        // PAYMENT DETAILS SECTION


        Box boxPaymentHorizontal = Box.createHorizontalBox();
        TitledBorder paymentBorder = BorderFactory.createTitledBorder("Thông tin thanh toán");
        paymentBorder.setTitleFont(new Font("Arial", Font.BOLD, 16));
        paymentBorder.setTitleColor(AppColors.PRIMARY);
        boxPaymentHorizontal.setBorder(paymentBorder);

        boxInvoiceVertical.add(boxPaymentHorizontal);

        Box boxPaymentVertical = Box.createVerticalBox();
        boxPaymentHorizontal.add(boxPaymentVertical);

        // Adding promotion code label and text field
        JLabel lblPromotionCode = new JLabel("Mã khuyến mãi:");
        JTextField txtPromotionCode = new JTextField();

        boxPaymentVertical.add(generateLabelAndTextField(lblPromotionCode, txtPromotionCode, "Điền mã khuyến mãi (nếu có)...", "Điền mã khuyến mãi", 93));

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
        JTextField txtVat = new JTextField();
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
        JTextField txtTotal = new JTextField();
        txtTotal.setEditable(false);
        txtTotal.setFocusable(false);

        boxPaymentVertical.add(generateLabelAndTextField(lblTotal, txtTotal, "", "Tổng tiền", 133));

        boxPaymentVertical.add(Box.createVerticalStrut(10));

        // Create formatted text field for Vietnamese currency
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator('.');
        dfs.setDecimalSeparator(',');
        DecimalFormat currencyFormat = new DecimalFormat("#,000 'Đ'", dfs);
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setGroupingSize(3);

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

        JPanel pnlCashOptions = createCashOptionsPanel();
        boxCashOptions.add(pnlCashOptions);

        // Add action listeners to show/hide cash options based on payment method
        rdoCash.addActionListener(e -> {
            boxCashOptions.setVisible(true);
            boxPaymentVertical.revalidate();
            boxPaymentVertical.repaint();
        });

        rdoBankOrDigitalWallet.addActionListener(e -> {
            boxCashOptions.setVisible(false);
            boxPaymentVertical.revalidate();
            boxPaymentVertical.repaint();
        });

        boxInvoiceVertical.add(Box.createVerticalStrut(20));

        // PAYMENT BUTTON SECTION

        Box boxPaymentButton = Box.createHorizontalBox();
        boxInvoiceVertical.add(boxPaymentButton);

        boxPaymentButton.add(Box.createHorizontalGlue());

        JButton btnProcessPayment = createStyledButton("Thanh toán");
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

        for (int amount = 100_000; amount <= 600_000; amount += 100_000) {
            JButton btn = createCashButton(amount);
            pnlCashOptions.add(btn);
        }

        return pnlCashOptions;
    }

    /**
     * Creates a single cash amount button with styling
     */
    private JButton createCashButton(int amount) {
        // Format amount with dots as thousands separator (e.g., 100.000)
        String text = String.format("%,d", amount).replace(',', '.');

        JButton btn = createStyledButton(text);

        // Set the amount in the payment field when clicked
        btn.addActionListener(e -> {
            if (txtCustomerPayment != null) {
                txtCustomerPayment.setValue(Long.valueOf(amount));
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
     * @noinspection ALL
     */
}