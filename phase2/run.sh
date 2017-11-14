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
				java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ResImpl.ResourceManagerImpl 1100
	           	break;;

	   	[Ff]* ) 
				# pkill -f rmiregistry
				# sleep(2000)
				# rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1101 &
				java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ResImpl.ResourceManagerImpl 1102
				break;;

		[Hh]* )	
				# pkill -f rmiregistry
				# sleep(2000)
				# rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1102 &
				java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/resourcemanagers/ ResImpl.ResourceManagerImpl 1101
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
	java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$DIR/middleware/ MiddlewareImpl.MiddlewareManagerImpl cs-24.cs.mcgill.ca cs-25.cs.mcgill.ca cs-26.cs.mcgill.ca cs-27.cs.mcgill.ca
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

	java -Djava.security.policy=java.policy Client cs-24.cs.mcgill.ca
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

	java -Djava.security.policy=java.policy ClientManager cs-24.cs.mcgill.ca $testType $loopCount $load
fi