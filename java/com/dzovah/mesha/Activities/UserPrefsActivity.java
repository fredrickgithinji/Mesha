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

public class UserPrefsActivity extends AppCompatActivity {

    private MeshaDatabase database;
    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private Spinner currencySpinner;

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

    private void setupCategoriesSection() {
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter();
        categoriesRecyclerView.setAdapter(categoryAdapter);
        loadCategories();
    }

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

    private void loadCategories() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = database.categoryDao().getAllCategories();
            runOnUiThread(() -> categoryAdapter.setCategories(categories));
        });
    }

    private void showAddCategoryDialog() {
        // Navigate to CategoryManagementActivity
        Intent intent = new Intent(this, CategoryManagementActivity.class);
        startActivity(intent);
    }

    private void saveCurrencyPreference() {
        CurrencyType selectedCurrency = (CurrencyType) currencySpinner.getSelectedItem();
        CurrencyFormatter.setCurrency(selectedCurrency);
        CurrencyFormatter.saveCurrencyPreference(this, selectedCurrency);
        Toast.makeText(this, "Currency preference saved", Toast.LENGTH_SHORT).show();
    }
} 