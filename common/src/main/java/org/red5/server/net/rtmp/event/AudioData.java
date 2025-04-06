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
import org.red5.codec.AudioCodec;
import org.red5.io.ITag;

public class AudioData extends MediaDataStreamEvent<AudioData> {

  private static final long serialVersionUID = -4102940670913999407L;

  protected IoBuffer data;

  protected AudioCodec codec;

  /** True if this is configuration data and false otherwise */
  protected boolean config;

  public AudioData() {
    super(TYPE_AUDIO_DATA);
  }

  public AudioData(IoBuffer data) {
    super(data, TYPE_AUDIO_DATA);
  }

  public AudioData(IoBuffer data, boolean copy) {
    super(data, TYPE_AUDIO_DATA, copy);
  }

  @Override
  public void setData(IoBuffer data) {
    super.setData(data);
    if (data != null && data.limit() > 0) {
      data.mark();
      codec = AudioCodec.valueOfById(((data.get(0) & 0xff) & ITag.MASK_SOUND_FORMAT) >> 4);
      if (AudioCodec.getConfigured().contains(codec)) {
        config = (data.get() == 0);
      }
      data.reset();
    }
  }

  public int getCodecId() {
    return codec.getId();
  }

  public boolean isConfig() {
    return config;
  }

  /**
   * Duplicate this message / event.
   *
   * @return duplicated event
   */
  public AudioData duplicate() throws IOException, ClassNotFoundException {
    AudioData result = super.duplicate();
    result.codec = this.codec;
    result.config = this.config;
    return result;
  }

  @Override
  protected AudioData createInstance() {
    return new AudioData();
  }

}
