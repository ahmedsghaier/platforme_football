package com.example.platforme_backend;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class ExportLogDTO {
    private Long userId;
    private String exportFormat;
    private String dataType;
    private List<Integer> playerIds;
    private String timestamp;

    public ExportLogDTO() {}

}
