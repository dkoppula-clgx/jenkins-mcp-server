# GitHub Copilot Instructions

## Project Overview

This is a **custom Jenkins MCP (Model Context Protocol) server** tailored to individual users' credentials and project configurations. It exposes Jenkins CI/CD capabilities as MCP tools consumable by AI assistants such as GitHub Copilot.

## Purpose

Provides a set of MCP tools to:

- Retrieve build details and statuses for Jenkins jobs
- Trigger and manage application builds
- Deploy applications to Kubernetes clusters (supporting both **KF** and **CNTV** platforms)
- Manage Kubernetes instances across KF and CNTV environments
- Run Veracode security scans
- Manage KF self-service operations (start, stop, restart)

> **Note:** This server is custom-built to cater to each user's specific project and credentials — it is not a generic Jenkins MCP server.

## Technology Stack

| Aspect       | Details                  |
|--------------|--------------------------|
| Language     | Java 21                  |
| Framework    | Spring AI (MCP Server)   |
| Build Tool   | Gradle                   |
| HTTP Client  | Spring RestClient        |

## Design Patterns

- **Strategy Pattern**: `JenkinsApiClient` (interface) with `JenkinsApiRestClient` as the concrete implementation, decoupling the service layer from the underlying HTTP client and enabling easy addition of alternative client implementations.

