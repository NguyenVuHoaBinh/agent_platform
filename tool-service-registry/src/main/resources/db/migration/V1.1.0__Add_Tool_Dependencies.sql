-- Tool Dependency Table
CREATE TABLE tool_dependency (
                                 id VARCHAR(36) PRIMARY KEY,
                                 tool_id VARCHAR(36) NOT NULL,
                                 dependency_tool_id VARCHAR(36) NOT NULL,
                                 dependency_type VARCHAR(20) NOT NULL,
                                 description TEXT,
                                 FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE,
                                 FOREIGN KEY (dependency_tool_id) REFERENCES tool(id) ON DELETE CASCADE,
                                 UNIQUE INDEX idx_tool_dependency (tool_id, dependency_tool_id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Parameter Mapping Table
CREATE TABLE parameter_mapping (
                                   id VARCHAR(36) PRIMARY KEY,
                                   dependency_id VARCHAR(36) NOT NULL,
                                   source_parameter VARCHAR(100) NOT NULL,
                                   target_parameter VARCHAR(100) NOT NULL,
                                   FOREIGN KEY (dependency_id) REFERENCES tool_dependency(id) ON DELETE CASCADE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;