package com.gui;

import com.bus.BUS_Product;
import com.entities.Lot;
import com.entities.MeasurementName;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.enums.DosageForm;
import com.enums.LotStatus;
import com.enums.ProductCategory;
import com.utils.AppColors;
import com.utils.ProductIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Giao diện quản lý Sản phẩm (UI/UX tương tự TAB_Promotion)
 * - Trái: danh sách sản phẩm + phân trang
 * - Phải: chi tiết sản phẩm (thông tin + bảng Đơn vị quy đổi + bảng Lô/HSD)
 * - Trên cùng: thanh tìm kiếm + lọc + Thêm mới + Xuất CSV
 * - Dưới cùng (khung phải): 2 nút chính theo ngữ cảnh: Cập nhật/Thêm mới + Hủy
 */
public class TAB_Product extends JPanel {

    // ==== Services ====
    private final BUS_Product productBUS = new BUS_Product();
    private final ProductIO productIO = new ProductIO();

    // ==== List & pagination ====
    private JTable table; // left list
    private DefaultTableModel productModel;
    private final java.util.List<Product> allProducts = new ArrayList<>();
    private final java.util.List<Product> pageCache = new ArrayList<>();
    private int currentPage = 0;
    private int itemsPerPage = 10;
    private JLabel lblPageInfo;
    private JButton btnPrevPage, btnNextPage;
    private JComboBox<Integer> cbPageSize;

    // ==== Toolbar ====
    private JTextField txtSearch;
    private JComboBox<String> cbCategory, cbForm;
    private JButton btnExportCSV, btnImportCSV, btnAddTop, btnSearch, btnRefresh;

    // ==== Detail (right) fields ====
    private JTextField txtId, txtName, txtShortName, txtBarcode, txtActiveIngredient, txtManufacturer, txtStrength;
    private JComboBox<String> cbCategoryDetail, cbFormDetail;
    private JComboBox<MeasurementName> cbBaseUom;
    private JSpinner spVat;
    private JTextArea txtDescription;

    // ==== UOM & Lot tables ====
    private JTable tblUom, tblLot;
    private ToggleEditableTableModel uomModel, lotModel;
    private JPanel uomFooterBar, lotFooterBar;
    private JButton btnUomAdd, btnUomDelete, btnLotAdd, btnLotDelete;

    // ==== State ====
    private boolean isViewing = false;
    private boolean isAdding = false;
    private boolean isEditingFields = false;
    private boolean addConfirmPending = false;
    private String currentEditingId = null;

    // Guards to prevent recursive/auto-add reentrancy
    private boolean uomAutoChanging = false;
    private boolean lotAutoChanging = false;

    // ==== Bottom buttons ====
    private JButton bottomPrimaryButton; // Cập nhật/Thêm mới
    private JButton bottomSecondaryButton; // Hủy

    // ==== Data helpers ====
    private final java.util.List<MeasurementName> allMeasurementNames = new ArrayList<>();

    // ==== Column indexes (avoid magic numbers) ====
    private static final int UOM_COL_ID = 0, UOM_COL_NAME = 1, UOM_COL_RATE = 2;
    // Add hidden ID column for lots at index 0
    private static final int LOT_COL_HIDDEN_ID = 0, LOT_COL_ID = 1, LOT_COL_QTY = 2, LOT_COL_PRICE = 3, LOT_COL_HSD = 4, LOT_COL_STAT = 5;

