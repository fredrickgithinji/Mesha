package com.dzovah.mesha.Methods.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import com.dzovah.mesha.Activities.Adapters.IconAdapter;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.R;
import com.google.android.material.textfield.TextInputEditText;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog for editing or deleting existing Alpha and Beta accounts.
 * <p>
 * This dialog allows users to:
 * <ul>
 *   <li>Edit account names</li>
 *   <li>Change account icons</li>
 *   <li>Delete accounts with confirmation</li>
 * </ul>
 * </p>
 * <p>
 * The dialog automatically determines whether it's editing an AlphaAccount or
 * BetaAccount based on the object type passed to the constructor, and adjusts its
 * behavior accordingly. For example, deleting an AlphaAccount will also delete all
 * associated BetaAccounts and transactions, while deleting a BetaAccount will only
 * delete that account and its transactions.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see AlphaAccount
 * @see BetaAccount
 * @see CreateAccountDialog
 */
public class EditAccountDialog {
    /** The application context used for UI operations */
    private final Context context;
    
    /** The database instance for data access */
    private final MeshaDatabase database;
    
    /** The account being edited (either AlphaAccount or BetaAccount) */
    private final Object account;
    
    /** Listener to notify when an account is edited or deleted */
    private OnAccountEditedListener listener;
    
    /** The AlertDialog instance that displays the UI */
    private AlertDialog dialog;
    
    /** Flag indicating whether the dialog is editing a BetaAccount (true) or AlphaAccount (false) */
    private final boolean isBetaAccount;
    
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
     * Interface for notifying when an account has been edited or deleted.
     * <p>
     * This callback allows the parent activity or fragment to be notified when
     * account operations are completed, enabling it to refresh its UI or
     * perform additional actions.
     * </p>
     */
    public interface OnAccountEditedListener {
        /**
         * Called when an account is successfully edited.
         */
        void onAccountEdited();
        
        /**
         * Called when an account is successfully deleted.
         */
        void onAccountDeleted();
    }

    /**
     * Creates a new EditAccountDialog instance.
     * <p>
     * Initializes the dialog with references to the necessary context, database,
     * and account object to be edited. The dialog automatically determines whether
     * it's editing an AlphaAccount or BetaAccount based on the object type.
     * </p>
     *
     * @param context The context in which the dialog will be shown
     * @param database The database instance for accessing data
     * @param account The account object to edit (either AlphaAccount or BetaAccount)
     */
    public EditAccountDialog(Context context, MeshaDatabase database, Object account) {
        this.context = context;
        this.database = database;
        this.account = account;
        this.isBetaAccount = account instanceof BetaAccount;
    }

    /**
     * Sets the listener to be notified when an account is edited or deleted.
     * <p>
     * The listener will be called after an account is successfully
     * updated or removed from the database.
     * </p>
     *
     * @param listener The listener to be notified
     */
    public void setOnAccountEditedListener(OnAccountEditedListener listener) {
        this.listener = listener;
    }

