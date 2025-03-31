
package org.red5.io.mp4.impl;

import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MP4Parser {

    private static final Logger log = LoggerFactory.getLogger(MP4Parser.class);

    private final MP4TrackInfo trackInfo;

    public MP4Parser(MP4TrackInfo trackInfo) {
        this.trackInfo = trackInfo;
    }

    public void parseMovie(SeekableByteChannel dataSource) throws IOException {
        // JCodec parse call
        Movie movie = MP4Util.parseFullMovieChannel(dataSource);
        MovieBox moov = movie.getMoov();

        // decode "mvhd"
        for (Box box : moov.getBoxes()) {
            if (box instanceof MovieHeaderBox) {
                MovieHeaderBox mvhd = (MovieHeaderBox) box;
                trackInfo.setTimeScale(mvhd.getTimescale());
                trackInfo.setDuration(mvhd.getDuration());
                log.debug("Movie timescale={} duration={}", mvhd.getTimescale(), mvhd.getDuration());
            }
        }

        TrakBox[] tracks = moov.getTracks();
        decodeTracks(tracks);
    }

    private void decodeTracks(TrakBox[] tracks) {
        for (TrakBox trak : tracks) {
            MP4TrackType trackType = TrakBox.getTrackType(trak);
            if (trackType == MP4TrackType.SOUND) {
                trackInfo.setHasAudio(true);
            } else if (trackType == MP4TrackType.VIDEO) {
                trackInfo.setHasVideo(true);
            }
            decodeTrackBoxes(trak);
        }
    }

    private void decodeTrackBoxes(TrakBox trak) {
        AtomicInteger scale = new AtomicInteger(0);
        AtomicBoolean isAudio = new AtomicBoolean(false);
        AtomicBoolean isVideo = new AtomicBoolean(false);

        // parse track-level boxes
        for (Box box : trak.getBoxes()) {
            if (box instanceof TrackHeaderBox) {
                TrackHeaderBox tkhd = (TrackHeaderBox) box;
                if (tkhd.getWidth() > 0) {
                    trackInfo.setWidth((int) tkhd.getWidth());
                    trackInfo.setHeight((int) tkhd.getHeight());
                }
            } else if (box instanceof MediaBox) {
                MediaBox mdia = (MediaBox) box;
                decodeMediaBox(mdia, scale, isAudio, isVideo);
            }
            // else ignore
        }

        // parse stbl inside minf
        MediaInfoBox minf = trak.getMdia().getMinf();
        if (minf != null) {
            decodeMediaInfoBox(minf, isAudio, isVideo, scale.get());
        }
    }

    private void decodeMediaBox(MediaBox mdia, AtomicInteger scale, AtomicBoolean isAudio, AtomicBoolean isVideo) {
        for (Box box : mdia.getBoxes()) {
            if (box instanceof MediaHeaderBox) {
                MediaHeaderBox mh = (MediaHeaderBox) box;
                scale.set(mh.getTimescale());
            } else if (box instanceof HandlerBox) {
                HandlerBox hdlr = (HandlerBox) box;
                String hdlrType = hdlr.getComponentSubType();
                if ("vide".equals(hdlrType)) {
                    isVideo.set(true);
                    isAudio.set(false);
                    trackInfo.setVideoTimeScale(scale.get() * 1.0);
                } else if ("soun".equals(hdlrType)) {
                    isAudio.set(true);
                    isVideo.set(false);
                    trackInfo.setAudioTimeScale(scale.get() * 1.0);
                }
            }
        }
    }

    private void decodeMediaInfoBox(MediaInfoBox minf, AtomicBoolean isAudio, AtomicBoolean isVideo, int scale) {
        NodeBox stbl = minf.getStbl();
        if (stbl != null) {
            for (Box sbox : stbl.getBoxes()) {
                MP4SampleEntryProcessor.decodeStblBox(sbox, trackInfo, isAudio.get(), isVideo.get(), scale);
            }
        }
    }

}
