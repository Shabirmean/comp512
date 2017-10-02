package socs.distributed.client;

import org.apache.log4j.Logger;
import socs.distributed.client.exception.ClientException;
import socs.distributed.resource.message.MsgType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;


public class Client {
    private static final Logger log = Logger.getLogger(Client.class);

    public static void main(String args[]) {
        Client obj = new Client();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        Vector arguments;

        String clientId = "";
        String middlewareIP = "localhost";
        short middlewarePort = 1099;

        if (args.length > 0) {
            middlewareIP = args[0];
        }
        if (args.length > 1) {
            middlewarePort = Short.parseShort(args[1]);
        }

        if (args.length > 2) {
            clientId = args[2];
        }

        if (args.length > 3) {
            log.info("Usage: java src.java.client [MiddlewareHost [MiddlewarePort]]");
            System.exit(1);
        }

        ClientRequestHandler clientRequestHandler = new ClientRequestHandler(clientId, middlewareIP, middlewarePort);
        log.info("\n\n\tClient [" + clientId + "] Interface connecting to " + middlewareIP + ":" + middlewarePort);
        log.info("Type \"help\" for list of supported commands");

        // TODO:: Remove this debugging stuff
        if (clientId.equals("cs-25")) {
            log.info("============================================================");
            log.info("This client would sleep at the MIDDLEWARE SERVER for 25 secs");
            log.info("============================================================");
        }

        if (clientId.equals("cs-22")) {
            log.info("===========================================================");
            log.info("This client would sleep at the RESOURCE-MANAGER for 25 secs");
            log.info("===========================================================");
        }

        while (true) {
            System.out.print("\n>");
            try {
                //read the next command
                command = stdin.readLine();
            } catch (IOException io) {
                log.info("Unable to read from standard in");
                System.exit(1);
            }
            //remove heading and trailing white space
            command = command.trim();
            arguments = obj.parse(command);

            try {
                //decide which of the commands this was
                switch (obj.findChoice((String) arguments.elementAt(0))) {
                    case 1: //help section
                        if (arguments.size() == 1)   //command was "help"
                            obj.listCommands();
                        else if (arguments.size() == 2)  //command was "help <commandname>"
                            obj.listSpecific((String) arguments.elementAt(1));
                        else  //wrong use of help command
                            log.info("Improper use of help command. Type help or help, <commandname>");
                        break;

                    case 2:  //new flight
                        if (arguments.size() != 5) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Adding a new Flight using id: " + arguments.elementAt(1));
                        log.info("Flight number: " + arguments.elementAt(2));
                        log.info("Add Flight Seats: " + arguments.elementAt(3));
                        log.info("Set Flight Price: " + arguments.elementAt(4));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.ADD_FLIGHT, arguments);
                        break;

                    case 3:  //new Car
                        if (arguments.size() != 5) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Adding a new Car using id: " + arguments.elementAt(1));
                        log.info("Car Location: " + arguments.elementAt(2));
                        log.info("Add Number of Cars: " + arguments.elementAt(3));
                        log.info("Set Price: " + arguments.elementAt(4));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.ADD_CARS, arguments);
                        break;

                    case 4:  //new Room
                        if (arguments.size() != 5) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Adding a new Room using id: " + arguments.elementAt(1));
                        log.info("Room Location: " + arguments.elementAt(2));
                        log.info("Add Number of Rooms: " + arguments.elementAt(3));
                        log.info("Set Price: " + arguments.elementAt(4));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.ADD_ROOMS, arguments);
                        break;

                    case 5:  //new Customer
                        if (arguments.size() != 2) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Adding a new Customer using id:" + arguments.elementAt(1));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.ADD_NEW_CUSTOMER_WITHOUT_ID, arguments);
                        break;

                    case 6: //delete Flight
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Deleting a flight using id: " + arguments.elementAt(1));
                        log.info("Flight Number: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.DELETE_FLIGHT, arguments);
                        break;

                    case 7: //delete Car
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Deleting the cars from a particular location  using id: " + arguments.elementAt(1));
                        log.info("Car Location: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.DELETE_CARS, arguments);
                        break;

                    case 8: //delete Room
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Deleting all rooms from a particular location  using id: " + arguments.elementAt(1));
                        log.info("Room Location: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.DELETE_ROOMS, arguments);
                        break;

                    case 9: //delete Customer
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Deleting a customer from the database using id: " + arguments.elementAt(1));
                        log.info("Customer id: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.DELETE_CUSTOMER, arguments);
                        break;

                    case 10: //querying a flight
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Querying a flight using id: " + arguments.elementAt(1));
                        log.info("Flight number: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.QUERY_FLIGHT, arguments);
                        break;

                    case 11: //querying a Car Location
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Querying a car location using id: " + arguments.elementAt(1));
                        log.info("Car location: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.QUERY_CARS, arguments);
                        break;

                    case 12: //querying a Room location
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Querying a room location using id: " + arguments.elementAt(1));
                        log.info("Room location: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.QUERY_ROOMS, arguments);
                        break;

                    case 13: //querying Customer Information
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Querying Customer information using id: " + arguments.elementAt(1));
                        log.info("Customer id: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.QUERY_CUSTOMER_INFO, arguments);
                        break;

                    case 14: //querying a flight Price
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Querying a flight Price using id: " + arguments.elementAt(1));
                        log.info("Flight number: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.QUERY_FLIGHT_PRICE, arguments);
                        break;

                    case 15: //querying a Car Price
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Querying a car price using id: " + arguments.elementAt(1));
                        log.info("Car location: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.QUERY_CAR_PRICE, arguments);
                        break;

                    case 16: //querying a Room price
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Querying a room price using id: " + arguments.elementAt(1));
                        log.info("Room Location: " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.QUERY_ROOM_PRICE, arguments);
                        break;

                    case 17:  //reserve a flight
                        if (arguments.size() != 4) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Reserving a seat on a flight using id: " + arguments.elementAt(1));
                        log.info("Customer id: " + arguments.elementAt(2));
                        log.info("Flight number: " + arguments.elementAt(3));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.RESERVE_FLIGHT, arguments);
                        break;

                    case 18:  //reserve a car
                        if (arguments.size() != 4) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Reserving a car at a location using id: " + arguments.elementAt(1));
                        log.info("Customer id: " + arguments.elementAt(2));
                        log.info("Location: " + arguments.elementAt(3));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.RESERVE_CAR, arguments);
                        break;

                    case 19:  //reserve a room
                        if (arguments.size() != 4) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Reserving a room at a location using id: " + arguments.elementAt(1));
                        log.info("Customer id: " + arguments.elementAt(2));
                        log.info("Location: " + arguments.elementAt(3));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.RESERVE_ROOM, arguments);
                        break;

                    case 20:  //reserve an Itinerary
                        if (arguments.size() < 7) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Reserving an Itinerary using id:" + arguments.elementAt(1));
                        log.info("Customer id:" + arguments.elementAt(2));
                        for (int i = 0; i < arguments.size() - 6; i++)
                            log.info("Flight number" + arguments.elementAt(3 + i));
                        log.info("Location for Car/Room booking:" + arguments.elementAt(arguments.size() - 3));
                        log.info("Car to book?:" + arguments.elementAt(arguments.size() - 2));
                        log.info("Room to book?:" + arguments.elementAt(arguments.size() - 1));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.RESERVE_ITINERARY, arguments);
                        break;

                    case 21:  //quit the src.java.client
                        if (arguments.size() != 1) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Quitting src.java.client.");
                        System.exit(1);


                    case 22:  //new Customer given id
                        if (arguments.size() != 3) {
                            obj.wrongNumber();
                            break;
                        }
                        log.info("Adding a new Customer using id:" +
                                arguments.elementAt(1) + " and cid " + arguments.elementAt(2));
                        clientRequestHandler.sendRequestToMiddleware(MsgType.ADD_NEW_CUSTOMER_WITH_ID, arguments);
                        break;

                    default:
                        log.info("The interface does not support this command.");
                        break;
                }//end of switch

            } catch (ClientException e) {
                e.printStackTrace();
            }
        }//end of while(true)
    }

    @SuppressWarnings("unchecked")
    private Vector parse(String command) {
        Vector arguments = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument;
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
        else
            return 666;

    }

    private void listCommands() {
        log.info("\nWelcome to the src.java.client interface provided to test your project.");
        log.info("Commands accepted by the interface are:");
        log.info("help");
        log.info("newflight\nnewcar\nnewroom\nnewcustomer\nnewcusomterid\ndeleteflight\ndeletecar" +
                "\ndeleteroom");
        log.info("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
        log.info("queryflightprice\nquerycarprice\nqueryroomprice");
        log.info("reserveflight\nreservecar\nreserveroom\nitinerary");
        log.info("nquit");
        log.info("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
    }


    private void listSpecific(String command) {
        System.out.print("Help on: ");
        switch (findChoice(command)) {
            case 1:
                log.info("Help");
                log.info("\nTyping help on the prompt gives a list of all the commands available.");
                log.info("Typing help, <commandname> gives details on how to use the particular command.");
                break;

            case 2:  //new flight
                log.info("Adding a new Flight.");
                log.info("Purpose:");
                log.info("\tAdd information about a new flight.");
                log.info("\nUsage:");
                log.info("\tnewflight,<id>,<flightnumber>,<flightSeats>,<flightprice>");
                break;

            case 3:  //new Car
                log.info("Adding a new Car.");
                log.info("Purpose:");
                log.info("\tAdd information about a new car location.");
                log.info("\nUsage:");
                log.info("\tnewcar,<id>,<location>,<numberofcars>,<pricepercar>");
                break;

            case 4:  //new Room
                log.info("Adding a new Room.");
                log.info("Purpose:");
                log.info("\tAdd information about a new room location.");
                log.info("\nUsage:");
                log.info("\tnewroom,<id>,<location>,<numberofrooms>,<priceperroom>");
                break;

            case 5:  //new Customer
                log.info("Adding a new Customer.");
                log.info("Purpose:");
                log.info("\tGet the system to provide a new customer id. (same as adding a new customer)");
                log.info("\nUsage:");
                log.info("\tnewcustomer,<id>");
                break;


            case 6: //delete Flight
                log.info("Deleting a flight");
                log.info("Purpose:");
                log.info("\tDelete a flight's information.");
                log.info("\nUsage:");
                log.info("\tdeleteflight,<id>,<flightnumber>");
                break;

            case 7: //delete Car
                log.info("Deleting a Car");
                log.info("Purpose:");
                log.info("\tDelete all cars from a location.");
                log.info("\nUsage:");
                log.info("\tdeletecar,<id>,<location>,<numCars>");
                break;

            case 8: //delete Room
                log.info("Deleting a Room");
                log.info("\nPurpose:");
                log.info("\tDelete all rooms from a location.");
                log.info("Usage:");
                log.info("\tdeleteroom,<id>,<location>,<numRooms>");
                break;

            case 9: //delete Customer
                log.info("Deleting a Customer");
                log.info("Purpose:");
                log.info("\tRemove a customer from the database.");
                log.info("\nUsage:");
                log.info("\tdeletecustomer,<id>,<customerid>");
                break;

            case 10: //querying a flight
                log.info("Querying flight.");
                log.info("Purpose:");
                log.info("\tObtain Seat information about a certain flight.");
                log.info("\nUsage:");
                log.info("\tqueryflight,<id>,<flightnumber>");
                break;

            case 11: //querying a Car Location
                log.info("Querying a Car location.");
                log.info("Purpose:");
                log.info("\tObtain number of cars at a certain car location.");
                log.info("\nUsage:");
                log.info("\tquerycar,<id>,<location>");
                break;

            case 12: //querying a Room location
                log.info("Querying a Room Location.");
                log.info("Purpose:");
                log.info("\tObtain number of rooms at a certain room location.");
                log.info("\nUsage:");
                log.info("\tqueryroom,<id>,<location>");
                break;

            case 13: //querying Customer Information
                log.info("Querying Customer Information.");
                log.info("Purpose:");
                log.info("\tObtain information about a customer.");
                log.info("\nUsage:");
                log.info("\tquerycustomer,<id>,<customerid>");
                break;

            case 14: //querying a flight for price
                log.info("Querying flight.");
                log.info("Purpose:");
                log.info("\tObtain price information about a certain flight.");
                log.info("\nUsage:");
                log.info("\tqueryflightprice,<id>,<flightnumber>");
                break;

            case 15: //querying a Car Location for price
                log.info("Querying a Car location.");
                log.info("Purpose:");
                log.info("\tObtain price information about a certain car location.");
                log.info("\nUsage:");
                log.info("\tquerycarprice,<id>,<location>");
                break;

            case 16: //querying a Room location for price
                log.info("Querying a Room Location.");
                log.info("Purpose:");
                log.info("\tObtain price information about a certain room location.");
                log.info("\nUsage:");
                log.info("\tqueryroomprice,<id>,<location>");
                break;

            case 17:  //reserve a flight
                log.info("Reserving a flight.");
                log.info("Purpose:");
                log.info("\tReserve a flight for a customer.");
                log.info("\nUsage:");
                log.info("\treserveflight,<id>,<customerid>,<flightnumber>");
                break;

            case 18:  //reserve a car
                log.info("Reserving a Car.");
                log.info("Purpose:");
                log.info("\tReserve a given number of cars for a customer at a particular location.");
                log.info("\nUsage:");
                log.info("\treservecar,<id>,<customerid>,<location>,<nummberofCars>");
                break;

            case 19:  //reserve a room
                log.info("Reserving a Room.");
                log.info("Purpose:");
                log.info("\tReserve a given number of rooms for a customer at a particular location.");
                log.info("\nUsage:");
                log.info("\treserveroom,<id>,<customerid>,<location>,<nummberofRooms>");
                break;

            case 20:  //reserve an Itinerary
                log.info("Reserving an Itinerary.");
                log.info("Purpose:");
                log.info("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
                log.info("\nUsage:");
                log.info("\titinerary,<id>,<customerid>,<flightnumber1>....<flightnumberN>," +
                        "<LocationToBookCarsOrRooms>,<NumberOfCars>,<NumberOfRoom>");
                break;


            case 21:  //quit the src.java.client
                log.info("Quitting src.java.client.");
                log.info("Purpose:");
                log.info("\tExit the src.java.client application.");
                log.info("\nUsage:");
                log.info("\tquit");
                break;

            case 22:  //new customer with id
                log.info("Create new customer providing an id");
                log.info("Purpose:");
                log.info("\tCreates a new customer with the id provided");
                log.info("\nUsage:");
                log.info("\tnewcustomerid, <id>, <customerid>");
                break;

            default:
                log.info(command);
                log.info("The interface does not support this command.");
                break;
        }
    }

    private void wrongNumber() {
        log.info("The number of arguments provided in this command are wrong.");
        log.info("Type help, <commandname> to check usage of this command.");
    }
}