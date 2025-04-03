package com.dzovah.mesha.Methods.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Activities.Adapters.SelectPBetaAccountAdapter;
import com.dzovah.mesha.Database.Entities.PAlphaAccount;
import com.dzovah.mesha.Database.Entities.PBetaAccount;
import com.dzovah.mesha.Database.Entities.PTransaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog for selecting an alternative account when a transaction requires more funds than available.
 * <p>
 * This dialog is displayed when a user attempts to create a debit transaction with an amount
 * greater than the current balance of the selected BetaAccount. It allows the user to:
 * <ul>
 *   <li>View other accounts with sufficient funds</li>
 *   <li>Select an account to transfer funds from</li>
 *   <li>Process the transfer to cover the transaction</li>
 * </ul>
 * </p>
 * <p>
 * When a transfer is completed, the dialog automatically creates two transactions:
 * a debit transaction from the selected source account and a credit transaction to the
 * destination account, then updates all associated account balances.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see AddTransactionDialog
 * @see PBetaAccount
 * @see PTransaction
 */
public class AlternativePBetaAccountDialog extends Dialog {
    /** The application context used for UI operations */
    private final Context context;

    /** The database instance for data access */
    private final MeshaDatabase database;

    /** The BetaAccount that needs funds (destination account) */
    private final PBetaAccount sourceBetaAccount;

    /** The transaction amount required */
    private final double transactionAmount;

    /** The description of the original transaction */
    private final String transactionDescription;

    /** The selected account to transfer funds from */
    private PBetaAccount selectedTargetAccount = null;

    /** Listener to notify when the fund transfer is completed */
    private OnTransactionCompletedListener listener;

    /** Progress indicator for loading and processing */
    private ProgressBar progressBar;
    private TextView statusTextView;

    /** Main thread handler for UI updates */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Dedicated executor for database operations */
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    /** Flag to track if the dialog is active to prevent memory leaks */
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    /**
     * Interface for notifying when a fund transfer transaction has been completed.
     * <p>
     * This callback allows the calling dialog or activity to be notified when
     * funds have been successfully transferred, enabling it to proceed with
     * the original transaction.
     * </p>
     */
    public interface OnTransactionCompletedListener {
        /**
         * Called when a fund transfer transaction is successfully completed.
         */
        void onTransactionCompleted();
    }

    /**
     * Creates a new AlternativeBetaAccountDialog instance.
     * <p>
     * Initializes the dialog with references to the necessary context, database,
     * source account, and transaction details, then calls setupDialog() to
     * initialize the UI components.
     * </p>
     *
     * @param context The context in which the dialog will be shown
     * @param database The database instance for accessing data
     * @param sourceBetaAccount The BetaAccount that needs funds (original transaction account)
     * @param transactionAmount The amount needed for the original transaction
     * @param transactionDescription The description of the original transaction
     */
    public AlternativePBetaAccountDialog(Context context, MeshaDatabase database,
                                        PBetaAccount sourceBetaAccount,
                                        double transactionAmount,
                                        String transactionDescription) {
        super(context);
        this.context = context;
        this.database = database;
        this.sourceBetaAccount = sourceBetaAccount;
        this.transactionAmount = transactionAmount;
        this.transactionDescription = transactionDescription;

        // Set a dismiss listener to handle cleanup
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

        // Shutdown the executor service (gracefully)
        dbExecutor.shutdown();
    }

    /**
     * Sets the listener to be notified when a fund transfer is completed.
     * <p>
     * The listener will be called after funds are successfully transferred
     * from the selected account to the source account.
     * </p>
     *
     * @param listener The listener to be notified
     */
    public void setOnTransactionCompletedListener(OnTransactionCompletedListener listener) {
        this.listener = listener;
    }

    /**
     * Sets up the dialog UI and event handlers.
     * <p>
     * This method initializes the dialog's layout, finds all UI components,
     * sets up event listeners, and loads the list of available accounts with
     * sufficient funds for the transfer.
     * </p>
     */
    private void setupDialog() {
        if (!isActive.get()) return;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_alternative_beta_account, null);
        setContentView(dialogView);

        TextView tvMessage = dialogView.findViewById(R.id.tvInsufficientFundsMessage);
        RecyclerView rvAccounts = dialogView.findViewById(R.id.rvAlternativeAccounts);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnProceed = dialogView.findViewById(R.id.btnProceed);

        // Initialize progress indicators if they exist in the layout
        progressBar = dialogView.findViewById(R.id.progressBar);

        // Set message
        String message = "Insufficient funds in " + sourceBetaAccount.getPBetaAccountName() +
                ". Available: " + CurrencyFormatter.format(sourceBetaAccount.getPBetaAccountBalance()) +
                ", Required: " + CurrencyFormatter.format(transactionAmount);
        tvMessage.setText(message);

        // Set up RecyclerView
        rvAccounts.setLayoutManager(new LinearLayoutManager(context));
        SelectPBetaAccountAdapter adapter = new SelectPBetaAccountAdapter(context);
        rvAccounts.setAdapter(adapter);

        // Load alternative accounts
        loadAlternativeAccounts(adapter);

        // Set account selection listener
        adapter.setOnAccountSelectedListener(account -> {
            if (!isActive.get()) return;
            selectedTargetAccount = account;
            btnProceed.setEnabled(selectedTargetAccount != null);
        });

        // Set button listeners
        btnCancel.setOnClickListener(v -> {
            if (!isActive.get()) return;
            dismiss();
        });

