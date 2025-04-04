CREATE TABLE api_auth_config (
                                 id VARCHAR(36) PRIMARY KEY,
                                 api_metadata_id VARCHAR(36) NOT NULL,
                                 auth_type VARCHAR(20) NOT NULL,
                                 name VARCHAR(100),
                                 description VARCHAR(500),
                                 is_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- API Key specific fields
                                 api_key VARCHAR(500),
                                 key_name VARCHAR(100),
                                 key_location VARCHAR(20),

    -- Basic Auth specific fields
                                 username VARCHAR(100),
                                 password VARCHAR(500),

    -- Bearer Token specific fields
                                 token VARCHAR(2000),
                                 token_prefix VARCHAR(20),

    -- OAuth2 specific fields
                                 client_id VARCHAR(100),
                                 client_secret VARCHAR(500),
                                 token_url VARCHAR(500),
                                 authorization_url VARCHAR(500),
                                 scope VARCHAR(500),
                                 grant_type VARCHAR(50),
                                 access_token VARCHAR(2000),
                                 refresh_token VARCHAR(2000),
                                 token_expiry BIGINT,

                                 FOREIGN KEY (api_metadata_id) REFERENCES api_tool_metadata(id) ON DELETE CASCADE,
                                 INDEX idx_auth_config_api_metadata (api_metadata_id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;