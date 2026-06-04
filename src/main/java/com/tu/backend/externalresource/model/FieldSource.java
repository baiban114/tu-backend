package com.tu.backend.externalresource.model;

public final class FieldSource {

    public static final String AUTO = "auto";
    public static final String MANUAL = "manual";

    private FieldSource() {
    }

    public static boolean isManual(String value) {
        return MANUAL.equalsIgnoreCase(value);
    }

    public static String orAuto(String value) {
        return isManual(value) ? MANUAL : AUTO;
    }
}
