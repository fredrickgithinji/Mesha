package com.dzovah.mesha.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Activities.Adapters.CategoryAdapter;
import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.R;

import java.util.List;

/**
 * Activity for managing transaction categories.
 * <p>
 * This activity provides functionality for:
 * <ul>
 *     <li>Viewing all existing transaction categories</li>
 *     <li>Adding new categories to the system</li>
 *     <li>Managing category organization</li>
 * </ul>
 * Categories are used throughout the application to categorize and organize
 * financial transactions, enabling better analysis and reporting of spending patterns.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Category
 * @see CategoryAdapter
 */
public class CategoryManagementActivity extends AppCompatActivity {

    /** Input field for entering new category names */
    private EditText categoryNameInput;
    
    /** Adapter for displaying categories in the RecyclerView */
    private CategoryAdapter categoryAdapter;
    
    /** Database instance for accessing app data */
    private MeshaDatabase database;

    /**
     * Initializes the activity, sets up UI components, and loads existing categories.
     * <p>
     * This method initializes the database connection, sets up the RecyclerView with
     * the CategoryAdapter, and configures the button click listener for adding new categories.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        // Initialize database
        database = MeshaDatabase.Get_database(this);

        // Initialize views
        categoryNameInput = findViewById(R.id.categoryNameInput);
        Button addCategoryButton = findViewById(R.id.addCategoryButton);
        RecyclerView categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);

        // Setup RecyclerView
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter();
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Load existing categories
        loadCategories();

        // Set up button click listener
        addCategoryButton.setOnClickListener(v -> addCategory());
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
     * Adds a new category to the database.
     * <p>
     * This method:
     * <ul>
     *     <li>Validates that the category name is not empty</li>
     *     <li>Creates a new Category entity with the provided name</li>
     *     <li>Inserts the category into the database using a background thread</li>
     *     <li>Updates the UI with success or error messages</li>
     *     <li>Reloads the category list to display the new category</li>
     * </ul>
     * </p>
     */
    private void addCategory() {
        String categoryName = categoryNameInput.getText().toString().trim();
        
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show();
            return;
        }

        Category newCategory = new Category();
        newCategory.setCategory(categoryName);

        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                database.categoryDao().insert(newCategory);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show();
                    categoryNameInput.setText("");
                    loadCategories();
                });
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error adding category: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}