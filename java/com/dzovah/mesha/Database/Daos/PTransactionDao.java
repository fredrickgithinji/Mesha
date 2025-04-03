package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.PTransaction;
import com.dzovah.mesha.Database.Utils.TransactionType;

import java.util.List;

/**
 * Data Access Object (DAO) interface for the Transaction entity.
 * <p>
 * This interface defines database operations for Transaction entities, including
 * CRUD operations (Create, Read, Update, Delete) and specialized queries for financial
 * analysis and reporting. Room auto-generates the implementation at compile time.
 * </p>
 * <p>
 * TransactionDao is central to the financial tracking capabilities of the Mesha app,
 * providing methods to record, manipulate, and analyze financial transactions.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see PTransaction
 * @see TransactionType
 */
@Dao
public interface PTransactionDao {

    /**
     * Inserts a new Transaction into the database.
     * <p>
     * Room auto-generates the SQL INSERT statement and handles the primary key generation.
     * After insertion, the account balances should be updated to reflect the new transaction.
     * </p>
     *
     * @param transaction The Transaction object to insert
     */
    @Insert
    void insert(PTransaction transaction);

    /**
     * Updates an existing Transaction in the database.
     * <p>
     * Room identifies the transaction to update based on its primary key and
     * auto-generates the SQL UPDATE statement. After updating, account balances
     * should be recalculated to reflect the changes.
     * </p>
     *
     * @param transaction The Transaction object with updated values
     */
    @Update
    void update(PTransaction transaction);

    /**
     * Deletes a Transaction from the database.
     * <p>
     * Room identifies the transaction to delete based on its primary key and
     * auto-generates the SQL DELETE statement. After deletion, account balances
     * should be updated to reflect the removed transaction.
     * </p>
     *
     * @param transaction The Transaction object to delete
     */
    @Delete
    void delete(PTransaction transaction);

    /**
     * Deletes a Transaction from the database by its ID.
     * <p>
     * This method provides a way to delete a transaction when only its ID is known,
     * without having to retrieve the full Transaction object first.
     * </p>
     *
     * @param transactionId The ID of the Transaction to delete
     */
    @Query("DELETE FROM PTransactions WHERE PTransaction_id = :transactionId")
    void deleteById(int transactionId);

    /**
     * Retrieves all Transactions for a specific BetaAccount, ordered by entry time (newest first).
     * <p>
     * This query is used to display the transaction history for a specific account,
     * with the most recent transactions appearing first.
     * </p>
     *
     * @param betaAccountId The ID of the BetaAccount to retrieve transactions for
     * @return A list of Transaction objects for the specified BetaAccount
     */
    @Query("SELECT * FROM PTransactions WHERE PBeta_account_id = :betaAccountId ORDER BY PEntry_time DESC")
    List<PTransaction> getAllPTransactionsByBetaAccountId(int betaAccountId);

    /**
     * Retrieves Transactions of a specific type for a BetaAccount, ordered by entry time.
     * <p>
     * This query allows filtering transactions by type (CREDIT or DEBIT) for a specific
     * account, which is useful for analyzing income vs. expenses separately.
     * </p>
     *
     * @param betaAccountId The ID of the BetaAccount to retrieve transactions for
     * @param transactionType The type of transactions to retrieve (CREDIT or DEBIT)
     * @return A list of filtered Transaction objects for the specified BetaAccount
     */
    @Query("SELECT * FROM PTransactions WHERE PBeta_account_id = :betaAccountId AND PTransaction_type = :transactionType ORDER BY PEntry_time DESC")
    List<PTransaction> getPTransactionsByType(int betaAccountId, TransactionType transactionType);

    /**
     * Calculates the current balance of a BetaAccount based on its transactions.
     * <p>
     * This query computes the net balance by summing all transaction amounts,
     * considering credits as positive and debits as negative values.
     * </p>
     *
     * @param betaAccountId The ID of the BetaAccount to calculate the balance for
     * @return The calculated balance of the BetaAccount
     */
    @Query("SELECT SUM(CASE WHEN PTransaction_type = 'CREDIT' THEN PTransaction_amount ELSE -PTransaction_amount END) " +
    "FROM PTransactions WHERE PBeta_account_id = :betaAccountId")
    double getPBetaAccountBalance(int betaAccountId);

