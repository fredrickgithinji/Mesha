package com.dzovah.mesha.Activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.BitmapFactory;
import android.content.Intent;

import com.airbnb.lottie.LottieAnimationView;
import com.dzovah.mesha.Methods.Dialogs.CreateAccountDialog;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Methods.Utils.Quotes;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.dzovah.mesha.Activities.Adapters.BetaAccountAdapter;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;

import java.io.InputStream;
import java.util.List;

/**
 * Activity for displaying detailed information about an Alpha Account.
 * <p>
 * This activity provides:
 * <ul>
 *     <li>Detailed view of a specific Alpha Account with its name, icon, and balance</li>
 *     <li>List of all Beta Accounts associated with this Alpha Account</li>
 *     <li>Ability to create new Beta Accounts within this Alpha Account</li>
 *     <li>Real-time calculation and display of the Alpha Account balance</li>
 * </ul>
 * The activity receives the Alpha Account ID via intent extra and loads all
 * necessary data from the database to populate the UI. Beta Accounts are displayed
 * in a RecyclerView with their respective details.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see AlphaAccount
 * @see BetaAccount
 * @see BetaAccountAdapter
 * @see CreateAccountDialog
 */
public class AlphaAccountDetailActivity extends AppCompatActivity {
    /** Database instance for accessing app data */
    private MeshaDatabase database;
    
    /** Adapter for displaying beta accounts in the RecyclerView */
    private BetaAccountAdapter betaAccountAdapter;
    
    /** ID of the Alpha Account being displayed */
    private int alphaAccountId;

    /**
     * Initializes the activity, sets up UI components, and loads Alpha Account data.
     * <p>
     * This method retrieves the Alpha Account ID from the intent, initializes views,
     * and triggers the loading of Alpha Account details and associated Beta Accounts.
     * If the Alpha Account ID is invalid, the activity finishes with an error message.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alpha_account_detail);
        
        alphaAccountId = getIntent().getIntExtra("alpha_account_id", -1);
        if (alphaAccountId == -1) {
            Toast.makeText(this, "Error loading account", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = MeshaDatabase.Get_database(getApplicationContext());
        initializeViews();
        loadAlphaAccountDetails();
        loadBetaAccounts();
    }

    /**
     * Initializes and sets up all UI components of the detail screen.
     * <p>
     * This includes setting up:
     * <ul>
     *     <li>Animations</li>
     *     <li>RecyclerView for Beta Accounts</li>
     *     <li>Floating action button for adding new Beta Accounts</li>
     * </ul>
     * </p>
     */
    private void initializeViews() {
        LottieAnimationView bubblesAnimationView = findViewById(R.id.bubbles);
        LottieAnimationView piechartAnimationView = findViewById(R.id.money);
        FloatingActionButton fabAddBeta = findViewById(R.id.fabAddBetaAccount);

        bubblesAnimationView.playAnimation();
        piechartAnimationView.playAnimation();
        
        RecyclerView rvBetaAccounts = findViewById(R.id.rvBetaAccounts);
        rvBetaAccounts.setLayoutManager(new LinearLayoutManager(this));
        betaAccountAdapter = new BetaAccountAdapter(this);
        rvBetaAccounts.setAdapter(betaAccountAdapter);

        fabAddBeta.setOnClickListener(v -> showCreateBetaDialog());
    }

    /**
     * Loads and displays the Alpha Account details from the database.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves the Alpha Account data from the database</li>
     *     <li>Calculates its current balance based on transactions</li>
     *     <li>Updates the UI with account name, balance, and icon</li>
     * </ul>
     * The operations are performed on a background thread to avoid blocking the UI.
     * </p>
     */
    private void loadAlphaAccountDetails() {
        if (alphaAccountId == -1) return;
        
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get the account
                AlphaAccount account = database.alphaAccountDao().getAlphaAccountById(alphaAccountId);
                
                if (account != null) {
                    // Calculate accurate balance using the transaction DAO method
                    double calculatedBalance = database.transactionDao().getAlphaAccountBalanceById(alphaAccountId);
                    // Update the account with the calculated balance
                    account.setAlphaAccountBalance(calculatedBalance);
                    
                    // Use existing UI update code
                    runOnUiThread(() -> {
                        TextView tvAlphaName = findViewById(R.id.tvAlphaAccountName);
                        TextView tvAlphaBalance = findViewById(R.id.tvAlphaAccountBalance);
                        ImageView ivAlphaIcon = findViewById(R.id.ivAlphaAccountIcon);

                        tvAlphaName.setText(account.getAlphaAccountName());
                        tvAlphaBalance.setText(CurrencyFormatter.format(account.getAlphaAccountBalance()));

                        try {
                            String iconPath = account.getAlphaAccountIcon().replace("Assets/", "");
                            InputStream is = getAssets().open(iconPath);
                            ivAlphaIcon.setImageBitmap(BitmapFactory.decodeStream(is));
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading account details", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    /**
     * Updates the UI elements with fresh account data.
     * <p>
     * This method is used to refresh the display when account details change.
     * </p>
     *
     * @param account The AlphaAccount with updated information
     */
    private void updateUI(AlphaAccount account) {
        // Update UI elements with fresh account data
        TextView tvBalance = findViewById(R.id.tvAlphaAccountBalance);
        tvBalance.setText(CurrencyFormatter.format(account.getAlphaAccountBalance()));
        // Update other UI elements...
    }

    /**
     * Loads and displays Beta Accounts associated with this Alpha Account.
     * <p>
     * This method retrieves all Beta Accounts linked to the current Alpha Account
     * from the database and updates the RecyclerView through the adapter.
     * The operations are performed on a background thread to avoid blocking the UI.
     * </p>
     */
    private void loadBetaAccounts() {
        try {
            MeshaDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    List<BetaAccount> accounts = database.betaAccountDao().getBetaAccountsByAlphaAccountId(alphaAccountId);
                    runOnUiThread(() -> {
                        try {
                            betaAccountAdapter.setBetaAccounts(accounts);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error displaying beta accounts", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Error loading beta accounts", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to access database", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Displays the dialog for creating a new Beta Account.
     * <p>
     * This method opens a dialog that allows the user to create a new Beta Account
     * within the current Alpha Account. After successful creation, it refreshes
     * the list of Beta Accounts and updates the Alpha Account details to reflect
     * any balance changes.
     * </p>
     */
    private void showCreateBetaDialog() {
        CreateAccountDialog dialog = new CreateAccountDialog(this, database, alphaAccountId);
        dialog.setOnAccountCreatedListener(account -> {
            try {
                if (account instanceof BetaAccount) {
                    Toast.makeText(this, "Beta Account created successfully", Toast.LENGTH_SHORT).show();
                    loadBetaAccounts();
                    loadAlphaAccountDetails(); // Reload alpha account to show updated balance
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error updating beta account list", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    /**
     * Handles activity results to refresh data when needed.
     * <p>
     * This method is called when an activity launched from this one returns a result.
     * If the result is RESULT_OK, it refreshes both the Alpha Account details and
     * the list of Beta Accounts to ensure the UI displays current data.
     * </p>
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult()
     * @param resultCode The integer result code returned by the child activity
     * @param data An Intent, which can return result data to the caller
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadAlphaAccountDetails();
            loadBetaAccounts();
        }
    }
}