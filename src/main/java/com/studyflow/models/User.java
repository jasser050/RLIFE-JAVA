package com.studyflow.models;

public class User {
    private int id;
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String phoneNumber;
    private String bio;
    private String gender;
    private String university;
    private String studentId;
    private String profilePic;
    private int coins;
    private boolean banned;
    private String banReason;
    private String createdAt;

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getInitials() {
        String f = (firstName != null && !firstName.isEmpty()) ? String.valueOf(firstName.charAt(0)) : "";
        String l = (lastName != null && !lastName.isEmpty()) ? String.valueOf(lastName.charAt(0)) : "";
        return (f + l).toUpperCase();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', name='" + getFullName() + "'}";
    }
}
