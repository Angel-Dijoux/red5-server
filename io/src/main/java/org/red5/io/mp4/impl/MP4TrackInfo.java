
package org.red5.io.mp4.impl;

import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;

import java.util.List;
import java.util.Map;

public class MP4TrackInfo {

    private boolean hasAudio;

    private boolean hasVideo;

    private String audioCodecId = "mp4a";

    private String videoCodecId = "avc1";

    private byte[] audioDecoderBytes;

    private byte[] videoDecoderBytes;

    private long duration;

    private long timeScale;

    private int width;

    private int height;

    private double audioTimeScale;

    private int audioChannels;

    private int audioCodecType = 1; // e.g. 1 = AAC LC

    private long audioSampleSize;

    private int[] audioSamples;

    private long[] audioChunkOffsets;

    private long audioSampleDuration = 1024;

    private double videoTimeScale;

    private int videoSampleCount;

    private double fps; // optional

    private int[] videoSamples;

    private long[] videoChunkOffsets;

    private long videoSampleDuration = 125;

    private int avcLevel;

    private int avcProfile;

    private List<CompositionOffsetsBox.Entry> compositionTimes;

    private SampleToChunkBox.SampleToChunkEntry[] audioSamplesToChunks;

    private SampleToChunkBox.SampleToChunkEntry[] videoSamplesToChunks;

    private int[] syncSamples;

    private Map<Integer, Long> timePosMap;

    private Map<Integer, Long> samplePosMap;

    public MP4TrackInfo() {
    }

    public MP4TrackInfo(boolean hasAudio, boolean hasVideo, String audioCodecId, String videoCodecId, byte[] audioDecoderBytes, byte[] videoDecoderBytes, long duration, long timeScale, int width, int height, double audioTimeScale, int audioChannels, int audioCodecType, long audioSampleSize, int[] audioSamples, long[] audioChunkOffsets, long audioSampleDuration, double videoTimeScale, int videoSampleCount, double fps,
            int[] videoSamples, long[] videoChunkOffsets, long videoSampleDuration, int avcLevel, int avcProfile, List<CompositionOffsetsBox.Entry> compositionTimes, SampleToChunkBox.SampleToChunkEntry[] audioSamplesToChunks, SampleToChunkBox.SampleToChunkEntry[] videoSamplesToChunks, int[] syncSamples, Map<Integer, Long> timePosMap, Map<Integer, Long> samplePosMap) {

        this.hasAudio = hasAudio;
        this.hasVideo = hasVideo;
        this.audioCodecId = audioCodecId;
        this.videoCodecId = videoCodecId;
        this.audioDecoderBytes = audioDecoderBytes;
        this.videoDecoderBytes = videoDecoderBytes;
        this.duration = duration;
        this.timeScale = timeScale;
        this.width = width;
        this.height = height;
        this.audioTimeScale = audioTimeScale;
        this.audioChannels = audioChannels;
        this.audioCodecType = audioCodecType;
        this.audioSampleSize = audioSampleSize;
        this.audioSamples = audioSamples;
        this.audioChunkOffsets = audioChunkOffsets;
        this.audioSampleDuration = audioSampleDuration;
        this.videoTimeScale = videoTimeScale;
        this.videoSampleCount = videoSampleCount;
        this.fps = fps;
        this.videoSamples = videoSamples;
        this.videoChunkOffsets = videoChunkOffsets;
        this.videoSampleDuration = videoSampleDuration;
        this.avcLevel = avcLevel;
        this.avcProfile = avcProfile;
        this.compositionTimes = compositionTimes;
        this.audioSamplesToChunks = audioSamplesToChunks;
        this.videoSamplesToChunks = videoSamplesToChunks;
        this.syncSamples = syncSamples;
        this.timePosMap = timePosMap;
        this.samplePosMap = samplePosMap;
    }

    public boolean hasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public boolean hasVideo() {
        return hasVideo;
    }

    public void setHasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    public String getAudioCodecId() {
        return audioCodecId;
    }

    public void setAudioCodecId(String audioCodecId) {
        this.audioCodecId = audioCodecId;
    }

    public String getVideoCodecId() {
        return videoCodecId;
    }

    public void setVideoCodecId(String videoCodecId) {
        this.videoCodecId = videoCodecId;
    }

    public byte[] getAudioDecoderBytes() {
        return audioDecoderBytes;
    }

    public void setAudioDecoderBytes(byte[] audioDecoderBytes) {
        this.audioDecoderBytes = audioDecoderBytes;
    }

    public byte[] getVideoDecoderBytes() {
        return videoDecoderBytes;
    }

