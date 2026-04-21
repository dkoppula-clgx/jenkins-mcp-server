#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Install Jenkins MCP agent to workspace or global location
.DESCRIPTION
    Copies the Jenkins MCP agent from the workspace .github/agents folder
    to either the local project or global Copilot agents directory.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

# Constants
$SOURCE_FILE = Join-Path $PSScriptRoot ".github\agents\jenkins-actions.agent.md"
$AGENT_FILENAME = "jenkins-actions.agent.md"
$MCP_JSON_PATH = Join-Path $env:USERPROFILE "AppData\Roaming\Code\User\mcp.json"

#region Helper Functions

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════════" -ForegroundColor DarkGray
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════════════════" -ForegroundColor DarkGray
    Write-Host ""
}

function Write-Step {
    param(
        [string]$Message,
        [int]$Step,
        [int]$Total
    )
    Write-Host ""
    Write-Host "[$Step/$Total] " -NoNewline -ForegroundColor Yellow
    Write-Host $Message -ForegroundColor White
}

function Write-Success {
    param([string]$Message)
    Write-Host "   ✓ " -NoNewline -ForegroundColor Green
    Write-Host $Message -ForegroundColor White
}

function Write-Info {
    param([string]$Message)
    Write-Host "   ℹ " -NoNewline -ForegroundColor Cyan
    Write-Host $Message -ForegroundColor Gray
}

function Write-Warning {
    param([string]$Message)
    Write-Host "   ⚠ " -NoNewline -ForegroundColor Yellow
    Write-Host $Message -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "   ✗ " -NoNewline -ForegroundColor Red
    Write-Host $Message -ForegroundColor Red
}

function Configure-McpServer {
    param(
        [ref]$McpActionRef
    )
    
    Write-Header "MCP Server Configuration"
    
    # Prompt for port
    Write-Host ""
    $port = Read-Host "   Enter server port (default: 8080)"
    if ([string]::IsNullOrWhiteSpace($port)) {
        $port = "8080"
    }
    
    # Validate port is numeric
    if (-not ($port -match '^\d+$')) {
        throw "Invalid port number. Please enter a numeric value."
    }
    
    # Read existing mcp.json or create new structure
    $mcpConfig = $null
    
    if (Test-Path $MCP_JSON_PATH) {
        Write-Info "Found existing mcp.json"
        try {
            $mcpContent = Get-Content -Path $MCP_JSON_PATH -Raw -ErrorAction Stop
            $mcpConfig = $mcpContent | ConvertFrom-Json -ErrorAction Stop
        }
        catch {
            throw "Failed to parse existing mcp.json: $($_.Exception.Message)"
        }
    }
    else {
        Write-Info "Creating new mcp.json structure"
        $mcpConfig = [PSCustomObject]@{
            servers = [PSCustomObject]@{}
            inputs = @()
        }
    }
    
    # Ensure servers object exists
    if (-not $mcpConfig.servers) {
        $mcpConfig | Add-Member -MemberType NoteProperty -Name "servers" -Value ([PSCustomObject]@{})
    }
    
    # Check if jenkins server already exists
    $jenkinsExists = $mcpConfig.servers.PSObject.Properties.Name -contains "jenkins"
    
    if ($jenkinsExists) {
        $currentUrl = $mcpConfig.servers.jenkins.url
        Write-Host ""
        Write-Warning "Jenkins MCP server already configured"
        Write-Info "Current URL: $currentUrl"
        Write-Host ""
        $update = Read-Host "   Do you want to update it? (y/n)"
        
        if ($update.Trim().ToLower() -ne 'y') {
            Write-Host ""
            Write-Info "MCP server update skipped"
            Write-Host ""
            $McpActionRef.Value = "update-skipped"
            return
        }
        
        # Update existing entry
        $mcpConfig.servers.jenkins.url = "http://localhost:$port/mcp"
        $mcpConfig.servers.jenkins.type = "http"
        $McpActionRef.Value = "updated"
    }
    else {
        # Add new jenkins server entry
        $mcpConfig.servers | Add-Member -MemberType NoteProperty -Name "jenkins" -Value ([PSCustomObject]@{
            url = "http://localhost:$port/mcp"
            type = "http"
        })
        $McpActionRef.Value = "configured"
    }
    
    # Create backup of existing file (silently)
    $backupPath = $null
    if (Test-Path $MCP_JSON_PATH) {
        $backupPath = "$MCP_JSON_PATH.backup"
        Copy-Item -Path $MCP_JSON_PATH -Destination $backupPath -Force -ErrorAction SilentlyContinue
    }
    
    # Write updated config back to file
    try {
        # Ensure directory exists
        $mcpDir = Split-Path $MCP_JSON_PATH -Parent
        if (-not (Test-Path $mcpDir)) {
            New-Item -Path $mcpDir -ItemType Directory -Force | Out-Null
        }
        
        # Convert to JSON with proper formatting
        $jsonOutput = $mcpConfig | ConvertTo-Json -Depth 10
        Set-Content -Path $MCP_JSON_PATH -Value $jsonOutput -Encoding UTF8 -Force -ErrorAction Stop
        
        # Delete backup file after successful write
        if ($backupPath -and (Test-Path $backupPath)) {
            Remove-Item -Path $backupPath -Force -ErrorAction SilentlyContinue
        }
        
        Write-Host ""
        Write-Host "-------------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host "   " -NoNewline -ForegroundColor Green
        Write-Host "MCP Server Configuration Complete!" -ForegroundColor Green
        Write-Host "-------------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host ""
        Write-Host "   📍 Server URL:  " -NoNewline -ForegroundColor Cyan
        Write-Host "http://localhost:$port/mcp" -ForegroundColor White
        Write-Host "   📄 Config File: " -NoNewline -ForegroundColor Cyan
        Write-Host "$MCP_JSON_PATH" -ForegroundColor White
        Write-Host ""
        Write-Host "   GitHub Copilot will automatically detect this configuration." -ForegroundColor Gray
        Write-Host ""
    }
    catch {
        # Restore backup if write failed
        if ($backupPath -and (Test-Path $backupPath)) {
            Copy-Item -Path $backupPath -Destination $MCP_JSON_PATH -Force -ErrorAction SilentlyContinue
            Remove-Item -Path $backupPath -Force -ErrorAction SilentlyContinue
        }
        throw "Failed to write mcp.json: $($_.Exception.Message)"
    }
}

