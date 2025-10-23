package com.bus;

import com.dao.StaffDAO;
import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.PasswordUtil;

import java.util.UUID;


/**
 * @author Tô Thanh Hậu
 */

public class StaffBUS implements IStaff {
    private final StaffDAO staffDAO;

    public StaffBUS() {
        this.staffDAO = new StaffDAO();
    }

    @Override
    public boolean addStaff(Staff s) {
        return true;





    }



    public void validateForm(Staff s){



    }


}
