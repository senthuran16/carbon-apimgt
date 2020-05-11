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

package org.wso2.carbon.apimgt.hybrid.gateway.usage.publisher.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.hybrid.gateway.common.config.ConfigManager;
import org.wso2.carbon.apimgt.hybrid.gateway.common.dto.ConfigDTO;
import org.wso2.carbon.apimgt.hybrid.gateway.common.exception.OnPremiseGatewayException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Class for scheduling APIUsageFileCleanupTask
 */
public class APIUsageFileCleanupScheduler {

    private static final Log log = LogFactory.getLog(APIUsageFileCleanupScheduler.class);

    public static void schedule() {

        try {
            ConfigDTO configDTO = ConfigManager.getConfigurationDTO();
            if (configDTO.isUsage_upload_cleanup_task_enabled()) {
                long syncPeriod = configDTO.getUsage_upload_cleanup_task_period();
                //Setting Thread name
                ThreadFactory threadFactory = runnable -> new Thread(runnable, "APIUsageFileCleanupTask");
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
                executor.scheduleAtFixedRate(new APIUsageFileCleanupTask(
                                Long.toString(configDTO.getUsage_upload_retention_days())), 0, syncPeriod,
                        TimeUnit.MINUTES);
                log.info("API Usage File Cleanup has been successfully scheduled at a rate of once every " +
                        syncPeriod + " minutes");
            }
        } catch (OnPremiseGatewayException e) {
            log.error("Error while scheduling API Usage File Cleanup Task", e);
        }
    }
}
