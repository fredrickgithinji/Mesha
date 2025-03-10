package com.dzovah.mesha.Database.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "Alpha_accounts")
public class AlphaAccount {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Alpha_account_id")
    private int alphaAccountId;

    @ColumnInfo(name = "Alpha_account_name")
    private String alphaAccountName;

    @ColumnInfo(name = "Alpha_account_icon")
    private String alphaAccountIcon;

    @ColumnInfo(name = "Alpha_account_balance")
    private double alphaAccountBalance;

    // Constructor
    public AlphaAccount(String alphaAccountName, String alphaAccountIcon, double alphaAccountBalance) {
        this.alphaAccountName = alphaAccountName;
        this.alphaAccountIcon = alphaAccountIcon != null ? alphaAccountIcon : "Assets/icons"; // Default icon path
        this.alphaAccountBalance = alphaAccountBalance;
    }

    // Getters and Setters
    public int getAlphaAccountId() {
        return alphaAccountId;
    }

    public void setAlphaAccountId(int alphaAccountId) {
        this.alphaAccountId = alphaAccountId;
    }

    public String getAlphaAccountName() {
        return alphaAccountName;
    }

    public void setAlphaAccountName(String alphaAccountName) {
        this.alphaAccountName = alphaAccountName;
    }

    public String getAlphaAccountIcon() {
        return alphaAccountIcon;
    }

    public void setAlphaAccountIcon(String alphaAccountIcon) {
        this.alphaAccountIcon = alphaAccountIcon;
    }

    public double getAlphaAccountBalance() {
        return alphaAccountBalance;
    }

    public void setAlphaAccountBalance(double alphaAccountBalance) {
        this.alphaAccountBalance = alphaAccountBalance;
    }

    public void updateBalanceFromBetaAccounts(List<BetaAccount> betaAccounts) {
        double totalBalance = 0;
        for (BetaAccount beta : betaAccounts) {
            totalBalance += beta.getBetaAccountBalance();
        }
        this.alphaAccountBalance = totalBalance;
    }
}
