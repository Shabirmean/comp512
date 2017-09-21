package socs.distributed.resource;


import java.io.Serializable;

public enum MsgType implements Serializable {
    ADD_FLIGHT(0),
    ADD_CARS(1),
    ADD_ROOMS(2),
    ADD_NEW_CUSTOMER_WITHOUT_ID(3),
    ADD_NEW_CUSTOMER_WITH_ID(4),

    DELETE_FLIGHT(5),
    DELETE_CARS(6),
    DELETE_ROOMS(7),
    DELETE_CUSTOMER(8),

    QUERY_FLIGHT(9),
    QUERY_CARS(10),
    QUERY_ROOMS(11),
    QUERY_CUSTOMER_INFO(12),
    QUERY_FLIGHT_PRICE(13),
    QUERY_CARS_PRICE(14),
    QUERY_ROOMS_PRICE(15),

    RESERVE_FLIGHT(16),
    RESERVE_CAR(17),
    RESERVE_ROOM(18),
    RESERVE_ITINERARY(19);

    private final int msgCode;

    MsgType(int msgCode) {
        this.msgCode = msgCode;
    }

    public int getMsgCode() {
        return this.msgCode;
    }
}
