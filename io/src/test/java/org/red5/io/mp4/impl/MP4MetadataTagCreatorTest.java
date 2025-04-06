package org.red5.io.mp4.impl;

import junit.framework.TestCase;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4MetadataTagCreatorTest extends TestCase {

  private static Logger log = LoggerFactory.getLogger(MP4MetadataTagCreatorTest.class);

  @Test
  public void testCreateFileMetaAV() {
    MP4TrackInfo trackInfo = new MP4TrackInfo();
    trackInfo.setHasAudio(true);
    trackInfo.setHasVideo(true);
    trackInfo.setDuration(45000);
    trackInfo.setTimeScale(90000);
    trackInfo.setWidth(1280);
    trackInfo.setHeight(720);
    trackInfo.setAudioCodecId("mp4a");
    trackInfo.setVideoCodecId("avc1");
    trackInfo.setAvcProfile(66);
    trackInfo.setAvcLevel(42);
    trackInfo.setFps(30.0);

    MP4MetadataTagCreator creator = new MP4MetadataTagCreator(trackInfo, null);
    ITag metaTag = creator.createFileMeta();
    assertNotNull(metaTag);
    assertEquals(IoConstants.TYPE_METADATA, metaTag.getDataType());

    IoBuffer body = metaTag.getBody();
    assertNotNull(body);
    log.debug("Meta tag body limit: {}", body.limit());
    assertTrue(body.limit() > 0);
  }

  @Test
  public void testCreateFileMetaAudioOnly() {
    MP4TrackInfo trackInfo = new MP4TrackInfo();
    trackInfo.setHasAudio(true);
    trackInfo.setHasVideo(false);
    trackInfo.setDuration(4000);
    trackInfo.setTimeScale(44100);

    MP4MetadataTagCreator creator = new MP4MetadataTagCreator(trackInfo, null);
    ITag metaTag = creator.createFileMeta();
    assertNotNull(metaTag);
    assertEquals(IoConstants.TYPE_METADATA, metaTag.getDataType());
    log.debug("Audio-only meta created: {}", metaTag);
  }
}
