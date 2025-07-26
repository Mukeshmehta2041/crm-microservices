# GitHub Workflows Setup

This directory contains comprehensive GitHub Actions workflows for the CRM Microservices Platform.

## Workflows Overview

### 1. CI/CD Pipeline (`ci-cd.yml`)
**Triggers:** Push to main/develop/feature branches, tags, and pull requests

**Features:**
- Tests shared libraries and services
- Builds and pushes Docker images to Docker Hub
- Multi-platform builds (linux/amd64, linux/arm64)
- Security scanning with Trivy
- Automated staging and production deployments
- Slack notifications

### 2. Pull Request Validation (`pr-validation.yml`)
**Triggers:** Pull requests to main/develop branches

**Features:**
- Code quality checks (Checkstyle, SpotBugs, PMD)
- Test coverage analysis with Codecov
- Integration tests with PostgreSQL and Redis
- Docker build validation
- Security checks with OWASP Dependency Check
- Performance tests (when labeled)
- Automated PR comments with results

### 3. Manual Docker Build (`docker-build-manual.yml`)
**Triggers:** Manual workflow dispatch

**Features:**
- Build specific services or all services
- Choose production or development environment
- Custom tagging support
- On-demand builds for testing

### 4. Dependency Updates (`dependency-update.yml`)
**Triggers:** Weekly schedule (Mondays) or manual dispatch

**Features:**
- Automated Maven dependency updates
- Excludes snapshot versions
- Creates pull requests with updates
- Runs tests to validate updates

### 5. Cleanup Old Images (`cleanup-images.yml`)
**Triggers:** Weekly schedule (Sundays) or manual dispatch

**Features:**
- Removes old Docker images from registry
- Keeps last 10 versions
- Helps manage storage costs

## Required Secrets

Add these secrets to your GitHub repository settings:

### Docker Hub
```
DOCKER_USERNAME=your-dockerhub-username
DOCKER_PASSWORD=your-dockerhub-password-or-token
```

### Slack Notifications (Optional)
```
SLACK_WEBHOOK_URL=your-slack-webhook-url
```

### Codecov (Optional)
```
CODECOV_TOKEN=your-codecov-token
```

## Setup Instructions

### 1. Docker Hub Setup
1. Create a Docker Hub account
2. Create repositories for each service:
   - `your-username/crm-auth-service`
   - `your-username/crm-tenant-service`
   - `your-username/crm-users-service`
   - `your-username/crm-discovery-server`
3. Generate an access token in Docker Hub settings
4. Add `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets to GitHub

### 2. Branch Protection Rules
Configure branch protection for `main` and `develop` branches:
- Require status checks to pass
- Require branches to be up to date
- Require review from code owners
- Restrict pushes to specific people/teams

### 3. Environment Setup
Create environments in GitHub repository settings:
- `staging` - for staging deployments
- `production` - for production deployments (with required reviewers)

### 4. Labels Setup
Create these labels for enhanced workflow functionality:
- `performance-test` - triggers performance tests in PRs
- `security-review` - for security-related changes
- `breaking-change` - for breaking changes

## Image Tagging Strategy

### Production Images
- `latest` - latest stable release (main branch)
- `v1.0.0` - semantic version tags
- `v1.0` - major.minor tags
- `v1` - major version tags
- `main-sha123456` - commit-specific tags

### Development Images
- `dev` - latest development build
- `develop` - develop branch builds
- `feature-branch-name` - feature branch builds

## Deployment Environments

### Staging
- Triggered on pushes to `main` branch
- Automatic deployment after successful builds
- Health checks and notifications

### Production
- Triggered on version tags (`v*`)
- Requires manual approval
- Comprehensive health checks
- Rollback capabilities

## Monitoring and Notifications

### Slack Integration
Configure Slack webhook for notifications:
- Build status updates
- Deployment notifications
- Security scan results
- Failed workflow alerts

### Security Scanning
- Trivy vulnerability scanning
- OWASP dependency checks
- Results uploaded to GitHub Security tab
- Automated security advisories

## Best Practices

### Commit Messages
Use conventional commits for better automation:
```
feat: add new authentication endpoint
fix: resolve database connection issue
chore: update dependencies
docs: update API documentation
```

### Pull Request Guidelines
- Include comprehensive description
- Add appropriate labels
- Ensure all checks pass
- Request reviews from relevant team members

### Version Management
- Use semantic versioning (v1.0.0)
- Tag releases properly
- Maintain changelog
- Document breaking changes

## Troubleshooting

### Common Issues

1. **Docker build failures**
   - Check Dockerfile syntax
   - Verify build context
   - Review .dockerignore files

2. **Test failures**
   - Check database connections
   - Verify environment variables
   - Review test configurations

3. **Security scan failures**
   - Update vulnerable dependencies
   - Review security advisories
   - Check for exposed secrets

### Getting Help
- Check workflow logs in GitHub Actions tab
- Review error messages and stack traces
- Consult team documentation
- Create issues for persistent problems