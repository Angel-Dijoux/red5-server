
package org.red5.io.mp4.impl;

import org.jcodec.common.io.SeekableByteChannel;
import org.red5.io.IoConstants;
import org.red5.io.mp4.MP4Frame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MP4FrameAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(MP4FrameAnalyzer.class);

    private final MP4TrackInfo trackInfo;

    private final SeekableByteChannel dataSource;

    public MP4FrameAnalyzer(MP4TrackInfo trackInfo, SeekableByteChannel dataSource) {
        this.trackInfo = trackInfo;
        this.dataSource = dataSource;
    }

    public List<MP4Frame> analyzeFrames() {
        List<MP4Frame> frames = new ArrayList<>();
        trackInfo.setTimePosMap(new HashMap<>());
        trackInfo.setSamplePosMap(new HashMap<>());

        if (trackInfo.hasVideo() && trackInfo.getVideoSamplesToChunks() != null) {
            analyzeVideoFrames(frames);
        }

        if (trackInfo.hasAudio() && trackInfo.getAudioSamplesToChunks() != null) {
            analyzeAudioFrames(frames);
        }

        Collections.sort(frames);
        log.debug("analyzeFrames() total frames: {}", frames.size());
        return frames;
    }

    private void analyzeVideoFrames(List<MP4Frame> frames) {
        log.debug("Analyzing video frames");
        int[] videoSamples = trackInfo.getVideoSamples();
        long[] videoChunkOffsets = trackInfo.getVideoChunkOffsets();
        double videoTimeScale = trackInfo.getVideoTimeScale();
        long videoSampleDuration = trackInfo.getVideoSampleDuration();
        int[] syncSamples = trackInfo.getSyncSamples();

        int sampleIndex = 1;
        var cttsList = trackInfo.getCompositionTimes();
        int cttsPos = 0;
        int cttsCount = 0;
        int cttsOffset = 0;

        if (!cttsList.isEmpty()) {
            cttsOffset = cttsList.get(cttsPos).getOffset();
            cttsCount = cttsList.get(cttsPos).getCount();
        }

        var videoStsc = trackInfo.getVideoSamplesToChunks();
        for (int i = 0; i < videoStsc.length; i++) {
            var record = videoStsc[i];
            long firstChunk = record.getFirst();
            long lastChunk = (i < videoStsc.length - 1) ? videoStsc[i + 1].getFirst() - 1 : videoChunkOffsets.length;

            for (long chunk = firstChunk; chunk <= lastChunk; chunk++) {
                long sampleCount = record.getCount();
                long pos = videoChunkOffsets[(int) (chunk - 1)];
                while (sampleCount > 0 && sampleIndex <= videoSamples.length) {
                    int curSample = sampleIndex++;
                    double ts = (videoSampleDuration * (curSample - 1)) / videoTimeScale;

                    boolean keyframe = false;
                    if (syncSamples != null) {

                        for (int sync : syncSamples) {
                            if (sync == curSample) {
                                keyframe = true;
                                break;
                            }
                        }
                    }

                    // handle ctts
                    int timeOffset = cttsOffset;
                    cttsCount--;
                    if (cttsCount == 0) {
                        cttsPos++;
                        if (cttsPos < cttsList.size()) {
                            cttsOffset = cttsList.get(cttsPos).getOffset();
                            cttsCount = cttsList.get(cttsPos).getCount();
                        }
                    }

                    int sampleSize = videoSamples[curSample - 1];
                    MP4Frame frame = new MP4Frame();
                    frame.setKeyFrame(keyframe);
                    frame.setOffset(pos);
                    frame.setSize(sampleSize);
                    frame.setTime(ts);
                    frame.setType(IoConstants.TYPE_VIDEO);

                    frame.setTimeOffset(timeOffset);

                    if (keyframe) {
                        int ms = (int) Math.round(ts * 1000.0);
                        trackInfo.getTimePosMap().put(ms, pos);
                    }
                    frames.add(frame);

                    pos += sampleSize;
                    sampleCount--;
                }
            }
        }
    }

    private void analyzeAudioFrames(List<MP4Frame> frames) {
        log.debug("Analyzing audio frames");
        int[] audioSamples = trackInfo.getAudioSamples();
        long audioSampleSize = trackInfo.getAudioSampleSize();
        long[] audioChunkOffsets = trackInfo.getAudioChunkOffsets();
        double audioTimeScale = trackInfo.getAudioTimeScale();
        long audioSampleDuration = trackInfo.getAudioSampleDuration();

        int sampleIndex = 1; // 1-based
        var audioStsc = trackInfo.getAudioSamplesToChunks();

        for (int i = 0; i < audioStsc.length; i++) {
            var record = audioStsc[i];
            long firstChunk = record.getFirst();
            long lastChunk = (i < audioStsc.length - 1) ? audioStsc[i + 1].getFirst() - 1 : audioChunkOffsets.length;

            for (long chunk = firstChunk; chunk <= lastChunk; chunk++) {
                long sampleCount = record.getCount();
                long pos = audioChunkOffsets[(int) (chunk - 1)];

                while (sampleCount > 0 && sampleIndex <= audioSamples.length) {
                    double ts = (audioSampleDuration * (sampleIndex - 1)) / audioTimeScale;
                    int size = audioSamples[sampleIndex - 1] != 0 ? audioSamples[sampleIndex - 1] : (int) audioSampleSize;

                    MP4Frame frame = new MP4Frame();
                    frame.setOffset(pos);
                    frame.setSize(size);
                    frame.setTime(ts);
                    frame.setType(IoConstants.TYPE_AUDIO); // <---- Use IoConstants

                    frames.add(frame);
                    pos += size;
                    sampleCount--;
                    sampleIndex++;
                }
            }
        }
    }
}
