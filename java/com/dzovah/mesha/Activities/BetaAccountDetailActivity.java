package com.dzovah.mesha.Activities;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.dzovah.mesha.Activities.Adapters.TransactionAdapter;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.Methods.Dialogs.EditAccountDialog;
import com.dzovah.mesha.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BetaAccountDetailActivity extends AppCompatActivity {
    private MeshaDatabase database;
    private TransactionAdapter transactionAdapter;
    private int betaAccountId;
    private BetaAccount currentBetaAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beta_account_detail);
        
        betaAccountId = getIntent().getIntExtra("beta_account_id", -1);
        if (betaAccountId == -1) {
            Toast.makeText(this, "Error loading account", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = MeshaDatabase.Get_database(getApplicationContext());
        initializeViews();
        loadBetaAccountDetails();
        loadTransactions();
    }

    private void initializeViews() {
        LottieAnimationView glowiAnimation = findViewById(R.id.glowi);
        glowiAnimation.playAnimation();

        RecyclerView rvTransactions = findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(this);
        rvTransactions.setAdapter(transactionAdapter);

        FloatingActionButton fabAddTransaction = findViewById(R.id.fabAddTransaction);
        fabAddTransaction.setOnClickListener(v -> showAddTransactionDialog());
    }

    private void loadBetaAccountDetails() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                currentBetaAccount = database.betaAccountDao().getBetaAccountById(betaAccountId);
                runOnUiThread(() -> {
                    if (currentBetaAccount != null) {
                        TextView tvBetaName = findViewById(R.id.tvBetaAccountName);
                        TextView tvBetaBalance = findViewById(R.id.tvBetaAccountBalance);
                        ImageView ivBetaIcon = findViewById(R.id.betaAccountIcon);
                        LottieAnimationView glowiView = findViewById(R.id.glowi);
    
                        tvBetaName.setText(currentBetaAccount.getBetaAccountName());
                        tvBetaBalance.setText(CurrencyFormatter.format(currentBetaAccount.getBetaAccountBalance()));
                        transactionAdapter.setBetaAccountIcon(currentBetaAccount.getBetaAccountIcon());
    
                        try {
                            String iconPath = currentBetaAccount.getBetaAccountIcon().replace("Assets/", "");
                            InputStream is = getAssets().open(iconPath);
                            ivBetaIcon.setImageBitmap(BitmapFactory.decodeStream(is));
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
    
                        // Set long-click listener on the glowi view instead of just the name
                        glowiView.setOnLongClickListener(v -> {
                            EditAccountDialog dialog = new EditAccountDialog(this, database, currentBetaAccount);
                            dialog.setOnAccountEditedListener(new EditAccountDialog.OnAccountEditedListener() {
                                @Override
                                public void onAccountEdited() {
                                    loadBetaAccountDetails();
                                    loadTransactions();
                                }
                                
                                @Override
                                public void onAccountDeleted() {
                                    finish();
                                }
                            });
                            dialog.show();
                            return true;
                        });
                    } else {
                        Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading account details", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    private void loadTransactions() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Transaction> transactions = database.transactionDao().getAllTransactionsByBetaAccountId(betaAccountId);
                runOnUiThread(() -> transactionAdapter.setTransactions(transactions));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error loading transactions", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showAddTransactionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction, null);
        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etDescription = dialogView.findViewById(R.id.etTransactionDescription);
        RadioGroup rgTransactionType = dialogView.findViewById(R.id.rgTransactionType);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Add", (dialog, which) -> {
                String amountStr = etAmount.getText().toString();
                String description = etDescription.getText().toString();
                String type = rgTransactionType.getCheckedRadioButtonId() == R.id.rbCredit ? "CREDIT" : "DEBIT";

                if (amountStr.isEmpty() || description.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    createTransaction(amount, type, description);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createTransaction(double amount, String type, String description) {
        if (currentBetaAccount == null) {
            Toast.makeText(this, "Error: Beta account not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        Transaction newTransaction = new Transaction(
            currentBetaAccount.getAlphaAccountId(),
            betaAccountId,
            1,  // General category
            description,
            amount,
            type.equals("CREDIT") ? TransactionType.CREDIT : TransactionType.DEBIT,
            System.currentTimeMillis()
        );

        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Insert transaction
                database.transactionDao().insert(newTransaction);
                
                // Update beta account balance
                double newBetaBalance = currentBetaAccount.getBetaAccountBalance();
                newBetaBalance += type.equals("CREDIT") ? amount : -amount;
                currentBetaAccount.setBetaAccountBalance(newBetaBalance);
                database.betaAccountDao().update(currentBetaAccount);

                // Update alpha account balance
                AlphaAccount alphaAccount = database.alphaAccountDao()
                    .getAlphaAccountById(currentBetaAccount.getAlphaAccountId());
                if (alphaAccount != null) {
                    double newAlphaBalance = alphaAccount.getAlphaAccountBalance();
                    newAlphaBalance += type.equals("CREDIT") ? amount : -amount;
                    alphaAccount.setAlphaAccountBalance(newAlphaBalance);
                    database.alphaAccountDao().update(alphaAccount);
                }

                runOnUiThread(() -> {
                    loadBetaAccountDetails();
                    loadTransactions();
                    // Notify the parent AlphaAccountDetailActivity
                    setResult(RESULT_OK);
                    Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error adding transaction", Toast.LENGTH_SHORT).show());
            }
        });
    }
  
} 