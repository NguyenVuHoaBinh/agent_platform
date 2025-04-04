package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an HTTP header for API requests.
 */
@Entity
@Table(name = "api_header")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiHeader {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "api_metadata_id")
    private ApiToolMetadata apiToolMetadata;

    @Column(name = "header_name")
    private String name;

    @Column(name = "header_value")
    private String value;

    @Column(name = "is_required")
    private boolean required;

    @Column(name = "is_sensitive")
    private boolean sensitive;
}