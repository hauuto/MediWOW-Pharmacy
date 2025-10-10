package com.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.text.NumberFormatter;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Random;

public class TAB_Selling {
    JPanel pnlSelling;
    private JTable tblInvoiceLine;
    private JPanel pnlInvoice;
    private JPanel pnlGeneralInfo;
    private JLabel lblTitle;
    private JTextField txtPrescriptionCode;
    private JLabel lblPrescriptionCode;
    private JLabel lblCustomerName;
    private JTextField txtCustomerName;
    private JTextField txtCustomerTeleNo;
    private JLabel lblCustomerTeleNo;
    private JLabel lblCustomerAddress;
    private JTextField txtCustomerAdress;
    private JScrollPane scrtblInvoiceLine;
    private JRadioButton radCash;
    private JRadioButton radBank;
    private JLabel lblPayment;
    private JLabel lblTotal;
    private JLabel lblTotalNumber;
    private JLabel lblVATNumber;
    private JLabel lblVAT;
    private JLabel lblPromotionID;
    private JTextField txtPromotionID;
    private JPanel pnlCashPayment;
    private JPanel pnlBillingInfo;
    private JLabel lblPromotionDetail;
    private JLabel lblPromotionDetailText;
    private JPanel pnlCashAmountList;
    private JButton btnCashAmount;
    private JLabel lblCashPaid;
    private JButton btnCreateInvoice;
    private JScrollPane scrCashAmountList;
    private JButton btnDeleteInvoiceLine;
    private JFormattedTextField txtCashPaidAmount;
    private JPanel pnlProduct;
    private JPanel pnlProductSort;
    private JLabel lblProductID;
    private JTextField txtProductID;
    private JLabel lblProductName;
    private JTextField txtProductName;
    private JLabel lblManufacturer;
    private JTextField txtManufacturer;
    private JLabel lblShortname;
    private JTextField txtShortname;
    private JLabel lblCategory;
    private JLabel lblForm;
    private JComboBox cmbCategory;
    private JComboBox cmbForm;
    private JButton btnFilterProducts;
    private JButton btnRefresh;
    private JScrollPane scrProductList;
    private JPanel pnlProductList;
    private JPanel pnlProductCard;
    private JPanel pnlProductDescription;
    private JLabel lblProductCardName;
    private JPanel pnlProductAmount;
    private JLabel lblProductAmount;
    private JSpinner spnProductAmount;
    private JComboBox cmbProductUnitOfMeasure;
    private JLabel lblProductUnitOfMeasure;
    private JPanel pnlProductUnitOfMeasure;
    private JButton btnAddProduct;
    private JPanel pnlProductImage;
    private JLabel lblProductImage;

