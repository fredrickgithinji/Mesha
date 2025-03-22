package com.dzovah.mesha.Methods.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EditTransactionDialog extends Dialog {
    private final Context context;
    private final MeshaDatabase database;
    private final Transaction transaction;
    private final BetaAccount betaAccount;
    private OnTransactionEditedListener listener;

    public interface OnTransactionEditedListener {
        void onTransactionEdited();
        void onTransactionDeleted();
    }

    public EditTransactionDialog(Context context, MeshaDatabase database, Transaction transaction, BetaAccount betaAccount) {
        super(context);
        this.context = context;
        this.database = database;
        this.transaction = transaction;
        this.betaAccount = betaAccount;
        setupDialog();
    }

    public void setOnTransactionEditedListener(OnTransactionEditedListener listener) {
        this.listener = listener;
    }

    private void setupDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_transaction, null);
        setContentView(dialogView);

        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etDescription = dialogView.findViewById(R.id.etTransactionDescription);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdate);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set current values
        etAmount.setText(String.valueOf(Math.abs(transaction.getTransactionAmount())));
        etDescription.setText(transaction.getTransactionDescription());

        btnUpdate.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            String description = etDescription.getText().toString();

            if (amountStr.isEmpty() || description.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                
                // Check if it's a debit transaction and would cause negative balance
                if (transaction.getTransactionType() == TransactionType.DEBIT) {
                    double currentBalance = betaAccount.getBetaAccountBalance();
                    double oldAmount = Math.abs(transaction.getTransactionAmount());
                    
                    // Calculate the additional withdrawal
                    double additionalWithdrawal = amount - oldAmount;
                    
                    // If additional withdrawal would cause negative balance
                    if (additionalWithdrawal > 0 && additionalWithdrawal > currentBalance) {
                        showAlternativeAccountDialog(additionalWithdrawal, description);
                        return;
                    }
                }
                
                updateTransaction(amount, description);
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTransaction();
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void updateTransaction(double newAmount, String newDescription) {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Calculate the difference in amount
                double oldAmount = transaction.getTransactionAmount();
                double amountDifference = newAmount - Math.abs(oldAmount);
                
                // Maintain the sign (credit/debit) of the original transaction
                if (oldAmount < 0) {
                    amountDifference = -amountDifference;
                }

                // Update transaction
                transaction.setTransactionAmount(oldAmount < 0 ? -newAmount : newAmount);
                transaction.setTransactionDescription(newDescription);
                database.transactionDao().update(transaction);

                // Update beta account balance
                double newBetaBalance = betaAccount.getBetaAccountBalance() + amountDifference;
                betaAccount.setBetaAccountBalance(newBetaBalance);
                database.betaAccountDao().update(betaAccount);

                // Update alpha account balance
                AlphaAccount alphaAccount = database.alphaAccountDao()
                    .getAlphaAccountById(betaAccount.getAlphaAccountId());
                if (alphaAccount != null) {
                    double newAlphaBalance = alphaAccount.getAlphaAccountBalance() + amountDifference;
                    alphaAccount.setAlphaAccountBalance(newAlphaBalance);
                    database.alphaAccountDao().update(alphaAccount);
                }

                if (listener != null) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        listener.onTransactionEdited();
                        Toast.makeText(context, "Transaction updated successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "Error updating transaction", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void deleteTransaction() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Delete transaction
                database.transactionDao().delete(transaction);

                // Update beta account balance
                double newBetaBalance = betaAccount.getBetaAccountBalance() - transaction.getTransactionAmount();
                betaAccount.setBetaAccountBalance(newBetaBalance);
                database.betaAccountDao().update(betaAccount);

                // Update alpha account balance
                AlphaAccount alphaAccount = database.alphaAccountDao()
                    .getAlphaAccountById(betaAccount.getAlphaAccountId());
                if (alphaAccount != null) {
                    double newAlphaBalance = alphaAccount.getAlphaAccountBalance() - transaction.getTransactionAmount();
                    alphaAccount.setAlphaAccountBalance(newAlphaBalance);
                    database.alphaAccountDao().update(alphaAccount);
                }

                if (listener != null) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        listener.onTransactionDeleted();
                        Toast.makeText(context, "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "Error deleting transaction", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showAlternativeAccountDialog(double additionalAmount, String description) {
        AlternativeBetaAccountDialog dialog = new AlternativeBetaAccountDialog(
            context, database, betaAccount, additionalAmount, description);
        
        dialog.setOnTransactionCompletedListener(() -> {
            // After funds transfer completed, refresh beta account data and continue
            MeshaDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    // Refresh beta account data
                    BetaAccount refreshedAccount = database.betaAccountDao()
                        .getBetaAccountById(betaAccount.getBetaAccountId());
                    
                    if (refreshedAccount != null) {
                        // Update our local copy with refreshed data
                        betaAccount.setBetaAccountBalance(refreshedAccount.getBetaAccountBalance());
                        
                        // Calculate total amount from form
                        double totalAmount = Double.parseDouble(
                            ((EditText)findViewById(R.id.etTransactionAmount)).getText().toString());
                        
                        // Now proceed with the original transaction update
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            // Proceed with updating transaction now that funds are available
                            updateTransaction(totalAmount, description);
                            dismiss(); // Now we can dismiss
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Error refreshing account data", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        
        dialog.show();
    }
} 