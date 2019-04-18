package new_connections_contention;

import failing_single_jvm.DummyTaker;
import failing_single_jvm.DummyUtils;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.ShutdownSignalBarrier;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.SessionConfiguration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static failing_single_jvm.SamplePingPong.getSessionConfiguration;
import static io.aeron.driver.ThreadingMode.SHARED;

public class NewConnectionsContention {

    private static final String BASE_LOG_DIR = System.getProperty("baseLogDir", "/var/log/skynet");
    private static final int NUMBER_OF_TAKERS = 20;

    public static void main(final String[] args) {
        startMediaDriver();
        final int portToLibraryTaker = 11111;
        final int portExternal = 11160;
        final EngineConfiguration configurationTaker = DummyUtils.getConfiguration(portToLibraryTaker);
        DummyUtils.cleanupOldLogFileDir(configurationTaker);
        FixEngine.launch(configurationTaker);
        final Thread[] threads = new Thread[NUMBER_OF_TAKERS];
        final AtomicBoolean shouldStart = new AtomicBoolean(false);
        for (int i = 0; i < NUMBER_OF_TAKERS; i++) {
            final SessionConfiguration sessionConfiguration = getSessionConfiguration(portExternal + i);
            final DummyTaker taker = new DummyTaker(sessionConfiguration);
            threads[i] = new Thread(() -> NewConnectionsContention.start(taker, shouldStart));
            threads[i].start();
        }
        shouldStart.set(true);
        new ShutdownSignalBarrier().await();
    }

    private static void start(final DummyTaker taker, final AtomicBoolean shouldStart)
    {
        while (!shouldStart.get())
        {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (!taker.start())
        {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 200));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private static void startMediaDriver()
    {
        final MediaDriver.Context context = new MediaDriver.Context()
                .threadingMode(SHARED)
                .dirDeleteOnStart(false);
        ArchivingMediaDriver.launch(context, createArchiveContext());
    }

    public static Archive.Context createArchiveContext()
    {
        return new Archive.Context()
                .threadingMode(ArchiveThreadingMode.SHARED)
                .archiveDirectoryName(BASE_LOG_DIR + "/aeron-archive");
    }
}
