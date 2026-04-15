#!/usr/bin/env node

const https = require('https');
const fs = require('fs');
const path = require('path');

// Parse command line arguments
const [businessUnit, space, username, password] = process.argv.slice(2);

if (!businessUnit || !space || !username || !password) {
    console.error('[ERROR] Missing required arguments');
    console.error('Usage: node scaffold-config.js <businessUnit> <space> <username> <password>');
    process.exit(1);
}

const BASE_URL = 'https://jenkins-cicd.solutions.corelogic.com';
const JENKINS_API_URL = `${BASE_URL}/${businessUnit}/job/${space}/api/json`;

/**
 * Make an authenticated HTTPS request to Jenkins API
 */
function fetchJenkinsJobs() {
    return new Promise((resolve, reject) => {
        const auth = Buffer.from(`${username}:${password}`).toString('base64');
        
        const options = {
            headers: {
                'Authorization': `Basic ${auth}`,
                'Accept': 'application/json'
            }
        };

        console.log(`Fetching jobs from: ${JENKINS_API_URL}`);
        
        https.get(JENKINS_API_URL, options, (res) => {
            let data = '';

            if (res.statusCode === 401) {
                reject(new Error('Authentication failed. Please check your username and password and retry.'));
                return;
            }

            if (res.statusCode !== 200) {
                reject(new Error(`Jenkins API returned status ${res.statusCode}. Please verify the Business Unit and Project Space are correct.`));
                return;
            }

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                try {
                    const jsonData = JSON.parse(data);
                    resolve(jsonData);
                } catch (error) {
                    reject(new Error(`Failed to parse Jenkins API response: ${error.message}`));
                }
            });
        }).on('error', (error) => {
            reject(new Error(`Network error while connecting to Jenkins: ${error.message}`));
        });
    });
}

/**
 * Extract job names from Jenkins response
 */
function extractJobNames(jenkinsData) {
    if (!jenkinsData.jobs || !Array.isArray(jenkinsData.jobs)) {
        throw new Error('Invalid Jenkins response: missing or invalid "jobs" array');
    }

    const workflowJobs = jenkinsData.jobs.filter(job => 
        job._class === 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject'
    );

    if (workflowJobs.length === 0) {
        console.warn('[WARNING] No WorkflowMultiBranchProject jobs found in this space');
        return [];
    }

    const jobNames = workflowJobs.map(job => {
        const url = job.url;
        // Extract last segment from URL (job name)
        const segments = url.split('/').filter(s => s.length > 0);
        return segments[segments.length - 1];
    });

    console.log(`\nDiscovered ${jobNames.length} branch-specific job(s):`);
    jobNames.forEach(name => console.log(`  - ${name}`));
    console.log();

    return jobNames;
}

/**
 * Check if build-release job exists and fetch GitHub repo configuration
 */
function fetchGithubRepos(businessUnit, space) {
    return new Promise((resolve, reject) => {
        const buildReleaseUrl = `${BASE_URL}/${businessUnit}/job/${space}/job/build-release/job/build-release/api/json`;
        const auth = Buffer.from(`${username}:${password}`).toString('base64');
        
        const options = {
            headers: {
                'Authorization': `Basic ${auth}`,
                'Accept': 'application/json'
            }
        };

        console.log(`Checking for build-release job...`);
        
        https.get(buildReleaseUrl, options, (res) => {
            let data = '';

            if (res.statusCode === 404) {
                console.warn('[WARNING] build-release job not found. Skipping GitHub repo discovery.');
                resolve([]);
                return;
            }

            if (res.statusCode !== 200) {
                console.warn(`[WARNING] build-release job returned status ${res.statusCode}. Skipping GitHub repo discovery.`);
                resolve([]);
                return;
            }

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                try {
                    const jsonData = JSON.parse(data);
                    const repos = extractGithubRepoChoices(jsonData, businessUnit, space);
                    resolve(repos);
                } catch (error) {
                    console.warn(`[WARNING] Failed to parse build-release response: ${error.message}`);
                    resolve([]);
                }
            });
        }).on('error', (error) => {
            console.warn(`[WARNING] Network error while fetching build-release: ${error.message}`);
            resolve([]);
        });
    });
}

