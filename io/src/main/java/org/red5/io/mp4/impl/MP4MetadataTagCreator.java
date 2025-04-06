package org.red5.io.mp4.impl;

import java.util.*;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.impl.Tag;

public class MP4MetadataTagCreator {

    private final MP4TrackInfo trackInfo;

    private final LinkedList<Integer> seekPoints;

    public MP4MetadataTagCreator(MP4TrackInfo trackInfo, LinkedList<Integer> seekPoints) {
        this.trackInfo = trackInfo;
        this.seekPoints = seekPoints;
    }

    public ITag createFileMeta() {
        IoBuffer buf = IoBuffer.allocate(256);
        buf.setAutoExpand(true);
        Output out = new Output(buf);

        out.writeString("onMetaData");

        Map<Object, Object> props = new HashMap<>();
        double durationSeconds = (trackInfo.getTimeScale() != 0) ? trackInfo.getDuration() / (double) trackInfo.getTimeScale() : 0.0;

        props.put("duration", durationSeconds);
        props.put("width", trackInfo.getWidth());
        props.put("height", trackInfo.getHeight());
        props.put("videocodecid", trackInfo.getVideoCodecId());
        props.put("avcprofile", trackInfo.getAvcProfile());
        props.put("avclevel", trackInfo.getAvcLevel());
        props.put("videoframerate", trackInfo.getFps());
        props.put("audiocodecid", trackInfo.getAudioCodecId());
        props.put("aacaot", trackInfo.getAudioCodecType());
        props.put("audiosamplerate", trackInfo.getAudioTimeScale());
        props.put("audiochannels", trackInfo.getAudioChannels());

        if (seekPoints != null && !seekPoints.isEmpty()) {
            props.put("seekpoints", seekPoints);
        }

        // trackinfo
        List<Map<String, Object>> tracks = new ArrayList<>();
        if (trackInfo.hasAudio()) {
            tracks.add(createAudioTrackInfo());
        }
        if (trackInfo.hasVideo()) {
            tracks.add(createVideoTrackInfo());
        }
        props.put("trackinfo", tracks);

        props.put("canSeekToEnd", (seekPoints != null && !seekPoints.isEmpty()));

        out.writeMap(props);
        buf.flip();

        Tag metaTag = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
        metaTag.setBody(buf);
        return metaTag;
    }

    private Map<String, Object> createAudioTrackInfo() {
        Map<String, Object> audioMap = new HashMap<>();
        audioMap.put("timescale", trackInfo.getAudioTimeScale());
        audioMap.put("language", "und");

        List<Map<String, String>> desc = new ArrayList<>();
        Map<String, String> sampleMap = new HashMap<>();
        sampleMap.put("sampletype", trackInfo.getAudioCodecId());
        desc.add(sampleMap);

        audioMap.put("sampledescription", desc);

        if (trackInfo.getAudioSamples() != null && trackInfo.getAudioSampleDuration() > 0) {
            audioMap.put("length_property", trackInfo.getAudioSampleDuration() * trackInfo.getAudioSamples().length);
        }
        return audioMap;
    }

    private Map<String, Object> createVideoTrackInfo() {
        Map<String, Object> videoMap = new HashMap<>();
        videoMap.put("timescale", trackInfo.getVideoTimeScale());
        videoMap.put("language", "und");

        List<Map<String, String>> desc = new ArrayList<>();
        Map<String, String> sampleMap = new HashMap<>();
        sampleMap.put("sampletype", trackInfo.getVideoCodecId());
        desc.add(sampleMap);

        videoMap.put("sampledescription", desc);

        if (trackInfo.getVideoSamples() != null && trackInfo.getVideoSampleDuration() > 0) {
            videoMap.put("length_property", trackInfo.getVideoSampleDuration() * trackInfo.getVideoSamples().length);
        }
        return videoMap;
    }
}
