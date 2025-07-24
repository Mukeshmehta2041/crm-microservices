# API Design

This document provides comprehensive API specifications for the CRM platform, including RESTful endpoints, GraphQL schemas, security patterns, and integration examples.

---

**Status**: In Progress  
**Last Updated**: January 2025  
**Version**: 1.0

## Table of Contents

1. [RESTful API Endpoints](#restful-api-endpoints)
2. [GraphQL Schemas](#graphql-schemas)
3. [Security and Authentication](#security-and-authentication)
4. [Integration API Examples](#integration-api-examples)

---

## RESTful API Endpoints

### Base URL Structure

```
Production: https://api.crm-platform.com/v1
Staging: https://staging-api.crm-platform.com/v1
Development: https://dev-api.crm-platform.com/v1
```

### Common Response Format

All API responses follow a consistent structure:

```json
{
  "success": true,
  "data": {},
  "meta": {
    "timestamp": "2025-01-24T10:30:00Z",
    "version": "1.0",
    "requestId": "req_123456789"
  },
  "errors": []
}
```

### Error Response Format

```json
{
  "success": false,
  "data": null,
  "meta": {
    "timestamp": "2025-01-24T10:30:00Z",
    "version": "1.0",
    "requestId": "req_123456789"
  },
  "errors": [
    {
      "code": "VALIDATION_ERROR",
      "message": "Invalid email format",
      "field": "email"
    }
  ]
}
```

### Contacts API

#### Base Endpoint: `/contacts`

**Create Contact**
```http
POST /contacts
Content-Type: application/json
Authorization: Bearer {token}

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-0123",
  "company": "Acme Corp",
  "jobTitle": "Sales Manager",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  },
  "customFields": {
    "leadSource": "Website",
    "industry": "Technology"
  }
}
```

**Get Contact**
```http
GET /contacts/{contactId}
Authorization: Bearer {token}
```

**Update Contact**
```http
PUT /contacts/{contactId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "firstName": "John",
  "lastName": "Smith",
  "email": "john.smith@example.com"
}
```

**Delete Contact**
```http
DELETE /contacts/{contactId}
Authorization: Bearer {token}
```

**List Contacts**
```http
GET /contacts?page=1&limit=50&sort=lastName&order=asc&search=john
Authorization: Bearer {token}
```

**Search Contacts**
```http
POST /contacts/search
Content-Type: application/json
Authorization: Bearer {token}

{
  "query": "john",
  "filters": {
    "company": "Acme Corp",
    "jobTitle": "Manager",
    "createdAfter": "2025-01-01T00:00:00Z"
  },
  "sort": {
    "field": "lastName",
    "order": "asc"
  },
  "pagination": {
    "page": 1,
    "limit": 50
  }
}
```

### Deals API

#### Base Endpoint: `/deals`

**Create Deal**
```http
POST /deals
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Enterprise Software License",
  "value": 50000.00,
  "currency": "USD",
  "stage": "qualification",
  "pipelineId": "pipe_123",
  "contactId": "contact_456",
  "accountId": "account_789",
  "expectedCloseDate": "2025-03-15T00:00:00Z",
  "probability": 75,
  "description": "Annual software license renewal",
  "customFields": {
    "dealSource": "Referral",
    "competitorInfo": "Salesforce"
  }
}
```

**Get Deal**
```http
GET /deals/{dealId}
Authorization: Bearer {token}
```

**Update Deal**
```http
PUT /deals/{dealId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "stage": "proposal",
  "probability": 85,
  "value": 55000.00
}
```

**Move Deal Stage**
```http
POST /deals/{dealId}/move-stage
Content-Type: application/json
Authorization: Bearer {token}

{
  "newStage": "negotiation",
  "reason": "Customer approved proposal"
}
```

**List Deals**
```http
GET /deals?pipelineId=pipe_123&stage=qualification&page=1&limit=25
Authorization: Bearer {token}
```

**Deal Forecasting**
```http
GET /deals/forecast?pipelineId=pipe_123&period=quarter&year=2025
Authorization: Bearer {token}
```

### Leads API

#### Base Endpoint: `/leads`

**Create Lead**
```http
POST /leads
Content-Type: application/json
Authorization: Bearer {token}

{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@prospect.com",
  "phone": "+1-555-0456",
  "company": "Prospect Inc",
  "jobTitle": "VP Sales",
  "source": "Website Form",
  "status": "new",
  "score": 75,
  "customFields": {
    "budget": "50000-100000",
    "timeline": "Q2 2025"
  }
}
```

**Qualify Lead**
```http
POST /leads/{leadId}/qualify
Content-Type: application/json
Authorization: Bearer {token}

{
  "qualificationNotes": "Budget confirmed, decision maker identified",
  "score": 90,
  "nextAction": "Schedule demo"
}
```

**Convert Lead to Contact/Deal**
```http
POST /leads/{leadId}/convert
Content-Type: application/json
Authorization: Bearer {token}

{
  "createContact": true,
  "createDeal": true,
  "dealTitle": "New Business Opportunity",
  "dealValue": 75000.00,
  "pipelineId": "pipe_123"
}
```

**Lead Scoring Update**
```http
PUT /leads/{leadId}/score
Content-Type: application/json
Authorization: Bearer {token}

{
  "score": 85,
  "scoringFactors": [
    "Email opened",
    "Website visited",
    "Demo requested"
  ]
}
```

### Accounts API

#### Base Endpoint: `/accounts`

**Create Account**
```http
POST /accounts
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Enterprise Corp",
  "website": "https://enterprise-corp.com",
  "industry": "Technology",
  "employees": 5000,
  "annualRevenue": 100000000,
  "type": "Customer",
  "parentAccountId": null,
  "address": {
    "street": "456 Business Ave",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94105",
    "country": "US"
  }
}
```

**Get Account Hierarchy**
```http
GET /accounts/{accountId}/hierarchy
Authorization: Bearer {token}
```

**Account Relationship Management**
```http
POST /accounts/{accountId}/relationships
Content-Type: application/json
Authorization: Bearer {token}

{
  "relatedAccountId": "account_456",
  "relationshipType": "subsidiary",
  "description": "Wholly owned subsidiary"
}
```

**List Account Contacts**
```http
GET /accounts/{accountId}/contacts?page=1&limit=25
Authorization: Bearer {token}
```

**List Account Deals**
```http
GET /accounts/{accountId}/deals?status=active&page=1&limit=25
Authorization: Bearer {token}
```

### Activities API

#### Base Endpoint: `/activities`

**Create Task**
```http
POST /activities/tasks
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Follow up call",
  "description": "Call to discuss proposal feedback",
  "dueDate": "2025-01-25T14:00:00Z",
  "priority": "high",
  "status": "pending",
  "assignedTo": "user_123",
  "relatedTo": {
    "type": "deal",
    "id": "deal_456"
  }
}
```

**Create Event**
```http
POST /activities/events
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Product Demo",
  "description": "Demo of CRM features",
  "startTime": "2025-01-26T10:00:00Z",
  "endTime": "2025-01-26T11:00:00Z",
  "location": "Conference Room A",
  "attendees": ["user_123", "contact_456"],
  "relatedTo": {
    "type": "deal",
    "id": "deal_789"
  }
}
```

**Log Communication**
```http
POST /activities/communications
Content-Type: application/json
Authorization: Bearer {token}

{
  "type": "email",
  "direction": "outbound",
  "subject": "Proposal Follow-up",
  "content": "Thank you for reviewing our proposal...",
  "timestamp": "2025-01-24T09:30:00Z",
  "participants": ["contact_456"],
  "relatedTo": {
    "type": "deal",
    "id": "deal_789"
  }
}
```

**Get Activity Timeline**
```http
GET /activities/timeline?relatedType=deal&relatedId=deal_456&page=1&limit=20
Authorization: Bearer {token}
```

### Pipelines API

#### Base Endpoint: `/pipelines`

**Create Pipeline**
```http
POST /pipelines
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Sales Pipeline",
  "description": "Standard sales process",
  "stages": [
    {
      "name": "Lead",
      "order": 1,
      "probability": 10
    },
    {
      "name": "Qualification",
      "order": 2,
      "probability": 25
    },
    {
      "name": "Proposal",
      "order": 3,
      "probability": 50
    },
    {
      "name": "Negotiation",
      "order": 4,
      "probability": 75
    },
    {
      "name": "Closed Won",
      "order": 5,
      "probability": 100
    }
  ]
}
```

**Update Pipeline Stage**
```http
PUT /pipelines/{pipelineId}/stages/{stageId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Qualified Lead",
  "probability": 30,
  "automationRules": [
    {
      "trigger": "stage_entered",
      "action": "create_task",
      "parameters": {
        "title": "Qualification call",
        "dueInDays": 1
      }
    }
  ]
}
```

**Pipeline Analytics**
```http
GET /pipelines/{pipelineId}/analytics?period=month&year=2025
Authorization: Bearer {token}
```

**Stage Automation**
```http
POST /pipelines/{pipelineId}/stages/{stageId}/automation
Content-Type: application/json
Authorization: Bearer {token}

{
  "rules": [
    {
      "trigger": "deal_entered_stage",
      "conditions": [
        {
          "field": "value",
          "operator": "greater_than",
          "value": 10000
        }
      ],
      "actions": [
        {
          "type": "assign_user",
          "userId": "user_senior_rep"
        },
        {
          "type": "create_task",
          "title": "High-value deal review",
          "dueInHours": 24
        }
      ]
    }
  ]
}
```
--
-

## GraphQL Schemas

### Overview

GraphQL provides a unified data access layer for complex queries, real-time subscriptions, and efficient data fetching across multiple CRM modules. The schema supports federation across microservices while maintaining type safety and performance.

### Base Schema Structure

```graphql
# Root Query Type
type Query {
  # Contact queries
  contact(id: ID!): Contact
  contacts(filter: ContactFilter, sort: SortInput, pagination: PaginationInput): ContactConnection
  searchContacts(query: String!, filters: ContactFilter): [Contact!]!
  
  # Deal queries
  deal(id: ID!): Deal
  deals(filter: DealFilter, sort: SortInput, pagination: PaginationInput): DealConnection
  dealForecast(pipelineId: ID!, period: ForecastPeriod!): ForecastData
  
  # Lead queries
  lead(id: ID!): Lead
  leads(filter: LeadFilter, sort: SortInput, pagination: PaginationInput): LeadConnection
  leadScoring(leadId: ID!): LeadScore
  
  # Account queries
  account(id: ID!): Account
  accounts(filter: AccountFilter, sort: SortInput, pagination: PaginationInput): AccountConnection
  accountHierarchy(accountId: ID!): AccountHierarchy
  
  # Activity queries
  activity(id: ID!): Activity
  activities(filter: ActivityFilter, sort: SortInput, pagination: PaginationInput): ActivityConnection
  activityTimeline(relatedTo: RelatedEntityInput!): [Activity!]!
  
  # Pipeline queries
  pipeline(id: ID!): Pipeline
  pipelines: [Pipeline!]!
  pipelineAnalytics(pipelineId: ID!, period: AnalyticsPeriod!): PipelineAnalytics
  
  # Complex reporting queries
  salesReport(filter: SalesReportFilter!): SalesReport
  performanceMetrics(userId: ID!, period: MetricsPeriod!): PerformanceMetrics
  dashboardData(dashboardId: ID!): DashboardData
}

# Root Mutation Type
type Mutation {
  # Contact mutations
  createContact(input: CreateContactInput!): ContactPayload
  updateContact(id: ID!, input: UpdateContactInput!): ContactPayload
  deleteContact(id: ID!): DeletePayload
  
  # Deal mutations
  createDeal(input: CreateDealInput!): DealPayload
  updateDeal(id: ID!, input: UpdateDealInput!): DealPayload
  moveDealStage(dealId: ID!, stageId: ID!, reason: String): DealPayload
  
  # Lead mutations
  createLead(input: CreateLeadInput!): LeadPayload
  qualifyLead(leadId: ID!, input: QualifyLeadInput!): LeadPayload
  convertLead(leadId: ID!, input: ConvertLeadInput!): ConvertLeadPayload
  
  # Account mutations
  createAccount(input: CreateAccountInput!): AccountPayload
  updateAccount(id: ID!, input: UpdateAccountInput!): AccountPayload
  createAccountRelationship(input: AccountRelationshipInput!): AccountRelationshipPayload
  
  # Activity mutations
  createTask(input: CreateTaskInput!): TaskPayload
  createEvent(input: CreateEventInput!): EventPayload
  logCommunication(input: CommunicationInput!): CommunicationPayload
  
  # Pipeline mutations
  createPipeline(input: CreatePipelineInput!): PipelinePayload
  updatePipelineStage(pipelineId: ID!, stageId: ID!, input: UpdateStageInput!): StagePayload
}

# Root Subscription Type
type Subscription {
  # Real-time deal updates
  dealUpdated(pipelineId: ID): Deal
  dealStageChanged(dealId: ID): DealStageChange
  
  # Activity notifications
  newActivity(userId: ID!): Activity
  taskDue(userId: ID!): Task
  
  # Lead scoring updates
  leadScoreChanged(leadId: ID!): LeadScore
  
  # Pipeline notifications
  pipelineMetricsUpdated(pipelineId: ID!): PipelineMetrics
}
```

### Core Entity Types

```graphql
# Contact Type
type Contact {
  id: ID!
  firstName: String!
  lastName: String!
  email: String
  phone: String
  company: String
  jobTitle: String
  address: Address
  customFields: JSON
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # Relationships
  account: Account
  deals: [Deal!]!
  activities: [Activity!]!
  leadSource: Lead
  
  # Computed fields
  fullName: String!
  lastActivity: Activity
  dealValue: Float
}

# Deal Type
type Deal {
  id: ID!
  title: String!
  value: Float!
  currency: String!
  stage: String!
  probability: Int!
  expectedCloseDate: DateTime
  actualCloseDate: DateTime
  description: String
  customFields: JSON
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # Relationships
  pipeline: Pipeline!
  contact: Contact
  account: Account
  owner: User!
  activities: [Activity!]!
  
  # Computed fields
  daysInStage: Int!
  weightedValue: Float!
  isOverdue: Boolean!
  nextActivity: Activity
}

# Lead Type
type Lead {
  id: ID!
  firstName: String!
  lastName: String!
  email: String!
  phone: String
  company: String
  jobTitle: String
  source: String!
  status: LeadStatus!
  score: Int!
  customFields: JSON
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # Relationships
  owner: User
  activities: [Activity!]!
  scoringHistory: [LeadScoreHistory!]!
  
  # Computed fields
  fullName: String!
  isQualified: Boolean!
  daysSinceCreated: Int!
  lastActivity: Activity
}

# Account Type
type Account {
  id: ID!
  name: String!
  website: String
  industry: String
  employees: Int
  annualRevenue: Float
  type: AccountType!
  address: Address
  customFields: JSON
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # Relationships
  parentAccount: Account
  childAccounts: [Account!]!
  contacts: [Contact!]!
  deals: [Deal!]!
  activities: [Activity!]!
  
  # Computed fields
  totalDealValue: Float!
  activeDealCount: Int!
  lastActivity: Activity
  hierarchy: AccountHierarchy!
}

# Activity Type (Union of Task, Event, Communication)
union Activity = Task | Event | Communication

type Task {
  id: ID!
  title: String!
  description: String
  dueDate: DateTime
  priority: Priority!
  status: TaskStatus!
  assignedTo: User!
  relatedTo: RelatedEntity
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # Computed fields
  isOverdue: Boolean!
  daysUntilDue: Int
}

type Event {
  id: ID!
  title: String!
  description: String
  startTime: DateTime!
  endTime: DateTime!
  location: String
  attendees: [User!]!
  relatedTo: RelatedEntity
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # Computed fields
  duration: Int!
  isUpcoming: Boolean!
}

type Communication {
  id: ID!
  type: CommunicationType!
  direction: CommunicationDirection!
  subject: String
  content: String!
  timestamp: DateTime!
  participants: [Contact!]!
  relatedTo: RelatedEntity
  createdAt: DateTime!
  
  # Computed fields
  isRecent: Boolean!
}

# Pipeline Type
type Pipeline {
  id: ID!
  name: String!
  description: String
  stages: [PipelineStage!]!
  deals: [Deal!]!
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # Computed fields
  totalValue: Float!
  averageDealSize: Float!
  conversionRate: Float!
  averageSalesCycle: Int!
}

type PipelineStage {
  id: ID!
  name: String!
  order: Int!
  probability: Int!
  automationRules: [AutomationRule!]!
  deals: [Deal!]!
  
  # Computed fields
  dealCount: Int!
  stageValue: Float!
  averageTimeInStage: Int!
}
```

### Complex Query Patterns

```graphql
# Advanced filtering and sorting
query GetDealsWithComplexFilter {
  deals(
    filter: {
      value: { gte: 10000, lte: 100000 }
      stage: { in: ["qualification", "proposal"] }
      expectedCloseDate: { gte: "2025-01-01", lte: "2025-03-31" }
      account: { industry: { eq: "Technology" } }
      contact: { email: { contains: "@enterprise.com" } }
    }
    sort: { field: VALUE, direction: DESC }
    pagination: { first: 20, after: "cursor123" }
  ) {
    edges {
      node {
        id
        title
        value
        stage
        expectedCloseDate
        contact {
          fullName
          email
          company
        }
        account {
          name
          industry
        }
        activities(first: 5) {
          edges {
            node {
              ... on Task {
                title
                dueDate
                isOverdue
              }
              ... on Event {
                title
                startTime
                duration
              }
            }
          }
        }
      }
      cursor
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}

# Reporting and analytics query
query SalesPerformanceReport {
  salesReport(
    filter: {
      period: { start: "2025-01-01", end: "2025-01-31" }
      userId: "user_123"
      pipelineIds: ["pipe_1", "pipe_2"]
    }
  ) {
    totalRevenue
    dealsClosed
    averageDealSize
    conversionRate
    topPerformers {
      user {
        id
        name
      }
      revenue
      dealsWon
    }
    pipelineBreakdown {
      pipeline {
        id
        name
      }
      stageMetrics {
        stage {
          name
          probability
        }
        dealCount
        totalValue
        averageTimeInStage
      }
    }
  }
}

# Account hierarchy with nested relationships
query AccountHierarchyWithDeals($accountId: ID!) {
  account(id: $accountId) {
    id
    name
    hierarchy {
      parent {
        id
        name
        totalDealValue
      }
      children {
        id
        name
        contacts(first: 10) {
          edges {
            node {
              fullName
              jobTitle
              deals(filter: { status: ACTIVE }) {
                edges {
                  node {
                    title
                    value
                    stage
                    probability
                  }
                }
              }
            }
          }
        }
      }
    }
    deals(filter: { status: ACTIVE }) {
      edges {
        node {
          title
          value
          stage
          contact {
            fullName
          }
          nextActivity {
            ... on Task {
              title
              dueDate
            }
            ... on Event {
              title
              startTime
            }
          }
        }
      }
    }
  }
}
```

### Real-time Subscriptions

```graphql
# Deal stage change notifications
subscription DealStageUpdates($pipelineId: ID!) {
  dealStageChanged(pipelineId: $pipelineId) {
    deal {
      id
      title
      value
      stage
      contact {
        fullName
      }
    }
    previousStage
    newStage
    changedBy {
      id
      name
    }
    timestamp
    reason
  }
}

# Activity notifications for user
subscription UserActivityNotifications($userId: ID!) {
  newActivity(userId: $userId) {
    ... on Task {
      id
      title
      dueDate
      priority
      relatedTo {
        ... on Deal {
          title
          value
        }
        ... on Contact {
          fullName
        }
      }
    }
    ... on Event {
      id
      title
      startTime
      location
      attendees {
        name
      }
    }
  }
}

# Lead scoring updates
subscription LeadScoringUpdates($leadId: ID!) {
  leadScoreChanged(leadId: $leadId) {
    lead {
      id
      fullName
      company
    }
    previousScore
    newScore
    scoringFactors {
      factor
      points
      reason
    }
    timestamp
  }
}
```

### GraphQL Federation

```graphql
# Service schemas for microservices federation

# Contacts Service Schema
extend type Query {
  contact(id: ID!): Contact @provides(fields: "fullName email")
  contacts(filter: ContactFilter): [Contact!]!
}

type Contact @key(fields: "id") {
  id: ID!
  firstName: String!
  lastName: String!
  email: String
  fullName: String! @computed
  
  # External relationships
  account: Account @external
  deals: [Deal!]! @external
}

# Deals Service Schema
extend type Query {
  deal(id: ID!): Deal
  deals(filter: DealFilter): [Deal!]!
}

type Deal @key(fields: "id") {
  id: ID!
  title: String!
  value: Float!
  stage: String!
  
  # References to other services
  contact: Contact @external
  account: Account @external
  pipeline: Pipeline @external
}

# Gateway Schema Composition
type Query {
  # Unified queries across services
  customerOverview(customerId: ID!): CustomerOverview
  salesDashboard(userId: ID!): SalesDashboard
  pipelineReport(pipelineId: ID!): PipelineReport
}

type CustomerOverview {
  contact: Contact
  account: Account
  deals: [Deal!]!
  activities: [Activity!]!
  totalValue: Float!
  lastInteraction: DateTime
}
```

### Input Types and Filters

```graphql
# Filter input types
input ContactFilter {
  id: IDFilter
  firstName: StringFilter
  lastName: StringFilter
  email: StringFilter
  company: StringFilter
  createdAt: DateTimeFilter
  customFields: JSONFilter
}

input DealFilter {
  id: IDFilter
  title: StringFilter
  value: FloatFilter
  stage: StringFilter
  pipelineId: IDFilter
  contactId: IDFilter
  accountId: IDFilter
  expectedCloseDate: DateTimeFilter
  probability: IntFilter
}

input LeadFilter {
  id: IDFilter
  firstName: StringFilter
  lastName: StringFilter
  email: StringFilter
  company: StringFilter
  source: StringFilter
  status: LeadStatusFilter
  score: IntFilter
  createdAt: DateTimeFilter
}

# Generic filter types
input StringFilter {
  eq: String
  ne: String
  in: [String!]
  nin: [String!]
  contains: String
  startsWith: String
  endsWith: String
}

input FloatFilter {
  eq: Float
  ne: Float
  gt: Float
  gte: Float
  lt: Float
  lte: Float
  in: [Float!]
  nin: [Float!]
}

input DateTimeFilter {
  eq: DateTime
  ne: DateTime
  gt: DateTime
  gte: DateTime
  lt: DateTime
  lte: DateTime
}

# Pagination and sorting
input PaginationInput {
  first: Int
  after: String
  last: Int
  before: String
}

input SortInput {
  field: String!
  direction: SortDirection!
}

enum SortDirection {
  ASC
  DESC
}
```
---


## Security and Authentication

### Overview

The CRM platform implements a comprehensive security model with OAuth2/PKCE for authentication, JWT tokens for session management, role-based access control (RBAC), and advanced rate limiting. All API endpoints are secured by default with granular permission controls.

### OAuth2 Authentication Flow

#### Authorization Code Flow with PKCE (Web Applications)

```http
# Step 1: Authorization Request
GET /oauth2/authorize?
  response_type=code&
  client_id=crm_web_app&
  redirect_uri=https://app.crm-platform.com/callback&
  scope=contacts:read deals:write pipelines:manage&
  state=random_state_string&
  code_challenge=base64url_encoded_challenge&
  code_challenge_method=S256
```

```http
# Step 2: Token Exchange
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
client_id=crm_web_app&
code=authorization_code_from_step1&
redirect_uri=https://app.crm-platform.com/callback&
code_verifier=original_code_verifier
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "refresh_token_string",
  "scope": "contacts:read deals:write pipelines:manage"
}
```

#### Mobile Application Flow

```http
# Mobile OAuth2 with PKCE
POST /oauth2/token
Content-Type: application/json

{
  "grant_type": "authorization_code",
  "client_id": "crm_mobile_app",
  "code": "mobile_auth_code",
  "redirect_uri": "com.crm-platform.mobile://oauth/callback",
  "code_verifier": "mobile_code_verifier"
}
```

#### Refresh Token Flow

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&
client_id=crm_web_app&
refresh_token=existing_refresh_token
```

### JWT Token Structure

#### Access Token Claims

```json
{
  "iss": "https://auth.crm-platform.com",
  "sub": "user_12345",
  "aud": "crm-api",
  "exp": 1706097600,
  "iat": 1706094000,
  "jti": "token_unique_id",
  "scope": "contacts:read deals:write pipelines:manage",
  "tenant_id": "tenant_abc123",
  "user_roles": ["sales_rep", "team_lead"],
  "permissions": [
    "contacts:read",
    "contacts:write",
    "deals:read",
    "deals:write",
    "pipelines:read"
  ],
  "user_info": {
    "email": "user@company.com",
    "name": "John Doe",
    "department": "Sales"
  }
}
```

#### Refresh Token Claims

```json
{
  "iss": "https://auth.crm-platform.com",
  "sub": "user_12345",
  "aud": "crm-api",
  "exp": 1708686000,
  "iat": 1706094000,
  "jti": "refresh_token_unique_id",
  "token_type": "refresh",
  "tenant_id": "tenant_abc123"
}
```

### Role-Based Access Control (RBAC)

#### Role Hierarchy

```yaml
# System Roles
system_admin:
  description: "Full system access across all tenants"
  permissions: ["*"]
  
tenant_admin:
  description: "Full access within tenant"
  permissions: ["tenant:*"]
  
# Business Roles
sales_manager:
  description: "Manage sales team and processes"
  permissions:
    - "contacts:*"
    - "deals:*"
    - "leads:*"
    - "pipelines:*"
    - "reports:sales:*"
    - "users:team:read"
    
sales_rep:
  description: "Standard sales representative"
  permissions:
    - "contacts:read"
    - "contacts:write:own"
    - "deals:read"
    - "deals:write:own"
    - "leads:read"
    - "leads:write:own"
    - "activities:*:own"
    
marketing_manager:
  description: "Manage marketing campaigns and leads"
  permissions:
    - "leads:*"
    - "contacts:read"
    - "campaigns:*"
    - "reports:marketing:*"
    
support_agent:
  description: "Customer support access"
  permissions:
    - "contacts:read"
    - "accounts:read"
    - "activities:support:*"
    - "tickets:*"
```

#### Permission Scopes

```yaml
# Resource-based permissions
contacts:
  - read: "View contact information"
  - write: "Create and update contacts"
  - delete: "Delete contacts"
  - export: "Export contact data"
  
deals:
  - read: "View deal information"
  - write: "Create and update deals"
  - delete: "Delete deals"
  - move_stage: "Move deals between stages"
  - forecast: "Access forecasting data"
  
# Ownership-based permissions
own: "Only resources owned by the user"
team: "Resources owned by user's team"
department: "Resources within user's department"
tenant: "All resources within tenant"

# Action-based permissions
read: "View resource"
write: "Create/update resource"
delete: "Delete resource"
manage: "Full control over resource"
```

### API Endpoint Security

#### Authentication Headers

```http
# Bearer Token Authentication
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

# API Key Authentication (for integrations)
X-API-Key: api_key_string
X-API-Secret: api_secret_string

# Tenant Context
X-Tenant-ID: tenant_abc123
```

#### Permission-based Endpoint Protection

```yaml
# Endpoint permission mapping
GET /contacts:
  required_permissions: ["contacts:read"]
  ownership_filter: true
  
POST /contacts:
  required_permissions: ["contacts:write"]
  
PUT /contacts/{id}:
  required_permissions: ["contacts:write"]
  ownership_check: true
  
DELETE /contacts/{id}:
  required_permissions: ["contacts:delete"]
  ownership_check: true
  
GET /deals/forecast:
  required_permissions: ["deals:forecast"]
  role_requirements: ["sales_manager", "tenant_admin"]
  
POST /pipelines/{id}/stages/{stageId}/automation:
  required_permissions: ["pipelines:manage"]
  role_requirements: ["sales_manager", "tenant_admin"]
```

### Rate Limiting and Throttling

#### Rate Limiting Tiers

```yaml
# User-based rate limits
authenticated_user:
  requests_per_minute: 1000
  requests_per_hour: 10000
  requests_per_day: 100000
  burst_limit: 50
  
api_integration:
  requests_per_minute: 5000
  requests_per_hour: 50000
  requests_per_day: 500000
  burst_limit: 100
  
premium_integration:
  requests_per_minute: 10000
  requests_per_hour: 100000
  requests_per_day: 1000000
  burst_limit: 200

# Endpoint-specific limits
expensive_endpoints:
  - "/contacts/search": 100/minute
  - "/deals/forecast": 50/minute
  - "/reports/*": 20/minute
  - "/export/*": 10/minute
```

#### Rate Limiting Headers

```http
# Response headers for rate limiting
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1706094060
X-RateLimit-Retry-After: 60

# When rate limit exceeded (429 status)
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706094060
Retry-After: 60

{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Try again in 60 seconds.",
    "details": {
      "limit": 1000,
      "window": "1 minute",
      "retry_after": 60
    }
  }
}
```

### API Key Management

#### API Key Creation

```http
POST /api-keys
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "name": "Third-party Integration",
  "description": "Integration with external system",
  "permissions": [
    "contacts:read",
    "deals:read",
    "leads:write"
  ],
  "rate_limit_tier": "api_integration",
  "expires_at": "2025-12-31T23:59:59Z",
  "ip_whitelist": [
    "192.168.1.0/24",
    "10.0.0.100"
  ]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "key_123456",
    "name": "Third-party Integration",
    "api_key": "ak_live_1234567890abcdef",
    "api_secret": "sk_live_abcdef1234567890",
    "permissions": ["contacts:read", "deals:read", "leads:write"],
    "rate_limit_tier": "api_integration",
    "created_at": "2025-01-24T10:30:00Z",
    "expires_at": "2025-12-31T23:59:59Z"
  }
}
```

#### API Key Usage

```http
GET /contacts
X-API-Key: ak_live_1234567890abcdef
X-API-Secret: sk_live_abcdef1234567890
X-Tenant-ID: tenant_abc123
```

### Security Headers and CORS

#### Required Security Headers

```http
# Response security headers
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'
Referrer-Policy: strict-origin-when-cross-origin
```

#### CORS Configuration

```yaml
# CORS settings
allowed_origins:
  - "https://app.crm-platform.com"
  - "https://*.crm-platform.com"
  - "https://localhost:3000"  # Development only
  
allowed_methods:
  - GET
  - POST
  - PUT
  - DELETE
  - OPTIONS
  
allowed_headers:
  - Authorization
  - Content-Type
  - X-API-Key
  - X-API-Secret
  - X-Tenant-ID
  - X-Request-ID
  
exposed_headers:
  - X-RateLimit-Limit
  - X-RateLimit-Remaining
  - X-RateLimit-Reset
  
max_age: 86400
credentials: true
```

### Error Handling and Security

#### Authentication Errors

```json
# 401 Unauthorized
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or expired token",
    "details": {
      "error_type": "token_expired",
      "expires_at": "2025-01-24T09:30:00Z"
    }
  }
}

