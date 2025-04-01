package com.dzovah.mesha.Methods.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.Activities.Adapters.CategorySpinnerAdapter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog for editing or deleting existing transactions in the Mesha financial system.
 * <p>
 * This dialog allows users to:
 * <ul>
 *   <li>Edit transaction amounts</li>
 *   <li>Update transaction descriptions</li>
 *   <li>Change transaction categories</li>
 *   <li>Delete transactions with confirmation</li>
 * </ul>
 * </p>
 * <p>
 * The dialog handles the complex logic of updating account balances when transaction
 * details are modified or when transactions are deleted. It also includes validation
 * to prevent transactions that would create negative balances, with the option to
 * transfer funds from alternative accounts when needed.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Transaction
 * @see BetaAccount
 * @see AlphaAccount
 * @see AlternativeBetaAccountDialog
 */
public class EditTransactionDialog extends Dialog {
    /**
     * The application context used for UI operations.
     */
    private final Context context;
    
    /**
     * The database instance for data access.
     */
    private final MeshaDatabase database;
    
    /**
     * The transaction being edited.
     */
    private final Transaction transaction;
    
    /**
     * The BetaAccount associated with the transaction.
     */
    private final BetaAccount betaAccount;
    
    /**
     * Listener to notify when a transaction is edited or deleted.
     */
    private OnTransactionEditedListener listener;
    
    /**
     * Spinner for selecting transaction categories.
     */
    private Spinner categorySpinner;
    
    /**
     * List of available categories.
     */
    private List<Category> categories;
    
    /**
     * Progress indicator for loading and processing.
     */
    private ProgressBar progressBar;
    private TextView statusTextView;
    
    /**
     * Main thread handler for UI updates.
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Dedicated executor for database operations.
     */
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    
    /**
     * Flag to track if the dialog is active to prevent memory leaks.
     */
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    /**
     * Interface for notifying when a transaction has been edited or deleted.
     * <p>
     * This callback allows the parent activity or fragment to be notified when
     * transaction operations are completed, enabling it to refresh its UI or
     * perform additional actions.
     * </p>
     */
    public interface OnTransactionEditedListener {
        /**
         * Called when a transaction is successfully edited.
         */
        void onTransactionEdited();
        
        /**
         * Called when a transaction is successfully deleted.
         */
        void onTransactionDeleted();
    }

    /**
     * Creates a new EditTransactionDialog instance.
     * <p>
     * Initializes the dialog with references to the necessary context, database,
     * transaction object to be edited, and the associated BetaAccount.
     * </p>
     *
     * @param context The context in which the dialog will be shown
     * @param database The database instance for accessing data
     * @param transaction The transaction object to edit
     * @param betaAccount The BetaAccount associated with the transaction
     */
    public EditTransactionDialog(Context context, MeshaDatabase database, Transaction transaction, BetaAccount betaAccount) {
        super(context);
        this.context = context;
        this.database = database;
        this.transaction = transaction;
        this.betaAccount = betaAccount;
        
        // Set dismiss listener to handle cleanup
        setOnDismissListener(dialog -> cleanupResources());
        
        setupDialog();
    }
    
    /**
     * Cleans up resources to prevent memory leaks when the dialog is dismissed.
     */
    private void cleanupResources() {
        // Mark dialog as inactive to prevent further callbacks
        isActive.set(false);
        
        // Remove all callbacks from the handler
        mainHandler.removeCallbacksAndMessages(null);
        
        // Shutdown the executor service
        dbExecutor.shutdown();
    }

    /**
     * Sets the listener to be notified when a transaction is edited or deleted.
     * <p>
     * The listener will be called after a transaction is successfully
     * updated or removed from the database.
     * </p>
     *
     * @param listener The listener to be notified
     */
    public void setOnTransactionEditedListener(OnTransactionEditedListener listener) {
        this.listener = listener;
    }

