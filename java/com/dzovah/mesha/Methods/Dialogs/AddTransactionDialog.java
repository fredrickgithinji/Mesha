package com.dzovah.mesha.Methods.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.Activity;

import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.Activities.Adapters.CategorySpinnerAdapter;

import java.util.List;

public class AddTransactionDialog extends Dialog {
    private final Context context;
    private final MeshaDatabase database;
    private final BetaAccount betaAccount;
    private OnTransactionAddedListener listener;
    private Spinner categorySpinner;
    private List<Category> categories;

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

        // Initialize category spinner
        categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        loadCategories();

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
                
                // For DEBIT transactions, check if it would cause a negative balance
                if (type.equals("DEBIT")) {
                    double currentBalance = betaAccount.getBetaAccountBalance();
                    if (amount > currentBalance) {
                        // Show alternative account dialog
                        showAlternativeAccountDialog(amount, description);
                        return;
                    }
                }
                
                // If no negative balance or CREDIT transaction, proceed normally
                createTransaction(amount, type, description);
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void loadCategories() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            categories = database.categoryDao().getAllCategories();
            ((Activity) context).runOnUiThread(() -> {
                CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(context, categories);
                categorySpinner.setAdapter(adapter);
            });
        });
    }

    private void createTransaction(double amount, String type, String description) {
        Category selectedCategory = (Category) categorySpinner.getSelectedItem();
        
        Transaction newTransaction = new Transaction(
            betaAccount.getAlphaAccountId(),
            betaAccount.getBetaAccountId(),
            selectedCategory != null ? selectedCategory.getCategoryId() : 1, // Default to General category
            description,
            amount,
            type.equals("CREDIT") ? TransactionType.CREDIT : TransactionType.DEBIT,
            System.currentTimeMillis()
        );

        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 1. Insert the transaction
                database.transactionDao().insert(newTransaction);
                
                // 2. Update Beta account balance - ensuring we get the latest data
                BetaAccount currentBeta = database.betaAccountDao().getBetaAccountById(betaAccount.getBetaAccountId());
                double newBetaBalance = currentBeta.getBetaAccountBalance();
                newBetaBalance += type.equals("CREDIT") ? amount : -amount;
                currentBeta.setBetaAccountBalance(newBetaBalance);
                database.betaAccountDao().update(currentBeta);

                // 3. Update Alpha account balance - ensuring we get the latest data
                AlphaAccount alphaAccount = database.alphaAccountDao().getAlphaAccountById(currentBeta.getAlphaAccountId());
                if (alphaAccount != null) {
                    double newAlphaBalance = alphaAccount.getAlphaAccountBalance();
                    newAlphaBalance += type.equals("CREDIT") ? amount : -amount;
                    alphaAccount.setAlphaAccountBalance(newAlphaBalance);
                    database.alphaAccountDao().update(alphaAccount);
                }

                // 4. Notify the UI on the main thread
                ((Activity) context).runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onTransactionAdded();
                    }
                    Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Error adding transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Add this new method to handle insufficient funds
    private void showAlternativeAccountDialog(double amount, String description) {
        AlternativeBetaAccountDialog dialog = new AlternativeBetaAccountDialog(
            context, database, betaAccount, amount, description);
        
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
                        
                        // Now proceed with the original transaction
                        ((Activity) context).runOnUiThread(() -> {
                            // Proceed with original transaction now that funds are available
                            createTransaction(amount, "DEBIT", description);
                            dismiss(); // Now we can dismiss
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Error refreshing account data", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        
        dialog.show();
    }
} 