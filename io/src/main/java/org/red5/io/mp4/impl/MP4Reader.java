
package org.red5.io.mp4.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.mina.core.buffer.IoBuffer;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.ColorExtension;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.MediaBox;
import org.jcodec.containers.mp4.boxes.MediaHeaderBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.boxes.WaveExtension;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.impl.Tag;
import org.red5.io.isobmff.atom.ShortEsdsBox;
import org.red5.io.mp4.MP4Frame;
import org.red5.io.utils.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4Reader implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

  private static final Logger log = LoggerFactory.getLogger(MP4Reader.class);

  private static final NumberFormat TIME_FORMAT = new DecimalFormat("0.00");

  public static final byte[] PREFIX_AUDIO_CONFIG_FRAME = { (byte) 0xaf, (byte) 0 };

  public static final byte[] PREFIX_AUDIO_FRAME = { (byte) 0xaf, (byte) 0x01 };

  public static final byte[] EMPTY_AAC = { (byte) 0x21, (byte) 0x10, (byte) 0x04, (byte) 0x60, (byte) 0x8c,
      (byte) 0x1c };

  public static final byte[] PREFIX_VIDEO_CONFIG_FRAME = { (byte) 0x17, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };

  public static final byte[] PREFIX_VIDEO_KEYFRAME = { (byte) 0x17, (byte) 0x01 };

  public static final byte[] PREFIX_VIDEO_FRAME = { (byte) 0x27, (byte) 0x01 };

  private SeekableByteChannel dataSource;

  private Map<Integer, Long> timePosMap = new HashMap<>();

  private Map<Integer, Long> samplePosMap = new HashMap<>();

  private List<MP4Frame> frames = new ArrayList<>();

  private final LinkedList<ITag> firstTags = new LinkedList<>();

  private LinkedList<Integer> seekPoints = new LinkedList<>();

  private final Semaphore lock = new Semaphore(1, true);

  private boolean hasVideo;

  private boolean hasAudio;

  private String videoCodecId = "avc1";

  private String audioCodecId = "mp4a";

  private byte[] audioDecoderBytes, videoDecoderBytes;

  private long duration;

  private long timeScale;

  private int width;

  private int height;

  private double audioTimeScale;

  private int audioChannels;

  private int audioCodecType = 1;

  private int videoSampleCount;

  private double fps;

  private double videoTimeScale;

  private int avcLevel;

  private int avcProfile;

  private String formattedDuration;

  private List<SampleToChunkEntry> audioSamplesToChunks, videoSamplesToChunks;

  private int[] syncSamples;

  private int[] audioSamples, videoSamples;

  private long audioSampleSize;

  private long[] audioChunkOffsets, videoChunkOffsets;

  private long audioSampleDuration = 1024, videoSampleDuration = 125;

  private int currentFrame = 0;

  private int prevFrameSize = 0;

  private int prevVideoTS = -1;

  private long audioCount;

  private long videoCount;

  private List<CompositionOffsetsBox.Entry> compositionTimes;

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
    parseMovie(dataSource);
    analyzeFrames();
    firstTags.add(createFileMeta());
    createPreStreamingTags(0, false);
  }

  public void parseMovie(SeekableByteChannel dataSource) {
    try {
      Movie movie = MP4Util.parseFullMovieChannel(dataSource);
      MovieBox moov = movie.getMoov();
      AtomicInteger scale = new AtomicInteger(0);
      dumpBox(moov);
      decodeMovieHeader(moov);
      decodeTracks(moov.getTracks(), scale);
      log.debug("Time scale {} Duration {} seconds: {}", timeScale, duration, duration / timeScale);
    } catch (Exception e) {
      log.error("Exception decoding header / atoms", e);
    }
  }

  private void decodeMovieHeader(MovieBox moov) {
    moov.getBoxes().forEach(box -> {
      if (box instanceof MovieHeaderBox) {
        MovieHeaderBox mvhd = (MovieHeaderBox) box;
        timeScale = mvhd.getTimescale();
        duration = mvhd.getDuration();
      } else {
        log.debug("Skipping box: {}", box);
      }
    });
  }

  private void decodeTracks(TrakBox[] tracks, AtomicInteger scale) {
    for (TrakBox trak : tracks) {
      MP4TrackType trackType = TrakBox.getTrackType(trak);
      if (trackType == MP4TrackType.SOUND) {
        hasAudio = true;
      } else if (trackType == MP4TrackType.VIDEO) {
        hasVideo = true;
      }
      trak.getBoxes().forEach(box -> decodeTrackBox(box, scale));
    }
    log.debug("Tracks: {}", tracks.length);
  }

  private void decodeTrackBox(Box box, AtomicInteger scale) {
    AtomicBoolean isAudio = new AtomicBoolean(false);
    AtomicBoolean isVideo = new AtomicBoolean(false);
    switch (box.getFourcc()) {
      case "tkhd":
        decodeTrackHeaderBox((TrackHeaderBox) box);
        break;
      case "mdia":
        decodeMediaBox((MediaBox) box, scale, isAudio, isVideo);
        break;
      default:
        log.warn("Unhandled box: {}", box);
        break;
    }
  }

  private void decodeTrackHeaderBox(TrackHeaderBox tkhd) {
    if (tkhd.getWidth() > 0) {
      width = (int) tkhd.getWidth();
      height = (int) tkhd.getHeight();
      log.debug("Width {} x Height {}", width, height);
    }
  }

  private void decodeMediaBox(MediaBox mdia, AtomicInteger scale, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    mdia.getBoxes().forEach(mdbox -> {
      if (mdbox instanceof MediaHeaderBox) {
        scale.set(((MediaHeaderBox) mdbox).getTimescale());
        log.debug("Time scale {}", scale);
      } else if (mdbox instanceof HandlerBox) {
        decodeHandlerBox((HandlerBox) mdbox, isAudio, isVideo, scale);
      } else {
        log.debug("Unhandled media box: {}", mdbox);
      }
    });
    MediaInfoBox minf = mdia.getMinf();
    if (minf != null) {
      decodeMediaInfoBox(minf, isAudio, isVideo, scale);
    }
  }

  private void decodeHandlerBox(HandlerBox hdlr, AtomicBoolean isAudio, AtomicBoolean isVideo, AtomicInteger scale) {
    String hdlrType = hdlr.getComponentSubType();
    if ("vide".equals(hdlrType)) {
      isVideo.set(true);
      isAudio.set(false);
      if (scale.get() > 0) {
        videoTimeScale = scale.get() * 1.0;
        log.debug("Video time scale: {}", videoTimeScale);
      }
    } else if ("soun".equals(hdlrType)) {
      isAudio.set(true);
      isVideo.set(false);
      if (scale.get() > 0) {
        audioTimeScale = scale.get() * 1.0;
        log.debug("Audio time scale: {}", audioTimeScale);
      }
    } else {
      log.debug("Unhandled handler type: {}", hdlrType);
    }
  }

  private void decodeMediaInfoBox(MediaInfoBox minf, AtomicBoolean isAudio, AtomicBoolean isVideo,
      AtomicInteger scale) {
    NodeBox stbl = minf.getStbl();
    if (stbl != null) {
      stbl.getBoxes().forEach(sbox -> decodeStblBox(sbox, isAudio, isVideo, scale));
    }
  }

  private void decodeStblBox(Box sbox, AtomicBoolean isAudio, AtomicBoolean isVideo, AtomicInteger scale) {
    switch (sbox.getFourcc()) {
      case "stsd":
        decodeSampleDescriptionBox((SampleDescriptionBox) sbox, isAudio, isVideo, scale);
        break;
      case "stsc":
        decodeSampleToChunkBox((SampleToChunkBox) sbox, isAudio, isVideo);
        break;
      case "stsz":
        decodeSampleSizesBox((SampleSizesBox) sbox, isAudio, isVideo);
        break;
      case "stco":
        decodeChunkOffsetsBox((ChunkOffsetsBox) sbox, isAudio, isVideo);
        break;
      case "co64":
        decodeChunkOffsets64Box((ChunkOffsets64Box) sbox, isAudio, isVideo);
        break;
      case "stss":
        decodeSyncSamplesBox((SyncSamplesBox) sbox, isAudio, isVideo);
        break;
      case "stts":
        decodeTimeToSampleBox((TimeToSampleBox) sbox, isAudio, isVideo);
        break;
      case "ctts":
        decodeCompositionOffsetsBox((CompositionOffsetsBox) sbox, isAudio, isVideo);
        break;
      default:
        log.debug("Unhandled stbl box: {}", sbox);
        break;
    }
  }

  private void decodeSampleDescriptionBox(SampleDescriptionBox stsd, AtomicBoolean isAudio, AtomicBoolean isVideo,
      AtomicInteger scale) {
    stsd.getBoxes().forEach(stbox -> {
      switch (stbox.getFourcc()) {
        case "mp4a":
          audioCodecId = "mp4a";
          processAudioSampleEntry((AudioSampleEntry) stbox, scale.get());
          break;
        case "ac-3":
          audioCodecId = "ac-3";
          processAudioSampleEntry((AudioSampleEntry) stbox, scale.get());
          break;
        case "mp4v":
          videoCodecId = "mp4v";
          processVideoSampleEntry((VideoSampleEntry) stbox, scale.get());
          break;
        case "avc1":
          videoCodecId = "avc1";
          processVideoSampleEntry((VideoSampleEntry) stbox, scale.get());
          break;
        case "hev1":
          videoCodecId = "hev1";
          break;
        case "hvc1":
          videoCodecId = "hvc1";
          processVideoSampleEntry((VideoSampleEntry) stbox, scale.get());
          break;
        default:
          log.warn("Unhandled sample description box: {}", stbox);
          break;
      }
    });
  }

  private void decodeSampleToChunkBox(SampleToChunkBox stsc, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    if (isAudio.get()) {
      audioSamplesToChunks = List.of(stsc.getSampleToChunk());
    } else if (isVideo.get()) {
      videoSamplesToChunks = List.of(stsc.getSampleToChunk());
    }
  }

  private void decodeSampleSizesBox(SampleSizesBox stsz, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    if (isAudio.get()) {
      audioSamples = stsz.getSizes();
      audioSampleSize = stsz.getDefaultSize();
    } else if (isVideo.get()) {
      videoSamples = stsz.getSizes();
      videoSampleCount = stsz.getCount();
    }
  }

  private void decodeChunkOffsetsBox(ChunkOffsetsBox stco, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    if (isAudio.get()) {
      audioChunkOffsets = stco.getChunkOffsets();
    } else if (isVideo.get()) {
      videoChunkOffsets = stco.getChunkOffsets();
    }
  }

  private void decodeChunkOffsets64Box(ChunkOffsets64Box co64, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    if (isAudio.get()) {
      audioChunkOffsets = co64.getChunkOffsets();
    } else if (isVideo.get()) {
      videoChunkOffsets = co64.getChunkOffsets();
    }
  }

  private void decodeSyncSamplesBox(SyncSamplesBox stss, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    if (isVideo.get()) {
      syncSamples = stss.getSyncSamples();
    }
  }

  private void decodeTimeToSampleBox(TimeToSampleBox stts, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    TimeToSampleEntry[] records = stts.getEntries();
    if (isAudio.get()) {
      audioSampleDuration = records[0].getSampleDuration();
    } else if (isVideo.get()) {
      videoSampleDuration = records[0].getSampleDuration();
    }
  }

  private void decodeCompositionOffsetsBox(CompositionOffsetsBox ctts, AtomicBoolean isAudio, AtomicBoolean isVideo) {
    compositionTimes = new LinkedList<>(List.of(ctts.getEntries()));
  }

  public static void dumpBox(NodeBox box) {
    log.debug("Dump box: {}", box);
    box.getBoxes().forEach(bx -> log.debug("{}", bx));
  }

  private void processAudioSampleEntry(AudioSampleEntry ase, int scale) {
    log.debug("Sample size: {}", ase.getSampleSize());
    float ats = ase.getSampleRate();
    if (ats > 0) {
      audioTimeScale = ats * 1.0;
    }
    log.debug("Sample rate (audio time scale): {}", audioTimeScale);
    audioChannels = ase.getChannelCount();
    ase.getBoxes().forEach(box -> {
      switch (box.getFourcc()) {
        case "esds":
          processEsdsBox(box);
          break;
        case "dac3":
          break;
        case "wave":
          processWaveExtension(box);
          break;
        case "btrt":
          break;
        default:
          log.warn("Unhandled sample desc extension: {}", box);
          break;
      }
    });
  }

  private void processEsdsBox(Box box) {
    long esdsBodySize = box.getHeader().getBodySize();
    log.debug("esds body size: {}", esdsBodySize);
    ShortEsdsBox esds = Box.asBox(ShortEsdsBox.class, box);
    log.debug("Process {} obj: {} avg bitrate: {} max bitrate: {}", esds.getFourcc(), esds.getObjectType(),
        esds.getAvgBitrate(), esds.getMaxBitrate());
    if (esds.hasStreamInfo()) {
      audioDecoderBytes = esds.getStreamInfo().array();
    } else {
      audioDecoderBytes = EMPTY_AAC;
    }
    log.debug("Audio config bytes: {}", HexDump.byteArrayToHexString(audioDecoderBytes));
    determineAudioCodecType();
  }

  private void processWaveExtension(Box box) {
    WaveExtension wave = Box.asBox(WaveExtension.class, box);
    log.debug("wave atom found");
    EsdsBox wesds = wave.getBoxes().stream().filter(b -> b instanceof EsdsBox).map(b -> (EsdsBox) b).findFirst()
        .orElse(null);
    if (wesds != null) {
      log.debug("Process {} obj: {} avg bitrate: {} max bitrate: {}", wesds.getFourcc(), wesds.getObjectType(),
          wesds.getAvgBitrate(), wesds.getMaxBitrate());
    } else {
      log.debug("esds not found in wave");
    }
  }

  private void determineAudioCodecType() {
    byte audioCoderType = audioDecoderBytes[0];
    switch (audioCoderType) {
      case 0x02:
      case 0x11:
        log.debug("Audio type AAC LC");
        audioCodecType = 1;
        break;
      case 0x01:
        log.debug("Audio type AAC Main");
        audioCodecType = 0;
        break;
      case 0x03:
        log.debug("Audio type AAC SBR");
        audioCodecType = 2;
        break;
      case 0x05:
      case 0x1d:
        log.debug("Audio type AAC HE");
        audioCodecType = 3;
        break;
      case 0x20:
      case 0x21:
      case 0x22:
        log.debug("Audio type MP3");
        audioCodecType = 33;
        audioCodecId = "mp3";
        break;
      default:
        log.debug("Unknown audio type");
        break;
    }
    log.debug("Audio coder type: {} {} id: {}", audioCoderType, Integer.toBinaryString(audioCoderType), audioCodecId);
  }

  private void processVideoSampleEntry(VideoSampleEntry vse, int scale) {
    String compressorName = vse.getCompressorName();
    long frameCount = vse.getFrameCount();
    log.debug("Compressor: {} frame count: {}", compressorName, frameCount);
    vse.getBoxes().forEach(box -> {
      switch (box.getFourcc()) {
        case "esds":
          processVideoEsdsBox((EsdsBox) box);
          break;
        case "avcC":
          processAvcCBox((AvcCBox) box);
          break;
        case "hvcC":
          break;
        case "btrt":
          break;
        case "pasp":
          processPixelAspectExt((PixelAspectExt) box);
          break;
        case "colr":
          processColorExtension((ColorExtension) box);
          break;
        default:
          log.warn("Unhandled sample desc extension: {}", box);
          break;
      }
    });
  }

  private void processVideoEsdsBox(EsdsBox esds) {
    log.debug("Process {} obj: {} avg bitrate: {} max bitrate: {}", esds.getFourcc(), esds.getObjectType(),
        esds.getAvgBitrate(), esds.getMaxBitrate());
    videoDecoderBytes = esds.getStreamInfo().array();
    log.debug("Video config bytes: {}", HexDump.byteArrayToHexString(videoDecoderBytes));
  }

  private void processAvcCBox(AvcCBox avcC) {
    avcLevel = avcC.getLevel();
    avcProfile = avcC.getProfile();
    log.debug("Process {} level: {} nal len: {} profile: {} compat: {}", avcC.getFourcc(), avcLevel,
        avcC.getNalLengthSize(), avcProfile, avcC.getProfileCompat());
    avcC.getSpsList().forEach(sps -> log.debug("SPS: {}", sps));
    avcC.getPpsList().forEach(pps -> log.debug("PPS: {}", pps));
  }

  private void processPixelAspectExt(PixelAspectExt pasp) {
    log.debug("Process {} hSpacing: {} vSpacing: {}", pasp.getFourcc(), pasp.gethSpacing(), pasp.getvSpacing());
  }

  private void processColorExtension(ColorExtension colr) {
    log.debug("Process {} primaries: {} transfer: {} matrix: {}", colr.getFourcc(), colr.getPrimariesIndex(),
        colr.getTransferFunctionIndex(), colr.getMatrixIndex());
  }

  @Override
  public long getTotalBytes() {
    try {
      return dataSource.size();
    } catch (Exception e) {
      log.error("Error getTotalBytes", e);
    }
    return 0;
  }

  private long getCurrentPosition() {
    try {
      return dataSource.position();
    } catch (Exception e) {
      log.error("Error getCurrentPosition", e);
      return 0;
    }
  }

  @Override
  public boolean hasVideo() {
    return hasVideo;
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
  public long getBytesRead() {
    return getCurrentPosition();
  }

  @Override
  public long getDuration() {
    return duration;
  }

  public String getVideoCodecId() {
    return videoCodecId;
  }

  public String getAudioCodecId() {
    return audioCodecId;
  }

  @Override
  public boolean hasMoreTags() {
    return currentFrame < frames.size();
  }

  private ITag createFileMeta() {
    log.debug("Creating onMetaData");
    IoBuffer buf = IoBuffer.allocate(1024);
    buf.setAutoExpand(true);
    Output out = new Output(buf);
    out.writeString("onMetaData");
    Map<Object, Object> props = new HashMap<>();
    props.put("duration", duration / timeScale);
    props.put("width", width);
    props.put("height", height);
    props.put("videocodecid", videoCodecId);
    props.put("avcprofile", avcProfile);
    props.put("avclevel", avcLevel);
    props.put("videoframerate", fps);
    props.put("audiocodecid", audioCodecId);
    props.put("aacaot", audioCodecType);
    props.put("audiosamplerate", audioTimeScale);
    props.put("audiochannels", audioChannels);
    if (seekPoints != null) {
      log.debug("Seekpoint list size: {}", seekPoints.size());
      props.put("seekpoints", seekPoints);
    }
    List<Map<String, Object>> arr = new ArrayList<>(2);
    if (hasAudio) {
      arr.add(createAudioTrackInfo());
    }
    if (hasVideo) {
      arr.add(createVideoTrackInfo());
    }
    props.put("trackinfo", arr);
    props.put("canSeekToEnd", seekPoints != null);
    out.writeMap(props);
    buf.flip();
    ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
    result.setBody(buf);
    return result;
  }

  private Map<String, Object> createAudioTrackInfo() {
    Map<String, Object> audioMap = new HashMap<>(4);
    audioMap.put("timescale", audioTimeScale);
    audioMap.put("language", "und");
    List<Map<String, String>> desc = new ArrayList<>(1);
    audioMap.put("sampledescription", desc);
    Map<String, String> sampleMap = new HashMap<>(1);
    sampleMap.put("sampletype", audioCodecId);
    desc.add(sampleMap);
    if (audioSamples != null) {
      if (audioSampleDuration > 0) {
        audioMap.put("length_property", audioSampleDuration * audioSamples.length);
      }
      audioSamples = null;
    }
    return audioMap;
  }

  private Map<String, Object> createVideoTrackInfo() {
    Map<String, Object> videoMap = new HashMap<>(3);
    videoMap.put("timescale", videoTimeScale);
    videoMap.put("language", "und");
    List<Map<String, String>> desc = new ArrayList<>(1);
    videoMap.put("sampledescription", desc);
    Map<String, String> sampleMap = new HashMap<>(1);
    sampleMap.put("sampletype", videoCodecId);
    desc.add(sampleMap);
    if (videoSamples != null) {
      if (videoSampleDuration > 0) {
        videoMap.put("length_property", videoSampleDuration * videoSamples.length);
      }
      videoSamples = null;
    }
    return videoMap;
  }

  private void createPreStreamingTags(int timestamp, boolean clear) {
    log.debug("Creating pre-streaming tags");
    if (clear) {
      firstTags.clear();
    }
    if (hasVideo) {
      addVideoPreStreamingTag(timestamp);
    }
    if (hasAudio) {
      addAudioPreStreamingTag(timestamp);
    }
  }

  private void addVideoPreStreamingTag(int timestamp) {
    IoBuffer body = IoBuffer.allocate(41);
    body.setAutoExpand(true);
    body.put(PREFIX_VIDEO_CONFIG_FRAME);
    if (videoDecoderBytes != null) {
      body.put(videoDecoderBytes);
    }
    ITag tag = new Tag(IoConstants.TYPE_VIDEO, timestamp, body.position(), null, 0);
    body.flip();
    tag.setBody(body);
    firstTags.add(tag);
  }

  private void addAudioPreStreamingTag(int timestamp) {
    if (audioDecoderBytes != null) {
      IoBuffer body = IoBuffer.allocate(audioDecoderBytes.length + 3);
      body.setAutoExpand(true);
      body.put(PREFIX_AUDIO_CONFIG_FRAME);
      body.put(audioDecoderBytes);
      body.put((byte) 0x06);
      ITag tag = new Tag(IoConstants.TYPE_AUDIO, timestamp, body.position(), null, 0);
      body.flip();
      tag.setBody(body);
      firstTags.add(tag);
    } else {
      log.info("Audio decoder bytes were not available");
    }
  }

  @Override
  public ITag readTag() {
    ITag tag = null;
    if (!frames.isEmpty()) {
      try {
        lock.acquire();
        if (!firstTags.isEmpty()) {
          return firstTags.removeFirst();
        }
        MP4Frame frame = frames.get(currentFrame);
        if (frame != null) {
          tag = createTagFromFrame(frame);
          currentFrame++;
          prevFrameSize = tag.getBodySize();
        }
      } catch (InterruptedException e) {
        log.warn("Exception acquiring lock", e);
      } finally {
        lock.release();
      }
    } else {
      log.warn("No frames are available for the requested item");
    }
    return tag;
  }

  private ITag createTagFromFrame(MP4Frame frame) {
    int sampleSize = frame.getSize();
    int time = (int) Math.round(frame.getTime() * 1000.0);
    long samplePos = frame.getOffset();
    byte type = frame.getType();
    int pad = (type == TYPE_AUDIO) ? 2 : 5;
    ByteBuffer data = ByteBuffer.allocate(sampleSize + pad);
    try {
      if (type == TYPE_VIDEO) {
        data.put(frame.isKeyFrame() ? PREFIX_VIDEO_KEYFRAME : PREFIX_VIDEO_FRAME);
        int timeOffset = prevVideoTS != -1 ? time - prevVideoTS : 0;
        data.put((byte) ((timeOffset >>> 16) & 0xff));
        data.put((byte) ((timeOffset >>> 8) & 0xff));
        data.put((byte) (timeOffset & 0xff));
        videoCount++;
        prevVideoTS = time;
      } else {
        data.put(PREFIX_AUDIO_FRAME);
        audioCount++;
      }
      dataSource.setPosition(samplePos);
      dataSource.read(data);
    } catch (IOException e) {
      log.error("Error on channel position / read", e);
    }
    IoBuffer payload = IoBuffer.wrap(data.array());
    return new Tag(type, time, payload.limit(), payload, prevFrameSize);
  }

  public void analyzeFrames() {
    log.debug("Analyzing frames - video samples/chunks: {}", videoSamplesToChunks);
    timePosMap = new HashMap<>();
    samplePosMap = new HashMap<>();
    if (videoSamplesToChunks != null) {
      analyzeVideoFrames();
    }
    if (audioSamplesToChunks != null) {
      analyzeAudioFrames();
    }
    Collections.sort(frames);
    log.debug("Frames count: {}", frames.size());
    releaseMemory();
  }

  private void analyzeVideoFrames() {
    AtomicInteger compositeIndex = new AtomicInteger(0);
    AtomicReference<CompositionOffsetsBox.Entry> compositeTimeEntry = new AtomicReference<>(
        compositionTimes != null && !compositionTimes.isEmpty() ? compositionTimes.remove(0) : null);

    AtomicInteger sample = new AtomicInteger(1);

    IntStream.range(0, videoSamplesToChunks.size()).forEach(i -> {
      SampleToChunkEntry record = videoSamplesToChunks.get(i);
      long firstChunk = record.getFirst();
      long lastChunk = (i < videoSamplesToChunks.size() - 1) ? videoSamplesToChunks.get(i + 1).getFirst() - 1
          : videoChunkOffsets.length;

      LongStream.rangeClosed(firstChunk, lastChunk).forEach(chunk -> {
        long sampleCount = record.getCount();
        long pos = videoChunkOffsets[(int) (chunk - 1)];

        while (sampleCount > 0 && sample.get() <= videoSamples.length) {
          int currentSample = sample.getAndIncrement();
          samplePosMap.put(currentSample, pos);
          double ts = (videoSampleDuration * (currentSample - 1)) / videoTimeScale;
          boolean keyframe = syncSamples != null && ArrayUtils.contains(syncSamples, currentSample);

          if (keyframe && seekPoints == null) {
            seekPoints = new LinkedList<>();
          }

          int frameTs = (int) Math.round(ts * 1000.0);
          if (keyframe) {
            seekPoints.add(frameTs);
          }

          timePosMap.put(frameTs, pos);
          int size = (currentSample - 1 < videoSamples.length) ? (int) videoSamples[currentSample - 1] : 0;
          MP4Frame frame = new MP4Frame();
          frame.setKeyFrame(keyframe);
          frame.setOffset(pos);
          frame.setSize(size);
          frame.setTime(ts);
          frame.setType(TYPE_VIDEO);

          if (compositeTimeEntry.get() != null) {
            frame.setTimeOffset(compositeTimeEntry.get().getOffset());
            compositeIndex.incrementAndGet();
            if (compositeIndex.get() - compositeTimeEntry.get().getCount() == 0) {
              compositeTimeEntry.set(!compositionTimes.isEmpty() ? compositionTimes.remove(0) : null);
              compositeIndex.set(0);
            }
          }

          frames.add(frame);
          pos += size;
          sampleCount--;
        }
      });
    });
  }

  private void analyzeAudioFrames() {
    int sample = 1;
    for (int i = 0; i < audioSamplesToChunks.size(); i++) {
      SampleToChunkEntry record = audioSamplesToChunks.get(i);
      long firstChunk = record.getFirst();
      long lastChunk = audioChunkOffsets.length;
      if (i < audioSamplesToChunks.size() - 1) {
        lastChunk = audioSamplesToChunks.get(i + 1).getFirst() - 1;
      }
      for (long chunk = firstChunk; chunk <= lastChunk; chunk++) {
        long sampleCount = record.getCount();
        long pos = audioChunkOffsets[(int) (chunk - 1)];
        while (sampleCount > 0) {
          double ts = (audioSampleDuration * (sample - 1)) / audioTimeScale;
          int size = 0;
          if (audioSamples.length > 0) {
            size = (int) audioSamples[sample - 1];
            if (size == 6) {
              try {
                long position = dataSource.position();
                dataSource.setPosition(pos);
                ByteBuffer dst = ByteBuffer.allocate(6);
                dataSource.read(dst);
                dst.flip();
                dataSource.setPosition(position);
                byte[] tmp = dst.array();
                if (Arrays.equals(EMPTY_AAC, tmp)) {
                  pos += size;
                  sampleCount--;
                  sample++;
                  continue;
                }
              } catch (IOException e) {
                log.warn("Exception during audio analysis", e);
              }
            }
          }
          size = (int) (size != 0 ? size : audioSampleSize);
          MP4Frame frame = new MP4Frame();
          frame.setOffset(pos);
          frame.setSize(size);
          frame.setTime(ts);
          frame.setType(TYPE_AUDIO);
          frames.add(frame);
          pos += size;
          sampleCount--;
          sample++;
        }
      }
    }
  }

  private void releaseMemory() {
    if (audioSamplesToChunks != null) {
      audioChunkOffsets = null;
      audioSamplesToChunks = null;
    }
    if (videoSamplesToChunks != null) {
      videoChunkOffsets = null;
      videoSamplesToChunks = null;
    }
    if (syncSamples != null) {
      syncSamples = null;
    }
  }

  @Override
  public void position(long pos) {
    log.debug("Position: {}", pos);
    log.debug("Current frame: {}", currentFrame);
    int len = frames.size();
    MP4Frame frame = null;
    for (int f = 0; f < len; f++) {
      frame = frames.get(f);
      long offset = frame.getOffset();
      if (pos == offset || (offset > pos && frame.isKeyFrame())) {
        if (!frame.isKeyFrame()) {
          log.debug("Frame #{} was not a key frame, so trying again..", f);
          continue;
        }
        log.info("Frame #{} found for seek: {}", f, frame);
        createPreStreamingTags((int) (frame.getTime() * 1000), true);
        currentFrame = f;
        break;
      }
      prevVideoTS = (int) (frame.getTime() * 1000);
    }
    log.debug("Setting current frame: {}", currentFrame);
  }

  @Override
  public void close() {
    log.debug("Close");
    if (dataSource != null) {
      try {
        dataSource.close();
      } catch (IOException e) {
        log.error("Channel close {}", e);
      } finally {
        if (frames != null) {
          frames.clear();
          frames = null;
        }
      }
    }
  }

  public void setVideoCodecId(String videoCodecId) {
    this.videoCodecId = videoCodecId;
  }

  public void setAudioCodecId(String audioCodecId) {
    this.audioCodecId = audioCodecId;
  }

  @Override
  public KeyFrameMeta analyzeKeyFrames() {
    KeyFrameMeta result = new KeyFrameMeta();
    result.audioOnly = hasAudio && !hasVideo;
    result.duration = duration;
    if (result.audioOnly) {
      result.positions = new long[frames.size()];
      result.timestamps = new int[frames.size()];
      result.audioOnly = true;
      for (int i = 0; i < result.positions.length; i++) {
        frames.get(i).setKeyFrame(true);
        result.positions[i] = frames.get(i).getOffset();
        result.timestamps[i] = (int) Math.round(frames.get(i).getTime() * 1000.0);
      }
    } else {
      if (seekPoints != null) {
        int seekPointCount = seekPoints.size();
        result.positions = new long[seekPointCount];
        result.timestamps = new int[seekPointCount];
        for (int idx = 0; idx < seekPointCount; idx++) {
          final Integer ts = seekPoints.get(idx);
          result.positions[idx] = timePosMap.get(ts);
          result.timestamps[idx] = ts;
        }
      } else {
        log.warn("Seek points array was null");
      }
    }
    return result;
  }
}
