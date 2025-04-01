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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository class that manages Meshans data operations with both Firebase and Room databases.
 * <p>
 * This class follows the repository pattern and serves as a clean API for data access.
 * It manages query threads, data caching strategies, and coordinates data fetching and storage 
 * between local Room database and remote Firebase database.
 * </p>
 */
public class MeshansRepository {

    private final Firebase_Meshans_Data_linkDao firebaseDao;
    private final MeshansDao roomDao;
    
    /**
     * Shared executor service for all database operations to avoid creating
     * a new thread pool for each operation, which improves efficiency.
     */
    private final ExecutorService databaseExecutor;

    /**
     * Constructs a new MeshansRepository.
     *
     * @param firebaseDao Firebase DAO for remote database operations
     * @param roomDao Room DAO for local database operations
     */
    public MeshansRepository(Firebase_Meshans_Data_linkDao firebaseDao, MeshansDao roomDao) {
        this.firebaseDao = firebaseDao;
        this.roomDao = roomDao;
        this.databaseExecutor = Executors.newFixedThreadPool(2); // Fixed size pool for database operations
    }

    /**
     * Saves a Meshans object to Firebase and caches it in Room.
     * <p>
     * This method writes to Firebase first, and only after successful Firebase
     * write does it cache the data in Room. All Room operations are performed
     * on a background thread.
     * </p>
     *
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
                    databaseExecutor.execute(() -> {
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
     * Retrieves a Meshans object from Firebase Realtime Database.
     * <p>
     * This method directly reads from Firebase and then caches the result
     * in Room on a background thread. It returns the Firebase data immediately
     * without waiting for the Room caching to complete.
     * </p>
     *
     * @param userId The user id.
     * @return Task that will complete with the user object from Firebase.
     */
    public Task<Meshans> getUser(String userId) { // for sign in
        TaskCompletionSource<Meshans> taskCompletionSource = new TaskCompletionSource<>();
        
        // Read directly from Firebase
        firebaseDao.readFromDatabase(userId)
            .addOnSuccessListener(firebaseUser -> {
                if (firebaseUser != null) {
                    // Cache the data in Room on a background thread
                    databaseExecutor.execute(() -> {
                        try {
                            roomDao.insert(firebaseUser); // Save fetched user to Room
                        } catch (Exception e) {
                            // Log error but continue - we already have the Firebase data
                            e.printStackTrace();
                        }
                    });
                    
                    // Return the Firebase data immediately
                    taskCompletionSource.setResult(firebaseUser);
                } else {
                    // No user data found in Firebase
                    taskCompletionSource.setResult(null);
                }
            })
            .addOnFailureListener(e -> {
                // Handle Firebase failure
                taskCompletionSource.setException(e);
            });

        return taskCompletionSource.getTask();
    }

    /**
     * Updates specific fields of a user's details in Firebase and refreshes the cache in Room.
     * <p>
     * This method optimizes the update process by:
     * 1. Applying the updates to Firebase
     * 2. Using the updates map to also update the Room cache without requiring another network call
     * 3. Handling errors appropriately for both operations
     * </p>
     *
     * @param userId  The user id.
     * @param updates A map of fields to update.
     * @return Task representing the asynchronous update operation.
     */
    public Task<Void> editUserDetails(String userId, Map<String, Object> updates) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        
        // Update Firebase with new user details
        firebaseDao.updateDatabaseUserInfo(userId, updates)
                .addOnSuccessListener(aVoid -> {
                    // After successful Firebase update, update Room cache on background thread
                    databaseExecutor.execute(() -> {
                        try {
                            // Get current user from Room (if it exists)
                            LiveData<Meshans> userLiveData = roomDao.get(userId);
                            Meshans localUser = userLiveData.getValue();
                            
                            if (localUser != null) {
                                // Apply the same updates to the local user object
                                if (updates.containsKey("username")) {
                                    localUser.setUsername((String) updates.get("username"));
                                }
                                if (updates.containsKey("email")) {
                                    localUser.setEmail((String) updates.get("email"));
                                }
                                if (updates.containsKey("profilePictureUrl")) {
                                    localUser.setProfilePictureUrl((String) updates.get("profilePictureUrl"));
                                }
                                if (updates.containsKey("isPremium")) {
                                    localUser.setPremium((Boolean) updates.get("isPremium"));
                                }
                                
                                // Save updated user to Room
                                roomDao.insert(localUser);
                            } else {
                                // If not in Room cache yet, fetch complete user from Firebase
                                firebaseDao.readFromDatabase(userId)
                                        .addOnSuccessListener(updatedUser -> {
                                            if (updatedUser != null) {
                                                databaseExecutor.execute(() -> roomDao.insert(updatedUser));
                                            }
                                        });
                            }
                            
                            // Set task result on main thread
                            tcs.setResult(null);
                        } catch (Exception e) {
                            // Log Room error but don't fail the task since Firebase update succeeded
                            e.printStackTrace();
                            tcs.setResult(null);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // If Firebase update fails, propagate the failure
                    tcs.setException(e);
                });
                
        return tcs.getTask();
    }

    /**
     * Deletes a Meshans object from Firebase and removes it from Room.
     * <p>
     * This method ensures proper deletion from both databases and handles any
     * errors that might occur during the process.
     * </p>
     *
     * @param userId The user id to delete.
     * @return Task representing the asynchronous delete operation.
     */
    public Task<Void> deleteUser(String userId) { // For delete Account
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        
        firebaseDao.deleteFromDatabase(userId)
                .addOnSuccessListener(aVoid -> {
                    // Delete from Room by userId on a background thread
                    databaseExecutor.execute(() -> {
                        try {
                            roomDao.deleteByUserId(userId);
                            tcs.setResult(null);
                        } catch (Exception e) {
                            // Log Room error but don't fail the task since Firebase delete succeeded
                            e.printStackTrace();
                            tcs.setResult(null);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // If Firebase delete fails, propagate the failure
                    tcs.setException(e);
                });
                
        return tcs.getTask();
    }
    
    /**
     * Releases resources when the repository is no longer needed.
     * Should be called when the app is closing or the repository is no longer used.
     */
    public void cleanup() {
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
    }
}
