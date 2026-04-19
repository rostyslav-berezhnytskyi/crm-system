-- 1. Add Lockout fields to your users table
ALTER TABLE users ADD COLUMN locked BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;

-- 2. Create the standard Spring Security Remember-Me table
CREATE TABLE persistent_logins (
                                   username VARCHAR(64) NOT NULL,
                                   series VARCHAR(64) PRIMARY KEY,
                                   token VARCHAR(64) NOT NULL,
                                   last_used TIMESTAMP NOT NULL
);