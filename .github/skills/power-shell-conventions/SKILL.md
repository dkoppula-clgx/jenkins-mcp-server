---
name: power-shell-conventions
description: Use this for writing PowerShell scripts adhering to user-defined PowerShell conventions.
---


When creating or modifying `.ps1` scripts in this project, follow these critical practices to ensure compatibility with Windows PowerShell 5.1:

### File Encoding

**CRITICAL:** PowerShell scripts **MUST** be saved with **UTF-8 BOM** (Byte Order Mark) encoding.

- **Why:** Windows PowerShell 5.1 (pre-installed on Windows) requires BOM to properly parse UTF-8 files. Without BOM, scripts with Unicode characters will fail with "Unexpected token" errors when run in standalone PowerShell terminals (even if they work in VS Code integrated terminal).
- **How to fix:**
  ```powershell
  # Re-save with UTF-8 BOM
  $content = Get-Content -Path "script.ps1" -Raw
  [System.IO.File]::WriteAllText("script.ps1", $content, (New-Object System.Text.UTF8Encoding $true))
  ```
- **VS Code settings:** Ensure file encoding shows "UTF-8 with BOM" in the status bar (click encoding to change).

### Unicode Characters

**Be cautious with Unicode box-drawing and emoji characters:**

- ✓ **Safe:** Basic Unicode like `✓ ✗ ○ • →`
- ⚠️ **Problematic:** Box-drawing characters like `═ ║ ╔ ╗ ╚ ╝ ─ │` (can cause parse errors in Windows PowerShell)
- **Recommendation:** Test scripts in standalone PowerShell terminal (not just VS Code integrated terminal) before committing
- **Validation:** Use `[System.Management.Automation.PSParser]::Tokenize()` to check for syntax errors:
  ```powershell
  $errors = $null
  $null = [System.Management.Automation.PSParser]::Tokenize((Get-Content -Path "script.ps1" -Raw), [ref]$errors)
  if ($errors) { $errors | Format-Table -AutoSize }
  ```

### Duplicate Prevention Pattern

When modifying user configuration files (like PowerShell profile), always check for existing entries:

```powershell
# Example from Configure-PowerShellShortcuts
$profileContent = Get-Content $PROFILE -Raw -ErrorAction SilentlyContinue
$shortcutsExist = $profileContent -match "jenkins-setup"

if ($shortcutsExist) {
    Write-Warning "Already configured"
    return
}
```

This prevents duplicate entries when scripts are run multiple times.