-- Make username column nullable since the User entity doesn't use it
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;

-- Update the unique constraint to handle nullable username
DROP INDEX IF EXISTS uk_users_tenant_username;
CREATE UNIQUE INDEX uk_users_tenant_username ON users(tenant_id, username) WHERE username IS NOT NULL;