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
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private AuthManager authManager;
    private EditText emailEditText, passwordEditText;
    private Button signInButton, googleSignInButton;
    private TextView signUpTextView, passwordResetTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        authManager = new AuthManager(this);
        
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

        authManager.signInWithEmail(email, password, new AuthManager.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(SignInActivity.this, "Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignInActivity.this, Dashboard.class));
                finish();
            }

            @Override
            public void onFailure(Exception exception) {
                Toast.makeText(SignInActivity.this, "Sign-in failed: " + exception.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            authManager.handleGoogleSignInResult(task, new AuthManager.OnAuthCompleteListener() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    startActivity(new Intent(SignInActivity.this, Dashboard.class));
                    finish();
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