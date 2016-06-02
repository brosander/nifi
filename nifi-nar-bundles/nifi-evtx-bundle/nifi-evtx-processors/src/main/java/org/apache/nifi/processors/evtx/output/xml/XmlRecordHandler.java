package org.apache.nifi.processors.evtx.output.xml;

import org.apache.nifi.processors.evtx.output.BadRecord;
import org.apache.nifi.processors.evtx.parser.Record;
import org.apache.nifi.processors.evtx.parser.XmlBxmlNodeVisitor;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Created by brosander on 6/2/16.
 */
public class XmlRecordHandler implements Consumer<Record> {
    private final XMLStreamWriter xmlStreamWriter;
    private final Consumer<BadRecord> badRecordConsumer;

    public XmlRecordHandler(XMLStreamWriter xmlStreamWriter, Consumer<BadRecord> badRecordConsumer) {
        this.xmlStreamWriter = xmlStreamWriter;
        this.badRecordConsumer = badRecordConsumer;
    }

    @Override
    public void accept(Record record) {
        try {
            new XmlBxmlNodeVisitor(xmlStreamWriter, record.getRootNode());
        } catch (IOException e) {
            badRecordConsumer.accept(new BadRecord(record, e));
        }
    }
}
