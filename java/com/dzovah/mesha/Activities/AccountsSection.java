package com.dzovah.mesha.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.dzovah.mesha.Database.Daos.Firebase_Meshans_Data_linkDao;
import com.dzovah.mesha.Database.Daos.MeshansDao;
import com.dzovah.mesha.Database.Entities.Meshans;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Repositories.MeshansRepository;
import com.dzovah.mesha.Methods.Utils.AuthManager;
import com.dzovah.mesha.Methods.Utils.LocalStorageUtil;
import com.dzovah.mesha.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountsSection extends AppCompatActivity {

    private ImageView profilePictureImageView;
    private Button changeProfilePictureButton, saveButton;
    private EditText usernameEditText, emailEditText;
    private ProgressBar progressBar;
    private TextView statusTextView;

    private MeshansRepository meshansRepository;
    private AuthManager authManager;
    private FirebaseUser currentUser;
    private Meshans currentMeshansUser;
    private Uri selectedImageUri = null;

    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_pictures");

    // ActivityResultLauncher for image selection
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    loadImageIntoView(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_section);

        // Initialize views
        profilePictureImageView = findViewById(R.id.profilePictureImageView);
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);

        // Initialize repositories and auth manager
        initializeRepositories();

        // Set up event listeners
        setupEventListeners();

        // Load current user data
        loadCurrentUserData();
    }

    private void initializeRepositories() {
        // Initialize AuthManager
        authManager = new AuthManager(this);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            // User not logged in, redirect to login
            Toast.makeText(this, "Please log in to edit your profile", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize MeshansRepository
        Firebase_Meshans_Data_linkDao firebaseDao = new Firebase_Meshans_Data_linkDao();
        MeshansDao roomDao = MeshaDatabase.Get_database(this).meshansDao();
        meshansRepository = new MeshansRepository(firebaseDao, roomDao);
    }

    private void setupEventListeners() {
        // Set up change profile picture button
        changeProfilePictureButton.setOnClickListener(v -> openImagePicker());

        // Set up save button
        saveButton.setOnClickListener(v -> {
            // Disable the button to prevent multiple clicks
            saveButton.setEnabled(false);
            saveUserChanges();
        });
    }


    private void loadCurrentUserData() {
        if (currentUser == null) return;

        // Show loading state
        showLoadingState("Loading profile data...");

        meshansRepository.getUser(currentUser.getUid())
                .addOnSuccessListener(user -> {
                    if (user != null) {
                        currentMeshansUser = user;

                        // Populate UI with user data
                        usernameEditText.setText(user.getUsername());
                        emailEditText.setText(user.getEmail());

                        // Load profile picture - improved handling
                        loadProfilePicture(user);

                        hideLoadingState();
                    } else {
                        hideLoadingState();
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoadingState();
                    Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfilePicture(Meshans user) {
        String fileName = user.getUserId() + "_profile.png";

        // Set placeholder image initially
        profilePictureImageView.setImageResource(R.drawable.icon_mesha);

        if (user.getProfilePictureUrl() == null || user.getProfilePictureUrl().equals("to edit")) {
            // No profile picture to load
            return;
        }

        // Always try to load cached image first
        Bitmap cachedImage = LocalStorageUtil.loadImage(this, fileName);
        if (cachedImage != null) {
            // Use cached image
            Log.d("AccountsSection", "Using cached profile image");
            profilePictureImageView.setImageBitmap(cachedImage);
            return; // Return early if we have a cached image
        }

        // If we're here, there's no cached image but we have a URL
        // Check if we have connectivity before attempting to download
        if (isNetworkAvailable()) {
            Log.d("AccountsSection", "Downloading profile image: " + user.getProfilePictureUrl());
            downloadAndSaveProfilePicture(user.getProfilePictureUrl(), user.getUserId());
        } else {
            Log.d("AccountsSection", "No network connection and no cached image available");
            // Just keep the placeholder visible
        }
    }

    private void downloadAndSaveProfilePicture(String imageUrl, String userId) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        // Show a small loading indicator
        updateProgressStatus("Downloading profile picture...");

        // Use Glide to handle the downloading and caching
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new com.bumptech.glide.request.target.SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        // Save the image to local storage
                        String fileName = userId + "_profile.png";
                        boolean saved = LocalStorageUtil.saveImage(AccountsSection.this, bitmap, fileName);
                        Log.d("AccountsSection", "Profile image saved to local storage: " + saved);

                        // Set the image
                        profilePictureImageView.setImageBitmap(bitmap);
                        hideLoadingState();
                    }

                    @Override
                    public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                        Log.e("AccountsSection", "Failed to download profile image: " + imageUrl);
                        hideLoadingState();
                    }
                });
    }

    // Add this helper method to check for network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadImageIntoView(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.icon_mesha)
                .into(profilePictureImageView);
    }

    private void saveUserChanges() {
        if (currentUser == null || currentMeshansUser == null) {
            saveButton.setEnabled(true);
            return;
        }

        String newUsername = usernameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();

        // Validate inputs
        if (newUsername.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        // Show loading state
        showLoadingState("Saving changes...");

        // Create a map for the updates
        Map<String, Object> updates = new HashMap<>();

        // Check if username changed
        if (!newUsername.equals(currentMeshansUser.getUsername())) {
            updates.put("username", newUsername);
        }

        // Check if email changed
        boolean emailChanged = !newEmail.equals(currentMeshansUser.getEmail());
        if (emailChanged) {
            updates.put("email", newEmail);
        }

        // Handle profile picture upload if selected
        if (selectedImageUri != null) {
            showLoadingState("Uploading profile picture...");
            uploadProfilePicture(updates);
        } else {
            // Just update the user details without changing the profile picture
            updateUserDetails(updates, emailChanged, newEmail);
        }
    }

    private void uploadProfilePicture(Map<String, Object> updates) {
        // Generate a unique filename for the image
        String filename = "profile_" + UUID.randomUUID().toString();
        StorageReference fileRef = storageRef.child(filename);

        // Upload the image
        fileRef.putFile(selectedImageUri)
                .addOnProgressListener(taskSnapshot -> {
                    // Calculate progress percentage
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    updateProgressStatus("Uploading image: " + (int)progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    updateProgressStatus("Processing image...");
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Add the profile picture URL to the updates
                        updates.put("profilePictureUrl", downloadUri.toString());

                        // Save the new image locally
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            LocalStorageUtil.saveImage(this, bitmap, currentUser.getUid() + "_profile.png");
                        } catch (IOException e) {
                            Log.e("AccountsSection", "Error saving new image: " + e.getMessage());
                        }

                        // Now update the user details
                        boolean emailChanged = !emailEditText.getText().toString().trim()
                                .equals(currentMeshansUser.getEmail());
                        updateUserDetails(updates, emailChanged, emailEditText.getText().toString().trim());
                    });
                })
                .addOnFailureListener(e -> {
                    hideLoadingState();
                    Toast.makeText(this, "Failed to upload profile picture: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                });
    }

    private void updateUserDetails(Map<String, Object> updates, boolean emailChanged, String newEmail) {
        if (updates.isEmpty() && !emailChanged) {
            hideLoadingState();
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        // If email is changed, we need to update Firebase Auth as well
        if (emailChanged) {
            updateProgressStatus("Updating email address...");
            updateEmailInFirebaseAuth(newEmail, updates);
        } else {
            // Just update the Meshans data in Firestore
            updateProgressStatus("Saving profile data...");
            updateMeshansData(updates);
        }
    }

    private void updateEmailInFirebaseAuth(String newEmail, Map<String, Object> updates) {
        // Update email in Firebase Auth
        currentUser.updateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    // Email updated successfully in Auth, now update in Firestore
                    updateProgressStatus("Email updated, saving profile...");
                    updateMeshansData(updates);
                })
                .addOnFailureListener(e -> {
                    // This could fail if the user has not recently signed in
                    hideLoadingState();
                    Toast.makeText(this, "Failed to update email. You may need to sign in again: "
                            + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);

                    // Option to re-authenticate (this would require getting the password from user)
                    promptForReauthentication(newEmail, updates);
                });
    }

    private void reauthenticateAndUpdateEmail(String password, String newEmail, Map<String, Object> updates) {
        if (currentUser == null) return;

        showLoadingState("Re-authenticating...");

        // Create credential
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

        // Re-authenticate
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Now update the email
                    updateProgressStatus("Updating email address...");
                    currentUser.updateEmail(newEmail)
                            .addOnSuccessListener(aVoid2 -> {
                                updateProgressStatus("Email updated, saving profile...");
                                updateMeshansData(updates);
                            })
                            .addOnFailureListener(e -> {
                                hideLoadingState();
                                Toast.makeText(this, "Failed to update email: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                saveButton.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    hideLoadingState();
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                });
    }

    private void updateMeshansData(Map<String, Object> updates) {
        if (updates.isEmpty()) {
            hideLoadingState();
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        // Use the repository method to update user details
        meshansRepository.editUserDetails(currentUser.getUid(), updates)
                .addOnSuccessListener(aVoid -> {
                    hideLoadingState();
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    // Reset selected image
                    selectedImageUri = null;
                    // Reload user data to reflect changes
                    loadCurrentUserData();
                })
                .addOnFailureListener(e -> {
                    hideLoadingState();
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                });
    }

    // Implement re-authentication dialog
    private void promptForReauthentication(String newEmail, Map<String, Object> updates) {
        // This is a simplified example - you would create a proper dialog in your app
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Re-authentication Required");
        builder.setMessage("Please enter your password to continue");

        // Add an EditText to input password
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("Continue", (dialog, which) -> {
            String password = passwordInput.getText().toString();
            if (!password.isEmpty()) {
                reauthenticateAndUpdateEmail(password, newEmail, updates);
            } else {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                saveButton.setEnabled(true);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            saveButton.setEnabled(true);
        });

        builder.show();
    }


    private void showLoadingState(String message) {
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText(message);
        statusTextView.setVisibility(View.VISIBLE);
        disableInputs(true);
    }

    private void updateProgressStatus(String message) {
        statusTextView.setText(message);
    }

    private void hideLoadingState() {
        progressBar.setVisibility(View.INVISIBLE);
        statusTextView.setVisibility(View.INVISIBLE);
        disableInputs(false);
    }

    private void disableInputs(boolean disable) {
        usernameEditText.setEnabled(!disable);
        emailEditText.setEnabled(!disable);
        changeProfilePictureButton.setEnabled(!disable);
        // saveButton is managed separately to prevent multiple clicks
    }
}