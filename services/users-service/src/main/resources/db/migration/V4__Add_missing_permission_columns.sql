-- Add missing columns to permissions table
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS category VARCHAR(50);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS is_system_permission BOOLEAN DEFAULT FALSE;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS constraints TEXT;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS updated_by UUID;

-- Update the existing is_system column to match the entity
UPDATE permissions SET is_system_permission = is_system WHERE is_system_permission IS NULL;
ALTER TABLE permissions DROP COLUMN IF EXISTS is_system;

-- Create additional indexes
CREATE INDEX IF NOT EXISTS idx_permissions_category ON permissions(category);
CREATE INDEX IF NOT EXISTS idx_permissions_resource_action ON permissions(resource, action);
CREATE INDEX IF NOT EXISTS idx_permissions_is_active ON permissions(is_active);
CREATE INDEX IF NOT EXISTS idx_permissions_priority ON permissions(priority);