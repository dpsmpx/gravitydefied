package org.happysanta.gd;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public final class LanguageManager {

    public static final String ENGLISH = "en";
    public static final String RUSSIAN = "ru";
    private static final String PREFERENCES = "GDSettings";
    private static final String LANGUAGE = "language";

    private LanguageManager() {
    }

    public static String getLanguage(Context context) {
        if (context == null) {
            return ENGLISH;
        }
        String language = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(LANGUAGE, ENGLISH);
        return RUSSIAN.equals(language) ? RUSSIAN : ENGLISH;
    }

    public static void setLanguage(Context context, String language) {
        if (context == null) {
            return;
        }
        String normalized = RUSSIAN.equals(language) ? RUSSIAN : ENGLISH;
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(LANGUAGE, normalized)
                .apply();
    }

    public static Context wrap(Context context) {
        String language = getLanguage(context);
        Locale locale = Locale.forLanguageTag(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        }

        configuration.locale = locale;
        return context.createConfigurationContext(configuration);
    }
}
