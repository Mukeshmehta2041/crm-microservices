-- =====================================================
-- USERS SERVICE - FIX ENTITY ALIGNMENT
-- =====================================================
-- This migration fixes misalignments between entities and database schema

-- =====================================================
-- 1. FIX ROLES TABLE TO MATCH ENTITY
-- =====================================================

-- Add missing columns to roles table (using simple ALTER statements)
ALTER TABLE roles ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS parent_role_id UUID;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS hierarchy_level INTEGER DEFAULT 0;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS hierarchy_path VARCHAR(1000);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS role_type VARCHAR(20) DEFAULT 'CUSTOM';
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS color VARCHAR(7);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS icon VARCHAR(50);

-- Update existing roles to have display_name if missing
UPDATE roles SET display_name = name WHERE display_name IS NULL;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_roles_parent_id ON roles(parent_role_id);
CREATE INDEX IF NOT EXISTS idx_roles_hierarchy_level ON roles(hierarchy_level);
CREATE INDEX IF NOT EXISTS idx_roles_role_type ON roles(role_type);
CREATE INDEX IF NOT EXISTS idx_roles_is_active ON roles(is_active);

-- =====================================================
-- 2. FIX USER_ROLES TABLE TO MATCH ENTITY
-- =====================================================

-- Add missing columns to user_roles table
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS is_inherited BOOLEAN DEFAULT FALSE;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS inherited_from UUID;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS assignment_type VARCHAR(20) DEFAULT 'DIRECT';
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS assignment_reason VARCHAR(500);

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_user_roles_expires ON user_roles(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_roles_active ON user_roles(is_active);
CREATE INDEX IF NOT EXISTS idx_user_roles_assignment_type ON user_roles(assignment_type);

-- =====================================================
-- 3. FIX TEAMS TABLE TO MATCH ENTITY
-- =====================================================

-- Add missing columns to teams table
ALTER TABLE teams ADD COLUMN IF NOT EXISTS manager_id UUID;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS hierarchy_level INTEGER DEFAULT 0;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS hierarchy_path VARCHAR(1000);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS team_type VARCHAR(20) DEFAULT 'FUNCTIONAL';
ALTER TABLE teams ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE teams ADD COLUMN IF NOT EXISTS department VARCHAR(100);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS location VARCHAR(200);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS cost_center VARCHAR(50);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS budget DECIMAL(15,2);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS max_members INTEGER;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS color VARCHAR(7);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS icon VARCHAR(50);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS settings TEXT;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS goals TEXT;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS kpis TEXT;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS slack_channel VARCHAR(100);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS teams_channel VARCHAR(100);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS working_hours_start TIME;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS working_hours_end TIME;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);
ALTER TABLE teams ADD COLUMN IF NOT EXISTS member_count INTEGER DEFAULT 0;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS active_member_count INTEGER DEFAULT 0;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMP;
ALTER TABLE teams ADD COLUMN IF NOT EXISTS performance_score DECIMAL(5,2);

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_teams_manager_id ON teams(manager_id);
CREATE INDEX IF NOT EXISTS idx_teams_team_type ON teams(team_type);
CREATE INDEX IF NOT EXISTS idx_teams_status ON teams(status);
CREATE INDEX IF NOT EXISTS idx_teams_department ON teams(department);

-- =====================================================
-- 4. CREATE TEAM WORKING DAYS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS team_working_days (
    team_id UUID NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (team_id, day_of_week),
    CONSTRAINT fk_team_working_days_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);

-- =====================================================
-- 5. CREATE TEAM ROLES JUNCTION TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS team_roles (
    team_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (team_id, role_id),
    CONSTRAINT fk_team_roles_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_team_roles_team ON team_roles(team_id);
CREATE INDEX IF NOT EXISTS idx_team_roles_role ON team_roles(role_id);

-- =====================================================
-- 6. FIX TEAM_MEMBERS TABLE TO MATCH ENTITY
-- =====================================================

-- Add missing columns to team_members table
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS team_role VARCHAR(20) DEFAULT 'MEMBER';
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS left_at TIMESTAMP;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS is_primary_team BOOLEAN DEFAULT FALSE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS allocation_percentage INTEGER DEFAULT 100;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS reporting_manager_id UUID;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS position_title VARCHAR(150);
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS responsibilities TEXT;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS skills_required TEXT;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS performance_rating DECIMAL(3,2);
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS last_performance_review TIMESTAMP;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS billable_rate DECIMAL(10,2);
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS cost_rate DECIMAL(10,2);
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS can_manage_team BOOLEAN DEFAULT FALSE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS can_add_members BOOLEAN DEFAULT FALSE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS can_remove_members BOOLEAN DEFAULT FALSE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS can_view_team_analytics BOOLEAN DEFAULT FALSE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS can_edit_team_settings BOOLEAN DEFAULT FALSE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMP;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS activity_score DECIMAL(5,2);
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS contribution_score DECIMAL(5,2);
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS email_notifications BOOLEAN DEFAULT TRUE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS slack_notifications BOOLEAN DEFAULT TRUE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS teams_notifications BOOLEAN DEFAULT TRUE;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS added_by UUID;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS removed_by UUID;
ALTER TABLE team_members ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update tenant_id from users table where missing
UPDATE team_members SET tenant_id = u.tenant_id 
FROM users u 
WHERE team_members.user_id = u.id AND team_members.tenant_id IS NULL;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_team_members_tenant ON team_members(tenant_id);
CREATE INDEX IF NOT EXISTS idx_team_members_team_role ON team_members(team_role);
CREATE INDEX IF NOT EXISTS idx_team_members_status ON team_members(status);
CREATE INDEX IF NOT EXISTS idx_team_members_reporting_manager ON team_members(reporting_manager_id);

-- =====================================================
-- 7. CREATE USER AUDIT LOGS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS user_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) DEFAULT 'USER',
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    description TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    performed_by UUID,
    severity VARCHAR(20) DEFAULT 'INFO',
    additional_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for user_audit_logs
CREATE INDEX IF NOT EXISTS idx_user_audit_user_id ON user_audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_audit_tenant_id ON user_audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_audit_action ON user_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_user_audit_created_at ON user_audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_user_audit_performed_by ON user_audit_logs(performed_by);

-- =====================================================
-- 8. MAKE USERNAME NULLABLE
-- =====================================================

-- Make username column nullable since the User entity doesn't use it
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;

-- Add unique constraint for username per tenant if missing (handling nullable username)
DROP INDEX IF EXISTS uk_users_tenant_username;
CREATE UNIQUE INDEX uk_users_tenant_username ON users(tenant_id, username) WHERE username IS NOT NULL;

-- =====================================================
-- 9. UPDATE TRIGGERS FOR NEW TABLES
-- =====================================================

-- Create updated_at trigger for team_members
DROP TRIGGER IF EXISTS update_team_members_updated_at ON team_members;
CREATE TRIGGER update_team_members_updated_at BEFORE UPDATE ON team_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 10. COMMENTS FOR DOCUMENTATION
-- =====================================================

COMMENT ON TABLE team_working_days IS 'Working days configuration for teams';
COMMENT ON TABLE team_roles IS 'Junction table for team-role associations';
COMMENT ON TABLE user_audit_logs IS 'Audit trail for user-related changes';