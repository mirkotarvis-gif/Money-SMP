package net.moneysmp.tier;

import java.util.Locale;

public enum Tier {
    S, A, B, C, D, F;

    public static Tier parse(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
