package com.dzovah.mesha.Methods.Utils;

import android.content.Context;
import android.content.Intent;

import com.dzovah.mesha.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Authentication manager for handling user authentication in the Mesha application.
 * <p>
 * This class provides a unified interface for various authentication methods, including:
 * <ul>
 *   <li>Email and password authentication</li>
 *   <li>Google Sign-In</li>
 *   <li>Password reset functionality</li>
 * </ul>
 * </p>
 * <p>
 * The class encapsulates Firebase Authentication and Google Sign-In implementations,
 * providing simplified methods that handle the complexities of authentication flows
 * and callback management.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see FirebaseAuth
 * @see GoogleSignInClient
 */
public class AuthManager {

    /** Tag for logging purposes */
    private static final String TAG = "AuthManager";
    
    /** Firebase Authentication instance */
    private final FirebaseAuth auth;
    
    /** Google Sign-In client for authentication with Google */
    private final GoogleSignInClient googleSignInClient;
    
    /** Request code for Google Sign-In intent */
    private static final int RC_SIGN_IN = 9001;

    /**
     * Constructs a new AuthManager instance.
     * <p>
     * Initializes Firebase Authentication and configures Google Sign-In options
     * with the default web client ID from resources.
     * </p>
     *
     * @param context The application context, used to initialize GoogleSignInClient
     */
    public AuthManager(Context context) {
        this.auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        this.googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    /**
     * Registers a new user with email and password.
     * <p>
     * Creates a new user account with the provided email and password
     * using Firebase Authentication. This is an alias for createUserWithEmail.
     * </p>
     *
     * @param email User's email address
     * @param password User's password
     * @param listener Callback to handle success or failure
     */
    public void signUpWithEmail(String email, String password, OnAuthCompleteListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(auth.getCurrentUser());
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    /**
     * Signs in an existing user with email and password.
     * <p>
     * Authenticates a user using their email and password credentials
     * through Firebase Authentication.
     * </p>
     *
     * @param email User's email address
     * @param password User's password
     * @param listener Callback to handle success or failure
     */
    public void signInWithEmail(String email, String password, OnAuthCompleteListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(auth.getCurrentUser());
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    /**
     * Returns an Intent for starting the Google Sign-In flow.
     * <p>
     * This method signs out any existing Google Sign-In session before
     * creating a new sign-in intent to ensure a clean authentication flow.
     * </p>
     *
     * @return An Intent that should be used with startActivityForResult to begin the sign-in process
     */
    public Intent getGoogleSignInIntent() {
        googleSignInClient.signOut();
        return googleSignInClient.getSignInIntent();
    }

    /**
     * Processes the result from a Google Sign-In activity.
     * <p>
     * This method should be called from the onActivityResult method of the activity
     * that started the Google Sign-In flow. It extracts the GoogleSignInAccount from
     * the task result and uses it to authenticate with Firebase.
     * </p>
     *
     * @param task Task containing the GoogleSignInAccount result
     * @param listener Callback to handle success or failure
     */
    public void handleGoogleSignInResult(Task<GoogleSignInAccount> task, OnAuthCompleteListener listener) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            auth.signInWithCredential(credential)
                    .addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            listener.onSuccess(auth.getCurrentUser());
                        } else {
                            listener.onFailure(authTask.getException());
                        }
                    });
        } catch (ApiException e) {
            listener.onFailure(e);
        }
    }

    /**
     * Signs out the current user from both Firebase and Google.
     * <p>
     * This method signs the user out of both Firebase Authentication
     * and Google Sign-In to ensure a complete logout.
     * </p>
     */
    public void signOut() {
        auth.signOut();
        googleSignInClient.signOut();
    }

    /**
     * Gets the currently signed-in user.
     * <p>
     * Retrieves the currently authenticated FirebaseUser, or null if no user
     * is currently signed in.
     * </p>
     *
     * @return The current FirebaseUser if signed in, or null otherwise
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Interface for authentication operation callbacks.
     * <p>
     * This interface provides methods to handle both successful and failed
     * authentication operations.
     * </p>
     */
    public interface OnAuthCompleteListener {
        /**
         * Called when authentication succeeds.
         * 
         * @param user The authenticated FirebaseUser (may be null for some operations)
         */
        void onSuccess(FirebaseUser user);
        
        /**
         * Called when authentication fails.
         * 
         * @param exception The exception that caused the failure
         */
        void onFailure(Exception exception);
    }

    /**
     * Sends a password reset email to the specified email address.
     * <p>
     * This method initiates the password reset flow by sending an email
     * to the user with instructions on how to reset their password.
     * </p>
     *
     * @param email Email address of the user requesting password reset
     * @param listener Callback to handle success or failure
     */
    public void resetPassword(String email, OnAuthCompleteListener listener) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(null);
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    /**
     * Creates a new user with email and password.
     * <p>
     * This method creates a new user account with the provided email and
     * password using Firebase Authentication.
     * </p>
     *
     * @param email User's email address
     * @param password User's password
     * @param listener Callback to handle success or failure
     */
    public void createUserWithEmail(String email, String password, OnAuthCompleteListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(auth.getCurrentUser());
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }
}