# 403 Forbidden
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "Insufficient permissions",
    "details": {
      "required_permissions": ["deals:write"],
      "user_permissions": ["deals:read"]
    }
  }
}
```

#### Security Event Logging

```yaml
# Security events to log
authentication_events:
  - login_success
  - login_failure
  - token_refresh
  - logout
  
authorization_events:
  - permission_denied
  - role_escalation_attempt
  - resource_access_denied
  
api_security_events:
  - rate_limit_exceeded
  - invalid_api_key
  - suspicious_request_pattern
  - data_export_request
```

### Multi-tenant Security

#### Tenant Isolation

```yaml
# Tenant-based security
tenant_isolation:
  database_level: "Separate schemas per tenant"
  application_level: "Tenant ID in all queries"
  api_level: "Tenant context in JWT and headers"
  
tenant_permissions:
  cross_tenant_access: false
  tenant_admin_override: true
  system_admin_access: true
```

#### Tenant-specific Configuration

```json
{
  "tenant_id": "tenant_abc123",
  "security_config": {
    "password_policy": {
      "min_length": 12,
      "require_uppercase": true,
      "require_numbers": true,
      "require_symbols": true,
      "max_age_days": 90
    },
    "session_config": {
      "timeout_minutes": 480,
      "concurrent_sessions": 3,
      "remember_me_days": 30
    },
    "api_security": {
      "rate_limit_multiplier": 1.5,
      "ip_whitelist_enabled": true,
      "api_key_rotation_days": 90
    }
  }
}
```--
-

## Integration API Examples

### Overview

The CRM platform provides comprehensive integration capabilities through RESTful APIs, webhooks, and specialized endpoints for third-party applications, marketplace apps, and AI-powered features. This section demonstrates practical integration patterns with real-world examples.

### Third-Party Integration Patterns

#### Email Marketing Platform Integration

**Sync Contacts to Email Platform**
```http
POST /integrations/email-marketing/sync-contacts
Authorization: Bearer {token}
Content-Type: application/json

