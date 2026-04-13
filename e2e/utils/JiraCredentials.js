// utils/JiraCredentials.js
// Reads Jira credentials from environment variables (.env file).
// Connection name is NOT read from .env — it is passed in dynamically
// (generated at runtime by GenerateTestData.generateJiraConnectionName()).

// Load .env in the worker process (Playwright workers are separate Node processes)
import 'dotenv/config';

/**
 * Returns Jira credentials from process.env.
 * Throws a descriptive error immediately if any required variable is missing —
 * so tests fail fast before touching the browser UI.
 *
 * @param {string} connectionName - Dynamically generated connection name (from GenerateTestData)
 * @returns {{ jiraUrl: string, connectionName: string, email: string, apiToken: string }}
 */
function getJiraCredentials(connectionName) {
  const { JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN } = process.env;

  const missing = [];
  if (!JIRA_BASE_URL)  missing.push('JIRA_BASE_URL');
  if (!JIRA_EMAIL)     missing.push('JIRA_EMAIL');
  if (!JIRA_API_TOKEN) missing.push('JIRA_API_TOKEN');

  if (missing.length > 0) {
    throw new Error(
      `Missing required environment variables: ${missing.join(', ')}.\n` +
      `Create a .env file in the e2e/ directory with JIRA_BASE_URL, JIRA_EMAIL, and JIRA_API_TOKEN.`
    );
  }

  return {
    jiraUrl:        JIRA_BASE_URL,
    connectionName: connectionName,
    email:          JIRA_EMAIL,
    apiToken:       JIRA_API_TOKEN,
  };
}

export { getJiraCredentials };
