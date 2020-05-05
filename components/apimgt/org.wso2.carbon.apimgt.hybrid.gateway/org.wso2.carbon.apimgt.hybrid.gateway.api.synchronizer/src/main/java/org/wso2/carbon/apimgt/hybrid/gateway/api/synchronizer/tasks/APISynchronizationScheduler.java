package org.wso2.carbon.apimgt.hybrid.gateway.api.synchronizer.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.hybrid.gateway.common.config.ConfigManager;
import org.wso2.carbon.apimgt.hybrid.gateway.common.dto.ConfigDTO;
import org.wso2.carbon.apimgt.hybrid.gateway.common.exception.OnPremiseGatewayException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to schedule API Synchronization task
 **/
public class APISynchronizationScheduler {
    private static final Log log = LogFactory.getLog(APISynchronizationScheduler.class);
    private static long api_sync_period;
    private static ScheduledThreadPoolExecutor executor;

    public static void schedule(){
        try{
            ConfigDTO configDTO = ConfigManager.getConfigurationDTO();
            if(configDTO.isApi_update_task_enabled()){
                api_sync_period = configDTO.getApi_update_task_period();
                log.info("Scheduling API synchronization");
                executor = new ScheduledThreadPoolExecutor(1);
                executor.scheduleAtFixedRate(new APISynchronizationTask(), 0, api_sync_period, TimeUnit.MINUTES);
            }
        }
        catch (IllegalArgumentException | IllegalStateException | NullPointerException | OnPremiseGatewayException e){
            log.error("Error while scheduling API sync task", e);
        }
    }
}
