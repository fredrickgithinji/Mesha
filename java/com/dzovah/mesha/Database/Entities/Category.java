package com.dzovah.mesha.Database.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
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

    @ColumnInfo(name = "description")
    private String description;

    // Primary constructor that Room will use
    public Category(@NonNull String category, String description) {
        this.category = category;
        this.description = description;
    }

    // Secondary constructor - mark with @Ignore so Room won't get confused
    @Ignore
    public Category(@NonNull String category) {
        this.category = category;
        this.description = "";
    }

    // Default empty constructor - also ignored
    @Ignore
    public Category() {
    }

    // Getters and Setters
    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
