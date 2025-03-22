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

public class AnalysisActivity extends AppCompatActivity {
    private MeshaDatabase database;
    private AnalysisTransactionAdapter adapter;
    private TextView tvNetBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        database = MeshaDatabase.Get_database(this);
        initializeViews();
        loadTransactions();
    }

    private void initializeViews() {
        // Set default profile icon
        ImageView analysisIcon = findViewById(R.id.AnalysisIcon);
        analysisIcon.setImageResource(R.drawable.default_profile);
        tvNetBalance = findViewById(R.id.CurrentBalance);

        RecyclerView rvTransactions = findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new AnalysisTransactionAdapter(this);
        adapter.setOnTransactionClickListener(this::showTransactionDetails);
        rvTransactions.setAdapter(adapter);
    }

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

