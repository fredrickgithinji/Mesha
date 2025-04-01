package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.dzovah.mesha.Database.Entities.Meshans;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Methods.Utils.Quotes;
import com.dzovah.mesha.Methods.Dialogs.CreateAccountDialog;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.dzovah.mesha.Activities.Adapters.AlphaAccountAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import java.util.List;

import com.bumptech.glide.Glide;
import com.dzovah.mesha.Methods.Utils.LocalStorageUtil;

public class Dashboard extends AppCompatActivity {
    private MeshaDatabase database;
    private AlphaAccountAdapter accountAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton;
    private Menu navMenu;
    private ImageView profileImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuButton = findViewById(R.id.menuButton);
        profileImageView = findViewById(R.id.profileImageView);
        LottieAnimationView piechart = findViewById(R.id.piechart);

        // Get the navigation menu immediately
        navMenu = navigationView.getMenu();

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
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        piechart.setOnClickListener(v -> {
            // Navigate to AnalysisActivity when clicked
            Intent intent = new Intent(Dashboard.this, AnalysisActivity.class);
            startActivity(intent);
        });

        // Set up navigation item selection
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_account) {
                startActivity(new Intent(Dashboard.this, AccountsSection.class));
            }
            else if (id == R.id.nav_userpref) {
                startActivity(new Intent(Dashboard.this, UserPrefsActivity.class));
            }
            else if (id == R.id.nav_signin) {
                startActivity(new Intent(Dashboard.this, SignInActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(Dashboard.this, SignInActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        database = MeshaDatabase.Get_database(getApplicationContext());
        initializeViews();
        loadAccounts();
        checkUserAndUpdateMenu();
        loadProfileImage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccounts();
        checkUserAndUpdateMenu();
        loadProfileImage();
    }

    private void checkUserAndUpdateMenu() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateNavigationForSignedInUser(true);
            observePremiumStatusAndUpdateMenu(currentUser.getUid());
        } else {
            updateNavigationForSignedInUser(false);
            MenuItem accountMenuItem = navMenu.findItem(R.id.nav_account);
            if (accountMenuItem != null) {
                accountMenuItem.setIcon(R.drawable.ic_normal_account);
            }
        }
    }

    private void updateNavigationForSignedInUser(boolean isSignedIn) {
        MenuItem signInItem = navMenu.findItem(R.id.nav_signin);
        MenuItem logoutItem = navMenu.findItem(R.id.nav_logout);

        if (signInItem != null && logoutItem != null) {
            signInItem.setVisible(!isSignedIn);
            logoutItem.setVisible(isSignedIn);
        }
    }

    private void observePremiumStatusAndUpdateMenu(String userId) {
        LiveData<Meshans> userLiveData = database.meshansDao().get(userId);
        userLiveData.observe(this, user -> {
            if (user != null) {
                boolean isPremium = user.isPremium();
                updateMenuIcon(isPremium);
            } else {
                updateMenuIcon(false);
            }
        });
    }

    private void updateMenuIcon(boolean isPremium) {
        runOnUiThread(() -> {
            MenuItem accountMenuItem = navMenu.findItem(R.id.nav_account);
            if (accountMenuItem != null) {
                if (isPremium) {
                    accountMenuItem.setIcon(R.drawable.ic_premium_account);
                } else {
                    accountMenuItem.setIcon(R.drawable.ic_normal_account);
                }
                invalidateOptionsMenu();
            } else {
            }
        });
    }

    private void initializeViews() {
        TextView tvQuoteOfTheDay = findViewById(R.id.tvQuoteOfTheDay);
        LottieAnimationView bubblesAnimationView = findViewById(R.id.bubbles);
        LottieAnimationView piechartAnimationView = findViewById(R.id.piechart);
        FloatingActionButton fabAddAccount = findViewById(R.id.fabAddAccount);
        bubblesAnimationView.playAnimation();
        piechartAnimationView.playAnimation();
        Quotes quotes = new Quotes();
        tvQuoteOfTheDay.setText(quotes.presentQuote());
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
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<AlphaAccount> accounts = database.alphaAccountDao().getAllAlphaAccounts();
                for (AlphaAccount account : accounts) {
                    double calculatedBalance = database.transactionDao().getAlphaAccountBalanceById(account.getAlphaAccountId());
                    account.setAlphaAccountBalance(calculatedBalance); // Or use setCalculatedBalance if you implemented it
                }
                runOnUiThread(() -> {
                    accountAdapter.setAccounts(accounts);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, 
                    "Error loading accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
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
        dialog.show(); 
    }

    private void loadProfileImage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String fileName = userId + "_profile.png";
            Bitmap cachedImage = LocalStorageUtil.loadImage(this, fileName);

            if (cachedImage != null) {
                profileImageView.setImageBitmap(cachedImage);
            } else {
                profileImageView.setImageResource(R.drawable.icon_mesha);
            }
            
            profileImageView.setOnClickListener(v -> {
                startActivity(new Intent(Dashboard.this, AccountsSection.class));
            });
        } else {
         
            profileImageView.setImageResource(R.drawable.icon_mesha);
            profileImageView.setOnClickListener(v -> {
                startActivity(new Intent(Dashboard.this, SignInActivity.class));
            });
        }
    }
}