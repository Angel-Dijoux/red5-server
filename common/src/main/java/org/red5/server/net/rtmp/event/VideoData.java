/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.net.rtmp.event;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.VideoCodec;
import org.red5.io.ITag;
import org.red5.io.IoConstants;

/**
 * Video data event
 */
public class VideoData extends MediaDataStreamEvent<VideoData> implements IoConstants {

  private static final long serialVersionUID = 5538859593815804830L;

  public enum FrameType {
    UNKNOWN, KEYFRAME, INTERFRAME, DISPOSABLE_INTERFRAME, END_OF_SEQUENCE
  }

  private FrameType frameType = FrameType.UNKNOWN;
  private VideoCodec codec;
  private boolean config;
  private boolean endOfSequence;

  public VideoData() {
    super(IoBuffer.allocate(0).flip(), TYPE_VIDEO_DATA);
  }

  public VideoData(IoBuffer data) {
    super(data, TYPE_VIDEO_DATA);
    parseData(data);
  }

  public VideoData(IoBuffer data, boolean copy) {
    super(data, TYPE_VIDEO_DATA, copy);
    parseData(this.data);
  }

  @Override
  public void setData(IoBuffer data) {
    super.setData(data);
    parseData(data);
  }

  public FrameType getFrameType() {
    return frameType;
  }

  public int getCodecId() {
    return codec != null ? codec.getId() : -1;
  }

  public boolean isConfig() {
    return config;
  }

  public boolean isEndOfSequence() {
    return endOfSequence;
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    frameType = (FrameType) in.readObject();
    byte[] byteBuf = (byte[]) in.readObject();
    if (byteBuf != null) {
      setData(byteBuf);
    }
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(frameType);
    out.writeObject(data != null ? data.array() : null);
  }

  @Override
  protected VideoData createInstance() {
    return new VideoData();
  }

  private void parseData(IoBuffer data) {
    if (data == null || data.limit() <= 0) {
      return;
    }

    data.mark();

    int firstByte = data.get(0) & 0xFF;
    codec = VideoCodec.valueOfById(firstByte & ITag.MASK_VIDEO_CODEC);

    if (VideoCodec.getConfigured().contains(codec) && data.limit() > 1) {
      int secondByte = data.get(1) & 0xFF;
      config = (secondByte == 0);
      endOfSequence = (secondByte == 2);
    }

    switch ((firstByte & MASK_VIDEO_FRAMETYPE) >> 4) {
      case FLAG_FRAMETYPE_KEYFRAME:
        frameType = FrameType.KEYFRAME;
        break;
      case FLAG_FRAMETYPE_INTERFRAME:
        frameType = FrameType.INTERFRAME;
        break;
      case FLAG_FRAMETYPE_DISPOSABLE:
        frameType = FrameType.DISPOSABLE_INTERFRAME;
        break;
      default:
        frameType = FrameType.UNKNOWN;
    }

    data.reset();
  }

}
