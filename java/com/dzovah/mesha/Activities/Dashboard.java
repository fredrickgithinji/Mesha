package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import java.util.List;

public class Dashboard extends AppCompatActivity {
    private MeshaDatabase database;
    private AlphaAccountAdapter accountAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton; // Add this line

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        FirebaseAppCheck.getInstance().getAppCheckToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult().getToken();
                        Log.d("AppCheckDebug", "Debug token: " + token);
                    } else {
                        Log.e("AppCheckDebug", "Error retrieving App Check token", task.getException());
                    }
                });

        // In your Application class or main Activity's onCreate
       /* FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(

                 PlayIntegrityAppCheckProviderFactory.getInstance()
        );
*/
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        /*if (currentUser == null) {
            startActivity(new Intent(Dashboard.this, SignInActivity.class));
            finish();
            return;
        }
*/
        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuButton = findViewById(R.id.menuButton); // Initialize the menu button

        // Set up the ActionBarDrawerToggle for the right-side drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set click listener for the menu button
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END); // Close the drawer if it's open
            } else {
                drawerLayout.openDrawer(GravityCompat.END); // Open the drawer if it's closed
            }
        });

        // Set up navigation item selection
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_account) {
                startActivity(new Intent(Dashboard.this, AccountsSection.class));
            } else if (id == R.id.nav_signin) {
                startActivity(new Intent(Dashboard.this, SignInActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(Dashboard.this, SignInActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.END); // Changed from START to END
            return true;
        });


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

        RecyclerView rvAccounts = findViewById(R.id.alpha_accounts_recyclerview);
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