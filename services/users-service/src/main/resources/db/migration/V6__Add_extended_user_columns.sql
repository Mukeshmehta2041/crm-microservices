-- Add username column if missing (some migrations have it, some don't)
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(100);

-- Add extended user profile columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS website_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS twitter_handle VARCHAR(100);

-- Add address columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS state_province VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(100);

-- Add employment columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS hire_date DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS employee_id VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS cost_center VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS office_location VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS work_phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS mobile_phone VARCHAR(20);

-- Add emergency contact columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(100);

-- Add custom fields and preferences
ALTER TABLE users ADD COLUMN IF NOT EXISTS custom_fields TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS theme_preference VARCHAR(20) DEFAULT 'light';
ALTER TABLE users ADD COLUMN IF NOT EXISTS currency_preference VARCHAR(10) DEFAULT 'USD';
ALTER TABLE users ADD COLUMN IF NOT EXISTS number_format VARCHAR(20) DEFAULT '1,234.56';
ALTER TABLE users ADD COLUMN IF NOT EXISTS week_start_day INTEGER DEFAULT 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS working_hours_start TIME;
ALTER TABLE users ADD COLUMN IF NOT EXISTS working_hours_end TIME;

-- Add security and privacy columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_visibility VARCHAR(20) DEFAULT 'TEAM';
ALTER TABLE users ADD COLUMN IF NOT EXISTS activity_visibility VARCHAR(20) DEFAULT 'TEAM';
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_visibility VARCHAR(20) DEFAULT 'TEAM';
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_visibility VARCHAR(20) DEFAULT 'TEAM';

-- Add activity and engagement columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS login_count BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_password_change_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMP;

-- Add notification preferences
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS push_notifications_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS sms_notifications_enabled BOOLEAN DEFAULT FALSE;

-- Add onboarding columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_step INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_completed_at TIMESTAMP;

-- Add data retention and compliance columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS data_retention_policy VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS gdpr_consent_given BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS gdpr_consent_date TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS marketing_consent_given BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS marketing_consent_date TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS data_export_requested BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS data_export_requested_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_scheduled_at TIMESTAMP;

-- Add audit columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_by UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- Add team reference
ALTER TABLE users ADD COLUMN IF NOT EXISTS team_id UUID;

-- Add activity tracking
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMP;

-- Update profile_image_url column name if it doesn't exist (it's avatar_url in the current schema)
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);

-- Update the avatar_url to profile_image_url if data exists
UPDATE users SET profile_image_url = avatar_url WHERE avatar_url IS NOT NULL AND profile_image_url IS NULL;

-- Add date and time format columns (update existing if needed)
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_format VARCHAR(20) DEFAULT 'MM/dd/yyyy';
ALTER TABLE users ADD COLUMN IF NOT EXISTS time_format VARCHAR(10) DEFAULT '12h';

-- Create supporting tables for collections

-- User skills table
CREATE TABLE IF NOT EXISTS user_skills (
    user_id UUID NOT NULL,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, skill),
    CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- User certifications table
CREATE TABLE IF NOT EXISTS user_certifications (
    user_id UUID NOT NULL,
    certification VARCHAR(200) NOT NULL,
    PRIMARY KEY (user_id, certification),
    CONSTRAINT fk_user_certifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- User languages table
CREATE TABLE IF NOT EXISTS user_languages (
    user_id UUID NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (user_id, language_code),
    CONSTRAINT fk_user_languages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- User working days table
CREATE TABLE IF NOT EXISTS user_working_days (
    user_id UUID NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (user_id, day_of_week),
    CONSTRAINT fk_user_working_days_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_day_of_week CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

-- User completed trainings table
CREATE TABLE IF NOT EXISTS user_completed_trainings (
    user_id UUID NOT NULL,
    training_id UUID NOT NULL,
    PRIMARY KEY (user_id, training_id),
    CONSTRAINT fk_user_completed_trainings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_users_team_id ON users(team_id);
CREATE INDEX IF NOT EXISTS idx_users_employee_id ON users(employee_id);
CREATE INDEX IF NOT EXISTS idx_users_hire_date ON users(hire_date);
CREATE INDEX IF NOT EXISTS idx_users_account_locked_until ON users(account_locked_until);
CREATE INDEX IF NOT EXISTS idx_users_last_activity_at ON users(last_activity_at);
CREATE INDEX IF NOT EXISTS idx_users_onboarding_completed ON users(onboarding_completed);

-- Add constraints for visibility fields (using DO block to handle IF NOT EXISTS)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'valid_profile_visibility') THEN
        ALTER TABLE users ADD CONSTRAINT valid_profile_visibility 
            CHECK (profile_visibility IN ('PUBLIC', 'TEAM', 'PRIVATE'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'valid_activity_visibility') THEN
        ALTER TABLE users ADD CONSTRAINT valid_activity_visibility 
            CHECK (activity_visibility IN ('PUBLIC', 'TEAM', 'PRIVATE'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'valid_email_visibility') THEN
        ALTER TABLE users ADD CONSTRAINT valid_email_visibility 
            CHECK (email_visibility IN ('PUBLIC', 'TEAM', 'PRIVATE'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'valid_phone_visibility') THEN
        ALTER TABLE users ADD CONSTRAINT valid_phone_visibility 
            CHECK (phone_visibility IN ('PUBLIC', 'TEAM', 'PRIVATE'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'valid_theme_preference') THEN
        ALTER TABLE users ADD CONSTRAINT valid_theme_preference 
            CHECK (theme_preference IN ('light', 'dark', 'auto'));
    END IF;
END $$;