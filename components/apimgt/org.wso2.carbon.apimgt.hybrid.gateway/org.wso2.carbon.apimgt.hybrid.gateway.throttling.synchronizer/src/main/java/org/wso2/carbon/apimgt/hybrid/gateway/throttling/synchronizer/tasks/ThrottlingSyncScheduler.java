package org.wso2.carbon.apimgt.hybrid.gateway.throttling.synchronizer.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.hybrid.gateway.common.config.ConfigManager;
import org.wso2.carbon.apimgt.hybrid.gateway.common.dto.ConfigDTO;
import org.wso2.carbon.apimgt.hybrid.gateway.common.exception.OnPremiseGatewayException;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to schedule Throttling Synchronization
 **/
public class ThrottlingSyncScheduler {
    private static final Log log = LogFactory.getLog(ThrottlingSyncScheduler.class);
    private static ScheduledThreadPoolExecutor executor;
    private static long throttling_sync_period;

    public static void schedule(){
        try{
            ConfigDTO configDTO = ConfigManager.getConfigurationDTO();
            if(configDTO.isThrottling_synchronization_task_enabled()){
                throttling_sync_period = configDTO.getApi_update_task_period();
                log.info("Scheduling Throttling Sync Task");
                executor = new ScheduledThreadPoolExecutor(1);
                executor.scheduleAtFixedRate(new ThrottlingSyncTask(), 0, throttling_sync_period, TimeUnit.MINUTES);
            }

        }
        catch (IllegalArgumentException | IllegalStateException | NullPointerException | OnPremiseGatewayException e){
            log.error("Error occurred while scheduling throttling sync task", e);
        }
    }
}
