package com.lms.lmsfinal;

public class UserSummary {
    private String uid;
    private String email;
    private String role;

    public UserSummary() {}
    public UserSummary(String uid, String email, String role) {
        this.uid = uid;
        this.email = email;
        this.role = role;
    }

    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
}
