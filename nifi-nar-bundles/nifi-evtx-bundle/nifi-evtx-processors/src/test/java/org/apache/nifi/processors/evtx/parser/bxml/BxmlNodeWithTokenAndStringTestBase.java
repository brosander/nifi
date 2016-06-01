package org.apache.nifi.processors.evtx.parser.bxml;

import java.io.IOException;

import static org.mockito.Mockito.when;

/**
 * Created by brosander on 6/1/16.
 */
public abstract class BxmlNodeWithTokenAndStringTestBase extends BxmlNodeWithTokenTestBase {
    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.putDWord(0);
        when(chunkHeader.getOffset()).thenReturn(4096L);
        when(chunkHeader.getString(0)).thenReturn(getString());
    }

    protected abstract String getString();
}
