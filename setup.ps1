#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Jenkins MCP Server Configuration Setup
.DESCRIPTION
    This script scaffolds your application.yml configuration by discovering jobs from your Jenkins space.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

# Constants
$BASE_URL = 'https://jenkins-cicd.solutions.corelogic.com'

# Display header
Write-Host ""
Write-Host "Jenkins MCP Server Configuration Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will scaffold your application.yml configuration"
Write-Host "by discovering jobs from your Jenkins space."
Write-Host ""

#region Input Collection

# Collect Business Unit
$businessUnit = Read-Host "Enter Business Unit (e.g., credit-us)"
if ([string]::IsNullOrWhiteSpace($businessUnit)) {
    Write-Host "[ERROR] Business Unit cannot be empty" -ForegroundColor Red
    exit 1
}

# Collect Project Space
$space = Read-Host "Enter Project Space (e.g., pbs, dhqcare)"
if ([string]::IsNullOrWhiteSpace($space)) {
    Write-Host "[ERROR] Project Space cannot be empty" -ForegroundColor Red
    exit 1
}

# Collect Username
$username = Read-Host "Enter Jenkins username"
if ([string]::IsNullOrWhiteSpace($username)) {
    Write-Host "[ERROR] Username cannot be empty" -ForegroundColor Red
    exit 1
}

# Collect Password (secure)
$securePassword = Read-Host "Enter Jenkins password" -AsSecureString
$password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
)
if ([string]::IsNullOrWhiteSpace($password)) {
    Write-Host "[ERROR] Password cannot be empty" -ForegroundColor Red
    exit 1
}

# Display configuration
Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Business Unit: $businessUnit"
Write-Host "  Project Space: $space"
Write-Host "  Username: $username"
Write-Host ""
Write-Host "Discovering jobs from Jenkins..." -ForegroundColor Yellow
Write-Host ""

#endregion

#region Function Definitions

function Invoke-JenkinsApi {
    param(
        [Parameter(Mandatory)]
        [string]$Url,
        
        [Parameter(Mandatory)]
        [string]$Username,
        
        [Parameter(Mandatory)]
        [string]$Password,
        
        [switch]$AllowNotFound
    )
    
    try {
        $base64Auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${Username}:${Password}"))
        $headers = @{
            'Authorization' = "Basic $base64Auth"
            'Accept' = 'application/json'
        }
        
        $response = Invoke-RestMethod -Uri $Url -Headers $headers -Method Get -ErrorAction Stop
        return $response
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 401) {
            throw "Authentication failed. Please check your username and password and retry."
        }
        elseif ($statusCode -eq 404) {
            if ($AllowNotFound) {
                return $null
            }
            throw "Jenkins API returned status 404. Please verify the Business Unit and Project Space are correct."
        }
        elseif ($statusCode) {
            throw "Jenkins API returned status $statusCode. Please verify the Business Unit and Project Space are correct."
        }
        else {
            throw "Network error while connecting to Jenkins: $($_.Exception.Message)"
        }
    }
}

function Get-BranchSpecificJobs {
    param(
        [Parameter(Mandatory)]
        [string]$BusinessUnit,
        
        [Parameter(Mandatory)]
        [string]$Space,
        
        [Parameter(Mandatory)]
        [string]$Username,
        
        [Parameter(Mandatory)]
        [string]$Password
    )
    
    $jenkinsApiUrl = "$BASE_URL/$BusinessUnit/job/$Space/api/json"
    Write-Host "Fetching jobs from: $jenkinsApiUrl"
    
    try {
        $jenkinsData = Invoke-JenkinsApi -Url $jenkinsApiUrl -Username $Username -Password $Password
        
        if (-not $jenkinsData.jobs) {
            throw "Invalid Jenkins response: missing or invalid 'jobs' array"
        }
        
        # Filter for WorkflowMultiBranchProject jobs
        $workflowJobs = $jenkinsData.jobs | Where-Object {
            $_.'_class' -eq 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject'
        }
        
        if ($workflowJobs.Count -eq 0) {
            Write-Host "[WARNING] No WorkflowMultiBranchProject jobs found in this space" -ForegroundColor Yellow
            return @()
        }
        
        # Extract job names from URLs
        $jobNames = $workflowJobs | ForEach-Object {
            $url = $_.url
            # Extract last segment from URL (job name)
            $segments = $url -split '/' | Where-Object { $_.Length -gt 0 }
            $segments[-1]
        }
        
        Write-Host ""
        Write-Host "Discovered $($jobNames.Count) branch-specific job(s):" -ForegroundColor Green
        $jobNames | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
        
        return $jobNames
    }
    catch {
        throw "Failed to fetch Jenkins jobs: $_"
    }
}

