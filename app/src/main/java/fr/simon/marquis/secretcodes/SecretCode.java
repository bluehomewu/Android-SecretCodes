package fr.simon.marquis.secretcodes;

import android.graphics.drawable.Drawable;

class SecretCode {

    private final String code;
    private final String input;
    private final String packageName;
    private final String label;
    private final Drawable icon;

    /** A dialer secret code, entered as *#*#code#*#*. */
    public SecretCode(String code, String packageName, Drawable icon, String label) {
        this(code, "*#*#" + code + "#*#*", packageName, icon, label);
    }

    /** @param input what the user types to run the code, e.g. .12345+= in a calculator */
    public SecretCode(String code, String input, String packageName, Drawable icon, String label) {
        this.code = code;
        this.input = input;
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

    public String getInput() {
        return input;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toString() {
        return "SecretCode{" +
                "code='" + code + '\'' +
                ", input='" + input + '\'' +
                ", packageName='" + packageName + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
