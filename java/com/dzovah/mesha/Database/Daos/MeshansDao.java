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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Meshans meshan);
    @Delete
    void delete(Meshans meshans); // Delete the entire Meshans object


     @Query("SELECT * FROM Meshans WHERE userId = :userId")
    LiveData<Meshans> get(String userId); // Fetch by userId

    @Query("DELETE FROM Meshans WHERE userId = :userId")
    void deleteByUserId(String userId);



}
