#!/bin/bash

# NexHub Services Port Killer Script
# This script kills all processes running on NexHub service ports

echo "🔥 NexHub Port Killer Script"
echo "=============================="

# Define ports used by NexHub services  
PORTS=(5432 8080 8888 8081 8082)
SERVICE_NAMES=("PostgreSQL" "pgAdmin" "Config Server" "API Gateway" "Auth Service")

# Function to kill process on a specific port
kill_port() {
    local port=$1
    local service_name=$2
    
    echo "🔍 Checking port $port ($service_name)..."
    
    # Find process ID using the port
    if command -v lsof &> /dev/null; then
        # Unix/Linux/macOS using lsof
        PID=$(lsof -ti:$port)
    elif command -v netstat &> /dev/null; then
        # Windows/Linux using netstat
        if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
            # Windows
            PID=$(netstat -ano | findstr ":$port " | awk '{print $5}' | head -1)
        else
            # Linux
            PID=$(netstat -tlnp | grep ":$port " | awk '{print $7}' | cut -d'/' -f1 | head -1)
        fi
    else
        echo "❌ Neither lsof nor netstat found. Cannot detect processes."
        return 1
    fi
    
    if [[ -n "$PID" && "$PID" != "0" ]]; then
        echo "📍 Found process $PID running on port $port"
        
        # Kill the process
        if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
            # Windows
            taskkill //F //PID $PID > /dev/null 2>&1
        else
            # Unix/Linux/macOS
            kill -9 $PID > /dev/null 2>&1
        fi
        
        if [[ $? -eq 0 ]]; then
            echo "✅ Successfully killed $service_name (PID: $PID) on port $port"
        else
            echo "❌ Failed to kill process $PID on port $port"
        fi
    else
        echo "✨ Port $port is already free"
    fi
    echo ""
}

# Function to show all NexHub processes
show_processes() {
    echo "📊 Current NexHub service processes:"
    echo "======================================"
    
    for i in "${!PORTS[@]}"; do
        port=${PORTS[$i]}
        service_name=${SERVICE_NAMES[$i]}
        
        if command -v lsof &> /dev/null; then
            result=$(lsof -ti:$port 2>/dev/null)
        elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
            result=$(netstat -ano | findstr ":$port " | head -1)
        else
            result=$(netstat -tlnp | grep ":$port " | head -1)
        fi
        
        if [[ -n "$result" ]]; then
            echo "🔴 $service_name (Port $port): RUNNING"
        else
            echo "⚪ $service_name (Port $port): FREE"
        fi
    done
    echo ""
}

# Main execution
case "${1:-}" in
    "show"|"status"|"list")
        show_processes
        ;;
    "kill"|"stop"|"")
        echo "🚀 Starting port cleanup for NexHub services..."
        echo ""
        
        # Kill each service
        for i in "${!PORTS[@]}"; do
            kill_port ${PORTS[$i]} "${SERVICE_NAMES[$i]}"
        done
        
        echo "🎉 Port cleanup completed!"
        echo ""
        
        # Show final status
        show_processes
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  kill, stop    Kill all NexHub service processes (default)"
        echo "  show, status  Show current status of NexHub service ports"
        echo "  help          Show this help message"
        echo ""
        echo "Ports managed:"
        for i in "${!PORTS[@]}"; do
            echo "  ${PORTS[$i]} - ${SERVICE_NAMES[$i]}"
        done
        ;;
    *)
        echo "❌ Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac