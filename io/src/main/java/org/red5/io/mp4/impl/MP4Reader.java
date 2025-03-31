
package org.red5.io.mp4.impl;

import org.apache.mina.core.buffer.IoBuffer;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.impl.Tag;
import org.red5.io.mp4.MP4Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class MP4Reader implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(MP4Reader.class);

    public static final byte[] PREFIX_AUDIO_CONFIG_FRAME = { (byte) 0xaf, (byte) 0 };

    public static final byte[] PREFIX_AUDIO_FRAME = { (byte) 0xaf, (byte) 0x01 };

    public static final byte[] EMPTY_AAC = { 0x21, 0x10, 0x04, 0x60, (byte) 0x8c, 0x1c };

    public static final byte[] PREFIX_VIDEO_CONFIG_FRAME = { 0x17, 0, 0, 0, 0 };

    public static final byte[] PREFIX_VIDEO_KEYFRAME = { 0x17, 0x01 };

    public static final byte[] PREFIX_VIDEO_FRAME = { 0x27, 0x01 };

    private SeekableByteChannel dataSource;

    private MP4TrackInfo trackInfo = new MP4TrackInfo();

    private List<MP4Frame> frames;

    private final LinkedList<ITag> firstTags = new LinkedList<>();

    private final LinkedList<Integer> seekPoints = new LinkedList<>();

    private final Semaphore lock = new Semaphore(1, true);

    private int currentFrame = 0;

    private int prevFrameSize = 0;

    private int prevVideoTS = -1;

    public MP4Reader() {
    }

    public MP4Reader(File file) throws IOException {
        initialize(file);
    }

    private void initialize(File file) throws IOException {
        if (file == null || !file.exists() || !file.canRead()) {
            log.warn("Invalid file provided");
            return;
        }

        dataSource = NIOUtils.readableChannel(file);

        MP4Parser parser = new MP4Parser(trackInfo);
        parser.parseMovie(dataSource);

        MP4FrameAnalyzer analyzer = new MP4FrameAnalyzer(trackInfo, dataSource);
        frames = analyzer.analyzeFrames();

        MP4MetadataTagCreator metadataCreator = new MP4MetadataTagCreator(trackInfo, seekPoints);
        ITag metaTag = metadataCreator.createFileMeta();
        firstTags.add(metaTag);

        createPreStreamingTags(0, false);
    }

    private void createPreStreamingTags(int timestamp, boolean clear) {
        if (clear) {
            firstTags.clear();
        }
        MP4PreStreamingTagCreator preTags = new MP4PreStreamingTagCreator(trackInfo);

        // If we have video:
        if (trackInfo.hasVideo()) {
            ITag videoTag = preTags.createVideoConfigTag(timestamp);
            if (videoTag != null) {
                firstTags.add(videoTag);
            }
        }
        // If we have audio:
        if (trackInfo.hasAudio()) {
            ITag audioTag = preTags.createAudioConfigTag(timestamp);
            if (audioTag != null) {
                firstTags.add(audioTag);
            }
        }
    }

    @Override
    public boolean hasMoreTags() {
        return currentFrame < (frames != null ? frames.size() : 0);
    }

    @Override
    public ITag readTag() {
        if (frames == null || frames.isEmpty()) {
            log.warn("No frames available");
            return null;
        }
        try {
            lock.acquire();

            if (!firstTags.isEmpty()) {
                return firstTags.removeFirst();
            }

            MP4Frame frame = frames.get(currentFrame);
            ITag tag = createTagFromFrame(frame);
            currentFrame++;
            prevFrameSize = tag.getBodySize();
            return tag;
        } catch (InterruptedException e) {
            log.error("Interrupted acquiring lock", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            lock.release();
        }
    }

    private ITag createTagFromFrame(MP4Frame frame) {
        // The FLV tag data
        int sampleSize = frame.getSize();
        int time = (int) Math.round(frame.getTime() * 1000.0);
        long samplePos = frame.getOffset();
        byte type = frame.getType();

        int pad = (type == IoConstants.TYPE_AUDIO) ? 2 : 5;
        ByteBuffer data = ByteBuffer.allocate(sampleSize + pad);

        try {
            if (type == IoConstants.TYPE_VIDEO) {

                data.put(frame.isKeyFrame() ? PREFIX_VIDEO_KEYFRAME : PREFIX_VIDEO_FRAME);

                int timeOffset = (prevVideoTS != -1) ? (time - prevVideoTS) : 0;
                data.put((byte) ((timeOffset >>> 16) & 0xff));
                data.put((byte) ((timeOffset >>> 8) & 0xff));
                data.put((byte) (timeOffset & 0xff));
                prevVideoTS = time;
            } else {

                data.put(PREFIX_AUDIO_FRAME);
            }

            dataSource.setPosition(samplePos);
            dataSource.read(data);
        } catch (IOException e) {
            log.error("Error reading sample data", e);
        }

        IoBuffer payload = IoBuffer.wrap(data.array());
        return new Tag(type, time, payload.limit(), payload, prevFrameSize);
    }

    @Override
    public long getTotalBytes() {
        if (dataSource != null) {
            try {
                return dataSource.size();
            } catch (IOException e) {
                log.error("getTotalBytes error", e);
            }
        }
        return 0;
    }

    @Override
    public long getBytesRead() {
        if (dataSource != null) {
            try {
                return dataSource.position();
            } catch (IOException e) {
                log.error("getBytesRead error", e);
            }
        }
        return 0;
    }

    @Override
    public boolean hasVideo() {
        return trackInfo.hasVideo();
    }

    @Override
    public IStreamableFile getFile() {

        return null;
    }

    @Override
    public int getOffset() {

        return 0;
    }

    @Override
    public long getDuration() {
        return trackInfo.getDuration();
    }

    @Override
    public void position(long pos) {
        log.debug("Seeking to file offset: {}", pos);

        if (frames == null) {
            return;
        }
        int len = frames.size();
        for (int i = 0; i < len; i++) {
            MP4Frame f = frames.get(i);
            long offset = f.getOffset();
            if (offset >= pos && f.isKeyFrame()) {
                log.info("Found keyframe at index={} for offset={} => frame={}", i, pos, f);
                createPreStreamingTags((int) (f.getTime() * 1000), true);
                currentFrame = i;
                prevVideoTS = (int) (f.getTime() * 1000);
                break;
            }
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (IOException e) {
                log.error("Error closing channel", e);
            }
        }
        if (frames != null) {
            frames.clear();
            frames = null;
        }
    }

    @Override
    public KeyFrameMeta analyzeKeyFrames() {
        KeyFrameMeta result = new KeyFrameMeta();
        result.duration = trackInfo.getDuration();
        result.audioOnly = trackInfo.hasAudio() && !trackInfo.hasVideo();

        if (result.audioOnly) {

            result.positions = new long[frames.size()];
            result.timestamps = new int[frames.size()];
            for (int i = 0; i < frames.size(); i++) {
                MP4Frame f = frames.get(i);
                f.setKeyFrame(true);
                result.positions[i] = f.getOffset();
                result.timestamps[i] = (int) Math.round(f.getTime() * 1000.0);
            }
        } else {

            if (!seekPoints.isEmpty()) {
                int count = seekPoints.size();
                result.positions = new long[count];
                result.timestamps = new int[count];
                Map<Integer, Long> timePosMap = trackInfo.getTimePosMap();
                for (int i = 0; i < count; i++) {
                    int ts = seekPoints.get(i);

                    result.positions[i] = timePosMap.getOrDefault(ts, 0L);
                    result.timestamps[i] = ts;
                }
            } else {
                log.warn("No seek points available for keyframes");
            }
        }
        return result;
    }

    public void setVideoCodecId(String videoCodecId) {
        trackInfo.setVideoCodecId(videoCodecId);
    }

    public void setAudioCodecId(String audioCodecId) {
        trackInfo.setAudioCodecId(audioCodecId);
    }

    public String getVideoCodecId() {
        return trackInfo.getVideoCodecId();
    }

    public String getAudioCodecId() {
        return trackInfo.getAudioCodecId();
    }
}
