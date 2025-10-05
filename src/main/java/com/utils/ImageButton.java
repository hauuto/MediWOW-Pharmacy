package com.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

public class ImageButton{


    public static void setImageButton(JButton btn, String imagePath){
        URL url = ImageButton.class.getResource(imagePath);
        if (url == null){
            System.err.println("Resource not found: " + imagePath);
            return;
        }

        ImageIcon icon = new ImageIcon(url);

        Image scaledImage = icon.getImage().getScaledInstance(btn.getWidth(), btn.getHeight(), java.awt.Image.SCALE_SMOOTH);
        btn.setIcon(new ImageIcon(scaledImage));

        btn.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Image resizedImage = icon.getImage().getScaledInstance(btn.getWidth(), btn.getHeight(), Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(resizedImage));
            }
        });
    }


}
