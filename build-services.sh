#!/bin/bash

# NexHub Services Build Script
# This script builds all microservices and copies JAR files to root directory

echo "🏗️  NexHub Services Build Script"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES_DIR="$PROJECT_ROOT/services"

# Services to build
SERVICES=("config-server" "discovery-service" "api-gateway" "auth-service")

# Function to build a service
build_service() {
    local service_name=$1
    local service_dir="$SERVICES_DIR/$service_name"
    
    echo -e "${BLUE}📦 Building $service_name...${NC}"
    
    if [[ ! -d "$service_dir" ]]; then
        echo -e "${RED}❌ Service directory not found: $service_dir${NC}"
        return 1
    fi
    
    # Navigate to service directory
    cd "$service_dir" || return 1
    
    # Clean and build
    echo -e "${YELLOW}   🧹 Cleaning previous build...${NC}"
    if ! mvn clean > /dev/null 2>&1; then
        echo -e "${RED}❌ Failed to clean $service_name${NC}"
        return 1
    fi
    
    echo -e "${YELLOW}   🔨 Building JAR file...${NC}"
    if ! mvn package -DskipTests > /dev/null 2>&1; then
        echo -e "${RED}❌ Failed to build $service_name${NC}"
        return 1
    fi
    
    # Find the generated JAR file
    JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)
    
    if [[ -z "$JAR_FILE" ]]; then
        echo -e "${RED}❌ JAR file not found for $service_name${NC}"
        return 1
    fi
    
    # Copy JAR file to project root
    TARGET_JAR="$PROJECT_ROOT/$service_name.jar"
    cp "$JAR_FILE" "$TARGET_JAR"
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Successfully built $service_name${NC}"
        echo -e "${GREEN}   📁 JAR copied to: $service_name.jar${NC}"
    else
        echo -e "${RED}❌ Failed to copy JAR for $service_name${NC}"
        return 1
    fi
    
    # Return to project root
    cd "$PROJECT_ROOT" || return 1
    
    return 0
}

# Function to show build summary
show_summary() {
    echo ""
    echo -e "${BLUE}📊 Build Summary${NC}"
    echo "=================="
    
    for service in "${SERVICES[@]}"; do
        jar_file="$PROJECT_ROOT/$service.jar"
        if [[ -f "$jar_file" ]]; then
            size=$(du -h "$jar_file" | cut -f1)
            echo -e "${GREEN}✅ $service.jar ($size)${NC}"
        else
            echo -e "${RED}❌ $service.jar (missing)${NC}"
        fi
    done
    
    echo ""
    echo -e "${BLUE}🚀 Ready for Docker Compose!${NC}"
    echo "Run: docker-compose up --build"
}

# Main execution
echo -e "${YELLOW}🔍 Checking Maven installation...${NC}"
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed or not in PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Maven found: $(mvn -version | head -1)${NC}"
echo ""

# Build all services
FAILED_SERVICES=()
SUCCESSFUL_BUILDS=0

for service in "${SERVICES[@]}"; do
    if build_service "$service"; then
        ((SUCCESSFUL_BUILDS++))
    else
        FAILED_SERVICES+=("$service")
    fi
    echo ""
done

# Show results
echo "=================================="
echo -e "${BLUE}🎯 Build Results${NC}"
echo "=================================="

if [[ ${#FAILED_SERVICES[@]} -eq 0 ]]; then
    echo -e "${GREEN}🎉 All services built successfully! ($SUCCESSFUL_BUILDS/${#SERVICES[@]})${NC}"
    show_summary
else
    echo -e "${RED}❌ Some services failed to build:${NC}"
    for failed in "${FAILED_SERVICES[@]}"; do
        echo -e "${RED}   - $failed${NC}"
    done
    echo ""
    echo -e "${YELLOW}✅ Successfully built: $SUCCESSFUL_BUILDS/${#SERVICES[@]} services${NC}"
    
    if [[ $SUCCESSFUL_BUILDS -gt 0 ]]; then
        show_summary
    fi
    
    exit 1
fi

echo ""
echo -e "${GREEN}🏁 Build process completed successfully!${NC}"