    /**
     * Retrieves Transactions within a specific time range for a BetaAccount.
     * <p>
     * This query allows filtering transactions by date range, which is useful
     * for periodic financial analysis (e.g., monthly reviews).
     * </p>
     *
     * @param betaAccountId The ID of the BetaAccount to retrieve transactions for
     * @param startTime The start of the time range (as Unix timestamp in milliseconds)
     * @param endTime The end of the time range (as Unix timestamp in milliseconds)
     * @return A list of Transaction objects within the specified time range
     */
    @Query("SELECT * FROM PTransactions WHERE PBeta_account_id = :betaAccountId AND PEntry_time BETWEEN :startTime AND :endTime ORDER BY PEntry_time DESC")
    List<PTransaction> getPTransactionsByTimeRange(int betaAccountId, long startTime, long endTime);

    /**
     * Calculates the total amount for a specific transaction type within a time range.
     * <p>
     * This aggregation query is useful for calculating daily, weekly, or monthly
     * totals for income (CREDIT) or expenses (DEBIT).
     * </p>
     *
     * @param betaAccountId The ID of the BetaAccount to calculate totals for
     * @param transactionType The type of transactions to include (CREDIT or DEBIT)
     * @param startTime The start of the time range (as Unix timestamp in milliseconds)
     * @param endTime The end of the time range (as Unix timestamp in milliseconds)
     * @return The sum of transaction amounts for the specified criteria
     */
    @Query("SELECT SUM(PTransaction_amount) FROM PTransactions WHERE PBeta_account_id = :betaAccountId AND PTransaction_type = :transactionType AND PEntry_time BETWEEN :startTime AND :endTime")
    double calculatePDailyTotal(int betaAccountId, TransactionType transactionType, long startTime, long endTime);

    /**
     * Retrieves all Transactions for a specific AlphaAccount, ordered by entry time.
     * <p>
     * This query aggregates transactions across all BetaAccounts belonging to
     * a specific AlphaAccount, providing a holistic view of financial activity.
     * </p>
     *
     * @param alphaAccountId The ID of the AlphaAccount to retrieve transactions for
     * @return A list of Transaction objects for the specified AlphaAccount
     */
    @Query("SELECT * FROM PTransactions WHERE PAlpha_account_id = :alphaAccountId ORDER BY PEntry_time DESC")
    List<PTransaction> getAllTransactionsByAlphaAccountId(int alphaAccountId);

    /**
     * Retrieves all Transactions in the database, ordered by entry time (newest first).
     * <p>
     * This query provides a comprehensive view of all financial activity across
     * all accounts, which is useful for global financial analysis.
     * </p>
     *
     * @return A list of all Transaction objects ordered by entry time
     */
    @Query("SELECT * FROM PTransactions ORDER BY PEntry_time DESC")
    List<PTransaction> getAllPTransactionsByEntryTime();

    /**
     * Calculates the overall net balance across all accounts.
     * <p>
     * This aggregation query computes the total financial position by summing
     * all transaction amounts, considering credits as positive and debits as negative.
     * </p>
     *
     * @return The net balance across all accounts
     */
    @Query("SELECT SUM(CASE WHEN PTransaction_type = 'CREDIT' THEN PTransaction_amount ELSE -PTransaction_amount END) " +
            "FROM PTransactions")
    double getNetBalance();

    /**
     * Calculates the balance of an AlphaAccount based on its transactions.
     * <p>
     * This query computes the net balance for a specific AlphaAccount by
     * summing credits and subtracting debits across all associated transactions.
     * </p>
     *
     * @param alphaId The ID of the AlphaAccount to calculate the balance for
     * @return The calculated balance of the AlphaAccount
     */
    @Query("SELECT (SELECT COALESCE(SUM(PTransaction_amount), 0) FROM Ptransactions WHERE PAlpha_account_id = :alphaId AND PTransaction_type = 'CREDIT') - " +
       "(SELECT COALESCE(SUM(PTransaction_amount), 0) FROM Ptransactions WHERE PAlpha_account_id = :alphaId AND PTransaction_type = 'DEBIT') " +
       "AS balance")
    double getPAlphaAccountBalanceById(int alphaId);
}
