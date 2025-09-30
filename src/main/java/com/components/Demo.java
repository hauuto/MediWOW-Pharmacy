package com.components;

import javax.swing.*;

public class Demo extends JFrame {

    public Demo() {

        JPanel panel = new JPanel();
        panel.add(new MediWOWButton("Bấm vào tôi"));
        panel.add(new MediWOWButton("Primary",MediWOWTheme.PRIMARY));
        panel.add(new MediWOWButton("Secondary",MediWOWTheme.SECONDARY));
        panel.add(new MediWOWButton("Success",MediWOWTheme.SUCCESS));
        panel.add(new MediWOWButton("Danger",MediWOWTheme.DANGER));
        panel.add(new MediWOWButton("Warning",MediWOWTheme.WARNING));
        panel.add(new MediWOWButton("Info",MediWOWTheme.INFO));
        panel.add(new MediWOWButton("Accent",MediWOWTheme.ACCENT));
        panel.add(new MediWOWButton("Light",MediWOWTheme.BACKGROUND));
        panel.add(new MediWOWButton("Dark",MediWOWTheme.TEXT_DARK));
        panel.add(new MediWOWButton("White",MediWOWTheme.WHITE));
        panel.add(new MediWOWButton("Border",MediWOWTheme.BORDER));



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
