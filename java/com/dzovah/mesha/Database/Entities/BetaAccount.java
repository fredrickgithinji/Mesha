package com.dzovah.mesha.Database.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "Beta_accounts",
    foreignKeys = @ForeignKey(
        entity = AlphaAccount.class,
        parentColumns = "Alpha_account_id",
        childColumns = "Alpha_account_id",
        onDelete = ForeignKey.CASCADE // Define the action when the associated AlphaAccount is deleted
    )
)
public class BetaAccount {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Beta_account_id")
    private int betaAccountId;

    @ColumnInfo(name = "Alpha_account_id")
    private int alphaAccountId;  // Foreign Key to AlphaAccount

    @ColumnInfo(name = "Beta_account_name")
    private String betaAccountName;

    @ColumnInfo(name = "Beta_account_icon")
    private String betaAccountIcon;

    @ColumnInfo(name = "Beta_account_balance")
    private double betaAccountBalance;

    // Constructor
    public BetaAccount(int alphaAccountId, String betaAccountName, String betaAccountIcon, double betaAccountBalance) {
        this.alphaAccountId = alphaAccountId;
        this.betaAccountName = betaAccountName;
        this.betaAccountIcon = betaAccountIcon != null ? betaAccountIcon : "Assets/icons"; // Default image directory
        this.betaAccountBalance = betaAccountBalance > 0 ? betaAccountBalance : 0; // Default balance is 0
    }

    // Getters and Setters
    public int getBetaAccountId() {
        return betaAccountId;
    }

    public void setBetaAccountId(int betaAccountId) {
        this.betaAccountId = betaAccountId;
    }

    public int getAlphaAccountId() {
        return alphaAccountId;
    }

    public void setAlphaAccountId(int alphaAccountId) {
        this.alphaAccountId = alphaAccountId;
    }

    public String getBetaAccountName() {
        return betaAccountName;
    }

    public void setBetaAccountName(String betaAccountName) {
        this.betaAccountName = betaAccountName;
    }

    public String getBetaAccountIcon() {
        return betaAccountIcon;
    }

    public void setBetaAccountIcon(String betaAccountIcon) {
        this.betaAccountIcon = betaAccountIcon;
    }

    public double getBetaAccountBalance() {
        return betaAccountBalance;
    }

    public void setBetaAccountBalance(double betaAccountBalance) {
        this.betaAccountBalance = betaAccountBalance;
    }
}
