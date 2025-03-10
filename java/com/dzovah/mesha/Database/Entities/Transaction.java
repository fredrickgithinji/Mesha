package com.dzovah.mesha.Database.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.TypeConverters;

import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.Database.Utils.TransactionTypeConverter;

@Entity(
    tableName = "Transactions",
    indices = {
        @Index("Alpha_account_id"),
        @Index("Beta_account_id"),
        @Index("Category_id"),
        @Index("Entry_time")
    },
    foreignKeys = {
        @ForeignKey(
            entity = AlphaAccount.class,
            parentColumns = "Alpha_account_id",
            childColumns = "Alpha_account_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = BetaAccount.class,
            parentColumns = "Beta_account_id",
            childColumns = "Beta_account_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Category.class,
            parentColumns = "Category_id",
            childColumns = "Category_id",
            onDelete = ForeignKey.CASCADE
        )
    }
)
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Transaction_id")
    private int transactionId;

    @ColumnInfo(name = "Alpha_account_id")
    private int alphaAccountId;

    @ColumnInfo(name = "Beta_account_id")
    private int betaAccountId;

    @ColumnInfo(name = "Category_id")
    private int categoryId;

    @ColumnInfo(name = "Transaction_description")
    private String transactionDescription;

    @ColumnInfo(name = "Transaction_amount")
    private double transactionAmount;

    @ColumnInfo(name = "Transaction_type")
    @TypeConverters(TransactionTypeConverter.class)
    private TransactionType transactionType;

    @ColumnInfo(name = "Entry_time")
    private long entryTime;

    // Constructor
    public Transaction(int alphaAccountId, int betaAccountId, int categoryId,
                      String transactionDescription, double transactionAmount,
                      TransactionType transactionType, long entryTime) {
        this.alphaAccountId = alphaAccountId;
        this.betaAccountId = betaAccountId;
        this.categoryId = categoryId;
        this.transactionDescription = transactionDescription;
        this.transactionAmount = transactionAmount;
        this.transactionType = transactionType;
        this.entryTime = entryTime;
    }

    // Getters and Setters
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getAlphaAccountId() {
        return alphaAccountId;
    }

    public void setAlphaAccountId(int alphaAccountId) {
        this.alphaAccountId = alphaAccountId;
    }

    public int getBetaAccountId() {
        return betaAccountId;
    }

    public void setBetaAccountId(int betaAccountId) {
        this.betaAccountId = betaAccountId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getTransactionDescription() {
        return transactionDescription;
    }

    public void setTransactionDescription(String transactionDescription) {
        this.transactionDescription = transactionDescription;
    }

    public double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public long getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(long entryTime) {
        this.entryTime = entryTime;
    }
}