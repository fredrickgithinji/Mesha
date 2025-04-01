package com.dzovah.mesha.Database.Daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.Meshans;

/**
 * Data Access Object (DAO) interface for the Meshans entity.
 * <p>
 * This interface defines the database operations available for user accounts in the Mesha application,
 * including insertion, retrieval, and deletion operations. Room auto-generates the implementation
 * of this interface at compile time.
 * </p>
 * <p>
 * The Meshans entity represents user information synchronized with Firebase Authentication,
 * storing essential user data like profile details and subscription status.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Meshans
 */
@Dao
public interface MeshansDao {

    /**
     * Inserts or updates a user in the database.
     * <p>
     * This method uses OnConflictStrategy.REPLACE, which means if a user with the same
     * primary key (userId) already exists, the existing record will be replaced with the new one.
     * This ensures that user data stays synchronized with Firebase.
     * </p>
     *
     * @param meshan The Meshans object (user data) to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Meshans meshan);
    
    /**
     * Deletes a user from the database.
     * <p>
     * This method removes the entire user record from the local database.
     * It might be used during account deletion or when signing out completely.
     * </p>
     *
     * @param meshans The Meshans object to delete
     */
    @Delete
    void delete(Meshans meshans); // Delete the entire Meshans object

    /**
     * Retrieves a specific user by their Firebase user ID.
     * <p>
     * This method returns a LiveData object, which allows the UI to observe changes
     * to the user data and update automatically when changes occur.
     * </p>
     *
     * @param userId The Firebase user ID to retrieve
     * @return LiveData wrapper around the Meshans object, or null if not found
     */
    @Query("SELECT * FROM Meshans WHERE userId = :userId")
    LiveData<Meshans> get(String userId); // Fetch by userId

    /**
     * Deletes a user from the database by their Firebase user ID.
     * <p>
     * This method provides an alternative way to delete a user when only the user ID
     * is known, without having to retrieve the full Meshans object first.
     * </p>
     *
     * @param userId The Firebase user ID of the user to delete
     */
    @Query("DELETE FROM Meshans WHERE userId = :userId")
    void deleteByUserId(String userId);
}
