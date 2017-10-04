package socs.distributed.middleware.server;

import org.apache.log4j.Logger;
import socs.distributed.middleware.exception.MiddlewareException;
import socs.distributed.middleware.util.MiddlewareConstants;
import socs.distributed.middleware.util.MiddlewareUtils;
import socs.distributed.resource.dto.RMConcurrentHashMap;
import socs.distributed.resource.dto.ReservableItem;
import socs.distributed.resource.dto.ReservedItem;
import socs.distributed.resource.entity.Car;
import socs.distributed.resource.entity.Customer;
import socs.distributed.resource.entity.Flight;
import socs.distributed.resource.entity.Hotel;
import socs.distributed.resource.message.MsgType;
import socs.distributed.resource.message.RequestMessage;
import socs.distributed.resource.message.ResponseMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

/**
 * An class which holds all the methods related to handling a new incoming request. Implements "Runnable"
 * whose run method is called by the parent class upon receiving a new incoming request. Consists of separate
 * methods to handle different types of incoming requests.
 */
@SuppressWarnings("Duplicates")
public class MiddlewareRequestHandler implements Runnable {
    private static final Logger log = Logger.getLogger(MiddlewareRequestHandler.class);
    // the unique socket allocated for this new request instance via which future communications happen.
    private final Socket clientSocket;
    private String clientHostName;

    MiddlewareRequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientHostName = clientSocket.getInetAddress().getHostName();
    }

    /**
     * Overridden "run" method from Runnable that initiates new-request handling procedures. Sets up the socket
     * reader and writer objects and invokes appropriate method according to the incoming message type.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        ResponseMessage responseToClient = new ResponseMessage();
        ObjectOutputStream socketWriter;
        ObjectInputStream socketReader;
        int id, customerId;

        try {
            log.info("Received request from client: " + clientHostName);
            socketWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            socketReader = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to open Input/Output stream on the " +
                    "client socket connection for READ/WRITE.");
            return;
        }

        try {
            RequestMessage requestMsgFromClient = (RequestMessage) socketReader.readObject();
            MsgType requestMsgType = requestMsgFromClient.getMsgType();
            Vector msgArgs = requestMsgFromClient.getMethodArguments();

            if (MiddlewareServer.isRequestForMW(requestMsgType)) {
                switch (requestMsgType) {
                    case ADD_NEW_CUSTOMER_WITHOUT_ID:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = MiddlewareServer.internalResourceManager.newCustomer(id);

                        log.info("New customer added with ID:" + customerId);
                        responseToClient.setMessage("A new customer was successfully added. ID [" + customerId + "]");
                        responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;

                    case ADD_NEW_CUSTOMER_WITH_ID:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));

                        if (MiddlewareServer.internalResourceManager.newCustomer(id, customerId)) {
                            log.info("New customer added with ID:" + customerId);
                            responseToClient.setMessage("New customer added with ID:" + customerId);
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("New customer could not be added with ID:" + customerId);
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToClient.setMessage("New customer could not be added with ID:" + customerId);
                        }
                        break;

                    case DELETE_CUSTOMER:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));
                        RMConcurrentHashMap customerResvMap =
                                MiddlewareServer.internalResourceManager.deleteCustomer(id, customerId);
                        if (customerResvMap != null) {
                            log.info("Customer deleted. ID:" + customerId);
                            responseToClient.setMessage("Customer deleted. ID:" + customerId);
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                            if (customerResvMap.size() > 0) {
                                updateReservations(Integer.toString(id), Integer.toString(customerId), customerResvMap);
                            }
                        } else {
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToClient.setMessage("Customer could not be deleted. ID:" + customerId);
                        }
                        break;

                    case QUERY_CUSTOMER_INFO:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));
                        String cusBill = MiddlewareServer.internalResourceManager.queryCustomerInfo(id, customerId);

                        log.info("Customer info:" + cusBill);
                        responseToClient.setMessage("Customer info:" + cusBill);
                        responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;

                    case RESERVE_ITINERARY:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));

                        String location = this.getString(msgArgs.elementAt(msgArgs.size() - 3));
                        boolean bookCars = this.getBoolean(msgArgs.elementAt(msgArgs.size() - 2));
                        boolean bookRooms = this.getBoolean(msgArgs.elementAt(msgArgs.size() - 1));

                        Customer customer = MiddlewareServer.internalResourceManager.isValidCustomer(id, customerId);
                        if (customer == null) {
                            log.info("Could not reserve itinerary for " +
                                    "[ID: " + id + ", CustomerID: " + customerId + "]");
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToClient.setMessage("Itinerary reserve request failed. Customer does not exist.");
                        } else {
                            MWResourceManager mwResourceManager =
                                    MiddlewareServer.externalResourceManagers.get(MWResourceManager.RM_Type.FLIGHTS);

                            //TODO:: Handle flight bookings
                            ResponseMessage respFromRM =
                                    contactResourceManager(mwResourceManager, requestMsgFromClient);
                            Vector<ReservableItem> reservableItems = respFromRM.getItems();
                            for (ReservableItem item : reservableItems) {
                                int flightNo = Integer.parseInt(item.getLocation());
                                customer.reserve(Flight.getKey(flightNo), String.valueOf(flightNo), item.getPrice());
                            }

                            // NEED to remove from customer
                            // TODO:: Need to be in a try-catch block to handle exceptions and rollback...
                            if (bookCars) {
                                mwResourceManager =
                                        MiddlewareServer.externalResourceManagers.get(MWResourceManager.RM_Type.CARS);
                                respFromRM = contactResourceManager(mwResourceManager, requestMsgFromClient);

                                if (respFromRM.getStatus() == MsgType.MessageStatus.RM_SERVER_FAIL_STATUS) {
                                    // rollback flight booking if car-booking failed
                                    log.warn("Itinerary Reservation failed when trying to make [Car] booking");
                                    responseToClient = rollBackReservation(
                                            requestMsgFromClient, MWResourceManager.RM_Type.FLIGHTS);

                                    for (ReservableItem item : reservableItems) {
                                        int flightNo = Integer.parseInt(item.getLocation());
                                        customer.unReserve(Flight.getKey(flightNo));
                                    }
                                    break;
                                }
                                ReservableItem carItem = respFromRM.getItems().get(0);
                                customer.reserve(Car.getKey(location), location, carItem.getPrice());
                            }

                            if (bookRooms) {
                                mwResourceManager =
                                        MiddlewareServer.externalResourceManagers.get(MWResourceManager.RM_Type.ROOMS);
                                respFromRM = contactResourceManager(mwResourceManager, requestMsgFromClient);

                                if (respFromRM.getStatus() == MsgType.MessageStatus.RM_SERVER_FAIL_STATUS) {
                                    // rollback car booking if hotel-booking failed
                                    log.warn("Itinerary Reservation failed when trying to make [Hotel] booking");
                                    responseToClient = rollBackReservation(requestMsgFromClient,
                                            MWResourceManager.RM_Type.FLIGHTS, MWResourceManager.RM_Type.CARS);
                                    customer.unReserve(Car.getKey(location));
                                    break;
                                }
                                ReservableItem roomItem = respFromRM.getItems().get(0);
                                customer.reserve(Hotel.getKey(location), location, roomItem.getPrice());
                            }

                            if (MiddlewareServer.internalResourceManager.reserveItem(id, customer)) {
                                String clientMsg = "Itinerary Reservation Successful.";
                                log.info(clientMsg);

                                int noOFFlights = msgArgs.size() - 6;
                                if (noOFFlights != reservableItems.size()) {
                                    log.warn("Not all flights were available for booking. Some were not booked!!!");
                                    clientMsg += "\n[WARN] Not all flights were available for booking. " +
                                            "Some were not reserved";
                                }
                                responseToClient.setMessage(clientMsg);
                                responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                            } else {
                                log.info("Itinerary Reservation Failed");
                                responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                                responseToClient.setMessage("Itinerary Reservation could not be completed");
                            }
                        }
                        break;
                }

            } else {
                MWResourceManager.RM_Type rmTypeOfRequest =
                        MWResourceManager.getRMCorrespondingToRequest(requestMsgType);
                MWResourceManager mwResourceManager =
                        MiddlewareServer.externalResourceManagers.get(rmTypeOfRequest);

                if (MiddlewareServer.isReserveRequest(requestMsgType)) {
                    id = this.getInt(msgArgs.elementAt(1));
                    customerId = this.getInt(msgArgs.elementAt(2));
                    String locationOrFlNo = this.getString(msgArgs.elementAt(3));

                    Customer customer = MiddlewareServer.internalResourceManager.isValidCustomer(id, customerId);
                    if (customer == null) {
                        log.info("Could not execute reserve for [ID: " + id + ", CustomerID: " + customerId + "]");
                        responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                        responseToClient.setMessage("Reservation request failed. Customer does not exist.");

                    } else {
                        ResponseMessage respFromRM = contactResourceManager(mwResourceManager, requestMsgFromClient);
                        if (respFromRM.getStatus() == 0) {
                            //TODO:: Need to handle exception cases.
                            log.warn("Reserve request did not pass. The resource-manger replied FAILURE.");
                        } else {
                            ReservableItem reservableItem = respFromRM.getItems().elementAt(0);
                            switch (requestMsgType) {
                                case RESERVE_FLIGHT:
                                    customer.reserve(Flight.getKey(Integer.parseInt(locationOrFlNo)),
                                            String.valueOf(locationOrFlNo), reservableItem.getPrice());
                                    break;

                                case RESERVE_CAR:
                                    customer.reserve(Car.getKey(locationOrFlNo), locationOrFlNo, reservableItem
                                            .getPrice());
                                    break;

                                case RESERVE_ROOM:
                                    customer.reserve(
                                            Hotel.getKey(locationOrFlNo), locationOrFlNo, reservableItem.getPrice());
                                    break;
                            }

                            if (MiddlewareServer.internalResourceManager.reserveItem(id, customer)) {
                                log.info("Reservation Successful");
                                responseToClient.setMessage("Reservation was successfully completed");
                                responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                            } else {
                                log.info("Reservation Failed");
                                responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                                responseToClient.setMessage("Reservation could not be completed");
                            }
                        }
                    }
                } else {
                    responseToClient = contactResourceManager(mwResourceManager, requestMsgFromClient);
                }
            }
            socketWriter.writeObject(responseToClient);

        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to READ [SOSPFPacket] object from socket stream.", e);
        } catch (ClassNotFoundException e) {
            log.error("An object type other than [SOSPFPacket] was recieved over the socket connection", e);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MiddlewareUtils.releaseSocket(clientSocket);
            MiddlewareUtils.releaseWriter(socketWriter);
            MiddlewareUtils.releaseReader(socketReader);
        }
    }

    private void updateReservations(String id, String customerID, RMConcurrentHashMap customerResvMap) throws
            MiddlewareException {
        Vector<String> flightItems = new Vector<>();
        Vector<String> carItems = new Vector<>();
        Vector<String> hotelItems = new Vector<>();

        // add request ID and CustomerID to the front of the Vector
        flightItems.add(id);
        flightItems.add(customerID);
        carItems.add(id);
        carItems.add(customerID);
        hotelItems.add(id);
        hotelItems.add(customerID);

        for (Object reservedItem : customerResvMap.values()) {
            ReservedItem reservdItem = (ReservedItem) reservedItem;
            String itemKey = reservdItem.getKey();

            if (itemKey.contains(MiddlewareConstants.FLIGHT_ITEM_KEY)) {
                flightItems.add(reservdItem.getLocation());
            } else if (itemKey.contains(MiddlewareConstants.CAR_ITEM_KEY)) {
                carItems.add(reservdItem.getLocation());
            } else if (itemKey.contains(MiddlewareConstants.HOTEL_ITEM_KEY)) {
                hotelItems.add(reservdItem.getLocation());
            } else {
                log.error("Unrecognized item key [" + itemKey + "] found for item of customer: [" + customerID + "]");
            }
        }

        RequestMessage updateResrvMsg = new RequestMessage();
        updateResrvMsg.setMsgType(MsgType.UNRESERVE_RESOURCE);
        updateResrvMsg.setMessage("Removing reservations for a deleted customer [" + customerID + "]");

        if (flightItems.size() > 2) {
            MWResourceManager mwResourceManager =
                    MiddlewareServer.externalResourceManagers.get(MWResourceManager.RM_Type.FLIGHTS);
            updateResrvMsg.setMethodArguments(flightItems);
            ResponseMessage respFromRM = contactResourceManager(mwResourceManager, updateResrvMsg);

            if (respFromRM.getStatus() == MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS) {
                log.info("Flight reservations for deleted customer [" + customerID + "] was updated.");
            } else {
                log.warn("Flight reservations for deleted customer [" + customerID + "] failed to update.");
            }
        }

        if (carItems.size() > 2) {
            MWResourceManager mwResourceManager =
                    MiddlewareServer.externalResourceManagers.get(MWResourceManager.RM_Type.CARS);
            updateResrvMsg.setMethodArguments(carItems);
            ResponseMessage respFromRM = contactResourceManager(mwResourceManager, updateResrvMsg);

            if (respFromRM.getStatus() == MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS) {
                log.info("Car reservations for deleted customer [" + customerID + "] was updated.");
            } else {
                log.warn("Car reservations for deleted customer [" + customerID + "] failed to update.");
            }
        }

        if (hotelItems.size() > 2) {
            MWResourceManager mwResourceManager =
                    MiddlewareServer.externalResourceManagers.get(MWResourceManager.RM_Type.ROOMS);
            updateResrvMsg.setMethodArguments(hotelItems);
            ResponseMessage respFromRM = contactResourceManager(mwResourceManager, updateResrvMsg);
            if (respFromRM.getStatus() == MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS) {
                log.info("Hotel reservations for deleted customer [" + customerID + "] was updated.");
            } else {
                log.warn("Hotel reservations for deleted customer [" + customerID + "] failed to update.");
            }
        }
    }


    private ResponseMessage rollBackReservation(RequestMessage requestMsgFromClient,
                                                MWResourceManager.RM_Type... rollbackRM) throws MiddlewareException {
        for (MWResourceManager.RM_Type rmType : rollbackRM) {
            MWResourceManager mwResourceManager = MiddlewareServer.externalResourceManagers.get(rmType);
            ResponseMessage respFromRM = contactResourceManager(mwResourceManager, requestMsgFromClient);

            if (respFromRM.getStatus() == MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS) {
                log.warn("Rollback of [" + rmType.toString() + "] booking was 'SUCCESSFUL'.");
            } else {
                log.warn("Rollback of [" + rmType.toString() + "] booking 'FAILED'.");
            }
        }
        ResponseMessage responseToClient = new ResponseMessage();
        responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
        responseToClient.setMessage("Itinerary Reservation could not be completed");
        return responseToClient;
    }


    @SuppressWarnings({"StatementWithEmptyBody", "InfiniteLoopStatement"})
    private ResponseMessage contactResourceManager(MWResourceManager mwResourceManager,
                                                   RequestMessage requestMsgFromClient) throws MiddlewareException {
        ResponseMessage responseFromRMServer;
        String rmIP = mwResourceManager.getRmIP();
        short rmPort = mwResourceManager.getRmPort();

        Socket clientSocket;
        ObjectOutputStream socketWriter = null;
        ObjectInputStream socketReader = null;

        try {
            clientSocket = new Socket(rmIP, rmPort);
            log.info("Connected to [" + mwResourceManager.getType() + "] resource-manager at " +
                    "[" + rmIP + ":" + rmPort + "] to serve request from client: " + clientHostName);
        } catch (IOException e) {
            String errMsg = "An error occurred whilst trying to establish Socket connection to " +
                    "resource-manager at [" + rmIP + ":" + rmPort + "]";
            log.error(errMsg);
            throw new MiddlewareException(errMsg, e);
        }

        try {
            socketWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            socketReader = new ObjectInputStream(clientSocket.getInputStream());
            socketWriter.writeObject(requestMsgFromClient);

            responseFromRMServer = (ResponseMessage) socketReader.readObject();
            int responseStatus = responseFromRMServer.getStatus();
            if (responseStatus == 0) {
                log.error("There was an error when processing request from [" + clientHostName + "]");
            } else {
                log.info("Resource manager replied successfully for request from [" + clientHostName + "]");
            }

        } catch (IOException e) {
            String errMsg = "An error occurred whilst trying to READ/WRITE from socket to RM server";
            responseFromRMServer = new ResponseMessage();
            responseFromRMServer.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
            responseFromRMServer.setMessage(errMsg);
            log.error(errMsg);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            String errMsg = "Object type received from RM was not [ResponseMessage]";
            responseFromRMServer = new ResponseMessage();
            responseFromRMServer.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
            responseFromRMServer.setMessage(errMsg);
            log.error(errMsg);
            e.printStackTrace();
        } finally {
            MiddlewareUtils.releaseSocket(clientSocket);
            MiddlewareUtils.releaseWriter(socketWriter);
            MiddlewareUtils.releaseReader(socketReader);
        }
        return responseFromRMServer;
    }

    private int getInt(Object temp) throws Exception {
        return new Integer((String) temp);
    }

    private String getString(Object temp) throws Exception {
        return (String) temp;
    }

    private boolean getBoolean(Object temp) throws Exception {
        return Boolean.valueOf((String) temp);
    }

}
