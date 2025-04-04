-- V1.3.0__Add_Tool_Type.sql
ALTER TABLE tool
    ADD COLUMN tool_type VARCHAR(20) NOT NULL DEFAULT 'OTHER';

-- Add index for better query performance
CREATE INDEX idx_tool_type ON tool (tool_type);