# ================================
# Windows Repository Security Cleanup Script
# ================================

Write-Host "🔐 Starting repository security cleanup..." -ForegroundColor Green

# Step 1: Remove sensitive files from git history and current tracking
Write-Host "📝 Removing sensitive files from git..." -ForegroundColor Yellow

# Remove files from current index (but keep local copies)
git rm --cached .env 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "No .env in index" -ForegroundColor Gray }

git rm --cached .env.* 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "No .env.* files in index" -ForegroundColor Gray }

git rm --cached Docker/.env 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "No Docker/.env in index" -ForegroundColor Gray }

git rm --cached Docker/.env.* 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "No Docker/.env.* files in index" -ForegroundColor Gray }

git rm --cached Docker/config/application-prod.yml 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "No application-prod.yml in index" -ForegroundColor Gray }

# Step 2: Create .gitignore if it doesn't exist or update it
Write-Host "📄 Creating/updating .gitignore..." -ForegroundColor Yellow

$gitignoreContent = @"
# ================================
# Security & Environment Files
# ================================
.env
.env.*
!.env.template
!.env.example

# Application configuration with secrets
application-prod.yml
application-staging.yml
application-local.yml
**/application-prod.yml
**/application-staging.yml
**/application-local.yml

# Docker environment files
Docker/.env
Docker/.env.*
!Docker/.env.template
!Docker/.env.example

# Database credentials
database.properties
db.properties

# SSL Certificates and Keys
*.pem
*.key
*.crt
*.p12
*.jks
keystore.*
truststore.*
certs/
certificates/

# Passwords and secrets
*password*
*secret*
*token*
credentials.json
auth.json

# ================================
# IDE and Editor Files
# ================================
# IntelliJ IDEA
.idea/
*.iml
*.iws
*.ipr
out/

# Eclipse
.project
.classpath
.settings/
bin/

# Visual Studio Code
.vscode/
*.code-workspace

# Sublime Text
*.sublime-project
*.sublime-workspace

# ================================
# Build and Dependencies
# ================================
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# Gradle
.gradle/
build/
gradle-app.setting
!gradle-wrapper.jar
!gradle-wrapper.properties

# ================================
# Node.js / React Frontend
# ================================
# Dependencies
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*
package-lock.json
yarn.lock

# Production builds
/client/build
/client/dist
/frontend/build
/frontend/dist

# Environment and config
/client/.env
/client/.env.*
/frontend/.env
/frontend/.env.*

# ================================
# Database and Data Files
# ================================
# Database files
*.db
*.sqlite
*.sqlite3
*.mdb

# Data directories
data/
backups/
dumps/
logs/
*.log

# ================================
# Docker and Deployment
# ================================
# Docker volumes and data
Docker/data/
Docker/logs/
Docker/backups/
Docker/certs/

# Kubernetes secrets
k8s-secrets.yml
secrets.yml

# ================================
# Operating System Files
# ================================
# Windows
Thumbs.db
ehthumbs.db
Desktop.ini
`$RECYCLE.BIN/
*.lnk

# macOS
.DS_Store
.AppleDouble
.LSOverride
Icon*
._*

# Linux
*~
.nfs*

# ================================
# Temporary and Cache Files
# ================================
# General
*.tmp
*.temp
temp/
tmp/
cache/

# Java
*.class
*.jar
!gradle-wrapper.jar
!maven-wrapper.jar
hs_err_pid*

# ================================
# Security Scanner Results
# ================================
# Dependency scanning
dependency-check-report.html
.snyk

# SAST results
sonar-project.properties
.sonarqube/

# ================================
# Monitoring and Analytics
# ================================
# Application Insights
ApplicationInsights.json

# New Relic
newrelic.yml
"@

Set-Content -Path ".gitignore" -Value $gitignoreContent -Encoding UTF8

Write-Host "✅ .gitignore created/updated" -ForegroundColor Green

Write-Host ""
Write-Host "🎉 Step 1 completed!" -ForegroundColor Green
Write-Host "Please type 'done' to continue to step 2..." -ForegroundColor Cyan