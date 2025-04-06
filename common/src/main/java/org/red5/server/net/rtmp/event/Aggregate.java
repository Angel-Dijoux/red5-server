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
import java.util.LinkedList;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;
import org.red5.io.utils.IOUtils;
import org.red5.server.net.rtmp.message.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Aggregate data event */
public class Aggregate extends MediaDataStreamEvent<Aggregate> implements IoConstants {

  private static final long serialVersionUID = 5538859593815804830L;

  private static Logger log = LoggerFactory.getLogger(Aggregate.class);

  /** Data */
  protected IoBuffer data;

  /** Constructs a new Aggregate. */
  public Aggregate() {
    super(IoBuffer.allocate(0).flip(), TYPE_AGGREGATE);
  }

  /**
   * Create aggregate data event with given data buffer.
   *
   * @param data data
   */
  public Aggregate(IoBuffer data) {
    super(data, TYPE_AGGREGATE);
  }

  /**
   * Create aggregate data event with given data buffer.
   *
   * @param data aggregate data
   * @param copy true to use a copy of the data or false to use reference
   */
  public Aggregate(IoBuffer data, boolean copy) {
    super(data, TYPE_AGGREGATE, copy);
  }

  public void setData(IoBuffer data) {
    this.data = data;
  }

  public void setData(byte[] data) {
    this.data = IoBuffer.allocate(data.length);
    this.data.put(data).flip();
  }

  /**
   * Breaks-up the aggregate into its individual parts and returns them as a list.
   * The parts are
   * returned based on the ordering of the aggregate itself.
   *
   * @return list of IRTMPEvent objects
   */

  public LinkedList<IRTMPEvent> getParts() {
    LinkedList<IRTMPEvent> parts = new LinkedList<>();
    log.trace("Aggregate data length: {}", data.limit());
    int position = data.position();

    while (position < data.limit()) {
      try {
        byte subType = data.get();
        if (subType == 0) {
          log.debug("Subtype 0 encountered within this aggregate, processing with exit");
          break;
        }

        int size = IOUtils.readUnsignedMediumInt(data);
        int timestamp = IOUtils.readExtendedMediumInt(data);
        int streamId = IOUtils.readUnsignedMediumInt(data);

        Header partHeader = createHeader(subType, size, timestamp, streamId);

        IRTMPEvent part = parseEvent(subType, size, timestamp, partHeader);
        if (part != null) {
          parts.add(part);
          validateBackPointer(size);
        }

        position = data.position();
        log.trace("Data position: {}", position);

      } catch (Exception e) {
        log.error("Exception decoding aggregate parts", e);
        break;
      }
    }

    log.trace("Aggregate processing complete, {} parts extracted", parts.size());
    return parts;
  }

  private Header createHeader(byte subType, int size, int timestamp, int streamId) {
    Header header = new Header();
    header.setChannelId(this.header.getChannelId());
    header.setDataType(subType);
    header.setSize(size);
    header.setStreamId(this.header.getStreamId());
    header.setTimer(timestamp);
    return header;
  }

  private IRTMPEvent parseEvent(byte subType, int size, int timestamp, Header header) {
    switch (subType) {
      case TYPE_AUDIO_DATA:
        return createAudioData(size, timestamp, header);
      case TYPE_VIDEO_DATA:
        return createVideoData(size, timestamp, header);
      default:
        return createUnknownData(subType, size, timestamp, header);
    }
  }

  private AudioData createAudioData(int size, int timestamp, Header header) {
    AudioData audio = new AudioData(data.getSlice(size));
    audio.setTimestamp(timestamp);
    audio.setHeader(header);
    log.debug("Audio header: {}", audio.getHeader());
    return audio;
  }

  private VideoData createVideoData(int size, int timestamp, Header header) {
    VideoData video = new VideoData(data.getSlice(size));
    video.setTimestamp(timestamp);
    video.setHeader(header);
    log.debug("Video header: {}", video.getHeader());
    return video;
  }

  private Unknown createUnknownData(byte subType, int size, int timestamp, Header header) {
    Unknown unknown = new Unknown(subType, data.getSlice(size));
    unknown.setTimestamp(timestamp);
    unknown.setHeader(header);
    log.debug("Non-A/V subtype: {}", subType);
    return unknown;
  }

  private void validateBackPointer(int size) {
    if (data.position() < data.limit() - 4) {
      int backPointer = data.getInt();
      if (backPointer != (size + 11)) {
        log.debug("Data size ({}) and back pointer ({}) did not match", size, backPointer);
      }
    }
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    byte[] byteBuf = (byte[]) in.readObject();
    if (byteBuf != null) {
      data = IoBuffer.allocate(byteBuf.length);
      data.setAutoExpand(true);
      SerializeUtils.ByteArrayToByteBuffer(byteBuf, data);
    }
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    if (data != null) {
      out.writeObject(SerializeUtils.ByteBufferToByteArray(data));
    } else {
      out.writeObject(null);
    }
  }

  @Override
  protected Aggregate createInstance() {
    return new Aggregate();
  }

}
