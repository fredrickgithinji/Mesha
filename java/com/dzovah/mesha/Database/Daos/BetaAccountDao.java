package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.BetaAccount;

import java.util.List;

/**
 * Data Access Object (DAO) interface for the BetaAccount entity.
 * <p>
 * This interface defines the database operations available for BetaAccount entities,
 * including CRUD operations (Create, Read, Update, Delete) and specialized queries.
 * Room auto-generates the implementation of this interface at compile time.
 * </p>
 * <p>
 * BetaAccounts are sub-accounts belonging to AlphaAccounts, representing specific
 * financial instruments such as checking accounts, savings accounts, or credit cards.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see BetaAccount
 * @see AlphaAccountDao
 */
@Dao
public interface BetaAccountDao {

    /**
     * Inserts a new BetaAccount into the database.
     * <p>
     * Room auto-generates the SQL INSERT statement and handles the primary key generation.
     * After inserting a BetaAccount, the parent AlphaAccount's balance should be updated.
     * </p>
     *
     * @param betaAccount The BetaAccount object to insert
     */
    @Insert
    void insert(BetaAccount betaAccount);

    /**
     * Updates an existing BetaAccount in the database.
     * <p>
     * Room identifies the account to update based on the primary key of the provided object
     * and auto-generates the SQL UPDATE statement. After updating a BetaAccount,
     * the parent AlphaAccount's balance should be recalculated.
     * </p>
     *
     * @param betaAccount The BetaAccount object with updated values
     */
    @Update
    void update(BetaAccount betaAccount);

    /**
     * Deletes a BetaAccount from the database.
     * <p>
     * Room identifies the account to delete based on the primary key of the provided object
     * and auto-generates the SQL DELETE statement. Due to the CASCADE relationship defined
     * in the database schema, deleting a BetaAccount will also delete all associated
     * Transactions. After deletion, the parent AlphaAccount's balance should be recalculated.
     * </p>
     *
     * @param betaAccount The BetaAccount object to delete
     */
    @Delete
    void delete(BetaAccount betaAccount);

    /**
     * Retrieves all BetaAccounts from the database.
     * <p>
     * This query returns a list of all BetaAccounts, which can be useful for
     * global operations or for providing a complete list of all financial instruments.
     * </p>
     *
     * @return A list of all BetaAccount objects in the database
     */
    @Query("SELECT * FROM Beta_accounts")
    List<BetaAccount> getAllBetaAccounts();

    /**
     * Retrieves a specific BetaAccount by its ID.
     * <p>
     * This query is used when detailed information about a specific BetaAccount is needed,
     * such as when viewing or editing an existing account, or for processing transactions.
     * </p>
     *
     * @param betaAccountId The ID of the BetaAccount to retrieve
     * @return The BetaAccount object with the specified ID, or null if not found
     */
    @Query("SELECT * FROM Beta_accounts WHERE Beta_account_id = :betaAccountId")
    BetaAccount getBetaAccountById(int betaAccountId);

    /**
     * Retrieves all BetaAccounts associated with a specific AlphaAccount.
     * <p>
     * This query returns a list of all BetaAccounts that belong to a given AlphaAccount,
     * which is useful for displaying the structure of sub-accounts within a parent account
     * and for calculating the total balance of the AlphaAccount.
     * </p>
     *
     * @param alphaAccountId The ID of the parent AlphaAccount
     * @return A list of BetaAccount objects associated with the specified AlphaAccount
     */
    @Query("SELECT * FROM Beta_accounts WHERE Alpha_account_id = :alphaAccountId")
    List<BetaAccount> getBetaAccountsByAlphaAccountId(int alphaAccountId);

    /**
     * Updates the balance of an AlphaAccount based on the sum of its BetaAccount balances.
     * <p>
     * This method ensures that the AlphaAccount's balance accurately reflects the combined
     * total of all its associated BetaAccounts. It should be called after any operation
     * that affects BetaAccount balances, such as creating, updating, or deleting accounts
     * or processing transactions.
     * </p>
     * <p>
     * The query uses COALESCE to handle the case where there are no BetaAccounts,
     * defaulting to 0 in that scenario.
     * </p>
     *
     * @param alphaId The ID of the AlphaAccount to update
     */
    @Query("UPDATE Alpha_accounts SET Alpha_account_balance = " +
            "(SELECT COALESCE(SUM(Beta_account_balance), 0) FROM Beta_accounts " +
            "WHERE Alpha_account_id = :alphaId) " +
            "WHERE Alpha_account_id = :alphaId")
    void updateAlphaAccountBalance(int alphaId);
}