/**
 * Extract GitHub repository choices from build-release job
 */
function extractGithubRepoChoices(buildReleaseData, businessUnit, space) {
    if (!buildReleaseData.property || !Array.isArray(buildReleaseData.property)) {
        return [];
    }

    // Find the ParametersDefinitionProperty
    const paramsProp = buildReleaseData.property.find(prop => 
        prop._class === 'hudson.model.ParametersDefinitionProperty'
    );

    if (!paramsProp || !paramsProp.parameterDefinitions) {
        return [];
    }

    // Find the GITHUB_REPO_NAME parameter
    const githubRepoParam = paramsProp.parameterDefinitions.find(param =>
        param.name === 'GITHUB_REPO_NAME' && param._class === 'hudson.model.ChoiceParameterDefinition'
    );

    if (!githubRepoParam || !githubRepoParam.choices) {
        return [];
    }

    // Filter choices that match the pattern: {businessUnit}-{space}-*
    // Note: GitHub repos use underscores in business unit (e.g., credit_us instead of credit-us)
    const businessUnitWithUnderscore = businessUnit.replace(/-/g, '_');
    const repoPrefix = `${businessUnitWithUnderscore}-${space}-`;
    const filteredRepos = githubRepoParam.choices.filter(choice => 
        choice.startsWith(repoPrefix)
    );

    if (filteredRepos.length > 0) {
        console.log(`\nDiscovered ${filteredRepos.length} GitHub repository/repositories:`);
        filteredRepos.forEach(repo => console.log(`  - ${repo}`));
        console.log();
    } else {
        console.log('\nNo matching GitHub repositories found.');
    }

    return filteredRepos;
}

/**
 * Generate application-test.yml content
 */
function generateYamlContent(businessUnit, space, branchSpecificJobs, githubRepos) {
    // Format branch jobs as YAML list
    const jobsList = branchSpecificJobs.length > 0 
        ? branchSpecificJobs.map(job => `    - ${job}`).join('\n')
        : '    # No jobs discovered';

    // Format GitHub repos as YAML list
    const reposList = githubRepos.length > 0
        ? githubRepos.map(repo => `        - ${repo}`).join('\n')
        : '        # No repositories discovered';

    return `spring:
  application:
    name: jenkins-mcp-server
  ai:
    mcp:
      server:
        protocol: streamable

jenkins:
  base-url: ${BASE_URL}
  api-paths:
    business-unit-job: ${businessUnit}
    project-space-job: ${space}
  branch-specific-jobs:
${jobsList}
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
${reposList}`;
}

/**
 * Write the generated YAML to application.yml
 */
function writeConfigFile(content) {
    const outputPath = path.join(__dirname, '..', 'src', 'main', 'resources', 'application.yml');
    
    try {
        fs.writeFileSync(outputPath, content, 'utf8');
        console.log(`Configuration written to: ${outputPath}`);
        return true;
    } catch (error) {
        throw new Error(`Failed to write configuration file: ${error.message}`);
    }
}

/**
 * Main execution
 */
async function main() {
    try {
        // Step 1: Fetch jobs from Jenkins
        const jenkinsData = await fetchJenkinsJobs();
        
        // Step 2: Extract job names
        const branchSpecificJobs = extractJobNames(jenkinsData);
        
        // Step 3: Fetch GitHub repositories from build-release job
        const githubRepos = await fetchGithubRepos(businessUnit, space);
        
        // Step 4: Generate YAML content
        const yamlContent = generateYamlContent(businessUnit, space, branchSpecificJobs, githubRepos);
        
        // Step 5: Write to file
        writeConfigFile(yamlContent);
        
        console.log('[SUCCESS] Setup completed successfully!');
        process.exit(0);
        
    } catch (error) {
        console.error(`\n[ERROR] ${error.message}`);
        console.error('\nPlease verify your inputs and try again.');
        process.exit(1);
    }
}

// Run the script
main();
