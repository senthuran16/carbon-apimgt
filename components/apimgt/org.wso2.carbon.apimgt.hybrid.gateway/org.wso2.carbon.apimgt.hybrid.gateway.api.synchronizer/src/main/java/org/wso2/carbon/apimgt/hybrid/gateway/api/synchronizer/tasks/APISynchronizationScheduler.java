package org.wso2.carbon.apimgt.hybrid.gateway.api.synchronizer.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.hybrid.gateway.common.config.ConfigManager;
import org.wso2.carbon.apimgt.hybrid.gateway.common.dto.ConfigDTO;
import org.wso2.carbon.apimgt.hybrid.gateway.common.exception.OnPremiseGatewayException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Class for scheduling API Synchronization task
 **/
public class APISynchronizationScheduler {

    private static final Log log = LogFactory.getLog(APISynchronizationScheduler.class);

    public static void schedule() {

        try {
            ConfigDTO configDTO = ConfigManager.getConfigurationDTO();
            if (configDTO.isApi_update_task_enabled()) {
                long syncPeriod = configDTO.getApi_update_task_period();
                // Setting thread name
                ThreadFactory threadFactory = runnable -> new Thread(runnable, "APISyncTask");
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
                executor.scheduleAtFixedRate(new APISynchronizationTask(), 0,
                        syncPeriod, TimeUnit.MINUTES);
                log.info("API Synchronization has been successfully scheduled once every " + syncPeriod +
                        " minutes");
            }
        } catch (OnPremiseGatewayException e) {
            log.error("Error while scheduling API sync task", e);
        }
    }
}
