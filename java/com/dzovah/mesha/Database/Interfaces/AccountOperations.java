package com.dzovah.mesha.Database.Interfaces;

/**
 * Common interface for account operations across both normal and hidden accounts.
 * <p>
 * This interface defines the standard operations that any account system should implement,
 * ensuring consistency between normal and hidden account systems.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 */
public interface AccountOperations {
    /**
     * Updates the account balance based on transactions.
     * <p>
     * Implementations should ensure atomicity and data integrity.
     * </p>
     *
     * @return true if the balance was updated successfully, false otherwise
     */
    boolean updateBalance();
    
    /**
     * Validates the account data to ensure integrity.
     * <p>
     * Implementations should check for data consistency and validity.
     * </p>
     *
     * @return true if the account data is valid, false otherwise
     */
    boolean validateData();
    
    /**
     * Gets the account identifier.
     *
     * @return The unique identifier for the account
     */
    int getAccountId();
    
    /**
     * Gets the account name.
     *
     * @return The name of the account
     */
    String getAccountName();
    
    /**
     * Gets the account balance.
     *
     * @return The current balance of the account
     */
    double getAccountBalance();
}
