package com.gui;

import com.entities.Staff;
import com.enums.Role;

import javax.swing.*;
import java.awt.*;

/**
 * Dashboard chính với phân quyền theo Role
 * - MANAGER: Hiển thị TAB_Dashboard_Manager
 * - PHARMACIST: Hiển thị TAB_Dashboard_Pharmacist
 *
 * @author Tô Thanh Hậu
 */
public class TAB_Dashboard extends JPanel {
    private Staff currentStaff;
    private JPanel contentPanel;

    public TAB_Dashboard() {
        this(null);
    }

    public TAB_Dashboard(Staff staff) {
        this.currentStaff = staff;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Determine which dashboard to show based on role
        if (currentStaff != null && currentStaff.getRole() == Role.MANAGER) {
            contentPanel = new TAB_Dashboard_Manager();
        } else if (currentStaff != null && currentStaff.getRole() == Role.PHARMACIST) {
            contentPanel = new TAB_Dashboard_Pharmacist();
        } else {
            // Default to Manager dashboard if no role specified (for backward compatibility)
            contentPanel = new TAB_Dashboard_Manager();
        }

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Public method to refresh dashboard data
     */
    public void refresh() {
        if (contentPanel instanceof TAB_Dashboard_Manager) {
            ((TAB_Dashboard_Manager) contentPanel).refresh();
        } else if (contentPanel instanceof TAB_Dashboard_Pharmacist) {
            ((TAB_Dashboard_Pharmacist) contentPanel).refresh();
        }
    }
}