{
  "provider": "mailchimp",
  "list_id": "mailchimp_list_123",
  "contact_filter": {
    "tags": ["newsletter_subscriber"],
    "status": "active",
    "created_after": "2025-01-01T00:00:00Z"
  },
  "field_mapping": {
    "email": "email_address",
    "firstName": "FNAME",
    "lastName": "LNAME",
    "company": "COMPANY",
    "customFields.industry": "INDUSTRY"
  },
  "sync_options": {
    "update_existing": true,
    "create_segments": true,
    "bidirectional": false
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "sync_id": "sync_789",
    "status": "in_progress",
    "contacts_queued": 1250,
    "estimated_completion": "2025-01-24T10:45:00Z",
    "webhook_url": "https://api.crm-platform.com/webhooks/sync-status/sync_789"
  }
}
```

#### Calendar Integration (Google Calendar)

**Create Meeting from Deal Activity**
```http
POST /integrations/calendar/create-event
Authorization: Bearer {token}
Content-Type: application/json

{
  "provider": "google_calendar",
  "calendar_id": "primary",
  "event": {
    "title": "Product Demo - Enterprise Corp",
    "description": "Demo of CRM features for potential $75k deal",
    "start_time": "2025-01-26T10:00:00Z",
    "end_time": "2025-01-26T11:00:00Z",
    "location": "Conference Room A",
    "attendees": [
      {
        "email": "john.doe@enterprise-corp.com",
        "name": "John Doe",
        "contact_id": "contact_456"
      }
    ]
  },
  "crm_context": {
    "deal_id": "deal_789",
    "activity_type": "demo",
    "create_crm_activity": true
  }
}
```

#### Accounting System Integration (QuickBooks)

**Sync Closed Deals to Invoices**
```http
POST /integrations/accounting/create-invoice
Authorization: Bearer {token}
Content-Type: application/json

