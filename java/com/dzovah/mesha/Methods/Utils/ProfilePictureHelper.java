package com.dzovah.mesha.Methods.Utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfilePictureHelper {

    private static final int PICK_IMAGE_REQUEST = 1;
    private final Activity activity;
    private final StorageReference storageRef;

    public ProfilePictureHelper(Activity activity) {
        this.activity = activity;
        this.storageRef = FirebaseStorage.getInstance().getReference("profile_pictures");
    }

    // Open the gallery to select an image
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Upload the selected image to Firebase Storage
    public UploadTask uploadProfilePicture(Uri imageUri, String userId) {
        StorageReference fileRef = storageRef.child(userId + ".jpg");
        return fileRef.putFile(imageUri);
    }

    // Load the profile picture into an ImageView
    public void loadProfilePicture(String userId, ImageView imageView) {
        StorageReference fileRef = storageRef.child(userId + ".jpg");
        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Use Glide or Picasso to load the image
            // Example with Glide:
            // Glide.with(activity).load(uri).into(imageView);
        });
    }
}