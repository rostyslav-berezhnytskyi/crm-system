-- Expand file_type to fit Microsoft Office MIME types
ALTER TABLE project_media ALTER COLUMN file_type TYPE VARCHAR(255);