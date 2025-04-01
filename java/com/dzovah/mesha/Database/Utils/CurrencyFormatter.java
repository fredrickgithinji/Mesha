package com.dzovah.mesha.Database.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for handling currency formatting and preferences in the Mesha application.
 * <p>
 * This class provides methods for formatting numeric values as currency strings with appropriate
 * symbols and formatting, as well as managing currency preference persistence. It uses the
 * {@link CurrencyType} enum to represent different currency options and their symbols.
 * </p>
 * <p>
 * The formatter supports standard currency formatting conventions including:
 * <ul>
 *   <li>Currency symbols (e.g., $, €, ₹)</li>
 *   <li>Thousand separators (e.g., 1,000,000)</li>
 *   <li>Fixed decimal places (always 2 decimal places)</li>
 * </ul>
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see CurrencyType
 */
public class CurrencyFormatter {
    /** SharedPreferences name for storing currency settings */
    private static final String PREFS_NAME = "CurrencyPrefs";
    
    /** SharedPreferences key for the selected currency */
    private static final String KEY_CURRENCY = "selected_currency";
    
    /** Currently selected currency type, initialized with the default value */
    private static CurrencyType currentCurrency = CurrencyType.getDefault();
    
    /** NumberFormat instance configured for currency formatting */
    private static final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.US);

    /**
     * Static initializer block to configure the NumberFormat instance.
     * Sets up standard currency formatting options including:
     * - Always showing 2 decimal places
     * - Using grouping separators for thousands
     */
    static {
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setGroupingUsed(true);
    }

    /**
     * Gets the currently selected currency type.
     * <p>
     * This method returns the current currency type that is being used
     * for formatting monetary values throughout the application.
     * </p>
     *
     * @return The currently selected {@link CurrencyType}
     */
    public static CurrencyType getCurrentCurrency() {
        return currentCurrency;
    }

    /**
     * Sets the current currency type for the application.
     * <p>
     * This method updates the current currency type used for formatting.
     * Note that this method only changes the in-memory value; to persist
     * the selection, use {@link #saveCurrencyPreference(Context, CurrencyType)}.
     * </p>
     *
     * @param currency The {@link CurrencyType} to set as current
     */
    public static void setCurrency(CurrencyType currency) {
        currentCurrency = currency;
    }

    /**
     * Formats a double value into currency format with symbol, commas and 2 decimal places.
     * <p>
     * This method applies the current currency symbol and number formatting rules
     * to convert a raw numeric value into a properly formatted currency string.
     * </p>
     *
     * @param amount The monetary amount to format
     * @return Formatted string with currency symbol (e.g., "$100,000,000.00")
     */
    public static String format(double amount) {
        return String.format("%s %s",
            currentCurrency.getSymbol(), 
            currencyFormat.format(amount)
        );
    }

    /**
     * Loads the saved currency preference from SharedPreferences.
     * <p>
     * This method should be called during application startup to restore
     * the user's preferred currency setting from the last session.
     * </p>
     *
     * @param context The Android context used to access SharedPreferences
     */
    public static void loadCurrencyPreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currencyName = prefs.getString(KEY_CURRENCY, CurrencyType.getDefault().name());
        currentCurrency = CurrencyType.valueOf(currencyName);
    }

    /**
     * Saves the currency preference to SharedPreferences.
     * <p>
     * This method persists the user's currency selection to be restored
     * in future application sessions. It should be called whenever the
     * user changes their currency preference.
     * </p>
     *
     * @param context The Android context used to access SharedPreferences
     * @param currency The {@link CurrencyType} to save as the user's preference
     */
    public static void saveCurrencyPreference(Context context, CurrencyType currency) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_CURRENCY, currency.name())
            .apply();
    }
}