===============================================================
	Running the TCP-Sockets based distributed setup	       
==============================================================

The "comp512.sh" script is written to easily run the setup.
This script is heavily customized to assume that the servers are run on the follwoing machines:

	Middleware Server:		cs-26.cs.mcgiil.ca:22000
	Resource Manager (Flight):	cs-27.cs.mcgill.ca:22000
	Resource Manager (Car):		cs-28.cs.mcgill.ca:22000
	Resource Manager (Hotel):	cs-29.cs.mcgill.ca:22000

	Clients:	Can be run on any machine, but need to provide the correct MiddlewareIP:PORT as arguments.

Before running the script ensure to make the necessary changes in the following ".conf" files if you are running the servers in other than the above hosts.
	middleware/conf/middleware.conf
	server/conf/server_car.conf
	server/conf/server_flight.conf
	server/conf/server_hotel.conf

Also note, that the "interface" package is common to all other sources (client, middleware & server). Thus, it needs to be installed to the local ".m2" repo first to build the others.

-------------------------------------------------------------------
Run the following commands in order.
	- Do the changes in Step(1) in the script
	- Run step(2) on all the machines in which the clients, servers or the middleware would run.
	- step(3) on the host where the middleware would run
	- step(4) on the hosts where the servers would run
	- step(5) on the hosts where the clients would run
-------------------------------------------------------------------

1) Change the following line:
	mvn install:install-file -Dfile=/home/2016/sabdul23/Desktop/COMP512/comp512/tcp_impl/interface/target/interface-1.0-SNAPSHOT.jar -DgroupId=comp512-ShaJian -DartifactId=interface -Dversion=1.0-SNAPSHOT -Dpackaging=jar

  by filling in the correct path in the following places:
	mvn install:install-file -Dfile=<HERE>/tcp_impl/interface/target/interface-1.0-SNAPSHOT.jar -DgroupId=comp512-ShaJian -DartifactId=interface -Dversion=1.0-SNAPSHOT -Dpackaging=jar

2) ./comp512.sh i	-	builds the "interface" package and installs it in the local repo
3) ./comp512.sh m	-	press "n" when it asks whether to do a "git pull"
				press "y" to build it

4) ./comp512.sh s	-	press "n" when it asks whether to do a "git pull"
                                press "y" to build it
				mention the type of RM (c) for Car, (f) for Flight and (h) for Hotel

5) ./comp512.sh c	-	press "n" when it asks whether to do a "git pull"
                                press "y" to build it
				provide the correct IP and the PORT of the Middleware server


For question, comments and queries:	shabir.abdulsamadh@mail.mcgill.ca
