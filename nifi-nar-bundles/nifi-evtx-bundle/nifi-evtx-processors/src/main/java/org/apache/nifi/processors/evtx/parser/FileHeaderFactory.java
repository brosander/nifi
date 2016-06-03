package org.apache.nifi.processors.evtx.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 6/3/16.
 */
public interface FileHeaderFactory {
    FileHeader create(InputStream inputStream) throws IOException;
}