function Configure-PowerShellShortcuts {
    param(
        [ref]$ShortcutsActionRef
    )
    
    Write-Header "PowerShell Shortcuts Configuration"
    
    # Check if profile exists
    $profileExists = Test-Path $PROFILE
    
    if (-not $profileExists) {
        Write-Info "PowerShell profile not found"
        Write-Info "Location: $PROFILE"
    } else {
        Write-Info "PowerShell profile found"
    }
    
    # Check if shortcuts already exist
    $profileContent = ""
    if ($profileExists) {
        $profileContent = Get-Content $PROFILE -Raw -ErrorAction SilentlyContinue
    }
    
    $shortcutsExist = $profileContent -match "jenkins-setup"
    
    if ($shortcutsExist) {
        Write-Host ""
        Write-Warning "PowerShell shortcuts already configured"
        Write-Info "Shortcuts: jenkins-setup, jenkins-install, jenkins-run"
        Write-Host ""
        $ShortcutsActionRef.Value = "already-exists"
        return
    }
    
    # Create the aliases
    $aliases = @"

# Jenkins MCP Server shortcuts (added by install-helpers.ps1)
function jenkins-setup { & "$PSScriptRoot\setup.ps1" }
function jenkins-install { & "$PSScriptRoot\install-helpers.ps1" }
function jenkins-run { & "$PSScriptRoot\run.bat" }
"@
    
    try {
        # Create profile if it doesn't exist
        if (-not $profileExists) {
            $profileDir = Split-Path $PROFILE -Parent
            if (-not (Test-Path $profileDir)) {
                New-Item -Path $profileDir -ItemType Directory -Force | Out-Null
            }
            New-Item -Path $PROFILE -ItemType File -Force | Out-Null
            Write-Info "Created PowerShell profile"
        }
        
        # Append shortcuts to profile
        Add-Content -Path $PROFILE -Value $aliases -Encoding UTF8
        
        Write-Host ""
        Write-Host "-------------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host "   " -NoNewline -ForegroundColor Green
        Write-Host "PowerShell Shortcuts Configured!" -ForegroundColor Green
        Write-Host "-------------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host ""
        Write-Host "   Available commands (from any directory):" -ForegroundColor Cyan
        Write-Host "     - jenkins-setup       Run setup.ps1" -ForegroundColor White
        Write-Host "     - jenkins-install     Run install-helpers.ps1" -ForegroundColor White
        Write-Host "     - jenkins-run         Run server" -ForegroundColor White
        Write-Host ""
        Write-Host "   Note: Restart PowerShell or run: . `$PROFILE" -ForegroundColor Gray
        Write-Host ""
        
        $ShortcutsActionRef.Value = "configured"
    }
    catch {
        throw "Failed to configure PowerShell shortcuts: $($_.Exception.Message)"
    }
}

