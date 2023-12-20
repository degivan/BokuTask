package money.boku.transfers.account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Holds information about all accounts in the system.
 */
public interface AccountDatastore {
    /**
     * Creates new account in the system.
     *
     * @param initialBalance initial balance in the account
     * @return new account
     */
    Account createNewAccount(BigDecimal initialBalance);

    /**
     * Returns account by id.
     *
     * @param accountId account id
     * @return account with corresponding id; if such account does not exist, returns null
     */
    Account getAccount(UUID accountId) throws AccountNotFoundException;

    /**
     * Increases balance in the account. Can be called concurrently.
     *
     * @param accountId id of the account
     * @param amount    to add to the balance
     * @throws AccountNotFoundException when no account with such id exists
     */
    void increaseBalance(UUID accountId, BigDecimal amount) throws AccountNotFoundException;

    /**
     * Decreases balance in the account. Can be called concurrently.
     *
     * @param accountId id of the account
     * @param amount    to subtract from the balance
     * @throws AccountNotFoundException        when no account with such id exists
     * @throws NegativeBalanceRequestException when executing operation would result in negative balance
     */
    void decreaseBalance(UUID accountId, BigDecimal amount) throws AccountNotFoundException, NegativeBalanceRequestException;
}
