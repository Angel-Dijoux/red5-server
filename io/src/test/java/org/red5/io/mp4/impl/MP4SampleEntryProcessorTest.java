package org.red5.io.mp4.impl;

import junit.framework.TestCase;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4SampleEntryProcessorTest extends TestCase {

    private static Logger log = LoggerFactory.getLogger(MP4SampleEntryProcessorTest.class);

    @Test
    public void testDecodeStblBoxStss() {

        SyncSamplesBox stss = SyncSamplesBox.createSyncSamplesBox(new int[] { 1, 5, 10 });

        MP4TrackInfo trackInfo = new MP4TrackInfo();
        boolean isAudio = false, isVideo = true;

        MP4SampleEntryProcessor.decodeStblBox(stss, trackInfo, isAudio, isVideo, 0);

        int[] syncSamples = trackInfo.getSyncSamples();
        assertNotNull("Sync samples array should not be null", syncSamples);
        assertEquals("Should have 3 sync samples", 3, syncSamples.length);
        assertEquals("Second sync sample should be 5", 5, syncSamples[1]);
        log.debug("Sync samples => {}", (Object) syncSamples);
    }

    @Test
    public void testDecodeStblBoxStco() {

        ChunkOffsetsBox stco = ChunkOffsetsBox.createChunkOffsetsBox(new long[] { 100, 200, 300 });

        MP4TrackInfo trackInfo = new MP4TrackInfo();
        boolean isAudio = false, isVideo = true;

        MP4SampleEntryProcessor.decodeStblBox(stco, trackInfo, isAudio, isVideo, 0);

        long[] videoOffsets = trackInfo.getVideoChunkOffsets();
        assertNotNull("Video chunk offsets should not be null", videoOffsets);
        assertEquals("Expected 3 offsets", 3, videoOffsets.length);
        assertEquals("First offset should be 100", 100, videoOffsets[0]);
    }

    @Test
    public void testDecodeStblBoxStts() {

        TimeToSampleEntry entry = new TimeToSampleEntry(10, 1024);
        TimeToSampleBox stts = TimeToSampleBox.createTimeToSampleBox(new TimeToSampleEntry[] { entry });

        MP4TrackInfo trackInfo = new MP4TrackInfo();
        boolean isAudio = true, isVideo = false;

        MP4SampleEntryProcessor.decodeStblBox(stts, trackInfo, isAudio, isVideo, 0);
        assertEquals("Audio sample duration should be 1024", 1024, trackInfo.getAudioSampleDuration());
        log.debug("Audio sample duration => {}", trackInfo.getAudioSampleDuration());
    }
}
