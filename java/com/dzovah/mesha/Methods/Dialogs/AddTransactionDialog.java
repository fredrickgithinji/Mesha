package com.dzovah.mesha.Methods.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
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
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.Activities.Adapters.CategorySpinnerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog for adding new financial transactions to a BetaAccount.
 * <p>
 * This dialog allows users to create new transactions by specifying an amount,
 * description, transaction type (credit or debit), and category. The dialog handles
 * validation of inputs and manages the process of creating and saving the transaction
 * to the database.
 * </p>
 * <p>
 * Additional functionality includes:
 * <ul>
 *   <li>Validating transaction amounts to prevent negative balances</li>
 *   <li>Offering an alternative account selection when insufficient funds are available</li>
 *   <li>Updating account balances automatically when transactions are created</li>
 *   <li>Notifying the parent activity when transactions are successfully created</li>
 * </ul>
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Transaction
 * @see BetaAccount
 * @see AlternativeBetaAccountDialog
 */
public class AddTransactionDialog extends Dialog {
    /**
     * The application context used for UI operations.
     */
    private final Context context;
    
    /**
     * The database instance for data access.
     */
    private final MeshaDatabase database;
    
    /**
     * The BetaAccount to which the transaction will be added.
     */
    private final BetaAccount betaAccount;
    
    /**
     * Listener to notify when a transaction is successfully added.
     */
    private OnTransactionAddedListener listener;
    
    /**
     * Spinner for selecting transaction categories.
     */
    private Spinner categorySpinner;
    
    /**
     * List of available transaction categories.
     */
    private List<Category> categories = new ArrayList<>();
    
    /**
     * UI components for the progress indicator
     */
    private ProgressBar progressBar;
    private TextView statusTextView;
    
    /**
     * Main thread handler for UI updates
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Dedicated executor for database operations
     */
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    
    /**
     * Flag to track if the dialog is active to prevent memory leaks
     */
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    /**
     * Interface for notifying when a transaction has been successfully added.
     * <p>
     * This callback allows the parent activity or fragment to be notified when
     * a transaction is successfully created, enabling it to refresh its UI or
     * perform additional actions.
     * </p>
     */
    public interface OnTransactionAddedListener {
        /**
         * Called when a transaction is successfully added to the database.
         */
        void onTransactionAdded();
    }

    /**
     * Creates a new AddTransactionDialog instance.
     * <p>
     * Initializes the dialog with references to the necessary context, database,
     * and BetaAccount, then calls setupDialog() to initialize the UI components.
     * </p>
     *
     * @param context The context in which the dialog will be shown
     * @param database The database instance for accessing data
     * @param betaAccount The BetaAccount to which the transaction will be added
     */
    public AddTransactionDialog(Context context, MeshaDatabase database, BetaAccount betaAccount) {
        super(context);
        this.context = context;
        this.database = database;
        this.betaAccount = betaAccount;
        
        // Set a dismiss listener to handle cleanup
        setOnDismissListener(dialog -> cleanupResources());
        
        // Load categories before setting up dialog to reduce main thread work
        preloadCategories();
    }
    
    /**
     * Cleans up resources to prevent memory leaks when the dialog is dismissed.
     */
    private void cleanupResources() {
        // Mark dialog as inactive to prevent further callbacks
        isActive.set(false);
        
        // Remove all callbacks from the handler
        mainHandler.removeCallbacksAndMessages(null);
        
        // Shutdown the executor service (gracefully)
        dbExecutor.shutdown();
    }

