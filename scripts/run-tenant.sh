#!/bin/bash
echo "Starting Tenant Service..."
cd services/tenant-service
nohup mvn spring-boot:run > ../../logs/tenant-service.log 2>&1 &
echo $! > ../../pids/tenant-service.pid
echo "Tenant Service started with PID $(cat ../../pids/tenant-service.pid)"
echo "Logs: tail -f logs/tenant-service.log"