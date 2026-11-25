package fr.simon.marquis.secretcodes;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class Crawler {

    private static final String NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String ANDROID_MANIFEST = "AndroidManifest.xml";
    private static final String ATTR_VAL_LABEL = "label";
    private static final String ATTR_VAL_ICON = "icon";
    private static final String ATTR_VAL_HOST = "host";
    private static final String ATTR_VAL_SCHEME = "scheme";
    // AOSP uses android_secret_code; OEMs add their own, e.g. vivo android_vivo_sec_code and iqoo_secret_code
    static final Pattern SECRET_CODE_SCHEME = Pattern.compile("[a-z0-9_]*_sec(ret)?_code");
    private static final String TAG_MANIFEST = "manifest";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_SPLIT = "split";
    private static final String TAG_APPLICATION = "application";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_ACTIVITY_ALIAS = "activity-alias";
    private static final String TAG_SERVICE = "service";
    private static final String TAG_RECEIVER = "receiver";
    private static final String TAG_INTENT_FILTER = "intent-filter";
    private static final String TAG_DATA = "data";

    private Crawler() {
    }

    @NonNull
    public static List<SecretCode> crawl(@NonNull SecretCodeLoader loader) {
        final Context context = loader.getContext();
        final PackageManager pm = loader.pm;
        Set<String> codes = new HashSet<>();
        ArrayList<SecretCode> secretCodes = new ArrayList<>();
        List<PackageInfo> pil = pm.getInstalledPackages(PackageManager.GET_INTENT_FILTERS);
        if (pil == null) {
            return secretCodes;
        }
        long max = pil.size();
        long cur = 0;
        int currentProgress = 0;

        for (PackageInfo pi : pil) {
            if (loader.isLoadInBackgroundCanceled()) {
                return secretCodes;
            }
            cur++;
            if (currentProgress != (int) (100 * cur / max)) {
                currentProgress = (int) (100 * cur / max);
                //CrawlerNotification.notify(getContext(), secretCodes, currentProgress);
            }
            try {
                XmlResourceParser xrp = openBaseManifest(context.createPackageContext(pi.packageName, 0).getAssets(), pi.packageName);

                int applicationLabel = 0;
                int componentLabel = 0;
                int intentFilterLabel = 0;

                int applicationIcon = 0;
                int componentIcon = 0;
                int intentFilterIcon = 0;

                boolean hasSecretCodeScheme = false;
                ArrayList<String> hosts = new ArrayList<>();

                ArrayList<String> path = new ArrayList<>();
                while (xrp.next() != XmlPullParser.END_DOCUMENT) {
                    if (loader.isLoadInBackgroundCanceled()) {
                        return secretCodes;
                    }

                    if (xrp.getEventType() == XmlPullParser.END_TAG) {
                        if (!path.isEmpty()) {
                            path.remove(path.size() - 1);
                        }
                        // <data> attributes merge across the whole intent-filter, so scheme and host may sit on different tags
                        if (TAG_INTENT_FILTER.equals(xrp.getName()) && hasSecretCodeScheme) {
                            for (String c : hosts) {
                                // the same code may be handled by several apps: keep one entry per package
                                if (!TextUtils.isEmpty(c) && codes.add(pi.packageName + "/" + c)) {
                                    String label = getBestString(pi, pm, applicationLabel, componentLabel, intentFilterLabel);
                                    Drawable icon = getBestIcon(pi, pm, applicationIcon, componentIcon, intentFilterIcon);
                                    secretCodes.add(new SecretCode(c, pi.packageName, icon, label));
                                }
                            }
                        }
                    }
                    if (xrp.getEventType() == XmlPullParser.START_TAG) {

                        // application
                        // |
                        // |--- activity, activity-alias, service, receiver
                        // |      |
                        // |      |--- intent filter
                        // |      |      |
                        // |      |      |--- data

                        String name = xrp.getName();
                        path.add(name);
                        switch (name) {
                            case TAG_APPLICATION:
                                applicationLabel = xrp.getAttributeResourceValue(NAMESPACE, ATTR_VAL_LABEL, 0);
                                applicationIcon = xrp.getAttributeResourceValue(NAMESPACE, ATTR_VAL_ICON, 0);
                                break;
                            case TAG_ACTIVITY:
                            case TAG_ACTIVITY_ALIAS:
                            case TAG_SERVICE:
                            case TAG_RECEIVER:
                                componentLabel = xrp.getAttributeResourceValue(NAMESPACE, ATTR_VAL_LABEL, 0);
                                componentIcon = xrp.getAttributeResourceValue(NAMESPACE, ATTR_VAL_ICON, 0);
                                break;
                            case TAG_INTENT_FILTER:
                                intentFilterLabel = xrp.getAttributeResourceValue(NAMESPACE, ATTR_VAL_LABEL, 0);
                                intentFilterIcon = xrp.getAttributeResourceValue(NAMESPACE, ATTR_VAL_ICON, 0);
                                hasSecretCodeScheme = false;
                                hosts.clear();
                                break;
                            case TAG_DATA:
                                String scheme = xrp.getAttributeValue(NAMESPACE, ATTR_VAL_SCHEME);
                                if (scheme != null && SECRET_CODE_SCHEME.matcher(scheme).matches()) {
                                    hasSecretCodeScheme = true;
                                }
                                // android:host="@string/..." is stored as a reference; read the app's string instead of "@id"
                                int hostRes = xrp.getAttributeResourceValue(NAMESPACE, ATTR_VAL_HOST, 0);
                                CharSequence resolved = hostRes != 0 ? pm.getText(pi.packageName, hostRes, pi.applicationInfo) : null;
                                String host = resolved != null ? resolved.toString() : xrp.getAttributeValue(NAMESPACE, ATTR_VAL_HOST);
                                if (host != null) {
                                    hosts.add(host);
                                }
                                break;
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
                e.printStackTrace();
            }
            addRuntimeCodes(pi, pm, codes, secretCodes);
        }
        return secretCodes;
    }

    /**
     * Codes whose receivers are registered at runtime (Context.registerReceiver) never appear in a manifest
     * and no public API lists them, so the well-known ones are added when their app is installed.
     * shortcut: hard-coded list, extend it when new runtime codes are found (dumpsys activity broadcasts).
     */
    private static final String[][] RUNTIME_CODES = {
            {"com.android.server.telecom", "823241", "823240", "826275"},
            {"com.google.android.gms", "426"},
    };

    private static void addRuntimeCodes(PackageInfo pi, PackageManager pm, Set<String> codes, List<SecretCode> secretCodes) {
        for (String[] entry : RUNTIME_CODES) {
            if (!entry[0].equals(pi.packageName)) {
                continue;
            }
            for (int i = 1; i < entry.length; i++) {
                if (codes.add(pi.packageName + "/" + entry[i])) {
                    secretCodes.add(new SecretCode(entry[i], pi.packageName, pi.applicationInfo.loadIcon(pm), String.valueOf(pi.applicationInfo.loadLabel(pm))));
                }
            }
        }
    }


    /**
     * Without a cookie, AssetManager searches its APKs from the last one, which returns a split APK's or an
     * overlay's manifest for apps installed as split APKs (e.g. Play services). Walk the cookies (1-based,
     * framework first) and return the parser positioned on the base APK's &lt;manifest&gt; tag.
     */
    private static XmlResourceParser openBaseManifest(AssetManager assets, String packageName) throws IOException, XmlPullParserException {
        for (int cookie = 1; ; cookie++) {
            XmlResourceParser xrp;
            try {
                xrp = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST);
            } catch (FileNotFoundException e) {
                // shortcut: an APK without a manifest ends the walk early, then we fall back to the default lookup
                return assets.openXmlResourceParser(ANDROID_MANIFEST);
            }
            while (xrp.next() != XmlPullParser.START_TAG && xrp.getEventType() != XmlPullParser.END_DOCUMENT) {
            }
            if (TAG_MANIFEST.equals(xrp.getName())
                    && packageName.equals(xrp.getAttributeValue(null, ATTR_PACKAGE))
                    && xrp.getAttributeValue(null, ATTR_SPLIT) == null) {
                return xrp;
            }
            xrp.close();
        }
    }

    /** Loaded through PackageManager, which also inflates XML/adaptive icons that bitmap decoders cannot read. */
    private static Drawable getBestIcon(PackageInfo p, PackageManager pm, int application, int component, int intentFilter) {
        int icon = intentFilter != 0 ? intentFilter : component != 0 ? component : application;
        Drawable drawable = icon != 0 ? pm.getDrawable(p.packageName, icon, p.applicationInfo) : null;
        return drawable != null ? drawable : p.applicationInfo.loadIcon(pm);
    }

    private static String getBestString(PackageInfo p, PackageManager pm, int application, int component, int intentFilter) {
        return String.valueOf(pm.getText(p.packageName, intentFilter != 0 ? intentFilter : component != 0 ? component : application, p.applicationInfo));
        // p.applicationInfo.loadLabel(pm);
    }
}
