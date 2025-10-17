package com.example.platforme_backend;

public class ValueHistoryDTO {

    private String month;
    private Long value;

    // Constructeurs
    public ValueHistoryDTO() {}

    public ValueHistoryDTO(String month, Long value) {
        this.month = month;
        this.value = value;
    }

    // Getters et Setters
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
}

