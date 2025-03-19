package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.dzovah.mesha.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountsSection extends AppCompatActivity {

    private ImageView profilePictureImageView;
    private Button changeProfilePictureButton, saveButton;
    private EditText usernameEditText, emailEditText;

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
        saveButton.setOnClickListener(v -> saveUserChanges());
    }

    private void loadCurrentUserData() {
        if (currentUser == null) return;

        // Show loading state
        // showLoadingState();

        meshansRepository.getUser(currentUser.getUid())
                .addOnSuccessListener(user -> {
                    // hideLoadingState();
                    if (user != null) {
                        currentMeshansUser = user;

                        // Populate UI with user data
                        usernameEditText.setText(user.getUsername());
                        emailEditText.setText(user.getEmail());

                        // Load profile picture
                        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().equals("to edit")) {
                            Glide.with(this)
                                    .load(user.getProfilePictureUrl())
                                    .placeholder(R.drawable.icon_mesha)
                                    .into(profilePictureImageView);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // hideLoadingState();
                    Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        if (currentUser == null || currentMeshansUser == null) return;

        String newUsername = usernameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();

        // Validate inputs
        if (newUsername.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        // showLoadingState();

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
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Add the profile picture URL to the updates
                        updates.put("profilePictureUrl", downloadUri.toString());

                        // Now update the user details
                        boolean emailChanged = !emailEditText.getText().toString().trim()
                                .equals(currentMeshansUser.getEmail());
                        updateUserDetails(updates, emailChanged, emailEditText.getText().toString().trim());
                    });
                })
                .addOnFailureListener(e -> {
                    // hideLoadingState();
                    Toast.makeText(this, "Failed to upload profile picture: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserDetails(Map<String, Object> updates, boolean emailChanged, String newEmail) {
        if (updates.isEmpty() && !emailChanged) {
            // hideLoadingState();
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // If email is changed, we need to update Firebase Auth as well
        if (emailChanged) {
            updateEmailInFirebaseAuth(newEmail, updates);
        } else {
            // Just update the Meshans data in Firestore
            updateMeshansData(updates);
        }
    }

    private void updateEmailInFirebaseAuth(String newEmail, Map<String, Object> updates) {
        // Update email in Firebase Auth
        currentUser.updateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    // Email updated successfully in Auth, now update in Firestore
                    updateMeshansData(updates);
                })
                .addOnFailureListener(e -> {
                    // This could fail if the user has not recently signed in
                    // You might need to re-authenticate the user
                    // hideLoadingState();
                    Toast.makeText(this, "Failed to update email. You may need to sign in again: "
                            + e.getMessage(), Toast.LENGTH_LONG).show();

                    // Option to re-authenticate (this would require getting the password from user)
                    // promptForReauthentication(newEmail, updates);
                });
    }

    // This method would be used if re-authentication is needed
    private void reauthenticateAndUpdateEmail(String password, String newEmail, Map<String, Object> updates) {
        if (currentUser == null) return;

        // Create credential
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

        // Re-authenticate
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Now update the email
                    currentUser.updateEmail(newEmail)
                            .addOnSuccessListener(aVoid2 -> updateMeshansData(updates))
                            .addOnFailureListener(e -> {
                                // hideLoadingState();
                                Toast.makeText(this, "Failed to update email: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // hideLoadingState();
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateMeshansData(Map<String, Object> updates) {
        if (updates.isEmpty()) {
            // hideLoadingState();
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the repository method to update user details
        meshansRepository.editUserDetails(currentUser.getUid(), updates)
                .addOnSuccessListener(aVoid -> {
                    // hideLoadingState();
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Reload user data to reflect changes
                    loadCurrentUserData();
                })
                .addOnFailureListener(e -> {
                    // hideLoadingState();
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Placeholder for a method to show a dialog for re-authentication
    // This would be implemented if needed for email changes
    /*
    private void promptForReauthentication(String newEmail, Map<String, Object> updates) {
        // Create a dialog to get password
        // On dialog complete, call reauthenticateAndUpdateEmail(password, newEmail, updates)
    }
    */

    /*
    private void showLoadingState() {
        // Show a progress dialog or other loading indicator
    }

    private void hideLoadingState() {
        // Hide the progress dialog or loading indicator
    }
    */
}