package com.dzovah.mesha.Activities;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.dzovah.mesha.Activities.Adapters.TransactionAdapter;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Database.Utils.TransactionType;
import com.dzovah.mesha.Methods.Dialogs.AddTransactionDialog;
import com.dzovah.mesha.Methods.Dialogs.EditAccountDialog;
import com.dzovah.mesha.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Activity for displaying detailed information about a Beta Account.
 * <p>
 * This activity provides:
 * <ul>
 *     <li>Detailed view of a specific Beta Account with its name, icon, and balance</li>
 *     <li>List of all transactions associated with this Beta Account</li>
 *     <li>Ability to add new transactions to this Beta Account</li>
 *     <li>Long-press functionality to edit or delete the Beta Account</li>
 *     <li>Real-time calculation and display of the Beta Account balance</li>
 * </ul>
 * The activity receives the Beta Account ID via intent extra and loads all
 * necessary data from the database to populate the UI. Transactions are displayed
 * in a RecyclerView sorted by entry time.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see AlphaAccountDetailActivity
 * @see BetaAccount
 * @see Transaction
 * @see TransactionAdapter
 * @see EditAccountDialog
 * @see AddTransactionDialog
 */
public class BetaAccountDetailActivity extends AppCompatActivity {
    /** Database instance for accessing app data */
    private MeshaDatabase database;
    
    /** Adapter for displaying transactions in the RecyclerView */
    private TransactionAdapter transactionAdapter;
    
    /** ID of the Beta Account being displayed */
    private int betaAccountId;
    
    /** Current Beta Account object being displayed */
    private BetaAccount currentBetaAccount;

    /**
     * Initializes the activity, sets up UI components, and loads Beta Account data.
     * <p>
     * This method retrieves the Beta Account ID from the intent, initializes views,
     * and triggers the loading of Beta Account details and associated transactions.
     * If the Beta Account ID is invalid, the activity finishes with an error message.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beta_account_detail);
        
        betaAccountId = getIntent().getIntExtra("beta_account_id", -1);
        if (betaAccountId == -1) {
            Toast.makeText(this, "Error loading account", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = MeshaDatabase.Get_database(getApplicationContext());
        initializeViews();
        loadBetaAccountDetails();
        loadTransactions();
    }

    /**
     * Initializes and sets up all UI components of the detail screen.
     * <p>
     * This includes setting up:
     * <ul>
     *     <li>Animations</li>
     *     <li>RecyclerView for transactions</li>
     *     <li>Floating action button for adding new transactions</li>
     * </ul>
     * </p>
     */
    private void initializeViews() {
        LottieAnimationView glowiAnimation = findViewById(R.id.glowi);
        glowiAnimation.playAnimation();

        RecyclerView rvTransactions = findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(this);
        rvTransactions.setAdapter(transactionAdapter);

        FloatingActionButton fabAddTransaction = findViewById(R.id.fabAddTransaction);
        fabAddTransaction.setOnClickListener(v -> showAddTransactionDialog());
    }

    /**
     * Loads and displays the Beta Account details from the database.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves the Beta Account data from the database</li>
     *     <li>Updates the UI with account name, balance, and icon</li>
     *     <li>Sets up long-click listener for editing and deleting the account</li>
     *     <li>Configures the transaction adapter with account information</li>
     * </ul>
     * The operations are performed on a background thread to avoid blocking the UI.
     * </p>
     */
    private void loadBetaAccountDetails() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                currentBetaAccount = database.betaAccountDao().getBetaAccountById(betaAccountId);
                runOnUiThread(() -> {
                    if (currentBetaAccount != null) {
                        TextView tvBetaName = findViewById(R.id.tvBetaAccountName);
                        TextView tvBetaBalance = findViewById(R.id.tvBetaAccountBalance);
                        ImageView ivBetaIcon = findViewById(R.id.betaAccountIcon);
                        LottieAnimationView glowiView = findViewById(R.id.glowi);
    
                        tvBetaName.setText(currentBetaAccount.getBetaAccountName());
                        tvBetaBalance.setText(CurrencyFormatter.format(currentBetaAccount.getBetaAccountBalance()));
                        transactionAdapter.setBetaAccountIcon(currentBetaAccount.getBetaAccountIcon());
                        transactionAdapter.setBetaAccount(currentBetaAccount);
    
                        try {
                            String iconPath = currentBetaAccount.getBetaAccountIcon().replace("Assets/", "");
                            InputStream is = getAssets().open(iconPath);
                            ivBetaIcon.setImageBitmap(BitmapFactory.decodeStream(is));
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
    
                        // Set long-click listener on the glowi view instead of just the name
                        glowiView.setOnLongClickListener(v -> {
                            EditAccountDialog dialog = new EditAccountDialog(this, database, currentBetaAccount);
                            dialog.setOnAccountEditedListener(new EditAccountDialog.OnAccountEditedListener() {
                                @Override
                                public void onAccountEdited() {
                                    loadBetaAccountDetails();
                                    loadTransactions();
                                }
                                
                                @Override
                                public void onAccountDeleted() {
                                    finish();
                                }
                            });
                            dialog.show();
                            return true;
                        });
                    } else {
                        Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
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
     * Loads and displays transactions associated with this Beta Account.
     * <p>
     * This method retrieves all transactions linked to the current Beta Account
     * from the database and updates the RecyclerView through the adapter.
     * The operations are performed on a background thread to avoid blocking the UI.
     * </p>
     */
    private void loadTransactions() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Transaction> transactions = database.transactionDao().getAllTransactionsByBetaAccountId(betaAccountId);
                runOnUiThread(() -> transactionAdapter.setTransactions(transactions));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error loading transactions", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Displays the dialog for adding a new transaction.
     * <p>
     * This method opens a dialog that allows the user to create a new transaction
     * for the current Beta Account. After successful creation, it refreshes
     * the account details and transaction list to reflect the new transaction.
     * Also sets the activity result to OK, notifying parent activities that data
     * has changed.
     * </p>
     */
    private void showAddTransactionDialog() {
        AddTransactionDialog dialog = new AddTransactionDialog(this, database, currentBetaAccount);
        dialog.setOnTransactionAddedListener(() -> {
            loadBetaAccountDetails();
            loadTransactions();
            setResult(RESULT_OK);
        });
        dialog.show();
    }
} 