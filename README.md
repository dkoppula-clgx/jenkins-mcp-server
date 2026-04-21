## Description
A **custom Jenkins MCP (Model Context Protocol) server** tailored to individual users' credentials and project configurations. This server provides AI assistants with tools to interact with Jenkins for build monitoring and deployment operations.

## Features

- Retrieve all build details for Jenkins jobs by parent and child job
- Retrieve build version details for a specific build number
- List all branch-specific Jenkins jobs (our applications)
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

3. **getAllBranchSpecificJobs**  
   Fetches a list of all branch-specific Jenkins jobs available in the system. Returns individual application jobs. **IMPORTANT: This tool MUST be called BEFORE performing any branch-specific job-related actions** to discover available branch-specific jobs and their exact names.

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

### Getting Started

1. **Clone or fork this repository** to your own workspace/GitHub account
2. Navigate to the cloned directory:
   ```powershell
   cd jenkins-mcp-server
   ```

### Prerequisites

- **Windows** operating system
- **Java 21+** installed
- **PowerShell** (pre-installed on Windows)

**NOTE:** It is advised to copy-paste the commands to facilitate seamless setup and integration without errors.

### Specialized Agent Installation

A **specialized jenkins-actions agent** ensures seamless usage of this MCP server with GitHub Copilot.

**Run the installation script:**
```powershell
.\install-helpers.ps1
```

**The script will:**
1. **Prompt for installation location:**
   - Option 1: Workspace (`.github/agents`) - Local to current project
   - Option 2: Global (`~/.copilot/agents`) - Available across all projects

2. **Copy the agent file** to your chosen location
   - If the agent already exists, you'll be prompted to overwrite or skip

3. **Optionally configure MCP server** in your `mcp.json`:
   - Automatically adds/updates the Jenkins MCP server configuration
   - Prompts for server port (default: 8080)
   - Safely merges with existing MCP servers (preserves other servers like Atlassian, GitHub)

4. **Optionally add PowerShell shortcuts** to your profile:
   - Adds convenient commands: `jenkins-setup`, `jenkins-install`, `jenkins-run`
   - Run scripts from any directory without navigating to project folder
   - Checks for existing shortcuts to prevent duplicates

5. **Display setup summary** showing what was configured or skipped

<img width="1462" height="477" alt="image" src="https://github.com/user-attachments/assets/e439ac79-8f64-43da-8de4-83f4616be07a" />


### Server Setup

#### Initial Configuration

**Run the setup script** to auto-discover your Jenkins configuration:
```powershell
.\setup.ps1
```

**The script will:**

1. **Collect your Jenkins credentials:**
   - Business Unit (e.g., `credit-us`)
   - Project Space (e.g., `pbs`, `dhqcare`)  
   - Jenkins username
   - Jenkins password (securely masked)

   <img width="830" height="327" alt="image" src="https://github.com/user-attachments/assets/8b20fcfb-0e29-470d-b2b0-b242663cc255" />

2. **Auto-discover your Jenkins jobs:**
   - Fetches all WorkflowMultiBranchProject jobs from your Jenkins space
   - Discovers GitHub repositories from the build-release job (if available)

3. **Generate configuration file:**
   - Creates `src/main/resources/application.yml` with your discovered settings
   - Populates business unit, project space, jobs, and repositories
   - Preserves common jobs (build-release, kf-cli-execution, etc.) from template

**Note:** A template configuration is available at `src/main/resources/template.yml` for reference.

#### Running the Server

Run the server using:

```powershell
.\run.bat
```

Enter your Jenkins credentials when prompted (and optionally the preferred port):

<img width="792" height="398" alt="image" src="https://github.com/user-attachments/assets/e29be1db-cc83-4a56-be52-c352f205bd86" />

#### PowerShell Shortcuts (Optional)

If you configured PowerShell shortcuts during `install-helpers.ps1`, you can run scripts from any directory:

```powershell
# Run setup from anywhere
jenkins-setup

# Install agent from anywhere  
jenkins-install

# Run server from anywhere
jenkins-run
```

**Restart your PowerShell terminal** after installation to activate the shortcuts.

**To manually add shortcuts to your profile:**
```powershell
notepad $PROFILE
```

Add these functions:
```powershell
function jenkins-setup { Set-Location "C:\Users\YourUser\Projects\jenkins-mcp-server"; .\setup.ps1 }
function jenkins-install { Set-Location "C:\Users\YourUser\Projects\jenkins-mcp-server"; .\install-helpers.ps1 }
function jenkins-run { Set-Location "C:\Users\YourUser\Projects\jenkins-mcp-server"; .\run.bat }
```

#### MCP Server Configuration

If you didn't use `install-helpers.ps1` to configure your `mcp.json`, you can manually add the Jenkins MCP server configuration in VsCode:

Add the following to your `mcp.json` configuration file:

```json
"jenkins": {
    "type": "http",
    "url": "http://localhost:8080/mcp"
}
```

**NOTE:** If you are using the specialized agent (discussed above), make sure the name of the mcp-server in the above json is `"jenkins"`. 

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
