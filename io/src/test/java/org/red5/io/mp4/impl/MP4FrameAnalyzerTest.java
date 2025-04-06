package org.red5.io.mp4.impl;

import java.io.File;
import java.util.List;
import junit.framework.TestCase;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.junit.Test;
import org.red5.io.mp4.MP4Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4FrameAnalyzerTest extends TestCase {

  private static Logger log = LoggerFactory.getLogger(MP4FrameAnalyzerTest.class);

  @Test
  public void testAnalyzeFramesAudioVideo() throws Exception {
    File file = new File("target/test-classes/fixtures/mov_h264.mp4");
    if (!file.exists()) {
      log.warn("Fixture missing: {}", file.getAbsolutePath());
      return;
    }

    MP4TrackInfo trackInfo = new MP4TrackInfo();
    MP4Parser parser = new MP4Parser(trackInfo);

    try (SeekableByteChannel channel = NIOUtils.readableChannel(file)) {
      parser.parseMovie(channel);
    }

    try (SeekableByteChannel channel = NIOUtils.readableChannel(file)) {
      MP4FrameAnalyzer analyzer = new MP4FrameAnalyzer(trackInfo, channel);
      List<MP4Frame> frames = analyzer.analyzeFrames();
      assertNotNull(frames);
      assertFalse(frames.isEmpty());

      double prevTime = -1.0;
      for (MP4Frame frame : frames) {
        assertTrue("Frame timestamps should be ascending", frame.getTime() >= prevTime);
        prevTime = frame.getTime();
      }
      log.debug("Analyzed {} frames total", frames.size());
    }
  }

  @Test
  public void testAnalyzeFramesAudioOnly() throws Exception {
    File file = new File("target/test-classes/fixtures/audio_only.mp4");
    if (!file.exists()) {
      log.warn("No audio-only fixture found: {}", file.getAbsolutePath());
      return;
    }
    MP4TrackInfo trackInfo = new MP4TrackInfo();
    MP4Parser parser = new MP4Parser(trackInfo);
    try (SeekableByteChannel channel = NIOUtils.readableChannel(file)) {
      parser.parseMovie(channel);
    }
    assertTrue(trackInfo.hasAudio());
    assertFalse(trackInfo.hasVideo());

    try (SeekableByteChannel channel = NIOUtils.readableChannel(file)) {
      MP4FrameAnalyzer analyzer = new MP4FrameAnalyzer(trackInfo, channel);
      List<MP4Frame> frames = analyzer.analyzeFrames();
      assertNotNull(frames);
      assertFalse(frames.isEmpty());
      for (MP4Frame f : frames) {
        assertEquals("Should be audio FLV tag", (byte) 8, f.getType());
      }
      log.debug("Audio-only analysis done, total frames={}", frames.size());
    }
  }
}
