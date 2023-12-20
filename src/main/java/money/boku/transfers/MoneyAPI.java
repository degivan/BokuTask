package money.boku.transfers;

import io.javalin.http.Context;
import money.boku.transfers.account.AccountNotFoundException;
import money.boku.transfers.account.NegativeBalanceRequestException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Encapsulates API related to money transactions.
 */
public class MoneyAPI {
    public static final String TRANSFER_PATH = "/transfer";
    public static final String WITHDRAW_PATH = "/withdraw";
    public static final String WITHDRAWAL_STATE_PATH = "/withdraw/{id}";

    private final MoneyService moneyService;

    public MoneyAPI(MoneyService moneyService) {
        this.moneyService = moneyService;
    }


    /**
     * Handles transfer HTTP request
     *
     * @param ctx request context
     * @throws NegativeBalanceRequestException when executing transfer request would've resulted in a negative balance
     * @throws AccountNotFoundException        when there's no sender or receiver account with specified account id
     */
    public void handleTransferRequest(@NotNull Context ctx) throws NegativeBalanceRequestException, AccountNotFoundException {
        Transfer transfer = ctx.bodyValidator(Transfer.class)
                .check(t -> t.from() != null, "sender shouldn't be empty")
                .check(t -> t.to() != null, "receiver shouldn't be empty")
                // nothing will break if they are the same, but such operations are pointless
                .check(t -> !Objects.equals(t.from(), t.to()), "sender should be different from receiver")
                .check(t -> t.amount() != null && t.amount().signum() > 0, "amount is required and should be positive number")
                .get();
        moneyService.transfer(transfer.from(), transfer.to(), transfer.amount());
    }

    /**
     * Handles withdraw HTTP request
     *
     * @param ctx request context
     * @throws NegativeBalanceRequestException when executing withdraw request would've resulted in a negative balance
     * @throws AccountNotFoundException        when there's no sender account with specified account id
     */
    public void handleWithdrawRequest(@NotNull Context ctx) throws NegativeBalanceRequestException, AccountNotFoundException {
        WithdrawalRequest withdrawalRequest = ctx.bodyValidator(WithdrawalRequest.class)
                .check(r -> r.accountId() != null, "sender shouldn't be empty")
                .check(r -> r.withdrawalAddress() != null && !r.withdrawalAddress().isEmpty(), "")
                .check(t -> t.amount() != null && t.amount().signum() > 0, "amount is required and should be positive number")
                .get();
        WithdrawalService.Address withdrawalAddress = new WithdrawalService.Address(withdrawalRequest.withdrawalAddress());
        WithdrawalService.WithdrawalId withdrawalId = moneyService.withdraw(withdrawalRequest.accountId(), withdrawalAddress, withdrawalRequest.amount());
        ctx.json(new WithdrawalRequestResponse(withdrawalId.value()));
    }

    /**
     * Handles withdrawal state HTTP request
     *
     * @param ctx request context
     * @throws WithdrawalNotFoundException when there's no withdrawal request with specified id
     */
    public void handleWithdrawalStateRequest(@NotNull Context ctx) throws WithdrawalNotFoundException {
        String idStr = ctx.pathParam("id");
        WithdrawalService.WithdrawalId withdrawalId = new WithdrawalService.WithdrawalId(UUID.fromString(idStr));
        WithdrawalService.WithdrawalState state = moneyService.withdrawRequestState(withdrawalId);
        ctx.json(new WithdrawalStateResponse(state));
    }
}
