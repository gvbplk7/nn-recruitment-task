package polak.nn.account.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import polak.nn.account.domain.model.AccountBalanceHistory;
import polak.nn.account.domain.model.BalanceChange;
import polak.nn.account.domain.port.AccountBalanceHistoryRepository;

@Component
@RequiredArgsConstructor
public class BalanceChangedEventListener {

    private final AccountBalanceHistoryRepository historyRepository;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBalanceChanged(BalanceChangedEvent event) {
        BalanceChange from = event.changes().get(0);
        BalanceChange to = event.changes().get(1);

        AccountBalanceHistory history = new AccountBalanceHistory();
        history.setUserId(event.userId());
        history.setFromCurrency(from.getCurrency());
        history.setFromPreviousBalance(from.getPreviousBalance());
        history.setFromNewBalance(from.getNewBalance());
        history.setToCurrency(to.getCurrency());
        history.setToPreviousBalance(to.getPreviousBalance());
        history.setToNewBalance(to.getNewBalance());
        history.setRate(event.rate());
        history.setChangedAt(from.getChangedAt());
        historyRepository.save(history);
    }
}
