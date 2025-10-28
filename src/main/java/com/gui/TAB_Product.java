package com.gui;

import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

/**
 * TAB_Product
 * - Root: public JPanel pProduct (để gắn vào GUI_MainMenu)
 * - Trái: bảng danh sách sản phẩm
 * - Phải: chi tiết sản phẩm (not editable -> Edit mode: editable)
 * - Ảnh mặc định: \src\main\resources\images\products\etc\etc1.jpg
 * - Hai bảng "Đơn vị quy đổi" & "Lô & hạn sử dụng":
 *     + Không đặt độ cao tối thiểu
 *     + Khi Edit mode: hiện footer với nút Thêm/Xóa (thêm 1 dòng mới & focus; xóa dòng đang chọn)
 */
public class TAB_Product {

    // === Root để gắn vào GUI_MainMenu ===
    public JPanel pProduct;

    // === Toolbar (Bộ lọc) ===
    private JTextField txtSearch;
    private JComboBox<String> cbCategory;   // Loại
    private JComboBox<String> cbForm;       // Dạng
    private JComboBox<String> cbStatus;     // Trạng thái
    private JComboBox<String> cbLotStatus;  // Lô

    // === Bảng danh sách trái ===
    private JTable tblProducts;
    private DefaultTableModel productModel;

    // === Chi tiết (phải) ===
    private JLabel lbImage;
    private JButton btnChangeImage;
    private JTextField txtId, txtName, txtBarcode, txtActiveIngredient, txtManufacturer, txtStrength, txtBaseUom;
    private JComboBox<String> cbCategoryDetail, cbFormDetail, cbStatusDetail;
    private JSpinner spVat;
    private JTextArea txtDescription;

    // Bảng UOM & Lot
    private JTable tblUom, tblLot;
    private ToggleEditableTableModel uomModel, lotModel;

    // Footer controls cho 2 bảng (chỉ hiện khi Edit mode)
    private JPanel uomFooterBar, lotFooterBar;
    private JButton btnUomAdd, btnUomDelete, btnLotAdd, btnLotDelete;

    // Action bar
    private JPanel actionBar;
    private JButton btnEdit, btnSave, btnCancel;

    // Trạng thái edit
    private boolean isEditMode = false;

    // Ảnh mặc định
    private static final String DEFAULT_IMG_PATH = "\\src\\main\\resources\\images\\products\\etc\\etc1.jpg";

    public TAB_Product() {
        buildUI();
        setEditMode(false); // mặc định không cho sửa
    }

