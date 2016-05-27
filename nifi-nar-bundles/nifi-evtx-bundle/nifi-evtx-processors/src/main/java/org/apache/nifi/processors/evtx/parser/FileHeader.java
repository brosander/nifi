package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.CRC32;

/**
 * Created by brosander on 5/24/16.
 */
public class FileHeader extends Block implements Iterator<ChunkHeader> {
    private final String magicString;
    private final UnsignedLong oldestChunk;
    private final UnsignedLong currentChunkNumber;
    private final UnsignedLong nextRecordNumber;
    private final UnsignedInteger headerSize;
    private final UnsignedInteger minorVersion;
    private final UnsignedInteger majorVersion;
    private final UnsignedInteger headerChunkSize;
    private final UnsignedInteger chunkCount;
    private final String unused1;
    private final UnsignedInteger flags;
    private final UnsignedInteger checksum;
    private final InputStream inputStream;
    private ChunkHeader next;

    public FileHeader(InputStream inputStream) throws IOException {
        super(inputStream, 0);
        // Bytes will be checksummed
        byte[] bytes = readBytes(120);

        // Not part of checksum
        flags = readDWord();
        checksum = readDWord();

        BinaryReader headerReader = new BinaryReader(new ByteArrayInputStream(bytes));
        magicString = headerReader.readString(8);
        oldestChunk = headerReader.readQWord();
        currentChunkNumber = headerReader.readQWord();
        nextRecordNumber = headerReader.readQWord();
        headerSize = headerReader.readDWord();
        minorVersion = headerReader.readWord();
        majorVersion = headerReader.readWord();
        headerChunkSize = headerReader.readWord();
        chunkCount = headerReader.readWord();
        unused1 = headerReader.readString(76);

        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        if (crc32.getValue() != checksum.longValue()) {
            throw new IOException("Invalid checksum");
        }
        if (minorVersion.intValue() != 1) {
            throw new IOException("Invalid minor version");
        }
        if (majorVersion.intValue() != 3) {
            throw new IOException("Invalid major version");
        }
        if (headerChunkSize.intValue() != 4096) {
            throw new IOException("Invalid header chunk size");
        }
        this.inputStream = inputStream;

        init();
        initNext();
    }

    @Override
    protected int getEndOfHeader() {
        return 4096;
    }

    public String getMagicString() {
        return magicString;
    }

    public UnsignedLong getOldestChunk() {
        return oldestChunk;
    }

    public UnsignedLong getCurrentChunkNumber() {
        return currentChunkNumber;
    }

    public UnsignedLong getNextRecordNumber() {
        return nextRecordNumber;
    }

    public UnsignedInteger getHeaderSize() {
        return headerSize;
    }

    public UnsignedInteger getMinorVersion() {
        return minorVersion;
    }

    public UnsignedInteger getMajorVersion() {
        return majorVersion;
    }

    public UnsignedInteger getHeaderChunkSize() {
        return headerChunkSize;
    }

    public UnsignedInteger getChunkCount() {
        return chunkCount;
    }

    public String getUnused1() {
        return unused1;
    }

    public UnsignedInteger getFlags() {
        return flags;
    }

    public UnsignedInteger getChecksum() {
        return checksum;
    }

    private void initNext() {
        try {
            long offset = getCurrentOffset();
            next = new ChunkHeader(new ByteArrayInputStream(readBytes(65536)), offset);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public ChunkHeader next() {
        ChunkHeader result = next;
        initNext();
        return result;
    }
}
