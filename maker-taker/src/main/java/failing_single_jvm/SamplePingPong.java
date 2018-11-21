package failing_single_jvm;

import io.aeron.driver.MediaDriver;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.SessionConfiguration;

public class SamplePingPong
{
    public static void main(final String[] args) throws InterruptedException
    {
        final MediaDriver driver = DummyUtils.startDefaultMediaDriver();
        final int portToLibraryTaker = 11111;
        final int portToLibraryMaker = 11113;
        final int portExternal = 11160;
        final EngineConfiguration configurationMaker =
            DummyUtils.getConfiguration(portToLibraryMaker).bindTo("localhost", portExternal);
        DummyUtils.cleanupOldLogFileDir(configurationMaker);
        final EngineConfiguration configurationTaker = DummyUtils.getConfiguration(portToLibraryTaker);
        DummyUtils.cleanupOldLogFileDir(configurationTaker);
        FixEngine.launch(configurationTaker);
        FixEngine.launch(configurationMaker);
        final SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
            .address("localhost", portExternal)
            .targetCompId("MAKER")
            .senderCompId("TAKER")
            .resetSeqNum(DummyUtils.NTPRO_RESET_SEQ_NUM)
            .build();
        final DummyTaker taker = new DummyTaker(sessionConfiguration);
        final DummyMaker maker = new DummyMaker("INITIATOR", "ACCEPTOR");
        final boolean makerStarted = maker.start();
        assert makerStarted;
        final boolean startedTaker = taker.start();
        assert startedTaker;
        Thread.sleep(Integer.MAX_VALUE);
    }
}
