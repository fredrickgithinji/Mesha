package com.dzovah.mesha.Methods.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Activities.Adapters.SelectBetaAccountAdapter;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.R;

import java.util.List;

public class AlternativeBetaAccountDialog extends Dialog {
    private final Context context;
    private final MeshaDatabase database;
    private final BetaAccount sourceBetaAccount;
    private final double transactionAmount;
    private final String transactionDescription;
    private BetaAccount selectedTargetAccount = null;
    private OnTransactionCompletedListener listener;

    public interface OnTransactionCompletedListener {
        void onTransactionCompleted();
    }

    public AlternativeBetaAccountDialog(Context context, MeshaDatabase database, 
                                        BetaAccount sourceBetaAccount,
                                        double transactionAmount, 
                                        String transactionDescription) {
        super(context);
        this.context = context;
        this.database = database;
        this.sourceBetaAccount = sourceBetaAccount;
        this.transactionAmount = transactionAmount;
        this.transactionDescription = transactionDescription;
        setupDialog();
    }

    public void setOnTransactionCompletedListener(OnTransactionCompletedListener listener) {
        this.listener = listener;
    }

    private void setupDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_alternative_beta_account, null);
        setContentView(dialogView);

        TextView tvMessage = dialogView.findViewById(R.id.tvInsufficientFundsMessage);
        RecyclerView rvAccounts = dialogView.findViewById(R.id.rvAlternativeAccounts);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnProceed = dialogView.findViewById(R.id.btnProceed);

        // Set message
        String message = "Insufficient funds in " + sourceBetaAccount.getBetaAccountName() + 
                         ". Available: " + CurrencyFormatter.format(sourceBetaAccount.getBetaAccountBalance()) + 
                         ", Required: " + CurrencyFormatter.format(transactionAmount);
        tvMessage.setText(message);

        // Set up RecyclerView
        rvAccounts.setLayoutManager(new LinearLayoutManager(context));
        SelectBetaAccountAdapter adapter = new SelectBetaAccountAdapter(context);
        rvAccounts.setAdapter(adapter);

        // Load alternative accounts
        loadAlternativeAccounts(adapter);

        // Set account selection listener
        adapter.setOnAccountSelectedListener(account -> {
            selectedTargetAccount = account;
            btnProceed.setEnabled(selectedTargetAccount != null);
        });

        // Set button listeners
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnProceed.setEnabled(false); // Disable until an account is selected
        btnProceed.setOnClickListener(v -> {
            if (selectedTargetAccount != null) {
                processTransaction();
            } else {
                Toast.makeText(context, "Please select an account", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAlternativeAccounts(SelectBetaAccountAdapter adapter) {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get all beta accounts from the same alpha account that have sufficient balance
                List<BetaAccount> accounts = database.betaAccountDao().getAllBetaAccounts();
                
                // Filter out the source account and accounts with insufficient balance
                accounts.removeIf(account -> 
                    account.getBetaAccountId() == sourceBetaAccount.getBetaAccountId() || 
                    account.getBetaAccountBalance() < transactionAmount
                );
                
                ((android.app.Activity) context).runOnUiThread(() -> {
                    adapter.setAccounts(accounts);
                    if (accounts.isEmpty()) {
                        TextView tvNoAccounts = findViewById(R.id.tvNoAlternativeAccounts);
                        if (tvNoAccounts != null) {
                            tvNoAccounts.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "Error loading alternative accounts", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void processTransaction() {
        // Get the Alpha account names for better descriptions
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get Alpha account names
                AlphaAccount sourceAlpha = database.alphaAccountDao().getAlphaAccountById(
                    sourceBetaAccount.getAlphaAccountId());
                AlphaAccount targetAlpha = database.alphaAccountDao().getAlphaAccountById(
                    selectedTargetAccount.getAlphaAccountId());
                
                String sourceAlphaName = sourceAlpha != null ? sourceAlpha.getAlphaAccountName() : "Unknown";
                String targetAlphaName = targetAlpha != null ? targetAlpha.getAlphaAccountName() : "Unknown";
                
                // Create a debit transaction on the target account
                Transaction debitTransaction = new Transaction(
                    selectedTargetAccount.getAlphaAccountId(),
                    selectedTargetAccount.getBetaAccountId(),
                    1,  // General category
                    "Transfer to " + sourceBetaAccount.getBetaAccountName() + " in " + 
                    sourceAlphaName + " Alpha Account: " + transactionDescription,
                    transactionAmount,
                    TransactionType.DEBIT,
                    System.currentTimeMillis()
                );

                // Create a credit transaction on the source account
                Transaction creditTransaction = new Transaction(
                    sourceBetaAccount.getAlphaAccountId(),
                    sourceBetaAccount.getBetaAccountId(),
                    1,  // General category
                    "Transfer from " + selectedTargetAccount.getBetaAccountName() + " in " +
                    targetAlphaName + " Alpha Account: " + transactionDescription,
                    transactionAmount,
                    TransactionType.CREDIT,
                    System.currentTimeMillis()
                );

                // Insert both transactions
                database.transactionDao().insert(debitTransaction);
                database.transactionDao().insert(creditTransaction);
                
                // Update beta account balances
                // Target account (debit)
                double newTargetBalance = selectedTargetAccount.getBetaAccountBalance() - transactionAmount;
                selectedTargetAccount.setBetaAccountBalance(newTargetBalance);
                database.betaAccountDao().update(selectedTargetAccount);
                
                // Source account (credit)
                double newSourceBalance = sourceBetaAccount.getBetaAccountBalance() + transactionAmount;
                sourceBetaAccount.setBetaAccountBalance(newSourceBalance);
                database.betaAccountDao().update(sourceBetaAccount);

                // Update alpha account balances if needed
                if (selectedTargetAccount.getAlphaAccountId() != sourceBetaAccount.getAlphaAccountId()) {
                    if (sourceAlpha != null && targetAlpha != null) {
                        // Update source alpha (credit)
                        double newSourceAlphaBalance = sourceAlpha.getAlphaAccountBalance() + transactionAmount;
                        sourceAlpha.setAlphaAccountBalance(newSourceAlphaBalance);
                        database.alphaAccountDao().update(sourceAlpha);
                        
                        // Update target alpha (debit)
                        double newTargetAlphaBalance = targetAlpha.getAlphaAccountBalance() - transactionAmount;
                        targetAlpha.setAlphaAccountBalance(newTargetAlphaBalance);
                        database.alphaAccountDao().update(targetAlpha);
                    }
                }

                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Your account has been topped up successfully", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                        listener.onTransactionCompleted();
                    }
                    dismiss();
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Error processing transaction: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}