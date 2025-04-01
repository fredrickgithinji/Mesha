package com.dzovah.mesha.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 * Activity for managing user account profiles and settings.
 * <p>
 * This activity provides a user interface for:
 * <ul>
 *     <li>Viewing and editing user profile information (username, email)</li>
 *     <li>Uploading and changing profile pictures</li>
 *     <li>Managing authentication credentials</li>
 *     <li>Performing account-related operations with proper caching</li>
 * </ul>
 * The activity implements efficient data loading with local caching to improve
 * performance and reduce network usage. It handles both online and offline states
 * gracefully, providing appropriate fallbacks when network connectivity is unavailable.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see SignInActivity
 * @see AuthManager
 * @see MeshansRepository
 * @see LocalStorageUtil
 */
public class AccountsSection extends AppCompatActivity {

    /** ImageView for displaying the user's profile picture */
    private ImageView profilePictureImageView;
    
    /** Buttons for changing profile picture and saving profile changes */
    private Button changeProfilePictureButton, saveButton;
    
    /** EditText fields for username and email input */
    private EditText usernameEditText, emailEditText;
    
    /** Progress indicator for async operations */
    private ProgressBar progressBar;
    
    /** Text view for displaying operation status messages */
    private TextView statusTextView;

    /** Repository for Meshans user data operations */
    private MeshansRepository meshansRepository;
    
    /** Manager for authentication operations */
    private AuthManager authManager;
    
    /** Current authenticated Firebase user */
    private FirebaseUser currentUser;
    
    /** Current user's Meshans profile data */
    private Meshans currentMeshansUser;
    
    /** URI of selected profile image from gallery */
    private Uri selectedImageUri = null;

    /** Firebase Storage reference for profile pictures */
    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_pictures");

    /** 
     * ActivityResultLauncher for image selection from gallery 
     * Handles the result of the image picker intent and loads the selected image
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    loadImageIntoView(selectedImageUri);
                }
            });

    /** Flag to indicate whether data should be refreshed from network */
    private boolean shouldRefreshData = false;

