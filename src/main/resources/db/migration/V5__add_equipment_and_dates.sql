-- Add automatic dates to projects
ALTER TABLE projects ADD COLUMN finish_date TIMESTAMP;

-- Create the new Equipment table
CREATE TABLE equipment (
                           id BIGSERIAL PRIMARY KEY,
                           project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
                           name VARCHAR(255) NOT NULL,
                           serial_number VARCHAR(255),
                           warranty_months INT,
                           notes TEXT
);