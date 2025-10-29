package com.gui;

import com.bus.BUS_Product;
import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.enums.DosageForm;
import com.enums.LotStatus;
import com.enums.ProductCategory;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.AbstractCellEditor;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextField;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;               // Date, List, ArrayList, BitSet, ...
import java.util.List;

public class TAB_Product {

    // ==== UI CONSTANTS ====
    private static final Font  FONT_TITLE_16B = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font  FONT_TITLE_14B = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font  FONT_LABEL_12B = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font  FONT_BTN_13B   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Color COL_BG_MAIN    = new Color(245, 250, 250);
    private static final Color COL_BG_BODY    = new Color(240, 250, 250);
    private static final Color COL_BORDER     = new Color(200, 230, 240);
    private static final Color COL_HEADER_BG  = AppColors.PRIMARY;
    private static final Color COL_HEADER_FG  = Color.WHITE;
    private static final Color COL_GRID       = new Color(220, 220, 220);
    private static final Color COL_SEL_BG     = new Color(230, 245, 255);

    // ==== HẰNG SỐ CỘT (tránh magic-number) ====
    private static final int UOM_COL_ID   = 0, UOM_COL_NAME = 1, UOM_COL_RATE   = 2;
    private static final int LOT_COL_ID   = 0, LOT_COL_QTY  = 1, LOT_COL_PRICE  = 2,
            LOT_COL_HSD  = 3, LOT_COL_STAT = 4;

    // ==== Business Logic ====
    private final BUS_Product busProduct = new BUS_Product();
    private List<Product> allProducts = new ArrayList<>();

    // ==== Root ====
    public JPanel pProduct;

    // ==== Toolbar ====
    private JTextField txtSearch;
    private JComboBox<String> cbCategory, cbForm, cbStatus;
    private JButton btnExportExcel;

    // ==== Danh sách trái ====
    private JTable tblProducts;
    private DefaultTableModel productModel;
    private JButton btnAddProduct;
    private JButton btnImportExcel;

    // ==== Chi tiết phải ====
    private JLabel lbImage;
    private JButton btnChangeImage;
    private JTextField txtId, txtName, txtShortName, txtBarcode, txtActiveIngredient, txtManufacturer, txtStrength, txtBaseUom;
    private JComboBox<String> cbCategoryDetail, cbFormDetail, cbStatusDetail;
    private JSpinner spVat;
    private JTextArea txtDescription;

    // ==== Bảng con ====
    private JTable tblUom, tblLot;
    private ToggleEditableTableModel uomModel, lotModel;
    private static final String[] LOT_STATUS_OPTIONS = {
            "Được bán", "Hết hạn sử dụng", "Lỗi nhà sản xuất"
    };

    private JPanel uomFooterBar, lotFooterBar;
    private JButton btnUomAdd, btnUomDelete, btnLotAdd, btnLotDelete;

    // ==== Action bar ====
    private JPanel actionBar;
    private JButton btnEdit, btnSave, btnCancel;

    // ==== Trạng thái ====
    private boolean isEditMode = false;
    private int currentSelectedRow = -1;
    private boolean isAddingNew = false;
    private int newProductRowIndex = -1;
    private boolean suppressSelectionEvent = false;
    private boolean isBindingFromTable = false;

    private static final String DEFAULT_IMG_PATH = "\\src\\main\\resources\\images\\products\\etc\\etc1.jpg";

    public TAB_Product() {
        buildUI();
        setEditMode(false);
        loadAllProducts();
    }

    // ===================== UI =====================
    private void buildUI() {
        pProduct = new JPanel(new BorderLayout());
        pProduct.setBackground(COL_BG_MAIN);
        pProduct.setBorder(new EmptyBorder(10, 10, 10, 10));
        pProduct.add(buildToolbar(), BorderLayout.NORTH);
        pProduct.add(buildCenter(), BorderLayout.CENTER);
    }

