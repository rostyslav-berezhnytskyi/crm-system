-- Create a junction table for Many-to-Many relationship between Projects and Contacts
CREATE TABLE project_additional_contacts (
                                             project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
                                             contact_id BIGINT REFERENCES contacts(id) ON DELETE CASCADE,
                                             PRIMARY KEY (project_id, contact_id)
);