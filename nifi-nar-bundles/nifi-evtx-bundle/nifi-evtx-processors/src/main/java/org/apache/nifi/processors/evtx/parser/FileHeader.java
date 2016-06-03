package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

/**
 * Created by brosander on 5/24/16.
 */
public class FileHeader extends Block {
    public static final int CHUNK_SIZE = 65536;
    public static final String ELF_FILE = "ElfFile";
    private final String magicString;
    private final UnsignedLong oldestChunk;
    private final UnsignedLong currentChunkNumber;
    private final UnsignedLong nextRecordNumber;
    private final UnsignedInteger headerSize;
    private final int minorVersion;
    private final int majorVersion;
    private final int headerChunkSize;
    private final int chunkCount;
    private final String unused1;
    private final UnsignedInteger flags;
    private final UnsignedInteger checksum;
    private final InputStream inputStream;
    private long currentOffset;
    private int count = 0;

    public FileHeader(InputStream inputStream) throws IOException {
        super(new BinaryReader(inputStream, 4096));
        // Bytes will be checksummed
        BinaryReader binaryReader = getBinaryReader();
        CRC32 crc32 = new CRC32();
        crc32.update(binaryReader.peekBytes(120));

        magicString = binaryReader.readString(8);
        if (!ELF_FILE.equals(magicString)) {
            throw new IOException("Invalid magic string. Expected " + ELF_FILE + " got " + magicString);
        }
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
        NumberUtil.intValueExpected(minorVersion, 1, "Invalid minor version.");
        NumberUtil.intValueExpected(majorVersion, 3, "Invalid minor version.");
        NumberUtil.intValueExpected(headerChunkSize, 4096, "Invalid header chunk size.");
        this.inputStream = inputStream;
        currentOffset = 4096;

        init();
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

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getHeaderChunkSize() {
        return headerChunkSize;
    }

    public int getChunkCount() {
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

    public boolean hasNext() {
        return count < chunkCount;
    }

    public ChunkHeader next() throws IOException, MalformedChunkException {
        if (count < chunkCount) {
            long currentOffset = this.currentOffset;
            currentOffset += CHUNK_SIZE;
            BinaryReader binaryReader = new BinaryReader(inputStream, CHUNK_SIZE);
            try {
                return new ChunkHeader(binaryReader, currentOffset, count++);
            } catch (IOException e) {
                throw new MalformedChunkException("Malformed chunk, unable to parse", e, currentOffset, count, binaryReader.getBytes());
            }
        } else {
            return null;
        }
    }
}
