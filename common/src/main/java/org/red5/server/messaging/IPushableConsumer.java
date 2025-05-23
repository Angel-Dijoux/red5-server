/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.messaging;

import java.io.IOException;

/**
 * A consumer that supports event-driven message handling and message pushing through pipes.
 *
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IPushableConsumer extends IConsumer {
  public static final String KEY = IPushableConsumer.class.getName();

  /**
   * Pushes message through pipe
   *
   * @param pipe Pipe
   * @param message Message
   * @throws IOException if message could not be written
   */
  void pushMessage(IPipe pipe, IMessage message) throws IOException;
}