function Get-GithubRepos {
    param(
        [Parameter(Mandatory)]
        [string]$BusinessUnit,
        
        [Parameter(Mandatory)]
        [string]$Space,
        
        [Parameter(Mandatory)]
        [string]$Username,
        
        [Parameter(Mandatory)]
        [string]$Password
    )
    
    $buildReleaseUrl = "$BASE_URL/$BusinessUnit/job/$Space/job/build-release/job/build-release/api/json"
    Write-Host "Checking for build-release job..."
    
    try {
        $buildReleaseData = Invoke-JenkinsApi -Url $buildReleaseUrl -Username $Username -Password $Password -AllowNotFound
        
        if (-not $buildReleaseData) {
            Write-Host "[WARNING] build-release job not found. Skipping GitHub repo discovery." -ForegroundColor Yellow
            return @()
        }
        
        if (-not $buildReleaseData.property) {
            return @()
        }
        
        # Find the ParametersDefinitionProperty
        $paramsProp = $buildReleaseData.property | Where-Object {
            $_.'_class' -eq 'hudson.model.ParametersDefinitionProperty'
        } | Select-Object -First 1
        
        if (-not $paramsProp -or -not $paramsProp.parameterDefinitions) {
            return @()
        }
        
        # Find the GITHUB_REPO_NAME parameter
        $githubRepoParam = $paramsProp.parameterDefinitions | Where-Object {
            $_.name -eq 'GITHUB_REPO_NAME' -and $_.'_class' -eq 'hudson.model.ChoiceParameterDefinition'
        } | Select-Object -First 1
        
        if (-not $githubRepoParam -or -not $githubRepoParam.choices) {
            return @()
        }
        
        # Filter choices that match the pattern
        # Note: GitHub repos use underscores in business unit (e.g., credit_us instead of credit-us)
        $businessUnitWithUnderscore = $BusinessUnit -replace '-', '_'
        $repoPrefix = "${businessUnitWithUnderscore}-${Space}-"
        
        $filteredRepos = $githubRepoParam.choices | Where-Object {
            $_ -like "$repoPrefix*"
        }
        
        if ($filteredRepos.Count -gt 0) {
            Write-Host ""
            Write-Host "Discovered $($filteredRepos.Count) GitHub repository/repositories:" -ForegroundColor Green
            $filteredRepos | ForEach-Object { Write-Host "  - $_" }
            Write-Host ""
        }
        else {
            Write-Host ""
            Write-Host "No matching GitHub repositories found."
            Write-Host ""
        }
        
        return $filteredRepos
    }
    catch {
        Write-Host "[WARNING] Failed to fetch build-release data: $_" -ForegroundColor Yellow
        Write-Host ""
        return ,@()  # Force return as array with comma operator
    }
}

function New-YamlContent {
    param(
        [Parameter(Mandatory)]
        [string]$BusinessUnit,
        
        [Parameter(Mandatory)]
        [string]$Space,
        
        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [array]$BranchSpecificJobs,
        
        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [array]$GithubRepos
    )
    
    # Format branch jobs as YAML list
    if ($BranchSpecificJobs.Count -gt 0) {
        $jobsList = ($BranchSpecificJobs | ForEach-Object { "    - $_" }) -join "`n"
    }
    else {
        $jobsList = "    # No jobs discovered"
    }
    
    # Format GitHub repos as YAML list
    if ($GithubRepos.Count -gt 0) {
        $reposList = ($GithubRepos | ForEach-Object { "        - $_" }) -join "`n"
    }
    else {
        $reposList = "        # No repositories discovered"
    }
    
    $yamlContent = @"
spring:
  application:
    name: jenkins-mcp-server
  ai:
    mcp:
      server:
        protocol: streamable

jenkins:
  base-url: $BASE_URL
  api-paths:
    business-unit-job: $BusinessUnit
    project-space-job: $Space
  branch-specific-jobs:
$jobsList
  common-jobs:
    build-release:
      - name: build-release
        description: to manage application deployments
    self-service:
      - name: kf-cli-execution
        description: to manage k8s instances in kf platform
      - name: cntv-cli-execution
        description: to manage k8s instances in cntv platform
      - name: veracodescan-ondemand
        description: to manage veracode scans

  integration:
    github:
      repos:
$reposList
"@
    
    return $yamlContent
}

#endregion

#region Main Execution

try {
    # Step 1: Fetch branch-specific jobs
    $branchSpecificJobs = Get-BranchSpecificJobs -BusinessUnit $businessUnit -Space $space -Username $username -Password $password
    
    # Step 2: Fetch GitHub repositories
    $githubRepos = Get-GithubRepos -BusinessUnit $businessUnit -Space $space -Username $username -Password $password
    
    # Ensure arrays are never null
    if (-not $branchSpecificJobs) { $branchSpecificJobs = @() }
    if (-not $githubRepos) { $githubRepos = @() }
    
    # Step 3: Generate YAML content
    $yamlContent = New-YamlContent -BusinessUnit $businessUnit -Space $space -BranchSpecificJobs $branchSpecificJobs -GithubRepos $githubRepos
    
    # Step 4: Write to file
    $outputPath = Join-Path $PSScriptRoot "src\main\resources\application.yml"
    
    # Write configuration file with error handling
    try {
        Set-Content -Path $outputPath -Value $yamlContent -Encoding UTF8 -Force -ErrorAction Stop
    }
    catch {
        throw "Failed to write configuration file: $($_.Exception.Message)"
    }
    
    Write-Host "Configuration written to: $outputPath" -ForegroundColor Green
    Write-Host ""
    Write-Host "[SUCCESS] Setup completed successfully!" -ForegroundColor Green
    
    exit 0
}
catch {
    Write-Host ""
    Write-Host "[ERROR] $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please verify your inputs and try again." -ForegroundColor Yellow
    exit 1
}

#endregion
