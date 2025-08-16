-- Add missing timestamp columns to permissions table
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to have proper timestamps
UPDATE permissions SET updated_at = created_at WHERE updated_at IS NULL;