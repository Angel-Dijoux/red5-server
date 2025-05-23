/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.api.statistics;

/**
 * Statistical informations about a stream that is subscribed by a client.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IPlaylistSubscriberStreamStatistics extends IStreamStatistics {

  /**
   * Return total number of bytes sent to the client from this stream.
   *
   * @return number of bytes
   */
  public long getBytesSent();

  /**
   * Return the buffer duration as requested by the client.
   *
   * @return the buffer duration in milliseconds
   */
  public int getClientBufferDuration();

  /**
   * Return estimated fill ratio of the client buffer.
   *
   * @return fill ratio in percent
   */
  public double getEstimatedBufferFill();
}
