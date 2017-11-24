#!/bin/bash

DIR=$(pwd)

export CLASSPATH=$DIR/jgroups-4.0.8.Final.jar:$DIR/resourcemanagers:$DIR/middleware:$DIR/clientsrc:$DIR/lockmanager:$DIR/perf-clientsrc:$DIR/resourcemanagers/ResInterface.jar:$DIR/resourcemanagers/RMReplicationManager.jar:$DIR/resourcemanagers/ResourceManager.jar:$DIR/lockmanager/LockManager.jar:$DIR/middleware/Middleware.jar

cd $DIR/resourcemanagers
echo "grant codeBase \"file:$DIR/resourcemanagers/\" {
    permission java.security.AllPermission;
};" > java.policy

javac ResInterface/ResourceManager.java
jar cvf ResInterface.jar ResInterface/*.class
javac ReplicationManager/RMReplicationManager.java
jar cvf RMReplicationManager.jar ReplicationManager/*.class
javac ResImpl/ResourceManagerImpl.java
jar cvf ResourceManager.jar ResImpl/*.class

cd $DIR/lockmanager
javac LockManager/LockManager.java 
jar cvf LockManager.jar LockManager/*.class

cd $DIR/middleware
echo "grant codeBase \"file:$DIR/middleware/\" {
    permission java.security.AllPermission;
};" > java.policy

javac MiddlewareInterface/Middleware.java 
jar cvf Middleware.jar MiddlewareInterface/*.class
javac MiddlewareImpl/MiddlewareManagerImpl.java 

cd $DIR/clientsrc
echo "grant codeBase \"file:$DIR/clientsrc/\" {
    permission java.security.AllPermission;
};" > java.policy

javac Client.java

#cd $DIR/perf-clientsrc
#echo "grant codeBase \"file:$DIR/perf-clientsrc/\" {
#    permission java.security.AllPermission;
#};" > java.policy

#javac ClientManager.java

cd $DIR
chmod -R 705 ./*



# java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ResImpl.ResourceManagerImpl 1100


# java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/middleware/ MiddlewareImpl.MiddlewareManagerImpl 1099

# java -Djava.security.policy=java.policy Client localhost 1099