package viettel.dac.toolserviceregistry.model.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

/**
 * Response model for tool queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolQueryResponse {
    @Builder.Default
    private List<ToolSummary> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
