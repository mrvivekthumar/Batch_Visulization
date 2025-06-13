#!/bin/bash
# Production Docker Entrypoint Script
# File: Docker/scripts/docker-entrypoint.sh

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
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

# Function to check required environment variables
check_required_env() {
    local required_vars=("DB_PASSWORD" "ADMIN_PASSWORD" "VIEWER_PASSWORD")
    local missing_vars=()

    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("$var")
        fi
    done

    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        error "Missing required environment variables:"
        for var in "${missing_vars[@]}"; do
            error "  - $var"
        done
        error "Please set these variables and restart the container."
        exit 1
    fi
}

# Function to validate environment variables
validate_env() {
    log "Validating environment variables..."

    # Check database configuration
    if [[ -z "$DB_HOST" ]]; then
        warn "DB_HOST not set, using default: postgres"
        export DB_HOST="postgres"
    fi

    if [[ -z "$DB_PORT" ]]; then
        warn "DB_PORT not set, using default: 5432"
        export DB_PORT="5432"
    fi

    if [[ -z "$DB_NAME" ]]; then
        warn "DB_NAME not set, using default: performance_db"
        export DB_NAME="performance_db"
    fi

    # Validate password strength (basic check)
    if [[ ${#ADMIN_PASSWORD} -lt 8 ]]; then
        error "ADMIN_PASSWORD must be at least 8 characters long"
        exit 1
    fi

    if [[ ${#VIEWER_PASSWORD} -lt 8 ]]; then
        error "VIEWER_PASSWORD must be at least 8 characters long"
        exit 1
    fi

    success "Environment variables validated"
}

# Function to wait for database
wait_for_database() {
    log "Waiting for database at $DB_HOST:$DB_PORT..."
    
    local max_attempts=30
    local attempt=1
    
    while ! nc -z "$DB_HOST" "$DB_PORT" >/dev/null 2>&1; do
        if [[ $attempt -ge $max_attempts ]]; then
            error "Database is not available after $max_attempts attempts"
            exit 1
        fi
        
        log "Database not ready, waiting... (attempt $attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done
    
    success "Database is available"
}

# Function to test database connection
test_database_connection() {
    log "Testing database connection..."
    
    # Use PostgreSQL client to test connection if available
    if command -v psql &> /dev/null; then
        if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" &> /dev/null; then
            success "Database connection test successful"
        else
            error "Database connection test failed"
            exit 1
        fi
    else
        warn "PostgreSQL client not available, skipping connection test"
    fi
}

# Function to setup logging directory
setup_logging() {
    log "Setting up logging directory..."
    
    # Ensure logs directory exists and has correct permissions
    if [[ ! -d "/app/logs" ]]; then
        mkdir -p /app/logs
    fi
    
    # Set correct permissions
    chmod 755 /app/logs
    
    success "Logging directory setup complete"
}

# Function to check disk space
check_disk_space() {
    log "Checking disk space..."
    
    local available_space
    available_space=$(df /app | awk 'NR==2 {print $4}')
    local available_gb=$((available_space / 1024 / 1024))
    
    if [[ $available_gb -lt 1 ]]; then
        error "Insufficient disk space: ${available_gb}GB available"
        exit 1
    fi
    
    log "Available disk space: ${available_gb}GB"
}

# Function to check memory
check_memory() {
    log "Checking memory configuration..."
    
    local total_memory
    total_memory=$(free -m | awk 'NR==2{print $2}')
    
    if [[ $total_memory -lt 512 ]]; then
        warn "Low memory detected: ${total_memory}MB. Performance may be affected."
    else
        log "Available memory: ${total_memory}MB"
    fi
}

# Function to optimize JVM settings based on container resources
optimize_jvm_settings() {
    log "Optimizing JVM settings..."
    
    # Get container memory limit
    local memory_limit_bytes
    if [[ -f "/sys/fs/cgroup/memory/memory.limit_in_bytes" ]]; then
        memory_limit_bytes=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes)
    else
        memory_limit_bytes=$(free -b | awk 'NR==2{print $2}')
    fi
    
    local memory_limit_mb=$((memory_limit_bytes / 1024 / 1024))
    
    # Calculate optimal heap size (75% of available memory)
    local heap_size_mb=$((memory_limit_mb * 75 / 100))
    
    if [[ $heap_size_mb -gt 0 ]]; then
        export JAVA_OPTS="$JAVA_OPTS -Xmx${heap_size_mb}m"
        log "Set JVM heap size to ${heap_size_mb}MB"
    fi
    
    # Add additional JVM optimizations
    export JAVA_OPTS="$JAVA_OPTS -XX:+UnlockExperimentalVMOptions"
    export JAVA_OPTS="$JAVA_OPTS -XX:+UseCGroupMemoryLimitForHeap"
    export JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails"
    export JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCTimeStamps"
    export JAVA_OPTS="$JAVA_OPTS -Xloggc:/app/logs/gc.log"
    export JAVA_OPTS="$JAVA_OPTS -XX:+UseGCLogFileRotation"
    export JAVA_OPTS="$JAVA_OPTS -XX:NumberOfGCLogFiles=5"
    export JAVA_OPTS="$JAVA_OPTS -XX:GCLogFileSize=10M"
    
    success "JVM settings optimized"
}

# Function to create health check script
create_health_check() {
    log "Creating health check script..."
    
    cat > /app/health-check.sh << 'EOF'
#!/bin/bash
# Simple health check script

# Check if application is responding
if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
    exit 0
else
    exit 1
fi
EOF
    
    chmod +x /app/health-check.sh
    success "Health check script created"
}

# Function to setup signal handlers for graceful shutdown
setup_signal_handlers() {
    log "Setting up signal handlers..."
    
    # Function to handle shutdown signals
    shutdown_handler() {
        log "Received shutdown signal, performing graceful shutdown..."
        
        # Send SIGTERM to Java process if it exists
        if [[ -n "$JAVA_PID" ]]; then
            kill -TERM "$JAVA_PID"
            
            # Wait for graceful shutdown (max 30 seconds)
            local count=0
            while kill -0 "$JAVA_PID" 2>/dev/null && [[ $count -lt 30 ]]; do
                sleep 1
                ((count++))
            done
            
            # Force kill if still running
            if kill -0 "$JAVA_PID" 2>/dev/null; then
                warn "Forcefully killing Java process"
                kill -KILL "$JAVA_PID"
            fi
        fi
        
        success "Graceful shutdown completed"
        exit 0
    }
    
    # Register signal handlers
    trap shutdown_handler SIGTERM SIGINT
}

# Function to start the application
start_application() {
    log "Starting Database Batch Performance Analyzer..."
    log "Configuration:"
    log "  - Database: $DB_HOST:$DB_PORT/$DB_NAME"
    log "  - Profile: ${SPRING_PROFILES_ACTIVE:-production}"
    log "  - Port: ${SERVER_PORT:-8080}"
    log "  - JVM Options: $JAVA_OPTS"
    
    # Start the application in background
    exec "$@" &
    JAVA_PID=$!
    
    # Wait for the process
    wait $JAVA_PID
}

# Function to print banner
print_banner() {
    cat << 'EOF'
    ____        _        _                     ____             __                                            
   / __ \____ _| |_ ____| |__  ____ _________ / __ \___  ____  / /_____  ______ ___  ____ _____  __________ 
  / / / / __ `/ __/ __ `/ __ \/ __ `/ ___/ _ \/ /_/ / _ \/ __ \/ __/ __ \/ ___/ __ \/ __ `/ __ \/ ___/ _ \   
 / /_/ / /_/ / /_/ /_/ / /_/ / /_/ (__  )  __/ ____/  __/ /_/ / /_/ /_/ / /  / / / / /_/ / / / / /__/  __/   
/_____/\__,_/\__/\__,_/_.___/\__,_/____/\___/_/    \___/\____/\__/\____/_/  /_/ /_/\__,_/_/ /_/\___/\___/    
                                                                                                            
    Performance Analyzer v2.0.0 - Production Mode
EOF
    echo ""
}

# Main execution
main() {
    print_banner
    
    log "Starting production initialization..."
    
    # Check if we're running in production mode
    if [[ "$SPRING_PROFILES_ACTIVE" == *"prod"* ]]; then
        log "Running in PRODUCTION mode"
        
        # Run all production checks
        check_required_env
        validate_env
        setup_logging
        check_disk_space
        check_memory
        optimize_jvm_settings
        create_health_check
        setup_signal_handlers
        wait_for_database
        test_database_connection
        
        success "Production initialization completed"
    else
        log "Running in DEVELOPMENT mode"
        warn "Some production checks are skipped in development mode"
        
        # Run minimal checks for development
        setup_logging
        setup_signal_handlers
        wait_for_database
    fi
    
    # Start the application
    start_application "$@"
}

# Execute main function with all arguments
main "$@"