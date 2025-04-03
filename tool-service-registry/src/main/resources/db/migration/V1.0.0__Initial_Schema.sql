-- Tool Table
CREATE TABLE tool (
                      id VARCHAR(36) PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      description TEXT NOT NULL,
                      active BOOLEAN NOT NULL DEFAULT TRUE,
                      version INT NOT NULL DEFAULT 1,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      UNIQUE INDEX idx_tool_name (name),
                      INDEX idx_tool_active (active)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tool Parameter Table
CREATE TABLE tool_parameter (
                                id VARCHAR(36) PRIMARY KEY,
                                tool_id VARCHAR(36) NOT NULL,
                                name VARCHAR(255) NOT NULL,
                                description TEXT NOT NULL,
                                parameter_type VARCHAR(50) NOT NULL,
                                required BOOLEAN NOT NULL DEFAULT FALSE,
                                default_value TEXT,
                                FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE,
                                UNIQUE INDEX idx_tool_param (tool_id, name),
                                INDEX idx_param_name (name)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tool Category Table
CREATE TABLE tool_category (
                               id VARCHAR(36) PRIMARY KEY,
                               name VARCHAR(255) NOT NULL,
                               description TEXT,
                               UNIQUE INDEX idx_category_name (name)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tool Category Mapping Table
CREATE TABLE tool_category_mapping (
                                       tool_id VARCHAR(36) NOT NULL,
                                       category_id VARCHAR(36) NOT NULL,
                                       PRIMARY KEY (tool_id, category_id),
                                       FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE,
                                       FOREIGN KEY (category_id) REFERENCES tool_category(id) ON DELETE CASCADE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Insert default categories
INSERT INTO tool_category (id, name, description) VALUES
                                                      (UUID(), 'Analysis', 'Tools for analyzing data and content'),
                                                      (UUID(), 'Generation', 'Tools for generating content'),
                                                      (UUID(), 'Processing', 'Tools for processing and transforming data'),
                                                      (UUID(), 'Integration', 'Tools for integrating with external systems'),
                                                      (UUID(), 'Utility', 'Utility tools for common operations');