-- 1. Drop the old constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- 2. Add the new constraint with the updated roles
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'DIRECTOR', 'MANAGER', 'INSTALLER', 'GUEST'));