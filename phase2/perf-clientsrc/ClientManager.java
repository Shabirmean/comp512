import MiddlewareInterface.Middleware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by shabirmean on 2017-11-13 with some hope.
 */
@SuppressWarnings("Duplicates")
public class ClientManager {
    private static Middleware rm = null;
    private static ArrayList<String> locations = new ArrayList<>();
    private static RandomString rgen = new RandomString(8, ThreadLocalRandom.current());
    //    static Scanner scanner = new Scanner(System.in);
    private static int testType;
    private static int loopCount;
    private static int load;
    private static boolean distributed = false;
    private static int clients;

    private static long averageTime = 0;
    private static final Object Lock = new Object();


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
//        if (args.length > 1) {
//            port = Integer.parseInt(args[1]);
//        }
        if (args.length > 2) {
            testType = Integer.parseInt(args[1]);
            loopCount = Integer.parseInt(args[2]);
            load = Integer.parseInt(args[3]);
//            System.out.println("Usage: java Client [rmihost [rmiport]]");
//            System.exit(1);
            distributed = Boolean.parseBoolean(args[4]);
            clients = Integer.parseInt(args[5]);
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
        int rmType;


        if (distributed) {
            ConcurrentHashMap<Integer, Long> clientAverages = new ConcurrentHashMap<>();

            for (int rmO = 0; rmO < 4; rmO++) {
                addRandomResources(rmO);
            }

            ArrayList<Timer> clientTimers = new ArrayList<Timer>();
            for (int count = 0; count < clients; count++) {
                Timer transactionTimer = new Timer();
                clientTimers.add(transactionTimer);
            }

            int clientCounter = 0;
            long millisPerT = 1000 / load;
            int interval = clients / load;
            long iterIntervalMS = interval * 1000 * 1000;
            long allIters = 0;

            for (Timer timer : clientTimers) {
//                long delay = (clientCounter * 1000) / millisPerT;
                long delay = ((interval * 1000) / clients) * clientCounter;
                int finalClientCounter = clientCounter;
//                timer.scheduleAtFixedRate(new TimerTask() {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        long clientAverage = 0;
                        for (int tCount = 0; tCount < loopCount; tCount++) {
                                long timeTaken = randomReadAndWriteFromMultipleRM();
                                long sleepTime = iterIntervalMS - timeTaken;
                                if (sleepTime > 0) {
                                    waitBeforeNextT(sleepTime);
                                }
                                clientAverage += timeTaken;
                        }
                        clientAverage /= loopCount;
                        System.out.println("Client-" + finalClientCounter + " had average of " +
                                clientAverage + "micro-secs");
                        clientAverages.put(finalClientCounter, clientAverage);
                    }
                }, delay);
                clientCounter++;
            }


            while(clientAverages.size() != clients){

            }

            long totalAverage = 0;
            for (Long cavrg : clientAverages.values()){
                totalAverage += cavrg;
            }
            totalAverage /= clients;
            System.out.println("Total Average (micro-seconds): " + totalAverage + " for " + clients +
                    " at load " + load + " per second");
        } else {
            switch (testType) {
                case 1:
                    rmType = ThreadLocalRandom.current().nextInt(0, 4);
                    System.out.println("Randomly chosen RM for test: " + ResourceManagerType.getCodeString(rmType));
                    System.out.println("Adding some random values to be conduct read operations.");
                    addRandomResources(rmType);
                    randomReadFromRM(rmType, loopCount);
                    break;
                case 2:
                    System.out.println("Adding some random values to be conduct read operations.");
                    for (int rmO = 0; rmO < 4; rmO++) {
                        addRandomResources(rmO);
                    }
                    randomReadFromMultipleRM(loopCount);
                    break;
                case 3:
                    // Customer Ommitted
                    rmType = ThreadLocalRandom.current().nextInt(0, 3);
                    System.out.println("Randomly chosen RM for test: " + ResourceManagerType.getCodeString(rmType));
                    for (int rmO = 0; rmO < 4; rmO++) {
                        addRandomResources(rmO);
                    }
                    randomWriteToRM(rmType, loopCount);
                    break;
                case 4:
                    for (int rmO = 0; rmO < 4; rmO++) {
                        addRandomResources(rmO);
                    }
                    randomWriteToMultipleRMs(loopCount);
                    break;
                case 5:
                    rmType = ThreadLocalRandom.current().nextInt(0, 3);
                    System.out.println("Randomly chosen RM for test: " + ResourceManagerType.getCodeString(rmType));
                    for (int rmO = 0; rmO < 4; rmO++) {
                        addRandomResources(rmO);
                    }
                    randomReadAndWriteFromRM(rmType, loopCount);
                    break;
                case 6:
                    for (int rmO = 0; rmO < 4; rmO++) {
                        addRandomResources(rmO);
                    }
                    randomReadAndWriteFromMultipleRM(loopCount);
                    break;
                default:
                    System.out.println("Invalid test type.");
                    System.exit(0);
                    break;
            }
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


    static void randomReadFromMultipleRM(int loopCount) {
        int locSize = locations.size();
        long lStartTime, lEndTime, respTime = 0;
        long secToMicro = 1000000;

        long microPerT = secToMicro / load;
        System.out.println("#### Time Per Transaction (micro-seconds): " + microPerT);

        long averageT4Load = 0;
        int start;

        for (start = 0; start < loopCount; start++) {
            int rmType = ThreadLocalRandom.current().nextInt(0, 4);
            int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
            String location = locations.get(readLocIndex);
            int locHashCode = location.hashCode();

            long respTInMS = 0;
            try {
                switch (rmType) {
                    case 0: {
                        //get average response time first
                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryCars(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryCarsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        break;
                    }
                    case 1: {
                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryRooms(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryRoomsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        break;
                    }
                    case 2: {
                        if (priceOCount == 0) {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryFlight(tId, locHashCode);
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
                        break;
                    }
                    case 3: {
                        lStartTime = System.nanoTime();
                        int tId = rm.start();
                        String info = rm.queryCustomerInfo(tId, locHashCode);
                        rm.commit(tId);
                        lEndTime = System.nanoTime();
                        respTime = lEndTime - lStartTime;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            respTInMS = respTime / 1000;
            averageT4Load += respTInMS;
            System.out.println(start + "," + respTInMS);  // in microseconds
            long sleepTime = microPerT - respTInMS;
            if (sleepTime > 0) {
                waitBeforeNextT(sleepTime);
            }
        }
        averageT4Load /= start;
        System.out.println(
                "Average RT for load-" + load + " on " + loopCount + "-loops is " + averageT4Load + " micro-secs");
        System.out.println("Loops ran: " + start);

    }

    static void randomReadAndWriteFromRM(int rmType, int loopCount) {
        int locSize = locations.size();
        long lStartTime, lEndTime, respTime;
//        int checkSec = 1;
        long secToMicro = 1000000;
        long avRTime = 0;

        avRTime = getAverageResponseTime(rmType);
        System.out.println("#### Average Time: " + avRTime);
        System.out.print("Load (# of Transaction per second: ): ");
        long microPerT = secToMicro / load;
        System.out.println("#### Time Per Transaction (micro-seconds): " + microPerT);

        long averageT4Load = 0;
        int start = 0;
        try {
            switch (rmType) {
                case 0:
                    //get average response time first
                    for (start = 0; start < loopCount; start++) {
                        int noOfOps = ThreadLocalRandom.current().nextInt(0, 10);
                        List<Boolean> opVector = getOpVector(noOfOps);

                        lStartTime = System.nanoTime();
                        int tId = rm.start();
                        for (boolean opType : opVector) {
                            if (opType) {
                                int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                                String location = locations.get(readLocIndex);

                                if (priceOCount == 0) {
                                    rm.queryCars(tId, location);
                                } else {
                                    rm.queryCarsPrice(tId, location);
                                }
                            } else {
                                int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                                int count = ThreadLocalRandom.current().nextInt(1, 100);
                                int price = ThreadLocalRandom.current().nextInt(100, 1000);
                                String location = rgen.nextString();

                                if (add_delete_reserve == 0) {
                                    locations.add(location);
                                    rm.addCars(tId, location, count, price);
                                } else if (add_delete_reserve == 1) {
                                    int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(readLocIndex);
                                    rm.deleteCars(tId, location);
                                    locations.remove(location);
                                } else {
                                    int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(resLocationIn);
                                    String customer = locations.get(cusLocationIn);
                                    rm.reserveCar(tId, customer.hashCode(), location);
                                }
                            }
                        }

                        rm.commit(tId);
                        lEndTime = System.nanoTime();
                        respTime = lEndTime - lStartTime;

                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
                case 1:
                    for (start = 0; start < loopCount; start++) {
                        int noOfOps = ThreadLocalRandom.current().nextInt(0, 10);
                        List<Boolean> opVector = getOpVector(noOfOps);

                        lStartTime = System.nanoTime();
                        int tId = rm.start();
                        for (boolean opType : opVector) {
                            if (opType) {
                                int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                                String location = locations.get(readLocIndex);

                                if (priceOCount == 0) {
                                    rm.queryRooms(tId, location);
                                } else {
                                    rm.queryRoomsPrice(tId, location);
                                }
                            } else {
                                int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                                int count = ThreadLocalRandom.current().nextInt(1, 100);
                                int price = ThreadLocalRandom.current().nextInt(100, 1000);
                                String location = rgen.nextString();

                                if (add_delete_reserve == 0) {
                                    locations.add(location);
                                    rm.addRooms(tId, location, count, price);
                                } else if (add_delete_reserve == 1) {
                                    int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(readLocIndex);
                                    rm.deleteRooms(tId, location);
                                    locations.remove(location);
                                } else {
                                    int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(resLocationIn);
                                    String customer = locations.get(cusLocationIn);
                                    rm.reserveRoom(tId, customer.hashCode(), location);
                                }
                            }
                        }

                        rm.commit(tId);
                        lEndTime = System.nanoTime();
                        respTime = lEndTime - lStartTime;

                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
                case 2:
                    for (start = 0; start < loopCount; start++) {
                        int noOfOps = ThreadLocalRandom.current().nextInt(0, 10);
                        List<Boolean> opVector = getOpVector(noOfOps);

                        lStartTime = System.nanoTime();
                        int tId = rm.start();
                        for (boolean opType : opVector) {
                            if (opType) {
                                int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                                String location = locations.get(readLocIndex);

                                if (priceOCount == 0) {
                                    rm.queryFlight(tId, location.hashCode());
                                } else {
                                    rm.queryFlightPrice(tId, location.hashCode());
                                }
                            } else {
                                int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                                int count = ThreadLocalRandom.current().nextInt(1, 100);
                                int price = ThreadLocalRandom.current().nextInt(100, 1000);
                                String location = rgen.nextString();

                                if (add_delete_reserve == 0) {
                                    locations.add(location);
                                    rm.addFlight(tId, location.hashCode(), count, price);
                                } else if (add_delete_reserve == 1) {
                                    int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(readLocIndex);
                                    rm.deleteFlight(tId, location.hashCode());
                                    locations.remove(location);
                                } else {
                                    int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(resLocationIn);
                                    String customer = locations.get(cusLocationIn);
                                    rm.reserveFlight(tId, customer.hashCode(), location.hashCode());
                                }
                            }
                        }

                        rm.commit(tId);
                        lEndTime = System.nanoTime();
                        respTime = lEndTime - lStartTime;

                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
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

        averageT4Load /= start;
        System.out.println(
                "Average RT for load-" + load + " on " + loopCount + "-loops is " + averageT4Load + " micro-secs");
        System.out.println("Loops ran: " + start);
    }


    static void randomReadAndWriteFromMultipleRM(int loopCount) {
        int locSize = locations.size();
        long lStartTime, lEndTime, respTime;
        long secToMicro = 1000000;

        long microPerT = secToMicro / load;
        System.out.println("#### Time Per Transaction (micro-seconds): " + microPerT);

        long averageT4Load = 0;
        int start = 0;

        try {
            for (start = 0; start < loopCount; start++) {
                int noOfOps = ThreadLocalRandom.current().nextInt(0, 10);
                List<Boolean> opVector = getOpVector(noOfOps);

                lStartTime = System.nanoTime();
                int tId = rm.start();
                for (boolean opType : opVector) {
                    int rmType = ThreadLocalRandom.current().nextInt(0, 4);
                    switch (rmType) {
                        case 0: {
                            if (opType) {
                                int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                                String location = locations.get(readLocIndex);

                                if (priceOCount == 0) {
                                    rm.queryCars(tId, location);
                                } else {
                                    rm.queryCarsPrice(tId, location);
                                }
                            } else {
                                int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                                int count = ThreadLocalRandom.current().nextInt(1, 100);
                                int price = ThreadLocalRandom.current().nextInt(100, 1000);
                                String location = rgen.nextString();

                                if (add_delete_reserve == 0) {
                                    locations.add(location);
                                    rm.addCars(tId, location, count, price);
                                } else if (add_delete_reserve == 1) {
                                    int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(readLocIndex);
                                    rm.deleteCars(tId, location);
                                    locations.remove(location);
                                } else {
                                    int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(resLocationIn);
                                    String customer = locations.get(cusLocationIn);
                                    rm.reserveCar(tId, customer.hashCode(), location);
                                }
                            }
                            break;
                        }

                        case 1: {
                            if (opType) {
                                int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                                String location = locations.get(readLocIndex);

                                if (priceOCount == 0) {
                                    rm.queryRooms(tId, location);
                                } else {
                                    rm.queryRoomsPrice(tId, location);
                                }
                            } else {
                                int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                                int count = ThreadLocalRandom.current().nextInt(1, 100);
                                int price = ThreadLocalRandom.current().nextInt(100, 1000);
                                String location = rgen.nextString();

                                if (add_delete_reserve == 0) {
                                    locations.add(location);
                                    rm.addRooms(tId, location, count, price);
                                } else if (add_delete_reserve == 1) {
                                    int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(readLocIndex);
                                    rm.deleteRooms(tId, location);
                                    locations.remove(location);
                                } else {
                                    int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(resLocationIn);
                                    String customer = locations.get(cusLocationIn);
                                    rm.reserveRoom(tId, customer.hashCode(), location);
                                }
                            }
                            break;
                        }

                        case 2: {
                            if (opType) {
                                int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                                String location = locations.get(readLocIndex);

                                if (priceOCount == 0) {
                                    rm.queryFlight(tId, location.hashCode());
                                } else {
                                    rm.queryFlightPrice(tId, location.hashCode());
                                }
                            } else {
                                int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                                int count = ThreadLocalRandom.current().nextInt(1, 100);
                                int price = ThreadLocalRandom.current().nextInt(100, 1000);
                                String location = rgen.nextString();

                                if (add_delete_reserve == 0) {
                                    locations.add(location);
                                    rm.addFlight(tId, location.hashCode(), count, price);
                                } else if (add_delete_reserve == 1) {
                                    int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(readLocIndex);
                                    rm.deleteFlight(tId, location.hashCode());
                                    locations.remove(location);
                                } else {
                                    int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(resLocationIn);
                                    String customer = locations.get(cusLocationIn);
                                    rm.reserveFlight(tId, customer.hashCode(), location.hashCode());
                                }
                            }
                            break;
                        }

                        case 3: {
                            if (opType) {
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                                String location = locations.get(readLocIndex);
                                rm.queryCustomerInfo(tId, location.hashCode());
                            } else {
                                int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                                String location = rgen.nextString();

                                if (add_delete_reserve == 0) {
                                    locations.add(location);
                                    rm.newCustomer(tId, location.hashCode());
                                } else if (add_delete_reserve == 1) {
                                    int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(readLocIndex);
                                    rm.deleteCustomer(tId, location.hashCode());
                                    locations.remove(location);
                                } else {
                                    int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    location = locations.get(resLocationIn);
                                    String customer = locations.get(cusLocationIn);

                                    int twoPercent = (int) (0.1 * locations.size());
                                    int randomFlightC = ThreadLocalRandom.current().nextInt(0, twoPercent);
                                    Vector<String> flightNumbers = new Vector<>();
                                    for (int i = 0; i < randomFlightC; i++) {
                                        int randLoc = ThreadLocalRandom.current().nextInt(0, locations.size());
                                        String flNUm = Integer.toString(locations.get(randLoc).hashCode());
                                        flightNumbers.addElement(flNUm);
                                    }
                                    boolean boolCar = boolVal(ThreadLocalRandom.current().nextInt(0, 1));
                                    boolean boolRoom = boolVal(ThreadLocalRandom.current().nextInt(0, 1));
                                    rm.itinerary(tId, customer.hashCode(), flightNumbers, location, boolCar, boolRoom);
                                }
                            }
                            break;
                        }
                    }

                }
                rm.commit(tId);
                lEndTime = System.nanoTime();
                respTime = lEndTime - lStartTime;

                long respTInMS = respTime / 1000;
                averageT4Load += respTInMS;
                System.out.println(start + "," + respTInMS);  // in microseconds
                long sleepTime = microPerT - respTInMS;
                if (sleepTime > 0) {
                    waitBeforeNextT(sleepTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        averageT4Load /= start;
        System.out.println(
                "Average RT for load-" + load + " on " + loopCount + "-loops is " + averageT4Load + " micro-secs");
        System.out.println("Loops ran: " + start);
    }


    static void randomReadFromRM(int rmType, int loopCount) {
        int locSize = locations.size();
        long lStartTime, lEndTime, respTime;
//        int checkSec = 1;
        long secToMicro = 1000000;
        long avRTime = 0;

        avRTime = getAverageResponseTime(rmType);
        System.out.println("#### Average Time: " + avRTime);
        System.out.print("Load (# of Transaction per second: ): ");
        long microPerT = secToMicro / load;
        System.out.println("#### Time Per Transaction (micro-seconds): " + microPerT);

        long averageT4Load = 0;
        int start = 0;
        try {
            switch (rmType) {
                case 0:
                    //get average response time first
                    for (start = 0; start < loopCount; start++) {
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
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryCarsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
                case 1:
                    for (start = 0; start < loopCount; start++) {
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
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryRoomsPrice(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
                case 2:
                    for (start = 0; start < loopCount; start++) {
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
                        } else {
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            int count = rm.queryFlightPrice(tId, locHashCode);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
                case 3:
                    for (start = 0; start < loopCount; start++) {
                        int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                        String location = locations.get(readLocIndex);
                        int locHashCode = location.hashCode();

                        lStartTime = System.nanoTime();
                        int tId = rm.start();
                        String info = rm.queryCustomerInfo(tId, locHashCode);
                        rm.commit(tId);
                        lEndTime = System.nanoTime();
                        respTime = lEndTime - lStartTime;

                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
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

        averageT4Load /= start;
        System.out.println(
                "Average RT for load-" + load + " on " + loopCount + "-loops is " + averageT4Load + " micro-secs");
        System.out.println("Loops ran: " + start);
    }

    static void randomWriteToMultipleRMs(int loopCount) {
        long lStartTime, lEndTime, respTime = 0;
        long secToMicro = 1000000;

        long microPerT = secToMicro / load;
        System.out.println("#### Time Per Transaction (micro-seconds): " + microPerT);
        long averageT4Load = 0;
        int start = 0;

        for (start = 0; start < loopCount; start++) {
            int rmType = ThreadLocalRandom.current().nextInt(0, 4);
            int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
            String location = rgen.nextString();

            try {
                switch (rmType) {
                    case 0: {
                        //get average response time first
                        if (add_delete_reserve == 0) {
                            int count = ThreadLocalRandom.current().nextInt(1, 100);
                            int price = ThreadLocalRandom.current().nextInt(100, 1000);
                            locations.add(location);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.addCars(tId, location, count, price);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else if (add_delete_reserve == 1) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(readLocIndex);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.deleteCars(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            locations.remove(location);
                        } else {
                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(resLocationIn);
                            String customer = locations.get(cusLocationIn);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.reserveCar(tId, customer.hashCode(), location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        break;
                    }
                    case 1: {
                        if (add_delete_reserve == 0) {
                            int count = ThreadLocalRandom.current().nextInt(1, 100);
                            int price = ThreadLocalRandom.current().nextInt(100, 1000);
                            locations.add(location);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.addRooms(tId, location, count, price);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else if (add_delete_reserve == 1) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(readLocIndex);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.deleteRooms(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            locations.remove(location);
                        } else {
                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(resLocationIn);
                            String customer = locations.get(cusLocationIn);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.reserveRoom(tId, customer.hashCode(), location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        break;
                    }
                    case 2: {
                        if (add_delete_reserve == 0) {
                            int count = ThreadLocalRandom.current().nextInt(1, 100);
                            int price = ThreadLocalRandom.current().nextInt(100, 1000);
                            locations.add(location);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.addFlight(tId, location.hashCode(), count, price);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else if (add_delete_reserve == 1) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(readLocIndex);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.deleteFlight(tId, location.hashCode());
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            locations.remove(location);
                        } else {
                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(resLocationIn);
                            String customer = locations.get(cusLocationIn);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.reserveFlight(tId, customer.hashCode(), location.hashCode());
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        break;
                    }
                    case 3:
                        if (add_delete_reserve == 0) {
                            locations.add(location);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.newCustomer(tId, location.hashCode());
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else if (add_delete_reserve == 1) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(readLocIndex);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.deleteCustomer(tId, location.hashCode());
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            locations.remove(location);
                        } else {
                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(resLocationIn);
                            String customer = locations.get(cusLocationIn);

                            int twoPercent = (int) (0.1 * locations.size());
                            int randomFlightC = ThreadLocalRandom.current().nextInt(0, twoPercent);
                            Vector<String> flightNumbers = new Vector<>();
                            for (int i = 0; i < randomFlightC; i++) {
                                int randLoc = ThreadLocalRandom.current().nextInt(0, locations.size());
                                String flNUm = Integer.toString(locations.get(randLoc).hashCode());
                                flightNumbers.addElement(flNUm);
                            }
                            boolean boolCar = boolVal(ThreadLocalRandom.current().nextInt(0, 1));
                            boolean boolRoom = boolVal(ThreadLocalRandom.current().nextInt(0, 1));

                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.itinerary(tId, customer.hashCode(), flightNumbers, location, boolCar, boolRoom);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            long respTInMS = respTime / 1000;
            averageT4Load += respTInMS;
            System.out.println(start + "," + respTInMS);  // in microseconds
            long sleepTime = microPerT - respTInMS;
            if (sleepTime > 0) {
                waitBeforeNextT(sleepTime);
            }
        }

        averageT4Load /= start;
        System.out.println(
                "Average RT for load-" + load + " on " + loopCount + "-loops is " + averageT4Load + " micro-secs");
        System.out.println("Loops ran: " + start);
    }

    static void randomWriteToRM(int rmType, int loopCount) {
//        ArrayList<String> randomStuff = new ArrayList<>();

        long lStartTime, lEndTime, respTime = 0;
        long secToMicro = 1000000;
        long microPerT = secToMicro / load;
        System.out.println("#### Time Per Transaction (micro-seconds): " + microPerT);

        long averageT4Load = 0;
        int start = 0;
        try {
            switch (rmType) {
                case 0:
                    //get average response time first
                    for (start = 0; start < loopCount; start++) {
                        int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                        int count = ThreadLocalRandom.current().nextInt(1, 100);
                        int price = ThreadLocalRandom.current().nextInt(100, 1000);
                        String location = rgen.nextString();

                        if (add_delete_reserve == 0) {
                            locations.add(location);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.addCars(tId, location, count, price);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else if (add_delete_reserve == 1) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(readLocIndex);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.deleteCars(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            locations.remove(location);
                        } else {
                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(resLocationIn);
                            String customer = locations.get(cusLocationIn);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.reserveCar(tId, customer.hashCode(), location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }

                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
                case 1:
                    for (start = 0; start < loopCount; start++) {
                        int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                        int count = ThreadLocalRandom.current().nextInt(1, 100);
                        int price = ThreadLocalRandom.current().nextInt(100, 1000);
                        String location = rgen.nextString();

                        if (add_delete_reserve == 0) {
                            locations.add(location);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.addRooms(tId, location, count, price);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else if (add_delete_reserve == 1) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(readLocIndex);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.deleteRooms(tId, location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            locations.remove(location);
                        } else {
                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(resLocationIn);
                            String customer = locations.get(cusLocationIn);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.reserveRoom(tId, customer.hashCode(), location);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }

                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
                case 2:
                    for (start = 0; start < loopCount; start++) {
                        int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                        int count = ThreadLocalRandom.current().nextInt(1, 100);
                        int price = ThreadLocalRandom.current().nextInt(100, 1000);
                        String location = rgen.nextString();

                        if (add_delete_reserve == 0) {
                            locations.add(location);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.addFlight(tId, location.hashCode(), count, price);
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        } else if (add_delete_reserve == 1) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(readLocIndex);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.deleteFlight(tId, location.hashCode());
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                            locations.remove(location);
                        } else {
                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                            location = locations.get(resLocationIn);
                            String customer = locations.get(cusLocationIn);
                            lStartTime = System.nanoTime();
                            int tId = rm.start();
                            rm.reserveFlight(tId, customer.hashCode(), location.hashCode());
                            rm.commit(tId);
                            lEndTime = System.nanoTime();
                            respTime = lEndTime - lStartTime;
                        }

                        long respTInMS = respTime / 1000;
                        averageT4Load += respTInMS;
                        System.out.println(start + "," + respTInMS);  // in microseconds
                        long sleepTime = microPerT - respTInMS;
                        if (sleepTime > 0) {
                            waitBeforeNextT(sleepTime);
                        }
                    }
                    break;
//                case 3:
//                    for (start = 0; start < loopCount; start++) {
//                        int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
//                        int count = ThreadLocalRandom.current().nextInt(1, 100);
//                        int price = ThreadLocalRandom.current().nextInt(100, 1000);
//                        String location = rgen.nextString();
//
//                        if (add_delete_reserve == 0) {
//                            locations.add(location);
//                            lStartTime = System.nanoTime();
//                            int tId = rm.start();
//                            rm.addCars(tId, location, count, price);
//                            rm.commit(tId);
//                            lEndTime = System.nanoTime();
//                            respTime = lEndTime - lStartTime;
//                        } else if (add_delete_reserve == 1){
//                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
//                            location = locations.get(readLocIndex);
//                            lStartTime = System.nanoTime();
//                            int tId = rm.start();
//                            rm.deleteCars(tId, location);
//                            rm.commit(tId);
//                            lEndTime = System.nanoTime();
//                            respTime = lEndTime - lStartTime;
//                            locations.remove(location);
//                        } else {
//                            int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
//                            int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
//                            location = locations.get(resLocationIn);
//                            String customer = locations.get(cusLocationIn);
//                            lStartTime = System.nanoTime();
//                            int tId = rm.start();
//                            rm.reserveCar(tId, customer.hashCode(), location);
//                            rm.commit(tId);
//                            lEndTime = System.nanoTime();
//                            respTime = lEndTime - lStartTime;
//                        }
//
//                        long respTInMS = respTime / 1000;
//                        averageT4Load += respTInMS;
//                        System.out.println(start + "," + respTInMS);  // in microseconds
//                        long sleepTime = microPerT - respTInMS;
//                        if (sleepTime > 0) {
//                            waitBeforeNextT(sleepTime);
//                        }
//                    }
//                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        averageT4Load /= start;
        System.out.println(
                "Average RT for load-" + load + " on " + loopCount + "-loops is " + averageT4Load + " micro-secs");
        System.out.println("Loops ran: " + start);
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


    static long randomReadAndWriteFromMultipleRM() {
        int locSize = locations.size();
        long lStartTime, lEndTime, respTime;
        long secToMicro = 1000000;
        long respTInMS = 0;

        try {
            int noOfOps = ThreadLocalRandom.current().nextInt(0, 10);
            List<Boolean> opVector = getOpVector(noOfOps);

            lStartTime = System.nanoTime();
            int tId = rm.start();
            for (boolean opType : opVector) {
                int rmType = ThreadLocalRandom.current().nextInt(0, 4);
                switch (rmType) {
                    case 0: {
                        if (opType) {
                            int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                            String location = locations.get(readLocIndex);

                            if (priceOCount == 0) {
                                rm.queryCars(tId, location);
                            } else {
                                rm.queryCarsPrice(tId, location);
                            }
                        } else {
                            int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                            int count = ThreadLocalRandom.current().nextInt(1, 100);
                            int price = ThreadLocalRandom.current().nextInt(100, 1000);
                            String location = rgen.nextString();

                            if (add_delete_reserve == 0) {
                                locations.add(location);
                                rm.addCars(tId, location, count, price);
                            } else if (add_delete_reserve == 1) {
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(readLocIndex);
                                rm.deleteCars(tId, location);
                                locations.remove(location);
                            } else {
                                int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(resLocationIn);
                                String customer = locations.get(cusLocationIn);
                                rm.reserveCar(tId, customer.hashCode(), location);
                            }
                        }
                        break;
                    }

                    case 1: {
                        if (opType) {
                            int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                            String location = locations.get(readLocIndex);

                            if (priceOCount == 0) {
                                rm.queryRooms(tId, location);
                            } else {
                                rm.queryRoomsPrice(tId, location);
                            }
                        } else {
                            int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                            int count = ThreadLocalRandom.current().nextInt(1, 100);
                            int price = ThreadLocalRandom.current().nextInt(100, 1000);
                            String location = rgen.nextString();

                            if (add_delete_reserve == 0) {
                                locations.add(location);
                                rm.addRooms(tId, location, count, price);
                            } else if (add_delete_reserve == 1) {
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(readLocIndex);
                                rm.deleteRooms(tId, location);
                                locations.remove(location);
                            } else {
                                int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(resLocationIn);
                                String customer = locations.get(cusLocationIn);
                                rm.reserveRoom(tId, customer.hashCode(), location);
                            }
                        }
                        break;
                    }

                    case 2: {
                        if (opType) {
                            int priceOCount = ThreadLocalRandom.current().nextInt(0, 2);
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                            String location = locations.get(readLocIndex);

                            if (priceOCount == 0) {
                                rm.queryFlight(tId, location.hashCode());
                            } else {
                                rm.queryFlightPrice(tId, location.hashCode());
                            }
                        } else {
                            int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                            int count = ThreadLocalRandom.current().nextInt(1, 100);
                            int price = ThreadLocalRandom.current().nextInt(100, 1000);
                            String location = rgen.nextString();

                            if (add_delete_reserve == 0) {
                                locations.add(location);
                                rm.addFlight(tId, location.hashCode(), count, price);
                            } else if (add_delete_reserve == 1) {
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(readLocIndex);
                                rm.deleteFlight(tId, location.hashCode());
                                locations.remove(location);
                            } else {
                                int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(resLocationIn);
                                String customer = locations.get(cusLocationIn);
                                rm.reserveFlight(tId, customer.hashCode(), location.hashCode());
                            }
                        }
                        break;
                    }

                    case 3: {
                        if (opType) {
                            int readLocIndex = ThreadLocalRandom.current().nextInt(0, locSize);
                            String location = locations.get(readLocIndex);
                            rm.queryCustomerInfo(tId, location.hashCode());
                        } else {
                            int add_delete_reserve = ThreadLocalRandom.current().nextInt(0, 3);
                            String location = rgen.nextString();

                            if (add_delete_reserve == 0) {
                                locations.add(location);
                                rm.newCustomer(tId, location.hashCode());
                            } else if (add_delete_reserve == 1) {
                                int readLocIndex = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(readLocIndex);
                                rm.deleteCustomer(tId, location.hashCode());
                                locations.remove(location);
                            } else {
                                int resLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                int cusLocationIn = ThreadLocalRandom.current().nextInt(0, locations.size());
                                location = locations.get(resLocationIn);
                                String customer = locations.get(cusLocationIn);

                                int twoPercent = (int) (0.1 * locations.size());
                                int randomFlightC = ThreadLocalRandom.current().nextInt(0, twoPercent);
                                Vector<String> flightNumbers = new Vector<>();
                                for (int i = 0; i < randomFlightC; i++) {
                                    int randLoc = ThreadLocalRandom.current().nextInt(0, locations.size());
                                    String flNUm = Integer.toString(locations.get(randLoc).hashCode());
                                    flightNumbers.addElement(flNUm);
                                }
                                boolean boolCar = boolVal(ThreadLocalRandom.current().nextInt(0, 1));
                                boolean boolRoom = boolVal(ThreadLocalRandom.current().nextInt(0, 1));
                                rm.itinerary(tId, customer.hashCode(), flightNumbers, location, boolCar, boolRoom);
                            }
                        }
                        break;
                    }
                }

            }
            rm.commit(tId);
            lEndTime = System.nanoTime();
            respTime = lEndTime - lStartTime;
            respTInMS = respTime / 1000;

        } catch (Exception e) {
            e.printStackTrace();
        }

//        System.out.println(clientNum + " - average RT for load-" + load + " is " + respTInMS + " micro-secs");
        return respTInMS;
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
        for (int all = 0; all < 1000; all++) {
            String newLoc = rgen.nextString();
            locations.add(newLoc);
        }
//        locations.add("montreal");
//        locations.add("toronto");
//        locations.add("vancouver");
//        locations.add("sanfransico");
//        locations.add("paloalto");
//        locations.add("colombo");
//        locations.add("kandy");
//        locations.add("shanghai");
//        locations.add("melbourne");
//        locations.add("london");
//        locations.add("sydney");
//        locations.add("venice");
//        locations.add("newyork");
//        locations.add("canbarra");
//        locations.add("delhi");
//        locations.add("frankfurt");
    }

    private static boolean boolVal(int intVal) {
        return intVal == 0;
    }

    private static List<Boolean> getOpVector(int noOfOps) {
        List<Boolean> flags = new ArrayList<Boolean>();
        for (int i = 0; i < noOfOps / 2; i++) flags.add(true);
        for (int i = 0; i < noOfOps / 2; i++) flags.add(false);
        Collections.shuffle(flags);
        return flags;
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
