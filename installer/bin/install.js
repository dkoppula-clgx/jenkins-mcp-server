#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const https = require('https');
const os = require('os');

console.log('🚀 Starting Jenkins MCP agent installation...\n');

const AGENT_URL = 'https://raw.githubusercontent.com/dkoppula-clgx/jenkins-mcp-server/main/.github/agents/jenkins-actions.agent.md';

// Target the .copilot directory in user's home directory
const COPILOT_DIR = path.join(os.homedir(), '.copilot');
const TARGET_DIR = path.join(COPILOT_DIR, 'agents');
const TARGET_FILE = path.join(TARGET_DIR, 'jenkins-actions.agent.md');

async function install() {
  console.log('📦 Installing Jenkins MCP agent...\n');

  try {
    // Validate/create .copilot/agents directory
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
  }
}

install();

