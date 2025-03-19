package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

public class SignUpActivity extends AppCompatActivity {

    private AuthManager authManager;
    private MeshansRepository meshansRepository;
    private EditText emailEditText, passwordEditText;
    private Button signUpButton;
    private TextView signInTextView;
    private static final int RC_SIGN_IN = 9001;
    private Button googleSignUpButton;

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

        TextView continueWithoutSignUpText = findViewById(R.id.continueWithoutSignUpText);
        continueWithoutSignUpText.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, Dashboard.class));
            finish();
        });
    }

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

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }

    private void startGoogleSignIn() {
        Intent signInIntent = authManager.getGoogleSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

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