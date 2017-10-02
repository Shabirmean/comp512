#!/bin/bash

rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1099 &
sleep 1

DIR=$(dirname -- $(readlink -fn -- "$0"))
export CLASSPATH=$DIR/servercode
cd servercode

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/servercode/ ResImpl.ResourceManagerImpl 1099
