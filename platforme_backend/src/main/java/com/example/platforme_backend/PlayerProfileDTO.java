package com.example.platforme_backend;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerProfileDTO {

    private Long id;
    private String name;
    private Integer age;
    private String position;
    private String nationality;
    private String clubName;
    private String image;
    private String marketValue;
    private Integer marketValueNumeric;
    private String confidence;

    // Map dynamique pour les statistiques (clé = nom de stat, valeur = valeur correspondante)
    private Map<String, Object> stats;

    // Historique de la valeur marchande
    private List<ValueHistoryDTO> valueHistory;

    // Constructeurs
    public PlayerProfileDTO() {}

    public PlayerProfileDTO(Long id, String name, Integer age, String position, String nationality,
                            String clubName, String image, String marketValue, Integer marketValueNumeric,
                            String confidence, Map<String, Object> stats, List<ValueHistoryDTO> valueHistory) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.position = position;
        this.nationality = nationality;
        this.clubName = clubName;
        this.image = image;
        this.marketValue = marketValue;
        this.marketValueNumeric = marketValueNumeric;
        this.confidence = confidence;
        this.stats = stats;
        this.valueHistory = valueHistory;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getMarketValue() { return marketValue; }
    public void setMarketValue(String marketValue) { this.marketValue = marketValue; }

    public Integer getMarketValueNumeric() { return marketValueNumeric; }
    public void setMarketValueNumeric(Integer marketValueNumeric) { this.marketValueNumeric = marketValueNumeric; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }

    public List<ValueHistoryDTO> getValueHistory() { return valueHistory; }
    public void setValueHistory(List<ValueHistoryDTO> valueHistory) { this.valueHistory = valueHistory; }
}
