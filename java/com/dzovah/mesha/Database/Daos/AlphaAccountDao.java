package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.AlphaAccount;

import java.util.List;

/**
 * Data Access Object (DAO) interface for the AlphaAccount entity.
 * <p>
 * This interface defines the database operations available for AlphaAccount entities,
 * including CRUD operations (Create, Read, Update, Delete) and specialized queries.
 * Room auto-generates the implementation of this interface at compile time.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see AlphaAccount
 */
@Dao
public interface AlphaAccountDao {

    /**
     * Inserts a new AlphaAccount into the database.
     * <p>
     * Room auto-generates the SQL INSERT statement and handles the primary key generation.
     * </p>
     *
     * @param alphaAccount The AlphaAccount object to insert
     */
    @Insert
    void insert(AlphaAccount alphaAccount);

    /**
     * Updates an existing AlphaAccount in the database.
     * <p>
     * Room identifies the account to update based on the primary key of the provided object
     * and auto-generates the SQL UPDATE statement.
     * </p>
     *
     * @param alphaAccount The AlphaAccount object with updated values
     */
    @Update
    void update(AlphaAccount alphaAccount);

    /**
     * Deletes an AlphaAccount from the database.
     * <p>
     * Room identifies the account to delete based on the primary key of the provided object
     * and auto-generates the SQL DELETE statement. Due to the CASCADE relationship defined
     * in the database schema, deleting an AlphaAccount will also delete all associated
     * BetaAccounts and their Transactions.
     * </p>
     *
     * @param alphaAccount The AlphaAccount object to delete
     */
    @Delete
    void delete(AlphaAccount alphaAccount);

    /**
     * Retrieves all AlphaAccounts from the database, ordered by name.
     * <p>
     * This query returns a list of all AlphaAccounts sorted alphabetically by name,
     * which is useful for displaying accounts in a consistent order in the UI.
     * </p>
     *
     * @return A list of all AlphaAccount objects ordered by name
     */
    @Query("SELECT * FROM Alpha_accounts ORDER BY Alpha_account_name")
    List<AlphaAccount> getAllAlphaAccounts();

    /**
     * Retrieves a specific AlphaAccount by its ID.
     * <p>
     * This query is used when detailed information about a specific account is needed,
     * such as when viewing or editing an existing account.
     * </p>
     *
     * @param alphaAccountId The ID of the AlphaAccount to retrieve
     * @return The AlphaAccount object with the specified ID, or null if not found
     */
    @Query("SELECT * FROM Alpha_accounts WHERE Alpha_account_id = :alphaAccountId")
    AlphaAccount getAlphaAccountById(int alphaAccountId);

    /**
     * Calculates the total balance of all AlphaAccounts.
     * <p>
     * This aggregation query is useful for displaying the user's total financial position
     * across all accounts at a glance.
     * </p>
     *
     * @return The sum of balances from all AlphaAccounts
     */
    @Query("SELECT SUM(Alpha_account_balance) FROM Alpha_accounts")
    double getTotalAlphaAccountsBalance();

    /**
     * Searches for AlphaAccounts with names containing the provided search term.
     * <p>
     * This query uses SQL LIKE for partial matching of account names, allowing
     * users to find accounts without knowing their exact names.
     * </p>
     *
     * @param searchQuery The search term to match against account names
     * @return A list of AlphaAccount objects whose names contain the search term
     */
    @Query("SELECT * FROM Alpha_accounts WHERE Alpha_account_name LIKE '%' || :searchQuery || '%'")
    List<AlphaAccount> searchAlphaAccounts(String searchQuery);
}