    /**
     * Initializes the activity, sets up UI components, and loads user data.
     * <p>
     * This method handles the initial setup of the account profile screen,
     * including initializing repositories, setting up event listeners,
     * and loading user data with a preference for cached data when available.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
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

        // Disable save button until initialization is complete
        saveButton.setEnabled(false);
        
        // Setup event listeners first (will be enabled after init)
        setupEventListeners();

        // Initialize repositories and auth manager asynchronously
        initializeRepositoriesAsync();
    }

    /**
     * Initializes the repositories and authentication manager required for user operations
     * on a background thread to avoid blocking the UI thread.
     * <p>
     * This method sets up:
     * <ul>
     *     <li>The AuthManager for handling Firebase authentication</li>
     *     <li>The MeshansRepository for user data operations</li>
     * </ul>
     * If no user is logged in, the method redirects to the sign-in screen.
     * </p>
     */
    private void initializeRepositoriesAsync() {
        showLoadingState("Initializing...");
        
        new Thread(() -> {
            try {
                // Initialize AuthManager (must be done on main thread as it may use Activity context)
                runOnUiThread(() -> {
                    authManager = new AuthManager(AccountsSection.this);
                });
                
                // Get current user - can be done on background thread
                currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser == null) {
                    // User not logged in, redirect to login on UI thread
                    runOnUiThread(() -> {
                        Toast.makeText(AccountsSection.this, "Please log in to edit your profile", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AccountsSection.this, SignInActivity.class));
                        finish();
                    });
                    return;
                }

                // Initialize MeshansRepository
                Firebase_Meshans_Data_linkDao firebaseDao = new Firebase_Meshans_Data_linkDao();
                MeshansDao roomDao = MeshaDatabase.Get_database(AccountsSection.this).meshansDao();
                meshansRepository = new MeshansRepository(firebaseDao, roomDao);
                
                // Now that initialization is complete, load user data
                loadCurrentUserDataFromCacheAsync();
            } catch (Exception e) {
                Log.e("AccountsSection", "Error initializing repositories: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(AccountsSection.this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideLoadingState();
                });
            }
        }).start();
    }

    /**
     * Sets up event listeners for user interface components.
     * <p>
     * Configures:
     * <ul>
     *     <li>The profile picture change button to open the image picker</li>
     *     <li>The save button to save changes to the user profile</li>
     * </ul>
     * </p>
     */
    private void setupEventListeners() {
        // Set up change profile picture button
        changeProfilePictureButton.setOnClickListener(v -> openImagePicker());

        // Set up save button
        saveButton.setOnClickListener(v -> {
            // Disable the button to prevent multiple clicks
            saveButton.setEnabled(false);
            saveUserChanges();
            // Set flag to refresh data next time to verify changes
            shouldRefreshData = true;
        });
    }

    /**
     * Loads user data from local cache with fallback to network fetch.
     * <p>
     * This method implements an efficient data loading strategy:
     * <ul>
     *     <li>First attempts to load data from SharedPreferences</li>
     *     <li>Loads cached profile image from local storage</li>
     *     <li>Falls back to network loading if cache is empty or refresh is needed</li>
     * </ul>
     * All operations are performed on background threads to avoid blocking the UI.
     * </p>
     */
    private void loadCurrentUserDataFromCacheAsync() {
        runOnUiThread(() -> showLoadingState("Loading your profile..."));
        
        new Thread(() -> {
            try {
                // Try to load from cache first
                SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                String userId = currentUser.getUid();
                String username = prefs.getString(userId + "_username", null);
                String email = prefs.getString(userId + "_email", null);
                String profilePictureUrl = prefs.getString(userId + "_profilePictureUrl", null);
                boolean isPremium = prefs.getBoolean(userId + "_isPremium", false);

                if (username != null && email != null) {
                    // Create a Meshans object from the cached data
                    currentMeshansUser = new Meshans(userId, username, email, profilePictureUrl, isPremium);
                    
                    // Update UI on the main thread
                    runOnUiThread(() -> {
                        usernameEditText.setText(username);
                        emailEditText.setText(email);
                        
                        // Enable save button now that data is loaded
                        saveButton.setEnabled(true);
                        hideLoadingState();
                    });
                    
                    // Load profile picture (can happen after UI is updated)
                    loadProfilePictureAsync(currentMeshansUser);
                    
                    // If refresh is needed or we're online, also load from network
                    if (shouldRefreshData || isNetworkAvailable()) {
                        loadCurrentUserDataFromNetworkAsync();
                    }
                } else {
                    // No cached data, try to load from network
                    if (isNetworkAvailable()) {
                        loadCurrentUserDataFromNetworkAsync();
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(AccountsSection.this, "No cached data and no network connection", Toast.LENGTH_SHORT).show();
                            hideLoadingState();
                            saveButton.setEnabled(true);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("AccountsSection", "Error loading from cache: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(AccountsSection.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideLoadingState();
                    
                    // Still enable the save button so user can try to make changes
                    saveButton.setEnabled(true);
                });
                
                // Try network as fallback
                if (isNetworkAvailable()) {
                    loadCurrentUserDataFromNetworkAsync();
                }
            }
        }).start();
    }

    /**
     * Loads user data from the network and updates local cache.
     * <p>
     * This method:
     * <ul>
     *     <li>Fetches user data from Firebase via the MeshansRepository</li>
     *     <li>Updates the UI with the fetched data</li>
     *     <li>Caches the fetched data in SharedPreferences</li>
     *     <li>Loads the user's profile picture</li>
     * </ul>
     * All operations are performed on background threads to avoid blocking the UI.
     * </p>
     */
    private void loadCurrentUserDataFromNetworkAsync() {
        runOnUiThread(() -> showLoadingState("Syncing with server..."));
        
        meshansRepository.getUser(currentUser.getUid())
            .addOnSuccessListener(user -> {
                if (user != null) {
                    // Save the user data to the class variable
                    currentMeshansUser = user;
                    
                    // Cache the user data in SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(user.getUserId() + "_username", user.getUsername());
                    editor.putString(user.getUserId() + "_email", user.getEmail());
                    editor.putString(user.getUserId() + "_profilePictureUrl", user.getProfilePictureUrl());
                    editor.putBoolean(user.getUserId() + "_isPremium", user.isPremium());
                    editor.apply();
                    
                    // Update UI on the main thread
                    runOnUiThread(() -> {
                        usernameEditText.setText(user.getUsername());
                        emailEditText.setText(user.getEmail());
                        
                        // Enable save button now that data is loaded
                        saveButton.setEnabled(true);
                        hideLoadingState();
                    });
                    
                    // Load profile picture asynchronously
                    loadProfilePictureAsync(user);
                } else {
                    // No user data in Firebase
                    runOnUiThread(() -> {
                        Toast.makeText(AccountsSection.this, "No user data found", Toast.LENGTH_SHORT).show();
                        hideLoadingState();
                        saveButton.setEnabled(true);
                    });
                }
            })
            .addOnFailureListener(e -> {
                Log.e("AccountsSection", "Error loading user from network: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(AccountsSection.this, "Error loading from server: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideLoadingState();
                    saveButton.setEnabled(true);
                });
            });
    }

    /**
     * Loads the user's profile picture with caching for efficiency.
     * <p>
     * This method follows these steps:
     * <ul>
     *     <li>First attempts to load the profile picture from local cache</li>
     *     <li>If cached image is not available, downloads from network if available</li>
     *     <li>Falls back to default image if neither cache nor network is available</li>
     * </ul>
     * All image loading operations are performed on background threads to avoid blocking the UI.
     * </p>
     *
     * @param user The Meshans user object containing profile data
     */
    private void loadProfilePictureAsync(Meshans user) {
        new Thread(() -> {
            try {
                // Try to load from local storage first
                Bitmap cachedImage = LocalStorageUtil.loadImage(this, currentUser.getUid() + "_profile.png");
                if (cachedImage != null) {
                    // Use cached image
                    runOnUiThread(() -> profilePictureImageView.setImageBitmap(cachedImage));
                } else if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().equals("to edit") && isNetworkAvailable()) {
                    // Load from network using Glide (which handles threading)
                    runOnUiThread(() -> {
                        Glide.with(AccountsSection.this)
                            .asBitmap()
                            .load(user.getProfilePictureUrl())
                            .placeholder(R.drawable.icon_mesha)
                            .error(R.drawable.icon_mesha)
                            .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap bitmap, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                                    // Set image to ImageView
                                    profilePictureImageView.setImageBitmap(bitmap);
                                    
                                    // Save image to local storage on a background thread
                                    new Thread(() -> {
                                        LocalStorageUtil.saveImage(AccountsSection.this, bitmap, currentUser.getUid() + "_profile.png");
                                    }).start();
                                }
                                
                                @Override
                                public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                                    // Set default image
                                    profilePictureImageView.setImageResource(R.drawable.icon_mesha);
                                }
                                
                                @Override
                                public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                                    // Do nothing
                                }
                            });
                    });
                } else {
                    // Use default image
                    runOnUiThread(() -> profilePictureImageView.setImageResource(R.drawable.icon_mesha));
                }
            } catch (Exception e) {
                Log.e("AccountsSection", "Error loading profile picture: " + e.getMessage());
                // Use default image
                runOnUiThread(() -> profilePictureImageView.setImageResource(R.drawable.icon_mesha));
            }
        }).start();
    }

    /**
     * Saves changes to the user profile.
     * <p>
     * This method:
     * <ul>
     *     <li>Validates user input (username, email)</li>
     *     <li>Identifies which fields have changed</li>
     *     <li>Handles profile picture uploads if a new image was selected</li>
     *     <li>Updates user details in the database</li>
     * </ul>
     * Shows appropriate loading status during the process.
     * </p>
     */
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

    /**
     * Uploads a new profile picture to Firebase Storage.
     * <p>
     * This method:
     * <ul>
     *     <li>Generates a unique filename for the image</li>
     *     <li>Uploads the image to Firebase Storage</li>
     *     <li>Updates the profile picture URL in the user's profile</li>
     *     <li>Saves the image to local storage for caching</li>
     * </ul>
     * Shows upload progress during the operation.
     * </p>
     *
     * @param updates Map of user profile updates to apply after upload
     */
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
                            Log.e("AccountsSection", "Error saving profile image locally: " + e.getMessage());
                        }

                        // Update the user details
                        boolean emailChanged = updates.containsKey("email");
                        String newEmail = emailChanged ? (String) updates.get("email") : null;
                        updateUserDetails(updates, emailChanged, newEmail);
                    });
                })
                .addOnFailureListener(e -> {
                    hideLoadingState();
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                });
    }

    /**
     * Updates user details in the database.
     * <p>
     * This method handles:
     * <ul>
     *     <li>Email changes (which require Firebase Auth update)</li>
     *     <li>Username and other profile field updates</li>
     * </ul>
     * If the email is being changed, this requires special handling through Firebase Auth.
     * </p>
     *
     * @param updates Map of user profile updates to apply
     * @param emailChanged Boolean indicating if the email address is being changed
     * @param newEmail The new email address (if being changed)
     */
    private void updateUserDetails(Map<String, Object> updates, boolean emailChanged, String newEmail) {
        if (updates.isEmpty() && !emailChanged) {
            // No changes to make
            hideLoadingState();
            Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        if (emailChanged && newEmail != null) {
            // Email change requires special handling through Firebase Auth
            promptForReauthentication(newEmail, updates);
        } else {
            // Just update the Meshans data without changing email
            updateMeshansData(updates);
        }
    }

    /**
     * Updates the user's email in Firebase Authentication.
     * <p>
     * This method:
     * <ul>
     *     <li>Updates the email in Firebase Auth</li>
     *     <li>Then updates the Meshans database record</li>
     * </ul>
     * This two-step process ensures consistency between Auth and database.
     * </p>
     *
     * @param newEmail The new email address to set
     * @param updates Map of user profile updates to apply after email change
     */
    private void updateEmailInFirebaseAuth(String newEmail, Map<String, Object> updates) {
        // Update the email in Firebase Auth
        currentUser.updateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AccountsSection", "Email updated in Firebase Auth");
                    
                    // Now update the Meshans data
                    updateMeshansData(updates);
                })
                .addOnFailureListener(e -> {
                    Log.e("AccountsSection", "Failed to update email in Firebase Auth: " + e.getMessage());
                    
                    // Check if we need to re-authenticate
                    if (e.getMessage() != null && e.getMessage().contains("requires recent authentication")) {
                        // Need to re-authenticate
                        promptForReauthentication(newEmail, updates);
                    } else {
                        hideLoadingState();
                        Toast.makeText(this, "Failed to update email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        saveButton.setEnabled(true);
                    }
                });
    }

    /**
     * Re-authenticates the user and then updates their email.
     * <p>
     * This method is used when changing the email requires recent authentication.
     * </p>
     *
     * @param password The user's current password for re-authentication
     * @param newEmail The new email address to set
     * @param updates Map of user profile updates to apply after email change
     */
    private void reauthenticateAndUpdateEmail(String password, String newEmail, Map<String, Object> updates) {
        // Create credentials with the current email and provided password
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
        
        // Re-authenticate
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Re-authentication successful, now update the email
                    updateEmailInFirebaseAuth(newEmail, updates);
                })
                .addOnFailureListener(e -> {
                    hideLoadingState();
                    Toast.makeText(this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);
                });
    }

    /**
     * Updates the user's profile data in the Meshans database.
     * <p>
     * This method updates the Room database and Firebase with the provided changes.
     * </p>
     *
     * @param updates Map of user profile updates to apply
     */
    private void updateMeshansData(Map<String, Object> updates) {
        if (updates.isEmpty()) {
            hideLoadingState();
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        showLoadingState("Updating profile...");

        meshansRepository.editUserDetails(currentUser.getUid(), updates)
                .addOnSuccessListener(aVoid -> {
                    // Update successful, refresh local data
                    shouldRefreshData = true;
                    
                    // Update the SharedPreferences cache with the new values
                    new Thread(() -> {
                        try {
                            SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            
                            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                                String key = entry.getKey();
                                Object value = entry.getValue();
                                
                                if (key.equals("username") && value instanceof String) {
                                    editor.putString(currentUser.getUid() + "_username", (String)value);
                                    if (currentMeshansUser != null) {
                                        currentMeshansUser.setUsername((String)value);
                                    }
                                } else if (key.equals("email") && value instanceof String) {
                                    editor.putString(currentUser.getUid() + "_email", (String)value);
                                    if (currentMeshansUser != null) {
                                        currentMeshansUser.setEmail((String)value);
                                    }
                                } else if (key.equals("profilePictureUrl") && value instanceof String) {
                                    editor.putString(currentUser.getUid() + "_profilePictureUrl", (String)value);
                                    if (currentMeshansUser != null) {
                                        currentMeshansUser.setProfilePictureUrl((String)value);
                                    }
                                } else if (key.equals("isPremium") && value instanceof Boolean) {
                                    editor.putBoolean(currentUser.getUid() + "_isPremium", (Boolean)value);
                                    if (currentMeshansUser != null) {
                                        currentMeshansUser.setPremium((Boolean)value);
                                    }
                                }
                            }
                            
                            editor.apply();
                        } catch (Exception e) {
                            Log.e("AccountsSection", "Error updating SharedPreferences: " + e.getMessage());
                        }
                    }).start();
                    
                    runOnUiThread(() -> {
                        hideLoadingState();
                        Toast.makeText(AccountsSection.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        saveButton.setEnabled(true);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("AccountsSection", "Error updating profile: " + e.getMessage());
                    runOnUiThread(() -> {
                        hideLoadingState();
                        Toast.makeText(AccountsSection.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        saveButton.setEnabled(true);
                    });
                });
    }

    /**
     * Prompts the user to re-authenticate when changing sensitive account information.
     * <p>
     * This method creates a dialog to collect the user's password for re-authentication.
     * </p>
     *
     * @param newEmail The new email address to set after re-authentication
     * @param updates Map of user profile updates to apply after re-authentication
     */
    private void promptForReauthentication(String newEmail, Map<String, Object> updates) {
        // Create an EditText for the password input
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Current password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Create and show the dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Re-authentication Required")
                .setMessage("Please enter your current password to change your email address.")
                .setView(passwordInput)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (!password.isEmpty()) {
                        reauthenticateAndUpdateEmail(password, newEmail, updates);
                    } else {
                        Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
                        saveButton.setEnabled(true);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    hideLoadingState();
                    saveButton.setEnabled(true);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Checks if network connectivity is available.
     * <p>
     * Used to determine whether to attempt network operations or fall back to cached data.
     * </p>
     *
     * @return boolean indicating if the device has network connectivity
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Opens the system image picker to select a new profile picture.
     * <p>
     * Uses the imagePickerLauncher to handle the result of the selection.
     * </p>
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Loads the selected image into the profile picture view.
     * <p>
     * Uses Glide to efficiently load and display the image from the provided URI.
     * </p>
     *
     * @param imageUri The URI of the selected image
     */
    private void loadImageIntoView(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.icon_mesha)
                .into(profilePictureImageView);
    }

    /**
     * Shows the loading state in the UI.
     * <p>
     * Displays a progress bar and status message, and disables input controls.
     * </p>
     *
     * @param message The message to display during loading
     */
    private void showLoadingState(String message) {
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText(message);
        disableInputs(true);
    }

    /**
     * Updates the progress status message in the UI.
     *
     * @param message The message to display
     */
    private void updateProgressStatus(String message) {
        runOnUiThread(() -> {
            statusTextView.setText(message);
        });
    }

    /**
     * Hides the loading state in the UI.
     * <p>
     * Hides the progress bar and status message, and re-enables input controls.
     * </p>
     */
    private void hideLoadingState() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            statusTextView.setVisibility(View.GONE);
            disableInputs(false);
        });
    }

    /**
     * Enables or disables user input controls.
     * <p>
     * Used during loading operations to prevent user interaction.
     * </p>
     *
     * @param disable Whether to disable the input controls
     */
    private void disableInputs(boolean disable) {
        usernameEditText.setEnabled(!disable);
        emailEditText.setEnabled(!disable);
        changeProfilePictureButton.setEnabled(!disable);
        saveButton.setEnabled(!disable);
    }
}