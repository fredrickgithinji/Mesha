package com.dzovah.mesha.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.dzovah.mesha.R;
import com.dzovah.mesha.PActivities.PDashboard;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.concurrent.Executor;

/**
 * Activity for authenticating the user before accessing the app.
 * <p>
 * This activity is displayed when the app is launched and security settings are enabled.
 * It provides two authentication methods:
 * <ul>
 *     <li>PIN entry - user enters the 4-character PIN they previously set</li>
 *     <li>Fingerprint authentication - if enabled, user can authenticate with fingerprint</li>
 * </ul>
 * Upon successful authentication, the user is directed to the appropriate dashboard.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see UserPrefsActivity
 * @see Dashboard
 * @see PDashboard
 */
public class AuthenticationActivity extends AppCompatActivity {
    /** Shared preferences name for security settings */
    private static final String SECURITY_PREFS = "MeshaSecurity";
    
    /** Key for storing the PIN in shared preferences */
    private static final String PIN_KEY = "pin_code";
    
    /** Key for storing fingerprint setting in shared preferences */
    private static final String FINGERPRINT_ENABLED_KEY = "fingerprint_enabled";
    
    /** Key for storing last dashboard preference */
    private static final String LAST_DASHBOARD_KEY = "last_dashboard";
    
    /** Constant for regular dashboard */
    private static final String DASHBOARD_REGULAR = "regular";
    
    /** Constant for hidden dashboard */
    private static final String DASHBOARD_HIDDEN = "hidden";
    
    /** EditText for entering the PIN */
    private EditText etPin;
    
    /** TextView for displaying instruction or status messages */
    private TextView tvInstructions;
    
    /** Shared preferences for security settings */
    private SharedPreferences securityPrefs;
    
    /** Stored PIN for verification */
    private String savedPin;
    
    /** Flag indicating whether fingerprint is enabled */
    private boolean isFingerprintEnabled;
    
    /** Biometric prompt for fingerprint authentication */
    private BiometricPrompt biometricPrompt;
    
    /** Prompt info for displaying fingerprint dialog */
    private BiometricPrompt.PromptInfo promptInfo;
    
    /** Toggle button for switching between regular and hidden dashboards */
    private Button btnToggleDashboard;
    
    /** Current dashboard type */
    private String currentDashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load security preferences
        securityPrefs = getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE);
        savedPin = securityPrefs.getString(PIN_KEY, null);
        isFingerprintEnabled = securityPrefs.getBoolean(FINGERPRINT_ENABLED_KEY, false);
        currentDashboard = securityPrefs.getString(LAST_DASHBOARD_KEY, DASHBOARD_REGULAR);
        
        // If no security methods are set, skip to dashboard
        if ((savedPin == null || savedPin.isEmpty()) && !isFingerprintEnabled) {
            proceedToDashboard();
            return;
        }
        
        setContentView(R.layout.activity_authentication);
        
        // Initialize views
        etPin = findViewById(R.id.etAuthPin);
        tvInstructions = findViewById(R.id.tvAuthInstructions);
        Button btnVerifyPin = findViewById(R.id.btnVerifyPin);
        ShapeableImageView btnUseFingerprint = findViewById(R.id.btnUseFingerprint);

        
        // Set up PIN verification
        btnVerifyPin.setOnClickListener(v -> verifyPin());
        
        // Set up fingerprint authentication
        if (isFingerprintEnabled) {
            btnUseFingerprint.setVisibility(View.VISIBLE);
            btnUseFingerprint.setOnClickListener(v -> showFingerprintPrompt());
            setupBiometricAuthentication();
        } else {
            btnUseFingerprint.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up biometric (fingerprint) authentication.
     */
    private void setupBiometricAuthentication() {
        Executor executor = ContextCompat.getMainExecutor(this);
        
        biometricPrompt = new BiometricPrompt(this, executor, 
                new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Only show error toast if it's not a user cancellation
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(AuthenticationActivity.this, 
                            "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(AuthenticationActivity.this, 
                        "Authentication successful", Toast.LENGTH_SHORT).show();
                proceedToDashboard();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(AuthenticationActivity.this, 
                        "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Verify your fingerprint to access Mesha")
                .setNegativeButtonText("Cancel")
                .build();
    }

    /**
     * Shows the fingerprint authentication prompt.
     */
    private void showFingerprintPrompt() {
        if (biometricPrompt != null && promptInfo != null) {
            biometricPrompt.authenticate(promptInfo);
        }
    }

    /**
     * Verifies the PIN entered by the user.
     */
    private void verifyPin() {
        String enteredPin = etPin.getText().toString();
        
        // Validate PIN input
        if (TextUtils.isEmpty(enteredPin)) {
            Toast.makeText(this, "Please enter your PIN", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verify PIN
        if (enteredPin.equals(savedPin)) {
            Toast.makeText(this, "PIN correct", Toast.LENGTH_SHORT).show();
            proceedToDashboard();
        } else {
            Toast.makeText(this, "Incorrect PIN, please try again", Toast.LENGTH_SHORT).show();
            etPin.setText("");
        }
    }

    /**
     * Proceeds to the dashboard after successful authentication.
     * Navigation depends on whether the user selected the regular
     * or hidden dashboard.
     */
    private void proceedToDashboard() {
        Intent intent;
        
        // Start the appropriate dashboard based on user selection
        if (currentDashboard.equals(DASHBOARD_HIDDEN)) {
            intent = new Intent(this, PDashboard.class);
            Toast.makeText(this, "Launching hidden dashboard", Toast.LENGTH_SHORT).show();
        } else {
            intent = new Intent(this, Dashboard.class);
        }
        
        startActivity(intent);
        finish();
    }
}
