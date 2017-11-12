package Transaction;

/**
 * Created by shabirmean on 2017-11-12 with some hope.
 */
public enum ReqStatus {
    SUCCESS(0),
    FAILURE(-1);

    private final int statusCode;
    private String status;

    ReqStatus(int statusCode) {
        this.statusCode = statusCode;
        switch (statusCode) {
            case 0:
                this.status = "SUCCESS";
                break;
            case 1:
                this.status = "FAILURE";
                break;
        }
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getStatus() {
        return this.status;
    }
}
