/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.hybrid.gateway.throttling.synchronizer.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.hybrid.gateway.common.config.ConfigManager;
import org.wso2.carbon.apimgt.hybrid.gateway.common.dto.ConfigDTO;
import org.wso2.carbon.apimgt.hybrid.gateway.common.exception.OnPremiseGatewayException;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Class for scheduling Throttling Synchronization task
 **/
public class ThrottlingSyncScheduler {

    private static final Log log = LogFactory.getLog(ThrottlingSyncScheduler.class);

    public static void schedule() {

        try {
            ConfigDTO configDTO = ConfigManager.getConfigurationDTO();
            if (configDTO.isThrottling_synchronization_task_enabled()) {
                long syncPeriod = configDTO.getThrottling_synchronization_task_period();
                // Setting thread name
                ThreadFactory threadFactory = runnable -> new Thread(runnable, "ThrottlingSyncTask");
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
                executor.scheduleAtFixedRate(new ThrottlingSyncTask(), 0,
                        syncPeriod, TimeUnit.MINUTES);
                log.info("Throttling Synchronization has been successfully scheduled once every " +
                        syncPeriod + " minutes");
            }

        } catch (OnPremiseGatewayException e) {
            log.error("Error occurred while scheduling throttling sync task", e);
        }
    }
}
