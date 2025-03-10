package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Methods.Utils.Quotes;
import com.dzovah.mesha.Methods.Dialogs.CreateAccountDialog;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.dzovah.mesha.Activities.Adapters.AlphaAccountAdapter;

import java.util.List;

public class Dashboard extends AppCompatActivity {
    private MeshaDatabase database;
    private AlphaAccountAdapter accountAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);
        
        database = MeshaDatabase.Get_database(getApplicationContext());

        initializeViews();
        loadAccounts();
    }

    private void initializeViews() {
        TextView tvQuoteOfTheDay = findViewById(R.id.tvQuoteOfTheDay);
        LottieAnimationView bubblesAnimationView = findViewById(R.id.bubbles);
        LottieAnimationView piechartAnimationView = findViewById(R.id.piechart);
        FloatingActionButton fabAddAccount = findViewById(R.id.fabAddAccount);

        bubblesAnimationView.playAnimation();
        piechartAnimationView.playAnimation();

        Quotes quotes = new Quotes();
        tvQuoteOfTheDay.setText("Quote: " + quotes.presentQuote());

        RecyclerView rvAccounts = findViewById(R.id.rvAccounts);
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        accountAdapter = new AlphaAccountAdapter(this);
        accountAdapter.setOnAccountActionListener(new AlphaAccountAdapter.OnAccountActionListener() {
            @Override
            public void onAccountClicked(AlphaAccount account, int position) {
                Intent intent = new Intent(Dashboard.this, AlphaAccountDetailActivity.class);
                intent.putExtra("alpha_account_id", account.getAlphaAccountId());
                startActivity(intent);
            }

            @Override
            public void onAccountUpdated() {
                loadAccounts();
            }
        });
        rvAccounts.setAdapter(accountAdapter);

        fabAddAccount.setOnClickListener(v -> showCreateAccountDialog());
    }

    private void loadAccounts() {
        try {
            MeshaDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    List<AlphaAccount> accounts = database.alphaAccountDao().getAllAlphaAccounts();
                    runOnUiThread(() -> {
                        try {
                            accountAdapter.setAccounts(accounts);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error displaying accounts", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Error loading accounts", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to access database", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreateAccountDialog() {
        CreateAccountDialog dialog = new CreateAccountDialog(this, database);
        dialog.setOnAccountCreatedListener(account -> {
            try {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
                loadAccounts(); // Reload accounts after creation
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error updating account list", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show(); // Show the dialog
    }
}