    /**
     * Preloads categories in the background before setting up the dialog UI.
     * This helps reduce main thread work during dialog display.
     */
    private void preloadCategories() {
        executeIfActive(() -> {
            try {
                // Load categories in background thread
                categories = database.categoryDao().getAllCategories();
                
                // Setup dialog on main thread after categories are loaded
                postToMainThreadIfActive(this::setupDialog);
            } catch (Exception e) {
                // Handle errors
                postToMainThreadIfActive(() -> {
                    setupDialog();
                    Toast.makeText(context, "Failed to load categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Sets the listener to be notified when a transaction is added.
     * <p>
     * The listener will be called after a transaction is successfully
     * added to the database.
     * </p>
     *
     * @param listener The listener to be notified
     */
    public void setOnTransactionAddedListener(OnTransactionAddedListener listener) {
        this.listener = listener;
    }

    /**
     * Sets up the dialog UI and event handlers.
     * <p>
     * This method initializes the dialog's layout, finds all UI components,
     * sets up event listeners, and loads the category spinner data.
     * </p>
     */
    private void setupDialog() {
        if (!isActive.get()) return;
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null);
        setContentView(dialogView);

        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etDescription = dialogView.findViewById(R.id.etTransactionDescription);
        RadioGroup rgTransactionType = dialogView.findViewById(R.id.rgTransactionType);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Initialize progress components
        progressBar = dialogView.findViewById(R.id.progressBar);
        statusTextView = dialogView.findViewById(R.id.statusTextView);
        
        if (progressBar == null || statusTextView == null) {
            // If these views don't exist in your layout, handle gracefully
            // You can add them programmatically or skip using them
        }

        // Initialize category spinner
        categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        
        // Setup spinner with preloaded categories
        setupCategorySpinner();

        btnAdd.setOnClickListener(v -> {
            if (!isActive.get()) return;
            
            // Disable button to prevent multiple clicks
            btnAdd.setEnabled(false);
            
            String amountStr = etAmount.getText().toString();
            String description = etDescription.getText().toString();
            String type = rgTransactionType.getCheckedRadioButtonId() == R.id.rbCredit ? "CREDIT" : "DEBIT";

            if (amountStr.isEmpty() || description.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                btnAdd.setEnabled(true);
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                
                // For DEBIT transactions, check if it would cause a negative balance
                if (type.equals("DEBIT")) {
                    // Show loading status during validation
                    showLoading("Checking balance...");
                    
                    // Move balance check to background thread
                    executeIfActive(() -> {
                        try {
                            // Get the latest balance from database
                            BetaAccount latestAccount = database.betaAccountDao().getBetaAccountById(betaAccount.getBetaAccountId());
                            double currentBalance = latestAccount != null ? 
                                latestAccount.getBetaAccountBalance() : betaAccount.getBetaAccountBalance();
                            
                            if (amount > currentBalance) {
                                // Insufficient funds - show alternative dialog on main thread
                                postToMainThreadIfActive(() -> {
                                    hideLoading();
                                    btnAdd.setEnabled(true);
                                    showAlternativeAccountDialog(amount, description);
                                });
                                return;
                            }
                            
                            // Sufficient funds - proceed with transaction on main thread
                            postToMainThreadIfActive(() -> {
                                createTransaction(amount, type, description);
                                btnAdd.setEnabled(true);
                                dismiss();
                            });
                        } catch (Exception e) {
                            postToMainThreadIfActive(() -> {
                                hideLoading();
                                btnAdd.setEnabled(true);
                                Toast.makeText(context, "Error checking balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    // Credit transaction - no balance check needed
                    createTransaction(amount, type, description);
                    btnAdd.setEnabled(true);
                    dismiss();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
                btnAdd.setEnabled(true);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    /**
     * Sets up the category spinner with preloaded categories.
     * This method runs on the main thread but uses preloaded data.
     */
    private void setupCategorySpinner() {
        if (categorySpinner != null) {
            CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(context, categories);
            categorySpinner.setAdapter(adapter);
        }
    }

    /**
     * Creates a new transaction and updates account balances.
     * <p>
     * This method performs the following actions:
     * <ol>
     *   <li>Creates a new Transaction object with the provided data</li>
     *   <li>Inserts the transaction into the database</li>
     *   <li>Updates the BetaAccount balance</li>
     *   <li>Updates the parent AlphaAccount balance</li>
     *   <li>Notifies the listener of the successful transaction</li>
     * </ol>
     * </p>
     * <p>
     * All database operations are performed on a background thread.
     * </p>
     *
     * @param amount The transaction amount
     * @param type The transaction type ("CREDIT" or "DEBIT")
     * @param description The transaction description
     */
    private void createTransaction(double amount, String type, String description) {
        if (!isActive.get()) return;
        
        Category selectedCategory = (Category) categorySpinner.getSelectedItem();
        
        // Show loading status
        showLoading("Creating transaction...");
        
        Transaction newTransaction = new Transaction(
            betaAccount.getAlphaAccountId(),
            betaAccount.getBetaAccountId(),
            selectedCategory != null ? selectedCategory.getCategoryId() : 1, // Default to General category
            description,
            amount,
            type.equals("CREDIT") ? TransactionType.CREDIT : TransactionType.DEBIT,
            System.currentTimeMillis()
        );

        executeIfActive(() -> {
            try {
                // Update progress status
                updateLoadingStatus("Saving to database...");
                
                // Get the latest account data before making changes
                BetaAccount currentBeta = database.betaAccountDao().getBetaAccountById(betaAccount.getBetaAccountId());
                AlphaAccount alphaAccount = database.alphaAccountDao().getAlphaAccountById(currentBeta.getAlphaAccountId());
                
                // Calculate new balances
                double newBetaBalance = currentBeta.getBetaAccountBalance();
                newBetaBalance += type.equals("CREDIT") ? amount : -amount;
                currentBeta.setBetaAccountBalance(newBetaBalance);
                
                double newAlphaBalance = 0;
                if (alphaAccount != null) {
                    newAlphaBalance = alphaAccount.getAlphaAccountBalance();
                    newAlphaBalance += type.equals("CREDIT") ? amount : -amount;
                    alphaAccount.setAlphaAccountBalance(newAlphaBalance);
                }
                
                // Begin transaction to ensure consistency
                database.runInTransaction(() -> {
                    // 1. Insert the transaction
                    database.transactionDao().insert(newTransaction);
                    
                    // 2. Update Beta account balance
                    database.betaAccountDao().update(currentBeta);
                    
                    // 3. Update Alpha account balance
                    if (alphaAccount != null) {
                        database.alphaAccountDao().update(alphaAccount);
                    }
                });
                
                // Update our instance with the new balance
                betaAccount.setBetaAccountBalance(newBetaBalance);
                
                // Update UI on the main thread
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    
                    if (listener != null) {
                        listener.onTransactionAdded();
                    }
                    
                    Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    Toast.makeText(context, "Error adding transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Shows a dialog to select an alternative account when insufficient funds are available.
     * <p>
     * This method is called when attempting to create a debit transaction with an amount
     * greater than the current BetaAccount balance. It shows the AlternativeBetaAccountDialog
     * to allow the user to transfer funds from another account before completing the transaction.
     * </p>
     *
     * @param amount The transaction amount that exceeds the current balance
     * @param description The transaction description
     */
    private void showAlternativeAccountDialog(double amount, String description) {
        if (!isActive.get()) return;
        
        AlternativeBetaAccountDialog dialog = new AlternativeBetaAccountDialog(
            context, database, betaAccount, amount, description);
        
        dialog.setOnTransactionCompletedListener(() -> {
            if (!isActive.get()) return;
            
            // Show loading status
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
                        
                        // Now proceed with the original transaction
                        postToMainThreadIfActive(() -> {
                            // Proceed with original transaction now that funds are available
                            createTransaction(amount, "DEBIT", description);
                            dismiss(); // Now we can dismiss
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    postToMainThreadIfActive(() -> {
                        hideLoading();
                        Toast.makeText(context, "Error refreshing account data", Toast.LENGTH_SHORT).show();
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