package socs.distributed.server;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.resource.exception.COMP512Exception;
import socs.distributed.resource.message.MsgType;
import socs.distributed.resource.message.RequestMessage;
import socs.distributed.resource.message.ResponseMessage;
import socs.distributed.server.util.RMServerConstants;
import socs.distributed.server.util.RMServerUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

@SuppressWarnings("Duplicates")
public class RMServerRequestHandler implements Runnable {
    private final Log log = LogFactory.getLog(RMServerRequestHandler.class);
    // the unique socket allocated for this new request instance via which future communications happen.
    private final Socket middlewareClientSocket;

    RMServerRequestHandler(Socket middlewareClientSocket) {
        this.middlewareClientSocket = middlewareClientSocket;
    }

    /**
     * Overridden "run" method from Runnable that initiates new-request handling procedures. Sets up the socket
     * reader and writer objects and invokes appropriate method according to the incoming message type.
     */
    @Override
    public void run() {
        ResponseMessage responseToMW = new ResponseMessage();
        ObjectOutputStream socketWriter;
        ObjectInputStream socketReader;
        int id;
        try {
            socketWriter = new ObjectOutputStream(middlewareClientSocket.getOutputStream());
            socketReader = new ObjectInputStream(middlewareClientSocket.getInputStream());
        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to open Input/Output stream on the " +
                    "client socket connection for READ/WRITE.");
            return;
        }

        try {
            RequestMessage requestMsgFromMW = (RequestMessage) socketReader.readObject();
            MsgType requestMsgType = requestMsgFromMW.getMsgType();
            Vector msgArgs = requestMsgFromMW.getMethodArguments();

            if (!RMServer.isAllowedRequest(requestMsgType)) {
                COMP512Exception exception = new COMP512Exception("Request for invalid resource. This " +
                        "Resource-Manager only handles requests related to [" + RMServer.rmServerRole + "]");
                responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                responseToMW.setException(exception);
                responseToMW.setMessage("Call to resource manager failed");
            } else {
                switch (requestMsgType) {
                    case ADD_FLIGHT: {
                        id = this.getInt(msgArgs.elementAt(1));
                        int flightNum = this.getInt(msgArgs.elementAt(2));
                        int flightSeats = this.getInt(msgArgs.elementAt(3));
                        int flightPrice = this.getInt(msgArgs.elementAt(4));

                        if (RMServer.resourceManager.addFlight(id, flightNum, flightSeats, flightPrice)) {
                            log.info("Flight added");
                            responseToMW.setMessage("A new flight was successfully added");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Flight could not be added");
                            COMP512Exception exception = new COMP512Exception("Flight could not be added");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Flight could not be added");
                        }
                        break;
                    }

                    case ADD_CARS: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String carLocation = this.getString(msgArgs.elementAt(2));
                        int numCars = this.getInt(msgArgs.elementAt(3));
                        int carPrice = this.getInt(msgArgs.elementAt(4));

                        if (RMServer.resourceManager.addCars(id, carLocation, numCars, carPrice)) {
                            log.info("Cars added");
                            responseToMW.setMessage("A new car was successfully added");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Cars could not be added");
                            COMP512Exception exception = new COMP512Exception("Cars could not be added");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Cars could not be added");
                        }
                        break;
                    }

                    case ADD_ROOMS: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String roomLocation = this.getString(msgArgs.elementAt(2));
                        int numRooms = this.getInt(msgArgs.elementAt(3));
                        int hotelPrice = this.getInt(msgArgs.elementAt(4));

                        if (RMServer.resourceManager.addRooms(id, roomLocation, numRooms, hotelPrice)) {
                            log.info("Rooms added");
                            responseToMW.setMessage("A new room was successfully added");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Rooms could not be added");
                            COMP512Exception exception = new COMP512Exception("Rooms could not be added");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Rooms could not be added");
                        }
                        break;
                    }

                    case DELETE_FLIGHT: {
                        id = this.getInt(msgArgs.elementAt(1));
                        int flightNum = this.getInt(msgArgs.elementAt(2));

                        if (RMServer.resourceManager.deleteFlight(id, flightNum)) {
                            log.info("Flight Deleted");
                            responseToMW.setMessage("Flight was successfully deleted");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Flight could not be deleted");
                            COMP512Exception exception = new COMP512Exception("FLight could not be deleted");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Flight could not be deleted");
                        }
                        break;
                    }

                    case DELETE_CARS: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String carLocation = this.getString(msgArgs.elementAt(2));

                        if (RMServer.resourceManager.deleteCars(id, carLocation)) {
                            log.info("Car Deleted");
                            responseToMW.setMessage("Car was successfully deleted");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Car could not be deleted");
                            COMP512Exception exception = new COMP512Exception("Car could not be deleted");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Car could not be deleted");
                        }
                        break;
                    }

