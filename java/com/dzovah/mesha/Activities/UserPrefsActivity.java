package com.dzovah.mesha.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyType;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Activities.Adapters.CategoryAdapter;
import com.dzovah.mesha.Activities.Adapters.CurrencySpinnerAdapter;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Activity for managing user preferences and application settings.
 * <p>
 * This activity provides interfaces for:
 * <ul>
 *     <li>Setting up PIN protection</li>
 *     <li>Setting up fingerprint authentication</li>
 *     <li>Managing transaction categories</li>
 *     <li>Selecting preferred currency for displaying transaction amounts</li>
 *     <li>Configuring other application-wide settings</li>
 * </ul>
 * The preferences set in this activity affect how financial data is displayed
 * and how the app's security features operate.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see CategoryManagementActivity
 * @see CurrencyFormatter
 * @see CurrencyType
 * @see CategoryAdapter
 */
public class UserPrefsActivity extends AppCompatActivity {
    /** Shared preferences name for security settings */
    private static final String SECURITY_PREFS = "MeshaSecurity";
    
    /** Key for storing the PIN in shared preferences */
    private static final String PIN_KEY = "pin_code";
    
    /** Key for storing fingerprint setting in shared preferences */
    private static final String FINGERPRINT_ENABLED_KEY = "fingerprint_enabled";

    /** Database instance for accessing app data */
    private MeshaDatabase database;
    
    /** RecyclerView for displaying available categories */
    private RecyclerView categoriesRecyclerView;
    
    /** Adapter for displaying categories in the RecyclerView */
    private CategoryAdapter categoryAdapter;
    
    /** Spinner for selecting preferred currency */
    private Spinner currencySpinner;
    
    /** Layout container for PIN setup UI elements */
    private LinearLayout pinSetupContainer;
    
    /** Layout container for PIN status UI elements */
    private LinearLayout pinStatusContainer;
    
    /** EditText for entering the PIN */
    private EditText etPin;
    
    /** EditText for confirming the PIN */
    private EditText etConfirmPin;
    
    /** TextView showing PIN status */
    private TextView tvPinStatus;
    
    /** TextView showing fingerprint status */
    private TextView tvFingerprintStatus;
    
    /** Switch for enabling/disabling fingerprint authentication */
    private Switch switchFingerprint;
    
    /** Shared preferences for security settings */
    private SharedPreferences securityPrefs;
    
    /** Biometric manager for fingerprint operations */
    private BiometricManager biometricManager;
    
    /** Executor for biometric operations */
    private Executor executor;

    /**
     * Initializes the activity, sets up UI components, and loads saved preferences.
     * <p>
     * This method:
     * <ul>
     *     <li>Loads the current security preferences</li>
     *     <li>Loads the current currency preference</li>
     *     <li>Initializes the database connection</li>
     *     <li>Sets up the security sections (PIN and fingerprint)</li>
     *     <li>Sets up the categories section</li>
     *     <li>Sets up the currency selection section</li>
     *     <li>Configures button listeners for saving preferences</li>
     * </ul>
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_prefs);

        // Initialize security preferences
        securityPrefs = getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE);

        // Initialize biometric components
        executor = ContextCompat.getMainExecutor(this);
        biometricManager = BiometricManager.from(this);

        // Load saved currency preference
        CurrencyFormatter.loadCurrencyPreference(this);

        // Initialize database
        database = MeshaDatabase.Get_database(this);

        // Initialize views
        initializeViews();

        // Setup sections
        setupPinSection();
        setupFingerprintSection();
        setupCategoriesSection();
        setupCurrencySection();
    }

    /**
     * Initializes all the views and UI components.
     */
    private void initializeViews() {
        // Initialize security views
        pinSetupContainer = findViewById(R.id.pinSetupContainer);
        pinStatusContainer = findViewById(R.id.pinStatusContainer);
        etPin = findViewById(R.id.etPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        tvPinStatus = findViewById(R.id.tvPinStatus);
        tvFingerprintStatus = findViewById(R.id.tvFingerprintStatus);
        switchFingerprint = findViewById(R.id.switchFingerprint);
        
        Button btnSetPin = findViewById(R.id.btnSetPin);
        Button btnChangePin = findViewById(R.id.btnChangePin);
        Button btnRemovePin = findViewById(R.id.btnRemovePin);
        
        // Set click listeners for PIN buttons
        btnSetPin.setOnClickListener(v -> savePin());
        btnChangePin.setOnClickListener(v -> showPinSetupUI());
        btnRemovePin.setOnClickListener(v -> removePin());
        
        // Initialize category and currency views
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        currencySpinner = findViewById(R.id.currencySpinner);
        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        Button btnSaveCurrency = findViewById(R.id.btnSaveCurrency);
        
        // Set click listeners for currency and category buttons
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        btnSaveCurrency.setOnClickListener(v -> saveCurrencyPreference());
        
        // Initialize fingerprint switch
        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> toggleFingerprint(isChecked));
    }

