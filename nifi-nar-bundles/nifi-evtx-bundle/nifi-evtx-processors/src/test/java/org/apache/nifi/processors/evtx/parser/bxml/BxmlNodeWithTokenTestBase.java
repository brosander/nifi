package org.apache.nifi.processors.evtx.parser.bxml;

import java.io.IOException;

/**
 * Created by brosander on 6/1/16.
 */
public abstract class BxmlNodeWithTokenTestBase extends BxmlNodeTestBase {
    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.put(getToken());
    }

    protected abstract byte getToken();
}
