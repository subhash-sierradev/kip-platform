import { faker } from "@faker-js/faker";

class GenerateTestData {

constructor() 
{
  // Generate unique identifiers
  this.timestamp = Date.now();
}

// Basic Webhook Test Data
generateWebhookName() 
{
  return `Webhook_${faker.lorem.word()}_${this.timestamp}`;
}

// Jira Connection Name
generateJiraConnectionName()
{
  return `JiraConn_${faker.lorem.word()}_${this.timestamp}`;
}

generateDescription() 
{
  return faker.lorem.sentence(10);
}

// Sample Payload Generation
generateSamplePayload() 
{
  return {
    "form": {
      "title": faker.lorem.sentence(6),
      "assigneeEmail": faker.internet.email(),
      "priority": faker.helpers.arrayElement(['Low', 'Medium', 'High', 'Critical']),
      "department": faker.helpers.arrayElement(['IT Support', 'Security', 'Operations', 'Development']),
      "category": faker.helpers.arrayElement(['Infrastructure', 'Bug', 'Feature Request', 'Security Incident']),
      "description": faker.lorem.paragraph(),
      "reportedBy": faker.person.fullName(),
      "affectedSystems": faker.helpers.arrayElements(['Web App', 'Mobile App', 'API', 'Database', 'Authentication System'], { min: 2, max: 4 })
    },
    "metadata": {
      "timestamp": faker.date.recent().toISOString(),
      "source": faker.helpers.arrayElement(['monitoring-system', 'user-report', 'automated-scan']),
      "severity": faker.helpers.arrayElement(['low', 'medium', 'high', 'critical'])
    }
  };
}

// Basic Test Data Set
getBasicDetailsTestData() 
{
  return {
    validWebhookName: this.generateWebhookName(),
    validDescription: this.generateDescription(),
    samplePayload: this.generateSamplePayload(),
    jiraConnectionName: this.generateJiraConnectionName(),
    projectName: 'KIP Test Project A',
    issueType: 'Epic',
    summaryTemplate: '{{form.title}} - {{form.priority}} - {{form.category}}',
    descriptionTemplate: `Issue Details:
    Description: {{form.description}}
    Department: {{form.department}}
    Reported By: {{form.reportedBy}}
    Affected Systems: {{form.affectedSystems}}
    Additional Information:
    Source: {{metadata.source}}
    Timestamp: {{metadata.timestamp}}
    Severity: {{metadata.severity}}`
  };
}

// ArcGIS Integration Test Data
generateArcGISIntegrationName() 
{
  return `ArcGIS Integration ${faker.lorem.word()} ${this.timestamp}`;
}

getArcGISIntegrationConfig() 
{
  // Calculate tomorrow's date dynamically using local date components (timezone-safe)
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const year  = tomorrow.getFullYear();
  const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
  const day   = String(tomorrow.getDate()).padStart(2, '0');
  const tomorrowDate = `${year}-${month}-${day}`; // Format: YYYY-MM-DD (local, not UTC)
  
  return {
    name: this.generateArcGISIntegrationName(),
    description: this.generateDescription(),
    itemSubtype: 'Supplemental Report',
    schedule: {
      startDate: tomorrowDate,
      executionTime: '09:00',
      days: faker.helpers.arrayElements(['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'], { min: 2, max: 3 })
    },
    connection: {
      useExisting: true,
      selectFirstActive: true  // Dynamically picks the first Active connection at runtime
    },
    fieldMappings: [
      {
        row: 1,
        documentField: 'latitude (Double)',
        transformation: 'PASSTHROUGH',
        arcgisField: 'Latitude',
        isMandatory: false
      }
    ]
  };
}

// ArcGIS End-of-Month Schedule Test Data (startDate excluded — field is disabled in UI)
getEndOfMonthScheduleConfig() {
  return {
    frequency: 'Monthly',
    months: ['Jan', 'Mar', 'May'],
    monthsDisplay: ['January', 'March', 'May'],
    executionTime: '09:00'
  };
}

// ArcGIS Integration Test Data - CRON Schedule
getArcGISCronIntegrationConfig()
{
  return {
    name: this.generateArcGISIntegrationName(),
    description: this.generateDescription(),
    itemSubtype: 'Supplemental Report',
    schedule: {
      frequency: 'CRON',
      cronExpression: '0 0 9 * * ?'
    },
    connection: {
      useExisting: true,
      selectFirstActive: true
    },
    fieldMappings: [
      {
        row: 1,
        documentField: 'latitude (Double)',
        transformation: 'PASSTHROUGH',
        arcgisField: 'Latitude',
        isMandatory: false
      }
    ]
  };
}

// Confluence Integration Test Data
generateConfluenceIntegrationName() 
{
  return `Confluence_Integration_${faker.lorem.word()}_${this.timestamp}`;
}

getConfluenceIntegrationConfig() 
{
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const year  = tomorrow.getFullYear();
  const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
  const day   = String(tomorrow.getDate()).padStart(2, '0');
  const tomorrowDate = `${year}-${month}-${day}`;

  return {
    name: this.generateConfluenceIntegrationName(),
    description: this.generateDescription(),
    schedule: {
      mode: 'Rolling Window',
      frequency: 'Weekly',
      startDate: tomorrowDate,
      executionTime: '09:00',
      days: ['Mon'],
      rollingWindowSize: '7'
    },
    connection: {
      useExisting: true,
      selectFirstActive: true
    },
    fieldMappings: [
      {
        sourceField: 'Title',
        transformation: 'PASSTHROUGH',
        confluenceField: 'Page Title'
      }
    ]
  };
}

// ArcGIS Integration Update Test Data
generateArcGISUpdatedIntegrationName()
{
  return `ArcGIS Updated ${faker.lorem.word()} ${this.timestamp}`;
}

getArcGISUpdateConfig()
{
  // Use a date two days from today for the updated schedule
  const futureDate = new Date();
  futureDate.setDate(futureDate.getDate() + 2);
  const futureDateStr = futureDate.toISOString().split('T')[0]; // Format: YYYY-MM-DD

  return {
    newName: this.generateArcGISUpdatedIntegrationName(),
    newDescription: this.generateDescription(),
    // Change subtype from the default 'Supplemental Report'
    newItemSubtype: 'Dynamic Document',
    selectFirstDynamicDoc: true, // Picks the first available dynamic document at runtime
    schedule: {
      // Change frequency to 'Monthly' with specific months selected
      frequency: 'Monthly',
      startDate: futureDateStr,
      executionTime: '10:00',
      months: ['Jan', 'Feb', 'Mar'] // Run in January, February, and March
    },
    // New field mapping row to be added in Step 5
    newFieldMapping: {
      documentField: 'longitude (Double)',
      transformation: 'PASSTHROUGH',
      arcgisField: 'Longitude'
    }
  };
}
}

export { GenerateTestData };
