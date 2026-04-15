## Description
A **custom Jenkins MCP (Model Context Protocol) server** tailored to individual users' credentials and project configurations. This server provides AI assistants with tools to interact with Jenkins for build monitoring and deployment operations.

## Features

- Retrieve all build details for Jenkins jobs by parent and child job
- Retrieve build version details for a specific build number
- List all project-specific Jenkins jobs (our applications)
- List all common Jenkins jobs with their child jobs and descriptions
- List all GitHub repositories used for deployments
- Trigger deployments for applications
- Trigger builds for applications by job and branch
- Execute KF self-service CLI commands for environments (manage Kubernetes services: start, stop, restart)
- Run Veracode security scans on application versions

## MCP Tools

This server exposes 9 MCP tools:

1. **getAllBuildDetails**  
   Fetches all/recent build details for a job given the parentJob and childJob. Returns comprehensive build information including all builds, first build, last build, last successful build, last failed build, and more.

2. **getBuildDetailsByBuildNumber**  
   Fetches build version details for a specific build number of a job. Returns detailed information for a particular build including build number, result, timestamp, build version, and other relevant details.

3. **getAllProjectJobs**  
   Fetches a list of all project-specific Jenkins jobs available in the system. Returns individual application jobs. **IMPORTANT: This tool MUST be called BEFORE performing any project-specific job-related actions** to discover available project-specific jobs and their exact names.

4. **getAllCommonJobs**  
   Fetches a map of all common Jenkins jobs with their child jobs and descriptions. Returns parent-child job relationships with descriptions (e.g., build-release, self-service). Each child job includes its name and description of what it manages. **IMPORTANT: This tool MUST be called BEFORE performing any common job-related actions** to discover available common jobs, their descriptions, and their exact names.

5. **getAllRepos**  
   Fetches a list of all GitHub repositories associated with Jenkins jobs. **IMPORTANT: This tool MUST be called BEFORE performing any deployment-related actions** (such as deployApplication) to discover available repositories and their exact names.

6. **deployApplication**  
   Deploys an application using Jenkins. Triggers a deployment job with specified parameters including GitHub repository, branch, artifact version, and target environments.

7. **buildApplication**  
   Builds an application for a specified Jenkins job and branch. Triggers a Jenkins build job and returns the job URL for monitoring the build progress.

8. **manageKubernetesServicesInKfPlatform**  
   Performs start, stop, restart and other operations on k8s services in kf platform. Use this to manage your applications on k8s with the ability to start, restart, stop (and any other similar operation) them. Triggers a Jenkins job that requires manual approval to execute KF commands.

9. **runVeracodeScan**  
   Runs a Veracode security scan on a specified application version. Triggers a Jenkins Veracode scan job with the specified scan type and patterns.

## Setup
NOTE: it is advised to copy-paste the commands to facilitate seamless setup and integration without errors

### Specialized Agent

A **specialized jenkins-actions** agent is created to ensure seamless usage of this MCP server.  
This can be installed easily locally (workspace) or globally

#### - Manually copying the agent from .github/agents into either your-workspace/.github/agents or your-user/.copilot/agents
#### (or)
#### - NPM script.  
Follow these steps:
1. If you want to install in a single project, navigate to the project's directory, else no need to navigate
2. Then run this script
```bash
npx github:dkoppula-clgx/jenkins-mcp-server
```
3. Select between global and local installations
<img width="1462" height="477" alt="image" src="https://github.com/user-attachments/assets/e439ac79-8f64-43da-8de4-83f4616be07a" />
4. If the agent is already present in the directory, it is replaced


### Server Setup

#### Initial Configuration

Before running the server for the first time, you need to scaffold your configuration:

1. **Fork the project** to your own repository
2. **Run the setup script** to auto-discover your Jenkins jobs:
   ```bash
   setup.bat
   ```
3. **Provide the required information** when prompted:
   - Business Unit (e.g., `credit-us`)
   - Project Space (e.g., `pbs`, `dhqcare`)
   - Jenkins username
   - Jenkins password (securely masked)
   <img width="830" height="327" alt="image" src="https://github.com/user-attachments/assets/8b20fcfb-0e29-470d-b2b0-b242663cc255" />


The script will:
- Connect to your Jenkins space
- Discover all available WorkflowMultiBranchProject jobs
- Discover all GitHub repositories from the build-release job (if available)
- Generate `src/main/resources/application.yml` with your configuration

**Note:** The template configuration is available at `src/main/resources/application-template.yml` for reference.

#### What Gets Configured

The setup automatically populates:
- `jenkins.api-paths.business-unit-job` - your Business Unit
- `jenkins.api-paths.project-space-job` - your Project Space
- `jenkins.project-specific-jobs` - all discovered jobs in your space
- `jenkins.integration.github.repos` - all discovered GitHub repositories matching your space

Common jobs (build-release, kf-cli-execution, etc.) are preserved from the template.

**Note:** Jenkins credentials are required to be passed as runtime properties (see Running section below).

#### Running

powershell
```bash
.\run.bat
```

cmd
```bash
run.bat
```

Enter your credentials (optionally the preferred port)

<img width="792" height="398" alt="image" src="https://github.com/user-attachments/assets/e29be1db-cc83-4a56-be52-c352f205bd86" />

#### Registering the MCP Server with Copilot

Add the following to your `mcp.json` configuration file:

```json
"jenkins": {
    "type": "http",
    "url": "http://localhost:8080/mcp"
}
```
**NOTE: If you are using the specialized agent (discussed above), make sure the name of the mcp-server in the above json is "jenkins" 

Replace `8080` with your configured server port if different.

## Capabilities
- Supports single operation like `get last successful build for this application and branch <branch>`
- Supports sequential chain of operations `get last successful build for this application and branch <branch> and deploy it on dev env west kf platform`
- Supports parallel unrelated operations `get last successful build for this application and branch <branch>, the last successful deployment, last successful veracode scan`
- Supports implied chain of operations `deploy latest <application> of <branch> on dev cntv` : This will perform a sequential chain of operations

## Out of Scope
- Fetching the status of each pipeline stage in the build process
- Fetching configs for pipeline
- Fetching console logs for the build

Why? Jenkins only offers REST APIs for accessing build statuses and triggering builds and not to provide insights into builds
