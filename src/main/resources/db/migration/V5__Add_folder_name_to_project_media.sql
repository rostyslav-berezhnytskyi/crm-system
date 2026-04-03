-- Add the folder_name column to the project_media table to allow for virtual folders
ALTER TABLE project_media
ADD COLUMN folder_name VARCHAR(255);
