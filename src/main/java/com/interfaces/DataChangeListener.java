package com.interfaces;

/**
 * Interface listener cho các thay đổi dữ liệu trong hệ thống
 * Được sử dụng để thông báo cho dashboard và các components khác
 * cần cập nhật khi có thay đổi dữ liệu
 *
 * @author Tô Thanh Hậu
 */
public interface DataChangeListener {
    /**
     * Được gọi khi có hóa đơn mới được tạo
     */
    void onInvoiceCreated();

    /**
     * Được gọi khi có sản phẩm được thêm/sửa/xóa
     */
    void onProductChanged();

    /**
     * Được gọi khi có khuyến mãi được thêm/sửa/xóa
     */
    void onPromotionChanged();

    /**
     * Được gọi khi có bất kỳ thay đổi dữ liệu nào
     * (fallback cho các trường hợp chung)
     */
    void onDataChanged();
}

