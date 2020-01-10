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
 *
 */

package org.apache.skywalking.oap.server.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.skywalking.oap.server.core.CoreModuleConfig;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.query.entity.ProfileTask;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * cache need to execute profile task
 *
 * @author MrPro
 */
public class ProfileTaskCache implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileTaskCache.class);

    private final Cache<Integer, List<ProfileTask>> profileTaskCache;

    private final ModuleManager moduleManager;

    public ProfileTaskCache(ModuleManager moduleManager, CoreModuleConfig moduleConfig) {
        this.moduleManager = moduleManager;

        long initialSize = moduleConfig.getMaxSizeOfProfileTask() / 10L;
        int initialCapacitySize = (int)(initialSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : initialSize);

        profileTaskCache = CacheBuilder.newBuilder().initialCapacity(initialCapacitySize).maximumSize(moduleConfig.getMaxSizeOfProfileTask())
                // remove old profile task data
                .expireAfterWrite(Duration.ofMinutes(1)).build();
    }

    /**
     * query executable profile task
     * @param serviceId
     * @return
     */
    public List<ProfileTask> getProfileTaskList(int serviceId) {
        // read profile task list from cache only, use cache update timer mechanism
        List<ProfileTask> profileTaskList = profileTaskCache.getIfPresent(serviceId);
        return profileTaskList;
    }

    /**
     * save service task list
     * @param serviceId
     * @param taskList
     */
    public void saveTaskList(int serviceId, List<ProfileTask> taskList) {
        if (taskList == null) {
            taskList = Collections.emptyList();
        }

        profileTaskCache.put(serviceId, taskList);
    }

    /**
     * use for every db query
     * @return
     */
    public long getCacheStartTimeBucket() {
        return TimeBucket.getRecordTimeBucket(System.currentTimeMillis());
    }

    /**
     * use for every db query, +10 start time and +15 end time(because use task end time to search)
     * @return
     */
    public long getCacheEndTimeBucket() {
        return TimeBucket.getRecordTimeBucket(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(25));
    }
}
