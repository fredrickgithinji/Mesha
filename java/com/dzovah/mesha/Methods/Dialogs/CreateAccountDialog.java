package com.dzovah.mesha.Methods.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog for creating new Alpha and Beta accounts in the Mesha financial management system.
 * <p>
 * This class provides a dialog interface for users to create either:
 * <ul>
 *   <li>Alpha accounts (top-level accounts)</li>
 *   <li>Beta accounts (sub-accounts that belong to an Alpha account)</li>
 * </ul>
 * </p>
 * <p>
 * The dialog allows users to:
 * <ul>
 *   <li>Enter an account name</li>
 *   <li>Select an icon from the available icon gallery</li>
 *   <li>Create the account and save it to the database</li>
 * </ul>
 * </p>
 * <p>
 * Two constructors are provided to handle the creation of either Alpha or Beta accounts,
 * with appropriate parameter requirements for each type.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see AlphaAccount
 * @see BetaAccount
 * @see MeshaDatabase
 */
public class CreateAccountDialog {
    /**
     * The application context used for UI operations.
     */
    private final Context context;
    
    /**
     * The database instance for data access.
     */
    private final MeshaDatabase database;
    
    /**
     * Listener to notify when an account is successfully created.
     */
    private OnAccountCreatedListener listener;
    
    /**
     * The AlertDialog instance that displays the UI.
     */
    private AlertDialog dialog;
    
    /**
     * Flag indicating whether the dialog is creating a BetaAccount (true) or AlphaAccount (false).
     */
    private final boolean isBetaAccount;
    
    /**
     * The ID of the parent AlphaAccount (only used when creating a BetaAccount).
     */
    private final int parentAlphaId;
    
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
     * Interface for notifying when an account has been successfully created.
     * <p>
     * This callback allows the parent activity or fragment to be notified when
     * an account is successfully created, enabling it to refresh its UI or
     * perform additional actions.
     * </p>
     */
    public interface OnAccountCreatedListener {
        /**
         * Called when an account is successfully created.
         * 
         * @param account The newly created account object (either AlphaAccount or BetaAccount)
         */
        void onAccountCreated(Object account); // Changed to Object to handle both types
    }

    /**
     * Constructor for creating an Alpha Account dialog.
     * <p>
     * This constructor initializes a dialog for creating a new top-level account (AlphaAccount).
     * </p>
     *
     * @param context The context in which the dialog will be shown
     * @param database The database instance for accessing data
     */
    public CreateAccountDialog(Context context, MeshaDatabase database) {
        this.context = context;
        this.database = database;
        this.isBetaAccount = false;
        this.parentAlphaId = -1;
    }

    /**
     * Constructor for creating a Beta Account dialog.
     * <p>
     * This constructor initializes a dialog for creating a new sub-account (BetaAccount)
     * that belongs to an existing AlphaAccount.
     * </p>
     *
     * @param context The context in which the dialog will be shown
     * @param database The database instance for accessing data
     * @param alphaAccountId The ID of the parent AlphaAccount to which the new BetaAccount will belong
     */
    public CreateAccountDialog(Context context, MeshaDatabase database, int alphaAccountId) {
        this.context = context;
        this.database = database;
        this.isBetaAccount = true;
        this.parentAlphaId = alphaAccountId;
    }

    /**
     * Sets the listener to be notified when an account is created.
     * <p>
     * The listener will be called after an account is successfully
     * added to the database.
     * </p>
     *
     * @param listener The listener to be notified
     */
    public void setOnAccountCreatedListener(OnAccountCreatedListener listener) {
        this.listener = listener;
    }

    /**
     * Displays the account creation dialog to the user.
     * <p>
     * This method creates and shows the AlertDialog containing the account
     * creation UI. It calls getView() to populate the dialog with the necessary
     * UI components.
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
     * sets up event listeners, and configures the icon selection grid.
     * </p>
     *
     * @return The configured view for the AlertDialog
     */
    private View getView() {
        if (!isActive.get()) return null;
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_account, null);
        TextInputEditText etAccountName = dialogView.findViewById(R.id.etAccountName);
        RecyclerView rvIcons = dialogView.findViewById(R.id.rvIcons);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Initialize progress indicators if they exist in the layout
        progressBar = dialogView.findViewById(R.id.progressBar);
        statusTextView = dialogView.findViewById(R.id.statusTextView);

        // Setup icon recycler view
        String[] iconPaths = CreateAccountDialog.getIconPaths(context);
        final String[] selectedIcon = {null};

