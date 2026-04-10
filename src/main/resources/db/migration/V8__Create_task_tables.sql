-- 1. Create the Task Groups (The Kanban Columns)
CREATE TABLE task_groups (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(100) NOT NULL,
                             display_order INTEGER NOT NULL,
                             color_hex VARCHAR(7)
);

-- 2. Create the Tasks Table
CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       priority VARCHAR(20) DEFAULT 'MEDIUM',
                       display_order INTEGER NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       due_date TIMESTAMP,
                       is_completed BOOLEAN NOT NULL DEFAULT FALSE,

    -- Foreign Keys linking to users
                       creator_id BIGINT NOT NULL,
                       assignee_id BIGINT,

    -- Foreign Key linking to the Kanban Column (Required)
                       group_id BIGINT NOT NULL,

    -- Foreign Keys linking to CRM Entities (Optional Context)
                       project_id BIGINT,
                       company_id BIGINT,
                       contact_id BIGINT,

    -- Self-referencing Foreign Key for Subtasks
                       parent_task_id BIGINT,

    -- Constraints
                       CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES users(id),
                       CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
                       CONSTRAINT fk_task_group FOREIGN KEY (group_id) REFERENCES task_groups(id),
                       CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects(id),
                       CONSTRAINT fk_task_company FOREIGN KEY (company_id) REFERENCES companies(id),
                       CONSTRAINT fk_task_contact FOREIGN KEY (contact_id) REFERENCES contacts(id),
                       CONSTRAINT fk_task_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id)
);

-- 3. Create the Attachments Table
CREATE TABLE task_attachments (
                                  id BIGSERIAL PRIMARY KEY,
                                  task_id BIGINT NOT NULL,
                                  file_name VARCHAR(255) NOT NULL,
                                  file_url VARCHAR(255) NOT NULL,
                                  uploaded_at TIMESTAMP NOT NULL,

                                  CONSTRAINT fk_attachment_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- 4. Create Indexes to make the database ultra-fast when loading specific pages
CREATE INDEX idx_tasks_group_id ON tasks(group_id);
CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_company_id ON tasks(company_id);
CREATE INDEX idx_tasks_contact_id ON tasks(contact_id);

-- 5. BONUS: Pre-populate the standard Kanban columns so your board isn't empty!
INSERT INTO task_groups (name, display_order, color_hex) VALUES
                                                             ('📥 До виконання', 1, '#6c757d'),
                                                             ('⏳ В процесі', 2, '#0d6efd'),
                                                             ('✅ Готово', 3, '#198754');