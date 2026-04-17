-- V9__add_task_comments.sql

CREATE TABLE task_comments (
                               id BIGSERIAL PRIMARY KEY,
                               text TEXT NOT NULL,
                               created_at TIMESTAMP NOT NULL,
                               task_id BIGINT NOT NULL,
                               author_id BIGINT NOT NULL,

    -- Foreign key linking to the tasks table
                               CONSTRAINT fk_comment_task
                                   FOREIGN KEY (task_id)
                                       REFERENCES tasks (id)
                                       ON DELETE CASCADE,

    -- Foreign key linking to the users table
                               CONSTRAINT fk_comment_author
                                   FOREIGN KEY (author_id)
                                       REFERENCES users (id)
                                       ON DELETE CASCADE
);

-- Optional: Add an index on task_id to make loading comments super fast
CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);