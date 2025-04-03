package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dzovah.mesha.Database.Daos.Firebase_Meshans_Data_linkDao;
import com.dzovah.mesha.Database.Daos.MeshansDao;
import com.dzovah.mesha.Database.Entities.Meshans;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Repositories.MeshansRepository;
import com.dzovah.mesha.Methods.Utils.AuthManager;
import com.dzovah.mesha.R;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

/**
 * Activity for handling user authentication and sign-in.
 * <p>
 * This activity provides user interface and logic for:
 * <ul>
 *     <li>Email/password sign-in</li>
 *     <li>Google sign-in integration</li>
 *     <li>Password reset functionality</li>
 *     <li>Navigation to sign-up</li>
 *     <li>Option to continue without signing in</li>
 * </ul>
 * Once authenticated, users are directed to the Dashboard activity.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Dashboard
 * @see SignUpActivity
 * @see AuthManager
 */
public class SignInActivity extends AppCompatActivity {

    /** Request code for Google Sign-In */
    private static final int RC_SIGN_IN = 9001;
    
    /** Manager for Firebase authentication operations */
    private AuthManager authManager;
    
    /** Repository for user data operations */
    private MeshansRepository meshansRepository;
    
    /** Input fields for email and password */
    private EditText emailEditText, passwordEditText;
    
    /** Buttons for standard sign-in and Google sign-in */
    private Button signInButton, googleSignInButton;
    
    /** Text links for sign-up and password reset navigation */
    private TextView signUpTextView, passwordResetTextView;

    /** Image view to toggle password visibility */
    private ImageView passwordToggleIcon;

    /** Booloean for password visibility */
    private boolean passwordVisible = false;

    /**
     * Initializes the activity, sets up the UI components and event listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signinButton);
        signUpTextView = findViewById(R.id.signupTextView);
        passwordResetTextView = findViewById(R.id.Passwordreset);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        passwordToggleIcon = findViewById(R.id.passwordToggleButton);

        // Set click listeners
        signInButton.setOnClickListener(v -> signIn());
        signUpTextView.setOnClickListener(v -> navigateToSignUp());
        passwordResetTextView.setOnClickListener(v -> resetPassword());
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
        passwordToggleIcon.setOnClickListener(v -> togglePasswordVisibility());


        TextView continueWithoutSignInText = findViewById(R.id.continueWithoutSignInText);
        continueWithoutSignInText.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, Dashboard.class));
            finish();
        });

        updateToggleIcon();
        // Initialize dependencies asynchronously
        initializeDependenciesAsync();

    }
    
    /**
     * Initializes the AuthManager and MeshansRepository on a background thread
     * to avoid blocking the main thread during startup.
     */
    private void initializeDependenciesAsync() {
        new Thread(() -> {
            // Initialize AuthManager (lightweight operation, but still off main thread)
            authManager = new AuthManager(this);
            
            // Initialize MeshansRepository (database operations)
            Firebase_Meshans_Data_linkDao firebaseDao = new Firebase_Meshans_Data_linkDao();
            MeshansDao roomDao = MeshaDatabase.Get_database(this).meshansDao();
            meshansRepository = new MeshansRepository(firebaseDao, roomDao);
            
            // Enable UI buttons after initialization is complete
            runOnUiThread(() -> {
                signInButton.setEnabled(true);
                googleSignInButton.setEnabled(true);
            });
        }).start();
    }

