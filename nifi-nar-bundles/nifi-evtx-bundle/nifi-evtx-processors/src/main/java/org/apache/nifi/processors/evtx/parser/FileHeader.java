package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.CRC32;

/**
 * Created by brosander on 5/24/16.
 */
public class FileHeader extends Block implements Iterator<ChunkHeader> {
    public static final int CHUNK_SIZE = 65536;
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
    private int currentOffset;
    private int count = 0;

    public FileHeader(InputStream inputStream) throws IOException {
        super(new BinaryReader(inputStream, 4096));
        // Bytes will be checksummed
        BinaryReader binaryReader = getBinaryReader();
        CRC32 crc32 = new CRC32();
        crc32.update(binaryReader.peekBytes(120));

        magicString = binaryReader.readString(8);
        oldestChunk = binaryReader.readQWord();
        currentChunkNumber = binaryReader.readQWord();
        nextRecordNumber = binaryReader.readQWord();
        headerSize = binaryReader.readDWord();
        minorVersion = binaryReader.readWord();
        majorVersion = binaryReader.readWord();
        headerChunkSize = binaryReader.readWord();
        chunkCount = binaryReader.readWord();
        unused1 = binaryReader.readString(76);

        // Not part of checksum
        flags = binaryReader.readDWord();
        checksum = binaryReader.readDWord();

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
        currentOffset = 4096;

        init();
        initNext();
    }

    @Override
    protected int getHeaderLength() {
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
            if (count++ < chunkCount.intValue()) {
                int currentOffset = this.currentOffset;
                currentOffset += CHUNK_SIZE;
                next = new ChunkHeader(new BinaryReader(inputStream, CHUNK_SIZE), currentOffset);
            } else {
                next = null;
            }
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
