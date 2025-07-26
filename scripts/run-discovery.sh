#!/bin/bash
echo "Starting Discovery Server..."
cd services/discovery-server
nohup mvn spring-boot:run > ../../logs/discovery-server.log 2>&1 &
echo $! > ../../pids/discovery-server.pid
echo "Discovery Server started with PID $(cat ../../pids/discovery-server.pid)"
echo "Logs: tail -f logs/discovery-server.log"