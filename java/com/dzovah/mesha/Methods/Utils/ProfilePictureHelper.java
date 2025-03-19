package com.dzovah.mesha.Methods.Utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.dzovah.mesha.R;
import com.google.android.gms.tasks.Task;

public class ProfilePictureHelper {

    private static final int PICK_IMAGE_REQUEST = 1;
    private final Activity activity;
    private final StorageReference storageRef;

    public ProfilePictureHelper(Activity activity) {
        this.activity = activity;
        this.storageRef = FirebaseStorage.getInstance().getReference("Meshans-profile-pics");
    }

    // Open the gallery to select an image
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Upload the selected image to Firebase Storage
    public Task<Uri> uploadProfilePicture(Uri imageUri, String userId) {
        StorageReference fileRef = storageRef.child(userId + "/profile.jpg");

        // Delete the old image first (if it exists)
        return fileRef.delete()
                .continueWithTask(task -> {
                    // Whether delete succeeds or fails (file might not exist), continue with upload
                    return fileRef.putFile(imageUri);
                })
                .continueWithTask(uploadTask -> {
                    if (!uploadTask.isSuccessful()) {
                        throw uploadTask.getException();
                    }
                    // Get the secure download URL after upload
                    return fileRef.getDownloadUrl();
                });
    }

    // Load the profile picture into an ImageView
    public void loadProfilePicture(String userId, ImageView imageView) {
        StorageReference fileRef = storageRef.child(userId + "/profile.jpg");

        // Try to load the user's profile picture
        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Use Glide to load the image
            Glide.with(activity)
                    .load(uri)
                    .placeholder(R.drawable.icon_mesha) // Default image while loading
                    .error(R.drawable.icon_mesha) // Default image if loading fails
                    .circleCrop() // Crop the image into a circle
                    .into(imageView);
        }).addOnFailureListener(e -> {
            // If the user hasn't uploaded a profile picture, load the default image
            Glide.with(activity)
                    .load(R.drawable.icon_mesha)
                    .circleCrop()
                    .into(imageView);
        });
    }

    public static int getPickImageRequest() {
        return PICK_IMAGE_REQUEST;
    }
}