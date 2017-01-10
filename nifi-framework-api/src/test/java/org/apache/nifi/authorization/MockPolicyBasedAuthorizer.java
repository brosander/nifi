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
package org.apache.nifi.authorization;

import org.apache.nifi.authorization.exception.AuthorizationAccessException;
import org.apache.nifi.authorization.exception.AuthorizerCreationException;
import org.apache.nifi.authorization.exception.AuthorizerDestructionException;

import java.util.HashSet;
import java.util.Set;

/**
 * Mock implementation of AbstractPolicyBasedAuthorizer.
 */
public class MockPolicyBasedAuthorizer extends AbstractPolicyBasedAuthorizer {

    private Set<Group> groups = new HashSet<>();
    private Set<User> users = new HashSet<>();
    private Set<AccessPolicy> policies = new HashSet<>();

    public MockPolicyBasedAuthorizer() {

    }

    public MockPolicyBasedAuthorizer(Set<Group> groups, Set<User> users, Set<AccessPolicy> policies) {
        if (groups != null) {
            this.groups.addAll(groups);
        }
        if (users != null) {
            this.users.addAll(users);
        }
        if (policies != null) {
            this.policies.addAll(policies);
        }
    }

    @Override
    public Group doAddGroup(Group group) throws AuthorizationAccessException {
        groups.add(group);
        return group;
    }

    @Override
    public Group getGroup(String identifier) throws AuthorizationAccessException {
        for (Group group : groups) {
            if (group.getIdentifier().equals(identifier)) {
                return group;
            }
        }
        return null;
    }

    @Override
    public Group doUpdateGroup(Group group) throws AuthorizationAccessException {
        deleteGroup(group);
        return addGroup(group);
    }

    @Override
    public Group deleteGroup(Group group) throws AuthorizationAccessException {
        groups.remove(group);
        return group;
    }

    @Override
    public Set<Group> getGroups() throws AuthorizationAccessException {
        return groups;
    }

    @Override
    public User doAddUser(User user) throws AuthorizationAccessException {
        users.add(user);
        return user;
    }

    @Override
    public User getUser(String identifier) throws AuthorizationAccessException {
        for (User user : users) {
            if (user.getIdentifier().equals(identifier)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User getUserByIdentity(String identity) throws AuthorizationAccessException {
        for (User user : users) {
            if (user.getIdentity().equals(identity)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User doUpdateUser(User user) throws AuthorizationAccessException {
        deleteUser(user);
        return addUser(user);
    }

    @Override
    public User deleteUser(User user) throws AuthorizationAccessException {
        users.remove(user);
        return user;
    }

    @Override
    public Set<User> getUsers() throws AuthorizationAccessException {
        return users;
    }

    @Override
    protected AccessPolicy doAddAccessPolicy(AccessPolicy accessPolicy) throws AuthorizationAccessException {
        policies.add(accessPolicy);
        return accessPolicy;
    }

    @Override
    public AccessPolicy getAccessPolicy(String identifier) throws AuthorizationAccessException {
        for (AccessPolicy policy : policies) {
            if (policy.getIdentifier().equals(identifier)) {
                return policy;
            }
        }
        return null;
    }

    @Override
    public AccessPolicy updateAccessPolicy(AccessPolicy accessPolicy) throws AuthorizationAccessException {
        deleteAccessPolicy(accessPolicy);
        return addAccessPolicy(accessPolicy);
    }

    @Override
    public AccessPolicy deleteAccessPolicy(AccessPolicy policy) throws AuthorizationAccessException {
        policies.remove(policy);
        return policy;
    }

    @Override
    public Set<AccessPolicy> getAccessPolicies() throws AuthorizationAccessException {
        return policies;
    }

    @Override
    public UsersAndAccessPolicies getUsersAndAccessPolicies() throws AuthorizationAccessException {
        return new UsersAndAccessPolicies() {
            @Override
            public AccessPolicy getAccessPolicy(String resourceIdentifier, RequestAction action) {
                return null;
            }

            @Override
            public User getUser(String identity) {
                return getUserByIdentity(identity);
            }

            @Override
            public Set<Group> getGroups(String userIdentity) {
                User user = getUserByIdentity(userIdentity);
                if (user == null) {
                    return new HashSet<>();
                } else {
                    Set<Group> result = new HashSet<>();
                    for (Group group : groups) {
                        if (group.getUsers().contains(user.getIdentifier())) {
                            result.add(group);
                        }
                    }
                    return result;
                }
            }
        };
    }

    @Override
    public void initialize(AuthorizerInitializationContext initializationContext) throws AuthorizerCreationException {

    }

    @Override
    public void doOnConfigured(AuthorizerConfigurationContext configurationContext) throws AuthorizerCreationException {

    }

    @Override
    public void preDestruction() throws AuthorizerDestructionException {

    }

}
