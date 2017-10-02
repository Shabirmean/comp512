#!/bin/bash

if [ $# -eq 0 ]; then
    echo "No arguments supplied. Need to provide project name."
    echo " (i) - interface\\n (m) - middleware\\n (s) - server\\n (c) - client\\n"
    exit 1
fi

project=$1

while true
do
  read -p "Do I have to pull from git? " answer
  case $answer in
   [yY]* ) git pull
           break;;

   [nN]* ) break;;

   * )     echo "Dude, just enter Y or N, please."; continue;;
  esac
done


if [ "$project" = "i" ]; then
  	cd interface
  	mvn package
  	mvn install:install-file -Dfile=/home/2016/sabdul23/Desktop/COMP512/comp512/tcp_impl/interface/target/interface-1.0-SNAPSHOT.jar -DgroupId=comp512-ShaJian -DartifactId=interface -Dversion=1.0-SNAPSHOT -Dpackaging=jar
fi

if [ "$project" = "s" ]; then
  	cd server

	while true
	do
	  read -p "Do I have to build server? " answer
	  case $answer in
	   [yY]* ) mvn compile assembly:single
	           break;;

	   [nN]* ) break;;

	   * )     echo "Dude, just enter Y or N, please."; continue;;
	  esac
	done

	while true
	do
	  read -p "Whats the RM type [Car, Flight or Hotel]? " answer
	  case $answer in
	  	[Cc]* ) java -jar target/server-1.0-SNAPSHOT-jar-with-dependencies.jar conf/server_car.conf
	           	break;;

	   	[Ff]* ) java -jar target/server-1.0-SNAPSHOT-jar-with-dependencies.jar conf/server_flight.conf
				break;;

		[Hh]* )	java -jar target/server-1.0-SNAPSHOT-jar-with-dependencies.jar conf/server_hotel.conf
				break;;

	   	* )     echo "Dude, just enter Car, Flight or Hotel, please."; continue;;
	  esac
	done
fi


if [ "$project" = "m" ]; then
  	cd middleware

  	while true
	do
	  read -p "Do I have to build middleware? " answer
	  case $answer in
	   [yY]* ) mvn compile assembly:single
	           break;;

	   [nN]* ) break;;

	   * )     echo "Dude, just enter Y or N, please."; continue;;
	  esac
	done

	java -jar target/middleware-1.0-SNAPSHOT-jar-with-dependencies.jar conf/middleware.conf
fi


if [ "$project" = "c" ]; then
  	cd client

  	while true
	do
	  read -p "Do I have to build client? " answer
	  case $answer in
	   [yY]* ) mvn compile assembly:single
	           break;;

	   [nN]* ) break;;

	   * )     echo "Dude, just enter Y or N, please."; continue;;
	  esac
	done

	read -p "Middleware IP: " middlewareIP
	read -p "Middleware Port: " port
    clientId=`hostname`

	if [ -z "$middlewareIP" ]; then
    		middlewareIP="cs-26.cs.mcgill.ca"
	fi

	if [ -z "$port" ]; then
    		port="22000"
	fi

	java -jar target/client-1.0-SNAPSHOT-jar-with-dependencies.jar $middlewareIP $port $clientId
fi
