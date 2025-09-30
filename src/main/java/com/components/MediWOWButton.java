package com.components;

import javax.swing.*;
import java.awt.*;

public class MediWOWButton extends JButton {

    private Color backgroundColor;
    private Color hoverColor;
    private Color pressedColor;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public MediWOWButton(String text) {
        this(text, MediWOWTheme.PRIMARY);
    }

    public MediWOWButton(String text, Color bgColor) {
        super(text);
        this.backgroundColor = bgColor;
        this.hoverColor = bgColor.darker();
        this.pressedColor = bgColor.brighter();

        setForeground(MediWOWTheme.TEXT_LIGHT);
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(120, 40));

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                isHovered = true;
                repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                isHovered = false;
                repaint();
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                isPressed = true;
                repaint();
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color currentColor = backgroundColor;
        if (isPressed) {
            currentColor = pressedColor;
        } else if (isHovered) {
            currentColor = hoverColor;
        }

        g2.setColor(currentColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        g2.dispose();
        super.paintComponent(g);
    }



}
