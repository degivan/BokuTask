package money.boku.operations;

import money.boku.account.AccountDatastore;
import money.boku.account.AccountNotFoundException;
import money.boku.account.NegativeBalanceRequestException;
import money.boku.operations.withdrawal.WithdrawalNotFoundException;
import money.boku.operations.withdrawal.WithdrawalRequestWatcher;
import money.boku.operations.withdrawal.WithdrawalService;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of {@link MoneyService}.
 */
public class MoneyServiceImpl implements MoneyService {
    private final AccountDatastore accountDatastore;
    private final WithdrawalService withdrawalService;
    private final WithdrawalRequestWatcher withdrawalRequestWatcher;

    public MoneyServiceImpl(AccountDatastore accountDatastore, WithdrawalService withdrawalService, WithdrawalRequestWatcher withdrawalRequestWatcher) {
        this.accountDatastore = Objects.requireNonNull(accountDatastore);
        this.withdrawalService = Objects.requireNonNull(withdrawalService);
        this.withdrawalRequestWatcher = Objects.requireNonNull(withdrawalRequestWatcher);
    }

    @Override
    public void transfer(UUID from, UUID to, BigDecimal amount) throws NegativeBalanceRequestException, AccountNotFoundException {
        accountDatastore.decreaseBalance(from, amount);
        try {
            accountDatastore.increaseBalance(to, amount);
        } catch (AccountNotFoundException e) {
            // Rollback in case there's no receiver to receive money.
            // Within this solution we assume that account can't be deleted. In hypothetical situation where it can,
            // we would require to wait for deletion until all transfer/withdraw operations are complete + reject all new
            // transfer/withdraw requests.
            accountDatastore.increaseBalance(from, amount);
            throw e;
        }
    }

    @Override
    public WithdrawalService.WithdrawalId withdraw(UUID from, WithdrawalService.Address withdrawalAddress, BigDecimal amount) throws NegativeBalanceRequestException, AccountNotFoundException {
        // Since there's no reverse operation in withdrawal service, the execution flow is next:
        // 1. Decrease account balance
        // 2. Try to execute WithdrawalService#requestWithdraw
        // 3. If failed, increase account balance again
        // We operate under assumption that while withdrawal hasn't failed, the withdrawn money are "frozen" within the account.
        accountDatastore.decreaseBalance(from, amount);
        WithdrawalService.WithdrawalId withdrawalId = new WithdrawalService.WithdrawalId(UUID.randomUUID());
        while (true) {
            try {
                withdrawalService.requestWithdrawal(withdrawalId, withdrawalAddress, amount);
                break;
            } catch (IllegalArgumentException e) {
                withdrawalId = new WithdrawalService.WithdrawalId(UUID.randomUUID());
            }
        }
        withdrawalRequestWatcher.watchRequest(new WithdrawalRequestWatcher.WithdrawalRequestRecord(withdrawalId, from, amount));
        return withdrawalId;
    }

    @Override
    public WithdrawalService.WithdrawalState withdrawRequestState(WithdrawalService.WithdrawalId withdrawalId) throws WithdrawalNotFoundException {
        try {
            return withdrawalService.getRequestState(withdrawalId);
        } catch (IllegalArgumentException e) {
            throw new WithdrawalNotFoundException(e.getMessage());
        }
    }
}
