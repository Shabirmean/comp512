package socs.distributed.middleware.server;


public class MWResourceManager {
    private RM_Type type;
    private String rmIP;
    private short rmPort;

    MWResourceManager(RM_Type type, String rmIP, short rmPort) {
        this.type = type;
        this.rmIP = rmIP;
        this.rmPort = rmPort;
    }

    public RM_Type getType() {
        return type;
    }

    public void setType(RM_Type type) {
        this.type = type;
    }

    public String getRmIP() {
        return rmIP;
    }

    public void setRmIP(String rmIP) {
        this.rmIP = rmIP;
    }

    public short getRmPort() {
        return rmPort;
    }

    public void setRmPort(short rmPort) {
        this.rmPort = rmPort;
    }

    public enum RM_Type {
        FLIGHTS("FLIGHT"),
        CARS("CARS"),
        ROOMS("ROOMS");

        private final String text;

        private RM_Type(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
