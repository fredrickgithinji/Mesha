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

public class CategoryManagementActivity extends AppCompatActivity {

    private EditText categoryNameInput;
    private CategoryAdapter categoryAdapter;
    private MeshaDatabase database;

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

    private void loadCategories() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = database.categoryDao().getAllCategories();
            runOnUiThread(() -> categoryAdapter.setCategories(categories));
        });
    }

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