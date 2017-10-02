#!/bin/bash

rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1099 &

DIR=$(pwd)
export CLASSPATH=$DIR/Middleware:$DIR/Middleware/RMInterface.jar
cd Middleware

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/Middleware/ MidImpl.MidWareImpl 1099 localhost localhost localhost