    // ===================== UI Builder =====================

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
                "QUẢN LÝ SẢN PHẨM",
                0, 0, new Font("Segoe UI", Font.BOLD, 16), AppColors.PRIMARY
        ));

        txtSearch = new JTextField(25);
        JButton btnSearch = new JButton("Tìm kiếm");

        cbCategory = new JComboBox<>(new String[]{
                "Thuốc kê đơn", "Thuốc không kê đơn", "Sản phẩm chức năng"
        });
        cbForm = new JComboBox<>(new String[]{
                "Viên nén", "Viên nang", "Thuốc bột", "Kẹo ngậm", "Si rô", "Thuốc nhỏ giọt", "Súc miệng"
        });
        cbStatus = new JComboBox<>(new String[]{"Đang kinh doanh", "Ngừng kinh doanh"});
        cbLotStatus = new JComboBox<>(new String[]{"Được bán", "Hết hạn sử dụng", "Lỗi nhà sản xuất"});

        JButton btnRefresh = new JButton("Làm mới");
        styleButton(btnSearch, AppColors.PRIMARY, Color.WHITE);
        styleButton(btnRefresh, AppColors.PRIMARY, Color.WHITE);

        top.add(new JLabel("Tìm kiếm:"));
        top.add(txtSearch);
        top.add(btnSearch);

        top.add(new JLabel("Loại:"));
        top.add(cbCategory);

        top.add(new JLabel("Dạng:"));
        top.add(cbForm);

        top.add(new JLabel("Trạng thái:"));
        top.add(cbStatus);

        top.add(new JLabel("Lô:"));
        top.add(cbLotStatus);

        top.add(btnRefresh);

        // (Chỉ thiết kế UI; sự kiện lọc/refresh sẽ implement sau)
        return top;
    }

    private JComponent buildCenter() {
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.6); // 60/40
        mainSplit.setDividerSize(6);
        mainSplit.setBackground(new Color(245, 250, 250));

        mainSplit.setLeftComponent(buildLeftList());
        mainSplit.setRightComponent(buildRightDetail());

        return mainSplit;
    }

    // =========== LEFT ===========

    private JComponent buildLeftList() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 250, 250));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Danh sách sản phẩm",
                0, 0, new Font("Segoe UI", Font.BOLD, 14), AppColors.PRIMARY
        ));

        productModel = new DefaultTableModel(new String[]{
                "Mã", "Tên", "Loại", "Dạng", "Hoạt chất", "VAT(%)", "Trạng thái"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblProducts = new JTable(productModel);
        styleTable(tblProducts);

        JScrollPane scroll = new JScrollPane(tblProducts);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 230, 240)));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // =========== RIGHT ===========

    private JComponent buildRightDetail() {
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(new Color(240, 250, 250));
        right.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Chi tiết sản phẩm",
                0, 0, new Font("Segoe UI", Font.BOLD, 14), AppColors.PRIMARY
        ));

        // ----- BODY trong ScrollPane (padding 2 bên rộng hơn) -----
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(8, 16, 16, 16)); // padding 2 bên lớn hơn

        // Row 0: layout 2 cột (Ảnh + “Đổi ảnh…”) | (5 field: Mã, Tên, Mã vạch, Loại, Trạng thái)
        body.add(buildRow0ImageAndBasicInfo());
        body.add(Box.createVerticalStrut(10));

        // Các row khác (grid + mô tả)
        body.add(buildOtherInfoGrid());
        body.add(Box.createVerticalStrut(10));

        // Bảng Đơn vị quy đổi
        body.add(createTableSectionUom());
        body.add(Box.createVerticalStrut(10));

        // Bảng Lô & hạn sử dụng
        body.add(createTableSectionLot());

        JScrollPane scroll = new JScrollPane(body);
        scroll.getViewport().setBackground(new Color(250, 252, 252));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));

        // ----- ActionBar -----
        actionBar = buildActionBar();

        right.add(scroll, BorderLayout.CENTER);
        right.add(actionBar, BorderLayout.SOUTH);
        return right;
    }

    private JComponent buildRow0ImageAndBasicInfo() {
        JPanel row0 = new JPanel(new GridLayout(1, 2, 12, 0));
        row0.setOpaque(false);

        // LEFT: Ảnh + "Đổi ảnh…"
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        lbImage = new JLabel("No Image", SwingConstants.CENTER);
        lbImage.setPreferredSize(new Dimension(180, 180));
        setImage(DEFAULT_IMG_PATH);

        btnChangeImage = new JButton("Đổi ảnh…");
        styleButton(btnChangeImage, AppColors.PRIMARY, Color.WHITE);
        btnChangeImage.addActionListener(e -> chooseImage());

        left.add(lbImage, BorderLayout.CENTER);
        left.add(btnChangeImage, BorderLayout.SOUTH);

        // RIGHT: 5 field (Mã, Tên, Mã vạch, Loại, Trạng thái)
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new GridLayout(5, 1, 10, 8));

        txtId = new JTextField();
        txtName = new JTextField();
        txtBarcode = new JTextField();
        cbCategoryDetail = new JComboBox<>(new String[]{
                "Thuốc kê đơn", "Thuốc không kê đơn", "Sản phẩm chức năng"
        });
        cbStatusDetail = new JComboBox<>(new String[]{"Đang kinh doanh", "Ngừng kinh doanh"});

        right.add(labeled("Mã:", txtId));
        right.add(labeled("Tên:", txtName));
        right.add(labeled("Mã vạch:", txtBarcode));
        right.add(labeled("Loại:", cbCategoryDetail));
        right.add(labeled("Trạng thái:", cbStatusDetail));

        row0.add(left);
        row0.add(right);
        return row0;
    }

    private JComponent buildOtherInfoGrid() {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(3, 2, 15, 10));
        grid.setOpaque(false);

        cbFormDetail = new JComboBox<>(new String[]{
                "Viên nén", "Viên nang", "Thuốc bột", "Kẹo ngậm", "Si rô", "Thuốc nhỏ giọt", "Súc miệng"
        });
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

        // Mô tả
        txtDescription = new JTextArea(3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        JScrollPane descScroll = new JScrollPane(txtDescription);
        descScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Mô tả", 0, 0, new Font("Segoe UI", Font.BOLD, 12), AppColors.PRIMARY
        ));

        wrap.add(grid, BorderLayout.NORTH);
        wrap.add(descScroll, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent createTableSectionUom() {
        JPanel section = new JPanel(new BorderLayout(5, 5));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                "Đơn vị quy đổi", 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY
        ));
        section.setPreferredSize(new Dimension(500, 200)); // cao 200px cố định


        uomModel = new ToggleEditableTableModel(new String[]{"Mã ĐV", "Tên ĐV", "Quy đổi về ĐV gốc"}, 0);
        tblUom = new JTable(uomModel);
        styleTable(tblUom);
        capVisibleRows(tblUom, 5);

        JScrollPane scroll = new JScrollPane(tblUom);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        section.add(scroll, BorderLayout.CENTER);

        // Footer: Thêm/Xóa (ẩn khi không ở Edit mode)
        uomFooterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        uomFooterBar.setOpaque(false);

        btnUomAdd = new JButton("Thêm");
        btnUomDelete = new JButton("Xóa");
        styleButton(btnUomAdd, new Color(40, 167, 69), Color.WHITE);
        styleButton(btnUomDelete, new Color(220, 53, 69), Color.WHITE);

        btnUomAdd.addActionListener(e -> addRowAndFocus(uomModel, tblUom));
        btnUomDelete.addActionListener(e -> deleteSelectedRow(uomModel, tblUom));

        uomFooterBar.add(btnUomAdd);
        uomFooterBar.add(btnUomDelete);
        uomFooterBar.setVisible(false); // chỉ hiện khi Edit mode

        section.add(uomFooterBar, BorderLayout.SOUTH);
        return section;
    }

    private JComponent createTableSectionLot() {
        JPanel section = new JPanel(new BorderLayout(5, 5));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                "Lô & hạn sử dụng", 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY
        ));
        section.setPreferredSize(new Dimension(500, 200));

        lotModel = new ToggleEditableTableModel(new String[]{
                "Mã lô", "Số lượng", "Giá (ĐV gốc)", "HSD", "Tình trạng"
        }, 0);
        tblLot = new JTable(lotModel);
        styleTable(tblLot);
        capVisibleRows(tblLot, 5);

        JScrollPane scroll = new JScrollPane(tblLot);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        section.add(scroll, BorderLayout.CENTER);

        // Footer: Thêm/Xóa (ẩn khi không ở Edit mode)
        lotFooterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        lotFooterBar.setOpaque(false);

        btnLotAdd = new JButton("Thêm");
        btnLotDelete = new JButton("Xóa");
        styleButton(btnLotAdd, new Color(40, 167, 69), Color.WHITE);
        styleButton(btnLotDelete, new Color(220, 53, 69), Color.WHITE);

        btnLotAdd.addActionListener(e -> addRowAndFocus(lotModel, tblLot));
        btnLotDelete.addActionListener(e -> deleteSelectedRow(lotModel, tblLot));

        lotFooterBar.add(btnLotAdd);
        lotFooterBar.add(btnLotDelete);
        lotFooterBar.setVisible(false); // chỉ hiện khi Edit mode

        section.add(lotFooterBar, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bar.setBackground(new Color(245, 250, 250));

        btnEdit = new JButton("Chỉnh sửa");
        btnSave = new JButton("Lưu");
        btnCancel = new JButton("Hủy");

        styleButton(btnEdit, new Color(255, 153, 0), Color.WHITE);
        styleButton(btnSave, new Color(40, 167, 69), Color.WHITE);
        styleButton(btnCancel, new Color(220, 53, 69), Color.WHITE);

        btnEdit.addActionListener(e -> setEditMode(true));
        btnSave.addActionListener(e -> {
            // TODO: implement lưu sau (BUS/DAO)
            setEditMode(false);
        });
        btnCancel.addActionListener(e -> {
            // TODO: implement revert dữ liệu sau
            setEditMode(false);
        });

        bar.add(btnEdit);
        // (btnSave/btnCancel sẽ được show/hide theo edit mode)
        return bar;
    }

    // ===================== Helpers =====================

    private JPanel labeled(String text, Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setPreferredSize(new Dimension(110, 25));
        p.add(l, BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(120, 36));
    }

    private void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(230, 245, 255));
        table.setSelectionForeground(Color.BLACK);
        table.setBackground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void setComponentsEditable(boolean editable) {
        // Ảnh: cho đổi ảnh khi edit mode
        btnChangeImage.setEnabled(editable);

        // Basic
        txtId.setEditable(editable);
        txtName.setEditable(editable);
        txtBarcode.setEditable(editable);
        cbCategoryDetail.setEnabled(editable);
        cbStatusDetail.setEnabled(editable);

        // Other info
        cbFormDetail.setEnabled(editable);
        txtActiveIngredient.setEditable(editable);
        txtManufacturer.setEditable(editable);
        txtStrength.setEditable(editable);
        spVat.setEnabled(editable);
        txtBaseUom.setEditable(editable);
        txtDescription.setEditable(editable);

        // Tables
        uomModel.setEditable(editable);
        lotModel.setEditable(editable);
    }

    private void setEditMode(boolean edit) {
        isEditMode = edit;
        setComponentsEditable(edit);

        actionBar.removeAll();
        if (!edit) {
            actionBar.add(btnEdit);
        } else {
            actionBar.add(btnSave);
            actionBar.add(btnCancel);
        }
        actionBar.revalidate();
        actionBar.repaint();

        // Hiện/ẩn cụm nút Thêm/Xóa dưới mỗi bảng theo Edit mode
        if (uomFooterBar != null) uomFooterBar.setVisible(edit);
        if (lotFooterBar != null) lotFooterBar.setVisible(edit);
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(pProduct);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            setImage(file.getAbsolutePath());
        }
    }

    private void setImage(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                lbImage.setText("No Image");
                lbImage.setIcon(null);
                return;
            }
            ImageIcon icon = new ImageIcon(path);
            Image scaled = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
            lbImage.setIcon(new ImageIcon(scaled));
            lbImage.setText(null);
        } catch (Exception ex) {
            lbImage.setText("No Image");
            lbImage.setIcon(null);
        }
    }

    // == Helpers cho footer Thêm/Xóa ==
    private void addRowAndFocus(DefaultTableModel model, JTable table) {
        int cols = model.getColumnCount();
        model.addRow(new Object[cols]);
        int last = model.getRowCount() - 1;

        // Chọn, cuộn tới, và mở editor ô đầu tiên
        table.changeSelection(last, 0, false, false);
        table.scrollRectToVisible(table.getCellRect(last, 0, true));
        boolean editing = table.editCellAt(last, 0);
        if (editing) {
            Component editor = table.getEditorComponent();
            if (editor != null) editor.requestFocusInWindow();
        } else {
            table.requestFocusInWindow();
        }
    }

    private void deleteSelectedRow(DefaultTableModel model, JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0 && row < model.getRowCount()) {
            model.removeRow(row);
            int next = Math.min(row, model.getRowCount() - 1);
            if (next >= 0) {
                table.changeSelection(next, 0, false, false);
            }
        }
    }

    // Bảng có thể bật/tắt editable toàn cục
    private static class ToggleEditableTableModel extends DefaultTableModel {
        private boolean editable = false;
        public ToggleEditableTableModel(String[] cols, int rows) { super(cols, rows); }
        public void setEditable(boolean e) { this.editable = e; fireTableDataChanged(); }
        @Override public boolean isCellEditable(int r, int c) { return editable; }
    }

    // Giới hạn số dòng hiển thị cho 2 bảng con
    private void capVisibleRows(JTable table, int maxRows) {
        int header = table.getTableHeader().getPreferredSize().height;
        int rows   = Math.min(table.getRowCount(), maxRows); // không ép min
        int h      = header + table.getRowHeight() * rows + 2; // +2 padding nhẹ
        // Width không quan trọng, scrollpane sẽ giãn theo layout
        table.setPreferredScrollableViewportSize(new Dimension(0, h));
    }
}
