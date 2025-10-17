package com.example.platforme_backend;

public class AlertDTO {
    private String type;
    private String player;
    private String message;
    private String time;

    public AlertDTO(String type, String player, String message, String time) {
        this.type = type;
        this.player = player;
        this.message = message;
        this.time = time;
    }

    public String getType() { return type; }
    public String getPlayer() { return player; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
}