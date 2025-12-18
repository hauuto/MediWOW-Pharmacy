package com.interfaces;

import com.entities.PrescribedCustomer;

import java.util.List;

/**
 * @author Tô Thanh Hậu
 */
public interface ICustomer {

    public boolean addCustomer(PrescribedCustomer customer);
    public boolean updateCustomer(PrescribedCustomer customer);
    public boolean deleteCustomer(String id);
    public List<PrescribedCustomer> getAllCustomers();
    public PrescribedCustomer getCustomerById(String id);
    public PrescribedCustomer getCustomerByPhoneNumber(String phoneNumber);
    public List<PrescribedCustomer> searchCustomersByName(String name);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId);
}