    /**
     * Displays the account editing dialog to the user.
     * <p>
     * This method creates and shows the AlertDialog containing the account
     * editing UI. It calls getView() to populate the dialog with the necessary
     * UI components and pre-populate the fields with the current account data.
     * </p>
     */
    public void show() {
        if (!isActive.get()) return;
        
        View dialogView = getView();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        dialog = builder.create();
        
        // Set dismiss listener to clean up resources
        dialog.setOnDismissListener(dialogInterface -> cleanupResources());
        
        dialog.show();
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
     * Creates and returns the dialog's view with all UI components.
     * <p>
     * This method initializes the dialog's layout, finds all UI components,
     * pre-populates fields with the current account data, sets up event listeners,
     * and configures the icon selection grid.
     * </p>
     *
     * @return The configured view for the AlertDialog
     */
    private View getView() {
        if (!isActive.get()) return null;
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_account, null);
        TextInputEditText etAccountName = dialogView.findViewById(R.id.etAccountName);
        RecyclerView rvIcons = dialogView.findViewById(R.id.rvIcons);
        Button btnSave = dialogView.findViewById(R.id.btnCreate);
        Button btnDelete = dialogView.findViewById(R.id.btnCancel);
        
        // Initialize progress indicators if they exist in the layout
        progressBar = dialogView.findViewById(R.id.progressBar);
        statusTextView = dialogView.findViewById(R.id.statusTextView);

        // Set current values based on account type
        String currentName = isBetaAccount ? 
            ((BetaAccount)account).getBetaAccountName() : 
            ((AlphaAccount)account).getAlphaAccountName();
        String currentIcon = isBetaAccount ? 
            ((BetaAccount)account).getBetaAccountIcon() : 
            ((AlphaAccount)account).getAlphaAccountIcon();

        etAccountName.setText(currentName);
        btnSave.setText("Save Changes");
        btnDelete.setText("Delete Account");

        String[] iconPaths = CreateAccountDialog.getIconPaths(context);
        final String[] selectedIcon = {currentIcon};
        
        GridLayoutManager layoutManager = new GridLayoutManager(context, 4);
        rvIcons.setLayoutManager(layoutManager);
        IconAdapter adapter = new IconAdapter(iconPaths, iconPath -> {
            if (!isActive.get()) return;
            selectedIcon[0] = iconPath;
        });
        rvIcons.setAdapter(adapter);

        btnSave.setOnClickListener(v -> {
            if (!isActive.get()) return;
            
            String newName = etAccountName.getText().toString();
            if (newName.isEmpty()) {
                Toast.makeText(context, "Please enter an account name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Disable buttons to prevent multiple clicks
            btnSave.setEnabled(false);
            btnDelete.setEnabled(false);
            
            updateAccount(newName, selectedIcon[0]);
        });

        btnDelete.setOnClickListener(v -> {
            if (!isActive.get()) return;
            
            String message = isBetaAccount ? 
                "Are you sure you want to delete this account? This will delete all associated transactions." :
                "Are you sure you want to delete this account? This will delete all associated beta accounts and transactions.";

            new AlertDialog.Builder(context)
                .setTitle("Delete Account")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!isActive.get()) return;
                    deleteAccount();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        return dialogView;
    }

    /**
     * Updates an existing account with new name and icon.
     * <p>
     * This method updates either an AlphaAccount or BetaAccount (depending on
     * the account type) with the new name and icon selected by the user. The
     * update is performed asynchronously on a background thread.
     * </p>
     *
     * @param newName The new name for the account
     * @param newIcon The path of the newly selected icon, or null to keep the current icon
     */
    private void updateAccount(String newName, String newIcon) {
        if (!isActive.get()) return;
        
        showLoading("Updating account...");
        
        executeIfActive(() -> {
            try {
                String iconPath = newIcon != null ? "Assets/icons/" + newIcon : 
                    (isBetaAccount ? ((BetaAccount)account).getBetaAccountIcon() : 
                    ((AlphaAccount)account).getAlphaAccountIcon());

                if (isBetaAccount) {
                    BetaAccount betaAccount = (BetaAccount)account;
                    betaAccount.setBetaAccountName(newName);
                    betaAccount.setBetaAccountIcon(iconPath);
                    database.betaAccountDao().update(betaAccount);
                } else {
                    AlphaAccount alphaAccount = (AlphaAccount)account;
                    alphaAccount.setAlphaAccountName(newName);
                    alphaAccount.setAlphaAccountIcon(iconPath);
                    database.alphaAccountDao().update(alphaAccount);
                }
                
                handleSuccess(false); // false indicates edited, not deleted
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    /**
     * Deletes an existing account from the database.
     * <p>
     * This method removes either an AlphaAccount or BetaAccount (depending on
     * the account type) from the database. If deleting an AlphaAccount, all associated
     * BetaAccounts and transactions will also be deleted due to database CASCADE rules.
     * If deleting a BetaAccount, only that account and its transactions will be deleted.
     * </p>
     * <p>
     * The operation is performed asynchronously on a background thread.
     * </p>
     */
    private void deleteAccount() {
        if (!isActive.get()) return;
        
        showLoading("Deleting account...");
        
        executeIfActive(() -> {
            try {
                if (isBetaAccount) {
                    BetaAccount betaAccount = (BetaAccount)account;
                    database.betaAccountDao().delete(betaAccount);
                    database.betaAccountDao().updateAlphaAccountBalance(betaAccount.getAlphaAccountId());
                } else {
                    database.alphaAccountDao().delete((AlphaAccount)account);
                }
                handleSuccess(true); // true indicates deleted, not edited
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    /**
     * Handles successful account editing or deletion.
     * <p>
     * This method runs on the UI thread to notify the appropriate listener method
     * (either onAccountEdited or onAccountDeleted) based on which operation was
     * performed. It ensures that all UI operations occur on the main thread.
     * </p>
     * 
     * @param wasDeleted true if the account was deleted, false if it was edited
     */
    private void handleSuccess(boolean wasDeleted) {
        postToMainThreadIfActive(() -> {
            hideLoading();
            if (listener != null) {
                if (wasDeleted) {
                    listener.onAccountDeleted();
                } else {
                    listener.onAccountEdited();
                }
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
    }

    /**
     * Handles errors that occur during account operations.
     * <p>
     * This method logs the exception, displays an error message to the user,
     * and ensures that all UI operations occur on the main thread.
     * </p>
     *
     * @param e The exception that occurred during the operation
     */
    private void handleError(Exception e) {
        e.printStackTrace();
        postToMainThreadIfActive(() -> {
            hideLoading();
            Toast.makeText(context, "Operation failed", Toast.LENGTH_SHORT).show();
            
            // Re-enable buttons
            if (dialog != null) {
                Button btnSave = dialog.findViewById(R.id.btnCreate);
                Button btnDelete = dialog.findViewById(R.id.btnCancel);
                if (btnSave != null) btnSave.setEnabled(true);
                if (btnDelete != null) btnDelete.setEnabled(true);
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
}