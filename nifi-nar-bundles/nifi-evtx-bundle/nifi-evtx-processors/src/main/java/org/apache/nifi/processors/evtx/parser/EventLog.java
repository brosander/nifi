package org.apache.nifi.processors.evtx.parser;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/24/16.
 */
public class EventLog implements Closeable {
    private final InputStream inputStream;
    private final FileHeader fileHeader;

    public EventLog(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        fileHeader = new FileHeader(inputStream);
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
