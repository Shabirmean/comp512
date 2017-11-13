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
    RESERVE_RESOURCE(21),
    UNRESERVE_RESOURCE(22);

    private static final long serialVersionUID = 7526472295622776149L;

    private final int reqCode;
    private String reqMethod;

    RequestType(int reqCode) {
        this.reqCode = reqCode;
        switch (this.reqCode) {
            case 0:
                this.reqMethod = "newflight";
                break;
            case 1:
                this.reqMethod = "newcar";
                break;
            case 2:
                this.reqMethod = "newroom";
                break;
            case 3:
                this.reqMethod = "newcustomer";
            case 4:
                this.reqMethod = "newcustomerid";
                break;
            case 5:
                this.reqMethod = "deleteflight";
                break;
            case 6:
                this.reqMethod = "deletecar";
                break;
            case 7:
                this.reqMethod = "deleteroom";
            case 8:
                this.reqMethod = "deletecustomer";
                break;
            case 9:
                this.reqMethod = "queryflight";
                break;
            case 10:
                this.reqMethod = "querycar";
                break;
            case 11:
                this.reqMethod = "queryroom";
            case 12:
                this.reqMethod = "querycustomer";
                break;
            case 13:
                this.reqMethod = "queryflightprice";
                break;
            case 14:
                this.reqMethod = "querycarprice";
                break;
            case 15:
                this.reqMethod = "queryroomprice";
            case 16:
                this.reqMethod = "reserveflight";
                break;
            case 17:
                this.reqMethod = "reservecar";
                break;
            case 18:
                this.reqMethod = "reserveroom";
                break;
            case 19:
                this.reqMethod = "itinerary";
                break;
        }
    }

    public int getReqCode() {
        return this.reqCode;
    }

    public String getReqMethod() {
        return this.reqMethod;
    }

//    public class MessageStatus{
//        public static final int RM_SERVER_FAIL_STATUS = 0;
//        public static final int RM_SERVER_SUCCESS_STATUS = 1;
//    }
}
