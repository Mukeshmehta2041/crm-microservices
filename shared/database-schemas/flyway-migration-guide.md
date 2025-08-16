# Flyway Migration Implementation Guide

## Overview
This document provides comprehensive guidance for implementing Flyway migrations across all CRM microservices, including versioning strategies, rollback procedures, and best practices.

## Migration Versioning Strategy

### Version Format
- **Format**: `V{major}.{minor}__{description}.sql`
- **Examples**: 
  - `V1__Create_users_table.sql`
  - `V2.1__Add_user_preferences.sql`
  - `V3__Add_comprehensive_schema_enhancements.sql`

### Version Numbering Rules
1. **Major versions** (V1, V2, V3): Significant schema changes, new tables, major features
2. **Minor versions** (V1.1, V1.2): Small additions, index optimizations, constraint changes
3. **Patch versions** (V1.1.1): Bug fixes, data corrections, minor adjustments

### Service-Specific Versioning
Each service maintains its own migration version sequence:
- **auth-service**: V1-V10 (authentication and user management)
- **tenant-service**: V1-V10 (tenant and configuration management)
- **contacts-service**: V1-V10 (contact and relationship management)
- **accounts-service**: V1-V10 (account and territory management)
- **deals-service**: V1-V10 (deals and pipeline management)
- **custom-objects-service**: V1-V10 (custom objects and fields)
- **leads-service**: V1-V10 (lead management)
- **activities-service**: V1-V10 (activity tracking)
- **analytics-service**: V1-V10 (reporting and analytics)
- **workflow-service**: V1-V10 (workflow automation)

## Migration Categories

### 1. Schema Migrations
- Table creation and modification
- Index creation and optimization
- Constraint additions and modifications
- Function and trigger definitions

### 2. Data Migrations
- Reference data insertion
- Data transformation and cleanup
- Default value population
- Data validation and correction

### 3. Security Migrations
- Row-level security policy creation
- Permission and role setup
- Audit trail implementation
- Encryption key management

### 4. Performance Migrations
- Index optimization
- Partitioning implementation
- Query performance improvements
- Statistics updates

## Rollback Strategies

### 1. Undo Migrations
Create corresponding undo scripts for each migration:
- **Format**: `U{version}__{description}.sql`
- **Example**: `U3__Undo_comprehensive_schema_enhancements.sql`

### 2. Rollback Procedures
1. **Immediate Rollback**: For critical issues during deployment
2. **Planned Rollback**: For feature rollbacks with data preservation
3. **Emergency Rollback**: For production incidents with minimal data loss

### 3. Data Preservation
- Always backup data before destructive operations
- Use temporary tables for complex transformations
- Implement data validation before and after migrations

## Migration Testing Procedures

### 1. Development Testing
- Test migrations on local development databases
- Validate schema changes against application code
- Test rollback procedures for each migration

### 2. Staging Testing
- Run migrations on staging environment with production-like data
- Performance testing with realistic data volumes
- Integration testing across all services

### 3. Production Validation
- Pre-deployment validation checks
- Post-deployment verification procedures
- Monitoring and alerting setup

## Best Practices

### 1. Migration Design
- Keep migrations atomic and idempotent
- Use transactions for related changes
- Include comprehensive comments and documentation
- Test migrations thoroughly before deployment

### 2. Performance Considerations
- Create indexes concurrently in production
- Use batch processing for large data migrations
- Monitor migration execution time and resource usage
- Plan maintenance windows for long-running migrations

### 3. Error Handling
- Include proper error handling in migration scripts
- Log migration progress and errors
- Implement retry mechanisms for transient failures
- Provide clear error messages and resolution steps

### 4. Documentation
- Document all schema changes and their purpose
- Maintain migration logs and deployment history
- Include rollback procedures and data recovery steps
- Update API documentation for schema changes

## Monitoring and Alerting

### 1. Migration Monitoring
- Track migration execution time and success rates
- Monitor database performance during migrations
- Alert on migration failures or timeouts
- Log all migration activities for audit purposes

### 2. Health Checks
- Validate schema integrity after migrations
- Check data consistency and referential integrity
- Verify application functionality post-migration
- Monitor system performance and resource usage

### 3. Backup and Recovery
- Automated backups before major migrations
- Point-in-time recovery capabilities
- Cross-region backup replication
- Regular backup validation and testing

## Service-Specific Considerations

### Auth Service
- User credential security during migrations
- Session management during schema changes
- Password hash migration procedures
- Multi-factor authentication data handling

### Tenant Service
- Tenant isolation during migrations
- Configuration data consistency
- Feature flag migration procedures
- Usage metrics data preservation

### Contact/Account Services
- Large dataset migration strategies
- Relationship data integrity
- Custom field migration procedures
- Duplicate detection during migrations

### Deals Service
- Pipeline data consistency
- Revenue calculation accuracy
- Stage history preservation
- Forecasting data migration

### Custom Objects Service
- Dynamic schema migration challenges
- Custom field validation during migrations
- Index management for custom objects
- Workflow migration procedures

## Deployment Procedures

### 1. Pre-Deployment
- Database backup creation
- Migration script validation
- Dependency verification
- Resource availability check

### 2. Deployment Execution
- Sequential service migration order
- Inter-service dependency management
- Real-time monitoring and logging
- Rollback trigger conditions

### 3. Post-Deployment
- Schema validation and testing
- Application functionality verification
- Performance monitoring
- User acceptance testing

## Emergency Procedures

### 1. Migration Failure Response
- Immediate rollback procedures
- Data recovery processes
- Service restoration steps
- Communication protocols

### 2. Data Corruption Recovery
- Point-in-time recovery procedures
- Data validation and repair
- Service synchronization
- User notification processes

### 3. Performance Degradation
- Query optimization procedures
- Index rebuilding processes
- Resource scaling options
- Load balancing adjustments

## Tools and Automation

### 1. Migration Tools
- Flyway command-line interface
- Database migration scripts
- Automated testing frameworks
- Monitoring and alerting tools

### 2. CI/CD Integration
- Automated migration testing
- Deployment pipeline integration
- Rollback automation
- Quality gate enforcement

### 3. Monitoring Integration
- Database performance monitoring
- Application health checks
- Log aggregation and analysis
- Alert notification systems