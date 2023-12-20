package money.boku.transfers;

/**
 * Thrown when there's no such withdrawal request in the system.
 */
public class WithdrawalNotFoundException extends Exception {
    public WithdrawalNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
