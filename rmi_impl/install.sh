#!/bin/bash
DIR=$(pwd)
cd ResourceManagers
export CLASSPATH=$DIR/ResourceManagers
javac ResInterface/ResourceManager.java
jar cvf RMInterface.jar ResInterface/*.class
javac ResImpl/ResourceManagerImpl.java
cp RMInterface.jar ../Middleware

cd ../Middleware
export CLASSPATH=$DIR/Middleware:$DIR/Middleware/RMInterface.jar
javac MidInterface/MidWare.java
jar cvf MiddlewareInterface.jar MidInterface/*.class
javac MidImpl/MidWareImpl.java
cp MiddlewareInterface.jar ../clientsrc/

cd ../clientsrc
export CLASSPATH=$DIR/clientsrc:$DIR/clientsrc/MiddlewareInterface.jar
javac client.java
