package com.fitcoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSeedResponse {
    private int exercisesSaved;
    private int ingredientsSaved;
    private boolean replacedExisting;
    private String detail;
}
