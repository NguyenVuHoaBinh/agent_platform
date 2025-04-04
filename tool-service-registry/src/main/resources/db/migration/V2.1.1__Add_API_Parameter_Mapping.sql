CREATE TABLE api_parameter_mapping (
                                       id VARCHAR(36) PRIMARY KEY,
                                       api_metadata_id VARCHAR(36) NOT NULL,
                                       tool_parameter_id VARCHAR(36) NOT NULL,
                                       api_location VARCHAR(20) NOT NULL,
                                       api_parameter_name VARCHAR(100) NOT NULL,
                                       is_required_for_api BOOLEAN NOT NULL DEFAULT FALSE,
                                       transformation_expression VARCHAR(500),
                                       response_extraction_path VARCHAR(255),
                                       FOREIGN KEY (api_metadata_id) REFERENCES api_tool_metadata(id) ON DELETE CASCADE,
                                       FOREIGN KEY (tool_parameter_id) REFERENCES tool_parameter(id) ON DELETE CASCADE,
                                       INDEX idx_api_param_mapping_metadata (api_metadata_id),
                                       INDEX idx_api_param_mapping_parameter (tool_parameter_id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;