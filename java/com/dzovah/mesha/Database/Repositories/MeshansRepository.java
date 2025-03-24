package com.dzovah.mesha.Database.Repositories;

import androidx.lifecycle.LiveData;

import com.dzovah.mesha.Database.Daos.Firebase_Meshans_Data_linkDao;
import com.dzovah.mesha.Database.Daos.MeshansDao;
import com.dzovah.mesha.Database.Entities.Meshans;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MeshansRepository {

    private final Firebase_Meshans_Data_linkDao firebaseDao;
    private final MeshansDao roomDao;

    public MeshansRepository(Firebase_Meshans_Data_linkDao firebaseDao, MeshansDao roomDao) {
        this.firebaseDao = firebaseDao;
        this.roomDao = roomDao;
    }

    /**
     * Saves a Meshans object to Firebase and caches it in Room.
     * @param user Meshans object to be saved.
     * @return Task representing the asynchronous operation.
     */
    public Task<Void> saveUser(Meshans user) { // for signup
        // Create a TaskCompletionSource to manage the final task
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        // Start Firebase operation
        firebaseDao.writeToDatabase(user)
                .addOnSuccessListener(aVoid -> {
                    // When Firebase succeeds, perform Room operation
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        try {
                            // Insert user into Room
                            roomDao.insert(user);

                            // Mark the combined task as successful
                            tcs.setResult(null);
                        } catch (Exception e) {
                            // If Room operation fails, mark the combined task as failed
                            tcs.setException(new Exception("Room database operation failed", e));
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // If Firebase operation fails, mark the combined task as failed
                    tcs.setException(new Exception("Firebase operation failed", e));
                });

        // Return the task that will complete only when both operations finish
        return tcs.getTask();
    }

    /**
     * Retrieves a Meshans object from Room as LiveData while triggering a refresh from Firebase.
     * @param userId The user id.
     * @return LiveData of Meshans from the local Room database.
     */

    public Task<Meshans> getUser(String userId) { // for sign in
        TaskCompletionSource<Meshans> taskCompletionSource = new TaskCompletionSource<>();
        // Step 1: Delete local data if it exists
        Executors.newSingleThreadExecutor().execute(() -> {
            // Retrieve LiveData from Room
            LiveData<Meshans> localUserLiveData = roomDao.get(userId);
            // Observe LiveData to get the current value
            Meshans localUser = localUserLiveData.getValue();

            if (localUser != null) {
                roomDao.delete(localUser); // Delete local data
            }

            // Step 2: Read from Firebase
            firebaseDao.readFromDatabase(userId)
                .addOnSuccessListener(firebaseUser -> {
                    if (firebaseUser != null) {
                            // Step 3: Cache the new data in Room
                            Executors.newSingleThreadExecutor().execute(() -> {
                                roomDao.insert(firebaseUser); // Save fetched user to Room
                                taskCompletionSource.setResult(firebaseUser); // Return Firebase data
                            });
                        } else {
                            // Step 4: If no data in Firebase, return null
                            taskCompletionSource.setResult(null); // Indicate user not found
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Step 5: Handle Firebase failure
                        taskCompletionSource.setException(e); // Propagate the exception
                    });
        });

        return taskCompletionSource.getTask();
    }



    /**
     * Updates specific fields of a user's details in Firebase and refreshes the cache in Room.
     *
     * @param userId  The user id.
     * @param updates A map of fields to update.
     * @return Task representing the asynchronous update operation.
     */
    public Task<Void> editUserDetails(String userId, Map<String, Object> updates) {
        // Update Firebase with new user details.
        Task<Void> firebaseUpdateTask = firebaseDao.updateDatabaseUserInfo(userId, updates);

        // After a successful update in Firebase, refresh the local cache in Room.
        return firebaseUpdateTask
                .addOnSuccessListener(aVoid -> {
                    firebaseDao.readFromDatabase(userId)
                            .addOnSuccessListener(updatedUser -> {
                                if (updatedUser != null) {
                                    Executors.newSingleThreadExecutor().execute(() -> roomDao.insert(updatedUser));
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    // Handle Firebase update failure if needed.
                });
    }

    /**
     * Deletes a Meshans object from Firebase and removes it from Room.
     *
     * @param userId The user id to delete.
     * @return Task representing the asynchronous delete operation.
     */
    public Task<Void> deleteUser(String userId) { // For delete Account
        return firebaseDao.deleteFromDatabase(userId)
                .addOnSuccessListener(aVoid -> {
                    // Delete from Room by userId on a background thread
                    Executors.newSingleThreadExecutor().execute(() -> roomDao.deleteByUserId(userId));
                })
                .addOnFailureListener(e -> {
                    // Handle Firebase delete failure if needed
                });
    }

}
