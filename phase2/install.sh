#!/bin/bash
DIR=$(pwd)
cd ResourceManagers
export CLASSPATH=$DIR/resourcemanagers
javac ResInterface/ResourceManager.java
jar cvf RMInterface.jar ResInterface/*.class
javac ResImpl/ResourceManagerImpl.java
cp RMInterface.jar ../middleware

cd ../middleware
export CLASSPATH=$DIR/middleware:$DIR/middleware/RMInterface.jar
javac MiddlewareInterface/Middleware.java
jar cvf MiddlewareInterface.jar MiddlewareInterface/*.class
javac MiddlewareImpl/MiddlewareManagerImpl.java
cp MiddlewareInterface.jar ../clientsrc/

cd ../clientsrc
export CLASSPATH=$DIR/clientsrc:$DIR/clientsrc/MiddlewareInterface.jar
javac client.java
