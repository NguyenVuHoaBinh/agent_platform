package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ApiParameterLocation;

/**
 * Entity representing a mapping between tool parameters and API parameters.
 */
@Entity
@Table(name = "api_parameter_mapping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterMapping {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "api_metadata_id")
    private ApiToolMetadata apiToolMetadata;

    @ManyToOne
    @JoinColumn(name = "tool_parameter_id")
    private ToolParameter toolParameter;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_location")
    private ApiParameterLocation apiLocation;

    @Column(name = "api_parameter_name")
    private String apiParameterName;

    @Column(name = "is_required_for_api")
    private boolean requiredForApi;

    @Column(name = "transformation_expression")
    private String transformationExpression;

    @Column(name = "response_extraction_path")
    private String responseExtractionPath;
}