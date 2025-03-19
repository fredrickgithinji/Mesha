package com.dzovah.mesha.Database.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "Meshans")
public class Meshans {
    @PrimaryKey
    @NonNull
    private String userId; // Firebase UID
    private String username;
    private String email;
    private String profilePictureUrl; // URL to the profile picture (stored in Firebase Storage)
    private boolean isPremium; // Add this field

    // Default constructor (required for Firebase)
    public Meshans() {
        this.userId = "firebase_uid"; // Initialize with empty string
        this.isPremium = false; // Default value
    }

    // Parameterized constructor
    public Meshans(@NonNull String userId, String username, String email, String profilePictureUrl, boolean isPremium) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.isPremium = isPremium;
    }

    // Getters and Setters
    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}