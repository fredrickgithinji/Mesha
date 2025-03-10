package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.BetaAccount;

import java.util.List;

@Dao
public interface BetaAccountDao {

    // Insert a new Beta Account into the database
    @Insert
    void insert(BetaAccount betaAccount);

    // Update a Beta Account
    @Update
    void update(BetaAccount betaAccount);

    // Delete a Beta Account
    @Delete
    void delete(BetaAccount betaAccount);

    // Query all Beta Accounts
    @Query("SELECT * FROM Beta_accounts")
    List<BetaAccount> getAllBetaAccounts();

    // Query a Beta Account by its ID
    @Query("SELECT * FROM Beta_accounts WHERE Beta_account_id = :betaAccountId")
    BetaAccount getBetaAccountById(int betaAccountId);

    // Query Beta Accounts by their associated Alpha Account ID
    @Query("SELECT * FROM Beta_accounts WHERE Alpha_account_id = :alphaAccountId")
    List<BetaAccount> getBetaAccountsByAlphaAccountId(int alphaAccountId);

    @Query("UPDATE Alpha_accounts SET Alpha_account_balance = " +
            "(SELECT COALESCE(SUM(Beta_account_balance), 0) FROM Beta_accounts " +
            "WHERE Alpha_account_id = :alphaId) " +
            "WHERE Alpha_account_id = :alphaId")
    void updateAlphaAccountBalance(int alphaId);
}