{
  "provider": "quickbooks",
  "deal_id": "deal_789",
  "invoice_data": {
    "customer": {
      "name": "Enterprise Corp",
      "email": "billing@enterprise-corp.com",
      "address": {
        "street": "456 Business Ave",
        "city": "San Francisco",
        "state": "CA",
        "zip": "94105"
      }
    },
    "line_items": [
      {
        "description": "CRM Software License - Annual",
        "quantity": 1,
        "rate": 75000.00,
        "amount": 75000.00
      }
    ],
    "terms": "Net 30",
    "due_date": "2025-02-25"
  },
  "sync_options": {
    "update_deal_status": true,
    "create_payment_tracking": true
  }
}
```

### Marketplace API Examples

#### App Registration and Authentication

**Register Marketplace App**
```http
POST /marketplace/apps/register
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "app_info": {
    "name": "Advanced Analytics Dashboard",
    "description": "Enhanced reporting and analytics for CRM data",
    "version": "1.0.0",
    "developer": {
      "name": "Analytics Pro Inc",
      "email": "support@analyticspro.com",
      "website": "https://analyticspro.com"
    }
  },
  "permissions_requested": [
    "deals:read",
    "contacts:read",
    "pipelines:read",
    "reports:read",
    "custom_objects:read"
  ],
  "webhook_endpoints": [
    {
      "url": "https://app.analyticspro.com/webhooks/crm-data",
      "events": ["deal.updated", "contact.created", "pipeline.changed"]
    }
  ],
  "oauth_config": {
    "redirect_uris": [
      "https://app.analyticspro.com/oauth/callback"
    ],
    "scopes": ["analytics:read", "reports:generate"]
  }
}
```

**App Installation by Tenant**
```http
POST /marketplace/apps/install
Authorization: Bearer {tenant_admin_token}
Content-Type: application/json

