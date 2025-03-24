package com.dzovah.mesha.Database.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "Categories")
public class Category {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Category_id")
    private int categoryId;

    @NonNull
    @ColumnInfo(name = "Category")
    private String category;

    @ColumnInfo(name = "Description")
    private String description;

    // Constructor
    public Category() {
        this.category = category;
        this.description = description;
    }

    // Getters and Setters
    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
