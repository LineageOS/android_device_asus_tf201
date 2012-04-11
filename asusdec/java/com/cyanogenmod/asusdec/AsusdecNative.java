/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.asusdec;

import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;

public final class AsusdecNative {
    private static final String TAG = "AsusdecNative";

    private static final int KEYEVENT_CAUGHT = -1;
    private static final int KEYEVENT_UNCAUGHT = 0;

    private static final int MINIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_DIM + 10;
    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;

    private Context mContext;
    private WifiManager mWifiManager;
    private BluetoothAdapter mBluetoothAdapter;
    private IPowerManager mPowerManager;
    private Intent mSettingsIntent;
    private boolean mTouchpadEnabled = true;
    private boolean mAutomaticAvailable = false;

    static {
        System.loadLibrary("asusdec_jni");
    }

    public AsusdecNative(Context context) {
        mContext = context;

        mSettingsIntent = new Intent(Intent.ACTION_MAIN, null);
        mSettingsIntent.setAction(Settings.ACTION_SETTINGS);
        mSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
    }

    public int handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN
                || event.getRepeatCount() != 0) {
            return KEYEVENT_UNCAUGHT;
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_TOGGLE_WIFI:
                toggleWifi();
                break;
            case KeyEvent.KEYCODE_TOGGLE_BT:
                toggleBluetooth();
                break;
            case KeyEvent.KEYCODE_TOGGLE_TOUCHPAD:
                toggleTouchpad();
                break;
            case KeyEvent.KEYCODE_BRIGHTNESS_DOWN:
                brightnessDown();
                break;
            case KeyEvent.KEYCODE_BRIGHTNESS_UP:
                brightnessUp();
                break;
            case KeyEvent.KEYCODE_BRIGHTNESS_AUTO:
                brightnessAuto();
                break;
            case KeyEvent.KEYCODE_SCREENSHOT:
                takeScreenshot();
                break;
            case KeyEvent.KEYCODE_SETTINGS:
                launchSettings();
                break;

            default:
                return KEYEVENT_UNCAUGHT;
        }

        return KEYEVENT_CAUGHT;
    }

    private void toggleWifi() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }

        int state = mWifiManager.getWifiState();
        int apState = mWifiManager.getWifiApState();

        if (state == WifiManager.WIFI_STATE_ENABLING
                || state == WifiManager.WIFI_STATE_DISABLING) {
            return;
        }
        if (apState == WifiManager.WIFI_AP_STATE_ENABLING
                || apState == WifiManager.WIFI_AP_STATE_DISABLING) {
            return;
        }

        if (state == WifiManager.WIFI_STATE_ENABLED
                || apState == WifiManager.WIFI_AP_STATE_ENABLED) {
            mWifiManager.setWifiEnabled(false);
            mWifiManager.setWifiApEnabled(null, false);

        } else if (state == WifiManager.WIFI_STATE_DISABLED
                && apState == WifiManager.WIFI_AP_STATE_DISABLED) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    private void toggleBluetooth() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        int state = mBluetoothAdapter.getState();

        if (state == BluetoothAdapter.STATE_TURNING_OFF
                || state == BluetoothAdapter.STATE_TURNING_ON) {
            return;
        }
        if (state == BluetoothAdapter.STATE_OFF) {
            mBluetoothAdapter.enable();
        }
        if (state == BluetoothAdapter.STATE_ON) {
            mBluetoothAdapter.disable();
        }
    }

    private void toggleTouchpad() {
        // TODO add native method to get current touchpad status?
        mTouchpadEnabled = !mTouchpadEnabled;
        Log.d(TAG, "Setting touchpad " + mTouchpadEnabled);
        boolean ret = toggleTouchpad(mTouchpadEnabled);
    }


    private void brightnessDown() {
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        int value = getBrightness(MINIMUM_BACKLIGHT);

        value -= 10;
        if (value < MINIMUM_BACKLIGHT) {
            value = MINIMUM_BACKLIGHT;
        }
        setBrightness(value);
    }

    private void brightnessUp() {
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        int value = getBrightness(MAXIMUM_BACKLIGHT);

        value += 10;
        if (value > MAXIMUM_BACKLIGHT) {
            value = MAXIMUM_BACKLIGHT;
        }
        setBrightness(value);
    }

    private void brightnessAuto() {
        if (!mAutomaticAvailable) {
            return;
        }
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    private void setBrightnessMode(int mode) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    private void setBrightness(int value) {
        if (mPowerManager == null) {
            mPowerManager = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
        }
        try {
            mPowerManager.setBacklightBrightness(value);
        } catch (RemoteException ex) {
            Slog.e(TAG, "Could not set backlight brightness", ex);
        }
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, value);
    }

    private int getBrightness(int def) {
        int value = def;
        try {
            value = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException ex) {
        }
        return value;
    }


    private void takeScreenshot() {
        // TODO implement taking of screenshot
        Log.d(TAG, "TODO: implement takeScreenshot");
    }

    private void launchSettings() {
        try {
            mContext.startActivity(mSettingsIntent);
        } catch (ActivityNotFoundException ex) {
            Slog.e(TAG, "Could not launch settings intent", ex);
        }
    }


    /*
     * ------------------------------------------------------------------------
     * Native methods
     */
    native private static boolean toggleTouchpad(boolean status);
}
