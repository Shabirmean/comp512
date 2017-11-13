import MiddlewareInterface.Middleware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by shabirmean on 2017-11-13 with some hope.
 */
@SuppressWarnings("Duplicates")
public class ClientManager {
    static Middleware rm = null;
    static ArrayList<String> locations = new ArrayList<>();

    public static void main(String[] args) {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        Scanner scanner = new Scanner(System.in);

//        Client obj = new Client();
        Vector arguments;
        int flightNum, flightPrice, flightSeats;
        int price, numRooms, numCars;
        int Id, Cid;
        boolean Room, Car;
        String location;
        String command = "";
        String message = "blank";
        String server = "localhost";
        int port = 1099;

        if (args.length > 0) {
            server = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            System.out.println("Usage: java Client [rmihost [rmiport]]");
            System.exit(1);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(server, port);
            rm = (Middleware) registry.lookup("ShabirJianMiddleware");
            if (rm != null) {
                System.out.println("Successful");
                System.out.println("Connected to Middleware");
            } else {
                System.out.println("Unsuccessful");
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }


        if (System.getSecurityManager() == null) {
            //System.setSecurityManager(new RMISecurityManager());
        }

        generateLocationList();
        listTransactionTypes();
        int testType = scanner.nextInt();
        int rmType;
        int loopCount;

        switch (testType) {
            case 1:
                rmType = ThreadLocalRandom.current().nextInt(0, 4);
                System.out.println("Randomly chosen RM for test: " + ResourceManagerType.getCodeString(rmType));
                System.out.println("Adding some random values to be conduct write operations.");
                addRandomResources(rmType);

                System.out.print("How many loops to do READ for: ");
                loopCount = scanner.nextInt();
                randomReadFromRM(rmType, loopCount);


                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 0:
                break;
            default:
                System.out.println("Invalid test type.");
                System.exit(0);
                break;
        }


    }

    private static void listTransactionTypes() {
        System.out.println("\nSelect the transaction type you would like to test: ");
        System.out.print(
                "[1] - Read on one RM\n" +
                        "[2] - Read on multiple RM\n" +
                        "[3] - Write on one RM\n" +
                        "[4] - Write on multiple RM\n" +
                        "[5] - R/W on one RM\n" +
                        "[6] - R/W on multiple RM\n" +
                        "[0] - quit\n\n>");
    }


    static void randomReadFromRM(int rmType, int loopCount) {
        int locSize = locations.size();
        long lStartTime, lEndTime, respTime;
        int averageReadTime = 0;
        try {
            switch (rmType) {
                case 0:
                    for (int start = 0; start < loopCount; start++) {
                        int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);

                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryCars(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            System.out.println("Car: " + location + " returned count " + count);
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryCarsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            System.out.println("Car: " + location + " returned price " + count);
                        }
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        System.out.println("");
                    }
                    break;
                case 1:
                    for (int start = 0; start < loopCount; start++) {
                        int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);

                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryRooms(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            System.out.println("Hotel: " + location + " returned count " + count);
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryRoomsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            System.out.println("Hotel: " + location + " returned price " + count);
                        }
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        System.out.println("");
                    }
                    break;
                case 2:
                    for (int start = 0; start < loopCount; start++) {
                        int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);
                        int locHashCode = location.hashCode();

                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryFlight(tId, locHashCode);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            System.out.println("Flight: " + locHashCode + " returned count " + count);
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryFlightPrice(tId, locHashCode);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            System.out.println("Flight: " + locHashCode + " returned price " + count);
                        }
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        System.out.println("");
                    }
                    break;
                case 3:
                    for (int start = 0; start < loopCount; start++) {
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);
                        int locHashCode = location.hashCode();

                        lStartTime = System.nanoTime();
                        int tId = rm.start();
                        String info = rm.queryCustomerInfo(tId, locHashCode);
                        rm.commit(tId);
                        lEndTime = System.nanoTime();
                        respTime = lEndTime - lStartTime;

                        System.out.println("Customer: " + locHashCode + " info " + info);
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        System.out.println("");
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static void addRandomResources(int rmType) {
        try {
            switch (rmType) {
                case 0: {
                    int tId = rm.start();
                    for (String location : locations) {
                        int count = ThreadLocalRandom.current().nextInt(1, 100);
                        int price = ThreadLocalRandom.current().nextInt(100, 1000);
                        rm.addCars(tId, location, count, price);
                        System.out.println("Added Car: " + location + " - #" + count + " - $" + price);
                    }
                    rm.commit(tId);
                    break;
                }
                case 1: {
                    int tId = rm.start();
                    for (String location : locations) {
                        int count = ThreadLocalRandom.current().nextInt(1, 100);
                        int price = ThreadLocalRandom.current().nextInt(100, 1000);
                        rm.addRooms(tId, location, count, price);
                        System.out.println("Added Hotel: " + location + " - #" + count + " - $" + price);
                    }
                    rm.commit(tId);
                    break;
                }
                case 2: {
                    int tId = rm.start();
                    for (String location : locations) {
                        int count = ThreadLocalRandom.current().nextInt(1, 100);
                        int price = ThreadLocalRandom.current().nextInt(100, 1000);
                        rm.addFlight(tId, location.hashCode(), count, price);
                        System.out.println("Added Flight: " + location.hashCode() + " - #" + count + " - $" + price);
                    }
                    rm.commit(tId);
                    break;
                }
                case 3: {
                    int tId = rm.start();
                    for (String location : locations) {
                        rm.newCustomer(tId, location.hashCode());
                        System.out.println("Added Customer: " + location.hashCode());
                    }
                    rm.commit(tId);
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void generateLocationList() {
        locations.add("montreal");
        locations.add("toronto");
        locations.add("vancouver");
        locations.add("sanfransico");
        locations.add("paloalto");
        locations.add("colombo");
        locations.add("kandy");
        locations.add("shanghai");
        locations.add("melbourne");
        locations.add("london");
        locations.add("sydney");
        locations.add("venice");
        locations.add("newyork");
        locations.add("canbarra");
        locations.add("delhi");
        locations.add("frankfurt");
    }

    public enum ResourceManagerType {
        CAR(0),
        HOTEL(1),
        FLIGHT(2),
        CUSTOMER(3);

        private final int rmCode;
        private String rmCodeString;

        ResourceManagerType(int rmCode) {
            this.rmCode = rmCode;
            switch (this.rmCode) {
                case 0:
                    this.rmCodeString = "CAR";
                    break;
                case 1:
                    this.rmCodeString = "HOTEL";
                    break;
                case 2:
                    this.rmCodeString = "FLIGHT";
                    break;
                case 3:
                    this.rmCodeString = "CUSTOMER";
                    break;
            }

        }

        public int getRMCode() {
            return this.rmCode;
        }

        public String getCodeString() {
            return this.rmCodeString;
        }

        public static String getCodeString(int itemCode) {
            switch (itemCode) {
                case 0:
                    return "CAR";
                case 1:
                    return "HOTEL";
                case 2:
                    return "FLIGHT";
                case 3:
                    return "CUSTOMER";
            }
            return null;
        }

    }


}
