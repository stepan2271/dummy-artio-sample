package failing_single_jvm;

import org.agrona.concurrent.SleepingIdleStrategy;
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
    public Session session;
    private FixLibrary library;

    public DummyMaker(final String initiatorCompId, final String acceptorCompId)
    {
        startLambda = () ->
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
                (LibraryConfiguration)new LibraryConfiguration()
                    .sessionExistsHandler(new AcquiringSessionExistsHandler())
                    .sessionAcquireHandler(sessionAcquireHandler)
                    .libraryAeronChannels(singletonList(DummyUtils.buildChannelString(11113)))
                    .sessionIdStrategy(SessionIdStrategy.senderAndTarget())
                    .authenticationStrategy(authenticationStrategy);
            library = DummyUtils.blockingConnect(libraryConfiguration);
            new Thread(this::run).start();
            return true;
        };
    }

    private void run()
    {
        try
        {
            Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        final SleepingIdleStrategy sleepingIdleStrategy = new SleepingIdleStrategy(10);
        while (true)
        {
            sleepingIdleStrategy.idle((tryRead() ? 1 : 0));
        }
    }

    public boolean tryRead()
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
//        System.out.println(decoder);
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
    public void onHeartbeat(final HeartbeatDecoder decoder)
    {

    }

    @Override
    public void onTestRequest(TestRequestDecoder decoder) {

    }

    @Override
    public void onLogout(final LogoutDecoder decoder)
    {

    }
}
