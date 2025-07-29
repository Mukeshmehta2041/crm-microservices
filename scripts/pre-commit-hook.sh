#!/bin/bash

# Pre-commit hook for CRM Platform
# This script runs quality checks before allowing commits

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

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

# Get list of staged Java files
STAGED_JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.java$' || true)

if [ -z "$STAGED_JAVA_FILES" ]; then
    print_info "No Java files staged for commit"
    exit 0
fi

print_header "Pre-commit Quality Checks"
print_info "Checking $(echo "$STAGED_JAVA_FILES" | wc -l) staged Java files"

# Function to check if Maven wrapper exists
check_maven_wrapper() {
    if [ ! -f "./mvnw" ]; then
        print_error "Maven wrapper not found. Please run 'mvn wrapper:wrapper' first"
        exit 1
    fi
}

# Function to run code formatting check
check_code_formatting() {
    print_info "Checking code formatting..."
    
    if ./mvnw spotless:check -q > /dev/null 2>&1; then
        print_success "Code formatting check passed"
        return 0
    else
        print_error "Code formatting issues found"
        print_info "Run 'mvn spotless:apply' to fix formatting issues"
        
        # Ask user if they want to auto-fix
        read -p "Do you want to auto-fix formatting issues? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_info "Auto-fixing formatting issues..."
            ./mvnw spotless:apply -q
            
            # Add the formatted files back to staging
            for file in $STAGED_JAVA_FILES; do
                git add "$file"
            done
            
            print_success "Formatting issues fixed and files re-staged"
            return 0
        else
            return 1
        fi
    fi
}

# Function to run compilation check
check_compilation() {
    print_info "Checking compilation..."
    
    if ./mvnw compile -q > /dev/null 2>&1; then
        print_success "Compilation check passed"
        return 0
    else
        print_error "Compilation failed"
        print_info "Please fix compilation errors before committing"
        return 1
    fi
}

# Function to run basic static analysis
check_static_analysis() {
    print_info "Running basic static analysis..."
    
    # Check for common issues in staged files
    local issues_found=false
    
    for file in $STAGED_JAVA_FILES; do
        # Check for System.out.println
        if grep -n "System\.out\.println" "$file" > /dev/null 2>&1; then
            print_warning "Found System.out.println in $file"
            issues_found=true
        fi
        
        # Check for printStackTrace
        if grep -n "printStackTrace" "$file" > /dev/null 2>&1; then
            print_warning "Found printStackTrace in $file"
            issues_found=true
        fi
        
        # Check for TODO comments
        if grep -n "TODO" "$file" > /dev/null 2>&1; then
            print_info "Found TODO comments in $file"
        fi
        
        # Check for FIXME comments
        if grep -n "FIXME" "$file" > /dev/null 2>&1; then
            print_warning "Found FIXME comments in $file"
        fi
    done
    
    if [ "$issues_found" = true ]; then
        print_warning "Static analysis found potential issues"
        read -p "Do you want to continue with the commit? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            return 1
        fi
    fi
    
    print_success "Static analysis completed"
    return 0
}

# Function to run unit tests for changed modules
run_affected_tests() {
    print_info "Running tests for affected modules..."
    
    # Get list of affected modules
    local affected_modules=""
    for file in $STAGED_JAVA_FILES; do
        if [[ $file == services/* ]]; then
            module=$(echo "$file" | cut -d'/' -f1-2)
            if [[ ! $affected_modules =~ $module ]]; then
                affected_modules="$affected_modules $module"
            fi
        elif [[ $file == shared/* ]]; then
            module=$(echo "$file" | cut -d'/' -f1-2)
            if [[ ! $affected_modules =~ $module ]]; then
                affected_modules="$affected_modules $module"
            fi
        fi
    done
    
    if [ -n "$affected_modules" ]; then
        for module in $affected_modules; do
            if [ -d "$module" ]; then
                print_info "Running tests for $module..."
                if (cd "$module" && ../mvnw test -q > /dev/null 2>&1); then
                    print_success "Tests passed for $module"
                else
                    print_error "Tests failed for $module"
                    return 1
                fi
            fi
        done
    else
        print_info "No specific modules affected, running quick test suite..."
        if ./mvnw test -q > /dev/null 2>&1; then
            print_success "Quick test suite passed"
        else
            print_error "Quick test suite failed"
            return 1
        fi
    fi
    
    return 0
}

# Function to check commit message format
check_commit_message() {
    local commit_msg_file="$1"
    
    if [ -f "$commit_msg_file" ]; then
        local commit_msg=$(cat "$commit_msg_file")
        local first_line=$(echo "$commit_msg" | head -n1)
        
        # Check commit message length
        if [ ${#first_line} -gt 72 ]; then
            print_warning "Commit message first line is longer than 72 characters"
        fi
        
        # Check for conventional commit format (optional)
        if [[ ! $first_line =~ ^(feat|fix|docs|style|refactor|test|chore|perf|ci|build)(\(.+\))?: ]]; then
            print_info "Consider using conventional commit format: type(scope): description"
            print_info "Types: feat, fix, docs, style, refactor, test, chore, perf, ci, build"
        fi
    fi
}

# Main execution
main() {
    local exit_code=0
    
    check_maven_wrapper
    
    # Run checks
    check_code_formatting || exit_code=1
    check_compilation || exit_code=1
    check_static_analysis || exit_code=1
    
    # Run tests only if SKIP_TESTS is not set
    if [ "${SKIP_TESTS:-false}" != "true" ]; then
        run_affected_tests || exit_code=1
    else
        print_warning "Tests skipped (SKIP_TESTS=true)"
    fi
    
    if [ $exit_code -eq 0 ]; then
        print_success "All pre-commit checks passed!"
        print_info "Proceeding with commit..."
    else
        print_error "Pre-commit checks failed!"
        print_info "Please fix the issues above before committing"
        echo ""
        print_info "To skip these checks (not recommended), use: git commit --no-verify"
    fi
    
    exit $exit_code
}

# Run main function
main "$@"