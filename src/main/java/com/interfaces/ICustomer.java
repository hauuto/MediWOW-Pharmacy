package com.interfaces;

import com.entities.Customer;

import java.util.List;

/**
 * @author Tô Thanh Hậu
 */
public interface ICustomer {

    public boolean addCustomer(Customer customer);
    public boolean updateCustomer(Customer customer);
    public boolean deleteCustomer(String id);
    public List<Customer> getAllCustomers();
    public Customer getCustomerById(String id);
    public Customer getCustomerByPhoneNumber(String phoneNumber);
    public List<Customer> searchCustomersByName(String name);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId);
}

