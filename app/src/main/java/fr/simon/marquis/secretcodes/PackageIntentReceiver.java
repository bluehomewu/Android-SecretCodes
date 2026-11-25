package fr.simon.marquis.secretcodes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

/**
 * Helper class to look for interesting changes to the installed apps
 * so that the loader can be updated.
 */
public class PackageIntentReceiver extends BroadcastReceiver {
    private final SecretCodeLoader mLoader;

    public PackageIntentReceiver(SecretCodeLoader loader) {
        mLoader = loader;
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        ContextCompat.registerReceiver(mLoader.getContext(), this, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mLoader.onContentChanged();
    }
}