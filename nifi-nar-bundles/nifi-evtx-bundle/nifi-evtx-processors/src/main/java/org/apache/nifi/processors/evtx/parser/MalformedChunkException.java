package org.apache.nifi.processors.evtx.parser;

/**
 * Created by brosander on 6/3/16.
 */
public class MalformedChunkException extends Exception {
    private final long offset;
    private final int chunkNum;
    private byte[] badChunk;

    public MalformedChunkException(String message, Throwable cause, long offset, int chunkNum, byte[] badChunk) {
        super(message, cause);
        this.offset = offset;
        this.chunkNum = chunkNum;
        this.badChunk = badChunk;
    }

    public void clearBytes() {
        this.badChunk = null;
    }

    public byte[] getBadChunk() {
        return badChunk;
    }

    public int getChunkNum() {
        return chunkNum;
    }
}