#endregion

#region Main Script

Write-Host ""
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "          Jenkins MCP Agent Installation & Setup                  " -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host ""

# Validate source file exists
Write-Step "Validating source files" 1 4
if (-not (Test-Path $SOURCE_FILE)) {
    Write-Host ""
    Write-Error "Source agent file not found"
    Write-Info "Expected location: $SOURCE_FILE"
    Write-Info "Please ensure the workspace contains .github/agents/jenkins-actions.agent.md"
    Write-Host ""
    exit 1
}
Write-Success "Source file validated"

# Prompt user for installation location
Write-Step "Choosing installation location" 2 4

Write-Host ""
Write-Host "   📍 Where would you like to install the Jenkins MCP agent?" -ForegroundColor Cyan
Write-Host ""
Write-Host "      1. Workspace (.github/agents) - Local to current project" -ForegroundColor White
Write-Host "      2. Global (~/.copilot/agents) - Available globally" -ForegroundColor White
Write-Host ""

$choice = Read-Host '   Enter your choice (1 or 2)'
$choice = $choice.Trim()

# Validate choice
if ($choice -ne '1' -and $choice -ne '2') {
    Write-Host ''
    Write-Error 'Invalid choice. Please enter 1 or 2.'
    Write-Host ''
    exit 1
}

# Determine target paths based on choice
$targetDir = $null
$targetFile = $null
$location = $null

if ($choice -eq '1') {
    $location = "workspace"
    $targetDir = Join-Path (Get-Location) ".github\agents"
    $targetFile = Join-Path $targetDir $AGENT_FILENAME
    Write-Success "Installing to workspace: .github\agents"
} else {
    $location = "global"
    $copilotDir = Join-Path $env:USERPROFILE ".copilot"
    $targetDir = Join-Path $copilotDir "agents"
    $targetFile = Join-Path $targetDir $AGENT_FILENAME
    Write-Success "Installing globally: ~/.copilot/agents"
}

Write-Step "Installing agent file" 3 4

