package com.ragsmc.launcher.auth;

public class Account {

    private String id;
    private String username;
    private String uuid;
    private String type = "offline";
    private boolean selected;

    public Account() {
    }

    public Account(String id, String username, String uuid) {
        this.id = id;
        this.username = username;
        this.uuid = uuid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}