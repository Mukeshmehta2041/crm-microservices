#!/bin/bash
echo "Starting Users Service..."
cd services/users-service
nohup mvn spring-boot:run > ../../logs/users-service.log 2>&1 &
echo $! > ../../pids/users-service.pid
echo "Users Service started with PID $(cat ../../pids/users-service.pid)"
echo "Logs: tail -f logs/users-service.log"