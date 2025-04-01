package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

/**
 * Activity for managing user preferences and application settings.
 * <p>
 * This activity provides interfaces for:
 * <ul>
 *     <li>Managing transaction categories</li>
 *     <li>Selecting preferred currency for displaying transaction amounts</li>
 *     <li>Configuring other application-wide settings</li>
 * </ul>
 * The preferences set in this activity affect how financial data is displayed
 * throughout the entire application. Changes to currency settings are immediately
 * applied and persisted across app restarts.
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

    /** Database instance for accessing app data */
    private MeshaDatabase database;
    
    /** RecyclerView for displaying available categories */
    private RecyclerView categoriesRecyclerView;
    
    /** Adapter for displaying categories in the RecyclerView */
    private CategoryAdapter categoryAdapter;
    
    /** Spinner for selecting preferred currency */
    private Spinner currencySpinner;

    /**
     * Initializes the activity, sets up UI components, and loads saved preferences.
     * <p>
     * This method:
     * <ul>
     *     <li>Loads the current currency preference</li>
     *     <li>Initializes the database connection</li>
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

        // Load saved currency preference
        CurrencyFormatter.loadCurrencyPreference(this);

        // Initialize database
        database = MeshaDatabase.Get_database(this);

        // Initialize views
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        currencySpinner = findViewById(R.id.currencySpinner);
        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        Button btnSaveCurrency = findViewById(R.id.btnSaveCurrency);

        // Setup categories section
        setupCategoriesSection();

        // Setup currency section
        setupCurrencySection();

        // Set up button listeners
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        btnSaveCurrency.setOnClickListener(v -> saveCurrencyPreference());
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