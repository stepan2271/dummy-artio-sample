package failing_single_jvm;

import io.aeron.driver.MediaDriver;
import org.agrona.IoUtil;
import uk.co.real_logic.artio.builder.LogonEncoder;
import uk.co.real_logic.artio.builder.LogoutEncoder;
import uk.co.real_logic.artio.engine.DefaultEngineScheduler;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.session.SessionCustomisationStrategy;
import uk.co.real_logic.artio.session.SessionIdStrategy;
import uk.co.real_logic.artio.validation.SessionPersistenceStrategy;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;

import static io.aeron.driver.ThreadingMode.SHARED;

public class DummyUtils
{
    public static final boolean NTPRO_RESET_SEQ_NUM = true;
    static final int COUNTER_MASK = ((pow2(20)) - 1);

    public static FixLibrary blockingConnect(final LibraryConfiguration configuration)
    {
        final FixLibrary library = FixLibrary.connect(configuration);
        final long start = Instant.now().toEpochMilli();
        System.out.println(LocalDateTime.now() + " " + library.libraryId() + " start connecting to engine");
        while (!library.isConnected())
        {
            library.poll(1);
            if (Instant.now().toEpochMilli() - start > 60_000)
            {
                throw new RuntimeException(
                    LocalDateTime.now() + " " + library.libraryId() + " failed to connect after 1 minute");
            }
            Thread.yield();
        }
        return library;
    }

    public static String buildChannelString(final int port)
    {
        return "aeron:udp?endpoint=localhost:" + port;
    }

    public static int pow2(final int power)
    {
        return 1 << power;
    }

    public static MediaDriver startDefaultMediaDriver()
    {
        final MediaDriver.Context context = new MediaDriver.Context()
            .threadingMode(SHARED)
            .publicationTermBufferLength(65536)
            .dirDeleteOnStart(true);
        return MediaDriver.launch(context);
    }

    public static void cleanupOldLogFileDir(final EngineConfiguration configuration)
    {
        IoUtil.delete(new File(configuration.logFileDir()), true);
    }

    public static EngineConfiguration getConfiguration(final int port)
    {
        return (EngineConfiguration) new EngineConfiguration()
                .scheduler(new DefaultEngineScheduler())
                .sessionPersistenceStrategy(SessionPersistenceStrategy.alwaysIndexed())
                .libraryAeronChannel(DummyUtils.buildChannelString(port))
                .sessionIdStrategy(SessionIdStrategy.senderAndTarget());
    }
}
