-- API Tool Metadata table
CREATE TABLE api_tool_metadata (
                                   id VARCHAR(36) PRIMARY KEY,
                                   tool_id VARCHAR(36) NOT NULL,
                                   base_url VARCHAR(255),
                                   endpoint_path VARCHAR(255),
                                   http_method VARCHAR(10),
                                   content_type VARCHAR(100),
                                   authentication_type VARCHAR(20),
                                   request_timeout_ms INT,
                                   response_format VARCHAR(50),
                                   rate_limit_requests INT,
                                   rate_limit_period_seconds INT,
                                   retry_count INT,
                                   retry_delay_ms INT,
                                   FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE,
                                   UNIQUE INDEX idx_api_tool_metadata_tool_id (tool_id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- API Headers table
CREATE TABLE api_header (
                            id VARCHAR(36) PRIMARY KEY,
                            api_metadata_id VARCHAR(36) NOT NULL,
                            header_name VARCHAR(100) NOT NULL,
                            header_value VARCHAR(500),
                            is_required BOOLEAN NOT NULL DEFAULT FALSE,
                            is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
                            FOREIGN KEY (api_metadata_id) REFERENCES api_tool_metadata(id) ON DELETE CASCADE,
                            INDEX idx_api_header_metadata_id (api_metadata_id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;