{
  "app_id": "app_analytics_pro_123",
  "installation_config": {
    "permissions_granted": [
      "deals:read",
      "contacts:read",
      "pipelines:read"
    ],
    "data_access_level": "tenant",
    "webhook_config": {
      "enabled": true,
      "events": ["deal.updated", "contact.created"]
    },
    "custom_settings": {
      "report_frequency": "daily",
      "dashboard_theme": "dark",
      "data_retention_days": 90
    }
  }
}
```

#### Marketplace App Data Access

**Query CRM Data from Marketplace App**
```http
GET /marketplace/data/deals?status=active&limit=100
Authorization: Bearer {marketplace_app_token}
X-App-ID: app_analytics_pro_123
X-Tenant-ID: tenant_abc123
```

**Create Custom Dashboard Widget**
```http
POST /marketplace/widgets
Authorization: Bearer {marketplace_app_token}
Content-Type: application/json

{
  "widget_info": {
    "name": "Sales Velocity Chart",
    "description": "Track deal velocity across pipelines",
    "type": "chart",
    "size": "medium"
  },
  "data_source": {
    "query": "deals",
    "filters": {
      "status": "active",
      "created_after": "30_days_ago"
    },
    "aggregations": [
      {
        "field": "value",
        "operation": "sum",
        "group_by": "stage"
      }
    ]
  },
  "visualization": {
    "chart_type": "line",
    "x_axis": "date",
    "y_axis": "deal_value",
    "color_scheme": "blue"
  }
}
```

### AI Module Integration

#### Lead Scoring API

**Get AI Lead Score**
```http
POST /ai/lead-scoring/score
Authorization: Bearer {token}
Content-Type: application/json

