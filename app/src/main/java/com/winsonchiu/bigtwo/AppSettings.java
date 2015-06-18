package com.winsonchiu.bigtwo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;

/**
 * Created by TheKeeperOfPie on 1/22/2015.
 */
public class AppSettings {

    public static final String GAME_TYPE = "gameType";
    public static final String TURN_BASED = "Turn Based";
    public static final String REAL_TIME = "Real Time";
    public static final String JSON_INDENT = " ";

    private static SharedPreferences prefs;

    public static void initPrefs(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static void registerListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {

        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void resetFirstGame(boolean isFirst) {
        prefs.edit().putBoolean("is_first_game", isFirst).apply();
    }

    public static boolean isFirstGame() {
        return prefs.getBoolean("is_first_game", true);
    }

    public static boolean useAutoRotate() {
        return prefs.getBoolean("use_auto_rotate", false);
    }

    public static boolean useVibrator() {
        return prefs.getBoolean("use_vibrator", false);
    }

    public static void setUseBackgroundImage(boolean use) {
        prefs.edit().putBoolean("use_background_image", use).apply();
    }

    public static boolean useBackgroundImage() {
        return prefs.getBoolean("use_background_image", false);
    }

    public static void setBackgroundImage(File file) {
        prefs.edit().putString("background_image", file.getAbsolutePath()).commit();
    }

    public static File getBackgroundImage() {
        return new File(prefs.getString("background_image", ""));
    }

    public static void setUseCardImage(boolean use) {
        prefs.edit().putBoolean("use_card_image", use).apply();
    }

    public static boolean useCardImage() {
        return prefs.getBoolean("use_card_image", false);
    }

    public static void setCardImage(File file) {
        prefs.edit().putString("card_image", file.getAbsolutePath()).commit();
    }

    public static File getCardImage() {
        return new File(prefs.getString("card_image", ""));
    }
}