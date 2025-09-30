package com.components;

import javax.swing.*;

public class Demo extends JFrame {

    public Demo() {

        JPanel panel = new JPanel();
        panel.add(new MediWOWButton("Bấm vào tôi"));

        this.add(panel);



    }


    public static void main(String[] args) {

        JFrame frame = new Demo();
        frame.setTitle("MediWOW Components Demo");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }






}
