package com.dzovah.mesha.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private AuthManager authManager;
    private MeshansRepository meshansRepository;
    private EditText emailEditText, passwordEditText;
    private Button signInButton, googleSignInButton;
    private TextView signUpTextView, passwordResetTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        // Initialize AuthManager
        authManager = new AuthManager(this);

        // Initialize MeshansRepository
        Firebase_Meshans_Data_linkDao firebaseDao = new Firebase_Meshans_Data_linkDao();
        MeshansDao roomDao = MeshaDatabase.Get_database(this).meshansDao();
        meshansRepository = new MeshansRepository(firebaseDao, roomDao);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signinButton);
        signUpTextView = findViewById(R.id.signupTextView);
        passwordResetTextView = findViewById(R.id.Passwordreset);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        // Set click listeners
        signInButton.setOnClickListener(v -> signIn());
        signUpTextView.setOnClickListener(v -> navigateToSignUp());
        passwordResetTextView.setOnClickListener(v -> resetPassword());
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());

        TextView continueWithoutSignInText = findViewById(R.id.continueWithoutSignInText);
        continueWithoutSignInText.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, Dashboard.class));
            finish();
        });
    }

    private void signIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a loading indicator (optional)
        // showLoadingIndicator();

        authManager.signInWithEmail(email, password, new AuthManager.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // User authenticated, now fetch user data from repository
                fetchUserDataAndNavigate(user);
            }

            @Override
            public void onFailure(Exception exception) {
                // hideLoadingIndicator();
                Toast.makeText(SignInActivity.this, "Sign-in failed: " + exception.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserDataAndNavigate(FirebaseUser user) {
        meshansRepository.getUser(user.getUid())
                .addOnSuccessListener(meshansUser -> {
                    // hideLoadingIndicator();
                    if (meshansUser != null) {
                        // User data found, navigate to dashboard
                        Toast.makeText(SignInActivity.this, "Sign-in successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, Dashboard.class));
                        finish();
                    } else {
                        // User authenticated but no data in Firebase
                        // This could happen if authentication succeeded but user creation failed
                        Toast.makeText(SignInActivity.this,
                                "Account authenticated but profile data not found",
                                Toast.LENGTH_SHORT).show();

                        // Create a new user profile
                        createNewUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> {
                    // hideLoadingIndicator();
                    Toast.makeText(SignInActivity.this,
                            "Failed to fetch user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Despite the error, we can still navigate to Dashboard as the user is authenticated
                    startActivity(new Intent(SignInActivity.this, Dashboard.class));
                    finish();
                });
    }

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
                    Toast.makeText(SignInActivity.this, "User profile created", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignInActivity.this, Dashboard.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignInActivity.this,
                            "Failed to create user profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Navigate to dashboard anyway
                    startActivity(new Intent(SignInActivity.this, Dashboard.class));
                    finish();
                });
    }

    // Also update the Google Sign-In handling to use the same flow
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

    private void navigateToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

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

    private void startGoogleSignIn() {
        Intent signInIntent = authManager.getGoogleSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


}