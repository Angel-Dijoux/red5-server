package org.red5.io.mp4.impl;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.containers.mp4.boxes.*;
import org.red5.io.isobmff.atom.ShortEsdsBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Processes stbl sub-boxes ; stsd, stsc, stsz, stco, co64, stss, stts, ctts */
public class MP4SampleEntryProcessor {

  private static final Logger log = LoggerFactory.getLogger(MP4SampleEntryProcessor.class);

  public static void decodeStblBox(
      Box sbox, MP4TrackInfo trackInfo, boolean isAudio, boolean isVideo, int scale) {
    String fourcc = sbox.getFourcc();
    switch (fourcc) {
      case "stsd":
        decodeSampleDescriptionBox((SampleDescriptionBox) sbox, trackInfo, isAudio, isVideo, scale);
        break;
      case "stsc":
        SampleToChunkBox stsc = (SampleToChunkBox) sbox;
        if (isAudio) {
          trackInfo.setAudioSamplesToChunks(stsc.getSampleToChunk());
        } else if (isVideo) {
          trackInfo.setVideoSamplesToChunks(stsc.getSampleToChunk());
        }
        break;
      case "stsz":
        SampleSizesBox stsz = (SampleSizesBox) sbox;
        if (isAudio) {
          trackInfo.setAudioSamples(stsz.getSizes());
          trackInfo.setAudioSampleSize(stsz.getDefaultSize());
        } else if (isVideo) {
          trackInfo.setVideoSamples(stsz.getSizes());
          trackInfo.setVideoSampleCount(stsz.getCount());
        }
        break;
      case "stco":
        ChunkOffsetsBox stco = (ChunkOffsetsBox) sbox;
        if (isAudio) {
          trackInfo.setAudioChunkOffsets(stco.getChunkOffsets());
        } else if (isVideo) {
          trackInfo.setVideoChunkOffsets(stco.getChunkOffsets());
        }
        break;
      case "co64":
        ChunkOffsets64Box co64 = (ChunkOffsets64Box) sbox;
        if (isAudio) {
          trackInfo.setAudioChunkOffsets(co64.getChunkOffsets());
        } else if (isVideo) {
          trackInfo.setVideoChunkOffsets(co64.getChunkOffsets());
        }
        break;
      case "stss": // sync samples
        if (isVideo) {
          SyncSamplesBox stssBox = (SyncSamplesBox) sbox;
          trackInfo.setSyncSamples(stssBox.getSyncSamples());
        }
        break;
      case "stts":
        TimeToSampleBox stts = (TimeToSampleBox) sbox;
        var entries = stts.getEntries();
        if (entries != null && entries.length > 0) {
          if (isAudio) {
            trackInfo.setAudioSampleDuration(entries[0].getSampleDuration());
          } else if (isVideo) {
            trackInfo.setVideoSampleDuration(entries[0].getSampleDuration());
          }
        }
        break;
      case "ctts":
        CompositionOffsetsBox ctts = (CompositionOffsetsBox) sbox;
        // convert array to a List
        trackInfo.setCompositionTimes(Arrays.asList(ctts.getEntries()));
        break;
      default:
        // ignoring
        break;
    }
  }

  private static void decodeSampleDescriptionBox(
      SampleDescriptionBox stsd,
      MP4TrackInfo trackInfo,
      boolean isAudio,
      boolean isVideo,
      int scale) {
    for (Box stbox : stsd.getBoxes()) {
      if (isAudio && stbox instanceof AudioSampleEntry) {
        AudioSampleEntry ase = (AudioSampleEntry) stbox;
        trackInfo.setAudioCodecId(stbox.getFourcc());
        processAudioSampleEntry(ase, trackInfo);
      } else if (isVideo && stbox instanceof VideoSampleEntry) {
        VideoSampleEntry vse = (VideoSampleEntry) stbox;
        trackInfo.setVideoCodecId(stbox.getFourcc());
        processVideoSampleEntry(vse, trackInfo);
      } else {
        log.debug("Unhandled sample entry: fourcc={}", stbox.getFourcc());
      }
    }
  }

  private static void processAudioSampleEntry(AudioSampleEntry ase, MP4TrackInfo trackInfo) {
    float sampleRate = ase.getSampleRate();
    if (sampleRate > 0) {
      trackInfo.setAudioTimeScale(sampleRate);
    }
    trackInfo.setAudioChannels(ase.getChannelCount());
    for (Box box : ase.getBoxes()) {
      String fourcc = box.getFourcc();
      switch (fourcc) {
        case "esds":
          handleEsdsBox(box, trackInfo);
          break;
        case "wave":
          if (box instanceof WaveExtension) {
            WaveExtension wave = (WaveExtension) box;
            wave.getBoxes()
                .forEach(
                    inner -> {
                      if (inner instanceof EsdsBox) {
                        EsdsBox esds = (EsdsBox) inner;
                        trackInfo.setAudioDecoderBytes(
                            esds.getStreamInfo() != null
                                ? esds.getStreamInfo().array()
                                : MP4Reader.EMPTY_AAC);
                        trackInfo.determineAudioCodecType();
                      }
                    });
          }
          break;
        default:
          // e.g. "dac3", "btrt", etc. can be handled here
          break;
      }
    }
  }

  private static void handleEsdsBox(Box esdsBox, MP4TrackInfo trackInfo) {
    if (esdsBox instanceof ShortEsdsBox) {
      ShortEsdsBox s = (ShortEsdsBox) esdsBox;
      trackInfo.setAudioDecoderBytes(
          s.hasStreamInfo() ? s.getStreamInfo().array() : MP4Reader.EMPTY_AAC);
    } else if (esdsBox instanceof EsdsBox) {
      EsdsBox e = (EsdsBox) esdsBox;
      trackInfo.setAudioDecoderBytes(
          e.getStreamInfo() != null ? e.getStreamInfo().array() : MP4Reader.EMPTY_AAC);
    }
    trackInfo.determineAudioCodecType();
  }

  private static void processVideoSampleEntry(VideoSampleEntry vse, MP4TrackInfo trackInfo) {
    for (Box box : vse.getBoxes()) {
      String fourcc = box.getFourcc();
      switch (fourcc) {
        case "avcC":
          AvcCBox avcC = (AvcCBox) box;
          trackInfo.setAvcLevel(avcC.getLevel());
          trackInfo.setAvcProfile(avcC.getProfile());

          if (!avcC.getSpsList().isEmpty()) {
            ByteBuffer spsBuffer = avcC.getSpsList().get(0);

            byte[] spsBytes = new byte[spsBuffer.remaining()];
            spsBuffer.mark();
            spsBuffer.get(spsBytes);
            spsBuffer.reset();
            trackInfo.setVideoDecoderBytes(spsBytes);
          } else {
            trackInfo.setVideoDecoderBytes(MP4Reader.EMPTY_AAC);
          }
          break;
        case "esds":
          if (box instanceof EsdsBox) {
            EsdsBox e = (EsdsBox) box;
            trackInfo.setVideoDecoderBytes(
                e.getStreamInfo() != null ? e.getStreamInfo().array() : MP4Reader.EMPTY_AAC);
          }
          break;
        // handle "hvcC" if needed for HEVC
        // handle "pasp", "colr", etc.
        default:
          break;
      }
    }
  }
}