                    case DELETE_ROOMS: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String roomLocation = this.getString(msgArgs.elementAt(2));
                        if (RMServer.resourceManager.deleteRooms(id, roomLocation)) {
                            log.info("Room Deleted");
                            responseToMW.setMessage("Room was successfully deleted");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Room could not be deleted");
                            COMP512Exception exception = new COMP512Exception("Room could not be deleted");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Room could not be deleted");
                        }
                        break;
                    }

                    case QUERY_FLIGHT: {
                        id = this.getInt(msgArgs.elementAt(1));
                        int flightNum = this.getInt(msgArgs.elementAt(2));
                        int seats = RMServer.resourceManager.queryFlight(id, flightNum);

                        log.info("Number of seats available:" + seats);
                        responseToMW.setMessage("Number of seats available:" + seats);
                        responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;
                    }

                    case QUERY_CARS: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String carLocation = this.getString(msgArgs.elementAt(2));
                        int numCars = RMServer.resourceManager.queryCars(id, carLocation);

                        log.info("Number of Cars at this location:" + numCars);
                        responseToMW.setMessage("number of Cars at this location:" + numCars);
                        responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;
                    }

                    case QUERY_ROOMS: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String roomLocation = this.getString(msgArgs.elementAt(2));
                        int numRooms = RMServer.resourceManager.queryRooms(id, roomLocation);

                        log.info("Number of Rooms at this location:" + numRooms);
                        responseToMW.setMessage("Number of Rooms at this location:" + numRooms);
                        responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;
                    }

                    case QUERY_FLIGHT_PRICE: {
                        id = this.getInt(msgArgs.elementAt(1));
                        int flightNum = this.getInt(msgArgs.elementAt(2));
                        int flightPrice = RMServer.resourceManager.queryFlightPrice(id, flightNum);

                        log.info("Price of a seat:" + flightPrice);
                        responseToMW.setMessage("Price of a seat:" + flightPrice);
                        responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;
                    }

                    case QUERY_CAR_PRICE: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String carLocation = this.getString(msgArgs.elementAt(2));
                        int carPrice = RMServer.resourceManager.queryCarsPrice(id, carLocation);

                        log.info("Price of a car at this location:" + carPrice);
                        responseToMW.setMessage("Price of a car at this location:" + carPrice);
                        responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;
                    }

                    case QUERY_ROOM_PRICE: {
                        id = this.getInt(msgArgs.elementAt(1));
                        String roomLocation = this.getString(msgArgs.elementAt(2));
                        int hotelPrice = RMServer.resourceManager.queryRoomsPrice(id, roomLocation);

                        log.info("Price of Rooms at this location:" + hotelPrice);
                        responseToMW.setMessage("Price of Rooms at this location:" + hotelPrice);
                        responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;
                    }

                    case RESERVE_FLIGHT: {
                        id = this.getInt(msgArgs.elementAt(1));
                        int customer = this.getInt(msgArgs.elementAt(2));
                        int flightNum = this.getInt(msgArgs.elementAt(3));

                        if (RMServer.resourceManager.reserveFlight(id, customer, flightNum)) {
                            log.info("Flight Reserved");
                            responseToMW.setMessage("Flight was successfully reserved");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Flight could not be reserved");
                            COMP512Exception exception = new COMP512Exception("Flight could not be reserved");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Flight could not be reserved");
                        }
                        break;
                    }

                    case RESERVE_CAR: {
                        id = this.getInt(msgArgs.elementAt(1));
                        int customer = this.getInt(msgArgs.elementAt(2));
                        String carLocation = this.getString(msgArgs.elementAt(3));

                        if (RMServer.resourceManager.reserveCar(id, customer, carLocation)) {
                            log.info("Car Reserved");
                            responseToMW.setMessage("Car was successfully reserved");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Car could not be reserved");
                            COMP512Exception exception = new COMP512Exception("Car could not be reserved");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Car could not be reserved");
                        }
                        break;
                    }

                    case RESERVE_ROOM: {
                        id = this.getInt(msgArgs.elementAt(1));
                        int customer = this.getInt(msgArgs.elementAt(2));
                        String roomLocation = this.getString(msgArgs.elementAt(3));

                        if (RMServer.resourceManager.reserveRoom(id, customer, roomLocation)) {
                            log.info("Room Reserved");
                            responseToMW.setMessage("Room was successfully reserved");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Room could not be reserved");
                            COMP512Exception exception = new COMP512Exception("Room could not be reserved");
                            responseToMW.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToMW.setException(exception);
                            responseToMW.setMessage("Room could not be reserved");
                        }
                        break;
                    }
                }
            }

            socketWriter.writeObject(responseToMW);
        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to READ [RequestMessage] object from socket stream.", e);
        } catch (ClassNotFoundException e) {
            log.error("An object type other than [RequestMessage] was received over the socket connection", e);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RMServerUtils.releaseSocket(middlewareClientSocket);
            RMServerUtils.releaseWriter(socketWriter);
            RMServerUtils.releaseReader(socketReader);
        }
    }


    private int getInt(Object temp) throws Exception {
        return new Integer((String) temp);
    }

    private String getString(Object temp) throws Exception {
        return (String) temp;
    }
}
