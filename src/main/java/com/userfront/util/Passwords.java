package com.userfront.util;

import java.util.regex.Pattern;

public final class Passwords {

    private static final Pattern POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,72}$");

    private Passwords() {}

    public static boolean meetsPolicy(String password) {
        return password != null && POLICY.matcher(password).matches();
    }
}
