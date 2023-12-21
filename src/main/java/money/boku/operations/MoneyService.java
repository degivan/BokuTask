package money.boku.operations;

import money.boku.account.AccountNotFoundException;
import money.boku.account.NegativeBalanceRequestException;
import money.boku.operations.withdrawal.WithdrawalNotFoundException;
import money.boku.operations.withdrawal.WithdrawalService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service that handles money transactions.
 */
public interface MoneyService {
    /**
     * Transfers money between two accounts.
     *
     * @param from   sender account id
     * @param to     receiver account id
     * @param amount amount to transfer
     * @throws NegativeBalanceRequestException if there's not enough money in sender account to transfer the {@code amount}
     * @throws AccountNotFoundException        if {@code from} or {@code to} account is not found
     */
    void transfer(UUID from, UUID to, BigDecimal amount) throws NegativeBalanceRequestException, AccountNotFoundException;

    /**
     * Withdraws money from the account.
     *
     * @param from   sender account id
     * @param to     address to withdraw money to
     * @param amount amount to transfer
     * @return id of withdrawal request
     * @throws NegativeBalanceRequestException if there's not enough money in sender account to withdraw the {@code amount}
     * @throws AccountNotFoundException        if {@code from} account is not found
     */
    WithdrawalService.WithdrawalId withdraw(UUID from, WithdrawalService.Address to, BigDecimal amount) throws NegativeBalanceRequestException, AccountNotFoundException;

    /**
     * Returns the state of withdrawal request.
     *
     * @param withdrawalId id of withdrawal request
     * @return state of withdrawal request
     * @throws WithdrawalNotFoundException if there's no withdrawal request with such id
     */
    WithdrawalService.WithdrawalState withdrawRequestState(WithdrawalService.WithdrawalId withdrawalId) throws WithdrawalNotFoundException;
}
