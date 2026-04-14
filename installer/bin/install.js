#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const https = require('https');
const os = require('os');
const readline = require('readline');

const AGENT_URL = 'https://raw.githubusercontent.com/dkoppula-clgx/jenkins-mcp-server/main/.github/agents/jenkins-actions.agent.md';

console.log('🚀 Starting Jenkins MCP agent installation...\n');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function promptUser() {
  console.log('\n📍 Where would you like to install the Jenkins MCP agent?\n');
  console.log('  1. Workspace (.github/agents) - Local to current project');
  console.log('  2. Global (~/.copilot/agents) - Available globally\n');
  
  rl.question('Enter your choice (1 or 2): ', (answer) => {
    const choice = answer.trim();
    
    if (choice === '1') {
      install('workspace');
    } else if (choice === '2') {
      install('global');
    } else {
      console.error('❌ Invalid choice. Please enter 1 or 2.');
      rl.close();
      process.exit(1);
    }
  });
}

async function install(location) {
  let TARGET_DIR, TARGET_FILE;
  
  if (location === 'workspace') {
    TARGET_DIR = path.join(process.cwd(), '.github', 'agents');
    TARGET_FILE = path.join(TARGET_DIR, 'jenkins-actions.agent.md');
  } else {
    const COPILOT_DIR = path.join(os.homedir(), '.copilot');
    TARGET_DIR = path.join(COPILOT_DIR, 'agents');
    TARGET_FILE = path.join(TARGET_DIR, 'jenkins-actions.agent.md');
  }
  
  console.log('\n📦 Installing Jenkins MCP agent...\n');

  try {
    // Validate/create directory
    if (!fs.existsSync(TARGET_DIR)) {
      console.log(`Creating directory: ${TARGET_DIR}`);
      fs.mkdirSync(TARGET_DIR, { recursive: true });
    }

    // Download agent file
    console.log('Downloading agent file...');
    
    await new Promise((resolve, reject) => {
      https.get(AGENT_URL, (response) => {
        if (response.statusCode !== 200) {
          reject(new Error(`Failed to download: ${response.statusCode}`));
          return;
        }

        const fileStream = fs.createWriteStream(TARGET_FILE);
        response.pipe(fileStream);

        fileStream.on('finish', () => {
          fileStream.close();
          resolve();
        });

        fileStream.on('error', (err) => {
          fs.unlinkSync(TARGET_FILE);
          reject(err);
        });
      }).on('error', reject);
    });

    console.log('\n✅ Jenkins MCP agent installed successfully!');
    console.log(`📍 Location: ${TARGET_FILE}`);
    console.log('\nGitHub Copilot will automatically detect this agent.\n');

  } catch (error) {
    console.error('❌ Installation failed:', error.message);
    process.exit(1);
  } finally {
    rl.close();
  }
}

promptUser();

