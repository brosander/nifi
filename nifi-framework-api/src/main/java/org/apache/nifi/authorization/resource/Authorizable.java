/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.authorization.resource;

import org.apache.nifi.authorization.AuthorizationResult;
import org.apache.nifi.authorization.Authorizer;
import org.apache.nifi.authorization.RequestAction;
import org.apache.nifi.authorization.Resource;
import org.apache.nifi.authorization.user.NiFiUser;

import java.util.Map;

public interface Authorizable {

    /**
     * The parent for this Authorizable. May be null.
     *
     * @return the parent authorizable or null
     */
    Authorizable getParentAuthorizable();

    /**
     * The Resource for this Authorizable.
     *
     * @return the resource
     */
    Resource getResource();

    /**
     * Returns whether the current user is authorized for the specified action on the specified resource. This
     * method does not imply the user is directly attempting to access the specified resource. If the user is
     * attempting a direct access use Authorizable.authorize().
     *
     * @param authorizer authorizer
     * @param action action
     * @return is authorized
     */
    boolean isAuthorized(Authorizer authorizer, RequestAction action, NiFiUser user);

    /**
     * Returns the result of an authorization request for the specified user for the specified action on the specified
     * resource. This method does not imply the user is directly attempting to access the specified resource. If the user is
     * attempting a direct access use Authorizable.authorize().
     *
     * @param authorizer authorizer
     * @param action action
     * @param user user
     * @return is authorized
     */
    AuthorizationResult checkAuthorization(Authorizer authorizer, RequestAction action, NiFiUser user, Map<String, String> resourceContext);

    /**
     * Returns the result of an authorization request for the specified user for the specified action on the specified
     * resource. This method does not imply the user is directly attempting to access the specified resource. If the user is
     * attempting a direct access use Authorizable.authorize().
     *
     * @param authorizer authorizer
     * @param action action
     * @param user user
     * @return is authorized
     */
    AuthorizationResult checkAuthorization(Authorizer authorizer, RequestAction action, NiFiUser user);

    /**
     * Authorizes the current user for the specified action on the specified resource. This method does imply the user is
     * directly accessing the specified resource.
     *
     * @param authorizer authorizer
     * @param action action
     * @param user user
     * @param resourceContext resource context
     */
    void authorize(Authorizer authorizer, RequestAction action, NiFiUser user, Map<String, String> resourceContext);

    /**
     * Authorizes the current user for the specified action on the specified resource. This method does imply the user is
     * directly accessing the specified resource.
     *
     * @param authorizer authorizer
     * @param action action
     * @param user user
     */
    void authorize(Authorizer authorizer, RequestAction action, NiFiUser user);
}
