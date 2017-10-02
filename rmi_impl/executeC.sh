#!/bin/bash

DIR=$(pwd)
export CLASSPATH=$DIR/clientsrc:$DIR/clientsrc/MiddlewareInterface.jar
cd clientsrc
java -Djava.security.policy=java.policy client localhost 1099
