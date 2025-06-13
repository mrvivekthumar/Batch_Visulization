#!/bin/bash
# Production Deployment Script
# File: Docker/scripts/deploy-prod.sh

set -euo pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
DOCKER_DIR="$PROJECT_ROOT/Docker"

# Default values
ENVIRONMENT="production"
COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.prod"
BACKUP_BEFORE_DEPLOY=true
RUN_HEALTH_CHECKS=true
CLEANUP_OLD_IMAGES=true

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] SUCCESS:${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Production Deployment Script for Database Batch Performance Analyzer

Usage: $0 [OPTIONS]

Options:
    -e, --environment ENV       Environment to deploy (production, staging) [default: production]
    -f, --compose-file FILE     Docker compose file to use [default: docker-compose.prod.yml]
    -c, --config-file FILE      Environment config file [default: .env.prod]
    --no-backup                 Skip database backup before deployment
    --no-health-check          Skip health checks after deployment
    --no-cleanup               Skip cleanup of old Docker images
    --dry-run                   Show what would be done without executing
    -h, --help                  Show this help message

Examples:
    $0                                  # Deploy to production with defaults
    $0 -e staging                       # Deploy to staging environment
    $0 --no-backup --dry-run           # Dry run without backup

EOF
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -f|--compose-file)
                COMPOSE_FILE="$2"
                shift 2
                ;;
            -c|--config-file)
                ENV_FILE="$2"
                shift 2
                ;;
            --no-backup)
                BACKUP_BEFORE_DEPLOY=false
                shift
                ;;
            --no-health-check)
                RUN_HEALTH_CHECKS=false
                shift
                ;;
            --no-cleanup)
                CLEANUP_OLD_IMAGES=false
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
}

# Function to validate prerequisites
validate_prerequisites() {
    log "Validating prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        error "Docker daemon is not running"
        exit 1
    fi
    
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        error "Docker Compose is not installed"
        exit 1
    fi
    
    # Check if we're in the correct directory
    if [[ ! -f "$DOCKER_DIR/$COMPOSE_FILE" ]]; then
        error "Compose file not found: $DOCKER_DIR/$COMPOSE_FILE"
        exit 1
    fi
    
    # Check if environment file exists
    if [[ ! -f "$DOCKER_DIR/$ENV_FILE" ]]; then
        error "Environment file not found: $DOCKER_DIR/$ENV_FILE"
        error "Please copy .env.prod.template to $ENV_FILE and configure it"
        exit 1
    fi
    
    success "Prerequisites validated"
}

