package com.dzovah.mesha.Database.Daos;

import com.dzovah.mesha.Database.Entities.Meshans;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseMeshansDao implements MeshansDao {

    private static final String COLLECTION_NAME = "meshans";
    private final FirebaseFirestore db;

    public FirebaseMeshansDao() {
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public Task<Void> saveUserDetails(Meshans user) {
        return db.collection(COLLECTION_NAME)
                .document(user.getUserId())
                .set(user);
    }

    @Override
    public Task<Meshans> getUserDetails(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .get()
                .continueWith(task -> task.getResult().toObject(Meshans.class));
    }

    @Override
    public Task<Void> updateUsername(String userId, String newUsername) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .update("username", newUsername);
    }

    @Override
    public Task<Void> updateEmail(String userId, String newEmail) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .update("email", newEmail);
    }

    @Override
    public Task<Void> updateProfilePictureUrl(String userId, String newProfilePictureUrl) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .update("profilePictureUrl", newProfilePictureUrl);
    }

    @Override
    public Task<Void> createUserDetails(Meshans meshan) {
        return db.collection(COLLECTION_NAME)
                .document(meshan.getUserId())
                .set(meshan);
    }
}