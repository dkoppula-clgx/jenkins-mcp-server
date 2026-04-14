<img width="1462" height="477" alt="image" src="https://github.com/user-attachments/assets/4533fde6-e414-44b1-8d50-36239ab4ecc5" /># Jenkins MCP Server

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

## Specialized Agent

A **specialized jenkins-actions** agent is created to ensure seamless usage of this MCP server.
This can be installed easily by just running a script.
It supports installation both globally and in current workspace
Follow these steps:
1. If you want to install in a single project, navigate to the project's directory
2. Then run this script
```bash
npx github:dkoppula-clgx/jenkins-mcp-server
```
3. Select between global and local installations
<img width="1462" height="477" alt="image" src="https://github.com/user-attachments/assets/e439ac79-8f64-43da-8de4-83f4616be07a" />
4. If the agent is already present in the directory, it is replaced


## Server Setup

Simple! Fork the project and run the application.

**Jenkins credentials are required to be passed as VM arguments when starting the application.

## Running

### Option 1: Using Command Line

```bash
run.bat
```
Enter your credentials (optionally the preferred port)
<img width="812" height="358" alt="image" src="https://github.com/user-attachments/assets/c43c3340-19ab-4097-be26-70996fbc28e0" />


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

## Out of Scope
Fetching the status of each pipeline stage in the build process is currently out of scope for this implementation. The server provides the Jenkins job URL for monitoring build progress, but does not parse or return individual pipeline stage statuses.

## Capabilities
- Supports single operation like `get last successful build for this application and branch <branch>`
- Supports sequential chain of operations `get last successful build for this application and branch <branch> and deploy it on dev env kf platform`
- Supports parallel unrelated operations `get last successful build for this application and branch <branch>, the last successful deployment, last successful veracode scan`
