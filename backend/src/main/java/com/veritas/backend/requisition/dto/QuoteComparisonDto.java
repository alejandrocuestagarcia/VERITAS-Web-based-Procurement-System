package com.veritas.backend.requisition.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuoteComparisonDto {
    private Long requestId;
    private List<Object> quotes;
    private Double marketAverage;
}
