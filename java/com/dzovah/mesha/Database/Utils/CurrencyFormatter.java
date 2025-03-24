package com.dzovah.mesha.Database.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    private static final String PREFS_NAME = "CurrencyPrefs";
    private static final String KEY_CURRENCY = "selected_currency";
    private static CurrencyType currentCurrency = CurrencyType.getDefault();
    private static final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.US);

    static {
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setGroupingUsed(true);
    }

    /**
     * Gets the currently selected currency
     */
    public static CurrencyType getCurrentCurrency() {
        return currentCurrency;
    }

    /**
     * Sets the current currency and saves it to SharedPreferences
     */
    public static void setCurrency(CurrencyType currency) {
        currentCurrency = currency;
    }

    /**
     * Formats a double value into currency format with symbol, commas and 2 decimal places
     * @param amount The amount to format
     * @return Formatted string (e.g., "$100,000,000.00")
     */
    public static String format(double amount) {
        return String.format("%s %s",
            currentCurrency.getSymbol(), 
            currencyFormat.format(amount)
        );
    }

    /**
     * Loads the saved currency preference from SharedPreferences
     */
    public static void loadCurrencyPreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currencyName = prefs.getString(KEY_CURRENCY, CurrencyType.getDefault().name());
        currentCurrency = CurrencyType.valueOf(currencyName);
    }

    /**
     * Saves the currency preference to SharedPreferences
     */
    public static void saveCurrencyPreference(Context context, CurrencyType currency) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_CURRENCY, currency.name())
            .apply();
    }
} 