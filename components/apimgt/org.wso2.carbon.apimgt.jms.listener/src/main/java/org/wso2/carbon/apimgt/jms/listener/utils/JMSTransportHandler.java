/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.apimgt.jms.listener.utils;


import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.jms.listener.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.dto.ThrottleProperties;

import java.io.IOException;
import java.util.*;

public class JMSTransportHandler {
    private static final Log log = LogFactory.getLog(JMSTransportHandler.class);
    private ThrottleProperties.JMSConnectionProperties jmsConnectionProperties;
    private JMSConnectionFactory jmsConnectionFactory;
    private List<JMSListener> jmsListeners = new ArrayList<JMSListener>();
    private boolean stopIssued = false;
    private static final Object lock = new Object();

    public JMSTransportHandler() {
        if (ServiceReferenceHolder.getInstance().getAPIMConfiguration() != null) {
            jmsConnectionProperties =
                    ServiceReferenceHolder.getInstance().getAPIMConfiguration().getThrottleProperties()
                            .getJmsConnectionProperties();
        }
    }

    /**
     * This method will used to subscribe JMS and update throttle data map.
     * Then this will listen to topic updates and check all update and update throttle data map accordingly.
     */
    public void subscribeForJmsEvents() {
        createJMSListener(APIConstants.TopicNames.TOPIC_THROTTLE_DATA);
        createJMSListener(APIConstants.TopicNames.TOPIC_TOKEN_REVOCATION);
    }

    private void createJMSListener(String topicName) {
        Properties properties;
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        try {
            if (jmsConnectionProperties.getJmsConnectionProperties().isEmpty()) {
                properties = new Properties();
                ClassLoader classLoader = getClass().getClassLoader();
                properties.load(classLoader.getResourceAsStream(ListenerConstants.MB_PROPERTIES));
            } else {
                properties = jmsConnectionProperties.getJmsConnectionProperties();
            }
            for (final String name : properties.stringPropertyNames()) {
                parameters.put(name, properties.getProperty(name));
            }
            jmsConnectionFactory = new JMSConnectionFactory(parameters,
                    ListenerConstants
                            .CONNECTION_FACTORY_NAME);
            Map<String, String> messageConfig = new HashMap<String, String>();
            messageConfig.put(JMSConstants.PARAM_DESTINATION, topicName);
            int minThreadPoolSize = jmsConnectionProperties.getJmsTaskManagerProperties().getMinThreadPoolSize();
            int maxThreadPoolSize = jmsConnectionProperties.getJmsTaskManagerProperties().getMaxThreadPoolSize();
            int keepAliveTimeInMillis = jmsConnectionProperties.getJmsTaskManagerProperties()
                    .getKeepAliveTimeInMillis();
            int jobQueueSize = jmsConnectionProperties.getJmsTaskManagerProperties().getJobQueueSize();
            JMSTaskManager jmsTaskManager = JMSTaskManagerFactory.createTaskManagerForService(jmsConnectionFactory,
                    ListenerConstants.CONNECTION_FACTORY_NAME,
                    new NativeWorkerPool(minThreadPoolSize, maxThreadPoolSize,
                            keepAliveTimeInMillis, jobQueueSize, "JMS Threads",
                            "JMSThreads" + UUID.randomUUID().toString()), messageConfig);
            jmsTaskManager.setJmsMessageListener(new JMSMessageListener(ServiceReferenceHolder.getInstance()
                    .getThrottleDataHolder()));

            JMSListener jmsListener =
                    new JMSListener(ListenerConstants.CONNECTION_FACTORY_NAME + "#" + topicName,
                    jmsTaskManager);
            jmsListeners.add(jmsListener);
            jmsListener.startListener();
            log.info("Starting jms topic[" + topicName + "] consumer thread...");

        } catch (IOException e) {
            log.error("Cannot read properties file from resources. " + e.getMessage(), e);
        }
    }

    public void unSubscribeFromEvents() {

        log.info("Starting to Shutdown the Listener...");

        if (!stopIssued && jmsConnectionFactory != null) {
            // To prevent multiple components executing stop at the same time,
            // we are checking if a shutdown triggered by a previous thread is in progress.
            synchronized (lock) {
                if (!stopIssued) {
                    stopIssued = true;
                    log.debug("Stopping JMS Listeners");
                    for (JMSListener jmsListener : jmsListeners) {
                        jmsListener.stopListener();
                    }
                    log.debug("JMS Listeners Stopped");
                    jmsConnectionFactory.stop();
                    log.debug("JMS Connection Factory Stopped");
                }
            }
        }
    }

}
