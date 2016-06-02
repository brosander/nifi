package org.apache.nifi.processors.evtx.output;

import org.apache.nifi.processors.evtx.parser.Record;

/**
 * Created by brosander on 6/2/16.
 */
public class BadRecord {
    private final Record record;
    private final Exception exception;

    public BadRecord(Record record, Exception exception) {
        this.record = record;
        this.exception = exception;
    }

    public Record getRecord() {
        return record;
    }

    public Exception getException() {
        return exception;
    }
}
