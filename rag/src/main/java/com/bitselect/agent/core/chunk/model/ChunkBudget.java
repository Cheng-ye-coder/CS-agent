package com.bitselect.agent.core.chunk.model;

/**
 * 分块预算：用户唯一可配的分块自由度
 */
public record ChunkBudget(int maxChars, int overlapChars, int rowsPerChunk, int toleranceFactor) {

    private static final int WHOLE_DOCUMENT = Integer.MAX_VALUE;

    private static final int DEFAULT_TOLERANCE_FACTOR = 3;

    private static final int DEFAULT_MAX_CHARS = 1024;

    public static final int OVERLAP_DIVISOR = 8;

    public static final int TOLERANCE_FACTOR_LIMIT = 8;

    public static final int MAX_CHARS_LIMIT = 8192;

    public static final int ROWS_PER_CHUNK_LIMIT = 1000;

    public ChunkBudget {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars 必须 > 0，实际 " + maxChars);
        }
        if (maxChars != WHOLE_DOCUMENT) {
            if (maxChars > MAX_CHARS_LIMIT) {
                throw new IllegalArgumentException("maxChars 不得超过 " + MAX_CHARS_LIMIT + "，实际 " + maxChars);
            }
            if (rowsPerChunk > ROWS_PER_CHUNK_LIMIT) {
                throw new IllegalArgumentException("rowsPerChunk 不得超过 " + ROWS_PER_CHUNK_LIMIT + "，实际 " + rowsPerChunk);
            }
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlapChars 必须落在 [0, maxChars) 区间，实际 " + overlapChars);
        }
        if (rowsPerChunk <= 0) {
            throw new IllegalArgumentException("rowsPerChunk 必须 > 0，实际 " + rowsPerChunk);
        }
        if (toleranceFactor < 1 || toleranceFactor > TOLERANCE_FACTOR_LIMIT) {
            throw new IllegalArgumentException("toleranceFactor 必须落在 [1, " + TOLERANCE_FACTOR_LIMIT
                    + "] 区间，实际 " + toleranceFactor);
        }
    }

    public ChunkBudget(int maxChars, int overlapChars, int rowsPerChunk) {
        this(maxChars, overlapChars, rowsPerChunk, DEFAULT_TOLERANCE_FACTOR);
    }

    public static int defaultOverlapFor(int maxChars) {
        return Math.max(0, Math.min(maxChars - 1, maxChars / OVERLAP_DIVISOR));
    }

    public static ChunkBudget defaults() {
        return new ChunkBudget(DEFAULT_MAX_CHARS, defaultOverlapFor(DEFAULT_MAX_CHARS), 50);
    }

    public static ChunkBudget wholeDocument() {
        return new ChunkBudget(WHOLE_DOCUMENT, 0, WHOLE_DOCUMENT);
    }

    public int toleranceChars() {
        return isWholeDocument() ? WHOLE_DOCUMENT : Math.min(maxChars * toleranceFactor, MAX_CHARS_LIMIT);
    }

    public boolean isWholeDocument() {
        return maxChars == WHOLE_DOCUMENT;
    }
}