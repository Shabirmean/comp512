#!/bin/bash

DIR=$(pwd)
export CLASSPATH=$DIR/clientsrc:$DIR/clientsrc/MiddlewareInterface.jar
cd clientsrc
java -Djava.security.policy=java.policy client cs-26.cs.mcgill.ca 1099
