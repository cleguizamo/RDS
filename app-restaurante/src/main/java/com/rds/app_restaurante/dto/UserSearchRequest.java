package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {
    private String name;
    private String email;
    private String documentNumber;
    private String phone;
    private Long minPoints;
    private Long maxPoints;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection; // ASC, DESC
}

