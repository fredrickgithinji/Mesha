package com.dzovah.mesha.Database.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Entity class representing user information in the Mesha application.
 * <p>
 * This class stores essential user data including authentication information,
 * user profile details, and subscription status.
 * </p>
 * 
 * @author Electra Magus
 * @version 1.0
 */
@Entity(tableName = "Meshans")
public class Meshans {
    @PrimaryKey
    @NonNull
    private String userId; // Firebase UID
    private String username;
    private String email;
    private String profilePictureUrl; // URL to the profile picture (stored in Firebase Storage)
    private boolean isPremium; // Add this field

    /**
     * Default constructor required for Firebase and Room database operations.
     * Initializes a user with default values.
     */
    public Meshans() {
        this.userId = "firebase_uid"; // Initialize with empty string
        this.isPremium = false; // Default value
    }

    /**
     * Parameterized constructor to create a fully initialized Meshans object.
     *
     * @param userId The unique Firebase user ID
     * @param username The username chosen by the user
     * @param email The email address of the user
     * @param profilePictureUrl URL to the user's profile picture in Firebase Storage
     * @param isPremium Boolean indicating whether the user has a premium subscription
     */
    public Meshans(@NonNull String userId, String username, String email, String profilePictureUrl, boolean isPremium) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.isPremium = isPremium;
    }

    /**
     * Gets the user's unique Firebase ID.
     *
     * @return The Firebase user ID
     */
    @NonNull
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user's unique Firebase ID.
     *
     * @param userId The Firebase user ID to set
     */
    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user's username.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's username.
     *
     * @param username The username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the user's email address.
     *
     * @return The email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email The email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the URL to the user's profile picture.
     *
     * @return The profile picture URL
     */
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    /**
     * Sets the URL to the user's profile picture.
     *
     * @param profilePictureUrl The profile picture URL to set
     */
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    /**
     * Checks if the user has a premium subscription.
     *
     * @return true if the user has a premium subscription, false otherwise
     */
    public boolean isPremium() {
        return isPremium;
    }

    /**
     * Sets the user's premium subscription status.
     *
     * @param premium true to set user as premium, false otherwise
     */
    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}