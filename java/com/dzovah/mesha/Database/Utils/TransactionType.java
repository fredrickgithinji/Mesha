package com.dzovah.mesha.Database.Utils;

/**
 * Enumeration of transaction types in the Mesha financial management system.
 * <p>
 * This enum defines the two fundamental types of financial transactions:
 * CREDIT (money in) and DEBIT (money out). These types are used to categorize
 * all financial transactions in the system and determine how they affect account balances.
 * </p>
 * <p>
 * In financial calculations, CREDIT transactions increase the account balance,
 * while DEBIT transactions decrease the account balance.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see com.dzovah.mesha.Database.Entities.Transaction
 * @see TransactionTypeConverter
 */
public enum TransactionType {
    /**
     * Represents a credit transaction (money coming in).
     * Credits increase the account balance.
     * Examples include income, deposits, and transfers in.
     */
    CREDIT,
    
    /**
     * Represents a debit transaction (money going out).
     * Debits decrease the account balance.
     * Examples include expenses, withdrawals, and transfers out.
     */
    DEBIT
}
