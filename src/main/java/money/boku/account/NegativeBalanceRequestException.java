package money.boku.account;

/**
 * Thrown when user transfer/withdrawal request can result in negative balance on the account.
 */
public class NegativeBalanceRequestException extends Exception {
    public NegativeBalanceRequestException(String errorMessage) {
        super(errorMessage);
    }
}
