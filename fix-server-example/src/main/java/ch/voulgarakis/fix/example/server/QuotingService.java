package ch.voulgarakis.fix.example.server;

import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveFixSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import quickfix.Message;
import quickfix.field.MsgType;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.fix43.Quote;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.util.UUID;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.isMessageOfType;
import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.safeGetField;

@Service
public class QuotingService implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(QuotingService.class);

    private final ReactiveFixSession fixSession;
    private Disposable quoteRequestSubscription;

    @Autowired
    public QuotingService(
            //The quoting session does not specify in quickfixj.cfg a SessionName
            //Therefore to select the quoting session bean, we qualify with the sessionID
            @Qualifier("FIX.4.3:FIX_CLIENT_COMPID->FIX_SERVER_COMPID") ReactiveFixSession fixSession) {
        this.fixSession = fixSession;
    }

    @PreDestroy
    public void destroy() {
        quoteRequestSubscription.dispose();
    }

    @Override
    public void run(ApplicationArguments args) {
        quoteRequestSubscription = fixSession
                //Subscribe to quote requests
                .subscribe(message -> isMessageOfType(message, MsgType.QUOTE_REQUEST))
                // For each quote request, create a quote
                .flatMap(this::createQuote)
                //subscribe to start the processing
                .subscribe();
    }

    private Mono<Message> createQuote(Message quoteRequest) {
        //Copy over the quoteReqId
        String quoteReqId = safeGetField(quoteRequest, new QuoteReqID())
                .orElse("no-quote-request-found");
        LOG.info("Received a QuoteRequest with QuoteReqID={}", quoteReqId);

        Quote quote = new Quote();
        quote.set(new QuoteReqID(quoteReqId));
        //Set a random quote ID
        quote.set(new QuoteID(quoteReqId + "/" + UUID.randomUUID().toString()));

        //and send the quote back in the session
        return fixSession.send(() -> quote);
    }
}
