package com.dzovah.mesha.Methods.Utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;

import com.dzovah.mesha.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthManager {

    private static final String TAG = "AuthManager";
    private final FirebaseAuth auth;
    private final GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    public AuthManager(Context context) {
        this.auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        this.googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    // Email/Password Sign-Up
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

    // Email/Password Sign-In
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

    // Google Sign-In Intent
    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    // Handle Google Sign-In Result
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

    // Sign-Out
    public void signOut() {
        auth.signOut();
        googleSignInClient.signOut();
    }

    // Check if a user is signed in
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // Interface for authentication callbacks
    public interface OnAuthCompleteListener {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception exception);
    }

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

}
