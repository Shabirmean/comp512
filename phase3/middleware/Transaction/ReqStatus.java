package Transaction;

/**
 * Created by shabirmean on 2017-11-12 with some hope.
 */
public enum ReqStatus {
    SUCCESS(0),
    FAILURE(-1),
    INVALID_TRANSACTION_ID(10),
    COMMIT_TO_RM_FAILED(11),
    MW_RM_COMMUNICATION_FAILED(12),
    NO_MATCHING_RM(13),
    LOCK_REQUEST_FAILED(14),
    REQUEST_ON_DELETED_ITEM(15),
    REQUEST_ON_NULL_ITEM(16),
    DEADLOCK_MET(17),
    INVALID_RM_TRANSACTION(18),
    COMMIT_OR_ABORT_TO_RM_THROWED_ERROR(19),
    SHUTDOWN_TO_RM_THROWED_ERROR(20),
    SHUTDOWN_AT_TM_MIGHT_BE_PARTIAL(21),
    ACTIVE_TRANSACTIONS_EXIST(22),
    RESERVE_ITEM_ZERO(23);

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
            case 10:
                this.status = "INVALID_TRANSACTION_ID";
                break;
            case 11:
                this.status = "COMMIT_TO_RM_FAILED";
                break;
            case 12:
                this.status = "MW_RM_COMMUNICATION_FAILED";
                break;
            case 13:
                this.status = "NO_MATCHING_RM_FOUND";
                break;
            case 14:
                this.status = "LOCK_REQUEST_FAILED";
                break;
            case 15:
                this.status = "REQUEST_ON_DELETED_ITEM";
                break;
            case 16:
                this.status = "REQUEST_ON_NON_EXISTING_ITEM";
                break;
            case 17:
                this.status = "DEADLOCK_ENCOUNTERED";
                break;
            case 18:
                this.status = "INVALID_RM_TRANSACTION";
                break;
            case 19:
                this.status = "COMMIT_OR_ABORT_TO_RM_THROWED_ERROR";
                break;
            case 20:
                this.status = "SHUTDOWN_TO_RM_THROWED_ERROR";
                break;
            case 21:
                this.status = "SHUTDOWN_AT_TM_MIGHT_BE_PARTIAL";
                break;
            case 22:
                this.status = "ACTIVE_TRANSACTIONS_EXIST";
                break;
            case 23:
                this.status = "RESERVE_ITEM_ZERO";
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
