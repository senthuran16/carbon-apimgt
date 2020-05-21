/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.apimgt.impl;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.apimgt.api.NewPostLoginExecutor;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.identity.openidconnect.OIDCConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.text.ParseException;

/**
 * GroupID extractor implementation for OpenID Connect authentication.
 */
public class OIDCGroupIDExtractorImpl implements NewPostLoginExecutor {

    private static final Log log = LogFactory.getLog(OIDCGroupIDExtractorImpl.class);

    @Override
    public String[] getGroupingIdentifierList(String loginResponse) {
        String username = null;
        String[] groupIds = null;
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String claim = config.getFirstProperty(APIConstants.API_STORE_GROUP_EXTRACTOR_CLAIM_URI);

        if (StringUtils.isBlank(claim)) {
            claim = APIConstants.DEFAULT_GROUP_CLAIM;
        }

        try {
            username = getUsername(loginResponse);

            RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);

            UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
            UserStoreManager manager = realm.getUserStoreManager();
            String org = manager.getUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), claim, null);

            if (org != null) {
                if (org.contains(",")) {
                    groupIds = org.split(",");

                    for (int i = 0; i < groupIds.length; i++) {
                        groupIds[i] = groupIds[i].trim();
                    }
                } else {
                    org = org.trim();
                    groupIds = new String[]{org};
                }
            } else {
                // If claim is null then returning a empty string
                groupIds = new String[]{};
            }
        } catch (JSONException e) {
            log.error("Exception occurred while trying to get group Identifier from login response", e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            log.error("Error while checking user existence for " + username, e);
        } catch (ParseException e) {
            log.error("Error while parsing the assertion", e);
        }

        return groupIds;
    }

    @Override
    public String getGroupingIdentifiers(String loginResponse) {
        String username = null;
        String org = null;
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String claim = config.getFirstProperty(APIConstants.API_STORE_GROUP_EXTRACTOR_CLAIM_URI);

        if (StringUtils.isBlank(claim)) {
            claim = APIConstants.DEFAULT_GROUP_CLAIM;
        }

        try {
            username = getUsername(loginResponse);

            RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);

            UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
            UserStoreManager manager = realm.getUserStoreManager();
            org = manager.getUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), claim, null);

            if (org != null) {
                org = tenantDomain + "/" + org.trim();
            }
        } catch (ParseException e) {
            log.error("Error while parsing the assertion", e);
        } catch (JSONException e) {
            log.error("Exception occurred while trying to get group Identifier from login response", e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            log.error("Error while checking user existence for " + username, e);
        }

        return org;
    }

    /**
     * Retrieve username from the login response.
     *
     * @param loginResponse response received from the identity provider
     * @return username of the current logged in user
     * @throws JSONException error when parsing response to json object
     * @throws ParseException error while parsing the id token
     */
    protected String getUsername(String loginResponse) throws JSONException, ParseException {
        // get username from 'sub' claim
        JSONObject obj = new JSONObject(loginResponse);
        JWT idToken = JWTParser.parse(obj.get(OIDCConstants.ID_TOKEN).toString());

        return idToken.getJWTClaimsSet().getSubject();
    }
}
