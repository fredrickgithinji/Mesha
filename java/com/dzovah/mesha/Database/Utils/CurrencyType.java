package com.dzovah.mesha.Database.Utils;

/**
 * Enumeration of supported currency types in the Mesha financial management system.
 * <p>
 * This enum defines the various currencies supported by the application, each with
 * its associated symbol for display purposes. The enum provides a centralized way to
 * manage all supported currencies and their display characteristics.
 * </p>
 * <p>
 * Each currency is defined with its standard international symbol, which is used
 * when formatting monetary values for display in the user interface.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see CurrencyFormatter
 */
public enum CurrencyType {
    /**
     * Kenyan Shilling (KSH)
     * The default currency for the Mesha application.
     */
    KSH("Ksh"),
    
    /**
     * United States Dollar (USD)
     * Commonly used for international transactions.
     */
    USD("$"),
    
    /**
     * Euro (EUR)
     * The official currency of most European Union countries.
     */
    EUR("€"),
    
    /**
     * British Pound Sterling (GBP)
     * The official currency of the United Kingdom.
     */
    GBP("£");

    /** The display symbol associated with this currency */
    private final String symbol;

    /**
     * Constructor for the CurrencyType enum.
     * 
     * @param symbol The display symbol to associate with this currency
     */
    CurrencyType(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Gets the symbol associated with this currency.
     * <p>
     * This symbol is used for displaying formatted monetary values
     * in the user interface.
     * </p>
     *
     * @return The currency symbol as a string
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Gets the default currency for the application.
     * <p>
     * This method returns the default currency (KSH) that should be used
     * when no specific currency is selected by the user.
     * </p>
     *
     * @return The default CurrencyType (currently KSH)
     */
    public static CurrencyType getDefault() {
        return KSH;
    }
}