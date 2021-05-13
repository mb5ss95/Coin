package com.example.coin.model;

public class UserAccount {
    private String idToken;   //Firebase Uid (고유 토큰정보)
    private String emailId;   //이메일 아이디
    private String password;  //비밀번호
    private String friends;
    private String rooms;


    public UserAccount() {
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setFriends(String friends) {
        this.friends = friends;
    }
    public void setRooms(String rooms) {
        this.rooms = rooms;
    }

    public String getIdToken() {
        return idToken;
    }
    public String getEmailId() {
        return emailId;
    }
    public String getPassword() {
        return password;
    }
    public String getFriends() {
        return friends;
    }
    public String getRooms() {
        return rooms;
    }

}