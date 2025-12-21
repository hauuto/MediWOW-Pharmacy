package com.bus;

import com.dao.DAO_Customer;
import com.entities.PrescribedCustomer;
import com.interfaces.ICustomer;

import java.util.List;

/**
 * @author Tô Thanh Hậu
 */
public class BUS_Customer implements ICustomer {
    private final DAO_Customer daoCustomer;

    public BUS_Customer() {
        this.daoCustomer = new DAO_Customer();
    }

    @Override
    public boolean addCustomer(PrescribedCustomer customer) {
        validateCustomer(customer);
        checkDuplicates(customer);
        return daoCustomer.addCustomer(customer);
    }

    @Override
    public boolean updateCustomer(PrescribedCustomer customer) {
        if (customer == null || customer.getId() == null || customer.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin khách hàng cần cập nhật");
        }

        // Kiểm tra khách hàng có tồn tại không
        PrescribedCustomer existingCustomer = daoCustomer.getCustomerById(customer.getId());
        if (existingCustomer == null) {
            throw new IllegalArgumentException("Khách hàng không tồn tại trong hệ thống");
        }

        // Validate thông tin
        validateCustomer(customer);

        // Kiểm tra trùng lặp (loại trừ chính khách hàng đang cập nhật)
        checkDuplicatesForUpdate(customer);

        return daoCustomer.updateCustomer(customer);
    }

    @Override
    public boolean deleteCustomer(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã khách hàng không hợp lệ");
        }

        PrescribedCustomer customer = daoCustomer.getCustomerById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Khách hàng không tồn tại trong hệ thống");
        }

        return daoCustomer.deleteCustomer(id);
    }

    @Override
    public List<PrescribedCustomer> getAllCustomers() {
        return daoCustomer.getAllCustomers();
    }

    @Override
    public PrescribedCustomer getCustomerById(String id) {
        return daoCustomer.getCustomerById(id);
    }

    @Override
    public PrescribedCustomer getCustomerByPhoneNumber(String phoneNumber) {
        return daoCustomer.getCustomerByPhoneNumber(phoneNumber);
    }

    @Override
    public List<PrescribedCustomer> searchCustomersByName(String name) {
        return daoCustomer.searchCustomersByName(name);
    }

    /**
     * Search top 5 customers by name or phone for omni-search.
     */
    public List<PrescribedCustomer> searchTop5ByNameOrPhone(String keyword) {
        try {
            return daoCustomer.searchTop5ByNameOrPhone(keyword);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return daoCustomer.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId) {
        return daoCustomer.existsByPhoneNumberExcludingId(phoneNumber, excludeId);
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Validate customer information
     */
    private void validateCustomer(PrescribedCustomer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Thông tin khách hàng không được để trống");
        }

        // Validate name
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống");
        }

        if (customer.getName().length() > 255) {
            throw new IllegalArgumentException("Tên khách hàng không được vượt quá 255 ký tự");
        }

        // Validate phone number if provided
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().trim().isEmpty()) {
            String phone = customer.getPhoneNumber().trim();

            if (phone.length() < 10 || phone.length() > 20) {
                throw new IllegalArgumentException("Số điện thoại phải từ 10 đến 20 ký tự");
            }

            // Check if phone number contains only digits and optional leading +
            if (!phone.matches("^\\+?[0-9]+$")) {
                throw new IllegalArgumentException("Số điện thoại chỉ được chứa số và dấu + ở đầu");
            }
        }

        // Validate address if provided
        if (customer.getAddress() != null && customer.getAddress().length() > 500) {
            throw new IllegalArgumentException("Địa chỉ không được vượt quá 500 ký tự");
        }
    }

    /**
     * Check for duplicate phone number when adding new customer
     */
    private void checkDuplicates(PrescribedCustomer customer) {
        // Kiểm tra số điện thoại trùng (nếu có)
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().trim().isEmpty()) {
            if (existsByPhoneNumber(customer.getPhoneNumber())) {
                throw new IllegalArgumentException("Số điện thoại đã tồn tại trong hệ thống");
            }
        }
    }

    /**
     * Check for duplicate phone number when updating customer
     */
    private void checkDuplicatesForUpdate(PrescribedCustomer customer) {
        // Kiểm tra số điện thoại trùng (loại trừ chính khách hàng đang cập nhật)
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().trim().isEmpty()) {
            if (existsByPhoneNumberExcludingId(customer.getPhoneNumber(), customer.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã tồn tại trong hệ thống");
            }
        }
    }
}