    private void createUIComponents() {
        String[] columnNames = {"Mã thuốc", "Tên thuốc", "Số lượng", "Đơn vị", "Đơn giá", "Thành tiền"};
        Object[][] data = {
                {"T001", "Paracetamol", 2, "Viên", 5000, 10000},
                {"T002", "Amoxicillin", 1, "Hộp", 20000, 20000},
                {"T003", "Ibuprofen", 3, "Viên", 8000, 24000},
                {"T004", "Cetirizine", 1, "Hộp", 15000, 15000},
                {"T005", "Loratadine", 2, "Viên", 7000, 14000},
                {"T006", "Azithromycin", 1, "Hộp", 25000, 25000},
                {"T007", "Metformin", 2, "Viên", 12000, 24000},
                {"T008", "Aspirin", 1, "Hộp", 18000, 18000},
                {"T009", "Omeprazole", 3, "Viên", 9000, 27000},
                {"T010", "Simvastatin", 1, "Hộp", 22000, 22000},
                {"T011", "Ciprofloxacin", 2, "Viên", 11000, 22000},
                {"T012", "Doxycycline", 1, "Hộp", 16000, 16000},
                {"T013", "Clindamycin", 3, "Viên", 13000, 39000},
                {"T014", "Ranitidine", 1, "Hộp", 14000, 14000},
                {"T015", "Fexofenadine", 2, "Viên", 6000, 12000},
                {"T016", "Levofloxacin", 1, "Hộp", 23000, 23000},
                {"T017", "Nitrofurantoin", 3, "Viên", 10000, 30000},
                {"T018", "Hydrochlorothiazide", 1, "Hộp", 17000, 17000},
                {"T019", "Warfarin", 2, "Viên", 8000, 16000},
                {"T020", "Clopidogrel", 1, "Hộp", 21000, 21000}
        };

        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        tblInvoiceLine = new JTable(tableModel);
        // Set table header font size to 16
        JTableHeader header = tblInvoiceLine.getTableHeader();
        header.setFont(new Font(header.getFont().getName(), Font.BOLD, 16));
        // Prevent column reordering
        header.setReorderingAllowed(false);

        // Configure for row selection
        tblInvoiceLine.setEnabled(true);
        tblInvoiceLine.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblInvoiceLine.setRowSelectionAllowed(true);
        tblInvoiceLine.setColumnSelectionAllowed(false);
        tblInvoiceLine.setCellSelectionEnabled(false);

        // Set odd row color to blue
        tblInvoiceLine.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color oddRowColor = new Color(75, 191, 170);
            private final Color evenRowColor = Color.WHITE;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Color base = (row % 2 == 1) ? oddRowColor : evenRowColor;
                if (isSelected) {
                    c.setBackground(base.darker());
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(base);
                    c.setForeground(Color.BLACK);
                }
                if (c instanceof JComponent) ((JComponent) c).setOpaque(true);
                return c;
            }
        });

        // Make columns unresizable
        tblInvoiceLine.getTableHeader().setResizingAllowed(false);
        TableColumnModel cm = tblInvoiceLine.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setResizable(false);
        }

        radCash = new JRadioButton();
        radBank = new JRadioButton();

        ButtonGroup paymentGroup = new ButtonGroup();
        paymentGroup.add(radCash);
        paymentGroup.add(radBank);

        pnlCashPayment = new JPanel();

        // Add listeners for payment method changes
        radCash.addItemListener(e -> {
            boolean selected = radCash.isSelected();
            pnlCashPayment.setVisible(selected);
            pnlCashPayment.revalidate();
            pnlCashPayment.repaint();

            // Handle txtCashPaidAmount state
            if (selected) {
                txtCashPaidAmount.setValue(0L);
                txtCashPaidAmount.setEnabled(true);
            }
        });

        radBank.addItemListener(e -> {
            boolean selected = radBank.isSelected();

            // Handle txtCashPaidAmount state
            if (selected) {
                txtCashPaidAmount.setValue(0L);
                txtCashPaidAmount.setEnabled(false);
            }
        });

        scrCashAmountList = new JScrollPane();
        scrCashAmountList.getVerticalScrollBar().setUnitIncrement(5);
        pnlCashAmountList = new JPanel();

        btnCashAmount = new JButton();
        btnCashAmount.setBackground(new Color(-11812950));
        Font btnCashAmountFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnCashAmount.getFont());
        if (btnCashAmountFont != null) btnCashAmount.setFont(btnCashAmountFont);
        btnCashAmount.setForeground(new Color(-16777216));
        btnCashAmount.setMaximumSize(new Dimension(200, 34));
        btnCashAmount.setPreferredSize(new Dimension(127, 50));
        btnCashAmount.setText("100,000");
        btnCashAmount.setToolTipText("Chọn giá trị tiền khách đưa");
        pnlCashAmountList.add(btnCashAmount);

        for (int i = 1; i <= 20; i++) {
            JButton btnDraftAmount = cloneButton(btnCashAmount, i * 100000.0);
            pnlCashAmountList.add(btnDraftAmount);
        }

        btnCashAmount.setVisible(false);

        btnDeleteInvoiceLine = new JButton();

        // Wire up the delete button
        btnDeleteInvoiceLine.addActionListener(e -> {
            int selectedRow = tblInvoiceLine.getSelectedRow();
            if (selectedRow >= 0) {
                // Convert view row to model row in case of sorting
                int modelRow = tblInvoiceLine.convertRowIndexToModel(selectedRow);
                DefaultTableModel model = (DefaultTableModel) tblInvoiceLine.getModel();
                model.removeRow(modelRow);
            }
        });

        // Create formatted text field for Vietnamese currency
        DecimalFormat currencyFormat = new DecimalFormat("#,000 'Đ'");
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setGroupingSize(3);

        NumberFormatter formatter = new NumberFormatter(currencyFormat);
        formatter.setValueClass(Long.class);
        formatter.setMinimum(0L);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);

        txtCashPaidAmount = new JFormattedTextField(formatter);
        txtCashPaidAmount.setValue(0L);

        String[] categories = {"Chưa chọn", "Thuốc kê đơn", "Thuốc không kê đơn", "Thực phẩm bổ sung"};
        cmbCategory = new JComboBox<String>(categories);
        cmbCategory.setSelectedItem(categories[0]);

        String[] forms = {"Chưa chọn", "Thuốc dạng rắn", "Thuốc dạng lỏng", "Khác"};
        cmbForm = new JComboBox<String>(forms);
        cmbForm.setSelectedItem(forms[0]);

        btnRefresh = new JButton();
        btnRefresh.addActionListener(e -> {
            txtProductID.setText("");
            txtProductName.setText("");
            txtManufacturer.setText("");
            txtShortname.setText("");
            cmbCategory.setSelectedIndex(0);
            cmbForm.setSelectedIndex(0);
        });

        scrProductList = new JScrollPane();
        scrProductList.getVerticalScrollBar().setUnitIncrement(40);
        pnlProductList = new JPanel();
        pnlProductCard = new JPanel();
        pnlProductCard.setLayout(new BorderLayout(0, 0));
        pnlProductCard.setPreferredSize(new Dimension(482, 350));
        pnlProductList.add(pnlProductCard);
        pnlProductDescription = new JPanel();
        pnlProductDescription.setLayout(new GridBagLayout());
        pnlProductDescription.setBackground(new Color(-1));
        pnlProductDescription.setMinimumSize(new Dimension(0, 150));
        pnlProductDescription.setPreferredSize(new Dimension(0, 130));
        pnlProductCard.add(pnlProductDescription, BorderLayout.SOUTH);
        lblProductCardName = new JLabel();
        Font lblProductCardNameFont = this.$$$getFont$$$(null, -1, 16, lblProductCardName.getFont());
        if (lblProductCardNameFont != null) lblProductCardName.setFont(lblProductCardNameFont);
        lblProductCardName.setForeground(new Color(-16777216));
        lblProductCardName.setText("Tên sản phẩm: Sản phẩm A");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 10, 10, 0);
        pnlProductDescription.add(lblProductCardName, gbc);
        pnlProductAmount = new JPanel();
        pnlProductAmount.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlProductAmount.setBackground(new Color(-1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 6, 0, 0);
        pnlProductDescription.add(pnlProductAmount, gbc);
        lblProductAmount = new JLabel();
        Font lblProductAmountFont = this.$$$getFont$$$(null, -1, 16, lblProductAmount.getFont());
        if (lblProductAmountFont != null) lblProductAmount.setFont(lblProductAmountFont);
        lblProductAmount.setForeground(new Color(-16777216));
        lblProductAmount.setText("Số lượng sản phẩm: ");
        pnlProductAmount.add(lblProductAmount);
        spnProductAmount = new JSpinner();
        spnProductAmount.setBackground(new Color(-1));
        spnProductAmount.setFocusCycleRoot(false);
        Font spnProductAmountFont = this.$$$getFont$$$(null, -1, 16, spnProductAmount.getFont());
        if (spnProductAmountFont != null) spnProductAmount.setFont(spnProductAmountFont);
        spnProductAmount.setForeground(new Color(-16777216));
        spnProductAmount.setToolTipText("Điều chỉnh số lượng sản phẩm");
        pnlProductAmount.add(spnProductAmount);
        pnlProductUnitOfMeasure = new JPanel();
        pnlProductUnitOfMeasure.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlProductUnitOfMeasure.setBackground(new Color(-1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 6, 0, 0);
        pnlProductDescription.add(pnlProductUnitOfMeasure, gbc);
        lblProductUnitOfMeasure = new JLabel();
        Font lblProductUnitOfMeasureFont = this.$$$getFont$$$(null, -1, 16, lblProductUnitOfMeasure.getFont());
        if (lblProductUnitOfMeasureFont != null) lblProductUnitOfMeasure.setFont(lblProductUnitOfMeasureFont);
        lblProductUnitOfMeasure.setForeground(new Color(-16777216));
        lblProductUnitOfMeasure.setText("Đơn vị: ");
        pnlProductUnitOfMeasure.add(lblProductUnitOfMeasure);
        cmbProductUnitOfMeasure = new JComboBox();
        cmbProductUnitOfMeasure.setBackground(new Color(-1));
        cmbProductUnitOfMeasure.setFocusCycleRoot(false);
        Font cmbProductUnitOfMeasureFont = this.$$$getFont$$$(null, -1, 16, cmbProductUnitOfMeasure.getFont());
        if (cmbProductUnitOfMeasureFont != null) cmbProductUnitOfMeasure.setFont(cmbProductUnitOfMeasureFont);
        cmbProductUnitOfMeasure.setForeground(new Color(-16777216));
        cmbProductUnitOfMeasure.setToolTipText("Chọn đơn vị sản phẩm");
        pnlProductUnitOfMeasure.add(cmbProductUnitOfMeasure);
        btnAddProduct = new JButton();
        btnAddProduct.setActionCommand("Làm mới");
        btnAddProduct.setBackground(new Color(-11812950));
        btnAddProduct.setContentAreaFilled(true);
        Font btnAddProductFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnAddProduct.getFont());
        if (btnAddProductFont != null) btnAddProduct.setFont(btnAddProductFont);
        btnAddProduct.setForeground(new Color(-16777216));
        btnAddProduct.setMaximumSize(new Dimension(50, 34));
        btnAddProduct.setMinimumSize(new Dimension(50, 34));
        btnAddProduct.setPreferredSize(new Dimension(100, 28));
        btnAddProduct.setText("Thêm");
        btnAddProduct.setToolTipText("Làm mới bộ lọc");
        btnAddProduct.setToolTipText("Thêm sản phẩm vào hóa đơn nháp");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlProductDescription.add(btnAddProduct, gbc);
        pnlProductImage = new JPanel();
        pnlProductImage.setLayout(new BorderLayout(0, 0));
        pnlProductImage.setBackground(new Color(-1));
        pnlProductCard.add(pnlProductImage, BorderLayout.CENTER);
        lblProductImage = new JLabel(getResizedImage("src/main/resources/images/elixir.png", 486, 320));
        Font lblProductImageFont = this.$$$getFont$$$(null, -1, 16, lblProductImage.getFont());
        if (lblProductImageFont != null) lblProductImage.setFont(lblProductImageFont);
        lblProductImage.setForeground(new Color(-16777216));
        lblProductImage.setText("");
        pnlProductImage.add(lblProductImage, BorderLayout.CENTER);

        pnlProductCard.setVisible(false);

        for (int i = 1; i <= 20; i++) {
            JPanel pnlProductCardDraft = cloneProductCard("Product " + i);
            pnlProductList.add(pnlProductCardDraft);
        }
    }

    // java
    private JPanel cloneProductCard(String name) {
        JPanel pnlCardDraft = new JPanel(new BorderLayout(0, 0));
        pnlCardDraft.setPreferredSize(new Dimension(482, 350));
        pnlCardDraft.setBackground(Color.WHITE);

        // Image area
        JPanel pnlImageDraft = new JPanel(new BorderLayout(0, 0));
        pnlImageDraft.setBackground(Color.WHITE);
        Random random = new Random(System.currentTimeMillis());
        int potionOption = random.nextInt(5) + 1;
        ImageIcon icon = getResizedImage("src/main/resources/images/potion" + potionOption + ".png", 486, 320);
        JLabel lblImageDraft = new JLabel(icon);
        pnlImageDraft.add(lblImageDraft, BorderLayout.CENTER);
        pnlCardDraft.add(pnlImageDraft, BorderLayout.CENTER);

        // lblDescriptionription area
        JPanel lblDescriptionDraft = new JPanel(new GridBagLayout());
        lblDescriptionDraft.setBackground(Color.WHITE);
        GridBagConstraints gbc;

        JLabel lblNameDraft = new JLabel("Tên sản phẩm: " + name);
        lblNameDraft.setFont(lblProductCardName.getFont());
        lblNameDraft.setForeground(lblProductCardName.getForeground());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 10, 10, 0);
        lblDescriptionDraft.add(lblNameDraft, gbc);

        JPanel amountPanelDraft = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        amountPanelDraft.setBackground(Color.WHITE);
        JLabel lblAmountDraft = new JLabel("Số lượng sản phẩm:");
        lblAmountDraft.setFont(lblProductAmount.getFont());
        lblAmountDraft.setForeground(lblProductAmount.getForeground());
        JSpinner spnAmountDraft = new JSpinner();
        spnAmountDraft.setFont(spnProductAmount.getFont());
        spnAmountDraft.setToolTipText(spnProductAmount.getToolTipText());
        amountPanelDraft.add(lblAmountDraft);
        amountPanelDraft.add(spnAmountDraft);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 6, 0, 0);
        lblDescriptionDraft.add(amountPanelDraft, gbc);

        JPanel pnlUnitOfMeasureDraft = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlUnitOfMeasureDraft.setBackground(Color.WHITE);
        JLabel lblUnitOfMeasureDraft = new JLabel("Đơn vị:");
        lblUnitOfMeasureDraft.setFont(lblProductUnitOfMeasure.getFont());
        lblUnitOfMeasureDraft.setForeground(lblProductUnitOfMeasure.getForeground());
        JComboBox<String> cmbUnitOfMeasureDraft = new JComboBox<>(new String[]{"Viên", "Hộp", "Chai", "Tuýp", "Gói"});
        cmbUnitOfMeasureDraft.setFont(cmbProductUnitOfMeasure.getFont());
        cmbUnitOfMeasureDraft.setToolTipText(cmbProductUnitOfMeasure.getToolTipText());
        cmbUnitOfMeasureDraft.setSelectedIndex(0);
        pnlUnitOfMeasureDraft.add(lblUnitOfMeasureDraft);
        pnlUnitOfMeasureDraft.add(cmbUnitOfMeasureDraft);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 6, 0, 0);
        lblDescriptionDraft.add(pnlUnitOfMeasureDraft, gbc);

        JButton btnAddItemDraft = new JButton("Thêm");
        btnAddItemDraft.setBackground(btnAddProduct.getBackground());
        btnAddItemDraft.setForeground(btnAddProduct.getForeground());
        btnAddItemDraft.setFont(btnAddProduct.getFont());
        btnAddItemDraft.setToolTipText(btnAddProduct.getToolTipText());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lblDescriptionDraft.add(btnAddItemDraft, gbc);

        pnlCardDraft.add(lblDescriptionDraft, BorderLayout.SOUTH);
        return pnlCardDraft;
    }

    private ImageIcon getResizedImage(String path, int width, int height) {
        return new ImageIcon(new ImageIcon(path).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private JButton cloneButton(JButton template, double amount) {
        DecimalFormat decimalFormat = new DecimalFormat("###,###");

        JButton b = new JButton(decimalFormat.format(amount));
        // Copy visual settings
        b.setBackground(template.getBackground());
        b.setForeground(template.getForeground());
        b.setFont(template.getFont());
        b.setBorder(template.getBorder());
        b.setMargin(template.getMargin());
        b.setOpaque(template.isOpaque());
        b.setContentAreaFilled(template.isContentAreaFilled());
        b.setFocusPainted(template.isFocusPainted());
        b.setPreferredSize(template.getPreferredSize());
        b.setMinimumSize(template.getMinimumSize());
        b.setMaximumSize(template.getMaximumSize());
        b.setToolTipText(template.getToolTipText());
        b.setEnabled(template.isEnabled());
        return b;
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        pnlSelling = new JPanel();
        pnlSelling.setLayout(new BorderLayout(0, 0));
        pnlSelling.setBackground(new Color(-16777216));
        pnlInvoice = new JPanel();
        pnlInvoice.setLayout(new BorderLayout(0, 0));
        pnlInvoice.setAutoscrolls(false);
        pnlInvoice.setBackground(new Color(-1));
        pnlInvoice.setFocusCycleRoot(false);
        pnlInvoice.setFocusTraversalPolicyProvider(false);
        pnlInvoice.setPreferredSize(new Dimension(700, 500));
        pnlSelling.add(pnlInvoice, BorderLayout.EAST);
        pnlInvoice.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        pnlGeneralInfo = new JPanel();
        pnlGeneralInfo.setLayout(new GridBagLayout());
        pnlGeneralInfo.setAlignmentX(0.0f);
        pnlGeneralInfo.setBackground(new Color(-1));
        pnlGeneralInfo.setPreferredSize(new Dimension(245, 196));
        pnlInvoice.add(pnlGeneralInfo, BorderLayout.NORTH);
        lblTitle = new JLabel();
        lblTitle.setBackground(new Color(-1));
        Font lblTitleFont = this.$$$getFont$$$(null, Font.BOLD, 24, lblTitle.getFont());
        if (lblTitleFont != null) lblTitle.setFont(lblTitleFont);
        lblTitle.setForeground(new Color(-15185268));
        lblTitle.setHorizontalAlignment(2);
        lblTitle.setHorizontalTextPosition(2);
        lblTitle.setText("Hóa đơn bán");
        lblTitle.setVerticalAlignment(1);
        lblTitle.setVerticalTextPosition(1);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlGeneralInfo.add(lblTitle, gbc);
        txtPrescriptionCode = new JTextField();
        Font txtPrescriptionCodeFont = this.$$$getFont$$$(null, -1, 16, txtPrescriptionCode.getFont());
        if (txtPrescriptionCodeFont != null) txtPrescriptionCode.setFont(txtPrescriptionCodeFont);
        txtPrescriptionCode.setToolTipText("Điền mã đơn kê thuốc");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 13.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlGeneralInfo.add(txtPrescriptionCode, gbc);
        lblPrescriptionCode = new JLabel();
        lblPrescriptionCode.setFocusCycleRoot(false);
        lblPrescriptionCode.setFocusTraversalPolicyProvider(false);
        Font lblPrescriptionCodeFont = this.$$$getFont$$$(null, -1, 16, lblPrescriptionCode.getFont());
        if (lblPrescriptionCodeFont != null) lblPrescriptionCode.setFont(lblPrescriptionCodeFont);
        lblPrescriptionCode.setForeground(new Color(-16777216));
        lblPrescriptionCode.setHorizontalAlignment(2);
        lblPrescriptionCode.setText("Mã đơn kê thuốc:");
        lblPrescriptionCode.setVerifyInputWhenFocusTarget(true);
        lblPrescriptionCode.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlGeneralInfo.add(lblPrescriptionCode, gbc);
        lblCustomerName = new JLabel();
        lblCustomerName.setFocusCycleRoot(false);
        lblCustomerName.setFocusTraversalPolicyProvider(false);
        Font lblCustomerNameFont = this.$$$getFont$$$(null, -1, 16, lblCustomerName.getFont());
        if (lblCustomerNameFont != null) lblCustomerName.setFont(lblCustomerNameFont);
        lblCustomerName.setForeground(new Color(-16777216));
        lblCustomerName.setHorizontalAlignment(2);
        lblCustomerName.setText("Tên khách hàng:");
        lblCustomerName.setVerifyInputWhenFocusTarget(true);
        lblCustomerName.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlGeneralInfo.add(lblCustomerName, gbc);
        txtCustomerName = new JTextField();
        Font txtCustomerNameFont = this.$$$getFont$$$(null, -1, 16, txtCustomerName.getFont());
        if (txtCustomerNameFont != null) txtCustomerName.setFont(txtCustomerNameFont);
        txtCustomerName.setToolTipText("Điền tên khách hàng");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlGeneralInfo.add(txtCustomerName, gbc);
        txtCustomerTeleNo = new JTextField();
        Font txtCustomerTeleNoFont = this.$$$getFont$$$(null, -1, 16, txtCustomerTeleNo.getFont());
        if (txtCustomerTeleNoFont != null) txtCustomerTeleNo.setFont(txtCustomerTeleNoFont);
        txtCustomerTeleNo.setToolTipText("Điền số điện thoại khách hàng");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlGeneralInfo.add(txtCustomerTeleNo, gbc);
        lblCustomerTeleNo = new JLabel();
        lblCustomerTeleNo.setFocusCycleRoot(false);
        lblCustomerTeleNo.setFocusTraversalPolicyProvider(false);
        Font lblCustomerTeleNoFont = this.$$$getFont$$$(null, -1, 16, lblCustomerTeleNo.getFont());
        if (lblCustomerTeleNoFont != null) lblCustomerTeleNo.setFont(lblCustomerTeleNoFont);
        lblCustomerTeleNo.setForeground(new Color(-16777216));
        lblCustomerTeleNo.setHorizontalAlignment(2);
        lblCustomerTeleNo.setText("Số điện thoại khách hàng:");
        lblCustomerTeleNo.setVerifyInputWhenFocusTarget(true);
        lblCustomerTeleNo.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlGeneralInfo.add(lblCustomerTeleNo, gbc);
        lblCustomerAddress = new JLabel();
        lblCustomerAddress.setFocusCycleRoot(false);
        lblCustomerAddress.setFocusTraversalPolicyProvider(false);
        Font lblCustomerAddressFont = this.$$$getFont$$$(null, -1, 16, lblCustomerAddress.getFont());
        if (lblCustomerAddressFont != null) lblCustomerAddress.setFont(lblCustomerAddressFont);
        lblCustomerAddress.setForeground(new Color(-16777216));
        lblCustomerAddress.setHorizontalAlignment(2);
        lblCustomerAddress.setText("Địa chỉ khách hàng:");
        lblCustomerAddress.setVerifyInputWhenFocusTarget(true);
        lblCustomerAddress.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        pnlGeneralInfo.add(lblCustomerAddress, gbc);
        txtCustomerAdress = new JTextField();
        Font txtCustomerAdressFont = this.$$$getFont$$$(null, -1, 16, txtCustomerAdress.getFont());
        if (txtCustomerAdressFont != null) txtCustomerAdress.setFont(txtCustomerAdressFont);
        txtCustomerAdress.setText("");
        txtCustomerAdress.setToolTipText("Điền địa chỉ khách hàng");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        pnlGeneralInfo.add(txtCustomerAdress, gbc);
        scrtblInvoiceLine = new JScrollPane();
        Font scrtblInvoiceLineFont = this.$$$getFont$$$(null, -1, 16, scrtblInvoiceLine.getFont());
        if (scrtblInvoiceLineFont != null) scrtblInvoiceLine.setFont(scrtblInvoiceLineFont);
        pnlInvoice.add(scrtblInvoiceLine, BorderLayout.CENTER);
        tblInvoiceLine.setAutoCreateColumnsFromModel(true);
        tblInvoiceLine.setCellSelectionEnabled(false);
        tblInvoiceLine.setColumnSelectionAllowed(false);
        tblInvoiceLine.setDropMode(DropMode.USE_SELECTION);
        tblInvoiceLine.setEnabled(true);
        Font tblInvoiceLineFont = this.$$$getFont$$$(null, -1, 16, tblInvoiceLine.getFont());
        if (tblInvoiceLineFont != null) tblInvoiceLine.setFont(tblInvoiceLineFont);
        tblInvoiceLine.setName("test");
        tblInvoiceLine.setRowSelectionAllowed(true);
        scrtblInvoiceLine.setViewportView(tblInvoiceLine);
        pnlBillingInfo = new JPanel();
        pnlBillingInfo.setLayout(new GridBagLayout());
        pnlBillingInfo.setAlignmentX(0.0f);
        pnlBillingInfo.setBackground(new Color(-1));
        pnlInvoice.add(pnlBillingInfo, BorderLayout.SOUTH);
        lblPromotionID = new JLabel();
        lblPromotionID.setFocusCycleRoot(false);
        lblPromotionID.setFocusTraversalPolicyProvider(false);
        Font lblPromotionIDFont = this.$$$getFont$$$(null, -1, 16, lblPromotionID.getFont());
        if (lblPromotionIDFont != null) lblPromotionID.setFont(lblPromotionIDFont);
        lblPromotionID.setForeground(new Color(-16777216));
        lblPromotionID.setHorizontalAlignment(2);
        lblPromotionID.setText("Mã khuyến mãi:");
        lblPromotionID.setVerifyInputWhenFocusTarget(true);
        lblPromotionID.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 0);
        pnlBillingInfo.add(lblPromotionID, gbc);
        lblVAT = new JLabel();
        lblVAT.setFocusCycleRoot(false);
        lblVAT.setFocusTraversalPolicyProvider(false);
        Font lblVATFont = this.$$$getFont$$$(null, -1, 16, lblVAT.getFont());
        if (lblVATFont != null) lblVAT.setFont(lblVATFont);
        lblVAT.setForeground(new Color(-16777216));
        lblVAT.setHorizontalAlignment(2);
        lblVAT.setText("VAT:");
        lblVAT.setVerifyInputWhenFocusTarget(true);
        lblVAT.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        pnlBillingInfo.add(lblVAT, gbc);
        lblVATNumber = new JLabel();
        Font lblVATNumberFont = this.$$$getFont$$$(null, -1, 16, lblVATNumber.getFont());
        if (lblVATNumberFont != null) lblVATNumber.setFont(lblVATNumberFont);
        lblVATNumber.setText("");
        lblVATNumber.setToolTipText("Số thuế hóa đơn");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        pnlBillingInfo.add(lblVATNumber, gbc);
        lblTotalNumber = new JLabel();
        Font lblTotalNumberFont = this.$$$getFont$$$(null, -1, 16, lblTotalNumber.getFont());
        if (lblTotalNumberFont != null) lblTotalNumber.setFont(lblTotalNumberFont);
        lblTotalNumber.setText("");
        lblTotalNumber.setToolTipText("Tổng tiền hóa đơn");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        pnlBillingInfo.add(lblTotalNumber, gbc);
        lblTotal = new JLabel();
        lblTotal.setFocusCycleRoot(false);
        lblTotal.setFocusTraversalPolicyProvider(false);
        Font lblTotalFont = this.$$$getFont$$$(null, -1, 16, lblTotal.getFont());
        if (lblTotalFont != null) lblTotal.setFont(lblTotalFont);
        lblTotal.setForeground(new Color(-16777216));
        lblTotal.setHorizontalAlignment(2);
        lblTotal.setText("Tổng tiền:");
        lblTotal.setVerifyInputWhenFocusTarget(true);
        lblTotal.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        pnlBillingInfo.add(lblTotal, gbc);
        lblPayment = new JLabel();
        lblPayment.setFocusCycleRoot(false);
        lblPayment.setFocusTraversalPolicyProvider(false);
        Font lblPaymentFont = this.$$$getFont$$$(null, -1, 16, lblPayment.getFont());
        if (lblPaymentFont != null) lblPayment.setFont(lblPaymentFont);
        lblPayment.setForeground(new Color(-16777216));
        lblPayment.setHorizontalAlignment(2);
        lblPayment.setText("Phương thức thanh toán:");
        lblPayment.setVerifyInputWhenFocusTarget(true);
        lblPayment.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);
        pnlBillingInfo.add(lblPayment, gbc);
        radCash.setBackground(new Color(-1));
        Font radCashFont = this.$$$getFont$$$(null, -1, 16, radCash.getFont());
        if (radCashFont != null) radCash.setFont(radCashFont);
        radCash.setForeground(new Color(-16777216));
        radCash.setSelected(true);
        radCash.setText("Tiền mặt");
        radCash.setToolTipText("Thanh toán tiền mặt");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);
        pnlBillingInfo.add(radCash, gbc);
        radBank.setBackground(new Color(-1));
        Font radBankFont = this.$$$getFont$$$(null, -1, 16, radBank.getFont());
        if (radBankFont != null) radBank.setFont(radBankFont);
        radBank.setForeground(new Color(-16777216));
        radBank.setText("Ngân hàng/Ví điện tử");
        radBank.setToolTipText("Thanh toán chuyển khoản");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.weightx = 1.5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);
        pnlBillingInfo.add(radBank, gbc);
        txtPromotionID = new JTextField();
        Font txtPromotionIDFont = this.$$$getFont$$$(null, -1, 16, txtPromotionID.getFont());
        if (txtPromotionIDFont != null) txtPromotionID.setFont(txtPromotionIDFont);
        txtPromotionID.setToolTipText("Điền mã khuyến mãi");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        pnlBillingInfo.add(txtPromotionID, gbc);
        lblPromotionDetail = new JLabel();
        lblPromotionDetail.setFocusCycleRoot(false);
        lblPromotionDetail.setFocusTraversalPolicyProvider(false);
        Font lblPromotionDetailFont = this.$$$getFont$$$(null, -1, 16, lblPromotionDetail.getFont());
        if (lblPromotionDetailFont != null) lblPromotionDetail.setFont(lblPromotionDetailFont);
        lblPromotionDetail.setForeground(new Color(-16777216));
        lblPromotionDetail.setHorizontalAlignment(2);
        lblPromotionDetail.setText("Nội dung khuyến mãi:");
        lblPromotionDetail.setVerifyInputWhenFocusTarget(true);
        lblPromotionDetail.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        pnlBillingInfo.add(lblPromotionDetail, gbc);
        lblPromotionDetailText = new JLabel();
        Font lblPromotionDetailTextFont = this.$$$getFont$$$(null, -1, 16, lblPromotionDetailText.getFont());
        if (lblPromotionDetailTextFont != null) lblPromotionDetailText.setFont(lblPromotionDetailTextFont);
        lblPromotionDetailText.setText("");
        lblPromotionDetailText.setToolTipText("Nội dung khuyến mãi");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        pnlBillingInfo.add(lblPromotionDetailText, gbc);
        lblCashPaid = new JLabel();
        Font lblCashPaidFont = this.$$$getFont$$$(null, -1, 16, lblCashPaid.getFont());
        if (lblCashPaidFont != null) lblCashPaid.setFont(lblCashPaidFont);
        lblCashPaid.setForeground(new Color(-16777216));
        lblCashPaid.setHorizontalAlignment(4);
        lblCashPaid.setHorizontalTextPosition(2);
        lblCashPaid.setText("Tiền khách đưa:");
        lblCashPaid.setVerticalAlignment(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlBillingInfo.add(lblCashPaid, gbc);
        btnCreateInvoice = new JButton();
        btnCreateInvoice.setBackground(new Color(-11812950));
        Font btnCreateInvoiceFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnCreateInvoice.getFont());
        if (btnCreateInvoiceFont != null) btnCreateInvoice.setFont(btnCreateInvoiceFont);
        btnCreateInvoice.setForeground(new Color(-16777216));
        btnCreateInvoice.setMaximumSize(new Dimension(50, 34));
        btnCreateInvoice.setMinimumSize(new Dimension(50, 34));
        btnCreateInvoice.setPreferredSize(new Dimension(50, 50));
        btnCreateInvoice.setText("Tạo hóa đơn");
        btnCreateInvoice.setToolTipText("Tạo hóa đơn");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        pnlBillingInfo.add(btnCreateInvoice, gbc);
        pnlCashPayment.setLayout(new BorderLayout(0, 0));
        pnlCashPayment.setBackground(new Color(-1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        pnlBillingInfo.add(pnlCashPayment, gbc);
        scrCashAmountList.setHorizontalScrollBarPolicy(31);
        scrCashAmountList.setPreferredSize(new Dimension(100, 150));
        scrCashAmountList.setVerticalScrollBarPolicy(20);
        pnlCashPayment.add(scrCashAmountList, BorderLayout.CENTER);
        pnlCashAmountList.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlCashAmountList.setBackground(new Color(-1));
        pnlCashAmountList.setPreferredSize(new Dimension(100, 500));
        scrCashAmountList.setViewportView(pnlCashAmountList);
        btnCashAmount.setBackground(new Color(-11812950));
        Font btnCashAmountFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnCashAmount.getFont());
        if (btnCashAmountFont != null) btnCashAmount.setFont(btnCashAmountFont);
        btnCashAmount.setForeground(new Color(-16777216));
        btnCashAmount.setMaximumSize(new Dimension(200, 34));
        btnCashAmount.setPreferredSize(new Dimension(127, 50));
        btnCashAmount.setText("100,000");
        btnCashAmount.setToolTipText("Chọn giá trị tiền khách đưa");
        pnlCashAmountList.add(btnCashAmount);
        btnDeleteInvoiceLine.setActionCommand("Xóa chi tiết hóa đơn");
        btnDeleteInvoiceLine.setBackground(new Color(-11812950));
        btnDeleteInvoiceLine.setContentAreaFilled(true);
        Font btnDeleteInvoiceLineFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnDeleteInvoiceLine.getFont());
        if (btnDeleteInvoiceLineFont != null) btnDeleteInvoiceLine.setFont(btnDeleteInvoiceLineFont);
        btnDeleteInvoiceLine.setForeground(new Color(-16777216));
        btnDeleteInvoiceLine.setMaximumSize(new Dimension(50, 34));
        btnDeleteInvoiceLine.setMinimumSize(new Dimension(50, 34));
        btnDeleteInvoiceLine.setPreferredSize(new Dimension(50, 50));
        btnDeleteInvoiceLine.setText("Xóa chi tiết hóa đơn");
        btnDeleteInvoiceLine.setToolTipText("Xóa chi tiết hóa đơn được chọn");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.weightx = 3.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        pnlBillingInfo.add(btnDeleteInvoiceLine, gbc);
        txtCashPaidAmount.setAutoscrolls(true);
        txtCashPaidAmount.setBackground(new Color(-1));
        txtCashPaidAmount.setDisabledTextColor(new Color(-16777216));
        Font txtCashPaidAmountFont = this.$$$getFont$$$(null, -1, 16, txtCashPaidAmount.getFont());
        if (txtCashPaidAmountFont != null) txtCashPaidAmount.setFont(txtCashPaidAmountFont);
        txtCashPaidAmount.setForeground(new Color(-16777216));
        txtCashPaidAmount.setToolTipText("Số tiền khách đưa");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlBillingInfo.add(txtCashPaidAmount, gbc);
        pnlProduct = new JPanel();
        pnlProduct.setLayout(new BorderLayout(0, 0));
        pnlProduct.setBackground(new Color(-1));
        pnlSelling.add(pnlProduct, BorderLayout.CENTER);
        pnlProduct.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        pnlProductSort = new JPanel();
        pnlProductSort.setLayout(new GridBagLayout());
        pnlProductSort.setBackground(new Color(-1));
        pnlProductSort.setPreferredSize(new Dimension(0, 170));
        pnlProduct.add(pnlProductSort, BorderLayout.NORTH);
        lblProductID = new JLabel();
        Font lblProductIDFont = this.$$$getFont$$$(null, -1, 16, lblProductID.getFont());
        if (lblProductIDFont != null) lblProductID.setFont(lblProductIDFont);
        lblProductID.setForeground(new Color(-16777216));
        lblProductID.setHorizontalAlignment(2);
        lblProductID.setHorizontalTextPosition(2);
        lblProductID.setText("Mã sản phẩm:");
        lblProductID.setVerticalAlignment(1);
        lblProductID.setVerticalTextPosition(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlProductSort.add(lblProductID, gbc);
        txtProductID = new JTextField();
        txtProductID.setBackground(new Color(-1));
        Font txtProductIDFont = this.$$$getFont$$$(null, -1, 16, txtProductID.getFont());
        if (txtProductIDFont != null) txtProductID.setFont(txtProductIDFont);
        txtProductID.setText("");
        txtProductID.setToolTipText("Điền mã sản phẩm");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        pnlProductSort.add(txtProductID, gbc);
        lblProductName = new JLabel();
        Font lblProductNameFont = this.$$$getFont$$$(null, -1, 16, lblProductName.getFont());
        if (lblProductNameFont != null) lblProductName.setFont(lblProductNameFont);
        lblProductName.setForeground(new Color(-16777216));
        lblProductName.setHorizontalAlignment(2);
        lblProductName.setHorizontalTextPosition(2);
        lblProductName.setText("Tên sản phẩm:");
        lblProductName.setVerticalAlignment(1);
        lblProductName.setVerticalTextPosition(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlProductSort.add(lblProductName, gbc);
        txtProductName = new JTextField();
        txtProductName.setBackground(new Color(-1));
        Font txtProductNameFont = this.$$$getFont$$$(null, -1, 16, txtProductName.getFont());
        if (txtProductNameFont != null) txtProductName.setFont(txtProductNameFont);
        txtProductName.setText("");
        txtProductName.setToolTipText("Điền tên sản phẩm");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 3.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlProductSort.add(txtProductName, gbc);
        lblManufacturer = new JLabel();
        Font lblManufacturerFont = this.$$$getFont$$$(null, -1, 16, lblManufacturer.getFont());
        if (lblManufacturerFont != null) lblManufacturer.setFont(lblManufacturerFont);
        lblManufacturer.setForeground(new Color(-16777216));
        lblManufacturer.setHorizontalAlignment(2);
        lblManufacturer.setHorizontalTextPosition(2);
        lblManufacturer.setText("Nhà sản xuất:");
        lblManufacturer.setVerticalAlignment(1);
        lblManufacturer.setVerticalTextPosition(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlProductSort.add(lblManufacturer, gbc);
        txtManufacturer = new JTextField();
        txtManufacturer.setBackground(new Color(-1));
        Font txtManufacturerFont = this.$$$getFont$$$(null, -1, 16, txtManufacturer.getFont());
        if (txtManufacturerFont != null) txtManufacturer.setFont(txtManufacturerFont);
        txtManufacturer.setText("");
        txtManufacturer.setToolTipText("Điền nhà sản xuất sản phẩm");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        pnlProductSort.add(txtManufacturer, gbc);
        lblShortname = new JLabel();
        Font lblShortnameFont = this.$$$getFont$$$(null, -1, 16, lblShortname.getFont());
        if (lblShortnameFont != null) lblShortname.setFont(lblShortnameFont);
        lblShortname.setForeground(new Color(-16777216));
        lblShortname.setHorizontalAlignment(2);
        lblShortname.setHorizontalTextPosition(2);
        lblShortname.setText("Tên viết tắt:");
        lblShortname.setVerticalAlignment(1);
        lblShortname.setVerticalTextPosition(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlProductSort.add(lblShortname, gbc);
        txtShortname = new JTextField();
        txtShortname.setBackground(new Color(-1));
        Font txtShortnameFont = this.$$$getFont$$$(null, -1, 16, txtShortname.getFont());
        if (txtShortnameFont != null) txtShortname.setFont(txtShortnameFont);
        txtShortname.setText("");
        txtShortname.setToolTipText("Điền tên viết tắt của sản phẩm");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlProductSort.add(txtShortname, gbc);
        lblCategory = new JLabel();
        Font lblCategoryFont = this.$$$getFont$$$(null, -1, 16, lblCategory.getFont());
        if (lblCategoryFont != null) lblCategory.setFont(lblCategoryFont);
        lblCategory.setForeground(new Color(-16777216));
        lblCategory.setHorizontalAlignment(2);
        lblCategory.setHorizontalTextPosition(2);
        lblCategory.setText("Loại thuốc:");
        lblCategory.setVerticalAlignment(1);
        lblCategory.setVerticalTextPosition(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlProductSort.add(lblCategory, gbc);
        cmbCategory.setBackground(new Color(-1));
        Font cmbCategoryFont = this.$$$getFont$$$(null, -1, 16, cmbCategory.getFont());
        if (cmbCategoryFont != null) cmbCategory.setFont(cmbCategoryFont);
        cmbCategory.setForeground(new Color(-16777216));
        cmbCategory.setToolTipText("Chọn loại thuốc");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        pnlProductSort.add(cmbCategory, gbc);
        lblForm = new JLabel();
        Font lblFormFont = this.$$$getFont$$$(null, -1, 16, lblForm.getFont());
        if (lblFormFont != null) lblForm.setFont(lblFormFont);
        lblForm.setForeground(new Color(-16777216));
        lblForm.setHorizontalAlignment(2);
        lblForm.setHorizontalTextPosition(2);
        lblForm.setText("Dạng bào chế:");
        lblForm.setVerticalAlignment(1);
        lblForm.setVerticalTextPosition(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlProductSort.add(lblForm, gbc);
        cmbForm.setBackground(new Color(-1));
        Font cmbFormFont = this.$$$getFont$$$(null, -1, 16, cmbForm.getFont());
        if (cmbFormFont != null) cmbForm.setFont(cmbFormFont);
        cmbForm.setForeground(new Color(-16777216));
        cmbForm.setToolTipText("Chọn dạng bào chế");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlProductSort.add(cmbForm, gbc);
        btnFilterProducts = new JButton();
        btnFilterProducts.setBackground(new Color(-11812950));
        Font btnFilterProductsFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnFilterProducts.getFont());
        if (btnFilterProductsFont != null) btnFilterProducts.setFont(btnFilterProductsFont);
        btnFilterProducts.setForeground(new Color(-16777216));
        btnFilterProducts.setMaximumSize(new Dimension(50, 34));
        btnFilterProducts.setMinimumSize(new Dimension(50, 34));
        btnFilterProducts.setPreferredSize(new Dimension(50, 50));
        btnFilterProducts.setText("Lọc sản phẩm");
        btnFilterProducts.setToolTipText("Lọc danh sách sản phẩm");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 0, 0);
        pnlProductSort.add(btnFilterProducts, gbc);
        btnRefresh.setActionCommand("Làm mới");
        btnRefresh.setBackground(new Color(-11812950));
        btnRefresh.setContentAreaFilled(true);
        Font btnRefreshFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnRefresh.getFont());
        if (btnRefreshFont != null) btnRefresh.setFont(btnRefreshFont);
        btnRefresh.setForeground(new Color(-16777216));
        btnRefresh.setMaximumSize(new Dimension(50, 34));
        btnRefresh.setMinimumSize(new Dimension(50, 34));
        btnRefresh.setPreferredSize(new Dimension(50, 50));
        btnRefresh.setText("Làm mới");
        btnRefresh.setToolTipText("Làm mới bộ lọc");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        pnlProductSort.add(btnRefresh, gbc);
        scrProductList.setBackground(new Color(-1));
        scrProductList.setHorizontalScrollBarPolicy(31);
        scrProductList.setOpaque(true);
        pnlProduct.add(scrProductList, BorderLayout.CENTER);
        pnlProductList.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlProductList.setAutoscrolls(false);
        pnlProductList.setBackground(new Color(-1));
        pnlProductList.setPreferredSize(new Dimension(950, 10000));
        scrProductList.setViewportView(pnlProductList);
        pnlProductCard.setLayout(new BorderLayout(0, 0));
        pnlProductCard.setPreferredSize(new Dimension(482, 350));
        pnlProductList.add(pnlProductCard);
        pnlProductDescription.setLayout(new GridBagLayout());
        pnlProductDescription.setBackground(new Color(-1));
        pnlProductDescription.setMinimumSize(new Dimension(0, 150));
        pnlProductDescription.setPreferredSize(new Dimension(0, 130));
        pnlProductCard.add(pnlProductDescription, BorderLayout.SOUTH);
        Font lblProductCardNameFont = this.$$$getFont$$$(null, -1, 16, lblProductCardName.getFont());
        if (lblProductCardNameFont != null) lblProductCardName.setFont(lblProductCardNameFont);
        lblProductCardName.setForeground(new Color(-16777216));
        lblProductCardName.setText("Tên sản phẩm: Sản phẩm A");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 10, 10, 0);
        pnlProductDescription.add(lblProductCardName, gbc);
        pnlProductAmount.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlProductAmount.setBackground(new Color(-1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 6, 0, 0);
        pnlProductDescription.add(pnlProductAmount, gbc);
        Font lblProductAmountFont = this.$$$getFont$$$(null, -1, 16, lblProductAmount.getFont());
        if (lblProductAmountFont != null) lblProductAmount.setFont(lblProductAmountFont);
        lblProductAmount.setForeground(new Color(-16777216));
        lblProductAmount.setText("Số lượng sản phẩm: ");
        pnlProductAmount.add(lblProductAmount);
        spnProductAmount.setBackground(new Color(-1));
        spnProductAmount.setFocusCycleRoot(false);
        Font spnProductAmountFont = this.$$$getFont$$$(null, -1, 16, spnProductAmount.getFont());
        if (spnProductAmountFont != null) spnProductAmount.setFont(spnProductAmountFont);
        spnProductAmount.setForeground(new Color(-16777216));
        spnProductAmount.setToolTipText("Điều chỉnh số lượng sản phẩm");
        pnlProductAmount.add(spnProductAmount);
        pnlProductUnitOfMeasure.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlProductUnitOfMeasure.setBackground(new Color(-1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 6, 0, 0);
        pnlProductDescription.add(pnlProductUnitOfMeasure, gbc);
        Font lblProductUnitOfMeasureFont = this.$$$getFont$$$(null, -1, 16, lblProductUnitOfMeasure.getFont());
        if (lblProductUnitOfMeasureFont != null) lblProductUnitOfMeasure.setFont(lblProductUnitOfMeasureFont);
        lblProductUnitOfMeasure.setForeground(new Color(-16777216));
        lblProductUnitOfMeasure.setText("Đơn vị: ");
        pnlProductUnitOfMeasure.add(lblProductUnitOfMeasure);
        cmbProductUnitOfMeasure.setBackground(new Color(-1));
        cmbProductUnitOfMeasure.setFocusCycleRoot(false);
        Font cmbProductUnitOfMeasureFont = this.$$$getFont$$$(null, -1, 16, cmbProductUnitOfMeasure.getFont());
        if (cmbProductUnitOfMeasureFont != null) cmbProductUnitOfMeasure.setFont(cmbProductUnitOfMeasureFont);
        cmbProductUnitOfMeasure.setForeground(new Color(-16777216));
        cmbProductUnitOfMeasure.setToolTipText("Chọn đơn vị sản phẩm");
        pnlProductUnitOfMeasure.add(cmbProductUnitOfMeasure);
        btnAddProduct.setActionCommand("Làm mới");
        btnAddProduct.setBackground(new Color(-11812950));
        btnAddProduct.setContentAreaFilled(true);
        Font btnAddProductFont = this.$$$getFont$$$(null, Font.BOLD, 16, btnAddProduct.getFont());
        if (btnAddProductFont != null) btnAddProduct.setFont(btnAddProductFont);
        btnAddProduct.setForeground(new Color(-16777216));
        btnAddProduct.setMaximumSize(new Dimension(50, 34));
        btnAddProduct.setMinimumSize(new Dimension(50, 34));
        btnAddProduct.setPreferredSize(new Dimension(100, 28));
        btnAddProduct.setText("Thêm");
        btnAddProduct.setToolTipText("Thêm sản phẩm vào hóa đơn nháp");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlProductDescription.add(btnAddProduct, gbc);
        pnlProductImage.setLayout(new BorderLayout(0, 0));
        pnlProductImage.setBackground(new Color(-1));
        pnlProductCard.add(pnlProductImage, BorderLayout.CENTER);
        Font lblProductImageFont = this.$$$getFont$$$(null, -1, 16, lblProductImage.getFont());
        if (lblProductImageFont != null) lblProductImage.setFont(lblProductImageFont);
        lblProductImage.setForeground(new Color(-16777216));
        lblProductImage.setIcon(new ImageIcon(getClass().getResource("/icons/btn_customer.png")));
        lblProductImage.setText("");
        pnlProductImage.add(lblProductImage, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pnlSelling;
    }

}
