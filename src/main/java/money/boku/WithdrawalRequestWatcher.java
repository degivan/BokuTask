package money.boku;

import money.boku.account.AccountDatastore;
import money.boku.account.AccountNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * WithdrawalRequestWatcher keeps tracks of all active withdrawal requests. It's main job is to
 * handle transaction rollback when withdrawal has failed.
 */
public class WithdrawalRequestWatcher {
    private static final Logger logger = LoggerFactory.getLogger(WithdrawalRequestWatcher.class);

    private final AccountDatastore accountDatastore;
    private final WithdrawalService withdrawalService;
    private final Queue<WithdrawalRequestRecord> queue = new ConcurrentLinkedQueue<>();

    public WithdrawalRequestWatcher(AccountDatastore accountDatastore, WithdrawalService withdrawalService) {
        this.accountDatastore = Objects.requireNonNull(accountDatastore);
        this.withdrawalService = Objects.requireNonNull(withdrawalService);
    }

    /**
     * Add withdrawal request to the watch queue.
     *
     * @param withdrawalRequestRecord withdrawal request record
     */
    public void watchRequest(WithdrawalRequestRecord withdrawalRequestRecord) {
        queue.add(withdrawalRequestRecord);
    }

    /**
     * Starts separate thread that periodically goes through
     * all watched withdrawal requests and checks their status in withdrawal service.
     * In case withdrawal request has failed, performs rollback and returns money back
     * to the account.
     */
    public void startWatching() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            // Using size() instead of queue iterator gives better guarantee
            // we won't handle the same withdrawal request twice in the same run.
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                WithdrawalRequestRecord request = queue.poll();
                if (request == null) {
                    return;
                }
                // Hypothetically, if withdrawal requests can be removed from the withdrawal service,
                // we might want not to fail here if request is not found but rather continue to go through the loop.
                WithdrawalService.WithdrawalState requestState = withdrawalService.getRequestState(request.withdrawalId());
                if (requestState == WithdrawalService.WithdrawalState.PROCESSING) {
                    queue.add(request);
                } else if (requestState == WithdrawalService.WithdrawalState.FAILED) {
                    try {
                        accountDatastore.increaseBalance(request.from(), request.amount());
                    } catch (AccountNotFoundException e) {
                        logger.error("Failed to find account %s to return frozen funds to".formatted(request.from()));
                    }
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS); // can be configurable
    }

    record WithdrawalRequestRecord(WithdrawalService.WithdrawalId withdrawalId, UUID from, BigDecimal amount) {
    }
}
