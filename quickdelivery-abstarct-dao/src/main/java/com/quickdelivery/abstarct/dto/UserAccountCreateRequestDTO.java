package com.quickdelivery.abstarct.dto;

import java.util.Locale;

public class UserAccountCreateRequestDTO {
    private UserDTO user;
    private Locale locale;

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}
