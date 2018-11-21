package failing_single_jvm;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.artio.builder.ExampleMessageEncoder;
import uk.co.real_logic.artio.decoder.*;
import uk.co.real_logic.artio.library.AcquiringSessionExistsHandler;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.SessionAcquireHandler;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.session.SessionIdStrategy;
import uk.co.real_logic.artio.validation.AuthenticationStrategy;
import uk.co.real_logic.artio.validation.MessageValidationStrategy;

import java.util.Collections;
import java.util.function.BooleanSupplier;

import static java.util.Collections.singletonList;

public class DummyMaker
    implements DictionaryAcceptor
{
    private BooleanSupplier startLambda;
    private Session session;
    private FixLibrary library;
    private int numberOfResponsesWeWait = 0;
    private boolean isBlocked = false;
    private final ExampleMessageEncoder exampleMessageEncoder;
    private final String initiatorCompId;
    private final String acceptorCompId;

    private String randomReqId = "";

    DummyMaker(final String initiatorCompId, final String acceptorCompId)
    {
        this.initiatorCompId = initiatorCompId;
        this.acceptorCompId = acceptorCompId;
        startLambda = () ->
        {
            new Thread(this::run).start();
            return true;
        };
        exampleMessageEncoder = new ExampleMessageEncoder();
    }

    private void run()
    {
        final SessionAcquireHandler sessionAcquireHandler = (session, isSlow) ->
        {
            System.out.println("Session acceptor started");
            this.session = session;
            return new MessageHandler(this);
        };
        final MessageValidationStrategy validationStrategy = MessageValidationStrategy.targetCompId(acceptorCompId)
            .and(MessageValidationStrategy.senderCompId(Collections.singletonList(initiatorCompId)));
        final AuthenticationStrategy authenticationStrategy = AuthenticationStrategy.of(validationStrategy);
        LibraryConfiguration libraryConfiguration =
            (LibraryConfiguration) new LibraryConfiguration()
                .sessionExistsHandler(new AcquiringSessionExistsHandler())
                .sessionAcquireHandler(sessionAcquireHandler)
                .libraryAeronChannels(singletonList(DummyUtils.buildChannelString(11113)))
                .sessionIdStrategy(SessionIdStrategy.senderAndTarget())
                .authenticationStrategy(authenticationStrategy);
        library = DummyUtils.blockingConnect(libraryConfiguration);

        try
        {
            Thread.sleep(100);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        final IdleStrategy idleStrategy = new BusySpinIdleStrategy();

        while (true)
        {
            idleStrategy.idle(trySendMessage() ? 1 : 0);
            idleStrategy.idle(tryRead() ? 1 : 0);
            if (sentMessages % 1000 == 0)
            {
                System.out.println(sentMessages + " / " + receivedMessages);
            }
        }
    }

    private boolean tryRead()
    {
        int counter = 0;
        int fragmentsRead;
        do
        {
            fragmentsRead = library.poll(10);
            counter++;
            if ((counter & DummyUtils.COUNTER_MASK) == 0)
            {
                System.out.println("Maker too many messages in tryRead !!!!!");
            }
        }
        while (fragmentsRead > 0);
        return true;
    }

    private int sentMessages = 0;
    private int receivedMessages = 0;

    private boolean trySendMessage()
    {
        if (isBlocked || session == null)
        {
            try
            {
                Thread.sleep(1);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            return false;
        }
        randomReqId = System.currentTimeMillis() + (randomReqId.length() > 200 ? "" : randomReqId);
        exampleMessageEncoder.testReqID(randomReqId.toCharArray());
        session.send(exampleMessageEncoder);
        sentMessages++;
        numberOfResponsesWeWait++;
        if (numberOfResponsesWeWait == 5)
        {
            isBlocked = true;
        }
        return true;
    }

    public boolean start()
    {
        return startLambda.getAsBoolean();
    }

    @Override
    public void onLogon(final LogonDecoder decoder)
    {

    }

    @Override
    public void onExampleMessage(final ExampleMessageDecoder decoder)
    {
        numberOfResponsesWeWait--;
        if (numberOfResponsesWeWait == 0)
        {
            isBlocked = false;
        }
        receivedMessages++;
    }

    @Override
    public void onResendRequest(ResendRequestDecoder decoder)
    {

    }

    @Override
    public void onReject(RejectDecoder decoder)
    {

    }

    @Override
    public void onSequenceReset(SequenceResetDecoder decoder)
    {

    }

    @Override
    public void onHeartbeat(final HeartbeatDecoder decoder)
    {

    }

    @Override
    public void onTestRequest(TestRequestDecoder decoder)
    {

    }

    @Override
    public void onLogout(final LogoutDecoder decoder)
    {

    }
}
