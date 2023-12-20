package money.boku.transfers.account;

/**
 * Thrown when there's no such account in the system.
 */
public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
