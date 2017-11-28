#!/bin/bash

if [ $# -eq 0 ]; then
    echo "No arguments supplied. Need to provide project name."
    echo " (m) - middleware\\n (s) - server\\n (c) - client\\n"
    exit 1
fi

DIR=$(pwd)
project=$1
testType=$2
loopCount=$3
load=$4
distrib=$5
clients=$6


# "[1] - Read on one RM\n" +
#                         "[2] - Read on multiple RM\n" +
#                         "[3] - Write on one RM\n" +
#                         "[4] - Write on multiple RM\n" +
#                         "[5] - R/W on one RM\n" +
#                         "[6] - R/W on multiple RM\n" +
#                         "[0] - quit\n\n>");

if [ "$project" = "s" ]; then
  	cd resourcemanagers

	while true
	do
	  read -p "Whats the RM type [Car, Flight or Hotel]? " answer
	  case $answer in
	  	[Cc]* ) 
				# pkill -f rmiregistry
				# sleep(2000)
				# rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1100 &
				# java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ResImpl.ResourceManagerImpl 1100 car
				java -Djgroups.bind_addr=cs-25.cs.mcgill.ca -Djgroups.udp.mcast_port=10999 -Djava.net.preferIPv4Stack=true -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ReplicationManager.RMReplicationManager 1100 car
	           	break;;

	   	[Ff]* ) 
				# pkill -f rmiregistry
				# sleep(2000)
				# rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1101 &
				# java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ResImpl.ResourceManagerImpl 1102 flight
				java -Djgroups.bind_addr=cs-28.cs.mcgill.ca -Djgroups.udp.mcast_port=11000 -Djava.net.preferIPv4Stack=true -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ReplicationManager.RMReplicationManager 1102 flight
				break;;

		[Hh]* )	
				# pkill -f rmiregistry
				# sleep(2000)
				# rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1102 &
				# java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ResImpl.ResourceManagerImpl 1101 hotel
				java -Djgroups.bind_addr=cs-27.cs.mcgill.ca -Djgroups.udp.mcast_port=11001 -Djava.net.preferIPv4Stack=true -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ReplicationManager.RMReplicationManager 1101 hotel
				break;;

	   	* )     echo "Dude, just enter Car, Flight or Hotel, please."; continue;;
	  esac
	done
fi


if [ "$project" = "m" ]; then
  	cd middleware
  	# pkill -f rmiregistry
  	# sleep(2000)
  	# rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1099 &
	#java -Djava.net.preferIPv4Stack=true -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/middleware/ MiddlewareImpl.MiddlewareManagerImpl cs-24.cs.mcgill.ca cs-25.cs.mcgill.ca cs-28.cs.mcgill.ca cs-27.cs.mcgill.ca
	java -Djava.net.preferIPv4Stack=true -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/middleware/ Replication.MWReplicationManager cs-24.cs.mcgill.ca cs-25.cs.mcgill.ca cs-28.cs.mcgill.ca cs-27.cs.mcgill.ca
	#java -Djava.net.preferIPv4Stack=true -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/middleware/ Replication.MWReplicationManager 132.206.52.44 132.206.52.45 132.206.52.48 132.206.52.47
fi


if [ "$project" = "c" ]; then
  	cd clientsrc

	# read -p "Middleware IP: " middlewareIP
	# read -p "Middleware Port: " port
    	# clientId=`hostname`

	# if [ -z "$middlewareIP" ]; then
    		# middlewareIP="cs-26.cs.mcgill.ca"
	# fi

	# if [ -z "$port" ]; then
    		# port="22000"
	# fi

	java -Djava.net.preferIPv4Stack=true -Djava.security.policy=java.policy Client cs-24.cs.mcgill.ca
fi

if [ "$project" = "p" ]; then
  	cd perf-clientsrc

	# read -p "Middleware IP: " middlewareIP
	# read -p "Middleware Port: " port
    	# clientId=`hostname`

	# if [ -z "$middlewareIP" ]; then
    		# middlewareIP="cs-26.cs.mcgill.ca"
	# fi

	# if [ -z "$port" ]; then
    		# port="22000"
	# fi

	java -Djava.security.policy=java.policy ClientManager cs-24.cs.mcgill.ca $testType $loopCount $load $distrib $clients
fi