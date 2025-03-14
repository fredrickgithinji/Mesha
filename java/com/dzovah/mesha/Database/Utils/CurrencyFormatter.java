package com.dzovah.mesha.Database.Utils;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    private static CurrencyType currentCurrency = CurrencyType.getDefault();

    public static void setCurrency(CurrencyType currency) {
        currentCurrency = currency;
    }

    public static String format(double amount) {
        return String.format("%s %.2f", currentCurrency.getSymbol(), amount);
    }
} 