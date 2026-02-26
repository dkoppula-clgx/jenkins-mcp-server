# Jenkins MCP Server

MCP (Model Context Protocol) server for Jenkins integration **specifically for the PBS (Property Business Services) space**. This server provides AI assistants with tools to interact with Jenkins for build monitoring and deployment operations within the PBS ecosystem.

## Features

- Retrieve latest build status for PBS Jenkins jobs by branch
- Retrieve comprehensive build history for PBS jobs by branch
- List all available PBS Jenkins jobs
- List all PBS GitHub repositories used for deployments
- Trigger deployments for PBS applications

## MCP Tools

This server exposes 5 MCP tools:

1. **getLatestBuildDetailsByJobAndBranch**  
   Retrieves the most recent build information for a specified PBS Jenkins job and branch, including build number, result status, timestamp, build version, and other relevant metrics.

2. **getBuildDetailsByJobAndBranch**  
   Returns a comprehensive list of all builds for a specified PBS Jenkins job and branch, with references to first build, last build, last successful build, last failed build, and complete build history.

3. **getAllJobs**  
   Returns a list of all configured PBS Jenkins jobs available in the system. Use this to discover valid job names before querying build details.

4. **getAllRepos**  
   Returns a list of all PBS GitHub repositories associated with Jenkins jobs. These repositories represent the deployable PBS services and are used as source for deployments. Use this to discover available repositories and their names before triggering deployments

5. **deployApplication**  
   Triggers a Jenkins deployment job for a PBS application with specified parameters: GitHub repository name, Git branch, artifact version, and target environments (e.g., dev-usw1-kf, qa-usw1-kf).

## Setup

No setup needed. Jenkins credentials are required to be passed as VM arguments when starting the application.

## Running

### Option 1: Using Gradle bootRun

```bash
./gradlew bootRun -DUSERNAME=your-jenkins-username -DPASSWORD=your-jenkins-password
```

### Option 2: Using IntelliJ IDEA

1. Open **Run** → **Edit Configurations**
2. Select your application configuration
3. Click **Modify options** → **Add VM options**
4. Add the following VM arguments:
   ```
   -DUSERNAME=your-jenkins-username -DPASSWORD=your-jenkins-password
   ```
5. Click **Apply** and **Run**

## Registering the MCP Server with Copilot

Add the following to your `mcp.json` configuration file:

```json
"jenkins": {
    "type": "http",
    "url": "http://localhost:8080/mcp"
}
```

Replace `8080` with your configured server port if different.

