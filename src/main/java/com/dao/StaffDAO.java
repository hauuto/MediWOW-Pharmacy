package com.dao;

import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * @author Tô Thanh Hậu
 */

public class StaffDAO implements IStaff {
    private final SessionFactory sessionFactory;

    public StaffDAO() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @Override
    public boolean addStaff(Staff s) {
        return false;

    }
}
