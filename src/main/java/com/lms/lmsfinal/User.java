package com.lms.lmsfinal;

import java.time.LocalDate;

public class User {
    private int userId;
    private String email;
    private String userRole;
    private LocalDate joinDate;

    public User(int userId, String email, String userRole, LocalDate joinDate) {
        this.userId = userId;
        this.email = email;
        this.userRole = userRole;
        this.joinDate = joinDate;
    }

    public int getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getUserRole() { return userRole; }
    public LocalDate getJoinDate() { return joinDate; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
}
