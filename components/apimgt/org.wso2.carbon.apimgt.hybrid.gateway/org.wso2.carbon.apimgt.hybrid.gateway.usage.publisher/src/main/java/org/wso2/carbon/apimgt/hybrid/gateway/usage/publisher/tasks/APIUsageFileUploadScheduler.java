package org.wso2.carbon.apimgt.hybrid.gateway.usage.publisher.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.hybrid.gateway.common.config.ConfigManager;
import org.wso2.carbon.apimgt.hybrid.gateway.common.dto.ConfigDTO;
import org.wso2.carbon.apimgt.hybrid.gateway.common.exception.OnPremiseGatewayException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class APIUsageFileUploadScheduler {
    private static final Log log = LogFactory.getLog(APIUsageFileUploadScheduler.class);
    private static long api_usage_upload_sync_period;
    private static ScheduledThreadPoolExecutor executor;

    public static void schedule(){
        try{
            ConfigDTO configDTO = ConfigManager.getConfigurationDTO();
            if(configDTO.isUsage_upload_task_enabled()){
                api_usage_upload_sync_period = configDTO.getUsage_upload_task_period();
                log.info("Scheduling API Usage File Upload Task");
                executor = new ScheduledThreadPoolExecutor(1);
                executor.scheduleAtFixedRate(new APIUsageFileUploadTask(), 0, api_usage_upload_sync_period,
                        TimeUnit.MINUTES);
            }

        }
        catch (IllegalArgumentException | IllegalStateException | NullPointerException | OnPremiseGatewayException e){
            log.error("Error occurred while scheduling API Usage File Upload Task", e);
        }
    }
}
