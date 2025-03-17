package com.dzovah.mesha.Database.Entities;

public class Meshans {
    private String userId; // Firebase UID
    private String username;
    private String email;
    private String profilePictureUrl; // URL to the profile picture (stored in Firebase Storage)

    // Default constructor (required for Firebase)
    public Meshans() {}

    // Parameterized constructor
    public Meshans(String userId, String username, String email, String profilePictureUrl) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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
}