    // Add filter panels like TAB_Promotion
    private JPanel createFilterPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(6, 6, 6, 12));
        left.setBackground(new Color(245, 250, 250));

        JPanel categoryCard = createFilterCard("Loại", new String[]{"Tất cả","Thuốc kê đơn","Thuốc không kê đơn","Sản phẩm chức năng"}, true);
        JPanel formCard = createFilterCard("Dạng bào chế", new String[]{"Tất cả","Viên nén","Viên nang","Thuốc bột","Kẹo ngậm","Si rô","Thuốc nhỏ giọt","Súc miệng"}, false);

        left.add(categoryCard);
        left.add(Box.createVerticalStrut(12));
        left.add(formCard);
        left.add(Box.createVerticalGlue());
        return left;
    }

    private JPanel createFilterCard(String title, String[] options, boolean isCategory) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 240)),
                new EmptyBorder(10, 10, 10, 10)
        ));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(AppColors.PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(8));
        ButtonGroup group = new ButtonGroup();
        for (String opt : options) {
            JRadioButton radio = new JRadioButton(opt);
            radio.setBackground(Color.WHITE);
            radio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (opt.equals("Tất cả")) radio.setSelected(true);
            radio.addActionListener(e -> {
                // update toolbar combo boxes to reflect filter selection
                if (isCategory) cbCategory.setSelectedItem(opt);
                else cbForm.setSelectedItem(opt);
                doSearch();
            });
            group.add(radio);
            card.add(radio);
            card.add(Box.createVerticalStrut(4));
        }
        return card;
    }

    // Replace center layout to 1:4:5 split (filter:list:detail)
    private void rebuildCenterLayout() {
        JSplitPane leftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftRight.setDividerSize(6);
        leftRight.setBackground(new Color(245, 250, 250));

        JPanel leftFilter = createFilterPanel();
        JComponent listPanel = createListPanel();
        JComponent rightDetail = createRightDetail();

        // Split filter and list (1:4)
        JSplitPane filterList = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        filterList.setDividerSize(6);
        filterList.setBackground(new Color(245, 250, 250));
        filterList.setLeftComponent(leftFilter);
        filterList.setRightComponent(listPanel);
        filterList.setResizeWeight(0.2); // 1/(1+4) = 0.2 for filter

        leftRight.setLeftComponent(filterList);
        leftRight.setRightComponent(rightDetail);
        leftRight.setResizeWeight(0.5); // (1+4)/(1+4+5) = 0.5 for left side

        removeAll();
        add(createToolbar(), BorderLayout.NORTH);
        add(leftRight, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    public TAB_Product() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 250, 250));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        loadMeasurementNames();

        add(createToolbar(), BorderLayout.NORTH);

        // Instead of previous split, use three-pane split
        rebuildCenterLayout();
        loadProducts();
        showPlaceholder();
    }

    private void loadMeasurementNames() {
        java.util.List<MeasurementName> list = productBUS.getAllMeasurementNames();
        allMeasurementNames.clear();
        if (list != null) allMeasurementNames.addAll(list);
    }

    // ===================== Toolbar =====================
    private JComponent createToolbar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(new Color(245, 250, 250));
        top.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "QUẢN LÝ SẢN PHẨM", 0, 0, new Font("Segoe UI", Font.BOLD, 16), AppColors.PRIMARY));

        txtSearch = new JTextField(18);
        btnSearch = new JButton("Tìm");
        btnRefresh = new JButton("Làm mới");
        cbCategory = new JComboBox<>(new String[]{"Tất cả","Thuốc kê đơn","Thuốc không kê đơn","Sản phẩm chức năng"});
        cbForm = new JComboBox<>(new String[]{"Tất cả","Viên nén","Viên nang","Thuốc bột","Kẹo ngậm","Si rô","Thuốc nhỏ giọt","Súc miệng"});
        cbCategory.setSelectedIndex(0);
        cbForm.setSelectedIndex(0);

        btnExportCSV = new JButton("Xuất CSV");
        btnImportCSV = new JButton("Nhập CSV");
        btnAddTop = new JButton("Thêm mới");

        styleButton(btnSearch, AppColors.PRIMARY);
        styleButton(btnRefresh, AppColors.PRIMARY);
        styleButton(btnExportCSV, AppColors.PRIMARY);
        styleButton(btnImportCSV, AppColors.PRIMARY);
        styleButton(btnAddTop, new Color(40, 167, 69));

        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cbCategory.setSelectedIndex(0);
            cbForm.setSelectedIndex(0);
            loadProducts();
            handleCancelAll();
        });
        btnAddTop.addActionListener(e -> startAddMode());
        btnExportCSV.addActionListener(e -> exportProductsToCSV());
        btnImportCSV.addActionListener(e -> importProductsFromCSV());

        top.add(new JLabel("Tìm kiếm:")); top.add(txtSearch); top.add(btnSearch); top.add(btnRefresh);
        top.add(new JLabel("Loại:")); top.add(cbCategory);
        top.add(new JLabel("Dạng:")); top.add(cbForm);
        top.add(btnExportCSV); top.add(btnImportCSV); top.add(btnAddTop);
        return top;
    }

    // ===================== Left list =====================
    private JComponent createListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 250, 250));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Danh sách sản phẩm", 0, 0, new Font("Segoe UI", Font.BOLD, 14), AppColors.PRIMARY));

        productModel = new DefaultTableModel(new String[]{"Mã","Tên","Loại","Hoạt chất","Nhà sản xuất"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        table = new JTable(productModel);
        table.setEnabled(true);
        styleTable(table, true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0 && row < pageCache.size()) showProductDetails(pageCache.get(row));
        });
        // Allow delete key to remove selected row in list if needed
        table.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int row = table.getSelectedRow();
                    if (row >= 0) productModel.removeRow(row);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 230, 240)));
        panel.add(scroll, BorderLayout.CENTER);

        // Pagination
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pagination.setBackground(new Color(245, 250, 250));
        pagination.setBorder(new EmptyBorder(10, 0, 0, 0));
        btnPrevPage = new JButton("« Trang trước");
        btnNextPage = new JButton("Trang tiếp »");
        cbPageSize = new JComboBox<>(new Integer[]{5, 10, 20, 50});
        lblPageInfo = new JLabel();
        styleButton(btnPrevPage, AppColors.PRIMARY);
        styleButton(btnNextPage, AppColors.PRIMARY);
        btnPrevPage.addActionListener(e -> changePage(currentPage - 1));
        btnNextPage.addActionListener(e -> changePage(currentPage + 1));
        cbPageSize.addActionListener(e -> { itemsPerPage = (int) cbPageSize.getSelectedItem(); currentPage = 0; loadProducts(); });
        pagination.add(btnPrevPage); pagination.add(btnNextPage);
        pagination.add(cbPageSize); pagination.add(lblPageInfo);
        panel.add(pagination, BorderLayout.SOUTH);
        return panel;
    }

    private void loadProducts() {
        productModel.setRowCount(0);
        pageCache.clear();

        java.util.List<Product> list = productBUS.getAllProducts();
        allProducts.clear();
        if (list != null) allProducts.addAll(list);

        int start = currentPage * itemsPerPage;
        int end = Math.min(allProducts.size(), start + itemsPerPage);
        for (int i = start; i < end; i++) {
            Product p = allProducts.get(i);
            pageCache.add(p);
            productModel.addRow(new Object[]{
                    safe(p.getId()),
                    safe(p.getName()),
                    mapCategoryCodeToVN(p.getCategory() != null ? p.getCategory().name() : null),
                    safe(p.getActiveIngredient()),
                    safe(p.getManufacturer())
            });
        }
        updatePaginationInfo();
    }

    private void doSearch() {
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        String catLabel = (String) cbCategory.getSelectedItem();
        String formLabel = (String) cbForm.getSelectedItem();
        String catCode = mapCategoryVNToCode(catLabel);
        String formCode = mapFormVNToCode(formLabel);

        java.util.List<Product> result = productBUS.searchProducts(keyword, catCode, formCode);
        productModel.setRowCount(0);
        pageCache.clear();
        allProducts.clear();
        if (result != null) allProducts.addAll(result);
        currentPage = 0;
        int end = Math.min(allProducts.size(), itemsPerPage);
        for (int i = 0; i < end; i++) {
            Product p = allProducts.get(i);
            pageCache.add(p);
            productModel.addRow(new Object[]{
                    safe(p.getId()), safe(p.getName()), mapCategoryCodeToVN(p.getCategory() != null ? p.getCategory().name() : null), safe(p.getActiveIngredient()), safe(p.getManufacturer())
            });
        }
        updatePaginationInfo();
        if (allProducts.isEmpty()) JOptionPane.showMessageDialog(this, "Không tìm thấy sản phẩm phù hợp.", "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
        showPlaceholder();
    }

    private void changePage(int newPage) {
        int totalPages = (int) Math.ceil((double) allProducts.size() / itemsPerPage);
        if (newPage < 0 || newPage >= totalPages) return;
        currentPage = newPage;
        loadProducts();
    }

    private void updatePaginationInfo() {
        int totalItems = allProducts.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages + " (Tổng: " + totalItems + " sản phẩm)");
        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
    }

    private void exportProductsToCSV() {
        try {
            File dir = getDownloadsDir();
            JFileChooser chooser = new JFileChooser(dir);
            chooser.setSelectedFile(new File(dir, "products_export.csv"));
            int res = chooser.showSaveDialog(this);
            if (res != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();
            int count = productIO.exportProductsTableToCSV(productModel, file);
            JOptionPane.showMessageDialog(this, "Đã xuất " + count + " dòng tới: " + file.getAbsolutePath(), "Xuất CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Xuất thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importProductsFromCSV() {
        try {
            File dir = getDownloadsDir();
            JFileChooser chooser = new JFileChooser(dir);
            int res = chooser.showOpenDialog(this);
            if (res != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();
            int imported = productIO.importProductsFromCSV(file);
            JOptionPane.showMessageDialog(this, "Đã nhập " + imported + " sản phẩm từ: " + file.getAbsolutePath(), "Nhập CSV", JOptionPane.INFORMATION_MESSAGE);
            loadProducts();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Nhập thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File getDownloadsDir() {
        File d = new File(System.getProperty("user.home"), "Downloads");
        if (d.exists() && d.isDirectory()) return d;
        javax.swing.filechooser.FileSystemView v = javax.swing.filechooser.FileSystemView.getFileSystemView();
        File sys = v.getDefaultDirectory();
        if (sys != null && sys.exists()) {
            File dl = new File(sys, "Downloads");
            if (dl.exists() && dl.isDirectory()) return dl;
            return sys;
        }
        return new File(System.getProperty("user.home"));
    }

    // ===================== Right detail =====================
    private JPanel detailCards; private final String CARD_PLACEHOLDER = "placeholder", CARD_DETAIL = "detail";

    private JComponent createRightDetail() {
        detailCards = new JPanel(new CardLayout());
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setBackground(new Color(245, 250, 250));
        JLabel lbl = new JLabel("Bấm 1 sản phẩm để xem chi tiết hoặc Thêm mới", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(new Color(180, 180, 180));
        placeholder.add(lbl, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(new Color(240, 250, 250));
        mainPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Chi tiết sản phẩm", 0, 0, new Font("Segoe UI", Font.BOLD, 14), AppColors.PRIMARY));

        JPanel info = buildInfoPanel();
        JPanel tables = buildTablesPanel();

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setResizeWeight(0.45);
        verticalSplit.setDividerSize(6);
        verticalSplit.setTopComponent(info);
        verticalSplit.setBottomComponent(tables);
        verticalSplit.setBorder(null);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttons.setBackground(new Color(245, 250, 250));
        bottomPrimaryButton = new JButton("Cập nhật");
        styleButton(bottomPrimaryButton, new Color(255, 193, 7));
        bottomSecondaryButton = new JButton("Hủy");
        styleButton(bottomSecondaryButton, AppColors.PRIMARY);

        bottomPrimaryButton.addActionListener(e -> {
            if (isAdding) {
                if (!addConfirmPending) {
                    addConfirmPending = true;
                    bottomPrimaryButton.setText("Xác nhận");
                    styleButton(bottomPrimaryButton, new Color(255, 87, 34));
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận thêm sản phẩm mới?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) handleAdd();
                else { addConfirmPending = false; bottomPrimaryButton.setText("Thêm mới"); styleButton(bottomPrimaryButton, new Color(40, 167, 69)); }
            } else if (isViewing) {
                if (!isEditingFields) { setFormEditable(true); isEditingFields = true; }
                else handleUpdate();
            }
        });
        bottomSecondaryButton.addActionListener(e -> handleCancelAll());
        buttons.add(bottomPrimaryButton); buttons.add(bottomSecondaryButton);

        mainPanel.add(verticalSplit, BorderLayout.CENTER);
        mainPanel.add(buttons, BorderLayout.SOUTH);

        detailCards.add(placeholder, CARD_PLACEHOLDER);
        detailCards.add(mainPanel, CARD_DETAIL);
        return detailCards;
    }

    private JPanel buildInfoPanel() {
        JPanel info = new JPanel(new BorderLayout(0, 10));
        info.setBackground(new Color(245, 250, 250));
        info.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Create two columns to spare space
        JPanel leftCol = new JPanel(new GridLayout(3, 1, 15, 10)); leftCol.setOpaque(false);
        JPanel rightCol = new JPanel(new GridLayout(2, 1, 15, 10)); rightCol.setOpaque(false);
        JPanel topTwoCols = new JPanel(new GridLayout(1, 2, 15, 10)); topTwoCols.setOpaque(false);

        txtId = new JTextField(); txtId.setEditable(false);
        txtName = new JTextField();
        txtShortName = new JTextField();
        txtBarcode = new JTextField();
        cbCategoryDetail = new JComboBox<>(new String[]{"Thuốc kê đơn","Thuốc không kê đơn","Sản phẩm chức năng"});

        // Left column: Mã, Tên viết tắt, Loại
        leftCol.add(labeled("Mã:", txtId));
        leftCol.add(labeled("Tên viết tắt (tuỳ chọn):", txtShortName));
        leftCol.add(labeled("Loại:", cbCategoryDetail));
        // Right column: Tên, Mã vạch
        rightCol.add(labeled("Tên:", txtName));
        rightCol.add(labeled("Mã vạch:", txtBarcode));
        topTwoCols.add(leftCol); topTwoCols.add(rightCol);

        JPanel grid2 = new JPanel(new GridLayout(3, 2, 15, 10));
        grid2.setOpaque(false);
        cbFormDetail = new JComboBox<>(new String[]{"Viên nén", "Viên nang", "Thuốc bột", "Kẹo ngậm", "Si rô", "Thuốc nhỏ giọt", "Súc miệng"});
        txtActiveIngredient = new JTextField();
        txtManufacturer = new JTextField();
        txtStrength = new JTextField();
        spVat = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 100.0, 0.1));
        cbBaseUom = new JComboBox<>();

        grid2.add(labeled("Dạng:", cbFormDetail));
        grid2.add(labeled("Hoạt chất:", txtActiveIngredient));
        grid2.add(labeled("Nhà sản xuất:", txtManufacturer));
        grid2.add(labeled("Hàm lượng:", txtStrength));
        grid2.add(labeled("VAT (%):", spVat));
        grid2.add(labeled("ĐVT gốc:", cbBaseUom));

        // Make description smaller
        txtDescription = new JTextArea(3, 24);
        txtDescription.setLineWrap(true); txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        JScrollPane descScroll = new JScrollPane(txtDescription);
        descScroll.setPreferredSize(new Dimension(0, 130));
        descScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Mô tả", 0, 0, new Font("Segoe UI", Font.BOLD, 12), AppColors.PRIMARY));

        info.add(topTwoCols, BorderLayout.NORTH);
        info.add(grid2, BorderLayout.CENTER);
        info.add(descScroll, BorderLayout.SOUTH);

        cbCategoryDetail.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED && isEditingFields) applyDefaultVatByCategory(); });
        return info;
    }

    private JPanel buildTablesPanel() {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(createUomSection()); panel.add(Box.createVerticalStrut(12)); panel.add(createLotSection());
        return panel;
    }

    private JComponent createUomSection() {
        JPanel section = new JPanel(new BorderLayout(5, 5)); section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                "Đơn vị quy đổi", 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY));
        uomModel = new ToggleEditableTableModel(new String[]{"Mã ĐV", "Tên ĐV", "Quy đổi về ĐV gốc"}, 0);
        uomModel.setReadOnlyColumns(UOM_COL_ID);
        tblUom = new JTable(uomModel) {
            @Override public boolean isCellEditable(int row, int column) { return uomModel.isCellEditable(row, column); }
        };
        styleTable(tblUom, true);
        capVisibleRows(tblUom, 5);
        tblUom.getColumnModel().getColumn(UOM_COL_NAME).setCellEditor(new SearchableMeasurementNameEditor());
        tblUom.getColumnModel().getColumn(UOM_COL_NAME).setCellRenderer(new MeasurementNameRenderer());
        tblUom.getColumnModel().getColumn(UOM_COL_RATE).setCellEditor(new IntSpinnerEditor(1, Integer.MAX_VALUE, 1));

        // Auto-add behavior like TAB_Promotion tables
        uomModel.addTableModelListener(e -> {
            if (uomAutoChanging) return; // guard against re-entrancy
            uomAutoChanging = true;
            try {
                if (uomModel.getRowCount() == 0) { uomModel.addRow(new Object[uomModel.getColumnCount()]); return; }
                int last = uomModel.getRowCount() - 1; boolean complete = true;
                for (int c = 0; c < uomModel.getColumnCount(); c++) {
                    if (!uomModel.isCellEditable(last, c)) continue;
                    Object val = uomModel.getValueAt(last, c);
                    if (val == null || val.toString().trim().isEmpty()) { complete = false; break; }
                }
                if (complete) uomModel.addRow(new Object[uomModel.getColumnCount()]);
                updateBaseUomComboBox();
                tblUom.repaint();
            } finally {
                uomAutoChanging = false;
            }
        });
        // Delete key
        tblUom.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int row = tblUom.getSelectedRow(); if (row >= 0) { uomModel.removeRow(row); if (uomModel.getRowCount() == 0) uomModel.addRow(new Object[uomModel.getColumnCount()]); }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblUom); scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        section.add(scroll, BorderLayout.CENTER);
        // remove footer bar buttons
        uomFooterBar = null; btnUomAdd = null; btnUomDelete = null;
        return section;
    }

    private JComponent createLotSection() {
        JPanel section = new JPanel(new BorderLayout(5, 5)); section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                "Lô & hạn sử dụng", 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY));
        // Include hidden ID column
        lotModel = new ToggleEditableTableModel(new String[]{"ID", "Mã lô", "Số lượng", "Giá (ĐV gốc)", "HSD", "Tình trạng"}, 0);
        // Make ID column read-only
        lotModel.setReadOnlyColumns(LOT_COL_HIDDEN_ID);
        tblLot = new JTable(lotModel) {
            @Override public boolean isCellEditable(int row, int column) { return lotModel.isCellEditable(row, column); }
        };
        styleTable(tblLot, true);
        capVisibleRows(tblLot, 5);
        JComboBox<String> cbLotStat = new JComboBox<>(new String[]{"Được bán","Hết hạn sử dụng","Lỗi nhà sản xuất"});
        tblLot.getColumnModel().getColumn(LOT_COL_STAT).setCellEditor(new DefaultCellEditor(cbLotStat));
        tblLot.getColumnModel().getColumn(LOT_COL_QTY).setCellEditor(new IntSpinnerEditor(0, Integer.MAX_VALUE, 1));
        tblLot.getColumnModel().getColumn(LOT_COL_HSD).setCellEditor(new DatePickerCellEditor());
        // Hide the ID column visually
        tblLot.getColumnModel().getColumn(LOT_COL_HIDDEN_ID).setMinWidth(0);
        tblLot.getColumnModel().getColumn(LOT_COL_HIDDEN_ID).setMaxWidth(0);
        tblLot.getColumnModel().getColumn(LOT_COL_HIDDEN_ID).setWidth(0);

        lotModel.addTableModelListener(e -> {
            if (lotAutoChanging) return; // guard against re-entrancy
            lotAutoChanging = true;
            try {
                if (lotModel.getRowCount() == 0) { lotModel.addRow(new Object[lotModel.getColumnCount()]); return; }
                int last = lotModel.getRowCount() - 1; boolean complete = true;
                for (int c = 0; c < lotModel.getColumnCount(); c++) {
                    if (!lotModel.isCellEditable(last, c)) continue;
                    Object val = lotModel.getValueAt(last, c);
                    if (val == null || val.toString().trim().isEmpty()) { complete = false; break; }
                }
                if (complete) lotModel.addRow(new Object[lotModel.getColumnCount()]);
                tblLot.repaint();
            } finally {
                lotAutoChanging = false;
            }
        });
        tblLot.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int row = tblLot.getSelectedRow(); if (row >= 0) { lotModel.removeRow(row); if (lotModel.getRowCount() == 0) lotModel.addRow(new Object[lotModel.getColumnCount()]); }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblLot); scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        section.add(scroll, BorderLayout.CENTER);
        lotFooterBar = null; btnLotAdd = null; btnLotDelete = null;
        return section;
    }

    // ===================== Helpers & mappers =====================
    private JPanel labeled(String text, Component c) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        JLabel l = new JLabel(text); l.setPreferredSize(new Dimension(120, 25));
        p.add(l, BorderLayout.WEST); p.add(c, BorderLayout.CENTER); return p;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 35));
    }

    private void styleTable(JTable table, boolean showGrid) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(230, 245, 255));
        table.setSelectionForeground(Color.BLACK);
        table.setShowGrid(showGrid); table.setGridColor(new Color(220, 230, 240));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean hasFocus, int r, int c) {
                if (v instanceof MeasurementName mn) v = mn.getName();
                Component comp = super.getTableCellRendererComponent(t, v, sel, hasFocus, r, c);
                if (!t.isCellEditable(r, c)) { comp.setBackground(new Color(240,240,240)); comp.setForeground(new Color(150,150,150)); }
                else if (sel) { comp.setBackground(new Color(200, 230, 255)); comp.setForeground(Color.BLACK); }
                else { comp.setBackground(Color.WHITE); comp.setForeground(Color.BLACK); }
                return comp;
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", true);
    }

    private void capVisibleRows(JTable table, int maxRows) {
        int header = table.getTableHeader().getPreferredSize().height;
        int rows = Math.min(table.getRowCount(), maxRows);
        int h = header + table.getRowHeight() * rows + 2;
        table.setPreferredScrollableViewportSize(new Dimension(0, h));
    }

    private void deleteSelectedRow(DefaultTableModel model, JTable t) {
        int row = t.getSelectedRow(); if (row < 0 || row >= model.getRowCount()) return;
        if (JOptionPane.showConfirmDialog(this, "Xác nhận xóa dòng đang chọn?", "Xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        model.removeRow(row);
        int next = Math.min(row, model.getRowCount() - 1); if (next >= 0) t.changeSelection(next, 0, false, false);
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Thiếu thông tin", JOptionPane.WARNING_MESSAGE); }
    private String safe(Object v) { return v == null ? "" : String.valueOf(v).trim(); }

    private String mapCategoryVNToCode(String label) {
        if (label == null || label.equalsIgnoreCase("Tất cả")) return null;
        String l = label.trim().toLowerCase();
        if (l.contains("không kê đơn")) return "OTC";
        if (l.contains("kê đơn")) return "ETC";
        if (l.contains("chức năng")) return "SUPPLEMENT";
        return null;
    }
    private String mapFormVNToCode(String label) {
        if (label == null || label.equalsIgnoreCase("Tất cả")) return null;
        String l = label.trim().toLowerCase();
        if (l.contains("viên") || l.contains("bột") || l.contains("kẹo")) return "SOLID";
        if (l.contains("si rô") || l.contains("siro") || l.contains("nhỏ giọt") || l.contains("súc miệng")) return "LIQUID_DOSAGE";
        return null;
    }
    private String mapCategoryCodeToVN(String code) {
        if (code == null) return "";
        switch (code) { case "OTC": return "Thuốc không kê đơn"; case "ETC": return "Thuốc kê đơn"; case "SUPPLEMENT": return "Sản phẩm chức năng"; default: return code; }
    }
    private ProductCategory mapCategoryVNToEnum(String label) {
        if (label == null) return ProductCategory.OTC;
        String l = label.trim().toLowerCase();
        if (l.contains("không kê đơn")) return ProductCategory.OTC;
        if (l.contains("kê đơn")) return ProductCategory.ETC;
        if (l.contains("chức năng")) return ProductCategory.SUPPLEMENT;
        return ProductCategory.OTC;
    }
    private DosageForm mapFormVNToEnum(String label) {
        if (label == null) return DosageForm.SOLID;
        String l = label.trim().toLowerCase();
        if (l.contains("viên") || l.contains("bột") || l.contains("kẹo")) return DosageForm.SOLID;
        if (l.contains("si rô") || l.contains("siro") || l.contains("nhỏ giọt") || l.contains("súc miệng")) return DosageForm.LIQUID_DOSAGE;
        return DosageForm.SOLID;
    }
    private LotStatus mapLotStatusVN(String label) {
        if (label == null) return LotStatus.AVAILABLE;
        String l = label.trim().toLowerCase();
        if (l.contains("hết hạn")) return LotStatus.EXPIRED;
        if (l.contains("lỗi")) return LotStatus.FAULTY;
        return LotStatus.AVAILABLE;
    }
    private String mapLotStatusEnumToVN(LotStatus st) {
        if (st == null) return "Được bán";
        return switch (st) { case EXPIRED -> "Hết hạn sử dụng"; case FAULTY -> "Lỗi nhà sản xuất"; default -> "Được bán"; };
    }

    private String formatDMY(LocalDateTime dt) { return (dt == null) ? "" : dt.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }

    private Integer parsePositiveInt(Object v) { try { String s = String.valueOf(v).trim().replaceAll("\\s", ""); if (s.isEmpty()) return null; s = s.replace(".", "").replace(",", ""); int x = Integer.parseInt(s); return x > 0 ? x : null; } catch (Exception e) { return null; } }
    private Integer parseNonNegativeInt(Object v) { try { String s = String.valueOf(v).trim().replaceAll("\\s", ""); if (s.isEmpty()) return null; s = s.replace(".", "").replace(",", ""); int x = Integer.parseInt(s); return x >= 0 ? x : null; } catch (Exception e) { return null; } }
    private BigDecimal parseNonNegativeBigDecimal(Object v) {
        try { String s = String.valueOf(v).trim(); if (s.isEmpty()) return null; if (s.contains(",") && !s.contains(".")) s = s.replace(",", "."); s = s.replaceAll("(?<=\\d)[,\\.](?=\\d{3}(\\D|$))", ""); BigDecimal bd = new BigDecimal(s); return bd.compareTo(BigDecimal.ZERO) >= 0 ? bd.setScale(2, java.math.RoundingMode.HALF_UP) : null; } catch (Exception e) { return null; }
    }
    private boolean isValidDateDMY(String s) {
        if (s == null || (s = s.trim()).isEmpty()) return false; String[] ps = {"dd/MM/yy","d/M/yy","dd/MM/yyyy","d/M/yyyy"};
        for (String p : ps) try { SimpleDateFormat f = new SimpleDateFormat(p); f.setLenient(false); f.parse(s); return true; } catch (ParseException ignore) {}
        return false;
    }
    private LocalDate parseDMYToLocalDate(String s) {
        String[] ps = {"dd/MM/yy","d/M/yy","dd/MM/yyyy","d/M/yyyy"};
        for (String p : ps) { try { return LocalDate.parse(s, DateTimeFormatter.ofPattern(p)); } catch (DateTimeParseException ignore) {} }
        throw new IllegalArgumentException("Ngày (HSD) không hợp lệ: " + s);
    }

    private void selectComboItem(JComboBox<String> cb, String value) { if (cb == null || value == null) return; for (int i = 0; i < cb.getItemCount(); i++) if (String.valueOf(cb.getItemAt(i)).equalsIgnoreCase(value)) { cb.setSelectedIndex(i); return; } }

    // ===================== Bind & Build =====================
    private void showProductDetails(Product product) {
        if (product == null) return;
        isViewing = true; isAdding = false; isEditingFields = false; currentEditingId = product.getId();
        // Fill basic info
        txtId.setText(safe(product.getId()));
        txtName.setText(safe(product.getName()));
        if (txtShortName != null) txtShortName.setText(safe(product.getShortName()));
        txtBarcode.setText(safe(product.getBarcode()));
        txtActiveIngredient.setText(safe(product.getActiveIngredient()));
        txtManufacturer.setText(safe(product.getManufacturer()));
        txtStrength.setText(safe(product.getStrength()));
        txtDescription.setText(safe(product.getDescription()));
        if (product.getCategory() != null) selectComboItem(cbCategoryDetail, mapCategoryCodeToVN(product.getCategory().name()));
        if (product.getForm() != null) {
            switch (product.getForm()) { case SOLID -> selectComboItem(cbFormDetail, "Viên nén"); case LIQUID_DOSAGE -> selectComboItem(cbFormDetail, "Si rô"); default -> cbFormDetail.setSelectedIndex(0);} }
        spVat.setValue(product.getVatAsDouble());

        // Bind UOM
        uomAutoChanging = true;
        try {
            uomModel.setRowCount(0);
            if (product.getUnitOfMeasureSet() != null) {
                for (UnitOfMeasure u : product.getUnitOfMeasureSet()) {
                    uomModel.addRow(new Object[]{ u.getMeasurement().getId(), u.getMeasurement(), u.getBaseUnitConversionRate() });
                }
            }
        } finally {
            uomAutoChanging = false;
        }
        // Base UOM from product
        cbBaseUom.removeAllItems();
        if (product.getUnitOfMeasureSet() != null) {
            for (UnitOfMeasure u : product.getUnitOfMeasureSet()) cbBaseUom.addItem(u.getMeasurement());
            if (product.getBaseUnitOfMeasure() != null) {
                for (int i = 0; i < cbBaseUom.getItemCount(); i++) if (cbBaseUom.getItemAt(i).getName().equalsIgnoreCase(product.getBaseUnitOfMeasure())) { cbBaseUom.setSelectedIndex(i); break; }
            }
        }

        // Bind Lots
        lotAutoChanging = true;
        try {
            lotModel.setRowCount(0);
            if (product.getLotSet() != null) {
                for (Lot l : product.getLotSet()) {
                    lotModel.addRow(new Object[]{ safe(l.getId()), safe(l.getBatchNumber()), l.getQuantity(), l.getRawPrice(), formatDMY(l.getExpiryDate()), mapLotStatusEnumToVN(l.getStatus()) });
                }
            }
        } finally {
            lotAutoChanging = false;
        }
        capVisibleRows(tblUom, 5); capVisibleRows(tblLot, 5);

        showDetailCard();
        bottomPrimaryButton.setText("Cập nhật"); styleButton(bottomPrimaryButton, new Color(255, 193, 7));
        bottomSecondaryButton.setText("Hủy"); styleButton(bottomSecondaryButton, AppColors.PRIMARY);
        setFormEditable(false);
    }

    private void stopEditingOnTables() {
        // Commit any ongoing edits to ensure data is captured
        for (JTable t : new JTable[]{tblUom, tblLot}) {
            if (t == null) continue;
            if (t.isEditing()) {
                TableCellEditor ed = t.getCellEditor();
                if (ed != null) ed.stopCellEditing();
            }
        }
    }

    private Product buildProductFromForm(boolean isNew) {
        // Commit edits first
        stopEditingOnTables();
        // Basic required fields
        String name = txtName.getText().trim(); if (name.isEmpty()) { warn("Vui lòng nhập Tên sản phẩm."); txtName.requestFocusInWindow(); return null; }
        String barcode = txtBarcode.getText().trim(); if (barcode.isEmpty()) { warn("Vui lòng nhập Mã vạch."); txtBarcode.requestFocusInWindow(); return null; }
        if (!barcode.matches("\\d{8,20}")) { warn("Mã vạch chỉ gồm 8–20 chữ số."); txtBarcode.requestFocusInWindow(); return null; }
        if (cbBaseUom.getSelectedItem() == null) { warn("Vui lòng chọn ĐVT gốc."); cbBaseUom.requestFocusInWindow(); return null; }

        Object baseSel = cbBaseUom.getSelectedItem();
        String baseUomName = (baseSel instanceof MeasurementName mn) ? mn.getName() : (baseSel != null ? baseSel.toString().trim() : "");

        // Construct product using constructor (id required for update)
        String id = isNew ? null : safe(txtId.getText());
        Product p = new Product(
                id,
                barcode,
                mapCategoryVNToEnum(String.valueOf(cbCategoryDetail.getSelectedItem())),
                mapFormVNToEnum(String.valueOf(cbFormDetail.getSelectedItem())),
                name,
                (txtShortName == null) ? null : txtShortName.getText().trim(),
                txtManufacturer.getText().trim(),
                txtActiveIngredient.getText().trim(),
                ((Number) spVat.getValue()).doubleValue(),
                txtStrength.getText().trim(),
                txtDescription.getText().trim(),
                baseUomName,
                null,
                null,
                null
        );

        // UOM set: collect all non-empty rows
        Set<UnitOfMeasure> uoms = new HashSet<>();
        Set<String> seenUomNames = new HashSet<>();
        for (int r = 0; r < uomModel.getRowCount(); r++) {
            Object nm = uomModel.getValueAt(r, UOM_COL_NAME);
            Integer rate = parsePositiveInt(uomModel.getValueAt(r, UOM_COL_RATE));
            if (!(nm instanceof MeasurementName mn) || rate == null) continue; // skip empty rows only
            if (!seenUomNames.add(mn.getName().toLowerCase())) continue; // enforce uniqueness
            UnitOfMeasure u = new UnitOfMeasure(p, mn, BigDecimal.valueOf(0.0), BigDecimal.valueOf(rate));
            u.setProduct(p);
            uoms.add(u);
        }
        p.setUnitOfMeasureSet(uoms);

        // LOT set: collect all rows with batch number
        Set<Lot> lots = new HashSet<>();
        for (int r = 0; r < lotModel.getRowCount(); r++) {
            String lotId = safe(lotModel.getValueAt(r, LOT_COL_HIDDEN_ID));
            String batch = safe(lotModel.getValueAt(r, LOT_COL_ID));
            if (batch.isEmpty()) continue;
            Integer qty = parseNonNegativeInt(lotModel.getValueAt(r, LOT_COL_QTY)); if (qty == null) qty = 0;
            BigDecimal price = parseNonNegativeBigDecimal(lotModel.getValueAt(r, LOT_COL_PRICE)); if (price == null) price = BigDecimal.ZERO;
            String exp = safe(lotModel.getValueAt(r, LOT_COL_HSD));
            if (!exp.isEmpty() && !isValidDateDMY(exp)) { warn("HSD không hợp lệ ở dòng Lô: " + (r + 1)); return null; }
            LocalDateTime expiry = exp.isEmpty() ? null : parseDMYToLocalDate(exp).atStartOfDay();
            LotStatus status = mapLotStatusVN(safe(lotModel.getValueAt(r, LOT_COL_STAT)));
            // Assign ID: preserve existing on update, generate for new
            String idToUse = (lotId != null && !lotId.isEmpty()) ? lotId : java.util.UUID.randomUUID().toString();
            Lot lot = new Lot(idToUse, batch, p, qty, price, expiry, status);
            lots.add(lot);
        }
        if (isNew && lots.isEmpty()) { warn("Bảng Lô & hạn sử dụng phải có ít nhất 1 dòng."); return null; }
        p.setLotSet(lots);
        return p;
    }

    // ===================== Missing actions/helpers implementations =====================
    private void showPlaceholder() {
        if (detailCards == null) return;
        CardLayout cl = (CardLayout) detailCards.getLayout();
        cl.show(detailCards, CARD_PLACEHOLDER);
        isViewing = false; isAdding = false; isEditingFields = false; addConfirmPending = false;
    }

    private void showDetailCard() {
        if (detailCards == null) return;
        CardLayout cl = (CardLayout) detailCards.getLayout();
        cl.show(detailCards, CARD_DETAIL);
    }

    private void setFormEditable(boolean editable) {
        boolean enable = editable;
        txtName.setEnabled(enable);
        txtShortName.setEnabled(enable);
        txtBarcode.setEnabled(enable);
        cbCategoryDetail.setEnabled(enable);
        cbFormDetail.setEnabled(enable);
        txtActiveIngredient.setEnabled(enable);
        txtManufacturer.setEnabled(enable);
        txtStrength.setEnabled(enable);
        spVat.setEnabled(enable);
        cbBaseUom.setEnabled(enable);
        txtDescription.setEnabled(enable);
        if (uomModel != null) uomModel.setEditable(enable);
        if (lotModel != null) lotModel.setEditable(enable);
    }

    private void startAddMode() {
        isAdding = true; isViewing = false; isEditingFields = true; addConfirmPending = false; currentEditingId = null;
        // clear form
        txtId.setText("");
        txtName.setText("");
        if (txtShortName != null) txtShortName.setText("");
        txtBarcode.setText("");
        cbCategoryDetail.setSelectedIndex(0);
        cbFormDetail.setSelectedIndex(0);
        txtActiveIngredient.setText("");
        txtManufacturer.setText("");
        txtStrength.setText("");
        spVat.setValue(5.0);
        txtDescription.setText("");
        // tables
        uomAutoChanging = true; lotAutoChanging = true;
        try {
            uomModel.setRowCount(0);
            lotModel.setRowCount(0);
            uomModel.addRow(new Object[uomModel.getColumnCount()]);
            lotModel.addRow(new Object[lotModel.getColumnCount()]);
        } finally {
            uomAutoChanging = false; lotAutoChanging = false;
        }
        updateBaseUomComboBox();
        // Ensure all inputs are editable in add mode
        setFormEditable(true);
        if (tblUom != null) tblUom.setEnabled(true);
        if (tblLot != null) tblLot.setEnabled(true);
        bottomPrimaryButton.setText("Thêm mới"); styleButton(bottomPrimaryButton, new Color(40, 167, 69));
        bottomSecondaryButton.setText("Hủy"); styleButton(bottomSecondaryButton, AppColors.PRIMARY);
        showDetailCard();
        // Focus first editable field
        SwingUtilities.invokeLater(() -> txtName.requestFocusInWindow());
    }

    private void handleCancelAll() {
        // reset to placeholder
        setFormEditable(false);
        showPlaceholder();
        bottomPrimaryButton.setText("Cập nhật"); styleButton(bottomPrimaryButton, new Color(255, 193, 7));
        bottomSecondaryButton.setText("Hủy"); styleButton(bottomSecondaryButton, AppColors.PRIMARY);
        // clear selection in list
        if (table != null) table.clearSelection();
    }

    private void applyDefaultVatByCategory() {
        String label = String.valueOf(cbCategoryDetail.getSelectedItem());
        // Simple heuristic: OTC 5%, ETC 5%, Supplement 10%
        double vat = switch (mapCategoryVNToEnum(label)) {
            case SUPPLEMENT -> 10.0;
            case ETC -> 5.0;
            default -> 5.0;
        };
        spVat.setValue(vat);
    }

    private void updateBaseUomComboBox() {
        cbBaseUom.removeAllItems();
        Set<String> names = new LinkedHashSet<>();
        for (int r = 0; r < uomModel.getRowCount(); r++) {
            Object val = uomModel.getValueAt(r, UOM_COL_NAME);
            if (val instanceof MeasurementName mn) names.add(mn.getName());
        }
        for (MeasurementName mn : allMeasurementNames) {
            if (names.contains(mn.getName())) cbBaseUom.addItem(mn);
        }
    }

    private void handleAdd() {
        Product p = buildProductFromForm(true);
        if (p == null) { return; }
        try {
            boolean ok = productBUS.addProduct(p);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã thêm sản phẩm mới.");
                addConfirmPending = false; isAdding = false; isViewing = true; isEditingFields = false;
                loadProducts();
                showPlaceholder();
            } else {
                JOptionPane.showMessageDialog(this, "Thêm sản phẩm thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdate() {
        // Confirm before applying update
        int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận cập nhật sản phẩm này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        Product p = buildProductFromForm(false);
        if (p == null) return;
        try {
            boolean ok = productBUS.updateProduct(p);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Cập nhật sản phẩm thành công.");
                isEditingFields = false; setFormEditable(false);
                loadProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật sản phẩm thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== Table models & editors =====================
    private static class ToggleEditableTableModel extends DefaultTableModel {
        private boolean editable = true;
        private final BitSet readOnlyCols = new BitSet();
        ToggleEditableTableModel(String[] cols, int rows) { super(cols, rows); }
        void setEditable(boolean e) { if (this.editable != e) { this.editable = e; fireTableDataChanged(); } }
        void setReadOnlyColumns(int... cols) { readOnlyCols.clear(); if (cols != null) for (int c : cols) if (c >= 0) readOnlyCols.set(c); fireTableDataChanged(); }
        @Override public boolean isCellEditable(int r, int c) { return editable && !readOnlyCols.get(c); }
    }

    private class MeasurementNameRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String display = ""; if (value instanceof MeasurementName mn) display = mn.getName(); else if (value != null) display = value.toString();
            return super.getTableCellRendererComponent(table, display, isSelected, hasFocus, row, column);
        }
    }

    private class SearchableMeasurementNameEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<MeasurementName> combo; private final DefaultComboBoxModel<MeasurementName> model;
        private int editingRow = -1; private boolean isUpdating = false;
        SearchableMeasurementNameEditor() {
            model = new DefaultComboBoxModel<>(); combo = new JComboBox<>(model); combo.setEditable(true);
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    String text = (value instanceof MeasurementName mn) ? mn.getName() : (value == null ? "" : value.toString());
                    return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                }
            });
            JTextField ed = (JTextField) combo.getEditor().getEditorComponent();
            ed.addKeyListener(new KeyAdapter() {
                @Override public void keyReleased(KeyEvent e) {
                    if (isUpdating) return; int key = e.getKeyCode();
                    if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) return;
                    filterAndUpdateCombo(ed.getText().toLowerCase().trim(), ed);
                }
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) { selectBestMatch(((JTextField) combo.getEditor().getEditorComponent()).getText()); stopCellEditing(); }
                    else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { cancelCellEditing(); }
                }
            });
        }
        private void filterAndUpdateCombo(String filterText, JTextField ed) {
            isUpdating = true; try {
                int caret = ed.getCaretPosition(); Set<String> used = getUsedMeasurementNames(editingRow);
                model.removeAllElements();
                for (MeasurementName mn : allMeasurementNames) {
                    if (used.contains(mn.getName().toLowerCase())) continue; // enforce uniqueness per product
                    if (filterText.isEmpty() || mn.getName().toLowerCase().contains(filterText)) model.addElement(mn);
                }
                ed.setText(filterText); ed.setCaretPosition(Math.min(caret, filterText.length()));
                if (model.getSize() > 0 && !filterText.isEmpty()) combo.showPopup(); else combo.hidePopup();
            } finally { isUpdating = false; }
        }
        private void selectBestMatch(String text) {
            if (text == null || text.trim().isEmpty()) return; String lower = text.toLowerCase().trim();
            for (int i = 0; i < model.getSize(); i++) { MeasurementName mn = model.getElementAt(i); if (mn.getName().equalsIgnoreCase(lower)) { combo.setSelectedIndex(i); return; } }
            for (int i = 0; i < model.getSize(); i++) { MeasurementName mn = model.getElementAt(i); if (mn.getName().toLowerCase().startsWith(lower)) { combo.setSelectedIndex(i); return; } }
            for (int i = 0; i < model.getSize(); i++) { MeasurementName mn = model.getElementAt(i); if (mn.getName().toLowerCase().contains(lower)) { combo.setSelectedIndex(i); return; } }
        }
        private Set<String> getUsedMeasurementNames(int excludeRow) {
            Set<String> set = new HashSet<>(); for (int r = 0; r < uomModel.getRowCount(); r++) { if (r == excludeRow) continue; Object val = uomModel.getValueAt(r, UOM_COL_NAME); if (val instanceof MeasurementName mn) set.add(mn.getName().toLowerCase()); else if (val != null && !val.toString().trim().isEmpty()) set.add(val.toString().toLowerCase()); }
            return set;
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row; isUpdating = true; try {
                Set<String> used = getUsedMeasurementNames(row); model.removeAllElements();
                for (MeasurementName mn : allMeasurementNames) if (!used.contains(mn.getName().toLowerCase())) model.addElement(mn);
                JTextField ed = (JTextField) combo.getEditor().getEditorComponent();
                if (value instanceof MeasurementName mn) { combo.setSelectedItem(mn); ed.setText(mn.getName()); }
                else if (value != null && !value.toString().trim().isEmpty()) { String s = value.toString(); ed.setText(s); for (int i = 0; i < model.getSize(); i++) if (model.getElementAt(i).getName().equalsIgnoreCase(s)) { combo.setSelectedIndex(i); break; } }
                else { combo.setSelectedIndex(-1); ed.setText(""); }
                combo.hidePopup();
            } finally { isUpdating = false; }
            return combo;
        }
        @Override public Object getCellEditorValue() {
            Object selected = combo.getSelectedItem(); if (selected instanceof MeasurementName) return selected;
            String text = ((JTextField) combo.getEditor().getEditorComponent()).getText().trim(); Set<String> used = getUsedMeasurementNames(editingRow);
            for (int i = 0; i < model.getSize(); i++) { MeasurementName mn = model.getElementAt(i); if (mn.getName().equalsIgnoreCase(text)) { if (used.contains(mn.getName().toLowerCase())) return null; return mn; } }
            for (int i = 0; i < model.getSize(); i++) { MeasurementName mn = model.getElementAt(i); if (mn.getName().toLowerCase().startsWith(text.toLowerCase())) { if (used.contains(mn.getName().toLowerCase())) continue; return mn; } }
            return (model.getSize() > 0) ? model.getElementAt(0) : null;
        }
    }

    private static class IntSpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner(); private final int min, max, step;
        IntSpinnerEditor(int min, int max, int step) { this.min = min; this.max = max; this.step = step; spinner.setModel(new SpinnerNumberModel(min, min, max, step)); JComponent ed = spinner.getEditor(); if (ed instanceof JSpinner.DefaultEditor de) de.getTextField().setHorizontalAlignment(JTextField.RIGHT); }
        @Override public Object getCellEditorValue() { return ((Number) spinner.getValue()).intValue(); }
        @Override public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int r, int c) { int v = min; try { if (val != null && !String.valueOf(val).trim().isEmpty()) v = Integer.parseInt(String.valueOf(val).replaceAll("[^\\d-]", "")); } catch (Exception ignore) {} if (v < min) v = min; if (v > max) v = max; spinner.setModel(new SpinnerNumberModel(v, min, max, step)); return spinner; }
    }

    private class DatePickerCellEditor extends AbstractCellEditor implements TableCellEditor {
        private DIALOG_DatePicker picker;
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            picker = new DIALOG_DatePicker(new Date()); String s = (value == null) ? "" : String.valueOf(value).trim(); picker.setTextValue(s);
            picker.addPropertyChangeListener("date", e -> super.stopCellEditing()); return picker;
        }
        @Override public Object getCellEditorValue() { return picker.getTextValue(); }
    }
}
