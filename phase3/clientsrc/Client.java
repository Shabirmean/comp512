import MiddlewareInterface.Middleware;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.Protocol;
import util.MiddlewareConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

/**
 * Created by shabirmean on 2017-11-27 with some hope.
 */
@SuppressWarnings("Duplicates")
public class Client extends ReceiverAdapter {
    private static final int MW_CLUSTER_PORT = 10998;
    private static final String CLUSTER_NAME = "MWCluster";

    private Middleware middleware;
    private String clientId = "Client::MW-";
    private String clientAddress;
    private String mwRegistryAdd;
    private int mwRegistryPort;
    private JChannel channel;
    private Address leader;

    private Client(String cId, String clientAdd, String mwAdd, int mwPort) {
        clientId += cId;
        clientAddress = clientAdd;
        mwRegistryAdd = mwAdd;
        mwRegistryPort = mwPort;
    }

    public static void main(String args[]) {
        String clientServer = "localhost";
        String middlewareServer = "localhost";
        int port = 1099;

        if (args.length > 0) {
            clientServer = args[0];
        }
        if (args.length > 1) {
            middlewareServer = args[1];
        }
        if (args.length > 2) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length > 3) {
            System.out.println("Usage: java ClientRunner [MW-Host [MW-Port]]");
            System.exit(1);
        }

        final String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        System.out.println("Client::Random UUID-" + uuid);
        Client client = new Client(uuid, clientServer, middlewareServer, port);
        client.start();

