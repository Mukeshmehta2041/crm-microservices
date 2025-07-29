-- Create custom_field_options table
CREATE TABLE custom_field_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    custom_field_id UUID NOT NULL REFERENCES custom_fields(id) ON DELETE CASCADE,
    option_label VARCHAR(255) NOT NULL,
    option_value VARCHAR(255) NOT NULL,
    option_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    color VARCHAR(7),
    description TEXT,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT unique_option_per_field UNIQUE (custom_field_id, option_value),
    CONSTRAINT valid_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Create indexes for custom_field_options
CREATE INDEX idx_custom_field_options_custom_field_id ON custom_field_options(custom_field_id);
CREATE INDEX idx_custom_field_options_field_active ON custom_field_options(custom_field_id, is_active);
CREATE INDEX idx_custom_field_options_field_order ON custom_field_options(custom_field_id, option_order);
CREATE INDEX idx_custom_field_options_field_default ON custom_field_options(custom_field_id, is_default);
CREATE INDEX idx_custom_field_options_option_value ON custom_field_options(custom_field_id, option_value);
CREATE INDEX idx_custom_field_options_created_at ON custom_field_options(created_at);
CREATE INDEX idx_custom_field_options_updated_at ON custom_field_options(updated_at);

-- Add comments
COMMENT ON TABLE custom_field_options IS 'Options for picklist and multipicklist custom fields';
COMMENT ON COLUMN custom_field_options.custom_field_id IS 'Reference to the custom field this option belongs to';
COMMENT ON COLUMN custom_field_options.option_label IS 'Display label for the option';
COMMENT ON COLUMN custom_field_options.option_value IS 'Internal value for the option';
COMMENT ON COLUMN custom_field_options.option_order IS 'Display order of the option';
COMMENT ON COLUMN custom_field_options.is_default IS 'Whether this is a default option';
COMMENT ON COLUMN custom_field_options.is_active IS 'Whether the option is active';
COMMENT ON COLUMN custom_field_options.color IS 'Color code for UI display';
COMMENT ON COLUMN custom_field_options.description IS 'Description of the option';