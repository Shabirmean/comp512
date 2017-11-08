#!/bin/bash

rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1101 &

DIR=$(pwd)
export CLASSPATH=$DIR/ResourceManagers
cd ResourceManagers

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/ResourceManagers/ ResImpl.ResourceManagerImpl 1101
