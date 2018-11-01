package failing_single_jvm;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.artio.Reply;
import uk.co.real_logic.artio.builder.ExampleMessageEncoder;
import uk.co.real_logic.artio.decoder.*;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.SessionAcquireHandler;
import uk.co.real_logic.artio.library.SessionConfiguration;
import uk.co.real_logic.artio.session.Session;

import java.util.function.BooleanSupplier;

import static java.util.Collections.singletonList;

public class DummyTaker implements DictionaryAcceptor {
    public Session session;
    private BooleanSupplier startLambda;
    private FixLibrary library;
    private final ExampleMessageEncoder exampleMessageEncoder;

    public DummyTaker(final SessionConfiguration sessionConfig) {
        startLambda = () ->
        {
            final SessionAcquireHandler sessionAcquireHandler =
                    (session1, isSlow) -> new MessageHandler(this);
            library =
                    DummyUtils.blockingConnect(
                            new LibraryConfiguration()
                                    .sessionAcquireHandler(sessionAcquireHandler)
                                    .libraryAeronChannels(singletonList(DummyUtils.buildChannelString(11111))));
            final IdleStrategy idleStrategy = new SleepingIdleStrategy(1);
            final Reply<Session> reply = library.initiate(sessionConfig);
            while (reply.isExecuting()) {
                idleStrategy.idle(library.poll(1));
            }
            if (!reply.hasCompleted()) {
                System.out.println("unable to initiate the session");
                library.close();
                return false;
            }
            session = reply.resultIfPresent();
            System.out.println("Replied with: " + reply.state());
            new Thread(this::run).start();
            return true;
        };
        exampleMessageEncoder = new ExampleMessageEncoder();
        exampleMessageEncoder.testReqID("11144325435325325423321525236324321534".toCharArray());
    }

    private void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final IdleStrategy idleStrategy = new BusySpinIdleStrategy();
        while (true) {
            idleStrategy.idle((tryRead() ? 1 : 0));
        }
    }

    public boolean tryRead() {
        int counter = 0;
        int fragmentsRead;
        do {
            fragmentsRead = library.poll(10);
            counter++;
            if ((counter & DummyUtils.COUNTER_MASK) == 0) {
                System.out.println("Taker too many messages in tryRead !!!!!");
            }
        }
        while (fragmentsRead > 0);
        return true;
    }

    public boolean start() {
        return startLambda.getAsBoolean();
    }

    @Override
    public void onLogon(final LogonDecoder decoder) {

    }

    @Override
    public void onExampleMessage(final ExampleMessageDecoder decoder) {
        session.send(exampleMessageEncoder);
    }

    @Override
    public void onResendRequest(ResendRequestDecoder decoder) {

    }

    @Override
    public void onReject(RejectDecoder decoder) {

    }

    @Override
    public void onSequenceReset(SequenceResetDecoder decoder) {

    }

    @Override
    public void onHeartbeat(final HeartbeatDecoder decoder) {

    }

    @Override
    public void onTestRequest(TestRequestDecoder decoder) {

    }

    @Override
    public void onLogout(final LogoutDecoder decoder) {

    }
}
