package org.red5.io.mp4.impl;

import java.io.File;
import junit.framework.TestCase;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests for MP4Parser class. */
public class MP4ParserTest extends TestCase {

  private static Logger log = LoggerFactory.getLogger(MP4ParserTest.class);

  @Test
  public void testParseMovieBasic() throws Exception {
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

    // Basic checks
    assertTrue(trackInfo.getDuration() > 0);
    assertTrue(trackInfo.getTimeScale() > 0);
    log.debug(
        "Parsed: duration={} timeScale={}", trackInfo.getDuration(), trackInfo.getTimeScale());
    assertTrue("Should have at least audio or video", trackInfo.hasVideo() || trackInfo.hasAudio());
  }

  @Test
  public void testParseMovieInvalid() {
    File file = new File("target/test-classes/fixtures/not_exist.mp4");
    MP4TrackInfo trackInfo = new MP4TrackInfo();
    MP4Parser parser = new MP4Parser(trackInfo);

    try (SeekableByteChannel channel = NIOUtils.readableChannel(file)) {
      parser.parseMovie(channel);
      fail("Expected an exception or error for non-existent file");
    } catch (Exception e) {
      log.info("Caught expected exception: {}", e.getMessage());
    }
  }
}
