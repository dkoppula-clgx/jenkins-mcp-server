# Jenkins MCP Server

MCP (Model Context Protocol) server for Jenkins integration, providing AI assistants with tools to interact with Jenkins for build monitoring and deployment operations.

## Features

- Get latest build details for Jenkins jobs
- Get all build information for a job/branch
- List all available Jenkins jobs
- List all GitHub repositories
- Deploy applications to Jenkins

## MCP Tools

This server exposes 5 MCP tools:

1. **getLatestBuildDetailsByJobAndBranch** - Fetch latest build details
2. **getBuildDetailsByJobAndBranch** - Fetch all build information
3. **getAllJobs** - List all configured Jenkins jobs
4. **getAllRepos** - List all GitHub repositories
5. **deployApplication** - Deploy an application via Jenkins

## Setup

Configure your Jenkins instance details in `src/main/resources/application.yml`:

```yaml
jenkins:
  base-url: https://your-jenkins-instance.com
  username: ${USERNAME}
  password: ${PASSWORD}
```

## Running

```bash
./gradlew bootRun
```

## Documentation

- [Jenkins Crumb Issue Resolution](JENKINS_CRUMB_ISSUE_RESOLUTION.md)
- [Deploy API Documentation](DEPLOY_API_DOCUMENTATION.md)