{
  "lead_id": "lead_456",
  "scoring_factors": {
    "demographic": {
      "company_size": 500,
      "industry": "Technology",
      "job_title": "VP Sales",
      "location": "San Francisco"
    },
    "behavioral": {
      "email_opens": 5,
      "website_visits": 12,
      "content_downloads": 3,
      "demo_requested": true
    },
    "engagement": {
      "response_time_hours": 2,
      "meeting_acceptance_rate": 0.8,
      "referral_source": "existing_customer"
    }
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "lead_id": "lead_456",
    "score": 87,
    "confidence": 0.92,
    "scoring_breakdown": {
      "demographic_score": 25,
      "behavioral_score": 35,
      "engagement_score": 27
    },
    "key_factors": [
      {
        "factor": "demo_requested",
        "impact": "high",
        "points": 15
      },
      {
        "factor": "company_size",
        "impact": "medium",
        "points": 10
      }
    ],
    "recommendations": [
      "Schedule demo within 24 hours",
      "Assign to senior sales rep",
      "Send technical documentation"
    ],
    "next_best_action": {
      "action": "schedule_demo",
      "priority": "high",
      "suggested_time": "2025-01-25T14:00:00Z"
    }
  }
}
```

#### Predictive Analytics

**Deal Win Probability**
```http
POST /ai/analytics/deal-probability
Authorization: Bearer {token}
Content-Type: application/json

