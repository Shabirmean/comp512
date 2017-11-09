package util;

public enum RequestType {
    ADD_FLIGHT(0),
    DELETE_FLIGHT(5),
    QUERY_FLIGHT(9),
    QUERY_FLIGHT_PRICE(13),
    RESERVE_FLIGHT(16),

    ADD_CARS(1),
    DELETE_CARS(6),
    QUERY_CARS(10),
    QUERY_CAR_PRICE(14),
    RESERVE_CAR(17),

    ADD_ROOMS(2),
    DELETE_ROOMS(7),
    QUERY_ROOMS(11),
    QUERY_ROOM_PRICE(15),
    RESERVE_ROOM(18),

    ADD_NEW_CUSTOMER_WITHOUT_ID(3),
    ADD_NEW_CUSTOMER_WITH_ID(4),
    DELETE_CUSTOMER(8),
    QUERY_CUSTOMER_INFO(12),
    RESERVE_ITINERARY(19),
    ROLLBACK_ITINERARY(20),
    UNRESERVE_RESOURCE(21);

    private static final long serialVersionUID = 7526472295622776149L;

    private final int reqCode;

    RequestType(int reqCode) {
        this.reqCode = reqCode;
    }

    public int getReqCode() {
        return this.reqCode;
    }

//    public class MessageStatus{
//        public static final int RM_SERVER_FAIL_STATUS = 0;
//        public static final int RM_SERVER_SUCCESS_STATUS = 1;
//    }
}