# Function to validate environment configuration
validate_environment() {
    log "Validating environment configuration..."
    
    # Source the environment file
    set -a
    source "$DOCKER_DIR/$ENV_FILE"
    set +a
    
    # Check required variables
    local required_vars=(
        "DB_PASSWORD"
        "ADMIN_PASSWORD"
        "VIEWER_PASSWORD"
        "GRAFANA_ADMIN_PASSWORD"
        "REDIS_PASSWORD"
    )
    
    local missing_vars=()
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            missing_vars+=("$var")
        fi
    done
    
    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        error "Missing required environment variables in $ENV_FILE:"
        for var in "${missing_vars[@]}"; do
            error "  - $var"
        done
        exit 1
    fi
    
    # Validate password strength
    if [[ ${#ADMIN_PASSWORD} -lt 12 ]]; then
        error "ADMIN_PASSWORD must be at least 12 characters long"
        exit 1
    fi
    
    if [[ ${#VIEWER_PASSWORD} -lt 12 ]]; then
        error "VIEWER_PASSWORD must be at least 12 characters long"
        exit 1
    fi
    
    success "Environment configuration validated"
}

# Function to create necessary directories
create_directories() {
    log "Creating necessary directories..."
    
    local dirs=(
        "$DOCKER_DIR/data/postgres"
        "$DOCKER_DIR/data/prometheus"
        "$DOCKER_DIR/data/grafana"
        "$DOCKER_DIR/data/redis"
        "$DOCKER_DIR/data/alertmanager"
        "$DOCKER_DIR/logs/app"
        "$DOCKER_DIR/logs/nginx"
        "$DOCKER_DIR/backups/postgres"
        "$DOCKER_DIR/certs"
    )
    
    for dir in "${dirs[@]}"; do
        if [[ ! -d "$dir" ]]; then
            mkdir -p "$dir"
            log "Created directory: $dir"
        fi
    done
    
    # Set proper permissions
    chmod 750 "$DOCKER_DIR/data"
    chmod 750 "$DOCKER_DIR/logs"
    chmod 750 "$DOCKER_DIR/backups"
    chmod 700 "$DOCKER_DIR/certs"
    
    success "Directories created and permissions set"
}

# Function to backup database
backup_database() {
    if [[ "$BACKUP_BEFORE_DEPLOY" == "false" ]]; then
        log "Skipping database backup"
        return 0
    fi
    
    log "Creating database backup..."
    
    local backup_file="$DOCKER_DIR/backups/postgres/pre-deploy-$(date +%Y%m%d-%H%M%S).sql"
    
    # Check if database is running
    if docker-compose -f "$DOCKER_DIR/$COMPOSE_FILE" --env-file "$DOCKER_DIR/$ENV_FILE" ps postgres | grep -q "Up"; then
        if [[ "${DRY_RUN:-false}" == "true" ]]; then
            log "DRY RUN: Would create backup at $backup_file"
        else
            docker-compose -f "$DOCKER_DIR/$COMPOSE_FILE" --env-file "$DOCKER_DIR/$ENV_FILE" \
                exec -T postgres pg_dump -U postgres performance_db > "$backup_file"
            success "Database backup created: $backup_file"
        fi
    else
        warn "Database not running, skipping backup"
    fi
}

# Function to build and deploy
deploy_application() {
    log "Deploying application..."
    
    cd "$DOCKER_DIR"
    
    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log "DRY RUN: Would execute deployment commands"
        return 0
    fi
    
    # Set build time
    export BUILD_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    # Pull latest images
    log "Pulling latest base images..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull
    
    # Build application image
    log "Building application image..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache app
    
    # Stop existing containers gracefully
    log "Stopping existing containers..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down --timeout 30
    
    # Start services
    log "Starting services..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
    
    success "Application deployed"
}

# Function to run health checks
run_health_checks() {
    if [[ "$RUN_HEALTH_CHECKS" == "false" ]]; then
        log "Skipping health checks"
        return 0
    fi
    
    log "Running health checks..."
    
    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log "DRY RUN: Would run health checks"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # Wait for services to start
    log "Waiting for services to start..."
    sleep 30
    
    # Check application health
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
            success "Application health check passed"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            error "Application health check failed after $max_attempts attempts"
            return 1
        fi
        
        log "Health check attempt $attempt/$max_attempts failed, retrying in 10 seconds..."
        sleep 10
        ((attempt++))
    done
    
    # Check other services
    local services=("postgres:5432" "redis:6379" "prometheus:9090" "grafana:3000")
    
    for service in "${services[@]}"; do
        local host=${service%:*}
        local port=${service#*:}
        
        if nc -z localhost "$port" 2>/dev/null; then
            success "$host service is healthy"
        else
            warn "$host service health check failed"
        fi
    done
    
    # Run comprehensive application tests
    log "Running application functionality tests..."
    
    # Test authentication
    if curl -u admin:${ADMIN_PASSWORD} -f -s http://localhost:8080/api/v1/performance/health > /dev/null; then
        success "Authentication test passed"
    else
        error "Authentication test failed"
        return 1
    fi
    
    # Test database stats endpoint
    if curl -u admin:${ADMIN_PASSWORD} -f -s http://localhost:8080/api/v1/performance/stats/database > /dev/null; then
        success "Database connectivity test passed"
    else
        error "Database connectivity test failed"
        return 1
    fi
    
    success "All health checks passed"
}

# Function to cleanup old images
cleanup_old_images() {
    if [[ "$CLEANUP_OLD_IMAGES" == "false" ]]; then
        log "Skipping image cleanup"
        return 0
    fi
    
    log "Cleaning up old Docker images..."
    
    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log "DRY RUN: Would cleanup old Docker images"
        return 0
    fi
    
    # Remove dangling images
    docker image prune -f
    
    # Remove old application images (keep last 3)
    local old_images=$(docker images batch-performance-analyzer --format "{{.ID}}" | tail -n +4)
    if [[ -n "$old_images" ]]; then
        echo "$old_images" | xargs docker rmi -f || true
        success "Old images cleaned up"
    else
        log "No old images to cleanup"
    fi
}

# Function to show deployment summary
show_deployment_summary() {
    log "Deployment Summary:"
    echo "===========================================" 
    echo "Environment: $ENVIRONMENT"
    echo "Compose File: $COMPOSE_FILE"
    echo "Config File: $ENV_FILE"
    echo "Backup Created: $BACKUP_BEFORE_DEPLOY"
    echo "Health Checks: $RUN_HEALTH_CHECKS"
    echo "Image Cleanup: $CLEANUP_OLD_IMAGES"
    echo "Deployment Time: $(date)"
    echo "==========================================="
    
    if [[ "${DRY_RUN:-false}" != "true" ]]; then
        echo ""
        log "Service URLs:"
        echo "  Application: http://localhost:8080"
        echo "  Grafana: http://localhost:3000"
        echo "  Prometheus: http://localhost:9090"
        echo "  AlertManager: http://localhost:9093"
        echo ""
        log "Default Credentials:"
        echo "  Application Admin: admin / [configured password]"
        echo "  Application Viewer: viewer / [configured password]"
        echo "  Grafana: admin / [configured password]"
        echo ""
        log "Important Files:"
        echo "  Application Logs: $DOCKER_DIR/logs/app/"
        echo "  Database Backups: $DOCKER_DIR/backups/postgres/"
        echo "  Configuration: $DOCKER_DIR/$ENV_FILE"
    fi
}

# Function to rollback deployment
rollback_deployment() {
    error "Deployment failed! Initiating rollback..."
    
    cd "$DOCKER_DIR"
    
    # Stop current deployment
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down --timeout 30
    
    # Find latest backup
    local latest_backup=$(ls -t "$DOCKER_DIR/backups/postgres/pre-deploy-"*.sql 2>/dev/null | head -n1)
    
    if [[ -n "$latest_backup" ]]; then
        log "Restoring database from backup: $latest_backup"
        # Start only postgres for restore
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d postgres
        sleep 10
        
        # Restore database
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" \
            exec -T postgres psql -U postgres -d performance_db < "$latest_backup"
        
        success "Database restored from backup"
    else
        warn "No backup found for rollback"
    fi
    
    error "Rollback completed. Please investigate the deployment issue."
    exit 1
}

# Main execution function
main() {
    echo "======================================"
    echo "Production Deployment Script v2.0.0"
    echo "Database Batch Performance Analyzer"
    echo "======================================"
    echo ""
    
    # Parse arguments
    parse_args "$@"
    
    # Show what we're going to do
    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        warn "DRY RUN MODE - No actual changes will be made"
    fi
    
    log "Starting deployment to $ENVIRONMENT environment..."
    
    # Validate prerequisites
    validate_prerequisites
    validate_environment
    
    # Create directories
    create_directories
    
    # Set up error handling for rollback
    if [[ "${DRY_RUN:-false}" != "true" ]]; then
        trap rollback_deployment ERR
    fi
    
    # Execute deployment steps
    backup_database
    deploy_application
    
    # Disable rollback trap after successful deployment
    trap - ERR
    
    # Post-deployment steps
    run_health_checks
    cleanup_old_images
    
    # Show summary
    show_deployment_summary
    
    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        success "DRY RUN completed successfully"
    else
        success "Production deployment completed successfully!"
    fi
}

# Execute main function with all arguments
main "$@"