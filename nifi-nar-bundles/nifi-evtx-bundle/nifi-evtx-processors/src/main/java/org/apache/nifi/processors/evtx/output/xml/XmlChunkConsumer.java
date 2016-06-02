package org.apache.nifi.processors.evtx.output.xml;

import org.apache.nifi.processors.evtx.output.BadRecord;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import javax.xml.stream.XMLStreamWriter;
import java.util.function.Consumer;

/**
 * Created by brosander on 6/2/16.
 */
public class XmlChunkConsumer implements Consumer<ChunkHeader> {
    private final XMLStreamWriter xmlStreamWriter;
    private final Consumer<BadRecord> badRecordConsumer;

    public XmlChunkConsumer(XMLStreamWriter xmlStreamWriter, Consumer<BadRecord> badRecordConsumer) {
        this.xmlStreamWriter = xmlStreamWriter;
        this.badRecordConsumer = badRecordConsumer;
    }

    @Override
    public void accept(ChunkHeader chunkHeader) {
        chunkHeader.forEachRemaining(new XmlRecordHandler(xmlStreamWriter, badRecordConsumer));
    }
}
