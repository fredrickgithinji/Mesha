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

public class AlphaAccountDetailActivity extends AppCompatActivity {
    private MeshaDatabase database;
    private BetaAccountAdapter betaAccountAdapter;
    private int alphaAccountId;

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

    private void initializeViews() {
        LottieAnimationView bubblesAnimationView = findViewById(R.id.bubbles);
        LottieAnimationView piechartAnimationView = findViewById(R.id.piechart);
        FloatingActionButton fabAddBeta = findViewById(R.id.fabAddBetaAccount);

        bubblesAnimationView.playAnimation();
        piechartAnimationView.playAnimation();
        


        RecyclerView rvBetaAccounts = findViewById(R.id.rvBetaAccounts);
        rvBetaAccounts.setLayoutManager(new LinearLayoutManager(this));
        betaAccountAdapter = new BetaAccountAdapter(this);
        rvBetaAccounts.setAdapter(betaAccountAdapter);

        fabAddBeta.setOnClickListener(v -> showCreateBetaDialog());
    }

    private void loadAlphaAccountDetails() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AlphaAccount account = database.alphaAccountDao().getAlphaAccountById(alphaAccountId);
                runOnUiThread(() -> {
                    if (account != null) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadAlphaAccountDetails();
            loadBetaAccounts();
        }
    }
}