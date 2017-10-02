#!/bin/bash
DIR=$(dirname -- $(readlink -fn -- "$0"))
cd servercode
export CLASSPATH=$DIR/servercode
javac ResInterface/ResourceManager.java
jar cvf ResInterface.jar ResInterface/*.class
javac ResImpl/ResourceManagerImpl.java
cp ResInterface.jar ../clientsrc/
cd ../clientsrc
export CLASSPATH=$DIR/clientsrc:$DIR/clientsrc/ResInterface.jar
javac client.java
