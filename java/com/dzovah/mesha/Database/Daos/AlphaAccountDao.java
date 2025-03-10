package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.AlphaAccount;

import java.util.List;

@Dao
public interface AlphaAccountDao {

    // Insert an account into the database
    @Insert
    void insert(AlphaAccount alphaAccount);

    // Update an account
    @Update
    void update(AlphaAccount alphaAccount);

    // Delete an account
    @Delete
    void delete(AlphaAccount alphaAccount);

    // Query all accounts
    @Query("SELECT * FROM Alpha_accounts ORDER BY Alpha_account_name")
    List<AlphaAccount> getAllAlphaAccounts();

    // Query an account by ID
    @Query("SELECT * FROM Alpha_accounts WHERE Alpha_account_id = :alphaAccountId")
    AlphaAccount getAlphaAccountById(int alphaAccountId);

    // Get total balance of all alpha accounts
    @Query("SELECT SUM(Alpha_account_balance) FROM Alpha_accounts")
    double getTotalAlphaAccountsBalance();

    // Search accounts by name
    @Query("SELECT * FROM Alpha_accounts WHERE Alpha_account_name LIKE '%' || :searchQuery || '%'")
    List<AlphaAccount> searchAlphaAccounts(String searchQuery);
}
