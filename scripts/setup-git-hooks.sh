#!/bin/bash

# Setup Git Hooks for CRM Platform
# This script installs pre-commit hooks for code quality

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "Not in a git repository"
    exit 1
fi

GIT_HOOKS_DIR=".git/hooks"
PRE_COMMIT_HOOK="$GIT_HOOKS_DIR/pre-commit"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

print_info "Setting up Git hooks for CRM Platform..."

# Create hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Install pre-commit hook
if [ -f "$PRE_COMMIT_HOOK" ]; then
    print_warning "Pre-commit hook already exists. Creating backup..."
    cp "$PRE_COMMIT_HOOK" "$PRE_COMMIT_HOOK.backup"
fi

# Create the pre-commit hook
cat > "$PRE_COMMIT_HOOK" << 'EOF'
#!/bin/bash
# Pre-commit hook for CRM Platform
# This hook runs the pre-commit quality checks

# Get the directory of this script
HOOK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$HOOK_DIR/../.." && pwd)"

# Run the pre-commit script
exec "$PROJECT_ROOT/scripts/pre-commit-hook.sh" "$@"
EOF

# Make the hook executable
chmod +x "$PRE_COMMIT_HOOK"

print_success "Pre-commit hook installed"

# Create commit-msg hook for commit message validation
COMMIT_MSG_HOOK="$GIT_HOOKS_DIR/commit-msg"

cat > "$COMMIT_MSG_HOOK" << 'EOF'
#!/bin/bash
# Commit message hook for CRM Platform

commit_msg_file="$1"
commit_msg=$(cat "$commit_msg_file")
first_line=$(echo "$commit_msg" | head -n1)

# Colors
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check commit message length
if [ ${#first_line} -gt 72 ]; then
    echo -e "${YELLOW}Warning: Commit message first line is longer than 72 characters${NC}"
fi

# Check for empty commit message
if [ -z "$(echo "$commit_msg" | tr -d '[:space:]')" ]; then
    echo -e "${RED}Error: Commit message cannot be empty${NC}"
    exit 1
fi

# Check for conventional commit format (optional warning)
if [[ ! $first_line =~ ^(feat|fix|docs|style|refactor|test|chore|perf|ci|build)(\(.+\))?: ]]; then
    echo -e "${YELLOW}Tip: Consider using conventional commit format${NC}"
    echo -e "${YELLOW}Format: type(scope): description${NC}"
    echo -e "${YELLOW}Types: feat, fix, docs, style, refactor, test, chore, perf, ci, build${NC}"
fi

exit 0
EOF

chmod +x "$COMMIT_MSG_HOOK"
print_success "Commit message hook installed"

# Create prepare-commit-msg hook for commit message template
PREPARE_COMMIT_MSG_HOOK="$GIT_HOOKS_DIR/prepare-commit-msg"

cat > "$PREPARE_COMMIT_MSG_HOOK" << 'EOF'
#!/bin/bash
# Prepare commit message hook for CRM Platform

commit_msg_file="$1"
commit_source="$2"

# Only add template for regular commits (not merge, squash, etc.)
if [ -z "$commit_source" ] || [ "$commit_source" = "template" ]; then
    # Get current branch name
    branch_name=$(git symbolic-ref --short HEAD 2>/dev/null || echo "")
    
    # Extract ticket number from branch name (e.g., feature/CRM-123-description -> CRM-123)
    ticket_number=$(echo "$branch_name" | grep -oE '[A-Z]+-[0-9]+' || echo "")
    
    # If we have a ticket number, prepend it to the commit message
    if [ -n "$ticket_number" ] && ! grep -q "$ticket_number" "$commit_msg_file"; then
        temp_file=$(mktemp)
        echo "[$ticket_number] " > "$temp_file"
        cat "$commit_msg_file" >> "$temp_file"
        mv "$temp_file" "$commit_msg_file"
    fi
fi
EOF

chmod +x "$PREPARE_COMMIT_MSG_HOOK"
print_success "Prepare commit message hook installed"

# Create post-commit hook for notifications
POST_COMMIT_HOOK="$GIT_HOOKS_DIR/post-commit"

cat > "$POST_COMMIT_HOOK" << 'EOF'
#!/bin/bash
# Post-commit hook for CRM Platform

# Get commit information
commit_hash=$(git rev-parse HEAD)
commit_msg=$(git log -1 --pretty=format:"%s")
author=$(git log -1 --pretty=format:"%an")

echo "✓ Commit successful!"
echo "  Hash: $commit_hash"
echo "  Message: $commit_msg"
echo "  Author: $author"

# Optional: Send notification to team chat, update issue tracker, etc.
# This can be customized based on your team's workflow
EOF

chmod +x "$POST_COMMIT_HOOK"
print_success "Post-commit hook installed"

# Create configuration for skipping hooks when needed
cat > ".git/hooks/README.md" << 'EOF'
# Git Hooks for CRM Platform

This directory contains Git hooks that enforce code quality and consistency.

## Installed Hooks

- **pre-commit**: Runs code quality checks before allowing commits
- **commit-msg**: Validates commit message format
- **prepare-commit-msg**: Adds ticket numbers from branch names
- **post-commit**: Shows commit information after successful commits

## Skipping Hooks

To skip hooks for a specific commit (not recommended):
```bash
git commit --no-verify -m "Your commit message"
```

## Environment Variables

- `SKIP_TESTS=true`: Skip running tests in pre-commit hook
- `SKIP_QUALITY_CHECKS=true`: Skip quality checks in pre-commit hook

## Customization

Hooks can be customized by editing the scripts in the `scripts/` directory.
EOF

print_success "Git hooks documentation created"

# Test if hooks are working
print_info "Testing hook installation..."

if [ -x "$PRE_COMMIT_HOOK" ]; then
    print_success "Pre-commit hook is executable"
else
    print_error "Pre-commit hook is not executable"
fi

if [ -x "$COMMIT_MSG_HOOK" ]; then
    print_success "Commit message hook is executable"
else
    print_error "Commit message hook is not executable"
fi

print_success "Git hooks setup completed!"
print_info "Hooks installed:"
print_info "  - Pre-commit: Code quality checks"
print_info "  - Commit-msg: Message format validation"
print_info "  - Prepare-commit-msg: Auto-add ticket numbers"
print_info "  - Post-commit: Success notifications"
echo ""
print_info "To skip hooks for a commit: git commit --no-verify"
print_info "To configure hook behavior, set environment variables:"
print_info "  - SKIP_TESTS=true"
print_info "  - SKIP_QUALITY_CHECKS=true"