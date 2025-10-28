package com.gui;

import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextField;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class TAB_Product {

    // ==== HẰNG SỐ CỘT (tránh magic-number) ====
    private static final int UOM_COL_ID   = 0, UOM_COL_NAME = 1, UOM_COL_RATE   = 2;
    private static final int LOT_COL_ID   = 0, LOT_COL_QTY  = 1, LOT_COL_PRICE  = 2,
            LOT_COL_HSD  = 3, LOT_COL_STAT = 4;

    // ==== Root ====
    public JPanel pProduct;

    // ==== Toolbar ====
    private JTextField txtSearch;
    private JComboBox<String> cbCategory, cbForm, cbStatus, cbLotStatus;
    private JButton btnExportExcel;

    // ==== Danh sách trái ====
    private JTable tblProducts;
    private DefaultTableModel productModel;
    private JButton btnAddProduct;
    private JButton btnImportExcel;

    // ==== Chi tiết phải ====
    private JLabel lbImage;
    private JButton btnChangeImage;
    private JTextField txtId, txtName, txtBarcode, txtActiveIngredient, txtManufacturer, txtStrength, txtBaseUom;
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
    }

    // ===================== UI =====================
    private void buildUI() {
        pProduct = new JPanel(new BorderLayout());
        pProduct.setBackground(new Color(245, 250, 250));
        pProduct.setBorder(new EmptyBorder(10, 10, 10, 10));
        pProduct.add(buildToolbar(), BorderLayout.NORTH);
        pProduct.add(buildCenter(), BorderLayout.CENTER);
    }

    private JComponent buildToolbar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(new Color(245, 250, 250));
        top.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "QUẢN LÝ SẢN PHẨM", 0, 0, new Font("Segoe UI", Font.BOLD, 16), AppColors.PRIMARY));

        txtSearch = new JTextField(18);
        txtSearch.setPreferredSize(new Dimension(220, 30));
        JButton btnSearch = new JButton("Tìm kiếm");

        cbCategory = new JComboBox<>(new String[]{"Tất cả", "Thuốc kê đơn", "Thuốc không kê đơn", "Sản phẩm chức năng"});
        cbForm     = new JComboBox<>(new String[]{"Tất cả", "Viên nén", "Viên nang", "Thuốc bột", "Kẹo ngậm", "Si rô", "Thuốc nhỏ giọt", "Súc miệng"});
        cbStatus   = new JComboBox<>(new String[]{"Tất cả", "Đang kinh doanh", "Ngừng kinh doanh"});
        cbLotStatus= new JComboBox<>(new String[]{"Tất cả", "Được bán", "Hết hạn sử dụng", "Lỗi nhà sản xuất"});

        cbCategory.setSelectedIndex(0); cbForm.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);   cbLotStatus.setSelectedIndex(0);

        btnExportExcel = new JButton("Xuất Excel");
        styleButton(btnSearch, AppColors.PRIMARY, Color.WHITE);
        styleButton(btnExportExcel, AppColors.PRIMARY, Color.WHITE);

        top.add(new JLabel("Tìm kiếm:")); top.add(txtSearch); top.add(btnSearch);
        top.add(new JLabel("Loại:"));     top.add(cbCategory);
        top.add(new JLabel("Dạng:"));     top.add(cbForm);
        top.add(new JLabel("Trạng thái:")); top.add(cbStatus);
        top.add(new JLabel("Lô:"));       top.add(cbLotStatus);
        top.add(btnExportExcel);

        btnExportExcel.addActionListener(e -> exportProductsToCSV());
        return top;
    }

    private JComponent buildCenter() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sp.setResizeWeight(0.6);
        sp.setDividerSize(6);
        sp.setBackground(new Color(245, 250, 250));
        sp.setLeftComponent(buildLeftList());
        sp.setRightComponent(buildRightDetail());
        return sp;
    }

    private JComponent buildLeftList() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 250, 250));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Danh sách sản phẩm", 0, 0, new Font("Segoe UI", Font.BOLD, 14), AppColors.PRIMARY));

        productModel = new DefaultTableModel(new String[]{"Mã", "Tên", "Loại", "Dạng", "Hoạt chất", "VAT(%)", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblProducts = new JTable(productModel);
        styleTable(tblProducts);

        tblProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblProducts.getSelectionModel().addListSelectionListener(e -> {
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
        });
        // === PATCH: cho phép bấm Chỉnh sửa khi đã chọn 1 dòng
        if (btnEdit != null) btnEdit.setEnabled(true);

        JScrollPane scroll = new JScrollPane(tblProducts);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 230, 240)));
        panel.add(scroll, BorderLayout.CENTER);

        // leftFooter
        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        leftFooter.setOpaque(false);

        // btnImportExcel
        btnImportExcel = new JButton("Nhập Excel");
        styleButton(btnImportExcel, new Color(0,123,255), Color.WHITE);
        btnImportExcel.setPreferredSize(new Dimension(150, 36));
        btnImportExcel.addActionListener(e -> importProductsFromExcel());

        // btnAddProduct
        btnAddProduct = new JButton("Thêm sản phẩm mới");
        styleButton(btnAddProduct, new Color(40, 167, 69), Color.WHITE);
        btnAddProduct.setPreferredSize(new Dimension(190, 36));
        btnAddProduct.addActionListener(e -> addNewProductRowAndEdit());

        // Thứ tự hiển thị: [Nhập Excel] [Thêm sản phẩm mới]
        leftFooter.add(btnImportExcel);
        leftFooter.add(btnAddProduct);
        panel.add(leftFooter, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent buildRightDetail() {
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(new Color(240, 250, 250));
        right.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Chi tiết sản phẩm", 0, 0, new Font("Segoe UI", Font.BOLD, 14), AppColors.PRIMARY));

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

        JPanel right = new JPanel(new GridLayout(5, 1, 10, 8));
        right.setOpaque(false);

        txtId = new JTextField();
        txtName = new JTextField();
        txtBarcode = new JTextField();
        cbCategoryDetail = new JComboBox<>(new String[]{"Thuốc kê đơn", "Thuốc không kê đơn", "Sản phẩm chức năng"});
        cbStatusDetail   = new JComboBox<>(new String[]{"Đang kinh doanh", "Ngừng kinh doanh"});

        right.add(labeled("Mã:", txtId));
        right.add(labeled("Tên:", txtName));
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
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Mô tả", 0, 0, new Font("Segoe UI", Font.BOLD, 12), AppColors.PRIMARY));

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
        lotModel.setReadOnlyColumns(LOT_COL_ID); // KHÓA mã lô
        tblLot = new JTable(lotModel);
        styleTable(tblLot);
        capVisibleRows(tblLot, 5);

        // thêm ComboBox editor cho cột Tình trạng
        JComboBox<String> cbLotStatEditor = new JComboBox<>(LOT_STATUS_OPTIONS);
        tblLot.getColumnModel().getColumn(LOT_COL_STAT)
                .setCellEditor(new DefaultCellEditor(cbLotStatEditor));

        // Spinner cho "Số lượng"
        tblLot.getColumnModel().getColumn(LOT_COL_QTY).setCellEditor(new IntSpinnerEditor(0, Integer.MAX_VALUE, 1));
        // DatePicker editor cho "HSD" (chỉ mở khi bấm 📅; cảnh báo dời sang nút Lưu)
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

    // === PATCH: thêm dòng UOM mặc định (rate = 1) và focus vào "Tên ĐV"
    private void addUomRowAndFocus() {
        uomModel.addRow(new Object[]{null, "", 1});
        int r = uomModel.getRowCount() - 1;
        tblUom.changeSelection(r, UOM_COL_NAME, false, false);
        if (tblUom.editCellAt(r, UOM_COL_NAME)) {
            Component ed = tblUom.getEditorComponent();
            if (ed != null) ed.requestFocusInWindow();
        }
    }

    // === PATCH: thêm dòng Lô mặc định (qty=0, price=0, HSD trống, TT = "Được bán")
    private void addLotRowAndFocus() {
        lotModel.addRow(new Object[]{null, 0, 0.0, "", LOT_STATUS_OPTIONS[0]});
        int r = lotModel.getRowCount() - 1;
        tblLot.changeSelection(r, LOT_COL_QTY, false, false);
        tblLot.requestFocusInWindow();
    }


    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(new Color(245, 250, 250));
        btnEdit = new JButton("Chỉnh sửa");
        btnSave = new JButton("Lưu");
        btnCancel = new JButton("Hủy");

        styleButton(btnEdit,   new Color(255, 153, 0), Color.WHITE);
        styleButton(btnSave,   new Color(40, 167, 69), Color.WHITE);
        styleButton(btnCancel, new Color(220, 53, 69), Color.WHITE);

        btnEdit.addActionListener(e -> {
            // === PATCH: bắt buộc chọn 1 dòng trước khi vào Edit
            int sel = tblProducts.getSelectedRow();
            if (sel < 0) {
                warn("Vui lòng chọn 1 dòng trong Danh sách sản phẩm trước khi Chỉnh sửa.");
                return;
            }

            // Khóa các dòng hiện có ở UOM/Lô → chỉ dòng mới thêm trong phiên sửa mới editable
            uomModel.lockRowsBefore(uomModel.getRowCount());
            lotModel.lockRowsBefore(lotModel.getRowCount());

            setEditMode(true);
        });
        // === PATCH: mặc định chưa chọn danh sách → không cho bấm Chỉnh sửa
        btnEdit.setEnabled(false);

        btnSave.addActionListener(e -> {
            stopAllTableEditing();          // commit editor (spinner/date)
            // === PATCH: xác nhận trước khi lưu
            if (!confirm("Bạn có chắc muốn lưu thay đổi?")) return;
            if (!validateBeforeSave()) return;

            // TODO: lưu DB (BUS/DAO) tại đây

            int idx = tblProducts.getSelectedRow();
            if (idx < 0 && isAddingNew) idx = newProductRowIndex;
            if (idx >= 0) fillModelFromDetails(idx);

            isAddingNew = false; newProductRowIndex = -1;
            setEditMode(false);
        });
        btnCancel.addActionListener(e -> onCancel());

        bar.add(btnEdit); // btnSave/btnCancel tự show khi vào edit mode
        return bar;
    }

    // ===================== Helpers =====================
    private JPanel labeled(String text, Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setPreferredSize(new Dimension(110, 25));
        p.add(l, BorderLayout.WEST); p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(120, 36));
    }

    private void styleTable(JTable t) {
        t.setRowHeight(26);
        t.setShowGrid(true);
        t.setGridColor(new Color(220, 220, 220));
        t.setSelectionBackground(new Color(230, 245, 255));
        t.setSelectionForeground(Color.BLACK);
        t.setBackground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.getTableHeader().setBackground(AppColors.PRIMARY);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void setComponentsEditable(boolean editable) {
        btnChangeImage.setEnabled(editable);

        txtId.setEditable(false);                 // KHÓA MÃ SẢN PHẨM (luôn)
        txtName.setEditable(editable);
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
        if (!confirm("Xác nhận xóa dòng đang chọn?")) return;
        model.removeRow(row);
        int next = Math.min(row, model.getRowCount() - 1);
        if (next >= 0) table.changeSelection(next, 0, false, false);
        if (model instanceof ToggleEditableTableModel tm) {
            int start = tm.getEditableRowStart();
            if (isEditMode && !isAddingNew && row < start) {
                warn("Chỉ được xóa các dòng mới thêm trong phiên chỉnh sửa.");
                return;
            }
        }
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

        // === PATCH: seed dòng rỗng mặc định cho UOM & Lô
        seedChildTablesForNewProduct();

        // Cho phép edit tất cả dòng vì đây là sản phẩm mới
        uomModel.lockRowsBefore(0);
        lotModel.lockRowsBefore(0);

        setEditMode(true);
        if (txtName != null) txtName.requestFocusInWindow();
    }

    // === PATCH: seed dòng rỗng khi thêm SP mới
    private void seedChildTablesForNewProduct() {
        // bảo đảm trống
        uomModel.setRowCount(0);
        lotModel.setRowCount(0);
        // thêm 1 dòng mặc định cho mỗi bảng
        addUomRowAndFocus();
        addLotRowAndFocus();
    }

    private void clearProductDetails() {
        txtId.setText(""); txtName.setText(""); txtBarcode.setText("");
        cbCategoryDetail.setSelectedIndex(0); cbStatusDetail.setSelectedIndex(0); cbFormDetail.setSelectedIndex(0);
        txtActiveIngredient.setText(""); txtManufacturer.setText(""); txtStrength.setText("");
        txtBaseUom.setText("viên");                 // ĐVT gốc mặc định
        txtDescription.setText("");
        uomModel.setRowCount(0); lotModel.setRowCount(0);
        applyDefaultVatByCategory();                // VAT theo Loại
    }

    private void exportProductsToCSV() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("products_export.csv"));
            if (chooser.showSaveDialog(pProduct) != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();

            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (int c = 0; c < productModel.getColumnCount(); c++) {
                    pw.print(escapeCsv(productModel.getColumnName(c)));
                    if (c < productModel.getColumnCount() - 1) pw.print(",");
                }
                pw.print("\n");
                for (int r = 0; r < productModel.getRowCount(); r++) {
                    for (int c = 0; c < productModel.getColumnCount(); c++) {
                        Object val = productModel.getValueAt(r, c);
                        pw.print(escapeCsv(val == null ? "" : String.valueOf(val)));
                        if (c < productModel.getColumnCount() - 1) pw.print(",");
                    }
                    pw.print("\n");
                }
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
        // Cách 1: ~/Downloads (thông dụng trên Win/Mac/Linux)
        File d = new File(System.getProperty("user.home"), "Downloads");
        if (d.exists() && d.isDirectory()) return d;

        // Cách 2: thư mục “Documents” mặc định của hệ thống, rồi thử “Downloads”
        File sys = FileSystemView.getFileSystemView().getDefaultDirectory();
        if (sys != null && sys.exists()) {
            File dl = new File(sys, "Downloads");
            if (dl.exists() && dl.isDirectory()) return dl;
            return sys; // fallback: Documents
        }
        // Cuối cùng: home
        return new File(System.getProperty("user.home"));
    }

    private void exportProductsToExcel() {
        try {
            File downloads = getDownloadsDir();
            JFileChooser chooser = new JFileChooser(downloads);
            chooser.setSelectedFile(new File(downloads, "products_export.csv"));

            int result = chooser.showSaveDialog(pProduct);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File file = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

                // Header
                for (int c = 0; c < productModel.getColumnCount(); c++) {
                    pw.print(escapeCsv(productModel.getColumnName(c)));
                    if (c < productModel.getColumnCount() - 1) pw.print(",");
                }
                pw.print("\n");

                // Rows
                for (int r = 0; r < productModel.getRowCount(); r++) {
                    for (int c = 0; c < productModel.getColumnCount(); c++) {
                        Object val = productModel.getValueAt(r, c);
                        pw.print(escapeCsv(val == null ? "" : String.valueOf(val)));
                        if (c < productModel.getColumnCount() - 1) pw.print(",");
                    }
                    pw.print("\n");
                }
            }

            JOptionPane.showMessageDialog(pProduct,
                    "Đã xuất: " + file.getAbsolutePath() + "\n(Excel mở được CSV)",
                    "Xuất Excel (CSV)", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(pProduct,
                    "Xuất thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
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
                        "Hiện tại phiên bản này chỉ hỗ trợ nhập CSV.\n" +
                                "Vui lòng lưu/xuất file Excel thành .CSV rồi nhập lại.",
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
                        // Bỏ header nếu khớp mẫu export
                        if (cols.length >= 7) {
                            String h0 = cols[0].trim().toLowerCase();
                            if (h0.contains("mã") || h0.equals("id")) continue;
                        }
                    }

                    // Map 7 cột đầu: Mã, Tên, Loại, Dạng, Hoạt chất, VAT(%), Trạng thái
                    Object[] row = new Object[productModel.getColumnCount()];
                    for (int c = 0; c < Math.min(7, productModel.getColumnCount()); c++) {
                        row[c] = (c < cols.length) ? cols[c].trim().replaceAll("^\"|\"$", "") : "";
                    }
                    productModel.addRow(row);
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

    // === PATCH: ToggleEditableTableModel có thêm cơ chế khóa theo hàng
    private static class ToggleEditableTableModel extends DefaultTableModel {
        private boolean editable = false;
        private int[] readOnlyColumns = new int[0];
        // Mốc khóa: chỉ các hàng r >= editableRowStart mới được edit
        private int editableRowStart = 0;

        public ToggleEditableTableModel(String[] cols, int rows) { super(cols, rows); }

        public void setEditable(boolean e) {
            this.editable = e;
            fireTableDataChanged();
        }

        public void setReadOnlyColumns(int... cols) {
            this.readOnlyColumns = (cols == null) ? new int[0] : cols.clone();
            fireTableDataChanged();
        }

        public void lockRowsBefore(int rowStart) {
            this.editableRowStart = Math.max(0, rowStart);
            fireTableDataChanged();
        }

        public int getEditableRowStart() { return editableRowStart; }

        private boolean ro(int c) {
            for (int rc : readOnlyColumns) if (rc == c) return true;
            return false;
        }

        @Override public boolean isCellEditable(int r, int c) {
            return editable && !ro(c) && (r >= editableRowStart);
        }
    }

    // ==== Editors ====
    private static class IntSpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner();
        private final int min, max, step;
        IntSpinnerEditor(int min, int max, int step) { this.min = min; this.max = max; this.step = step; spinner.setModel(new SpinnerNumberModel(min, min, max, step));
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
        String id    = valStr(productModel.getValueAt(row, 0));
        String name  = valStr(productModel.getValueAt(row, 1));
        String cat   = valStr(productModel.getValueAt(row, 2));
        String form  = valStr(productModel.getValueAt(row, 3));
        String ingr  = valStr(productModel.getValueAt(row, 4));
        String vatS  = valStr(productModel.getValueAt(row, 5));
        String stat  = valStr(productModel.getValueAt(row, 6));

        txtId.setText(id); txtName.setText(name); txtBarcode.setText("");
        txtActiveIngredient.setText(ingr);
        selectComboItem(cbCategoryDetail, cat);
        selectComboItem(cbFormDetail,     form);
        selectComboItem(cbStatusDetail,   stat);
        spVat.setValue(parseVat(vatS));

        txtManufacturer.setText(""); txtStrength.setText(""); txtBaseUom.setText(""); txtDescription.setText("");
        uomModel.setRowCount(0); lotModel.setRowCount(0);
    }

    private String valStr(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
    private void selectComboItem(JComboBox<String> cb, String value) {
        if (cb == null || value == null) return;
        for (int i = 0; i < cb.getItemCount(); i++) if (String.valueOf(cb.getItemAt(i)).equalsIgnoreCase(value)) { cb.setSelectedIndex(i); return; }
    }
    private double parseVat(String s) { try { s = s.replace("%", "").trim(); return s.isEmpty() ? 0.0 : Double.parseDouble(s); } catch (Exception ex) { return 0.0; } }

    private void applyDefaultVatByCategory() {
        String cat = String.valueOf(cbCategoryDetail.getSelectedItem());
        double vat = (cat != null && cat.toLowerCase().contains("sản phẩm chức năng")) ? 10.0 : 5.0;
        spVat.setValue(vat);
    }

    private void onCancel() {
        if (!confirm("Hủy bỏ mọi thay đổi?")) return;
        if (isAddingNew && newProductRowIndex >= 0 && newProductRowIndex < productModel.getRowCount()) productModel.removeRow(newProductRowIndex);
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
    private boolean validateBeforeSave() {
        // 1) Bắt buộc chính
        if (txtName.getText().trim().isEmpty())              { warnAndFocus("Vui lòng nhập Tên sản phẩm.", txtName); return false; }
        if (txtBarcode.getText().trim().isEmpty())           { warnAndFocus("Vui lòng nhập Mã vạch.", txtBarcode); return false; }
        if (cbCategoryDetail.getSelectedItem() == null)      { warnAndFocus("Vui lòng chọn Loại sản phẩm.", cbCategoryDetail); return false; }
        if (cbFormDetail.getSelectedItem() == null)          { warnAndFocus("Vui lòng chọn Dạng bào chế.", cbFormDetail); return false; }
        if (cbStatusDetail.getSelectedItem() == null)        { warnAndFocus("Vui lòng chọn Trạng thái.", cbStatusDetail); return false; }
        if (txtBaseUom.getText().trim().isEmpty())           { warnAndFocus("Vui lòng nhập ĐVT gốc.", txtBaseUom); return false; }

        // 2) Hai bảng con phải có ít nhất 1 dòng
        if (uomModel.getRowCount() < 1) { warnAndFocus("Bảng Đơn vị quy đổi phải có ít nhất 1 dòng.", btnUomAdd); return false; }
        if (lotModel.getRowCount() < 1) { warnAndFocus("Bảng Lô & hạn sử dụng phải có ít nhất 1 dòng.", btnLotAdd); return false; }

        // 3) UOM từng dòng
        for (int r = 0; r < uomModel.getRowCount(); r++) {
            Object name = uomModel.getValueAt(r, UOM_COL_NAME);
            Object conv = uomModel.getValueAt(r, UOM_COL_RATE);
            if (isBlank(name)) { selectAndStartEdit(tblUom, r, UOM_COL_NAME); warn("Vui lòng nhập Tên ĐV ở dòng " + (r+1) + "."); return false; }
            Integer rate = parsePositiveInt(conv);
            if (rate == null)  { selectAndStartEdit(tblUom, r, UOM_COL_RATE); warn("Tỉ lệ quy đổi phải là số nguyên > 0 (dòng " + (r+1) + ")."); return false; }
        }

        // 4) Lot từng dòng (kể cả HSD – chỉ cảnh báo ở đây)
        for (int r = 0; r < lotModel.getRowCount(); r++) {
            Integer q = parseNonNegativeInt(lotModel.getValueAt(r, LOT_COL_QTY));
            if (q == null) { selectAndStartEdit(tblLot, r, LOT_COL_QTY);  warn("Số lượng phải là số nguyên ≥ 0 (dòng " + (r+1) + ")."); return false; }

            Double p = parseNonNegativeDouble(lotModel.getValueAt(r, LOT_COL_PRICE));
            if (p == null) { selectAndStartEdit(tblLot, r, LOT_COL_PRICE); warn("Giá phải là số ≥ 0 (dòng " + (r+1) + ")."); return false; }

            String exp = valStr(lotModel.getValueAt(r, LOT_COL_HSD));
            if (exp.isEmpty()) { selectAndStartEdit(tblLot, r, LOT_COL_HSD); warn("Vui lòng nhập HSD (dòng " + (r+1) + ")."); return false; }
            if (!isValidDateDMY(exp)) {
                selectAndStartEdit(tblLot, r, LOT_COL_HSD);
                warn("HSD không hợp lệ ở dòng " + (r+1) + ".\nVui lòng nhập dd/MM/yy hoặc dd/MM/yyyy.");
                return false;
            }

            if (isBlank(lotModel.getValueAt(r, LOT_COL_STAT))) {
                selectAndStartEdit(tblLot, r, LOT_COL_STAT); warn("Vui lòng nhập Tình trạng (dòng " + (r+1) + ")."); return false;
            }
        }
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
        productModel.setValueAt(valStr(String.valueOf(cbFormDetail.getSelectedItem())), row, 3);
        productModel.setValueAt(valStr(txtActiveIngredient.getText()), row, 4);
        productModel.setValueAt(String.format("%.1f", ((Number) spVat.getValue()).doubleValue()), row, 5);
        productModel.setValueAt(valStr(String.valueOf(cbStatusDetail.getSelectedItem())), row, 6);
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
}