#!/bin/bash
echo "Starting Auth Service..."
cd services/auth-service
nohup mvn spring-boot:run > ../../logs/auth-service.log 2>&1 &
echo $! > ../../pids/auth-service.pid
echo "Auth Service started with PID $(cat ../../pids/auth-service.pid)"
echo "Logs: tail -f logs/auth-service.log"