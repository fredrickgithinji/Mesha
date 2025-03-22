package com.dzovah.mesha.Methods.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;

public class AddTransactionDialog extends Dialog {
    private final Context context;
    private final MeshaDatabase database;
    private final BetaAccount betaAccount;
    private OnTransactionAddedListener listener;

    public interface OnTransactionAddedListener {
        void onTransactionAdded();
    }

    public AddTransactionDialog(Context context, MeshaDatabase database, BetaAccount betaAccount) {
        super(context);
        this.context = context;
        this.database = database;
        this.betaAccount = betaAccount;
        setupDialog();
    }

    public void setOnTransactionAddedListener(OnTransactionAddedListener listener) {
        this.listener = listener;
    }

    private void setupDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null);
        setContentView(dialogView);

        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etDescription = dialogView.findViewById(R.id.etTransactionDescription);
        RadioGroup rgTransactionType = dialogView.findViewById(R.id.rgTransactionType);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnAdd.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            String description = etDescription.getText().toString();
            String type = rgTransactionType.getCheckedRadioButtonId() == R.id.rbCredit ? "CREDIT" : "DEBIT";

            if (amountStr.isEmpty() || description.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                
                // Add validation for negative balance
                if (type.equals("DEBIT")) {
                    double currentBalance = betaAccount.getBetaAccountBalance();
                    if (amount > currentBalance) {
                        Toast.makeText(context, 
                            "Insufficient balance. Available: " + CurrencyFormatter.format(currentBalance), 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                createTransaction(amount, type, description);
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void createTransaction(double amount, String type, String description) {
        Transaction newTransaction = new Transaction(
            betaAccount.getAlphaAccountId(),
            betaAccount.getBetaAccountId(),
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
                double newBetaBalance = betaAccount.getBetaAccountBalance();
                newBetaBalance += type.equals("CREDIT") ? amount : -amount;
                betaAccount.setBetaAccountBalance(newBetaBalance);
                database.betaAccountDao().update(betaAccount);

                // Update alpha account balance
                AlphaAccount alphaAccount = database.alphaAccountDao()
                    .getAlphaAccountById(betaAccount.getAlphaAccountId());
                if (alphaAccount != null) {
                    double newAlphaBalance = alphaAccount.getAlphaAccountBalance();
                    newAlphaBalance += type.equals("CREDIT") ? amount : -amount;
                    alphaAccount.setAlphaAccountBalance(newAlphaBalance);
                    database.alphaAccountDao().update(alphaAccount);
                }

                if (listener != null) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        listener.onTransactionAdded();
                        Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "Error adding transaction", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
} 