{
  "deal_id": "deal_789",
  "analysis_factors": {
    "deal_characteristics": {
      "value": 75000,
      "stage": "proposal",
      "days_in_stage": 14,
      "contact_engagement": "high"
    },
    "historical_context": {
      "similar_deals_won": 12,
      "similar_deals_lost": 3,
      "average_sales_cycle": 45
    },
    "competitive_intel": {
      "competitors_identified": ["Salesforce", "HubSpot"],
      "competitive_advantage": "pricing"
    }
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "deal_id": "deal_789",
    "win_probability": 0.78,
    "confidence_interval": [0.72, 0.84],
    "risk_factors": [
      {
        "factor": "long_sales_cycle",
        "impact": "medium",
        "mitigation": "Increase follow-up frequency"
      }
    ],
    "success_indicators": [
      {
        "factor": "high_engagement",
        "strength": "strong",
        "evidence": "Multiple stakeholder meetings"
      }
    ],
    "forecasting": {
      "expected_close_date": "2025-02-15",
      "confidence": 0.85,
      "revenue_impact": 58500
    }
  }
}
```

#### AI-Powered Data Enrichment

**Enrich Contact Information**
```http
POST /ai/data-enrichment/contact
Authorization: Bearer {token}
Content-Type: application/json

{
  "contact_id": "contact_456",
  "enrichment_sources": [
    "linkedin",
    "company_database",
    "social_media",
    "public_records"
  ],
  "fields_to_enrich": [
    "job_title",
    "company_info",
    "social_profiles",
    "contact_preferences",
    "technology_stack"
  ]
}
```

### Webhook Patterns

#### Webhook Registration

**Register Webhook Endpoint**
```http
POST /webhooks
Authorization: Bearer {token}
Content-Type: application/json