try {
    # Create target directory if it doesn't exist
    if (-not (Test-Path $targetDir)) {
        Write-Info "Creating target directory"
        try {
            New-Item -Path $targetDir -ItemType Directory -Force -ErrorAction Stop | Out-Null
            Write-Success "Directory created: $targetDir"
        }
        catch {
            throw "Failed to create directory: $($_.Exception.Message)"
        }
    }
    else {
        Write-Info "Target directory exists"
    }

    # Check if agent file already exists
    $agentInstalled = $false
    $agentAction = ""
    
    if (Test-Path $targetFile) {
        Write-Host ""
        Write-Warning "Agent file already exists"
        Write-Info "Location: $targetFile"
        Write-Host ""
        $overwrite = Read-Host "   Do you want to overwrite it? (y/n)"
        
        if ($overwrite.Trim().ToLower() -ne 'y') {
            Write-Host ""
            Write-Info "Agent file installation skipped"
            Write-Host ""
            $agentInstalled = $false
            $agentAction = "skipped"
        }
        else {
            # User wants to overwrite - proceed with copy
            $agentInstalled = $true
            $agentAction = "updated"
        }
    }
    else {
        # File doesn't exist - proceed with copy
        $agentInstalled = $true
        $agentAction = "installed"
    }

    # Copy agent file if needed
    if ($agentInstalled) {
        Write-Info "Copying agent file..."
        try {
            Copy-Item -Path $SOURCE_FILE -Destination $targetFile -Force -ErrorAction Stop
        }
        catch {
            throw "Failed to copy agent file: $($_.Exception.Message)"
        }

        # Verify file was copied
        if (-not (Test-Path $targetFile)) {
            throw "Agent file was not copied successfully"
        }

        Write-Success "Agent file installed successfully"
        
        # Success message
        Write-Host ""
        Write-Host "-------------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host "   " -NoNewline -ForegroundColor Green
        Write-Host "Agent Installation Complete!" -ForegroundColor Green
        Write-Host "-------------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host ""
        Write-Host "   📍 Location: " -NoNewline -ForegroundColor Cyan
        Write-Host "$targetFile" -ForegroundColor White
        Write-Host ""
    }
    
    Write-Step "Optional MCP server configuration" 4 4
    
    # Ask if user wants to configure MCP server
    Write-Host ""
    $mcpAction = ""
    $configureMcp = Read-Host "   Would you like to configure the Jenkins MCP server in mcp.json? (y/n)"
    
    if ($configureMcp.Trim().ToLower() -eq 'y') {
        try {
            Configure-McpServer -McpActionRef ([ref]$mcpAction)
        }
        catch {
            Write-Host ""
            Write-Warning "MCP configuration failed"
            Write-Info "Error: $_"
            Write-Info "You can manually configure it later in mcp.json"
            Write-Host ""
            $mcpAction = "failed"
        }
    }
    else {
        Write-Host ""
        Write-Info "MCP configuration skipped"
        Write-Info "GitHub Copilot will automatically detect the agent"
        Write-Host ""
        $mcpAction = "skipped"
    }
    
    # Ask if user wants to configure PowerShell shortcuts
    Write-Host ""
    $shortcutsAction = ""
    $configureShortcuts = Read-Host "   Would you like to add PowerShell shortcuts? (jenkins-setup, jenkins-install, jenkins-run) (y/n)"
    
    if ($configureShortcuts.Trim().ToLower() -eq 'y') {
        try {
            Configure-PowerShellShortcuts -ShortcutsActionRef ([ref]$shortcutsAction)
        }
        catch {
            Write-Host ""
            Write-Warning "PowerShell shortcuts configuration failed"
            Write-Info "Error: $_"
            Write-Info "You can manually add them to your PowerShell profile later"
            Write-Host ""
            $shortcutsAction = "failed"
        }
    }
    else {
        Write-Host ""
        Write-Info "PowerShell shortcuts skipped"
        Write-Host ""
        $shortcutsAction = "skipped"
    }
    
    # Display summary checklist
    Write-Host ""
    Write-Host "===================================================================" -ForegroundColor DarkGray
    Write-Host "  Setup Summary" -ForegroundColor Cyan
    Write-Host "===================================================================" -ForegroundColor DarkGray
    Write-Host ""
    
    # Agent status
    switch ($agentAction) {
        "installed" {
            Write-Host "   ✓ " -NoNewline -ForegroundColor Green
            Write-Host "Agent installed successfully" -ForegroundColor White
        }
        "updated" {
            Write-Host "   ✓ " -NoNewline -ForegroundColor Green
            Write-Host "Agent updated successfully" -ForegroundColor White
        }
        "skipped" {
            Write-Host "   ○ " -NoNewline -ForegroundColor Gray
            Write-Host "Agent installation skipped (already exists)" -ForegroundColor Gray
        }
    }
    
    # MCP status
    switch ($mcpAction) {
        "configured" {
            Write-Host "   ✓ " -NoNewline -ForegroundColor Green
            Write-Host "MCP server configured successfully" -ForegroundColor White
        }
        "updated" {
            Write-Host "   ✓ " -NoNewline -ForegroundColor Green
            Write-Host "MCP server updated successfully" -ForegroundColor White
        }
        "update-skipped" {
            Write-Host "   ○ " -NoNewline -ForegroundColor Gray
            Write-Host "MCP server update skipped (already configured)" -ForegroundColor Gray
        }
        "skipped" {
            Write-Host "   ○ " -NoNewline -ForegroundColor Gray
            Write-Host "MCP server configuration skipped" -ForegroundColor Gray
        }
        "failed" {
            Write-Host "   ✗ " -NoNewline -ForegroundColor Red
            Write-Host "MCP server configuration failed" -ForegroundColor Red
        }
    }
    
    # PowerShell shortcuts status
    switch ($shortcutsAction) {
        "configured" {
            Write-Host "   ✓ " -NoNewline -ForegroundColor Green
            Write-Host "PowerShell shortcuts configured successfully" -ForegroundColor White
        }
        "already-exists" {
            Write-Host "   ○ " -NoNewline -ForegroundColor Gray
            Write-Host "PowerShell shortcuts already exist" -ForegroundColor Gray
        }
        "skipped" {
            Write-Host "   ○ " -NoNewline -ForegroundColor Gray
            Write-Host "PowerShell shortcuts configuration skipped" -ForegroundColor Gray
        }
        "failed" {
            Write-Host "   ✗ " -NoNewline -ForegroundColor Red
            Write-Host "PowerShell shortcuts configuration failed" -ForegroundColor Red
        }
    }
    
    Write-Host ""

    Write-Host "===================================================================" -ForegroundColor Green
    Write-Host "                     Setup Completed!                              " -ForegroundColor Green
    Write-Host "===================================================================" -ForegroundColor Green
    Write-Host ""

    exit 0
}
catch {
    Write-Host ""
    Write-Host "===================================================================" -ForegroundColor Red
    Write-Host "                       Setup Failed!                               " -ForegroundColor Red
    Write-Host "===================================================================" -ForegroundColor Red
    Write-Host ""
    Write-Error "$_"
    Write-Host ""
    exit 1
}

#endregion
