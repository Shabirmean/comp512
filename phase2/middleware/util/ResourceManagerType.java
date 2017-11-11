package util;

public enum ResourceManagerType {
    CAR(0),
    HOTEL(1),
    FLIGHT(2),
    CUSTOMER(3);

    private final int rmCode;
    private String rmCodeString;

    ResourceManagerType(int rmCode) {
        this.rmCode = rmCode;
        switch(this.rmCode){
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

}
