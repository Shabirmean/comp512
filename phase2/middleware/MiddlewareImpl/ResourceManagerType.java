package MiddlewareImpl;

public enum ResourceManagerType {
    CAR(0),
    HOTEL(1),
    FLIGHT(2);

    private final int rmCode;

    ResourceManagerType(int rmCode) {
        this.rmCode = rmCode;
    }

    public int getRMCode() {
        return this.rmCode;
    }
}
