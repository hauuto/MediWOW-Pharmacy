package com.gui;

import com.utils.ImageButton;

import javax.swing.*;

public class GUI_MainMenu {
    private JPanel pMainMenu;
    private JPanel pLeftHeader;
    private JLabel lblLogo;
    private JPanel pMenu;
    private JPanel pMain;
    private JPanel pRightHeader;
    private JButton btnHome;
    private JButton btnSelling;
    private JButton btnProduct;
    private JButton btnPromotion;
    private JButton btnStatistic;
    private JButton btnStaff;
    private JButton btnCustomer;
    private JButton btnGuideLine;
    private JButton btnLogOut;

    private void createUIComponents() {
        // TODO: place custom component creation code here





    }


    public GUI_MainMenu() {
        initButton();
    }

    private void initButton(){

        ImageButton.setImageButton(btnLogOut, "/images/logout.png");

    }
}
