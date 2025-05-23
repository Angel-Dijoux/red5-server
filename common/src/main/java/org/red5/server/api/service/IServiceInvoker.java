/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.api.service;

import org.red5.server.api.scope.IScope;

/**
 * Interface for objects that execute service calls (remote calls from client).
 *
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IServiceInvoker {

  /**
   * Execute the passed service call in the given scope. This looks up the handler for the call in
   * the scope and the context of the scope.
   *
   * @param call the call to invoke
   * @param scope the scope to search for a handler
   * @return
   *     <pre>
   * true
   * </pre>
   *     if the call was performed, otherwise
   *     <pre>
   * false
   * </pre>
   */
  boolean invoke(IServiceCall call, IScope scope);

  /**
   * Execute the passed service call in the given object.
   *
   * @param call the call to invoke
   * @param service the service to use
   * @return
   *     <pre>
   * true
   * </pre>
   *     if the call was performed, otherwise
   *     <pre>
   * false
   * </pre>
   */
  boolean invoke(IServiceCall call, Object service);
}
