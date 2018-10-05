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

import io.aeron.logbuffer.ControlledFragmentHandler.Action;
import org.agrona.DirectBuffer;
import uk.co.real_logic.artio.ValidationError;
import uk.co.real_logic.artio.dictionary.IntDictionary;
import uk.co.real_logic.artio.fields.AsciiFieldFlyweight;
import uk.co.real_logic.artio.library.SessionHandler;
import uk.co.real_logic.artio.messages.DisconnectReason;
import uk.co.real_logic.artio.otf.MessageControl;
import uk.co.real_logic.artio.otf.OtfMessageAcceptor;
import uk.co.real_logic.artio.otf.OtfParser;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.util.AsciiBuffer;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import static io.aeron.logbuffer.ControlledFragmentHandler.Action.CONTINUE;
import static uk.co.real_logic.artio.Constants.TEST_REQ_ID;

public class TestReqIdFinder implements SessionHandler, OtfMessageAcceptor
{

    private final OtfParser parser = new OtfParser(this, new IntDictionary());
    private final MutableAsciiBuffer latestTestRequestMessageBuffer = new MutableAsciiBuffer(new byte[8 * 1024]);
    private int latestTestRequestMessageLength = 0;

    private String testReqId;

    public Action onMessage(
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
        testReqId = null;
        parser.onMessage(buffer, offset, length);

        if (testReqId != null)
        {
            latestTestRequestMessageBuffer.putBytes(0, buffer, offset, length);
            latestTestRequestMessageLength = length;
        }

        return CONTINUE;
    }

    public void onTimeout(final int libraryId, final Session session)
    {
    }

    public void onSlowStatus(final int libraryId, final Session session, final boolean hasBecomeSlow)
    {
    }

    public Action onDisconnect(final int libraryId, final Session session, final DisconnectReason reason)
    {
        return CONTINUE;
    }

    public void onSessionStart(final Session session)
    {
    }

    public MessageControl onNext()
    {
        return MessageControl.CONTINUE;
    }

    public MessageControl onComplete()
    {
        return MessageControl.CONTINUE;
    }

    public MessageControl onField(final int tag, final AsciiBuffer buffer, final int offset, final int length)
    {
        if (tag == TEST_REQ_ID)
        {
            this.testReqId = buffer.getAscii(offset, length);
        }

        return MessageControl.CONTINUE;
    }

    public MessageControl onGroupHeader(final int tag, final int numInGroup)
    {
        return MessageControl.CONTINUE;
    }

    public MessageControl onGroupBegin(final int tag, final int numInGroup, final int index)
    {
        return MessageControl.CONTINUE;
    }

    public MessageControl onGroupEnd(final int tag, final int numInGroup, final int index)
    {
        return MessageControl.CONTINUE;
    }

    public boolean onError(
        final ValidationError error,
        final int messageType,
        final int tagNumber,
        final AsciiFieldFlyweight value)
    {
        return false;
    }

    public String getLatestMessage()
    {
        return latestTestRequestMessageBuffer.getAscii(0, latestTestRequestMessageLength);
    }

    public String testReqId()
    {
        return testReqId;
    }
}
