-- 1. Add Contact and Company relationships to Projects
ALTER TABLE projects ADD COLUMN client_id BIGINT;
ALTER TABLE projects ADD CONSTRAINT fk_projects_client FOREIGN KEY (client_id) REFERENCES contacts(id);

ALTER TABLE projects ADD COLUMN installer_id BIGINT;
ALTER TABLE projects ADD CONSTRAINT fk_projects_installer FOREIGN KEY (installer_id) REFERENCES contacts(id);

ALTER TABLE projects ADD COLUMN equipment_dealer_id BIGINT;
ALTER TABLE projects ADD CONSTRAINT fk_projects_equipment_dealer FOREIGN KEY (equipment_dealer_id) REFERENCES companies(id);

-- 2. Add Embedded Address fields to Projects
ALTER TABLE projects ADD COLUMN address_text VARCHAR(255);
ALTER TABLE projects ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE projects ADD COLUMN longitude DOUBLE PRECISION;

-- 3. Add the description field to the existing Project Media table
ALTER TABLE project_media ADD COLUMN description VARCHAR(1000);