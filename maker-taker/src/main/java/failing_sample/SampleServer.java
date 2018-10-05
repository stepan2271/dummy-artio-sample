/*
 * Copyright 2015-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package failing_sample;

import io.aeron.driver.MediaDriver;
import io.aeron.driver.MediaDriver.Context;
import org.agrona.IoUtil;
import org.agrona.concurrent.SigInt;
import uk.co.real_logic.artio.builder.ExampleMessageEncoder;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.*;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.validation.AuthenticationStrategy;
import uk.co.real_logic.artio.validation.MessageValidationStrategy;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.aeron.driver.ThreadingMode.SHARED;
import static java.util.Collections.singletonList;
import static uk.co.real_logic.artio.messages.SessionState.DISCONNECTED;

public final class SampleServer
{
    public static final String ACCEPTOR_COMP_ID = "acceptor";
    public static final String INITIATOR_COMP_ID = "initiator";

    private static Session session;

    public static void main(final String[] args) throws Exception
    {
        final MessageValidationStrategy validationStrategy = MessageValidationStrategy.targetCompId(ACCEPTOR_COMP_ID)
            .and(MessageValidationStrategy.senderCompId(Collections.singletonList(INITIATOR_COMP_ID)));

        final AuthenticationStrategy authenticationStrategy = AuthenticationStrategy.of(validationStrategy);

        // Static configuration lasts the duration of a FIX-Gateway instance
        final String aeronChannel = "aeron:udp?endpoint=127.0.0.1:10000";
        final EngineConfiguration configuration = new EngineConfiguration()
            .bindTo("127.0.0.1", 9999)
            .libraryAeronChannel(aeronChannel);
        configuration.authenticationStrategy(authenticationStrategy);

        cleanupOldLogFileDir(configuration);

        final Context context = new Context()
            .threadingMode(SHARED)
            .publicationTermBufferLength(65536)
            .dirDeleteOnStart(true);
        try (MediaDriver driver = MediaDriver.launch(context);
             FixEngine gateway = FixEngine.launch(configuration))
        {
            final LibraryConfiguration libraryConfiguration = new LibraryConfiguration();
            libraryConfiguration.authenticationStrategy(authenticationStrategy);

            // You register the new session handler - which is your application hook
            // that receives messages for new sessions
            libraryConfiguration
                .sessionAcquireHandler(SampleServer::onConnect)
                .scheduler(new DynamicLibraryScheduler())
                .sessionExistsHandler(new AcquiringSessionExistsHandler())
                .libraryAeronChannels(singletonList(aeronChannel));

            try (FixLibrary library = SampleUtil.blockingConnect(libraryConfiguration))
            {
                final AtomicBoolean running = new AtomicBoolean(true);
                SigInt.register(() -> running.set(false));
                char[] message = "sadsafdgrsgfdfe3242rdsf43253efrd532sr2143fd32".toCharArray();
                final ExampleMessageEncoder exampleMessageEncoder = new ExampleMessageEncoder().testReqID(message);
                while (running.get())
                {
                    int counter = 0;
                    int fragmentsRead;
                    do
                    {
                        fragmentsRead = library.poll(10);
                        counter++;
                        if (counter % 1_000 == 0)
                        {
                            System.out.println("Maker too much to read");
                        }
                    }
                    while (fragmentsRead > 0);

                    if (session != null && session.state() == DISCONNECTED)
                    {
                        break;
                    }
                    if (session != null && session.isActive())
                    {
                        session.send(exampleMessageEncoder);
                    }
                    Thread.sleep(100);
                }
            }
        }

        System.exit(0);
    }

    public static void cleanupOldLogFileDir(final EngineConfiguration configuration)
    {
        IoUtil.delete(new File(configuration.logFileDir()), true);
    }

    private static SessionHandler onConnect(final Session session, final boolean isSlow)
    {
        SampleServer.session = session;

        // Simple server just handles a single connection on a single thread
        // You choose how to manage threads for your application.

        return new SampleSessionHandler(session);
    }
}
