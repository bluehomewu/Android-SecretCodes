package fr.simon.marquis.secretcodes;

import android.graphics.drawable.Drawable;

class SecretCode {

    private final String code;
    private final String packageName;
    private final String label;
    private final Drawable icon;

    public SecretCode(String code, String packageName, Drawable icon, String label) {
        this.code = code;
        this.packageName = packageName;
        this.icon = icon;
        this.label = label;
    }


    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getCode() {
        return code;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toString() {
        return "SecretCode{" +
                "code='" + code + '\'' +
                ", packageName='" + packageName + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