    /**
     * Shows or hides a loading indicator.
     * 
     * @param show True to show loading indicator, false to hide it
     * @param message Optional message to display with the loading state
     */
    private void showLoading(boolean show, String message) {
        runOnUiThread(() -> {
            // Disable buttons during loading
            if (signInButton != null) {
                signInButton.setEnabled(!show);
            }
            if (googleSignInButton != null) {
                googleSignInButton.setEnabled(!show);
            }
            
            // Show loading message as a toast if provided
            if (show && message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

     /**
     * Toggles the visibility of the password and updates the toggle icon.
     * This method is called when the password toggle icon is clicked.
     */
     private void togglePasswordVisibility() {
         passwordVisible = !passwordVisible;
         updatePasswordVisibility();
         updateToggleIcon();
     }

    /**
     * Updates the visibility of the password in the EditText.
     * Sets the input type of the passwordEditText to show or hide the password.
     */
    private void updatePasswordVisibility() {
        int selection = passwordEditText.getSelectionEnd(); // Keep cursor position
        passwordEditText.setTransformationMethod(passwordVisible
                ? null
                : PasswordTransformationMethod.getInstance());
        passwordEditText.setSelection(selection); // Restore cursor position
    }

    /**
     * Updates the password toggle icon based on the password visibility state.
     * Sets the drawable of the passwordToggleIcon to either the "eye" or "closed eye" icon.
     */
    private void updateToggleIcon() {
        Drawable icon = ContextCompat.getDrawable(this, passwordVisible ? R.drawable.ic_eye : R.drawable.ic_closed_eye);
        passwordToggleIcon.setImageDrawable(icon);
    }


    /**
     * Attempts to sign in the user with the provided email and password.
     * <p>
     * Validates the input fields and uses the AuthManager to authenticate
     * the user with Firebase Authentication. On success, fetches the user's
     * data from the repository.
     * </p>
     */
    private void signIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        showLoading(true, "Signing in...");

        authManager.signInWithEmail(email, password, new AuthManager.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // User authenticated, now fetch user data from repository
                fetchUserDataAndNavigate(user);
            }

            @Override
            public void onFailure(Exception exception) {
                // Hide loading indicator
                showLoading(false, null);
                Toast.makeText(SignInActivity.this, "Sign-in failed: " + exception.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetches user data from the repository and navigates to the Dashboard.
     * <p>
     * After successful authentication, this method fetches the user's profile
     * data from Firebase. If no profile exists, it creates a new one.
     * </p>
     *
     * @param user The authenticated Firebase user
     */
    private void fetchUserDataAndNavigate(FirebaseUser user) {
        // Update loading message
        showLoading(true, "Loading profile...");
        
        meshansRepository.getUser(user.getUid())
                .addOnSuccessListener(meshansUser -> {
                    if (meshansUser != null) {
                        // User data found, navigate to dashboard
                        showLoading(false, null);
                        Toast.makeText(SignInActivity.this, "Sign-in successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, Dashboard.class));
                        finish();
                    } else {
                        // User authenticated but no data in Firebase
                        // Update loading message
                        showLoading(true, "Setting up profile...");
                        
                        // Create a new user profile
                        createNewUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false, null);
                    Log.e("SignInActivity", "Failed to fetch user data", e);
                    Toast.makeText(SignInActivity.this,
                            "Failed to fetch user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Despite the error, we can still navigate to Dashboard as the user is authenticated
                    startActivity(new Intent(SignInActivity.this, Dashboard.class));
                    finish();
                });
    }

    /**
     * Creates a new user profile in the database.
     * <p>
     * This method is called when a user successfully authenticates but
     * no corresponding user profile is found in the database.
     * </p>
     *
     * @param user The authenticated Firebase user for whom to create a profile
     */
    private void createNewUserProfile(FirebaseUser user) {
        // Create new Meshans object with required fields
        Meshans meshan = new Meshans();
        meshan.setUserId(user.getUid());
        meshan.setEmail(user.getEmail());
        meshan.setUsername("Meshan" + user.getUid().substring(0, 4));
        meshan.setProfilePictureUrl("to edit");
        meshan.setPremium(false);

        // Save user details using repository
        meshansRepository.saveUser(meshan)
                .addOnSuccessListener(aVoid -> {
                    // Profile created, now navigate to dashboard
                    showLoading(false, null);
                    Toast.makeText(SignInActivity.this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignInActivity.this, Dashboard.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false, null);
                    Log.e("SignInActivity", "Failed to create user profile", e);
                    Toast.makeText(SignInActivity.this,
                            "Failed to create profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    
                    // Despite the error, navigate to Dashboard as the user is authenticated
                    startActivity(new Intent(SignInActivity.this, Dashboard.class));
                    finish();
                });
    }

    /**
     * Navigates to the SignUpActivity.
     * <p>
     * Called when the user clicks on the sign-up text.
     * </p>
     */
    private void navigateToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    /**
     * Initiates the password reset process.
     * <p>
     * Validates that an email is provided and uses the AuthManager
     * to send a password reset email to the user.
     * </p>
     */
    private void resetPassword() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        authManager.resetPassword(email, new AuthManager.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(SignInActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception exception) {
                Toast.makeText(SignInActivity.this, "Failed to send reset email: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Initiates the Google Sign-In process.
     * <p>
     * Gets the Google Sign-In intent from the AuthManager and
     * starts the activity for result.
     * </p>
     */
    private void startGoogleSignIn() {
        Intent signInIntent = authManager.getGoogleSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Handles the result from the Google Sign-In activity.
     * <p>
     * Processes the result from the Google Sign-In intent and uses
     * the AuthManager to authenticate with Firebase using the Google credentials.
     * </p>
     *
     * @param requestCode The request code originally supplied to startActivityForResult
     * @param resultCode The result code returned by the child activity
     * @param data An Intent which can return result data to the caller
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            authManager.handleGoogleSignInResult(task, new AuthManager.OnAuthCompleteListener() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    // User authenticated with Google, now fetch user data
                    fetchUserDataAndNavigate(user);
                }

                @Override
                public void onFailure(Exception exception) {
                    Toast.makeText(SignInActivity.this,
                            "Google sign in failed: " + exception.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}