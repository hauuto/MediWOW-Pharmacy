package com.utils;

import java.awt.*;
import java.util.ResourceBundle;

public class AppColors {
    private static final ResourceBundle rb = ResourceBundle.getBundle("colors");

    public static final Color PRIMARY = getColor("color.primary");
    public static final Color SECONDARY = getColor("color.secondary");
    public static final Color DARK = getColor("color.dark");
    public static final Color LIGHT = getColor("color.light");
    public static final Color WHITE = getColor("color.white");
    public static final Color BACKGROUND = getColor("color.background");



    private static Color getColor(String key){
        String hex = rb.getString(key);
        return Color.decode(hex);
    }
}