        btnProceed.setEnabled(false); // Disable until an account is selected
        btnProceed.setOnClickListener(v -> {
            if (!isActive.get()) return;

            // Disable button to prevent multiple clicks
            btnProceed.setEnabled(false);

            if (selectedTargetAccount != null) {
                processTransaction();
            } else {
                Toast.makeText(context, "Please select an account", Toast.LENGTH_SHORT).show();
                btnProceed.setEnabled(true);
            }
        });
    }

    /**
     * Loads and displays alternative accounts with sufficient balance.
     * <p>
     * This method asynchronously fetches all BetaAccounts from the database,
     * filters them to include only those with sufficient balance for the transaction,
     * and updates the adapter with the filtered list of accounts.
     * </p>
     *
     * @param adapter The adapter to populate with the accounts data
     */
    private void loadAlternativeAccounts(SelectPBetaAccountAdapter adapter) {
        showLoading("Loading accounts...");

        executeIfActive(() -> {
            try {
                // Get all beta accounts from the same alpha account that have sufficient balance
                List<PBetaAccount> accounts = database.PbetaAccountDao().getAllPBetaAccounts();

                // Filter out the source account and accounts with insufficient balance
                accounts.removeIf(account ->
                        account.getPBetaAccountId() == sourceBetaAccount.getPBetaAccountId() ||
                                account.getPBetaAccountBalance() < transactionAmount
                );

                postToMainThreadIfActive(() -> {
                    hideLoading();
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
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    Toast.makeText(context, "Error loading alternative accounts", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Processes the fund transfer transaction between accounts.
     * <p>
     * This method performs the following actions on a background thread:
     * <ol>
     *   <li>Retrieves the AlphaAccount information for better transaction descriptions</li>
     *   <li>Creates a debit transaction for the source of funds (selected account)</li>
     *   <li>Creates a credit transaction for the destination (original transaction account)</li>
     *   <li>Updates the balance of both BetaAccounts involved</li>
     *   <li>Updates the balance of any AlphaAccounts involved if they differ</li>
     *   <li>Notifies the listener on successful completion</li>
     * </ol>
     * </p>
     */
    private void processTransaction() {
        if (!isActive.get()) return;

        showLoading("Processing transfer...");

        // Get the Alpha account names for better descriptions
        executeIfActive(() -> {
            try {
                // Get Alpha account names
                PAlphaAccount sourceAlpha = database.PalphaAccountDao().getPAlphaAccountById(
                        sourceBetaAccount.getPAlphaAccountId());
                PAlphaAccount targetAlpha = database.PalphaAccountDao().getPAlphaAccountById(
                        selectedTargetAccount.getPAlphaAccountId());

                String sourceAlphaName = sourceAlpha != null ? sourceAlpha.getPAlphaAccountName() : "Unknown";
                String targetAlphaName = targetAlpha != null ? targetAlpha.getPAlphaAccountName() : "Unknown";

                updateLoadingStatus("Creating transactions...");

                // Create a debit transaction on the target account
                PTransaction debitTransaction = new PTransaction(
                        selectedTargetAccount.getPAlphaAccountId(),
                        selectedTargetAccount.getPBetaAccountId(),
                        1,  // General category
                        "Transfer to " + sourceBetaAccount.getPBetaAccountName() + " in " +
                                sourceAlphaName + " Alpha Account: " + transactionDescription,
                        transactionAmount,
                        TransactionType.DEBIT,
                        System.currentTimeMillis()
                );

                // Create a credit transaction on the source account
                PTransaction creditTransaction = new PTransaction(
                        sourceBetaAccount.getPAlphaAccountId(),
                        sourceBetaAccount.getPBetaAccountId(),
                        1,  // General category
                        "Transfer from " + selectedTargetAccount.getPBetaAccountName() + " in " +
                                targetAlphaName + " Alpha Account: " + transactionDescription,
                        transactionAmount,
                        TransactionType.CREDIT,
                        System.currentTimeMillis()
                );

                updateLoadingStatus("Updating balances...");

                // Use a database transaction to ensure consistency
                database.runInTransaction(() -> {
                    // Insert both transactions
                    database.PtransactionDao().insert(debitTransaction);
                    database.PtransactionDao().insert(creditTransaction);

                    // Update beta account balances
                    // Target account (debit)
                    double newTargetBalance = selectedTargetAccount.getPBetaAccountBalance() - transactionAmount;
                    selectedTargetAccount.setPBetaAccountBalance(newTargetBalance);
                    database.PbetaAccountDao().update(selectedTargetAccount);

                    // Source account (credit)
                    double newSourceBalance = sourceBetaAccount.getPBetaAccountBalance() + transactionAmount;
                    sourceBetaAccount.setPBetaAccountBalance(newSourceBalance);
                    database.PbetaAccountDao().update(sourceBetaAccount);

                    // Update alpha account balances if needed
                    if (selectedTargetAccount.getPAlphaAccountId() != sourceBetaAccount.getPAlphaAccountId()) {
                        if (sourceAlpha != null && targetAlpha != null) {
                            // Update source alpha (credit)
                            double newSourceAlphaBalance = sourceAlpha.getPAlphaAccountBalance() + transactionAmount;
                            sourceAlpha.setPAlphaAccountBalance(newSourceAlphaBalance);
                            database.PalphaAccountDao().update(sourceAlpha);

                            // Update target alpha (debit)
                            double newTargetAlphaBalance = targetAlpha.getPAlphaAccountBalance() - transactionAmount;
                            targetAlpha.setPAlphaAccountBalance(newTargetAlphaBalance);
                            database.PalphaAccountDao().update(targetAlpha);
                        }
                    }
                });

                postToMainThreadIfActive(() -> {
                    hideLoading();
                    Toast.makeText(context, "Your account has been topped up successfully", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onTransactionCompleted();
                    }
                    dismiss();
                });
            } catch (Exception e) {
                e.printStackTrace();
                postToMainThreadIfActive(() -> {
                    hideLoading();
                    Toast.makeText(context, "Error processing transaction: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
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