    public void setVideoDecoderBytes(byte[] videoDecoderBytes) {
        this.videoDecoderBytes = videoDecoderBytes;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getTimeScale() {
        return timeScale;
    }

    public void setTimeScale(long timeScale) {
        this.timeScale = timeScale;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getAudioTimeScale() {
        return audioTimeScale;
    }

    public void setAudioTimeScale(double audioTimeScale) {
        this.audioTimeScale = audioTimeScale;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
    }

    public int getAudioCodecType() {
        return audioCodecType;
    }

    public void setAudioCodecType(int audioCodecType) {
        this.audioCodecType = audioCodecType;
    }

    public long getAudioSampleSize() {
        return audioSampleSize;
    }

    public void setAudioSampleSize(long audioSampleSize) {
        this.audioSampleSize = audioSampleSize;
    }

    public int[] getAudioSamples() {
        return audioSamples;
    }

    public void setAudioSamples(int[] audioSamples) {
        this.audioSamples = audioSamples;
    }

    public long[] getAudioChunkOffsets() {
        return audioChunkOffsets;
    }

    public void setAudioChunkOffsets(long[] audioChunkOffsets) {
        this.audioChunkOffsets = audioChunkOffsets;
    }

    public long getAudioSampleDuration() {
        return audioSampleDuration;
    }

    public void setAudioSampleDuration(long audioSampleDuration) {
        this.audioSampleDuration = audioSampleDuration;
    }

    public double getVideoTimeScale() {
        return videoTimeScale;
    }

    public void setVideoTimeScale(double videoTimeScale) {
        this.videoTimeScale = videoTimeScale;
    }

    public int getVideoSampleCount() {
        return videoSampleCount;
    }

    public void setVideoSampleCount(int videoSampleCount) {
        this.videoSampleCount = videoSampleCount;
    }

    public double getFps() {
        return fps;
    }

    public void setFps(double fps) {
        this.fps = fps;
    }

    public int[] getVideoSamples() {
        return videoSamples;
    }

    public void setVideoSamples(int[] videoSamples) {
        this.videoSamples = videoSamples;
    }

    public long[] getVideoChunkOffsets() {
        return videoChunkOffsets;
    }

    public void setVideoChunkOffsets(long[] videoChunkOffsets) {
        this.videoChunkOffsets = videoChunkOffsets;
    }

    public long getVideoSampleDuration() {
        return videoSampleDuration;
    }

    public void setVideoSampleDuration(long videoSampleDuration) {
        this.videoSampleDuration = videoSampleDuration;
    }

    public int getAvcLevel() {
        return avcLevel;
    }

    public void setAvcLevel(int avcLevel) {
        this.avcLevel = avcLevel;
    }

    public int getAvcProfile() {
        return avcProfile;
    }

    public void setAvcProfile(int avcProfile) {
        this.avcProfile = avcProfile;
    }

    public List<CompositionOffsetsBox.Entry> getCompositionTimes() {
        // returning an empty list instead of null
        return (compositionTimes != null) ? compositionTimes : List.of();
    }

    public void setCompositionTimes(List<CompositionOffsetsBox.Entry> compositionTimes) {
        this.compositionTimes = compositionTimes;
    }

    public SampleToChunkBox.SampleToChunkEntry[] getAudioSamplesToChunks() {
        return audioSamplesToChunks;
    }

    public void setAudioSamplesToChunks(SampleToChunkBox.SampleToChunkEntry[] audioSamplesToChunks) {
        this.audioSamplesToChunks = audioSamplesToChunks;
    }

    public SampleToChunkBox.SampleToChunkEntry[] getVideoSamplesToChunks() {
        return videoSamplesToChunks;
    }

    public void setVideoSamplesToChunks(SampleToChunkBox.SampleToChunkEntry[] videoSamplesToChunks) {
        this.videoSamplesToChunks = videoSamplesToChunks;
    }

    public int[] getSyncSamples() {
        return syncSamples;
    }

    public void setSyncSamples(int[] syncSamples) {
        this.syncSamples = syncSamples;
    }

    public Map<Integer, Long> getTimePosMap() {
        return timePosMap;
    }

    public void setTimePosMap(Map<Integer, Long> timePosMap) {
        this.timePosMap = timePosMap;
    }

    public Map<Integer, Long> getSamplePosMap() {
        return samplePosMap;
    }

    public void setSamplePosMap(Map<Integer, Long> samplePosMap) {
        this.samplePosMap = samplePosMap;
    }

    /**
     * Detects the type of AAC (or possibly MP3) based on the first byte
     * in the audioDecoderBytes buffer.
     */
    public void determineAudioCodecType() {
        if (audioDecoderBytes == null || audioDecoderBytes.length == 0) {
            return;
        }
        byte audioCoderType = audioDecoderBytes[0];
        switch (audioCoderType) {
            case 0x02:
            case 0x11:
                audioCodecType = 1; // AAC LC
                break;
            case 0x01:
                audioCodecType = 0; // AAC Main
                break;
            case 0x03:
                audioCodecType = 2; // AAC SBR
                break;
            case 0x05:
            case 0x1d:
                audioCodecType = 3; // AAC HE
                break;
            case 0x20:
            case 0x21:
            case 0x22:
                audioCodecType = 33; // MP3
                audioCodecId = "mp3";
                break;
            default:
                // Unknown / unhandled
                break;
        }
    }
}
