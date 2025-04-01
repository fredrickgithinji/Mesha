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

/**
 * Helper class for managing user profile pictures in the Mesha application.
 * <p>
 * This class provides methods for:
 * <ul>
 *   <li>Opening the device gallery to select profile pictures</li>
 *   <li>Uploading selected images to Firebase Storage</li>
 *   <li>Loading and displaying profile pictures with proper formatting</li>
 * </ul>
 * </p>
 * <p>
 * The helper uses Firebase Storage for cloud storage of profile pictures and
 * Glide for efficient image loading and caching. It includes fallback mechanisms
 * for loading default images when a user's profile picture is unavailable.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see FirebaseStorage
 * @see Glide
 */
public class ProfilePictureHelper {

    /** Request code for image picker intent */
    private static final int PICK_IMAGE_REQUEST = 1;
    
    /** The activity context used for UI operations */
    private final Activity activity;
    
    /** Firebase Storage reference for profile pictures */
    private final StorageReference storageRef;

    /**
     * Constructs a new ProfilePictureHelper instance.
     * <p>
     * Initializes Firebase Storage reference to the "Meshans-profile-pics" directory
     * for storing user profile pictures.
     * </p>
     *
     * @param activity The activity context used for UI operations
     */
    public ProfilePictureHelper(Activity activity) {
        this.activity = activity;
        this.storageRef = FirebaseStorage.getInstance().getReference("Meshans-profile-pics");
    }

    /**
     * Opens the device's gallery to allow the user to select a profile picture.
     * <p>
     * This method launches an intent to pick an image from the device's
     * external storage. The result will be returned to the activity's
     * onActivityResult method with the PICK_IMAGE_REQUEST code.
     * </p>
     */
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Uploads a user's profile picture to Firebase Storage.
     * <p>
     * This method performs a sequence of operations:
     * <ol>
     *   <li>First attempts to delete any existing profile picture for the user</li>
     *   <li>Uploads the new image to Firebase Storage</li>
     *   <li>Retrieves and returns the secure download URL for the uploaded image</li>
     * </ol>
     * </p>
     *
     * @param imageUri The URI of the image to upload
     * @param userId The user ID to associate with the profile picture
     * @return A Task that will complete with the download URL of the uploaded image
     */
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

    /**
     * Loads and displays a user's profile picture in an ImageView.
     * <p>
     * This method attempts to load the user's profile picture from Firebase Storage.
     * If the image is found, it is loaded into the ImageView using Glide with proper
     * formatting (circular crop). If the image is not found or fails to load, a
     * default image is displayed instead.
     * </p>
     *
     * @param userId The user ID whose profile picture should be loaded
     * @param imageView The ImageView to display the profile picture in
     */
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

    /**
     * Gets the request code used for image picker intent.
     * <p>
     * This method returns the constant used to identify image selection
     * results in the activity's onActivityResult method.
     * </p>
     *
     * @return The request code for image selection intent
     */
    public static int getPickImageRequest() {
        return PICK_IMAGE_REQUEST;
    }
}