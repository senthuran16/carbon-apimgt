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
public class APIUsageFileUploadScheduler {

    private static final Log log = LogFactory.getLog(APIUsageFileUploadScheduler.class);

    public static void schedule() {

        try {
            ConfigDTO configDTO = ConfigManager.getConfigurationDTO();
            if (configDTO.isUsage_upload_task_enabled()) {
                long syncPeriod = configDTO.getUsage_upload_task_period();
                // Setting thread name
                ThreadFactory threadFactory = runnable -> new Thread(runnable, "APIUsageFileUploadTask");
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
                executor.scheduleAtFixedRate(new APIUsageFileUploadTask(), 0, syncPeriod,
                        TimeUnit.MINUTES);
                log.info("API Usage File Upload has been successfully scheduled at a rate of once every " +
                        syncPeriod + " minutes");
            }
        } catch (OnPremiseGatewayException e) {
            log.error("Error occurred while scheduling API Usage File Upload Task", e);
        }
    }
}
