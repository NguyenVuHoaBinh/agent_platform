ALTER TABLE tool_parameter
    ADD COLUMN parameter_source VARCHAR(20) NOT NULL DEFAULT 'USER_INPUT',
ADD COLUMN min_value VARCHAR(100),
ADD COLUMN max_value VARCHAR(100),
ADD COLUMN min_length INT,
ADD COLUMN max_length INT,
ADD COLUMN allowed_values TEXT,
ADD COLUMN format_hint VARCHAR(255),
ADD COLUMN is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN is_array BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN array_item_type VARCHAR(50),
ADD COLUMN object_schema TEXT,
ADD COLUMN extraction_path VARCHAR(255);

-- Update parameter_type column to use the enum values
ALTER TABLE tool_parameter
    MODIFY COLUMN parameter_type VARCHAR(20);

-- Create index for common query patterns
CREATE INDEX idx_param_source ON tool_parameter (parameter_source);
CREATE INDEX idx_param_required ON tool_parameter (required);
CREATE INDEX idx_param_sensitive ON tool_parameter (is_sensitive);