        System.out.println("\n\n\tClient Interface");
        System.out.println("Type \"help\" for list of supported commands");
        client.fetchMiddlewareProxy();
        client.run();
    }

    public void start() {
        Protocol[] protocolStack;
        try {
            protocolStack = new Protocol[]{
                    new UDP()
                            .setValue(MiddlewareConstants.JGRP_BIND_ADDRESS, InetAddress.getByName(clientAddress))
                            .setValue(MiddlewareConstants.JGRP_MC_PORT, MW_CLUSTER_PORT),
                    new PING(),
                    new MERGE3(),
                    new FD_SOCK(),
                    new FD_ALL(),
                    new VERIFY_SUSPECT(),
                    new BARRIER(),
                    new NAKACK2(),
                    new UNICAST3(),
                    new STABLE(),
                    new GMS(),
                    new UFC(),
                    new MFC(),
                    new FRAG2(),
                    new STATE_TRANSFER()};

            channel = new JChannel(protocolStack).setReceiver(this).setName(clientId);
            channel.connect(CLUSTER_NAME);
            leader = channel.getView().getCoord();
            System.out.println("Client::MW::Connected to cluster-" + CLUSTER_NAME + " with id-" + clientId +
                    " under leader-" + leader);
        } catch (UnknownHostException e) {
            //TODO::Handle exceptions
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchMiddlewareProxy() {
        try {
            Registry registry = LocateRegistry.getRegistry(this.mwRegistryAdd, this.mwRegistryPort);
            middleware = (Middleware) registry.lookup("ShabirJianMiddleware");
            if (middleware != null) {
//                System.out.println("-----------------------------------------------");
//                System.out.println("Successful");
//                System.out.println("Connected to Middleware");
            } else {
                System.out.println("Unsuccessful");
            }
        } catch (Exception e) {
            System.err.println("ClientRunner exception: " + e.toString());
            e.printStackTrace();
        }
//        System.out.println("-----------------------------------------------");
//        if (System.getSecurityManager() == null) {
//            //System.setSecurityManager(new RMISecurityManager());
//        }
    }

    public void viewAccepted(View newView) {
//        System.out.println("MW::ViewChange " + CLUSTER_NAME + "-" + newView);
        Address previousHead = leader;
        leader = newView.getCoord();
        if (newView.size() > 1) {
            if (previousHead != null &&
                    (!newView.containsMember(previousHead) || previousHead.toString().equals(clientId))) {
//                System.out.println("Client::Lost leader node of custer-" + CLUSTER_NAME);
//                System.out.println("Client::New leader for cluster-" + CLUSTER_NAME + " is " + leader.toString());
                fetchMiddlewareProxy();
            }
        } else {
            System.out.println(clientId + "- is the only participant in the cluster-" + CLUSTER_NAME);
            System.out.println("Quitting client...");
            System.exit(-1);
        }
    }


    private void run() {
//    public static void main(String args[]) {
//        ClientRunner obj = new ClientRunner();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        Vector arguments;
        int Id, Cid;
        int flightNum;
        int flightPrice;
        int flightSeats;
        boolean Room;
        boolean Car;
        int price;
        int numRooms;
        int numCars;
        String location;

        while (true) {
            System.out.print("\n>");
            try {
                //read the next command
                command = stdin.readLine();
            } catch (IOException io) {
                System.out.println("Unable to read from standard in");
                System.exit(1);
            }
            //remove heading and trailing white space
            command = command.trim();
            arguments = parse(command);

            //decide which of the commands this was
            switch (findChoice((String) arguments.elementAt(0))) {
                case 1: //help section
                    if (arguments.size() == 1)   //command was "help"
                        listCommands();
                    else if (arguments.size() == 2)  //command was "help <commandname>"
                        listSpecific((String) arguments.elementAt(1));
                    else  //wrong use of help command
                        System.out.println("Improper use of help command. Type help or help, <commandname>");
                    break;

                case 2:  //new flight
                    if (arguments.size() != 5) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Flight using id: " + arguments.elementAt(1));
                    System.out.println("Flight number: " + arguments.elementAt(2));
                    System.out.println("Add Flight Seats: " + arguments.elementAt(3));
                    System.out.println("Set Flight Price: " + arguments.elementAt(4));

                    try {
                        Id = getInt(arguments.elementAt(1));
                        flightNum = getInt(arguments.elementAt(2));
                        flightSeats = getInt(arguments.elementAt(3));
                        flightPrice = getInt(arguments.elementAt(4));
                        if (middleware.addFlight(Id, flightNum, flightSeats, flightPrice))
                            System.out.println("Flight added");
                        else
                            System.out.println("Flight could not be added");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
//                        e.printStackTrace();
                    }
                    break;

                case 3:  //new Car
                    if (arguments.size() != 5) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Car using id: " + arguments.elementAt(1));
                    System.out.println("Car Location: " + arguments.elementAt(2));
                    System.out.println("Add Number of Cars: " + arguments.elementAt(3));
                    System.out.println("Set Price: " + arguments.elementAt(4));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));
                        numCars = getInt(arguments.elementAt(3));
                        price = getInt(arguments.elementAt(4));
                        if (middleware.addCars(Id, location, numCars, price))
                            System.out.println("Cars added");
                        else
                            System.out.println("Cars could not be added");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
//                        e.printStackTrace();
                    }
                    break;

                case 4:  //new Room
                    if (arguments.size() != 5) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Room using id: " + arguments.elementAt(1));
                    System.out.println("Room Location: " + arguments.elementAt(2));
                    System.out.println("Add Number of Rooms: " + arguments.elementAt(3));
                    System.out.println("Set Price: " + arguments.elementAt(4));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));
                        numRooms = getInt(arguments.elementAt(3));
                        price = getInt(arguments.elementAt(4));
                        if (middleware.addRooms(Id, location, numRooms, price))
                            System.out.println("Rooms added");
                        else
                            System.out.println("Rooms could not be added");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 5:  //new Customer
                    if (arguments.size() != 2) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Customer using id:" + arguments.elementAt(1));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        int customer = middleware.newCustomer(Id);
                        System.out.println("new customer id:" + customer);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 6: //delete Flight
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting a flight using id: " + arguments.elementAt(1));
                    System.out.println("Flight Number: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        flightNum = getInt(arguments.elementAt(2));
                        if (middleware.deleteFlight(Id, flightNum))
                            System.out.println("Flight Deleted");
                        else
                            System.out.println("Flight could not be deleted");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 7: //delete Car
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting the cars from a particular location  using id: " + arguments
                            .elementAt(1));
                    System.out.println("Car Location: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));

                        if (middleware.deleteCars(Id, location))
                            System.out.println("Cars Deleted");
                        else
                            System.out.println("Cars could not be deleted");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 8: //delete Room
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting all rooms from a particular location  using id: " + arguments
                            .elementAt(1));
                    System.out.println("Room Location: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));
                        if (middleware.deleteRooms(Id, location))
                            System.out.println("Rooms Deleted");
                        else
                            System.out.println("Rooms could not be deleted");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 9: //delete Customer
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting a customer from the database using id: " + arguments.elementAt(1));
                    System.out.println("Customer id: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        int customer = getInt(arguments.elementAt(2));
                        if (middleware.deleteCustomer(Id, customer))
                            System.out.println("Customer Deleted");
                        else
                            System.out.println("Customer could not be deleted");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 10: //querying a flight
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a flight using id: " + arguments.elementAt(1));
                    System.out.println("Flight number: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        flightNum = getInt(arguments.elementAt(2));
                        int seats = middleware.queryFlight(Id, flightNum);
                        System.out.println("Number of seats available:" + seats);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 11: //querying a Car Location
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a car location using id: " + arguments.elementAt(1));
                    System.out.println("Car location: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));
                        numCars = middleware.queryCars(Id, location);
                        System.out.println("number of Cars at this location:" + numCars);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 12: //querying a Room location
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a room location using id: " + arguments.elementAt(1));
                    System.out.println("Room location: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));
                        numRooms = middleware.queryRooms(Id, location);
                        System.out.println("number of Rooms at this location:" + numRooms);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 13: //querying Customer Information
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying Customer information using id: " + arguments.elementAt(1));
                    System.out.println("Customer id: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        int customer = getInt(arguments.elementAt(2));
                        String bill = middleware.queryCustomerInfo(Id, customer);
                        System.out.println("Customer info:" + bill);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 14: //querying a flight Price
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a flight Price using id: " + arguments.elementAt(1));
                    System.out.println("Flight number: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        flightNum = getInt(arguments.elementAt(2));
                        price = middleware.queryFlightPrice(Id, flightNum);
                        System.out.println("Price of a seat:" + price);
                    } catch (Exception e) {
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 15: //querying a Car Price
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a car price using id: " + arguments.elementAt(1));
                    System.out.println("Car location: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));
                        price = middleware.queryCarsPrice(Id, location);
                        System.out.println("Price of a car at this location:" + price);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 16: //querying a Room price
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a room price using id: " + arguments.elementAt(1));
                    System.out.println("Room Location: " + arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        location = getString(arguments.elementAt(2));
                        price = middleware.queryRoomsPrice(Id, location);
                        System.out.println("Price of Rooms at this location:" + price);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 17:  //reserve a flight
                    if (arguments.size() != 4) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving a seat on a flight using id: " + arguments.elementAt(1));
                    System.out.println("Customer id: " + arguments.elementAt(2));
                    System.out.println("Flight number: " + arguments.elementAt(3));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        int customer = getInt(arguments.elementAt(2));
                        flightNum = getInt(arguments.elementAt(3));
                        if (middleware.reserveFlight(Id, customer, flightNum))
                            System.out.println("Flight Reserved");
                        else
                            System.out.println("Flight could not be reserved.");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 18:  //reserve a car
                    if (arguments.size() != 4) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving a car at a location using id: " + arguments.elementAt(1));
                    System.out.println("Customer id: " + arguments.elementAt(2));
                    System.out.println("Location: " + arguments.elementAt(3));

                    try {
                        Id = getInt(arguments.elementAt(1));
                        int customer = getInt(arguments.elementAt(2));
                        location = getString(arguments.elementAt(3));

                        if (middleware.reserveCar(Id, customer, location))
                            System.out.println("Car Reserved");
                        else
                            System.out.println("Car could not be reserved.");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 19:  //reserve a room
                    if (arguments.size() != 4) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving a room at a location using id: " + arguments.elementAt(1));
                    System.out.println("Customer id: " + arguments.elementAt(2));
                    System.out.println("Location: " + arguments.elementAt(3));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        int customer = getInt(arguments.elementAt(2));
                        location = getString(arguments.elementAt(3));

                        if (middleware.reserveRoom(Id, customer, location))
                            System.out.println("Room Reserved");
                        else
                            System.out.println("Room could not be reserved.");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 20:  //reserve an Itinerary
                    if (arguments.size() < 7) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving an Itinerary using id:" + arguments.elementAt(1));
                    System.out.println("Customer id:" + arguments.elementAt(2));
                    for (int i = 0; i < arguments.size() - 6; i++)
                        System.out.println("Flight number" + arguments.elementAt(3 + i));
                    System.out.println("Location for Car/Room booking:" + arguments.elementAt(arguments.size() -
                            3));

                    System.out.println("Car to book?:" + arguments.elementAt(arguments.size() - 2));
                    System.out.println("Room to book?:" + arguments.elementAt(arguments.size() - 1));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        int customer = getInt(arguments.elementAt(2));
                        Vector flightNumbers = new Vector();
                        for (int i = 0; i < arguments.size() - 6; i++)
                            flightNumbers.addElement(arguments.elementAt(3 + i));
                        location = getString(arguments.elementAt(arguments.size() - 3));
                        Car = getBoolean(arguments.elementAt(arguments.size() - 2));
                        Room = getBoolean(arguments.elementAt(arguments.size() - 1));

                        if (middleware.itinerary(Id, customer, flightNumbers, location, Car, Room))
                            System.out.println("Itinerary Reserved");
                        else
                            System.out.println("Itinerary could not be reserved.");
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 21:  //quit the ClientRunner
                    if (arguments.size() != 1) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Quitting ClientRunner.");
                    System.exit(1);


                case 22:  //new Customer given id
                    if (arguments.size() != 3) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Customer using id:" + arguments.elementAt(1) + " and cid " +
                            arguments.elementAt(2));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        Cid = getInt(arguments.elementAt(2));
                        boolean customer = middleware.newCustomer(Id, Cid);
                        System.out.println("new customer id:" + Cid);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 23:  //start transaction
                    if (arguments.size() != 1) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Starting new transaction....");
                    try {
                        int transactionId = middleware.start();
                        System.out.println("New Transaction id:" + transactionId);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 24:  //commit transaction
                    if (arguments.size() != 2) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Committing transaction with id: " + arguments.elementAt(1));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        boolean committed = middleware.commit(Id);
                        if (committed) {
                            System.out.println("Transaction id:" + Id + " committed successfully.");
                        } else {
                            System.out.println("Transaction id:" + Id + " failed to commit");
                        }

                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 25:  //abort transaction
                    if (arguments.size() != 2) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Aborting transaction with id: " + arguments.elementAt(1));
                    try {
                        Id = getInt(arguments.elementAt(1));
                        middleware.abort(Id);
                        System.out.println("Abort request sent for transaction:" + Id);
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                case 26:  //shutdown all
                    if (arguments.size() != 1) {
                        wrongNumber();
                        break;
                    }
                    System.out.println("Calling shutdown of servers....");
                    try {
                        boolean shutdown = middleware.shutdown();
                        if (shutdown) {
                            System.out.println("All servers were shutdown successfully!");
                        } else {
                            System.out.println("Shutdown request failed.");
                        }
                    } catch (Exception e) {
                        System.out.print("EXCEPTION: ");
                        String exMsg = e.getMessage();
                        System.out.println(exMsg);
//                        String exMsg = e.getMessage();
//                        System.out.println(exMsg.substring(exMsg.lastIndexOf(":")));
//                        System.out.println(e.getMessage());
                    }
                    break;

                default:
                    System.out.println("The interface does not support this command.");
                    break;
            }//end of switch
        }//end of while(true)
    }

    @SuppressWarnings("unchecked")
    private Vector parse(String command) {
        Vector arguments = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument = "";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }

    private int findChoice(String argument) {
        if (argument.compareToIgnoreCase("help") == 0)
            return 1;
        else if (argument.compareToIgnoreCase("newflight") == 0)
            return 2;
        else if (argument.compareToIgnoreCase("newcar") == 0)
            return 3;
        else if (argument.compareToIgnoreCase("newroom") == 0)
            return 4;
        else if (argument.compareToIgnoreCase("newcustomer") == 0)
            return 5;
        else if (argument.compareToIgnoreCase("deleteflight") == 0)
            return 6;
        else if (argument.compareToIgnoreCase("deletecar") == 0)
            return 7;
        else if (argument.compareToIgnoreCase("deleteroom") == 0)
            return 8;
        else if (argument.compareToIgnoreCase("deletecustomer") == 0)
            return 9;
        else if (argument.compareToIgnoreCase("queryflight") == 0)
            return 10;
        else if (argument.compareToIgnoreCase("querycar") == 0)
            return 11;
        else if (argument.compareToIgnoreCase("queryroom") == 0)
            return 12;
        else if (argument.compareToIgnoreCase("querycustomer") == 0)
            return 13;
        else if (argument.compareToIgnoreCase("queryflightprice") == 0)
            return 14;
        else if (argument.compareToIgnoreCase("querycarprice") == 0)
            return 15;
        else if (argument.compareToIgnoreCase("queryroomprice") == 0)
            return 16;
        else if (argument.compareToIgnoreCase("reserveflight") == 0)
            return 17;
        else if (argument.compareToIgnoreCase("reservecar") == 0)
            return 18;
        else if (argument.compareToIgnoreCase("reserveroom") == 0)
            return 19;
        else if (argument.compareToIgnoreCase("itinerary") == 0)
            return 20;
        else if (argument.compareToIgnoreCase("quit") == 0)
            return 21;
        else if (argument.compareToIgnoreCase("newcustomerid") == 0)
            return 22;
        else if (argument.compareToIgnoreCase("start") == 0)
            return 23;
        else if (argument.compareToIgnoreCase("commit") == 0)
            return 24;
        else if (argument.compareToIgnoreCase("abort") == 0)
            return 25;
        else if (argument.compareToIgnoreCase("shutdown") == 0)
            return 26;
        else
            return 666;

    }

    private void listCommands() {
        System.out.println("\nWelcome to the ClientRunner interface provided to test your project.");
        System.out.println("Commands accepted by the interface are:");
        System.out.println("help");
        System.out.println("newflight\nnewcar\nnewroom\nnewcustomer\nnewcusomterid\ndeleteflight\ndeletecar" +
                "\ndeleteroom");
        System.out.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
        System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
        System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
        System.out.println("nquit");
        System.out.println("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
    }


    private void listSpecific(String command) {
        System.out.print("Help on: ");
        switch (findChoice(command)) {
            case 1:
                System.out.println("Help");
                System.out.println("\nTyping help on the prompt gives a list of all the commands available.");
                System.out.println("Typing help, <commandname> gives details on how to use the particular command.");
                break;

            case 2:  //new flight
                System.out.println("Adding a new Flight.");
                System.out.println("Purpose:");
                System.out.println("\tAdd information about a new flight.");
                System.out.println("\nUsage:");
                System.out.println("\tnewflight,<id>,<flightnumber>,<flightSeats>,<flightprice>");
                break;

            case 3:  //new Car
                System.out.println("Adding a new Car.");
                System.out.println("Purpose:");
                System.out.println("\tAdd information about a new car location.");
                System.out.println("\nUsage:");
                System.out.println("\tnewcar,<id>,<location>,<numberofcars>,<pricepercar>");
                break;

            case 4:  //new Room
                System.out.println("Adding a new Room.");
                System.out.println("Purpose:");
                System.out.println("\tAdd information about a new room location.");
                System.out.println("\nUsage:");
                System.out.println("\tnewroom,<id>,<location>,<numberofrooms>,<priceperroom>");
                break;

            case 5:  //new Customer
                System.out.println("Adding a new Customer.");
                System.out.println("Purpose:");
                System.out.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
                System.out.println("\nUsage:");
                System.out.println("\tnewcustomer,<id>");
                break;


            case 6: //delete Flight
                System.out.println("Deleting a flight");
                System.out.println("Purpose:");
                System.out.println("\tDelete a flight's information.");
                System.out.println("\nUsage:");
                System.out.println("\tdeleteflight,<id>,<flightnumber>");
                break;

            case 7: //delete Car
                System.out.println("Deleting a Car");
                System.out.println("Purpose:");
                System.out.println("\tDelete all cars from a location.");
                System.out.println("\nUsage:");
                System.out.println("\tdeletecar,<id>,<location>,<numCars>");
                break;

            case 8: //delete Room
                System.out.println("Deleting a Room");
                System.out.println("\nPurpose:");
                System.out.println("\tDelete all rooms from a location.");
                System.out.println("Usage:");
                System.out.println("\tdeleteroom,<id>,<location>,<numRooms>");
                break;

            case 9: //delete Customer
                System.out.println("Deleting a Customer");
                System.out.println("Purpose:");
                System.out.println("\tRemove a customer from the database.");
                System.out.println("\nUsage:");
                System.out.println("\tdeletecustomer,<id>,<customerid>");
                break;

            case 10: //querying a flight
                System.out.println("Querying flight.");
                System.out.println("Purpose:");
                System.out.println("\tObtain Seat information about a certain flight.");
                System.out.println("\nUsage:");
                System.out.println("\tqueryflight,<id>,<flightnumber>");
                break;

            case 11: //querying a Car Location
                System.out.println("Querying a Car location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain number of cars at a certain car location.");
                System.out.println("\nUsage:");
                System.out.println("\tquerycar,<id>,<location>");
                break;

            case 12: //querying a Room location
                System.out.println("Querying a Room Location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain number of rooms at a certain room location.");
                System.out.println("\nUsage:");
                System.out.println("\tqueryroom,<id>,<location>");
                break;

            case 13: //querying Customer Information
                System.out.println("Querying Customer Information.");
                System.out.println("Purpose:");
                System.out.println("\tObtain information about a customer.");
                System.out.println("\nUsage:");
                System.out.println("\tquerycustomer,<id>,<customerid>");
                break;

            case 14: //querying a flight for price
                System.out.println("Querying flight.");
                System.out.println("Purpose:");
                System.out.println("\tObtain price information about a certain flight.");
                System.out.println("\nUsage:");
                System.out.println("\tqueryflightprice,<id>,<flightnumber>");
                break;

            case 15: //querying a Car Location for price
                System.out.println("Querying a Car location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain price information about a certain car location.");
                System.out.println("\nUsage:");
                System.out.println("\tquerycarprice,<id>,<location>");
                break;

            case 16: //querying a Room location for price
                System.out.println("Querying a Room Location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain price information about a certain room location.");
                System.out.println("\nUsage:");
                System.out.println("\tqueryroomprice,<id>,<location>");
                break;

            case 17:  //reserve a flight
                System.out.println("Reserving a flight.");
                System.out.println("Purpose:");
                System.out.println("\tReserve a flight for a customer.");
                System.out.println("\nUsage:");
                System.out.println("\treserveflight,<id>,<customerid>,<flightnumber>");
                break;

            case 18:  //reserve a car
                System.out.println("Reserving a Car.");
                System.out.println("Purpose:");
                System.out.println("\tReserve a given number of cars for a customer at a particular location.");
                System.out.println("\nUsage:");
                System.out.println("\treservecar,<id>,<customerid>,<location>,<nummberofCars>");
                break;

            case 19:  //reserve a room
                System.out.println("Reserving a Room.");
                System.out.println("Purpose:");
                System.out.println("\tReserve a given number of rooms for a customer at a particular location.");
                System.out.println("\nUsage:");
                System.out.println("\treserveroom,<id>,<customerid>,<location>,<nummberofRooms>");
                break;

            case 20:  //reserve an Itinerary
                System.out.println("Reserving an Itinerary.");
                System.out.println("Purpose:");
                System.out.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
                System.out.println("\nUsage:");
                System.out.println("\titinerary,<id>,<customerid>,<flightnumber1>....<flightnumberN>," +
                        "<LocationToBookCarsOrRooms>,<NumberOfCars>,<NumberOfRoom>");
                break;


            case 21:  //quit the ClientRunner
                System.out.println("Quitting ClientRunner.");
                System.out.println("Purpose:");
                System.out.println("\tExit the ClientRunner application.");
                System.out.println("\nUsage:");
                System.out.println("\tquit");
                break;

            case 22:  //new customer with id
                System.out.println("Create new customer providing an id");
                System.out.println("Purpose:");
                System.out.println("\tCreates a new customer with the id provided");
                System.out.println("\nUsage:");
                System.out.println("\tnewcustomerid, <id>, <customerid>");
                break;

            default:
                System.out.println(command);
                System.out.println("The interface does not support this command.");
                break;
        }
    }

    private void wrongNumber() {
        System.out.println("The number of arguments provided in this command are wrong.");
        System.out.println("Type help, <commandname> to check usage of this command.");
    }


    private int getInt(Object temp) throws Exception {
        return new Integer((String) temp);
    }

    private boolean getBoolean(Object temp) throws Exception {
        return Boolean.valueOf((String) temp);
    }

    private String getString(Object temp) throws Exception {
        return (String) temp;
    }


}
