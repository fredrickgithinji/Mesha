package com.dzovah.mesha.Database.Daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.dzovah.mesha.Database.Entities.Category;

import java.util.List;

/**
 * Data Access Object (DAO) interface for the Category entity.
 * <p>
 * This interface defines the database operations available for transaction categories,
 * including CRUD operations (Create, Read, Update, Delete) and specialized queries.
 * Room auto-generates the implementation of this interface at compile time.
 * </p>
 * <p>
 * Categories are used to classify transactions for analysis and reporting purposes,
 * helping users track their spending patterns and organize their financial data.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Category
 * @see com.dzovah.mesha.Database.Entities.Transaction
 */
@Dao
public interface CategoryDao {

    /**
     * Inserts a new Category into the database.
     * <p>
     * Room auto-generates the SQL INSERT statement and handles the primary key generation.
     * This method is used to create both default system categories and user-defined categories.
     * </p>
     *
     * @param category The Category object to insert
     */
    @Insert
    void insert(Category category);

    /**
     * Updates an existing Category in the database.
     * <p>
     * Room identifies the category to update based on the primary key of the provided object
     * and auto-generates the SQL UPDATE statement. This is used when a category's name or
     * description needs to be modified.
     * </p>
     *
     * @param category The Category object with updated values
     */
    @Update
    void update(Category category);

    /**
     * Deletes a Category from the database.
     * <p>
     * Room identifies the category to delete based on the primary key of the provided object
     * and auto-generates the SQL DELETE statement. Due to the CASCADE relationship defined
     * in the database schema, any transactions using this category should be reassigned or
     * deleted before removing the category.
     * </p>
     *
     * @param category The Category object to delete
     */
    @Delete
    void delete(Category category);

    /**
     * Retrieves all Categories from the database, ordered by ID.
     * <p>
     * This query returns a list of all available transaction categories sorted by their ID,
     * which is useful for displaying categories in category management screens or
     * providing a list of options when categorizing transactions.
     * </p>
     *
     * @return A list of all Category objects ordered by ID
     */
    @Query("SELECT * FROM Categories ORDER BY Category_id ASC")
    List<Category> getAllCategories();

    /**
     * Retrieves a specific Category by its ID.
     * <p>
     * This query is used when detailed information about a specific category is needed,
     * such as when editing an existing category or when retrieving category information
     * for a transaction.
     * </p>
     *
     * @param id The ID of the Category to retrieve
     * @return The Category object with the specified ID, or null if not found
     */
    @Query("SELECT * FROM Categories WHERE Category_id = :id LIMIT 1")
    Category getCategoryById(int id);
}
