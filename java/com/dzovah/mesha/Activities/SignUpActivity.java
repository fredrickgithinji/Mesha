package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dzovah.mesha.Methods.Utils.AuthManager;
import com.dzovah.mesha.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

import com.dzovah.mesha.Database.Entities.Meshans;
import com.dzovah.mesha.Database.Daos.MeshansDao;
import com.dzovah.mesha.Database.Daos.Firebase_Meshans_Data_linkDao;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Repositories.MeshansRepository;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

/**
 * Activity for handling user registration and account creation.
 * <p>
 * This activity provides user interface and logic for:
 * <ul>
 *     <li>Email/password account creation</li>
 *     <li>Google sign-up integration</li>
 *     <li>Navigation to sign-in</li>
 *     <li>Option to continue without signing up</li>
 * </ul>
 * After successful registration, a user profile is created in the database
 * and the user is directed to the Dashboard activity.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Dashboard
 * @see SignInActivity
 * @see AuthManager
 */
public class SignUpActivity extends AppCompatActivity {

    /** Manager for Firebase authentication operations */
    private AuthManager authManager;
    
    /** Repository for user data operations */
    private MeshansRepository meshansRepository;
    
    /** Input fields for email and password */
    private EditText emailEditText, passwordEditText;
    
    /** Button to trigger the sign-up process */
    private Button signUpButton;
    
    /** Text link for sign-in navigation */
    private TextView signInTextView;
    
    /** Request code for Google Sign-In */
    private static final int RC_SIGN_IN = 9001;
    
    /** Button for Google sign-up */
    private Button googleSignUpButton;

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
        setContentView(R.layout.activity_sign_up);
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton = findViewById(R.id.signupButton);
        signInTextView = findViewById(R.id.signinTextView);
        googleSignUpButton = findViewById(R.id.googleSignUpButton);
        passwordToggleIcon = findViewById(R.id.passwordToggleButton);
        // Initialize AuthManager
        authManager = new AuthManager(this);

        // Initialize MeshansRepository
        Firebase_Meshans_Data_linkDao firebaseDao = new Firebase_Meshans_Data_linkDao();
        MeshansDao roomDao = MeshaDatabase.Get_database(this).meshansDao();
        meshansRepository = new MeshansRepository(firebaseDao, roomDao);

        // Set click listeners
        signUpButton.setOnClickListener(v -> signUp());
        signInTextView.setOnClickListener(v -> navigateToSignIn());
        googleSignUpButton.setOnClickListener(v -> startGoogleSignIn());
        passwordToggleIcon.setOnClickListener(v -> togglePasswordVisibility());

        TextView continueWithoutSignUpText = findViewById(R.id.continueWithoutSignUpText);
        continueWithoutSignUpText.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, Dashboard.class));
            finish();
        });
        updateToggleIcon();
    }

    /**
     * Attempts to register a new user with the provided email and password.
     * <p>
     * Validates the input fields and uses the AuthManager to create
     * a new user account with Firebase Authentication. On success,
     * stores the user's profile data in the database.
     * </p>
     */
    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        authManager.signUpWithEmail(email, password, new AuthManager.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(SignUpActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                storeUserDetails(user);
            }

            @Override
            public void onFailure(Exception exception) {
                Toast.makeText(SignUpActivity.this, "Sign-up failed: " + exception.getMessage(),
                        Toast.LENGTH_SHORT).show();
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
     * Navigates to the SignInActivity.
     * <p>
     * Called when the user clicks on the sign-in text.
     * </p>
     */
    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
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
                    Toast.makeText(SignUpActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                    storeUserDetails(user);
                }

                @Override
                public void onFailure(Exception exception) {
                    Toast.makeText(SignUpActivity.this,
                            "Google sign up failed: " + exception.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Stores the user's profile data in the database.
     * <p>
     * Creates a new Meshans object with default values and the user's
     * authentication information, then saves it to both Firebase and Room
     * databases using the repository.
     * </p>
     *
     * @param user The authenticated Firebase user whose details to store
     */
    private void storeUserDetails(FirebaseUser user) {
        // Create new Meshans object with required fields
        Meshans meshan = new Meshans();
        meshan.setUserId(user.getUid()); // Set UID as document ID
        meshan.setEmail(user.getEmail()); // Set user's email
        meshan.setUsername("Meshan" + user.getUid().substring(0, 4)); // Default username
        meshan.setProfilePictureUrl("to edit"); // Default profile picture URL
        meshan.setPremium(false); // Default premium status

        // Save user details using repository with the improved method
        meshansRepository.saveUser(meshan)
                .addOnSuccessListener(aVoid -> {
                    // Both Firebase and Room operations completed successfully
                    Toast.makeText(SignUpActivity.this, "User details stored successfully", Toast.LENGTH_SHORT).show();
                    // Navigate to dashboard after successful save
                    startActivity(new Intent(SignUpActivity.this, Dashboard.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Failed to store user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Even if saving fails, still try to navigate to dashboard
                    startActivity(new Intent(SignUpActivity.this, Dashboard.class));
                    finish();
                });
    }
}