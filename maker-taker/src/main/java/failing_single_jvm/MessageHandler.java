package failing_single_jvm;

import io.aeron.logbuffer.ControlledFragmentHandler;
import org.agrona.DirectBuffer;
import uk.co.real_logic.artio.decoder.DictionaryAcceptor;
import uk.co.real_logic.artio.decoder.DictionaryDecoder;
import uk.co.real_logic.artio.library.SessionHandler;
import uk.co.real_logic.artio.messages.DisconnectReason;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.util.AsciiBuffer;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import static io.aeron.logbuffer.ControlledFragmentHandler.Action.CONTINUE;

public class MessageHandler
    implements SessionHandler
{
    private final AsciiBuffer string = new MutableAsciiBuffer();
    private final DictionaryDecoder dictionaryDecoder;

    public MessageHandler(final DictionaryAcceptor dictionaryAcceptor)
    {
        this.dictionaryDecoder = new DictionaryDecoder(dictionaryAcceptor);
    }

    @Override
    public ControlledFragmentHandler.Action onMessage(
        final DirectBuffer buffer,
        final int offset,
        final int length,
        final int libraryId,
        final Session session,
        final int sequenceIndex,
        final int messageType,
        final long timestampInNs,
        final long position)
    {
        string.wrap(buffer);
        dictionaryDecoder.onMessage(string, offset, length, messageType);
        return CONTINUE;
    }

    @Override
    public void onTimeout(final int libraryId, final Session session)
    {
    }

    @Override
    public void onSlowStatus(final int libraryId, final Session session, final boolean hasBecomeSlow)
    {
    }

    @Override
    public ControlledFragmentHandler.Action onDisconnect(
        final int libraryId,
        final Session session,
        final DisconnectReason reason)
    {
        return CONTINUE;
    }

    @Override
    public void onSessionStart(final Session session)
    {
        System.out.println("Session started");
    }
}
