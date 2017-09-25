package socs.distributed.middleware.server;

import socs.distributed.resource.message.MsgType;

class MWResourceManager {
    private RM_Type type;
    private String rmIP;
    private short rmPort;

    MWResourceManager(RM_Type type, String rmIP, short rmPort) {
        this.type = type;
        this.rmIP = rmIP;
        this.rmPort = rmPort;
    }

    RM_Type getType() {
        return type;
    }

    String getRmIP() {
        return rmIP;
    }

    short getRmPort() {
        return rmPort;
    }

    public enum RM_Type {
        FLIGHTS("FLIGHT"),
        CARS("CARS"),
        ROOMS("ROOMS"),
        CUSTOMERS("CUSTOMERS"),
        ITINERARY("CUSTOMERS"),
        NOT_RECOGNIZED("NOT_RECOGNIZED");

        private final String text;

        RM_Type(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    static RM_Type getRMCorrespondingToRequest(MsgType msgType) {
        if (msgType == MsgType.ADD_FLIGHT || msgType == MsgType.DELETE_FLIGHT || msgType == MsgType.QUERY_FLIGHT ||
                msgType == MsgType.QUERY_FLIGHT_PRICE || msgType == MsgType.RESERVE_FLIGHT) {
            return RM_Type.FLIGHTS;
        } else if (msgType == MsgType.ADD_CARS || msgType == MsgType.DELETE_CARS || msgType == MsgType.QUERY_CARS ||
                msgType == MsgType.QUERY_CAR_PRICE || msgType == MsgType.RESERVE_CAR) {
            return RM_Type.CARS;
        } else if (msgType == MsgType.ADD_ROOMS || msgType == MsgType.DELETE_ROOMS || msgType == MsgType.QUERY_ROOMS ||
                msgType == MsgType.QUERY_ROOM_PRICE || msgType == MsgType.RESERVE_ROOM) {
            return RM_Type.ROOMS;
        } else if (msgType == MsgType.ADD_NEW_CUSTOMER_WITHOUT_ID || msgType == MsgType.ADD_NEW_CUSTOMER_WITH_ID ||
                msgType == MsgType.DELETE_CUSTOMER || msgType == MsgType.QUERY_CUSTOMER_INFO) {
            return RM_Type.CUSTOMERS;
        } else if (msgType == MsgType.RESERVE_ITINERARY) {
            return RM_Type.ITINERARY;
        }
        return RM_Type.NOT_RECOGNIZED;
    }
}
