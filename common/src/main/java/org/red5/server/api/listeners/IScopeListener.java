/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.api.listeners;

import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;

/**
 * Interface for listeners to scope events.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (bauch@struktur.de)
 */
public interface IScopeListener {

  /**
   * A scope has been created.
   *
   * @param scope the new scope
   */
  public void notifyScopeCreated(IScope scope);

  /**
   * A scope has been removed.
   *
   * @param scope the removed scope
   */
  public void notifyScopeRemoved(IScope scope);

  /**
   * A basic scope has been added.
   *
   * @param scope the added scope
   */
  public void notifyBasicScopeAdded(IBasicScope scope);

  /**
   * A basic scope has been removed.
   *
   * @param scope the removed scope
   */
  public void notifyBasicScopeRemoved(IBasicScope scope);
}
