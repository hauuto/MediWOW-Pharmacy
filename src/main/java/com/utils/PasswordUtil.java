package com.utils;

import java.security.SecureRandom;
import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * @author Tô Thanh Hậu
 * Utility class for password operations
 */
public class PasswordUtil {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final String TEMP_PASSWORD_PREFIX = "Tmp";
    private static final SecureRandom random = new SecureRandom();
    private static final int BCRYPT_LOG_ROUNDS = 12;


    public static String generatePassword() {
        return generateTemporaryPassword();
    }


    public static String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_PREFIX);
        for (int i = 0; i < 5; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }



    public static boolean isTemporaryPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.length() != 8) {
            return false;
        }
        return plainPassword.startsWith(TEMP_PASSWORD_PREFIX);
    }

    public static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("password cannot be null");
        return BCrypt.withDefaults().hashToString(BCRYPT_LOG_ROUNDS, password.toCharArray());
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
            return result.verified;
        } catch (Exception e) {
            return false;
        }
    }
}
