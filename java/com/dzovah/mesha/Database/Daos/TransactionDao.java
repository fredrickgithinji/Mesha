package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.Utils.TransactionType;

import java.util.List;

@Dao
public interface TransactionDao {

    // Insert a new transaction
    @Insert
    void insert(Transaction transaction);

    // Update an existing transaction
    @Update
    void update(Transaction transaction);

    // Delete a transaction
    @Delete
    void delete(Transaction transaction);

    // Delete a transaction by its ID
    @Query("DELETE FROM Transactions WHERE Transaction_id = :transactionId")
    void deleteById(int transactionId);

    // Retrieve all transactions for a Beta Account
    @Query("SELECT * FROM Transactions WHERE Beta_account_id = :betaAccountId ORDER BY Entry_time DESC")
    List<Transaction> getAllTransactionsByBetaAccountId(int betaAccountId);

    // Retrieve transactions by type for a Beta Account
    @Query("SELECT * FROM Transactions WHERE Beta_account_id = :betaAccountId AND Transaction_type = :transactionType ORDER BY Entry_time DESC")
    List<Transaction> getTransactionsByType(int betaAccountId, TransactionType transactionType);

    @Query("SELECT SUM(CASE WHEN Transaction_type = 'CREDIT' THEN Transaction_amount ELSE -Transaction_amount END) " +
    "FROM Transactions WHERE Beta_account_id = :betaAccountId")
double getBetaAccountBalance(int betaAccountId);

    // Retrieve transactions within a time range for a Beta Account
    @Query("SELECT * FROM Transactions WHERE Beta_account_id = :betaAccountId AND Entry_time BETWEEN :startTime AND :endTime ORDER BY Entry_time DESC")
    List<Transaction> getTransactionsByTimeRange(int betaAccountId, long startTime, long endTime);

    // Calculate daily totals for a specific transaction type
    @Query("SELECT SUM(Transaction_amount) FROM Transactions WHERE Beta_account_id = :betaAccountId AND Transaction_type = :transactionType AND Entry_time BETWEEN :startTime AND :endTime")
    double calculateDailyTotal(int betaAccountId, TransactionType transactionType, long startTime, long endTime);

    // Get all transactions for an Alpha Account
    @Query("SELECT * FROM Transactions WHERE Alpha_account_id = :alphaAccountId ORDER BY Entry_time DESC")
    List<Transaction> getAllTransactionsByAlphaAccountId(int alphaAccountId);

    @Query("SELECT * FROM Transactions ORDER BY Entry_time DESC")
    List<Transaction> getAllTransactionsByEntryTime();

    @Query("SELECT SUM(CASE WHEN Transaction_type = 'CREDIT' THEN Transaction_amount ELSE -Transaction_amount END) " +
            "FROM Transactions")
    double getNetBalance();

}
