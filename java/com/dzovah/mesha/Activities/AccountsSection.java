package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dzovah.mesha.Database.Daos.FirebaseMeshansDao;
import com.dzovah.mesha.Database.Daos.MeshansDao;
import com.dzovah.mesha.Methods.Utils.ProfilePictureHelper;
import com.dzovah.mesha.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountsSection extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText usernameEditText, emailEditText;
    private ImageView profilePictureImageView;
    private Button saveButton, changeProfilePictureButton;
    private MeshansDao meshansDao;
    private ProfilePictureHelper profilePictureHelper;
    private String userId;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_section);

        // Initialize views
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        profilePictureImageView = findViewById(R.id.profilePictureImageView);
        saveButton = findViewById(R.id.saveButton);
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton);

        // Initialize DAO and helper
        meshansDao = new FirebaseMeshansDao();
        profilePictureHelper = new ProfilePictureHelper(this);

        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadUserDetails();
        }

        // Set click listeners
        changeProfilePictureButton.setOnClickListener(v -> profilePictureHelper.openGallery());
        saveButton.setOnClickListener(v -> saveUserDetails());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profilePictureImageView.setImageURI(imageUri);
        }
    }

    private void loadUserDetails() {
        meshansDao.getUserDetails(userId).addOnSuccessListener(meshans -> {
            if (meshans != null) {
                usernameEditText.setText(meshans.getUsername());
                emailEditText.setText(meshans.getEmail());
                profilePictureHelper.loadProfilePicture(userId, profilePictureImageView);
            }
        });
    }

    private void saveUserDetails() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update username and email
        meshansDao.updateUsername(userId, username);
        meshansDao.updateEmail(userId, email);

        // Upload profile picture if selected
        if (imageUri != null) {
            profilePictureHelper.uploadProfilePicture(imageUri, userId)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the download URL and update the profile picture URL in Firestore
                        taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                            meshansDao.updateProfilePictureUrl(userId, uri.toString());
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        });
                    });
        } else {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        }
    }
}
