package org.apache.nifi.processors.evtx.output.xml;

import org.apache.nifi.processors.evtx.output.BadRecord;
import org.apache.nifi.processors.evtx.parser.FileHeader;

import javax.xml.stream.XMLStreamWriter;
import java.util.function.Consumer;

/**
 * Created by brosander on 6/2/16.
 */
public class XmlFileConxumer implements Consumer<FileHeader> {
    private final XMLStreamWriter xmlStreamWriter;
    private final Consumer<BadRecord> badRecordConsumer;

    public XmlFileConxumer(XMLStreamWriter xmlStreamWriter, Consumer<BadRecord> badRecordConsumer) {
        this.xmlStreamWriter = xmlStreamWriter;
        this.badRecordConsumer = badRecordConsumer;
    }

    @Override
    public void accept(FileHeader fileHeader) {
        fileHeader.forEachRemaining(new XmlChunkConsumer(xmlStreamWriter, badRecordConsumer));
    }
}
