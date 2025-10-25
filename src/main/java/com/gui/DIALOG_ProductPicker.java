package com.gui;

import com.bus.BUS_Product;
import com.entities.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DIALOG_ProductPicker extends JDialog {

    private JTable table;
    private Product selectedProduct = null;

    public DIALOG_ProductPicker(Window owner) {
        super(owner, "Chọn sản phẩm", ModalityType.APPLICATION_MODAL);
        setSize(600, 400);
        setLocationRelativeTo(owner);

        BUS_Product bus = new BUS_Product();
        List<Product> products = bus.getAllProducts();

        String[] cols = {"Mã", "Tên", "Hoạt chất", "Hãng SX"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        for (Product p : products) {
            model.addRow(new Object[]{p.getId(), p.getName(), p.getActiveIngredient(), p.getManufacturer()});
        }

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton btnSelect = new JButton("Chọn");
        btnSelect.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                selectedProduct = products.get(row);
                dispose();
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnSelect, BorderLayout.SOUTH);
    }

    public Product getSelectedProduct() { return selectedProduct; }
}