    /**
     * Sets up the dialog's UI components and event handlers.
     * <p>
     * This method initializes the dialog's layout, finds all UI components,
     * pre-populates fields with the current transaction data, sets up event
     * listeners, and configures the category spinner.
     * </p>
     */
    private void setupDialog() {
        if (!isActive.get()) return;
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_transaction, null);
        setContentView(dialogView);

        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etDescription = dialogView.findViewById(R.id.etTransactionDescription);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdate);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Initialize progress indicators
        progressBar = dialogView.findViewById(R.id.progressBar);
        statusTextView = dialogView.findViewById(R.id.statusTextView);

        // Set current values
        etAmount.setText(String.valueOf(Math.abs(transaction.getTransactionAmount())));
        etDescription.setText(transaction.getTransactionDescription());

        // Initialize category spinner
        categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        loadCategories();

        btnUpdate.setOnClickListener(v -> {
            if (!isActive.get()) return;
            
            String amountStr = etAmount.getText().toString();
            String description = etDescription.getText().toString();

            if (amountStr.isEmpty() || description.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                
                // Disable buttons to prevent multiple clicks
                btnUpdate.setEnabled(false);
                btnDelete.setEnabled(false);
                
                // Check if it's a debit transaction and would cause negative balance
                if (transaction.getTransactionType() == TransactionType.DEBIT) {
                    double currentBalance = betaAccount.getBetaAccountBalance();
                    double oldAmount = Math.abs(transaction.getTransactionAmount());
                    
                    // Calculate the additional withdrawal
                    double additionalWithdrawal = amount - oldAmount;
                    
                    // If additional withdrawal would cause negative balance
                    if (additionalWithdrawal > 0 && additionalWithdrawal > currentBalance) {
                        showAlternativeAccountDialog(additionalWithdrawal, description);
                        
                        // Re-enable buttons since we're showing another dialog
                        btnUpdate.setEnabled(true);
                        btnDelete.setEnabled(true);
                        return;
                    }
                }
                
                updateTransaction(amount, description);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
                btnUpdate.setEnabled(true);
                btnDelete.setEnabled(true);
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (!isActive.get()) return;
            
            new MaterialAlertDialogBuilder(context)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!isActive.get()) return;
                    
                    // Disable buttons to prevent multiple clicks
                    btnUpdate.setEnabled(false);
                    btnDelete.setEnabled(false);
                    
                    deleteTransaction();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnCancel.setOnClickListener(v -> {
            if (!isActive.get()) return;
            dismiss();
        });
    }

    /**
     * Loads all available transaction categories from the database.
     * <p>
     * This method asynchronously loads the categories and updates the UI
     * with a populated category spinner, pre-selecting the category that
     * matches the current transaction's category.
     * </p>
     */
    private void loadCategories() {
        if (!isActive.get()) return;
        
        showLoading("Loading categories...");
        
        executeIfActive(() -> {
            try {
                categories = database.categoryDao().getAllCategories();
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(context, categories);
                    categorySpinner.setAdapter(adapter);
                    
                    // Set selected category
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getCategoryId() == transaction.getCategoryId()) {
                            categorySpinner.setSelection(i);
                            break;
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    Toast.makeText(context, "Error loading categories", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Updates an existing transaction with new details.
     * <p>
     * This method updates the transaction with the new amount, description, and
     * selected category. It calculates the difference between the old and new
     * amounts to properly adjust the balances of both the BetaAccount and its
     * parent AlphaAccount. The update is performed asynchronously.
     * </p>
     *
     * @param newAmount The new transaction amount
     * @param newDescription The new transaction description
     */
    private void updateTransaction(double newAmount, String newDescription) {
        if (!isActive.get()) return;
        
        showLoading("Updating transaction...");
        
        Category selectedCategory = (Category) categorySpinner.getSelectedItem();
        
        transaction.setCategoryId(selectedCategory != null ? selectedCategory.getCategoryId() : 1);
        transaction.setTransactionDescription(newDescription);

        executeIfActive(() -> {
            try {
                // Calculate the difference in amount
                double oldAmount = transaction.getTransactionAmount();
                double amountDifference = newAmount - Math.abs(oldAmount);
                
                // Maintain the sign (credit/debit) of the original transaction
                if (oldAmount < 0) {
                    amountDifference = -amountDifference;
                }

                updateLoadingStatus("Saving changes...");
                
                // Update transaction
                transaction.setTransactionAmount(oldAmount < 0 ? -newAmount : newAmount);
                transaction.setTransactionDescription(newDescription);
                database.transactionDao().update(transaction);

                updateLoadingStatus("Updating account balances...");
                
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

                postToMainThreadIfActive(() -> {
                    hideLoading();
                    if (listener != null) {
                        listener.onTransactionEdited();
                    }
                    Toast.makeText(context, "Transaction updated successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            } catch (Exception e) {
                e.printStackTrace();
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    Toast.makeText(context, "Error updating transaction", Toast.LENGTH_SHORT).show();
                    
                    // Re-enable buttons
                    Button btnUpdate = findViewById(R.id.btnUpdate);
                    Button btnDelete = findViewById(R.id.btnDelete);
                    if (btnUpdate != null) btnUpdate.setEnabled(true);
                    if (btnDelete != null) btnDelete.setEnabled(true);
                });
            }
        });
    }

    /**
     * Deletes an existing transaction from the database.
     * <p>
     * This method removes the transaction from the database and adjusts the
     * balances of both the BetaAccount and its parent AlphaAccount to reflect
     * the removal of the transaction. The operation is performed asynchronously.
     * </p>
     */
    private void deleteTransaction() {
        if (!isActive.get()) return;
        
        showLoading("Deleting transaction...");
        
        executeIfActive(() -> {
            try {
                updateLoadingStatus("Removing transaction...");
                
                // Delete transaction
                database.transactionDao().delete(transaction);

                updateLoadingStatus("Updating account balances...");
                
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

                postToMainThreadIfActive(() -> {
                    hideLoading();
                    if (listener != null) {
                        listener.onTransactionDeleted();
                    }
                    Toast.makeText(context, "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            } catch (Exception e) {
                e.printStackTrace();
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    Toast.makeText(context, "Error deleting transaction", Toast.LENGTH_SHORT).show();
                    
                    // Re-enable buttons
                    Button btnUpdate = findViewById(R.id.btnUpdate);
                    Button btnDelete = findViewById(R.id.btnDelete);
                    if (btnUpdate != null) btnUpdate.setEnabled(true);
                    if (btnDelete != null) btnDelete.setEnabled(true);
                });
            }
        });
    }

    /**
     * Shows a dialog to select an alternative account when there are insufficient funds.
     * <p>
     * When editing a debit transaction to a larger amount than what's available in the
     * current BetaAccount, this method shows a dialog that allows the user to select
     * an alternative account to transfer the additional funds from. After the funds
     * transfer is completed, the transaction update continues with the refreshed account data.
     * </p>
     *
     * @param additionalAmount The additional amount needed beyond what's available in the account
     * @param description The description for the transaction being edited
     */
    private void showAlternativeAccountDialog(double additionalAmount, String description) {
        if (!isActive.get()) return;
        
        AlternativeBetaAccountDialog dialog = new AlternativeBetaAccountDialog(
            context, database, betaAccount, additionalAmount, description);
        
        dialog.setOnTransactionCompletedListener(() -> {
            if (!isActive.get()) return;
            
            showLoading("Refreshing account data...");
            
            // After funds transfer completed, refresh beta account data and continue
            executeIfActive(() -> {
                try {
                    // Refresh beta account data
                    BetaAccount refreshedAccount = database.betaAccountDao()
                        .getBetaAccountById(betaAccount.getBetaAccountId());
                    
                    if (refreshedAccount != null) {
                        // Update our local copy with refreshed data
                        betaAccount.setBetaAccountBalance(refreshedAccount.getBetaAccountBalance());
                        
                        // Calculate total amount from form
                        EditText etAmount = findViewById(R.id.etTransactionAmount);
                        if (etAmount != null) {
                            double totalAmount = Double.parseDouble(etAmount.getText().toString());
                            
                            // Now proceed with the original transaction update
                            postToMainThreadIfActive(() -> {
                                hideLoading();
                                // Proceed with updating transaction now that funds are available
                                updateTransaction(totalAmount, description);
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    postToMainThreadIfActive(() -> {
                        hideLoading();
                        Toast.makeText(context, "Error refreshing account data", Toast.LENGTH_SHORT).show();
                        
                        // Re-enable buttons
                        Button btnUpdate = findViewById(R.id.btnUpdate);
                        if (btnUpdate != null) btnUpdate.setEnabled(true);
                    });
                }
            });
        });
        
        dialog.show();
    }
    
    /**
     * Helper method to execute a task on the background thread only if the dialog is active.
     * Helps prevent memory leaks by not executing tasks after dialog dismissal.
     * 
     * @param task The task to execute
     */
    private void executeIfActive(Runnable task) {
        if (isActive.get()) {
            dbExecutor.execute(() -> {
                if (isActive.get()) {
                    task.run();
                }
            });
        }
    }
    
    /**
     * Helper method to post a task to the main thread only if the dialog is active.
     * Helps prevent memory leaks by not posting tasks after dialog dismissal.
     * 
     * @param task The task to post to the main thread
     */
    private void postToMainThreadIfActive(Runnable task) {
        if (isActive.get()) {
            mainHandler.post(() -> {
                if (isActive.get()) {
                    task.run();
                }
            });
        }
    }
    
    /**
     * Shows a loading indicator with a status message
     * @param message The status message to display
     */
    private void showLoading(@NonNull String message) {
        postToMainThreadIfActive(() -> {
            if (progressBar != null && statusTextView != null) {
                progressBar.setVisibility(View.VISIBLE);
                statusTextView.setVisibility(View.VISIBLE);
                statusTextView.setText(message);
            }
        });
    }
    
    /**
     * Updates the loading status message
     * @param message The new status message
     */
    private void updateLoadingStatus(@NonNull String message) {
        postToMainThreadIfActive(() -> {
            if (statusTextView != null) {
                statusTextView.setText(message);
            }
        });
    }
    
    /**
     * Hides the loading indicator
     */
    private void hideLoading() {
        postToMainThreadIfActive(() -> {
            if (progressBar != null && statusTextView != null) {
                progressBar.setVisibility(View.GONE);
                statusTextView.setVisibility(View.GONE);
            }
        });
    }
    
    @Override
    public void dismiss() {
        // Clean up resources before dismissing
        cleanupResources();
        super.dismiss();
    }
}