package com.dzovah.mesha.Database.Daos;

import com.dzovah.mesha.Database.Entities.Meshans;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class Firebase_Meshans_Data_linkDao {

    private final DatabaseReference usersDb;

    public Firebase_Meshans_Data_linkDao() {
        usersDb = FirebaseDatabase.getInstance().getReference("users");
    }

    /**
     * Writes a Meshans object to Firebase Realtime Database.
     */
    public Task<Void> writeToDatabase(Meshans user) {
        String userId = user.getUserId();
        return usersDb.child(userId).setValue(user);
    }

    /**
     * Reads a Meshans object from Firebase Realtime Database.
     */
    public Task<Meshans> readFromDatabase(String userId) {
        return usersDb.child(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().getValue(Meshans.class);
                    } else {
                        return null;
                    }
                });
    }

    /**
     * Updates specific fields of a user's data in Firebase Realtime Database.
     */
    public Task<Void> updateDatabaseUserInfo(String userId, Map<String, Object> updates) {
        return usersDb.child(userId).updateChildren(updates);
    }

    /**
     * Deletes a user's data from Firebase Realtime Database.
     */
    public Task<Void> deleteFromDatabase(String userId) {
        return usersDb.child(userId).removeValue();
    }

}