package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.dzovah.mesha.Database.Entities.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    void insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM Categories ORDER BY Category_id ASC")
    List<Category> getAllCategories();

    @Query("SELECT * FROM Categories WHERE Category_id = :id LIMIT 1")
    Category getCategoryById(int id);
}
