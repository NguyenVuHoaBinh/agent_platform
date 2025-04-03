-- Enhance Tool Parameter Table with additional fields
ALTER TABLE tool_parameter
    ADD COLUMN validation_pattern VARCHAR(255),
ADD COLUMN validation_message VARCHAR(255),
ADD COLUMN conditional_on VARCHAR(255),
ADD COLUMN priority INT DEFAULT 0,
ADD COLUMN examples TEXT,
ADD COLUMN suggestion_query VARCHAR(500);

-- Tool Example Table
CREATE TABLE tool_example (
                              id VARCHAR(36) PRIMARY KEY,
                              tool_id VARCHAR(36) NOT NULL,
                              input_text TEXT NOT NULL,
                              output_parameters JSON NOT NULL,
                              FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE,
                              INDEX idx_example_tool (tool_id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;