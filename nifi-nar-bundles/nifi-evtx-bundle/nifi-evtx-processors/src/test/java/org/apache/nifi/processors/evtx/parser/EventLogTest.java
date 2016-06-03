package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 5/24/16.
 */
public class EventLogTest {
    /*@Test
    public void testParseHeader() throws IOException {
        try (FileInputStream inputStream = new FileInputStream("/Users/brosander/Downloads/winlogs/system-logs.evtx")) {
            FileHeader fileHeader = new FileHeader(inputStream);
            assertEquals("ElfFile", fileHeader.getMagicString());
            assertEquals(UnsignedLong.fromLongBits(0L), fileHeader.getOldestChunk());
            assertEquals(UnsignedLong.fromLongBits(33L), fileHeader.getCurrentChunkNumber());
            assertEquals(UnsignedLong.fromLongBits(4315L), fileHeader.getNextRecordNumber());
            assertEquals(UnsignedInteger.fromIntBits(128), fileHeader.getHeaderSize());
            assertEquals(UnsignedInteger.fromIntBits(1), fileHeader.getMinorVersion());
            assertEquals(UnsignedInteger.fromIntBits(3), fileHeader.getMajorVersion());
            assertEquals(UnsignedInteger.fromIntBits(4096), fileHeader.getHeaderChunkSize());
            assertEquals(UnsignedInteger.fromIntBits(34), fileHeader.getChunkCount());
            assertEquals("", fileHeader.getUnused1());
            assertEquals(UnsignedInteger.fromIntBits(0), fileHeader.getFlags());
            assertEquals(UnsignedInteger.valueOf(3575959108L), fileHeader.getChecksum());
            fileHeader.forEachRemaining(chunkHeader -> {
                chunkHeader.forEachRemaining(record -> {
                    System.out.println(record);
                });
            });
        }
    }*/

//    @Test
    public void testXmlOutput() throws IOException, XMLStreamException, MalformedChunkException {
//        try (FileInputStream inputStream = new FileInputStream("/Users/brosander/Downloads/winlogs/system-logs.evtx")) {
        try (FileInputStream inputStream = new FileInputStream("/Users/brosander/Downloads/winlogs/winlogs/application-logs.evtx")) {
            FileHeader fileHeader = new FileHeader(inputStream);
            /*assertEquals("ElfFile", fileHeader.getMagicString());
            assertEquals(UnsignedLong.fromLongBits(0L), fileHeader.getOldestChunk());
            assertEquals(UnsignedLong.fromLongBits(33L), fileHeader.getCurrentChunkNumber());
            assertEquals(UnsignedLong.fromLongBits(4315L), fileHeader.getNextRecordNumber());
            assertEquals(UnsignedInteger.fromIntBits(128), fileHeader.getHeaderSize());
            assertEquals(UnsignedInteger.fromIntBits(1), fileHeader.getMinorVersion());
            assertEquals(UnsignedInteger.fromIntBits(3), fileHeader.getMajorVersion());
            assertEquals(UnsignedInteger.fromIntBits(4096), fileHeader.getHeaderChunkSize());
            assertEquals(UnsignedInteger.fromIntBits(34), fileHeader.getChunkCount());
            assertEquals("", fileHeader.getUnused1());
            assertEquals(UnsignedInteger.fromIntBits(0), fileHeader.getFlags());
            assertEquals(UnsignedInteger.valueOf(3575959108L), fileHeader.getChecksum());*/
            XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(new FileOutputStream("/Users/brosander/Github/python-evtx/scripts/test.out"), "UTF-8");
            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("Events");
            try {
                while (fileHeader.hasNext()) {
                    ChunkHeader chunkHeader = fileHeader.next();
                    while (chunkHeader.hasNext()) {
                        new XmlBxmlNodeVisitor(xmlStreamWriter, chunkHeader.next().getRootNode());
                    }
                }
            } finally {
                xmlStreamWriter.writeEndElement();
                xmlStreamWriter.close();
            }
        }
    }
}
