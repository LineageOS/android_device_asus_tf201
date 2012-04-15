
package com.cyanogenmod.settings.device;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;

public class DeviceSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "DeviceSettings";

    public static final String L10N_PREFIX = "asusdec,asusdec-";

    private static final String PREFS_FILE = "device_settings";
    private static final String PREFS_LANG = "lang";
    private static final String PREFERENCE_KEYBOARD_LAYOUT = "keyboard_layout";
    private static final String PREFERENCE_CPU_MODE = "cpu_settings";

    private static final String CPU_PROPERTY = "sys.cpu.mode";
    private static final String KEYSWAP_PROPERTY = "sys.dockkeys.change";

    private ListPreference mKeyboardLayout;
    private ListPreference mCpuMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        mKeyboardLayout = (ListPreference) getPreferenceScreen().findPreference(
                PREFERENCE_KEYBOARD_LAYOUT);
        mKeyboardLayout.setOnPreferenceChangeListener(this);

        String mCurrCpuMode = "1";

        if (SystemProperties.get(CPU_PROPERTY) != null)
            mCurrCpuMode = SystemProperties.get(CPU_PROPERTY);

        mCpuMode = (ListPreference) getPreferenceScreen().findPreference(
                PREFERENCE_CPU_MODE);

        mCpuMode.setValueIndex(getCpuModeOffser(mCurrCpuMode));
        mCpuMode.setOnPreferenceChangeListener(this);
    }

    private int getCpuModeOffser(String mode) {
        if (mode.equals("0"))
            return 0;
        else if (mode.equals("2"))
            return 2;
        else
            return 1;
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference.equals(mKeyboardLayout)) {
            final String newLanguage = (String) value;
            setNewKeyboardLanguage(getApplicationContext(), newLanguage);

        } else if (preference.equals(mCpuMode)) {
            final String newCpuMode = (String) value;
            mCpuMode.setValueIndex(getCpuModeOffser(newCpuMode));
            SystemProperties.set(CPU_PROPERTY, newCpuMode);
        }

        return false;
    }

    private static void setNewKeyboardLanguage(Context context, String language) {
        Log.d(TAG, "Setting new keyboard layout to l10n variant " + language);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE,
                Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFS_LANG, language);
        editor.commit();

        String layout = L10N_PREFIX + language;
        Settings.System.putString(context.getContentResolver(),
                Settings.System.KEYLAYOUT_OVERRIDES, layout);
    }

    public static class BootCompletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != Intent.ACTION_BOOT_COMPLETED) {
                return;
            }

            SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE,
                    Context.MODE_WORLD_READABLE);
            String layout = prefs.getString(PREFS_LANG, "");

            if (layout.equals("")) {
                layout = Locale.getDefault().toString();
                Log.d(TAG, "Setting default locale " + layout + " as keyboard layout");
            }

            setNewKeyboardLanguage(context, layout);
        }
    }
}
