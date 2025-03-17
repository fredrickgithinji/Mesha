package com.dzovah.mesha.Database.Daos;

import com.dzovah.mesha.Database.Entities.Meshans;
import com.google.android.gms.tasks.Task;

public interface MeshansDao {
    // Save or update user details
    Task<Void> saveUserDetails(Meshans user);

    // Fetch user details by userId
    Task<Meshans> getUserDetails(String userId);

    // Update username
    Task<Void> updateUsername(String userId, String newUsername);

    // Update email
    Task<Void> updateEmail(String userId, String newEmail);

    // Update profile picture URL
    Task<Void> updateProfilePictureUrl(String userId, String newProfilePictureUrl);

    // Add user details to the database
    Task<Void> createUserDetails(Meshans meshan);
}
