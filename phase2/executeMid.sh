#!/bin/bash

rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1099 &

DIR=$(pwd)
export CLASSPATH=$DIR/middleware:$DIR/middleware/RMInterface.jar
cd middleware

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/middleware/ MiddlewareImpl.MiddlewareManagerImpl 1099 localhost localhost localhost