        GridLayoutManager layoutManager = new GridLayoutManager(context, 4);
        rvIcons.setLayoutManager(layoutManager);
        IconAdapter adapter = new IconAdapter(iconPaths, iconPath -> {
            if (!isActive.get()) return;
            selectedIcon[0] = iconPath;
        });
        rvIcons.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> {
            if (!isActive.get()) return;
            
            String accountName = etAccountName.getText().toString();
            if (accountName.isEmpty()) {
                Toast.makeText(context, "Please enter an account name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Disable button to prevent multiple clicks
            btnCreate.setEnabled(false);

            if (isBetaAccount) {
                createBetaAccount(accountName, selectedIcon[0]);
            } else {
                createAlphaAccount(accountName, selectedIcon[0]);
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (!isActive.get()) return;
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        return dialogView;
    }

    /**
     * Retrieves the list of available account icons from the assets directory.
     * <p>
     * This static method accesses the application's assets to find all icon files
     * in the "icons" directory. These icons are then displayed in the dialog for
     * the user to select when creating a new account.
     * </p>
     *
     * @param context The context used to access application assets
     * @return An array of icon file paths, or an empty array if none are found
     */
    public static String[] getIconPaths(Context context) {
        try {
            String[] paths = context.getAssets().list("icons");
            Log.d("IconDebug", "Found " + (paths != null ? paths.length : 0) + " icons");
            if (paths != null) {
                for (String path : paths) {
                    Log.d("IconDebug", "Icon path: " + path);
                }
            }
            return paths != null ? paths : new String[0];
        } catch (IOException e) {
            Log.e("IconDebug", "Error loading icons: " + e.getMessage());
            e.printStackTrace();
            return new String[0];
        }
    }

    /**
     * Creates a new Alpha Account and inserts it into the database.
     * <p>
     * This method constructs an AlphaAccount object with the provided name,
     * selected icon (or default if none selected), and an initial balance of zero.
     * It then asynchronously inserts the account into the database and notifies
     * the listener upon success.
     * </p>
     *
     * @param accountName The name for the new Alpha Account
     * @param selectedIcon The path of the selected icon, or null if none selected
     */
    private void createAlphaAccount(String accountName, String selectedIcon) {
        if (!isActive.get()) return;
        
        showLoading("Creating alpha account...");
        
        try {
            String iconPath = selectedIcon != null ? "Assets/icons/" + selectedIcon : "Assets/icons/default_icon.png";
            AlphaAccount newAccount = new AlphaAccount(accountName, iconPath, 0.0);
            
            executeIfActive(() -> {
                try {
                    database.alphaAccountDao().insert(newAccount);
                    handleSuccess(newAccount);
                } catch (Exception e) {
                    handleError(e);
                }
            });
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Creates a new Beta Account and inserts it into the database.
     * <p>
     * This method constructs a BetaAccount object with the provided name,
     * selected icon (or default if none selected), an initial balance of zero,
     * and associates it with the specified parent AlphaAccount. It then
     * asynchronously inserts the account into the database, updates the parent
     * AlphaAccount's balance, and notifies the listener upon success.
     * </p>
     *
     * @param accountName The name for the new Beta Account
     * @param selectedIcon The path of the selected icon, or null if none selected
     */
    private void createBetaAccount(String accountName, String selectedIcon) {
        if (!isActive.get()) return;
        
        showLoading("Creating beta account...");
        
        try {
            String iconPath = selectedIcon != null ? "Assets/icons/" + selectedIcon : "Assets/icons/default_icon.png";
            BetaAccount newAccount = new BetaAccount(parentAlphaId, accountName, iconPath, 0.0);
            
            executeIfActive(() -> {
                try {
                    updateLoadingStatus("Saving to database...");
                    database.betaAccountDao().insert(newAccount);
                    
                    updateLoadingStatus("Updating parent account...");
                    database.betaAccountDao().updateAlphaAccountBalance(parentAlphaId);
                    
                    handleSuccess(newAccount);
                } catch (Exception e) {
                    handleError(e);
                }
            });
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Handles successful account creation.
     * <p>
     * This method runs on the UI thread to notify the listener of successful account
     * creation and dismiss the dialog. It ensures that all UI operations occur on
     * the main thread to avoid threading issues.
     * </p>
     *
     * @param account The newly created account object to pass to the listener
     */
    private void handleSuccess(Object account) {
        postToMainThreadIfActive(() -> {
            hideLoading();
            if (listener != null) {
                listener.onAccountCreated(account);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
    }

    /**
     * Handles errors that occur during account creation.
     * <p>
     * This method logs the exception, displays an error message to the user,
     * and ensures that all UI operations occur on the main thread.
     * </p>
     *
     * @param e The exception that occurred during account creation
     */
    private void handleError(Exception e) {
        e.printStackTrace();
        postToMainThreadIfActive(() -> {
            hideLoading();
            Toast.makeText(context, "Failed to create account", Toast.LENGTH_SHORT).show();
            
            // Re-enable create button
            if (dialog != null) {
                Button btnCreate = dialog.findViewById(R.id.btnCreate);
                if (btnCreate != null) {
                    btnCreate.setEnabled(true);
                }
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