package com.dzovah.mesha.Activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Activities.Adapters.AnalysisTransactionAdapter;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for analyzing and displaying financial transactions.
 * <p>
 * This activity provides users with a comprehensive view of their financial activity:
 * <ul>
 *     <li>Displays a chronological list of all transactions across accounts</li>
 *     <li>Shows the net balance of all financial activity</li>
 *     <li>Provides detailed views of individual transactions</li>
 *     <li>Associates transactions with their respective Alpha and Beta accounts</li>
 * </ul>
 * Users can tap on individual transactions to view complete details including
 * amounts, descriptions, dates, and associated accounts.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Transaction
 * @see AnalysisTransactionAdapter
 */
public class AnalysisActivity extends AppCompatActivity {
    /** Database instance for accessing app data */
    private MeshaDatabase database;
    
    /** Adapter for displaying transactions in the RecyclerView */
    private AnalysisTransactionAdapter adapter;
    
    /** TextView displaying the calculated net balance across all accounts */
    private TextView tvNetBalance;

    /**
     * Initializes the activity, sets up the UI components, and loads transaction data.
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        database = MeshaDatabase.Get_database(this);
        initializeViews();
        loadTransactions();
    }

    /**
     * Initializes and sets up all UI components of the analysis screen.
     * <p>
     * This includes setting up:
     * <ul>
     *     <li>The analysis icon</li>
     *     <li>The net balance display</li>
     *     <li>The RecyclerView for transactions</li>
     *     <li>The transaction adapter with click listener</li>
     * </ul>
     * </p>
     */
    private void initializeViews() {
        // Set default profile icon
        ImageView analysisIcon = findViewById(R.id.AnalysisIcon);
        analysisIcon.setImageResource(R.drawable.icon_mesha);
        tvNetBalance = findViewById(R.id.CurrentBalance);

        RecyclerView rvTransactions = findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new AnalysisTransactionAdapter(this);
        adapter.setOnTransactionClickListener(this::showTransactionDetails);
        rvTransactions.setAdapter(adapter);
    }

    /**
     * Loads all transactions from the database and updates the UI.
     * <p>
     * This method retrieves transactions sorted by entry time and calculates
     * the net balance. The operations are performed on a background thread
     * to avoid blocking the UI.
     * </p>
     */
    private void loadTransactions() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Transaction> transactions = database.transactionDao().getAllTransactionsByEntryTime();
                double netBalance = database.transactionDao().getNetBalance();

                runOnUiThread(() -> {
                    adapter.setTransactions(transactions);
                    tvNetBalance.setText(CurrencyFormatter.format(netBalance));
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error loading transactions", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Displays a dialog with detailed information about a selected transaction.
     * <p>
     * When a transaction is tapped, this method retrieves associated Alpha and Beta
     * account information and presents a comprehensive view of the transaction details,
     * including:
     * <ul>
     *     <li>Amount (formatted)</li>
     *     <li>Description</li>
     *     <li>Date and time</li>
     *     <li>Associated Alpha and Beta accounts</li>
     *     <li>Transaction type (credit or debit)</li>
     * </ul>
     * </p>
     *
     * @param transaction The transaction to display details for
     */
    private void showTransactionDetails(Transaction transaction) {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get Alpha and Beta account details
                BetaAccount betaAccount = database.betaAccountDao()
                    .getBetaAccountById(transaction.getBetaAccountId());
                AlphaAccount alphaAccount = database.alphaAccountDao()
                    .getAlphaAccountById(transaction.getAlphaAccountId());

                if (betaAccount != null && alphaAccount != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    String date = dateFormat.format(new Date(transaction.getEntryTime()));

                    String details = String.format(
                        "Amount: %s\n\n" +
                        "Description: %s\n\n" +
                        "Date: %s\n\n" +
                        "Alpha Account: %s\n" +
                        "Beta Account: %s\n\n" +
                        "Type: %s",
                        CurrencyFormatter.format(Math.abs(transaction.getTransactionAmount())),
                        transaction.getTransactionDescription(),
                        date,
                        alphaAccount.getAlphaAccountName(),
                        betaAccount.getBetaAccountName(),
                        transaction.getTransactionAmount() > 0 ? "Credit" : "Debit"
                    );

                    runOnUiThread(() -> {
                        new MaterialAlertDialogBuilder(this)
                            .setTitle("Transaction Details")
                            .setMessage(details)
                            .setPositiveButton("Close", null)
                            .show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error loading transaction details", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}
