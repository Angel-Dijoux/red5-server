package org.red5.io.mp4.impl;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.io.flv.impl.Tag;

public class MP4PreStreamingTagCreator {

  private final MP4TrackInfo trackInfo;

  public MP4PreStreamingTagCreator(MP4TrackInfo trackInfo) {
    this.trackInfo = trackInfo;
  }

  public ITag createVideoConfigTag(int timestamp) {
    if (!trackInfo.hasVideo() || trackInfo.getVideoDecoderBytes() == null) {
      return null;
    }
    byte[] prefix = MP4Reader.PREFIX_VIDEO_CONFIG_FRAME;
    byte[] decoderBytes = trackInfo.getVideoDecoderBytes();

    IoBuffer body = IoBuffer.allocate(prefix.length + decoderBytes.length);
    body.setAutoExpand(true);
    body.put(prefix);
    body.put(decoderBytes);
    body.flip();

    Tag tag = new Tag(IoConstants.TYPE_VIDEO, timestamp, body.limit(), null, 0);
    tag.setBody(body);
    return tag;
  }

  public ITag createAudioConfigTag(int timestamp) {
    if (!trackInfo.hasAudio() || trackInfo.getAudioDecoderBytes() == null) {
      return null;
    }
    byte[] prefix = MP4Reader.PREFIX_AUDIO_CONFIG_FRAME;
    byte[] decoderBytes = trackInfo.getAudioDecoderBytes();

    IoBuffer body = IoBuffer.allocate(prefix.length + decoderBytes.length + 1);
    body.setAutoExpand(true);
    body.put(prefix);
    body.put(decoderBytes);
    body.put((byte) 0x06); // optional
    body.flip();

    Tag tag = new Tag(IoConstants.TYPE_AUDIO, timestamp, body.limit(), null, 0);
    tag.setBody(body);
    return tag;
  }
}
