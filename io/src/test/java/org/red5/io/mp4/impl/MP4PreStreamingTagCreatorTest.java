package org.red5.io.mp4.impl;

import junit.framework.TestCase;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4PreStreamingTagCreatorTest extends TestCase {

  private static Logger log = LoggerFactory.getLogger(MP4PreStreamingTagCreatorTest.class);

  @Test
  public void testCreateVideoConfigTag() {
    MP4TrackInfo trackInfo = new MP4TrackInfo();
    trackInfo.setHasVideo(true);
    // Example avcC bytes
    byte[] avcc = new byte[] {0x01, 0x64, 0x00, 0x1F, (byte) 0xFF};
    trackInfo.setVideoDecoderBytes(avcc);

    MP4PreStreamingTagCreator creator = new MP4PreStreamingTagCreator(trackInfo);
    ITag videoTag = creator.createVideoConfigTag(0);

    assertNotNull(videoTag);
    assertEquals(IoConstants.TYPE_VIDEO, videoTag.getDataType());
    IoBuffer body = videoTag.getBody();
    assertNotNull(body);
    body.rewind();
    byte first = body.get();
    assertEquals("Should start with 0x17 for keyframe + AVC header", (byte) 0x17, first);
    log.debug("Video config tag => {}", videoTag);
  }

  @Test
  public void testCreateAudioConfigTag() {
    MP4TrackInfo trackInfo = new MP4TrackInfo();
    trackInfo.setHasAudio(true);
    byte[] audioConf = new byte[] {0x11, (byte) 0x90};
    trackInfo.setAudioDecoderBytes(audioConf);

    MP4PreStreamingTagCreator creator = new MP4PreStreamingTagCreator(trackInfo);
    ITag audioTag = creator.createAudioConfigTag(0);
    assertNotNull(audioTag);
    assertEquals(IoConstants.TYPE_AUDIO, audioTag.getDataType());
    IoBuffer body = audioTag.getBody();
    assertNotNull(body);
    body.rewind();
    byte first = body.get();
    assertEquals("Should start with 0xAF for AAC SoundFormat", (byte) 0xAF, first);
    log.debug("Audio config tag => {}", audioTag);
  }

  @Test
  public void testNoVideoBytes() {
    MP4TrackInfo trackInfo = new MP4TrackInfo();
    trackInfo.setHasVideo(true);

    MP4PreStreamingTagCreator creator = new MP4PreStreamingTagCreator(trackInfo);
    ITag videoTag = creator.createVideoConfigTag(1000);
    assertNull("Should be null if no videoDecoderBytes set", videoTag);
  }
}
