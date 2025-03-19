package com.dzovah.mesha.Database.Daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dzovah.mesha.Database.Entities.Meshans;

@Dao
public interface MeshansDao {

    @Insert
    void insert(Meshans meshans); // Insert the entire Meshans object
    @Delete
    void delete(Meshans meshans); // Delete the entire Meshans object


     @Query("SELECT * FROM Meshans WHERE userId = :userId")
    LiveData<Meshans> get(String userId); // Fetch by userId

    @Query("DELETE FROM Meshans WHERE userId = :userId")
    void deleteByUserId(String userId);



}
