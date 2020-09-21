/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.carbon.apimgt.keymgt.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.keymgt.internal.ServiceReferenceHolder;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.identity.oauth.event.AbstractOAuthEventInterceptor;
import org.wso2.carbon.identity.oauth2.ResponseHeader;
import org.wso2.carbon.identity.oauth2.dto.OAuthRevocationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuthRevocationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.RefreshTokenValidationDataDO;

import java.util.Collections;
import java.util.Map;

/**
 * This class provides an implementation of OAuthEventInterceptor interface in which
 * onPostTokenRevocationByClient method is overridden to handle token revocation feature logic
 */
public class APIMOAuthEventInterceptor extends AbstractOAuthEventInterceptor {

    private static final Log log = LogFactory.getLog(APIMOAuthEventInterceptor.class);

    /**
     * Overridden method to handle the post processing of token revocation
     * Called after revoking a token by oauth client
     *
     * @param revokeRequestDTO  requested revoke request object
     * @param revokeResponseDTO requested revoke response object
     * @param accessTokenDO     requested access token object
     * @param refreshTokenDO    requested refresh token object
     * @param params            requested params Map<String,Object>
     */
    @Override
    public void onPostTokenRevocationByClient(OAuthRevocationRequestDTO revokeRequestDTO,
                                              OAuthRevocationResponseDTO revokeResponseDTO, AccessTokenDO accessTokenDO,
                                              RefreshTokenValidationDataDO refreshTokenDO, Map<String, Object> params) {

        if (accessTokenDO != null) { // if accessTokenDO is not null, it implies the revocation was a success
            Object[] objects = new Object[]{accessTokenDO.getTokenId()};
            Event tokenRevocationMessage = new Event(APIConstants.TOKEN_REVOCATION_STREAM_ID, System.currentTimeMillis(),
                    null, null, objects);
            ServiceReferenceHolder.getInstance().getOutputEventAdapterService()
                    .publish(APIConstants.TOKEN_REVOCATION_EVENT_PUBLISHER, Collections.EMPTY_MAP, tokenRevocationMessage);
            log.debug("Successfully sent the revoked token notification on realtime");
        }
    }

    /**
     * Overridden method to handle the post processing of token revocation
     *
     * @param revokeRequestDTO requested revoke request object
     * @param revokeRespDTO    requested revoke request object
     * @param accessTokenDO    requested Access token object
     * @param params           requested params Map<String,Object>
     */
    @Override
    public void onPostTokenRevocationByResourceOwner(
            org.wso2.carbon.identity.oauth.dto.OAuthRevocationRequestDTO revokeRequestDTO,
            org.wso2.carbon.identity.oauth.dto.OAuthRevocationResponseDTO revokeRespDTO, AccessTokenDO accessTokenDO,
            Map<String, Object> params) {

        if (accessTokenDO != null) { // if accessTokenDO is not null, it implies the revocation was a success
            Object[] objects = new Object[]{accessTokenDO.getTokenId()};
            Event tokenRevocationMessage = new Event(APIConstants.TOKEN_REVOCATION_STREAM_ID, System.currentTimeMillis(),
                    null, null, objects);
            ServiceReferenceHolder.getInstance().getOutputEventAdapterService()
                    .publish(APIConstants.TOKEN_REVOCATION_EVENT_PUBLISHER, Collections.EMPTY_MAP, tokenRevocationMessage);
            log.debug("Successfully sent the revoked token notification on realtime");
        }
    }
    
    /**
     * Overridden method in which default implementation is to check the identity.xml for registered event listener
     *
     * @return boolean
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
