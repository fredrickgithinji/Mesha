package com.dzovah.mesha.Database.Utils;

public enum CurrencyType {
    KSH("Ksh"),
    USD("$"),
    EUR("€"),
    GBP("£");

    private final String symbol;

    CurrencyType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static CurrencyType getDefault() {
        return KSH;
    }
} 