    private JComponent buildToolbar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(COL_BG_MAIN);
        top.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COL_BORDER),
                "QUẢN LÝ SẢN PHẨM", 0, 0, FONT_TITLE_16B, AppColors.PRIMARY));

        txtSearch = new JTextField(18);
        txtSearch.setPreferredSize(new Dimension(220, 30));
        JButton btnSearch = new JButton("Tìm kiếm");

        cbCategory = new JComboBox<>(new String[]{"Tất cả","Thuốc kê đơn","Thuốc không kê đơn","Sản phẩm chức năng"});
        cbForm     = new JComboBox<>(new String[]{"Tất cả","Viên nén","Viên nang","Thuốc bột","Kẹo ngậm","Si rô","Thuốc nhỏ giọt","Súc miệng"});
        cbStatus   = new JComboBox<>(new String[]{"Tất cả","Đang kinh doanh","Ngừng kinh doanh"});

        cbCategory.setSelectedIndex(0); cbForm.setSelectedIndex(0); cbStatus.setSelectedIndex(0);

        btnExportExcel = new JButton("Xuất Excel");
        styleButton(btnSearch, AppColors.PRIMARY, Color.WHITE);
        styleButton(btnExportExcel, AppColors.PRIMARY, Color.WHITE);

        top.add(new JLabel("Tìm kiếm:"));  top.add(txtSearch);  top.add(btnSearch);
        top.add(new JLabel("Loại:"));      top.add(cbCategory);
        top.add(new JLabel("Dạng:"));      top.add(cbForm);
        top.add(new JLabel("Trạng thái:"));top.add(cbStatus);
        top.add(btnExportExcel);

        btnExportExcel.addActionListener(e -> exportProductsToCSV());
        return top;
    }

    private JComponent buildCenter() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sp.setResizeWeight(0.6);
        sp.setDividerSize(6);
        sp.setBackground(COL_BG_MAIN);
        sp.setLeftComponent(buildLeftList());
        sp.setRightComponent(buildRightDetail());
        return sp;
    }

    private JComponent buildLeftList() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG_MAIN);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COL_BORDER),
                "Danh sách sản phẩm", 0, 0, FONT_TITLE_14B, AppColors.PRIMARY));

        // 6 cột: Mã, Tên, Loại, Hoạt chất, Nhà sản xuất, Trạng thái
        productModel = new DefaultTableModel(
                new String[]{"Mã","Tên","Loại","Hoạt chất","Nhà sản xuất","Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblProducts = new JTable(productModel);
        styleTable(tblProducts);

        tblProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblProducts.getSelectionModel().addListSelectionListener(e -> {
            // luôn refresh trạng thái nút Chỉnh sửa theo selection hiện tại
            enableEditIfRowSelected();

            if (e.getValueIsAdjusting() || suppressSelectionEvent) return;

            int row = tblProducts.getSelectedRow();
            if (row < 0) return;

            if (isEditMode) {
                if (!confirm("Bạn chưa lưu thay đổi. Hủy sản phẩm đang chỉnh sửa?")) {
                    suppressSelectionEvent = true;
                    if (currentSelectedRow >= 0 && currentSelectedRow < productModel.getRowCount()) {
                        tblProducts.setRowSelectionInterval(currentSelectedRow, currentSelectedRow);
                    } else tblProducts.clearSelection();
                    suppressSelectionEvent = false;
                    return;
                } else {
                    if (isAddingNew && newProductRowIndex >= 0 && newProductRowIndex < productModel.getRowCount()) {
                        productModel.removeRow(newProductRowIndex);
                    }
                    isAddingNew = false; newProductRowIndex = -1;
                    setEditMode(false);
                }
            }

            currentSelectedRow = row;
            isBindingFromTable = true;
            bindProductFromTableRow(row);
            isBindingFromTable = false;

            setEditMode(false);
            // đảm bảo lại sau khi actionBar được rebuild
            enableEditIfRowSelected();
        });

        JScrollPane scroll = new JScrollPane(tblProducts);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 230, 240)));
        panel.add(scroll, BorderLayout.CENTER);

        // Footer trái
        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        leftFooter.setOpaque(false);

        btnImportExcel = new JButton("Nhập Excel");
        styleButton(btnImportExcel, new Color(0,123,255), Color.WHITE);
        btnImportExcel.setPreferredSize(new Dimension(150, 36));
        btnImportExcel.addActionListener(e -> importProductsFromExcel());

        btnAddProduct = new JButton("Thêm sản phẩm mới");
        styleButton(btnAddProduct, new Color(40, 167, 69), Color.WHITE);
        btnAddProduct.setPreferredSize(new Dimension(190, 36));
        btnAddProduct.addActionListener(e -> addNewProductRowAndEdit());

        leftFooter.add(btnImportExcel);
        leftFooter.add(btnAddProduct);
        panel.add(leftFooter, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent buildRightDetail() {
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(COL_BG_BODY);
        right.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COL_BORDER),
                "Chi tiết sản phẩm", 0, 0, FONT_TITLE_14B, AppColors.PRIMARY));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(8, 16, 16, 16));

        body.add(buildRow0ImageAndBasicInfo()); body.add(Box.createVerticalStrut(10));
        body.add(buildOtherInfoGrid());          body.add(Box.createVerticalStrut(10));
        body.add(createTableSectionUom());       body.add(Box.createVerticalStrut(10));
        body.add(createTableSectionLot());

        JScrollPane scroll = new JScrollPane(body);
        scroll.getViewport().setBackground(new Color(250, 252, 252));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));

        actionBar = buildActionBar();
        right.add(scroll, BorderLayout.CENTER);
        right.add(actionBar, BorderLayout.SOUTH);
        return right;
    }

    private JComponent buildRow0ImageAndBasicInfo() {
        JPanel row0 = new JPanel(new GridLayout(1, 2, 12, 0));
        row0.setOpaque(false);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                new EmptyBorder(10, 10, 10, 10)));

        lbImage = new JLabel("No Image", SwingConstants.CENTER);
        lbImage.setPreferredSize(new Dimension(180, 180));
        setImage(DEFAULT_IMG_PATH);

        btnChangeImage = new JButton("Đổi ảnh…");
        styleButton(btnChangeImage, AppColors.PRIMARY, Color.WHITE);
        btnChangeImage.addActionListener(e -> chooseImage());

        left.add(lbImage, BorderLayout.CENTER);
        left.add(btnChangeImage, BorderLayout.SOUTH);

        JPanel right = new JPanel(new GridLayout(6, 1, 10, 8)); // 6 hàng: + Tên viết tắt
        right.setOpaque(false);

        txtId = new JTextField();
        txtName = new JTextField();
        txtShortName = new JTextField(); // NEW: tuỳ chọn
        txtBarcode = new JTextField();
        cbCategoryDetail = new JComboBox<>(new String[]{"Thuốc kê đơn","Thuốc không kê đơn","Sản phẩm chức năng"});
        cbStatusDetail   = new JComboBox<>(new String[]{"Đang kinh doanh","Ngừng kinh doanh"});

        right.add(labeled("Mã:", txtId));
        right.add(labeled("Tên:", txtName));
        right.add(labeled("Tên viết tắt (tuỳ chọn):", txtShortName));
        right.add(labeled("Mã vạch:", txtBarcode));
        right.add(labeled("Loại:", cbCategoryDetail));
        right.add(labeled("Trạng thái:", cbStatusDetail));

        row0.add(left); row0.add(right);

        cbCategoryDetail.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && isEditMode && !isBindingFromTable) applyDefaultVatByCategory();
        });
        return row0;
    }

    private JComponent buildOtherInfoGrid() {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(3, 2, 15, 10));
        grid.setOpaque(false);

        cbFormDetail = new JComboBox<>(new String[]{"Viên nén", "Viên nang", "Thuốc bột", "Kẹo ngậm", "Si rô", "Thuốc nhỏ giọt", "Súc miệng"});
        txtActiveIngredient = new JTextField();
        txtManufacturer = new JTextField();
        txtStrength = new JTextField();
        spVat = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 100.0, 0.1));
        txtBaseUom = new JTextField();

        grid.add(labeled("Dạng:", cbFormDetail));
        grid.add(labeled("Hoạt chất:", txtActiveIngredient));
        grid.add(labeled("Nhà sản xuất:", txtManufacturer));
        grid.add(labeled("Hàm lượng:", txtStrength));
        grid.add(labeled("VAT (%):", spVat));
        grid.add(labeled("ĐVT gốc:", txtBaseUom));

        txtDescription = new JTextArea(3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        JScrollPane descScroll = new JScrollPane(txtDescription);
        descScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COL_BORDER),
                "Mô tả", 0, 0, FONT_LABEL_12B, AppColors.PRIMARY));

        wrap.add(grid, BorderLayout.NORTH);
        wrap.add(descScroll, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent createTableSectionUom() {
        JPanel section = new JPanel(new BorderLayout(5, 5));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                "Đơn vị quy đổi", 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY));
        section.setPreferredSize(new Dimension(500, 200));

        uomModel = new ToggleEditableTableModel(new String[]{"Mã ĐV", "Tên ĐV", "Quy đổi về ĐV gốc"}, 0);
        uomModel.setReadOnlyColumns(UOM_COL_ID); // KHÓA mã ĐV
        tblUom = new JTable(uomModel);
        styleTable(tblUom);
        capVisibleRows(tblUom, 5);

        // Spinner cho "Quy đổi về ĐV gốc"
        tblUom.getColumnModel().getColumn(UOM_COL_RATE).setCellEditor(new IntSpinnerEditor(1, Integer.MAX_VALUE, 1));

        JScrollPane scroll = new JScrollPane(tblUom);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        section.add(scroll, BorderLayout.CENTER);

        uomFooterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        uomFooterBar.setOpaque(false);
        btnUomAdd = new JButton("Thêm");
        btnUomDelete = new JButton("Xóa");
        styleButton(btnUomAdd, new Color(40, 167, 69), Color.WHITE);
        styleButton(btnUomDelete, new Color(220, 53, 69), Color.WHITE);

        btnUomAdd.addActionListener(e -> addUomRowAndFocus());
        btnUomDelete.addActionListener(e -> deleteSelectedRow(uomModel, tblUom));
        uomFooterBar.add(btnUomAdd); uomFooterBar.add(btnUomDelete);
        uomFooterBar.setVisible(false);
        section.add(uomFooterBar, BorderLayout.SOUTH);
        return section;
    }

    private JComponent createTableSectionLot() {
        JPanel section = new JPanel(new BorderLayout(5, 5));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                "Lô & hạn sử dụng", 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY));
        section.setPreferredSize(new Dimension(500, 200));

        lotModel = new ToggleEditableTableModel(new String[]{"Mã lô", "Số lượng", "Giá (ĐV gốc)", "HSD", "Tình trạng"}, 0);
        lotModel.setReadOnlyColumns(); // Cho phép edit Mã lô
        tblLot = new JTable(lotModel);
        styleTable(tblLot);
        capVisibleRows(tblLot, 5);

        // thêm ComboBox editor cho cột Tình trạng
        JComboBox<String> cbLotStatEditor = new JComboBox<>(LOT_STATUS_OPTIONS);
        tblLot.getColumnModel().getColumn(LOT_COL_STAT)
                .setCellEditor(new DefaultCellEditor(cbLotStatEditor));

        // Spinner cho "Số lượng"
        tblLot.getColumnModel().getColumn(LOT_COL_QTY).setCellEditor(new IntSpinnerEditor(0, Integer.MAX_VALUE, 1));
        // DatePicker editor cho "HSD"
        tblLot.getColumnModel().getColumn(LOT_COL_HSD).setCellEditor(new DatePickerCellEditor());

        JScrollPane scroll = new JScrollPane(tblLot);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        section.add(scroll, BorderLayout.CENTER);

        lotFooterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        lotFooterBar.setOpaque(false);
        btnLotAdd = new JButton("Thêm");
        btnLotDelete = new JButton("Xóa");
        styleButton(btnLotAdd, new Color(40, 167, 69), Color.WHITE);
        styleButton(btnLotDelete, new Color(220, 53, 69), Color.WHITE);

        btnLotAdd.addActionListener(e -> addLotRowAndFocus());
        btnLotDelete.addActionListener(e -> deleteSelectedRow(lotModel, tblLot));
        lotFooterBar.add(btnLotAdd); lotFooterBar.add(btnLotDelete);
        lotFooterBar.setVisible(false);
        section.add(lotFooterBar, BorderLayout.SOUTH);
        return section;
    }

    // === helpers thêm dòng & focus ===
    private void addUomRowAndFocus() {
        uomModel.addRow(new Object[]{null, "", 1});
        int r = uomModel.getRowCount() - 1;
        tblUom.changeSelection(r, UOM_COL_NAME, false, false);
        if (tblUom.editCellAt(r, UOM_COL_NAME)) {
            Component ed = tblUom.getEditorComponent();
            if (ed != null) ed.requestFocusInWindow();
        }
    }
    private void addLotRowAndFocus() {
        lotModel.addRow(new Object[]{null, 0, 0.0, "", LOT_STATUS_OPTIONS[0]});
        int r = lotModel.getRowCount() - 1;
        tblLot.changeSelection(r, LOT_COL_QTY, false, false);
        tblLot.requestFocusInWindow();
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(COL_BG_MAIN);
        btnEdit = new JButton("Chỉnh sửa");
        btnSave = new JButton("Lưu");
        btnCancel = new JButton("Hủy");

        styleButton(btnEdit,   new Color(255, 153, 0), Color.WHITE);
        styleButton(btnSave,   new Color(40, 167, 69), Color.WHITE);
        styleButton(btnCancel, new Color(220, 53, 69), Color.WHITE);

        btnEdit.addActionListener(e -> {
            int sel = (tblProducts != null) ? tblProducts.getSelectedRow() : -1;
            if (sel < 0) { warn("Vui lòng chọn 1 dòng trong Danh sách sản phẩm trước khi Chỉnh sửa."); return; }
            suppressSelectionEvent = true;
            try { setEditMode(true); }
            finally { SwingUtilities.invokeLater(() -> suppressSelectionEvent = false); }
        });
        btnEdit.setEnabled(false);

        btnSave.addActionListener(e -> {
            stopAllTableEditing(); // commit editor (spinner/date)
            if (!confirm("Bạn có chắc muốn lưu thay đổi?")) return;
            if (!validateBeforeSave()) return;

            // TODO: lưu DB (BUS/DAO) tại đây
            saveProductToDB();

            int idx = tblProducts.getSelectedRow();
            if (idx < 0 && isAddingNew) idx = newProductRowIndex;
            if (idx >= 0) fillModelFromDetails(idx);

            isAddingNew = false; newProductRowIndex = -1;
            setEditMode(false);
        });
        btnCancel.addActionListener(e -> onCancel());

        bar.add(btnEdit);
        SwingUtilities.invokeLater(this::enableEditIfRowSelected);
        return bar;
    }

    // ===================== Helpers =====================
    private JPanel labeled(String text, Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setPreferredSize(new Dimension(160, 25));
        p.add(l, BorderLayout.WEST); p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(FONT_BTN_13B);
        b.setPreferredSize(new Dimension(120, 36));
    }

    private void styleTable(JTable t) {
        t.setRowHeight(26);
        t.setShowGrid(true);
        t.setGridColor(COL_GRID);
        t.setSelectionBackground(COL_SEL_BG);
        t.setSelectionForeground(Color.BLACK);
        t.setBackground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.getTableHeader().setBackground(COL_HEADER_BG);
        t.getTableHeader().setForeground(COL_HEADER_FG);
        t.getTableHeader().setFont(FONT_BTN_13B);
    }

    private void setComponentsEditable(boolean editable) {
        btnChangeImage.setEnabled(editable);

        txtId.setEditable(false);                 // KHÓA MÃ SẢN PHẨM (luôn)
        txtName.setEditable(editable);
        if (txtShortName != null) txtShortName.setEditable(editable);
        txtBarcode.setEditable(editable);
        cbCategoryDetail.setEnabled(editable);
        cbStatusDetail.setEnabled(editable);

        cbFormDetail.setEnabled(editable);
        txtActiveIngredient.setEditable(editable);
        txtManufacturer.setEditable(editable);
        txtStrength.setEditable(editable);
        spVat.setEnabled(editable);
        txtBaseUom.setEditable(editable);
        txtDescription.setEditable(editable);

        uomModel.setEditable(editable);
        lotModel.setEditable(editable);
        if (uomFooterBar != null) uomFooterBar.setVisible(editable);
        if (lotFooterBar != null) lotFooterBar.setVisible(editable);
    }

    private void setEditMode(boolean edit) {
        isEditMode = edit;
        setComponentsEditable(edit);
        actionBar.removeAll();
        if (!edit) actionBar.add(btnEdit);
        else { actionBar.add(btnSave); actionBar.add(btnCancel); }
        actionBar.revalidate(); actionBar.repaint();

        // áp chính sách chi tiết cho từng chế độ
        SwingUtilities.invokeLater(() -> {
            applyEditabilityForMode();
            enableEditIfRowSelected();
        });
    }

    private void applyEditabilityForMode() {
        if (!isEditMode) {
            // View mode: mọi thứ read-only
            btnChangeImage.setEnabled(false);
            txtId.setVisible(true); // Hiển thị ID khi xem
            uomModel.setEditable(false); uomModel.lockRowsBefore(0); uomModel.setAlwaysEditableColumns();
            lotModel.setEditable(false); lotModel.lockRowsBefore(0); lotModel.setAlwaysEditableColumns();
            if (uomFooterBar != null) uomFooterBar.setVisible(false);
            if (lotFooterBar != null) lotFooterBar.setVisible(false);
            return;
        }

        if (isAddingNew) {
            // Thêm mới: cho chỉnh mọi field + mọi cột bảng con
            btnChangeImage.setEnabled(true);

            txtId.setEditable(false);
            txtId.setVisible(false); // Ẩn trường ID khi thêm mới (vì auto-generate)
            txtName.setEditable(true);
            if (txtShortName != null) txtShortName.setEditable(true);
            txtBarcode.setEditable(true);
            cbCategoryDetail.setEnabled(true);
            cbFormDetail.setEnabled(true);
            cbStatusDetail.setEnabled(true);
            txtActiveIngredient.setEditable(true);
            txtManufacturer.setEditable(true);
            txtStrength.setEditable(true);
            spVat.setEnabled(true);
            txtBaseUom.setEditable(true);
            txtDescription.setEditable(true);

            uomModel.setEditable(true); uomModel.lockRowsBefore(0); uomModel.setAlwaysEditableColumns();
            lotModel.setEditable(true); lotModel.lockRowsBefore(0); lotModel.setAlwaysEditableColumns(); // Lot: mã lô edit được
            if (uomFooterBar != null) uomFooterBar.setVisible(true);
            if (lotFooterBar != null) lotFooterBar.setVisible(true);
        } else {
            // Chỉnh sửa SP hiện có:
            // 1) chỉ cho đổi Trạng thái SP
            btnChangeImage.setEnabled(false);

            txtId.setEditable(false);
            txtId.setVisible(true); // Hiển thị ID khi chỉnh sửa
            txtName.setEditable(false);
            if (txtShortName != null) txtShortName.setEditable(false);
            txtBarcode.setEditable(false);
            cbCategoryDetail.setEnabled(false);
            cbFormDetail.setEnabled(false);
            txtActiveIngredient.setEditable(false);
            txtManufacturer.setEditable(false);
            txtStrength.setEditable(false);
            spVat.setEnabled(false);
            txtBaseUom.setEditable(false);
            txtDescription.setEditable(false);
            cbStatusDetail.setEnabled(true);

            // 2) bảng con: chỉ được THÊM dòng mới; dòng cũ read-only,
            //    riêng bảng Lô: cột "Tình trạng" luôn cho phép đổi
            uomModel.setEditable(true);
            uomModel.lockRowsBefore(uomModel.getRowCount());
            uomModel.setAlwaysEditableColumns(); // không có cột nào always-edit

            lotModel.setEditable(true);
            lotModel.lockRowsBefore(lotModel.getRowCount());
            lotModel.setAlwaysEditableColumns(LOT_COL_STAT); // "Tình trạng" luôn edit (cả dòng cũ)

            if (uomFooterBar != null) uomFooterBar.setVisible(true);
            if (lotFooterBar != null) lotFooterBar.setVisible(true);
        }
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser(getProjectImagesDir());
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
        chooser.setAcceptAllFileFilterUsed(true);

        int result = chooser.showOpenDialog(pProduct);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            setImage(file.getAbsolutePath());
        }
    }

    private void setImage(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) { lbImage.setText("No Image"); lbImage.setIcon(null); return; }
            ImageIcon icon = new ImageIcon(path);
            Image scaled = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
            lbImage.setIcon(new ImageIcon(scaled)); lbImage.setText(null);
        } catch (Exception ex) { lbImage.setText("No Image"); lbImage.setIcon(null); }
    }

    private void addRowAndFocus(DefaultTableModel model, JTable table) {
        int cols = model.getColumnCount();
        model.addRow(new Object[cols]);
        int last = model.getRowCount() - 1;

        table.changeSelection(last, 0, false, false);
        table.scrollRectToVisible(table.getCellRect(last, 0, true));

        int startCol = 0;
        for (int c = 0; c < cols; c++) if (model.isCellEditable(last, c)) { startCol = c; break; }

        table.changeSelection(last, startCol, false, false);
        if (table.editCellAt(last, startCol)) {
            Component ed = table.getEditorComponent();
            if (ed != null) ed.requestFocusInWindow();
        } else table.requestFocusInWindow();
    }

    private void deleteSelectedRow(DefaultTableModel model, JTable table) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= model.getRowCount()) return;

        // Chặn xóa dòng cũ ngay từ đầu nếu đang Edit sản phẩm hiện có
        if (model instanceof ToggleEditableTableModel tm) {
            int start = tm.getEditableRowStart();
            if (isEditMode && !isAddingNew && row < start) {
                warn("Chỉ được xóa các dòng mới thêm trong phiên chỉnh sửa.");
                return;
            }
        }

        if (!confirm("Xác nhận xóa dòng đang chọn?")) return;
        model.removeRow(row);

        int next = Math.min(row, model.getRowCount() - 1);
        if (next >= 0) table.changeSelection(next, 0, false, false);
    }

    private void addNewProductRowAndEdit() {
        if (isEditMode && !confirm("Bạn đang chỉnh sửa. Hủy thay đổi hiện tại để thêm sản phẩm mới?")) return;

        productModel.addRow(new Object[productModel.getColumnCount()]);
        int newIndex = productModel.getRowCount() - 1;
        isAddingNew = true; newProductRowIndex = newIndex; currentSelectedRow = newIndex;

        suppressSelectionEvent = true;
        tblProducts.changeSelection(newIndex, 0, false, false);
        tblProducts.scrollRectToVisible(tblProducts.getCellRect(newIndex, 0, true));
        suppressSelectionEvent = false;

        clearProductDetails();        // reset form

        // seed bảng con: KHÔNG auto-add UOM; Lô có thể thêm 1 dòng mặc định
        seedChildTablesForNewProduct();

        // Cho phép edit tất cả dòng vì đây là sản phẩm mới
        uomModel.lockRowsBefore(0);
        lotModel.lockRowsBefore(0);

        setEditMode(true);
        if (txtName != null) txtName.requestFocusInWindow();
    }

    private void seedChildTablesForNewProduct() {
        // làm trống hai bảng
        uomModel.setRowCount(0);
        lotModel.setRowCount(0);
        // Không auto-add UOM nữa
        addLotRowAndFocus(); // vẫn có thể giữ Lô mặc định nếu muốn
    }

    private void clearProductDetails() {
        txtId.setText(""); txtName.setText(""); if (txtShortName != null) txtShortName.setText("");
        txtBarcode.setText("");
        cbCategoryDetail.setSelectedIndex(0); cbStatusDetail.setSelectedIndex(0); cbFormDetail.setSelectedIndex(0);
        txtActiveIngredient.setText(""); txtManufacturer.setText(""); txtStrength.setText("");
        txtBaseUom.setText("viên");                 // ĐVT gốc mặc định
        txtDescription.setText("");
        uomModel.setRowCount(0); lotModel.setRowCount(0);
        applyDefaultVatByCategory();                // VAT theo Loại
    }

    // ===================== Export/Import =====================
    private void exportProductsToCSV() {
        try {
            File downloads = getDownloadsDir();
            JFileChooser chooser = new JFileChooser(downloads);
            chooser.setSelectedFile(new File(downloads, "products_export.csv"));
            if (chooser.showSaveDialog(pProduct) != JFileChooser.APPROVE_OPTION) return;

            File file = chooser.getSelectedFile();
            StringBuilder sb = new StringBuilder(1024);

            // Header
            for (int c = 0; c < productModel.getColumnCount(); c++) {
                sb.append(escapeCsv(productModel.getColumnName(c)));
                if (c < productModel.getColumnCount() - 1) sb.append(',');
            }
            sb.append('\n');

            // Rows
            for (int r = 0; r < productModel.getRowCount(); r++) {
                for (int c = 0; c < productModel.getColumnCount(); c++) {
                    Object val = productModel.getValueAt(r, c);
                    sb.append(escapeCsv(val == null ? "" : String.valueOf(val)));
                    if (c < productModel.getColumnCount() - 1) sb.append(',');
                }
                sb.append('\n');
            }

            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                w.write(sb.toString());
            }
            JOptionPane.showMessageDialog(pProduct, "Đã xuất: " + file.getAbsolutePath(), "Xuất Excel (CSV)", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(pProduct, "Xuất thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String s) {
        boolean needQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String escaped = s.replace("\"", "\"\"");
        return needQuotes ? ("\"" + escaped + "\"") : escaped;
    }

    private File getDownloadsDir() {
        // Cách 1: ~/Downloads (thông dụng)
        File d = new File(System.getProperty("user.home"), "Downloads");
        if (d.exists() && d.isDirectory()) return d;

        // Cách 2: Documents/Downloads
        File sys = FileSystemView.getFileSystemView().getDefaultDirectory();
        if (sys != null && sys.exists()) {
            File dl = new File(sys, "Downloads");
            if (dl.exists() && dl.isDirectory()) return dl;
            return sys; // fallback: Documents
        }
        // Cuối cùng: home
        return new File(System.getProperty("user.home"));
    }

    private void importProductsFromExcel() {
        try {
            File downloads = getDownloadsDir();
            JFileChooser chooser = new JFileChooser(downloads);
            chooser.setDialogTitle("Nhập Excel (CSV)");
            chooser.setFileFilter(new FileNameExtensionFilter("Excel/CSV (*.csv, *.xlsx)", "csv", "xlsx"));

            int result = chooser.showOpenDialog(pProduct);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File file = chooser.getSelectedFile();
            String name = file.getName().toLowerCase();

            if (name.endsWith(".xlsx")) {
                JOptionPane.showMessageDialog(pProduct,
                        "Hiện tại phiên bản này chỉ hỗ trợ nhập CSV.\nVui lòng lưu/xuất Excel thành .CSV rồi nhập lại.",
                        "Chưa hỗ trợ .xlsx", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (!name.endsWith(".csv")) {
                JOptionPane.showMessageDialog(pProduct,
                        "Vui lòng chọn file .csv (Excel có thể lưu ra CSV).",
                        "Định dạng không hỗ trợ", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int imported = 0;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

                String line;
                boolean headerChecked = false;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] cols = parseCsvLine(line);
                    if (!headerChecked) {
                        headerChecked = true;
                        if (cols.length >= 6) {
                            String h0 = cols[0].trim().toLowerCase();
                            if (h0.contains("mã") || h0.equals("id")) continue; // bỏ header
                        }
                    }
                    Object[] rowData = new Object[productModel.getColumnCount()];
                    for (int c = 0; c < Math.min(6, productModel.getColumnCount()); c++) {
                        rowData[c] = (c < cols.length) ? cols[c].trim().replaceAll("^\"|\"$", "") : "";
                    }
                    productModel.addRow(rowData);
                    imported++;
                }
            }

            JOptionPane.showMessageDialog(pProduct,
                    "Đã nhập " + imported + " dòng từ: " + file.getAbsolutePath(),
                    "Nhập Excel (CSV)", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(pProduct,
                    "Không thể nhập file: " + ex.getMessage(),
                    "Lỗi nhập Excel", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Parser CSV đơn giản: hỗ trợ dấu ngoặc kép & dấu phẩy trong ô. */
    private String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++; // escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                out.add(cur.toString()); cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private File getProjectImagesDir() {
        String userDir = System.getProperty("user.dir");
        String[] candidates = {"src/main/resources/images/products"};
        for (String c : candidates) {
            File f = new File(userDir, c);
            if (f.exists() && f.isDirectory()) return f;
        }
        return new File(userDir); // fallback: thư mục project
    }

    // ==== Model tối ưu (BitSet) ====
    private static final class ToggleEditableTableModel extends DefaultTableModel {
        private boolean editable = false;
        private final BitSet readOnlyCols       = new BitSet();
        private final BitSet alwaysEditableCols = new BitSet();
        private int editableRowStart = 0;

        ToggleEditableTableModel(String[] cols, int rows) { super(cols, rows); }

        void setEditable(boolean e) {
            if (this.editable != e) { this.editable = e; fireTableDataChanged(); }
        }
        void setReadOnlyColumns(int... cols) {
            readOnlyCols.clear();
            if (cols != null) for (int c : cols) if (c >= 0) readOnlyCols.set(c);
            fireTableDataChanged();
        }
        void setAlwaysEditableColumns(int... cols) {
            alwaysEditableCols.clear();
            if (cols != null) for (int c : cols) if (c >= 0) alwaysEditableCols.set(c);
            fireTableDataChanged();
        }
        void lockRowsBefore(int rowStart) {
            editableRowStart = Math.max(0, rowStart);
            fireTableDataChanged();
        }
        int  getEditableRowStart() { return editableRowStart; }

        @Override public boolean isCellEditable(int r, int c) {
            if (!editable) return false;
            if (readOnlyCols.get(c)) return false;
            return r >= editableRowStart || alwaysEditableCols.get(c);
        }
    }

    private void enableEditIfRowSelected() {
        if (btnEdit != null && tblProducts != null) {
            btnEdit.setEnabled(tblProducts.getSelectedRow() >= 0);
        }
    }

    // ==== Editors ====
    private static class IntSpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner();
        private final int min, max, step;
        IntSpinnerEditor(int min, int max, int step) {
            this.min = min; this.max = max; this.step = step;
            spinner.setModel(new SpinnerNumberModel(min, min, max, step));
            JComponent ed = spinner.getEditor();
            if (ed instanceof JSpinner.DefaultEditor de) de.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        }
        @Override public Object getCellEditorValue() { return ((Number) spinner.getValue()).intValue(); }
        @Override public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int r, int c) {
            int v = min;
            try { if (val != null && !String.valueOf(val).trim().isEmpty()) v = Integer.parseInt(String.valueOf(val).replaceAll("[^\\d-]", "")); } catch (Exception ignore) {}
            if (v < min) v = min; if (v > max) v = max;
            spinner.setModel(new SpinnerNumberModel(v, min, max, step));
            return spinner;
        }
    }

    /** Editor ngày: dùng DIALOG_DatePicker; không cảnh báo tại đây, chỉ trả về text thô. */
    private class DatePickerCellEditor extends AbstractCellEditor implements TableCellEditor {
        private DIALOG_DatePicker picker;
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            picker = new DIALOG_DatePicker(new Date());
            String s = (value == null) ? "" : String.valueOf(value).trim();
            picker.setTextValue(s);
            picker.addPropertyChangeListener("date", e -> super.stopCellEditing()); // chọn từ lịch -> đóng editor
            return picker;
        }
        @Override public Object getCellEditorValue() { return picker.getTextValue(); }
    }

    private void capVisibleRows(JTable table, int maxRows) {
        int header = table.getTableHeader().getPreferredSize().height;
        int rows = Math.min(table.getRowCount(), maxRows);
        int h = header + table.getRowHeight() * rows + 2;
        table.setPreferredScrollableViewportSize(new Dimension(0, h));
    }

    private void bindProductFromTableRow(int row) {
        if (row < 0 || productModel == null) return;

        String id = valStr(productModel.getValueAt(row, 0));

        // Load full product từ database
        Product product = null;
        if (!id.isEmpty()) {
            product = busProduct.getProductById(id);
        }

        if (product == null) {
            // Fallback: chỉ bind từ table
            String name = valStr(productModel.getValueAt(row, 1));
            String cat = valStr(productModel.getValueAt(row, 2));
            String ingr = valStr(productModel.getValueAt(row, 3));
            String manu = valStr(productModel.getValueAt(row, 4));
            String stat = valStr(productModel.getValueAt(row, 5));

            txtId.setText(id);
            txtId.setVisible(true); // Hiển thị ID khi bind từ DB
            txtName.setText(name);
            if (txtShortName != null) txtShortName.setText("");
            txtBarcode.setText("");
            selectComboItem(cbCategoryDetail, cat);
            selectComboItem(cbStatusDetail, stat);
            txtActiveIngredient.setText(ingr);
            txtManufacturer.setText(manu);
            cbFormDetail.setSelectedIndex(0);
            applyDefaultVatByCategory();
            txtStrength.setText("");
            txtBaseUom.setText("");
            txtDescription.setText("");
            uomModel.setRowCount(0);
            lotModel.setRowCount(0);
            return;
        }

        // Bind full product data
        txtId.setText(product.getId());
        txtId.setVisible(true); // Hiển thị ID khi bind từ DB
        txtName.setText(product.getName() != null ? product.getName() : "");
        if (txtShortName != null) txtShortName.setText(product.getShortName() != null ? product.getShortName() : "");
        txtBarcode.setText(product.getBarcode() != null ? product.getBarcode() : "");

        selectComboItem(cbCategoryDetail, mapCategoryToString(product.getCategory()));
        selectComboItem(cbStatusDetail, "Đang kinh doanh");
        selectComboItem(cbFormDetail, mapFormToString(product.getForm()));

        txtActiveIngredient.setText(product.getActiveIngredient() != null ? product.getActiveIngredient() : "");
        txtManufacturer.setText(product.getManufacturer() != null ? product.getManufacturer() : "");
        txtStrength.setText(product.getStrength() != null ? product.getStrength() : "");
        spVat.setValue(product.getVat());
        txtBaseUom.setText(product.getBaseUnitOfMeasure() != null ? product.getBaseUnitOfMeasure() : "");
        txtDescription.setText(product.getDescription() != null ? product.getDescription() : "");

        // Load UOM
        uomModel.setRowCount(0);
        if (product.getUnitOfMeasureList() != null) {
            for (UnitOfMeasure uom : product.getUnitOfMeasureList()) {
                uomModel.addRow(new Object[]{
                    uom.getId(),
                    uom.getName(),
                    (int) uom.getBaseUnitConversionRate()
                });
            }
        }

        // Load Lots
        lotModel.setRowCount(0);
        if (product.getLotList() != null) {
            for (Lot lot : product.getLotList()) {
                lotModel.addRow(new Object[]{
                    lot.getBatchNumber(),
                    lot.getQuantity(),
                    lot.getRawPrice(),
                    formatLocalDateTime(lot.getExpiryDate()),
                    mapLotStatusToString(lot.getStatus())
                });
            }
        }
    }

    private String valStr(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
    private void selectComboItem(JComboBox<String> cb, String value) {
        if (cb == null || value == null) return;
        for (int i = 0; i < cb.getItemCount(); i++) if (String.valueOf(cb.getItemAt(i)).equalsIgnoreCase(value)) { cb.setSelectedIndex(i); return; }
    }

    private void applyDefaultVatByCategory() {
        String cat = String.valueOf(cbCategoryDetail.getSelectedItem());
        double vat = (cat != null && cat.toLowerCase().contains("sản phẩm chức năng")) ? 10.0 : 5.0;
        spVat.setValue(vat);
    }

    private void onCancel() {
        if (!confirm("Hủy bỏ mọi thay đổi?")) return;
        if (isAddingNew && newProductRowIndex >= 0 && newProductRowIndex < productModel.getRowCount())
            productModel.removeRow(newProductRowIndex);
        isAddingNew = false; newProductRowIndex = -1;

        if (currentSelectedRow >= 0 && currentSelectedRow < productModel.getRowCount()) {
            suppressSelectionEvent = true; tblProducts.setRowSelectionInterval(currentSelectedRow, currentSelectedRow); suppressSelectionEvent = false;
            isBindingFromTable = true; bindProductFromTableRow(currentSelectedRow); isBindingFromTable = false;
        } else clearProductDetails();
        setEditMode(false);
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(pProduct, message, "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    // ===================== VALIDATION =====================
    private boolean validateUomRows(int fromRowIncl) {
        for (int r = fromRowIncl; r < uomModel.getRowCount(); r++) {
            Object name = uomModel.getValueAt(r, UOM_COL_NAME);
            Object conv = uomModel.getValueAt(r, UOM_COL_RATE);
            if (isBlank(name)) { selectAndStartEdit(tblUom, r, UOM_COL_NAME); warn("Vui lòng nhập Tên ĐV ở dòng " + (r+1) + "."); return false; }
            Integer rate = parsePositiveInt(conv);
            if (rate == null)  { selectAndStartEdit(tblUom, r, UOM_COL_RATE); warn("Tỉ lệ quy đổi phải là số nguyên > 0 (dòng " + (r+1) + ")."); return false; }
        }
        return true;
    }
    private boolean validateLotRows(int fromRowIncl) {
        for (int r = fromRowIncl; r < lotModel.getRowCount(); r++) {
            String code = valStr(lotModel.getValueAt(r, LOT_COL_ID));
            if (code.isEmpty()) {
                selectAndStartEdit(tblLot, r, LOT_COL_ID);
                warn("Vui lòng nhập Mã lô (dòng " + (r + 1) + ").");
                return false;
            }
            Integer q = parseNonNegativeInt(lotModel.getValueAt(r, LOT_COL_QTY));
            if (q == null) { selectAndStartEdit(tblLot, r, LOT_COL_QTY);  warn("Số lượng phải là số nguyên ≥ 0 (dòng " + (r+1) + ")."); return false; }
            Double p = parseNonNegativeDouble(lotModel.getValueAt(r, LOT_COL_PRICE));
            if (p == null) { selectAndStartEdit(tblLot, r, LOT_COL_PRICE); warn("Giá phải là số ≥ 0 (dòng " + (r+1) + ")."); return false; }
            String exp = valStr(lotModel.getValueAt(r, LOT_COL_HSD));
            if (exp.isEmpty() || !isValidDateDMY(exp)) {
                selectAndStartEdit(tblLot, r, LOT_COL_HSD);
                warn("HSD không hợp lệ (dòng " + (r+1) + ").\nVui lòng nhập dd/MM/yy hoặc dd/MM/yyyy.");
                return false;
            }
            if (isBlank(lotModel.getValueAt(r, LOT_COL_STAT))) {
                selectAndStartEdit(tblLot, r, LOT_COL_STAT); warn("Vui lòng nhập Tình trạng (dòng " + (r+1) + ")."); return false;
            }
        }
        return true;
    }

    private boolean validateBeforeSave() {
        if (isAddingNew) {
            if (txtName.getText().trim().isEmpty())        { warnAndFocus("Vui lòng nhập Tên sản phẩm.", txtName); return false; }
            if (txtBarcode.getText().trim().isEmpty())     { warnAndFocus("Vui lòng nhập Mã vạch.", txtBarcode); return false; }
            if (cbCategoryDetail.getSelectedItem() == null){ warnAndFocus("Vui lòng chọn Loại sản phẩm.", cbCategoryDetail); return false; }
            if (cbFormDetail.getSelectedItem() == null)    { warnAndFocus("Vui lòng chọn Dạng bào chế.", cbFormDetail); return false; }
            if (cbStatusDetail.getSelectedItem() == null)  { warnAndFocus("Vui lòng chọn Trạng thái.", cbStatusDetail); return false; }
            if (txtBaseUom.getText().trim().isEmpty())     { warnAndFocus("Vui lòng nhập ĐVT gốc.", txtBaseUom); return false; }

            // UOM: cho phép trống; nếu có dòng thì validate từng dòng
            if (!validateUomRows(0)) return false;

            // Lô: yêu cầu ≥ 1 dòng khi thêm mới
            if (lotModel.getRowCount() < 1)                { warnAndFocus("Bảng Lô & hạn sử dụng phải có ít nhất 1 dòng.", btnLotAdd); return false; }
            if (!validateLotRows(0)) return false;

            return true;
        }

        // === Chỉnh sửa SP hiện có ===
        // Chỉ kiểm tra các dòng mới được thêm (>= editableRowStart)
        if (!validateUomRows(uomModel.getEditableRowStart())) return false;
        if (!validateLotRows(lotModel.getEditableRowStart())) return false;

        return true;
    }

    private boolean isBlank(Object v) { return v == null || String.valueOf(v).trim().isEmpty(); }
    private void warn(String msg) { JOptionPane.showMessageDialog(pProduct, msg, "Thiếu thông tin", JOptionPane.WARNING_MESSAGE); }
    private void warnAndFocus(String msg, Component c) { warn(msg); if (c != null) c.requestFocusInWindow(); }

    private void selectAndStartEdit(JTable table, int row, int col) {
        table.changeSelection(row, col, false, false);
        table.scrollRectToVisible(table.getCellRect(row, col, true));
        if (table.editCellAt(row, col)) {
            Component ed = table.getEditorComponent();
            if (ed != null) ed.requestFocusInWindow();
        }
    }

    private void fillModelFromDetails(int row) {
        productModel.setValueAt(valStr(txtId.getText()), row, 0);
        productModel.setValueAt(valStr(txtName.getText()), row, 1);
        productModel.setValueAt(valStr(String.valueOf(cbCategoryDetail.getSelectedItem())), row, 2);
        productModel.setValueAt(valStr(txtActiveIngredient.getText()), row, 3);
        productModel.setValueAt(valStr(txtManufacturer.getText()), row, 4);
        productModel.setValueAt(valStr(String.valueOf(cbStatusDetail.getSelectedItem())), row, 5);
    }

    // ==== Editing helpers ====
    private void stopAllTableEditing() { stopEditing(tblUom); stopEditing(tblLot); }
    private void stopEditing(JTable t) { if (t != null && t.isEditing()) { TableCellEditor ed = t.getCellEditor(); if (ed != null) ed.stopCellEditing(); } }

    // ==== Parsers ====
    private Integer parsePositiveInt(Object v)      { try { String s = String.valueOf(v).trim().replaceAll("\\s", ""); if (s.isEmpty()) return null; s = s.replace(".", "").replace(",", ""); int x = Integer.parseInt(s); return x > 0 ? x : null; } catch (Exception e) { return null; } }
    private Integer parseNonNegativeInt(Object v)   { try { String s = String.valueOf(v).trim().replaceAll("\\s", ""); if (s.isEmpty()) return null; s = s.replace(".", "").replace(",", ""); int x = Integer.parseInt(s); return x >= 0 ? x : null; } catch (Exception e) { return null; } }
    private Double  parseNonNegativeDouble(Object v){ try { String s = String.valueOf(v).trim().replaceAll("\\s", ""); if (s.isEmpty()) return null; if (s.contains(",") && !s.contains(".")) s = s.replace(",", "."); s = s.replaceAll("(?<=\\d)[,\\.](?=\\d{3}(\\D|$))", ""); double d = Double.parseDouble(s); return d >= 0 ? d : null; } catch (Exception e) { return null; } }

    // Strict dd/MM/yy | dd/MM/yyyy
    private boolean isValidDateDMY(String s) {
        if (s == null || (s = s.trim()).isEmpty()) return false;
        String[] ps = {"dd/MM/yy", "d/M/yy", "dd/MM/yyyy", "d/M/yyyy"};
        for (String p : ps) try { SimpleDateFormat f = new SimpleDateFormat(p); f.setLenient(false); f.parse(s); return true; } catch (ParseException ignore) {}
        return false;
    }

    // ===================== DATABASE OPERATIONS =====================

    private void loadAllProducts() {
        try {
            allProducts = busProduct.getAllProducts();
            if (allProducts == null) {
                allProducts = new ArrayList<>();
                warn("Không thể tải danh sách sản phẩm từ database.");
                return;
            }

            productModel.setRowCount(0);
            for (Product p : allProducts) {
                productModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    mapCategoryToString(p.getCategory()),
                    p.getActiveIngredient(),
                    p.getManufacturer(),
                    "Đang kinh doanh" // Default status
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            warn("Lỗi khi tải danh sách sản phẩm: " + e.getMessage());
        }
    }

    private Product getProductFromUI() {
        Product product;

        // Khi UPDATE: load product từ DB (đã có ID)
        // Khi ADD NEW: tạo product mới (Hibernate sẽ tự generate ID)
        if (!isAddingNew) {
            String id = txtId.getText().trim();
            if (!id.isEmpty()) {
                product = busProduct.getProductById(id);
                if (product == null) {
                    product = new Product(); // Fallback nếu không tìm thấy
                }
            } else {
                product = new Product();
            }
        } else {
            product = new Product(); // Product mới, Hibernate sẽ generate ID
        }

        // Cập nhật các thuộc tính (không bao gồm ID)
        product.setName(txtName.getText().trim());
        product.setShortName(txtShortName.getText().trim());
        product.setBarcode(txtBarcode.getText().trim());
        product.setCategory(mapStringToCategory(String.valueOf(cbCategoryDetail.getSelectedItem())));
        product.setForm(mapStringToForm(String.valueOf(cbFormDetail.getSelectedItem())));
        product.setActiveIngredient(txtActiveIngredient.getText().trim());
        product.setManufacturer(txtManufacturer.getText().trim());
        product.setStrength(txtStrength.getText().trim());
        product.setVat(((Number) spVat.getValue()).doubleValue());
        product.setBaseUnitOfMeasure(txtBaseUom.getText().trim());
        product.setDescription(txtDescription.getText().trim());

        // UnitOfMeasure list
        List<UnitOfMeasure> uomList = new ArrayList<>();
        for (int r = 0; r < uomModel.getRowCount(); r++) {
            String uomId = valStr(uomModel.getValueAt(r, UOM_COL_ID));
            String uomName = valStr(uomModel.getValueAt(r, UOM_COL_NAME));
            Integer rate = parsePositiveInt(uomModel.getValueAt(r, UOM_COL_RATE));

            if (!uomName.isEmpty() && rate != null) {
                // Nếu là dòng mới (không có ID), để null để Hibernate tự generate
                if (uomId == null || uomId.isEmpty() || uomId.equals("null")) {
                    uomId = null; // Để Hibernate tự generate UUID
                }
                UnitOfMeasure uom = new UnitOfMeasure(uomId, product, uomName, rate);
                uomList.add(uom);
            }
        }
        product.setUnitOfMeasureList(uomList);

        // Lot list
        List<Lot> lotList = new ArrayList<>();
        for (int r = 0; r < lotModel.getRowCount(); r++) {
            String batchNum = valStr(lotModel.getValueAt(r, LOT_COL_ID));
            Integer qty = parseNonNegativeInt(lotModel.getValueAt(r, LOT_COL_QTY));
            Double price = parseNonNegativeDouble(lotModel.getValueAt(r, LOT_COL_PRICE));
            String expStr = valStr(lotModel.getValueAt(r, LOT_COL_HSD));
            String statusStr = valStr(lotModel.getValueAt(r, LOT_COL_STAT));

            if (!batchNum.isEmpty() && qty != null && price != null && !expStr.isEmpty()) {
                LocalDateTime expiryDate = parseDateToLocalDateTime(expStr);
                LotStatus status = mapStringToLotStatus(statusStr);
                Lot lot = new Lot(batchNum, product, qty, price, expiryDate, status);
                lotList.add(lot);
            }
        }
        product.setLotList(lotList);

        return product;
    }

    private void saveProductToDB() {
        try {
            Product product = getProductFromUI();
            boolean success;

            if (isAddingNew) {
                success = busProduct.addProduct(product);
                if (success) {
                    JOptionPane.showMessageDialog(pProduct,
                        "Thêm sản phẩm mới thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);

                    // Reload data để lấy sản phẩm mới từ DB (với ID đã được generate)
                    loadAllProducts();

                    // Tìm và select sản phẩm mới thêm (dòng cuối cùng)
                    if (productModel.getRowCount() > 0) {
                        int lastRow = productModel.getRowCount() - 1;
                        tblProducts.setRowSelectionInterval(lastRow, lastRow);
                        tblProducts.scrollRectToVisible(tblProducts.getCellRect(lastRow, 0, true));
                    }
                } else {
                    warn("Không thể thêm sản phẩm. Vui lòng kiểm tra lại thông tin.");
                    return;
                }
            } else {
                success = busProduct.updateProduct(product);
                if (success) {
                    JOptionPane.showMessageDialog(pProduct,
                        "Cập nhật sản phẩm thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);

                    // Reload data
                    loadAllProducts();

                    // Giữ selection ở dòng hiện tại
                    if (currentSelectedRow >= 0 && currentSelectedRow < productModel.getRowCount()) {
                        tblProducts.setRowSelectionInterval(currentSelectedRow, currentSelectedRow);
                    }
                } else {
                    warn("Không thể cập nhật sản phẩm. Vui lòng kiểm tra lại thông tin.");
                    return;
                }
            }

            isAddingNew = false;
            newProductRowIndex = -1;
            setEditMode(false);

        } catch (Exception e) {
            e.printStackTrace();
            warn("Lỗi khi lưu sản phẩm: " + e.getMessage());
        }
    }

    // ===================== ENUM MAPPING =====================

    private ProductCategory mapStringToCategory(String s) {
        if (s == null) return ProductCategory.OTC;
        s = s.toLowerCase();
        if (s.contains("kê đơn")) return ProductCategory.ETC;
        if (s.contains("không kê đơn")) return ProductCategory.OTC;
        if (s.contains("chức năng")) return ProductCategory.SUPPLEMENT;
        return ProductCategory.OTC;
    }

    private String mapCategoryToString(ProductCategory cat) {
        if (cat == null) return "Thuốc không kê đơn";
        return switch (cat) {
            case ETC -> "Thuốc kê đơn";
            case OTC -> "Thuốc không kê đơn";
            case SUPPLEMENT -> "Sản phẩm chức năng";
        };
    }

    private DosageForm mapStringToForm(String s) {
        if (s == null) return DosageForm.SOLID;
        s = s.toLowerCase();
        if (s.contains("si rô") || s.contains("nhỏ giọt") || s.contains("súc miệng")) {
            return DosageForm.LIQUID_ORAL_DOSAGE;
        }
        if (s.contains("dung dịch")) {
            return DosageForm.LIQUID_DOSAGE;
        }
        return DosageForm.SOLID;
    }

    private String mapFormToString(DosageForm form) {
        if (form == null) return "Viên nén";
        return switch (form) {
            case SOLID -> "Viên nén";
            case LIQUID_DOSAGE -> "Thuốc bột";
            case LIQUID_ORAL_DOSAGE -> "Si rô";
        };
    }

    private LotStatus mapStringToLotStatus(String s) {
        if (s == null) return LotStatus.AVAILABLE;
        s = s.toLowerCase();
        if (s.contains("hết hạn") || s.contains("expired")) return LotStatus.EXPIRED;
        if (s.contains("lỗi") || s.contains("faulty")) return LotStatus.FAULTY;
        return LotStatus.AVAILABLE;
    }

    private String mapLotStatusToString(LotStatus status) {
        if (status == null) return "Được bán";
        return switch (status) {
            case AVAILABLE -> "Được bán";
            case EXPIRED -> "Hết hạn sử dụng";
            case FAULTY -> "Lỗi nhà sản xuất";
        };
    }

    private LocalDateTime parseDateToLocalDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now().plusYears(2);
        }

        String[] patterns = {"dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                Date date = sdf.parse(dateStr.trim());
                return date.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            } catch (ParseException ignore) {}
        }

        return LocalDateTime.now().plusYears(2);
    }

    private String formatLocalDateTime(LocalDateTime ldt) {
        if (ldt == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date date = Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
        return sdf.format(date);
    }
}
