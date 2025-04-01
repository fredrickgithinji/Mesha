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

/**
 * Main dashboard activity of the Mesha application.
 * <p>
 * This activity serves as the home screen and provides:
 * <ul>
 *     <li>Overview of user's alpha accounts and balances</li>
 *     <li>Navigation drawer with access to various sections of the app</li>
 *     <li>Account creation functionality</li>
 *     <li>Quick access to account details</li>
 *     <li>Profile image and premium status display</li>
 *     <li>Inspirational quote display</li>
 *     <li>Access to financial analysis</li>
 * </ul>
 * The dashboard adapts its UI based on whether the user is signed in
 * and whether they have premium status.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see AccountsSection
 * @see AlphaAccountDetailActivity
 * @see AnalysisActivity
 * @see UserPrefsActivity
 */
public class Dashboard extends AppCompatActivity {
    /** Database instance for accessing app data */
    private MeshaDatabase database;
    
    /** Adapter for displaying alpha accounts in the RecyclerView */
    private AlphaAccountAdapter accountAdapter;
    
    /** DrawerLayout for the navigation drawer */
    private DrawerLayout drawerLayout;
    
    /** NavigationView containing the navigation menu items */
    private NavigationView navigationView;
    
    /** Button to open the navigation drawer */
    private ImageButton menuButton;
    
    /** Navigation menu reference for dynamic updates */
    private Menu navMenu;
    
    /** ImageView for displaying the user's profile picture */
    private ImageView profileImageView;

    /**
     * Initializes the dashboard activity, sets up the UI components, 
     * and loads required data.
     * <p>
     * This method initializes the navigation drawer, animations, account list,
     * and checks the user's authentication state to update the UI accordingly.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
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

        // Initialize database and UI on a background thread
        initializeAsync();
    }

    /**
     * Initializes database and loads initial data asynchronously
     * to avoid blocking the main thread.
     */
    private void initializeAsync() {
        
        // Initialize database and load data on a background thread
        new Thread(() -> {
            database = MeshaDatabase.Get_database(getApplicationContext());
            
            // Run UI initialization on the main thread
            runOnUiThread(() -> {
                initializeViews();
                checkUserAndUpdateMenu();
                // Load accounts will handle hiding the loading state
                loadAccounts();
                loadProfileImage();
            });
        }).start();
    }
    
    /**
     * Shows or hides a loading state in the UI.
     * 
     * @param show True to show loading state, false to hide it
     */

    /**
     * Refreshes account data, authentication state, and profile image
     * when the activity is resumed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Check if database is initialized before loading data
        if (database != null) {
            loadAccounts();
            checkUserAndUpdateMenu();
            loadProfileImage();
        } else {
            // Re-initialize if database is null
            initializeAsync();
        }
    }

    /**
     * Loads alpha accounts from the database and updates the recycler view.
     * <p>
     * This method performs database operations on a background thread and
     * updates the UI on the main thread when data is available.
     * </p>
     */
    private void loadAccounts() {
        
        // Execute database query on a background thread
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<AlphaAccount> accounts = database.alphaAccountDao().getAllAlphaAccounts();
                // Update UI on the main thread
                runOnUiThread(() -> {
                    accountAdapter.setAccounts(accounts);
                    
                    // Update empty state visibility
                   /* View emptyView = findViewById(R.id.emptyStateLayout);
                    if (emptyView != null) {
                        emptyView.setVisibility(accounts.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    */

                });
            } catch (Exception e) {
                Log.e("Dashboard", "Error loading accounts", e);
                runOnUiThread(() -> {
                    Toast.makeText(Dashboard.this, "Failed to load accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Loads the user's profile image efficiently using Glide.
     * <p>
     * Prioritizes cached images and loads from network only when necessary.
     * All image processing occurs off the main thread.
     * </p>
     */
    private void loadProfileImage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Set default image if user is not logged in
            Glide.with(this)
                .load(R.drawable.icon_mesha)
                .into(profileImageView);
            return;
        }

        // Try to load from cache first
        Bitmap cachedImage = LocalStorageUtil.loadImage(this, currentUser.getUid() + "_profile.png");
        if (cachedImage != null) {
            profileImageView.setImageBitmap(cachedImage);
            return;
        }

        // Check if user has a profile URL stored in Firebase
        database.meshansDao().get(currentUser.getUid()).observe(this, user -> {
            if (user != null && user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().equals("to edit")) {
                // Load from network using Glide (which handles threading automatically)
                Glide.with(this)
                    .load(user.getProfilePictureUrl())
                    .placeholder(R.drawable.icon_mesha)
                    .error(R.drawable.icon_mesha)
                    .into(profileImageView);
            } else {
                // Set default image
                profileImageView.setImageResource(R.drawable.icon_mesha);
            }
        });
    }

    /**
     * Initializes and sets up all UI components of the dashboard.
     * <p>
     * This includes setting up:
     * <ul>
     *     <li>Animations</li>
     *     <li>Quote of the day</li>
     *     <li>RecyclerView for accounts</li>
     *     <li>Adapter and click listeners</li>
     *     <li>Add account button</li>
     * </ul>
     * </p>
     */
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

    /**
     * Displays the dialog for creating a new alpha account.
     * <p>
     * When an account is successfully created, the account list is refreshed.
     * </p>
     */
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

    /**
     * Checks the current user authentication state and updates the navigation menu accordingly.
     * <p>
     * If a user is signed in, the sign-in option is hidden and the logout option is shown.
     * For premium users, a special account icon is displayed.
     * </p>
     */
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

    /**
     * Updates the navigation drawer menu based on whether the user is signed in.
     * <p>
     * Shows or hides the sign-in and logout options based on authentication state.
     * </p>
     *
     * @param isSignedIn Whether the user is currently signed in
     */
    private void updateNavigationForSignedInUser(boolean isSignedIn) {
        MenuItem signInItem = navMenu.findItem(R.id.nav_signin);
        MenuItem logoutItem = navMenu.findItem(R.id.nav_logout);

        if (signInItem != null && logoutItem != null) {
            signInItem.setVisible(!isSignedIn);
            logoutItem.setVisible(isSignedIn);
        }
    }

    /**
     * Observes the user's premium status from the database and updates the UI accordingly.
     * <p>
     * Uses LiveData to react to changes in the user's premium status and update
     * the account icon in the navigation menu.
     * </p>
     *
     * @param userId The ID of the current user
     */
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

    /**
     * Updates the account icon in the navigation menu based on premium status.
     * <p>
     * Premium users get a special premium icon, while regular users get the standard icon.
     * </p>
     *
     * @param isPremium Whether the user has premium status
     */
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
}