{
  "url": "https://external-system.com/webhooks/crm-events",
  "events": [
    "deal.created",
    "deal.stage_changed",
    "contact.updated",
    "lead.converted"
  ],
  "filters": {
    "deal.stage_changed": {
      "pipeline_ids": ["pipe_123", "pipe_456"],
      "stages": ["closed_won", "closed_lost"]
    }
  },
  "security": {
    "secret": "webhook_secret_key",
    "signature_header": "X-CRM-Signature"
  },
  "retry_config": {
    "max_retries": 3,
    "retry_delay_seconds": 60,
    "timeout_seconds": 30
  }
}
```

#### Webhook Payload Examples

**Deal Stage Changed Event**
```json
{
  "event_id": "evt_123456789",
  "event_type": "deal.stage_changed",
  "timestamp": "2025-01-24T10:30:00Z",
  "tenant_id": "tenant_abc123",
  "data": {
    "deal": {
      "id": "deal_789",
      "title": "Enterprise Software License",
      "value": 75000.00,
      "currency": "USD",
      "previous_stage": "proposal",
      "current_stage": "closed_won",
      "pipeline_id": "pipe_123",
      "contact": {
        "id": "contact_456",
        "name": "John Doe",
        "email": "john.doe@enterprise-corp.com"
      },
      "account": {
        "id": "account_789",
        "name": "Enterprise Corp"
      }
    },
    "change_info": {
      "changed_by": {
        "id": "user_123",
        "name": "Sales Rep",
        "email": "rep@crm-platform.com"
      },
      "reason": "Customer signed contract",
      "stage_duration_days": 14
    }
  }
}
```

**Lead Converted Event**
```json
{
  "event_id": "evt_987654321",
  "event_type": "lead.converted",
  "timestamp": "2025-01-24T11:15:00Z",
  "tenant_id": "tenant_abc123",
  "data": {
    "lead": {
      "id": "lead_456",
      "name": "Jane Smith",
      "email": "jane.smith@prospect.com",
      "company": "Prospect Inc",
      "score": 85
    },
    "conversion_result": {
      "contact_created": {
        "id": "contact_789",
        "name": "Jane Smith"
      },
      "deal_created": {
        "id": "deal_101112",
        "title": "New Business Opportunity",
        "value": 50000.00
      },
      "account_created": {
        "id": "account_456",
        "name": "Prospect Inc"
      }
    },
    "conversion_info": {
      "converted_by": {
        "id": "user_456",
        "name": "Lead Qualifier"
      },
      "qualification_notes": "Budget confirmed, decision maker identified"
    }
  }
}
```

### Real-time Integration Examples

#### WebSocket Connection for Live Updates

**Establish WebSocket Connection**
```javascript
// Client-side WebSocket connection
const ws = new WebSocket('wss://api.crm-platform.com/ws');

// Authentication
ws.onopen = function() {
  ws.send(JSON.stringify({
    type: 'auth',
    token: 'bearer_token_here',
    tenant_id: 'tenant_abc123'
  }));
};

// Subscribe to deal updates
ws.send(JSON.stringify({
  type: 'subscribe',
  channel: 'deals',
  filters: {
    pipeline_id: 'pipe_123',
    user_id: 'user_456'
  }
}));

// Handle incoming messages
ws.onmessage = function(event) {
  const message = JSON.parse(event.data);
  
  switch(message.type) {
    case 'deal.updated':
      updateDealInUI(message.data);
      break;
    case 'deal.stage_changed':
      showStageChangeNotification(message.data);
      break;
  }
};
```

#### Server-Sent Events (SSE) for Notifications

**SSE Endpoint for Activity Feed**
```http
GET /sse/activity-feed?user_id=user_123
Authorization: Bearer {token}
Accept: text/event-stream
Cache-Control: no-cache
```

**SSE Response Stream:**
```
data: {"type": "task.due", "data": {"task_id": "task_789", "title": "Follow up call", "due_date": "2025-01-24T14:00:00Z"}}

data: {"type": "deal.updated", "data": {"deal_id": "deal_456", "title": "Updated deal value", "value": 80000}}

data: {"type": "lead.scored", "data": {"lead_id": "lead_123", "score": 92, "change": "+15"}}
```

### Bulk Operations and Batch Processing

#### Bulk Data Import

**Import Contacts in Batch**
```http
POST /bulk/contacts/import
Authorization: Bearer {token}
Content-Type: application/json

{
  "import_options": {
    "update_existing": true,
    "skip_duplicates": false,
    "validation_level": "strict"
  },
  "field_mapping": {
    "email": "email_address",
    "first_name": "firstName",
    "last_name": "lastName",
    "company_name": "company"
  },
  "contacts": [
    {
      "email_address": "contact1@company.com",
      "firstName": "John",
      "lastName": "Doe",
      "company": "Company A"
    },
    {
      "email_address": "contact2@company.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "company": "Company B"
    }
  ]
}
```

#### Bulk Data Export

**Export Deals with Filters**
```http
POST /bulk/deals/export
Authorization: Bearer {token}
Content-Type: application/json

{
  "export_format": "csv",
  "filters": {
    "created_after": "2025-01-01T00:00:00Z",
    "stage": ["proposal", "negotiation", "closed_won"],
    "value_min": 10000
  },
  "fields": [
    "id",
    "title",
    "value",
    "stage",
    "contact.name",
    "contact.email",
    "account.name",
    "created_at",
    "expected_close_date"
  ],
  "delivery": {
    "method": "webhook",
    "webhook_url": "https://external-system.com/export-complete"
  }
}
```

### Error Handling in Integrations

#### Integration-specific Error Responses

```json
{
  "success": false,
  "error": {
    "code": "INTEGRATION_ERROR",
    "message": "Failed to sync with external system",
    "details": {
      "integration_type": "email_marketing",
      "provider": "mailchimp",
      "provider_error": {
        "code": "INVALID_LIST_ID",
        "message": "The list ID provided does not exist"
      },
      "retry_possible": true,
      "retry_after": 300
    }
  }
}
```

#### Webhook Delivery Failures

```json
{
  "webhook_id": "webhook_123",
  "delivery_attempt": {
    "attempt_number": 2,
    "status": "failed",
    "error": "Connection timeout",
    "response_code": null,
    "next_retry": "2025-01-24T10:35:00Z"
  },
  "event": {
    "id": "evt_456",
    "type": "deal.created",
    "timestamp": "2025-01-24T10:30:00Z"
  }
}
```