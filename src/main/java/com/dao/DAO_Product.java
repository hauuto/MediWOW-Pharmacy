//package com.dao;
//
//import com.connectDB.ConnectDB;
//import com.entities.Lot;
//import com.entities.Product;
//import com.entities.UnitOfMeasure;
//import com.enums.DosageForm;
//import com.enums.LotStatus;
//import com.enums.ProductCategory;
//
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//public class DAO_Product{
//    public List<Product> getAllProducts() {
//        List<Product> productList = new ArrayList<>();
//        Product product = null;
//        Lot lot;
//        UnitOfMeasure uom;
//
//        try {
//            Connection con = ConnectDB.getCon();
//            Statement statement = con.createStatement();
//            ResultSet rs = statement.executeQuery("SELECT * FROM Product" +
//                    " LEFT JOIN Lot ON Product.id = Lot.product" +
//                if (productList.isEmpty() || !productList.get(productList.size() - 1).getId().equals(rs.getString("Product.id"))) {
//                Product product;
//                            rs.getString("Product.id"),
//                    // New product
//                    product = new Product(
//                            currentProductId,
//                            rs.getString(2),
//                            ProductCategory.valueOf(rs.getString(3)),
//                            rs.getString(4).equals("SOLID") ? DosageForm.SOLID_ORAL_DOSAGE : DosageForm.LIQUID_ORAL_DOSAGE,
//                            rs.getString(5),
//                            rs.getString(6),
//                            rs.getString(7),
//                            rs.getString(8),
//                            rs.getFloat(9),
//                            rs.getString(10),
//                            rs.getString(11),
//                            rs.getString(12),
//                            rs.getTimestamp(13) != null ? rs.getTimestamp(13).toLocalDateTime() : null,
//                }
//                else {
//                    productList.add(product);
//                    // Existing product - get reference
//                    if (product.getLotList().isEmpty() || !product.getLotList().get(product.getLotList().size() - 1).getBatchNumber().equals(rs.getString("Lot.batchNumber"))) {
//                        lot = new Lot(
//                                rs.getString("batchNumber"),
//                                product,
//                                rs.getInt("quantity"),
//                                rs.getFloat("mwPrice"),
//                                rs.getTimestamp("expiryDate") != null ? rs.getTimestamp("expiryDate").toLocalDateTime() : null,
//                                LotStatus.valueOf(rs.getString("status"))
//                        );
//                        product.getLotList().add(lot);
//                    }
//                    if (product.getUnitOfMeasureList().isEmpty() || !product.getUnitOfMeasureList().get(product.getUnitOfMeasureList().size() - 1).getId().equals(rs.getString("UnitOfMeasure.id"))) {
//                        uom = new UnitOfMeasure(
//                                rs.getString("UnitOfMeasure.id"),
//                                product,
//                                rs.getString("name"),
//                                rs.getFloat("baseUnitConversionRate")
//                        );
//                        product.getUnitOfMeasureList().add(uom);
//                    }
//                            rs.getDouble(22)
//                    );
//                    product.getUnitOfMeasureList().add(uom);
//                }
//            }
//        } catch (SQLException e) {
//            System.out.println("Error while fetching products");
//            e.printStackTrace();
//        }
//
//        return productList;
//    }
//}
