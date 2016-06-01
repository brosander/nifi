package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.bxml.NameStringNode;
import org.apache.nifi.processors.evtx.parser.bxml.TemplateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Created by brosander on 5/24/16.
 */
public class ChunkHeader extends Block implements Iterator<Record> {
    private static final Logger logger = LoggerFactory.getLogger(ChunkHeader.class);
    private final String magicString;
    private final UnsignedLong fileFirstRecordNumber;
    private final UnsignedLong fileLastRecordNumber;
    private final UnsignedLong logFirstRecordNumber;
    private final UnsignedLong logLastRecordNumber;
    private final UnsignedInteger headerSize;
    private final UnsignedInteger lastRecordOffset;
    private final UnsignedInteger nextRecordOffset;
    private final UnsignedInteger dataChecksum;
    private final String unused;
    private final UnsignedInteger headerChecksum;
    private final Map<UnsignedInteger, NameStringNode> nameStrings;
    private final Map<UnsignedInteger, TemplateNode> templateNodes;
    private final int chunkNumber;
    private Record record;
    private UnsignedLong recordNumber;

    public ChunkHeader(BinaryReader binaryReader, int headerOffset, int chunkNumber) throws IOException {
        super(binaryReader, headerOffset);
        this.chunkNumber = chunkNumber;
        CRC32 crc32 = new CRC32();
        crc32.update(binaryReader.peekBytes(120));

        magicString = binaryReader.readString(8);
        fileFirstRecordNumber = binaryReader.readQWord();
        fileLastRecordNumber = binaryReader.readQWord();
        logFirstRecordNumber = binaryReader.readQWord();
        logLastRecordNumber = binaryReader.readQWord();
        headerSize = binaryReader.readDWord();
        lastRecordOffset = binaryReader.readDWord();
        nextRecordOffset = binaryReader.readDWord();
        dataChecksum = binaryReader.readDWord();
        unused = binaryReader.readString(68);

        if (!"ElfChnk".equals(magicString)) {
            throw new IOException("Invalid magic string " + this);
        }

        headerChecksum = binaryReader.readDWord();

        // These are included into the checksum
        crc32.update(binaryReader.peekBytes(384));

        if (crc32.getValue() != headerChecksum.longValue()) {
            throw new IOException("Invalid checksum " + this);
        }
        if (lastRecordOffset.compareTo(UnsignedInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IOException("Last record offset too big to fit into signed integer");
        }

        nameStrings = new HashMap<>();
        for (int i = 0; i < 64; i++) {
            UnsignedInteger offset = binaryReader.readDWord();
            while (offset.compareTo(UnsignedInteger.ZERO) > 0) {
                if (offset.compareTo(UnsignedInteger.valueOf(Integer.MAX_VALUE)) > 1) {
                    throw new IOException("Invalid offset");
                }
                NameStringNode nameStringNode = new NameStringNode(new BinaryReader(binaryReader, offset.intValue()), this);
                nameStrings.put(offset, nameStringNode);
                offset = nameStringNode.getNextOffset();
            }
        }

        templateNodes = new HashMap<>();
        for (int i = 0; i < 32; i++) {
            UnsignedInteger offset = binaryReader.readDWord();
            while (offset.compareTo(UnsignedInteger.ZERO) > 0) {
                if (offset.compareTo(UnsignedInteger.valueOf(Integer.MAX_VALUE)) > 1) {
                    throw new IOException("Invalid offset");
                }
                int token = new BinaryReader(binaryReader, offset.intValue() - 10).read();
                if (token != 0x0c) {
                    logger.warn("Unexpected token when parsing template at offset " + offset.intValue());
                    break;
                }
                BinaryReader templateReader = new BinaryReader(binaryReader, offset.intValue() - 4);
                UnsignedInteger pointer = templateReader.readDWord();
                if (!offset.equals(pointer)) {
                    logger.warn("Invalid pointer when parsing template at offset " + offset.intValue());
                    break;
                }
                TemplateNode templateNode = new TemplateNode(templateReader, this);
                templateNodes.put(offset, templateNode);
                offset = templateNode.getNextOffset();
            }
        }
        crc32 = new CRC32();
        crc32.update(binaryReader.peekBytes(nextRecordOffset.intValue() - 512));
        if (crc32.getValue() != dataChecksum.longValue()) {
            throw new IOException("Invalid data checksum " + this);
        }
        initNext();
    }

    public NameStringNode addNameStringNode(UnsignedInteger offset, BinaryReader binaryReader) throws IOException {
        NameStringNode nameStringNode = new NameStringNode(binaryReader, this);
        nameStrings.put(offset, nameStringNode);
        return nameStringNode;
    }

    public TemplateNode addTemplateNode(UnsignedInteger offset, BinaryReader binaryReader) throws IOException {
        TemplateNode templateNode = new TemplateNode(binaryReader, this);
        templateNodes.put(offset, templateNode);
        return templateNode;
    }

    public TemplateNode getTemplateNode(UnsignedInteger offset) {
        return templateNodes.get(offset);
    }

    @Override
    public String toString() {
        return "ChunkHeader{" +
                "magicString='" + magicString + '\'' +
                ", fileFirstRecordNumber=" + fileFirstRecordNumber +
                ", fileLastRecordNumber=" + fileLastRecordNumber +
                ", logFirstRecordNumber=" + logFirstRecordNumber +
                ", logLastRecordNumber=" + logLastRecordNumber +
                ", headerSize=" + headerSize +
                ", lastRecordOffset=" + lastRecordOffset +
                ", nextRecordOffset=" + nextRecordOffset +
                ", dataChecksum=" + dataChecksum +
                ", unused='" + unused + '\'' +
                ", headerChecksum=" + headerChecksum +
                '}';
    }

    private void initNext() {
        try {
            if (fileLastRecordNumber.equals(recordNumber)) {
                record = null;
                return;
            }
            record = new Record(getBinaryReader(), this);
            recordNumber = record.getRecordNum();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return record != null;
    }

    public String getString(UnsignedInteger offset) {
        NameStringNode nameStringNode = nameStrings.get(offset);
        if (nameStringNode == null) {
            return null;
        }
        return nameStringNode.getString();
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    @Override
    public Record next() {
        Record current = this.record;
        initNext();
        return current;
    }
}