    /**
     * Sets up the PIN section of the preferences screen.
     * <p>
     * This method:
     * <ul>
     *     <li>Checks if a PIN is already set</li>
     *     <li>Shows either the PIN setup UI or the PIN status UI</li>
     * </ul>
     * </p>
     */
    private void setupPinSection() {
        // Check if PIN is already set
        String savedPin = securityPrefs.getString(PIN_KEY, null);
        if (savedPin != null && !savedPin.isEmpty()) {
            // PIN is set, show status UI
            pinSetupContainer.setVisibility(View.GONE);
            pinStatusContainer.setVisibility(View.VISIBLE);
            tvPinStatus.setText("PIN protection is enabled");
        } else {
            // PIN is not set, show setup UI
            pinSetupContainer.setVisibility(View.VISIBLE);
            pinStatusContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up the fingerprint section of the preferences screen.
     * <p>
     * This method:
     * <ul>
     *     <li>Checks if fingerprint hardware is available</li>
     *     <li>Checks if fingerprint authentication is enabled</li>
     *     <li>Updates the UI to reflect the current status</li>
     * </ul>
     * </p>
     */
    private void setupFingerprintSection() {
        // Check if fingerprint hardware is available
        boolean isFingerprintAvailable = checkFingerprintAvailability();
        boolean isEnabled = securityPrefs.getBoolean(FINGERPRINT_ENABLED_KEY, false);
        
        if (isFingerprintAvailable) {
            // Update UI
            switchFingerprint.setEnabled(true);
            switchFingerprint.setChecked(isEnabled);
            tvFingerprintStatus.setText(isEnabled ? 
                    "Fingerprint authentication is enabled" : 
                    "Fingerprint authentication is not enabled");
        } else {
            // Fingerprint not available
            switchFingerprint.setEnabled(false);
            switchFingerprint.setChecked(false);
            tvFingerprintStatus.setText("Fingerprint authentication is not available on this device");
        }
    }

    /**
     * Checks if fingerprint authentication is available on the device.
     *
     * @return true if fingerprint authentication is available, false otherwise
     */
    private boolean checkFingerprintAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) 
                    == BiometricManager.BIOMETRIC_SUCCESS;
        }
        return false;
    }

    /**
     * Saves the PIN entered by the user.
     * <p>
     * This method:
     * <ul>
     *     <li>Validates the PIN input</li>
     *     <li>Saves the PIN to shared preferences if valid</li>
     *     <li>Updates the UI to show the PIN status</li>
     * </ul>
     * </p>
     */
    private void savePin() {
        String pin = etPin.getText().toString();
        String confirmPin = etConfirmPin.getText().toString();
        
        // Validate PIN
        if (TextUtils.isEmpty(pin) || pin.length() != 4) {
            Toast.makeText(this, "PIN must be exactly 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if PINs match
        if (!pin.equals(confirmPin)) {
            Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save PIN
        securityPrefs.edit().putString(PIN_KEY, pin).apply();
        
        // Update UI
        pinSetupContainer.setVisibility(View.GONE);
        pinStatusContainer.setVisibility(View.VISIBLE);
        tvPinStatus.setText("PIN protection is enabled");
        
        // Clear fields
        etPin.setText("");
        etConfirmPin.setText("");
        
        Toast.makeText(this, "PIN successfully set", Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows the PIN setup UI to allow the user to set or change the PIN.
     */
    private void showPinSetupUI() {
        pinSetupContainer.setVisibility(View.VISIBLE);
        pinStatusContainer.setVisibility(View.GONE);
        etPin.setText("");
        etConfirmPin.setText("");
    }

    /**
     * Removes the PIN protection.
     * <p>
     * This method:
     * <ul>
     *     <li>Removes the PIN from shared preferences</li>
     *     <li>Updates the UI to show the PIN setup form</li>
     * </ul>
     * </p>
     */
    private void removePin() {
        // Remove PIN
        securityPrefs.edit().remove(PIN_KEY).apply();
        
        // Update UI
        pinSetupContainer.setVisibility(View.VISIBLE);
        pinStatusContainer.setVisibility(View.GONE);
        
        // If fingerprint is enabled, disable it as it requires PIN
        if (securityPrefs.getBoolean(FINGERPRINT_ENABLED_KEY, false)) {
            securityPrefs.edit().putBoolean(FINGERPRINT_ENABLED_KEY, false).apply();
            switchFingerprint.setChecked(false);
            tvFingerprintStatus.setText("Fingerprint authentication is not enabled");
        }
        
        Toast.makeText(this, "PIN protection removed", Toast.LENGTH_SHORT).show();
    }

    /**
     * Toggles fingerprint authentication on or off.
     * <p>
     * This method:
     * <ul>
     *     <li>Checks if a PIN is set (required for fingerprint)</li>
     *     <li>Enables or disables fingerprint authentication</li>
     *     <li>Updates the UI to reflect the current status</li>
     * </ul>
     * </p>
     *
     * @param isEnabled true to enable fingerprint authentication, false to disable it
     */
    private void toggleFingerprint(boolean isEnabled) {
        // Check if PIN is set (required for fingerprint authentication)
        String savedPin = securityPrefs.getString(PIN_KEY, null);
        if (isEnabled && (savedPin == null || savedPin.isEmpty())) {
            Toast.makeText(this, "You must set a PIN before enabling fingerprint authentication", Toast.LENGTH_LONG).show();
            switchFingerprint.setChecked(false);
            return;
        }
        
        // Save setting
        securityPrefs.edit().putBoolean(FINGERPRINT_ENABLED_KEY, isEnabled).apply();
        
        // Update UI
        tvFingerprintStatus.setText(isEnabled ? 
                "Fingerprint authentication is enabled" : 
                "Fingerprint authentication is not enabled");
        
        Toast.makeText(this, isEnabled ? 
                "Fingerprint authentication enabled" : 
                "Fingerprint authentication disabled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Initializes the categories section of the preferences screen.
     * <p>
     * This method sets up the RecyclerView with a CategoryAdapter and
     * loads the list of available categories from the database.
     * </p>
     */
    private void setupCategoriesSection() {
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter();
        categoriesRecyclerView.setAdapter(categoryAdapter);
        loadCategories();
    }

    /**
     * Initializes the currency selection section of the preferences screen.
     * <p>
     * This method:
     * <ul>
     *     <li>Creates a CurrencySpinnerAdapter with all available currency types</li>
     *     <li>Sets the adapter on the currency spinner</li>
     *     <li>Selects the current currency preference in the spinner</li>
     * </ul>
     * </p>
     */
    private void setupCurrencySection() {
        CurrencySpinnerAdapter adapter = new CurrencySpinnerAdapter(this, CurrencyType.values());
        currencySpinner.setAdapter(adapter);
        
        // Set current selection
        CurrencyType currentCurrency = CurrencyFormatter.getCurrentCurrency();
        for (int i = 0; i < CurrencyType.values().length; i++) {
            if (CurrencyType.values()[i] == currentCurrency) {
                currencySpinner.setSelection(i);
                break;
            }
        }
    }

    /**
     * Loads all categories from the database and updates the UI.
     * <p>
     * This method retrieves all categories from the database using a background thread
     * and then updates the RecyclerView through the adapter on the UI thread.
     * </p>
     */
    private void loadCategories() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = database.categoryDao().getAllCategories();
            runOnUiThread(() -> categoryAdapter.setCategories(categories));
        });
    }

    /**
     * Opens the category management screen.
     * <p>
     * This method launches the CategoryManagementActivity which provides
     * a full interface for adding, editing, and managing transaction categories.
     * </p>
     */
    private void showAddCategoryDialog() {
        // Navigate to CategoryManagementActivity
        Intent intent = new Intent(this, CategoryManagementActivity.class);
        startActivity(intent);
    }

    /**
     * Saves the selected currency preference.
     * <p>
     * This method:
     * <ul>
     *     <li>Gets the selected currency from the spinner</li>
     *     <li>Updates the CurrencyFormatter with the new selection</li>
     *     <li>Persists the preference to SharedPreferences for future app launches</li>
     *     <li>Displays a confirmation message to the user</li>
     * </ul>
     * The saved preference will be used throughout the app for formatting currency values.
     * </p>
     */
    private void saveCurrencyPreference() {
        CurrencyType selectedCurrency = (CurrencyType) currencySpinner.getSelectedItem();
        CurrencyFormatter.setCurrency(selectedCurrency);
        CurrencyFormatter.saveCurrencyPreference(this, selectedCurrency);
        Toast.makeText(this, "Currency preference saved", Toast.LENGTH_SHORT).show();
    }
}