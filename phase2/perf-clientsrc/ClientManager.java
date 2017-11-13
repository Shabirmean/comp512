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
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));


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
        int checkSec = 1;
        long secToMicro = 1000000;
        long avRTime = 0;

        avRTime = getAverageResponseTime(rmType);
        System.out.println("#### Average Time: " + avRTime);

        System.out.println("Load (# of Transaction per second: ): ");
        int load = scanner.nextInt();

        long microPerT = secToMicro / load;
        System.out.println("#### Time Per Transaction (micro-seconds): " + microPerT);

//        long avgBuffer = avRTime / 10;
//        System.out.println("#### Average Buffer: " + avgBuffer);
//
//        avgBuffer += avRTime;
//        System.out.println("#### Average Buffer + Time: " + avgBuffer);
//
//        int numOfTransactions = (int) ((checkSec * secToMicro) / avgBuffer);
//        System.out.println("#### Num of Transactions: " + numOfTransactions);
//
//        while (numOfTransactions <= 0) {
//            numOfTransactions = (int) ((checkSec * secToMicro) / avgBuffer);
//            checkSec++;
//        }
//
//        long balanceSec = (checkSec * secToMicro) - (numOfTransactions * avgBuffer);
//        long balancePerT = (int) (balanceSec / numOfTransactions);
//        System.out.println("#### balanceSec: " + balanceSec);
//        System.out.println("#### balancePerT: " + balancePerT);
//
//        long timePerT = avgBuffer + balancePerT;
//        System.out.println("#### Time Per Transaction: " + timePerT);
//        System.out.println("#### No of Transaction: " + numOfTransactions + " per " + checkSec + "-seconds");

        try {
            switch (rmType) {
                case 0:
                    //get average response time first
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
//                            System.out.println("Car: " + location + " returned count " + count);
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryCarsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
//                            System.out.println("Car: " + location + " returned price " + count);
                        }
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        long respTInMS = respTime / 1000;
//                        System.out.println("Elapsed time in microseconds: " + respTInMS);
//                        System.out.println("");
//                        System.out.println("Iter: " + start + " - " + respTInMS + "ms.");
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
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
//                            System.out.println("Hotel: " + location + " returned count " + count);
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryRoomsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
//                            System.out.println("Hotel: " + location + " returned price " + count);
                        }
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        long respTInMS = respTime / 1000;
//                        System.out.println("Elapsed time in microseconds: " + respTInMS);
//                        System.out.println("");
//                        System.out.println("Iter: " + start + " - " + respTInMS + "ms.");
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
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
//                            System.out.println("Flight: " + locHashCode + " returned count " + count);
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryFlightPrice(tId, locHashCode);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
//                            System.out.println("Flight: " + locHashCode + " returned price " + count);
                        }
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        long respTInMS = respTime / 1000;
//                        System.out.println("Elapsed time in microseconds: " + respTInMS);
//                        System.out.println("");
//                        System.out.println("Iter: " + start + " - " + respTInMS + "ms.");
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
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

//                        System.out.println("Customer: " + locHashCode + " info " + info);
//                        System.out.println("Elapsed time in milliseconds: " + respTime / 1000000);
                        long respTInMS = respTime / 1000;
//                        System.out.println("Elapsed time in microseconds: " + respTInMS);
//                        System.out.println("");
//                        System.out.println("Iter: " + start + " - " + respTInMS + "micro-sec.");
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void waitBeforeNextT(long interval) {
//        final long INTERVAL = 100;
        long start = System.nanoTime();
        long end = 0;
        do {
            end = System.nanoTime();
        } while (start + (interval * 1000) >= end);
//        System.out.println(end - start);
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


    private static long getAverageResponseTime(int rmType) {
        int locSize = locations.size();
        long lStartTime, lEndTime, respTime;
        long avRTime = 0;
        int avCLoops = 100;
        try {
            switch (rmType) {
                case 0:
                    //get average response time first
                    for (int start = 0; start < avCLoops; start++) {
                        int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);

                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.queryCars(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.queryCarsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        avRTime += (respTime / 1000);
                    }
                    break;
                case 1:
                    for (int start = 0; start < avCLoops; start++) {
                        int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);

                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.queryRooms(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.queryRoomsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        avRTime += (respTime / 1000);
                    }
                    break;
                case 2:
                    for (int start = 0; start < avCLoops; start++) {
                        int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);
                        int locHashCode = location.hashCode();

                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.queryFlight(tId, locHashCode);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryFlightPrice(tId, locHashCode);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        avRTime += (respTime / 1000);
                    }
                    break;
                case 3:
                    for (int start = 0; start < avCLoops; start++) {
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);
                        int locHashCode = location.hashCode();

                        lStartTime = System.nanoTime();
                        int tId = rm.start();
                        rm.queryCustomerInfo(tId, locHashCode);
                        rm.commit(tId);
                        lEndTime = System.nanoTime();
                        respTime = lEndTime - lStartTime;
                        System.out.println("Elapsed time in microseconds: " + respTime / 1000);
                        avRTime += (respTime / 1000);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        avRTime = (avRTime / avCLoops);
        System.out.println("#### Average response time on " +
                avCLoops + " loops is " + avRTime + " micro-secs");
        return avRTime;
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
