/*
 *Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.apimgt.keymgt.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.token.ClaimsRetriever;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.keymgt.MethodStats;
import org.wso2.carbon.apimgt.keymgt.service.TokenValidationContext;
import org.wso2.carbon.claim.mgt.ClaimManagementException;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataHandler;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCache;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheEntry;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheKey;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;

import static org.apache.commons.collections.MapUtils.isNotEmpty;

@MethodStats
public class JWTGenerator extends AbstractJWTGenerator {

    private static final Log log = LogFactory.getLog(JWTGenerator.class);
    private static final String DISABLE_AUD_CLAIM = "apim.disableAudClaim";

    @Override
    public Map<String, String> populateStandardClaims(TokenValidationContext validationContext)
            throws APIManagementException {

        //generating expiring timestamp
        long currentTime = System.currentTimeMillis();
        long expireIn = currentTime + getTTL() * 1000;

        Date currentTimeDate = new Date(currentTime);
        String dialect;
        ClaimsRetriever claimsRetriever = getClaimsRetriever();
        if (claimsRetriever != null) {
            dialect = claimsRetriever.getDialectURI(validationContext.getValidationInfoDTO().getEndUserName());
        } else {
            dialect = getDialectURI();
        }

        String subscriber = validationContext.getValidationInfoDTO().getSubscriber();
        String applicationName = validationContext.getValidationInfoDTO().getApplicationName();
        String applicationId = validationContext.getValidationInfoDTO().getApplicationId();
        String tier = validationContext.getValidationInfoDTO().getTier();
        String endUserName = validationContext.getValidationInfoDTO().getEndUserName();
        String keyType = validationContext.getValidationInfoDTO().getType();
        String userType = validationContext.getValidationInfoDTO().getUserType();
        String applicationTier = validationContext.getValidationInfoDTO().getApplicationTier();
        String enduserTenantId = String.valueOf(APIUtil.getTenantId(endUserName));
        Application application = getApplicationbyId(Integer.parseInt(applicationId));
        String uuid = null;
        Map<String, String> appAttributes = null;
        if (application != null) {
            appAttributes = application.getApplicationAttributes();
            uuid = application.getUUID();
        }
        String usernameWithoutTenantDomain = MultitenantUtils.getTenantAwareUsername(endUserName);
        Map<String, String> claims = new LinkedHashMap<String, String>(20);
        OAuthAppDO oAuthAppDO = null;
        try {
            oAuthAppDO = OAuth2Util.
                    getAppInformationByClientId(validationContext.getValidationInfoDTO().getConsumerKey());
        } catch (IdentityOAuth2Exception e) {
            log.error("Error occurred while getting JWT Token client ID : "
                    + validationContext.getValidationInfoDTO().getConsumerKey() + " when getting oAuth App " +
                    "information", e);
            throw new APIManagementException("Error occurred while getting JWT Token client ID : "
                    + validationContext.getValidationInfoDTO().getConsumerKey(), e);
        } catch (InvalidOAuthClientException e) {
            log.warn("Error occurred while getting JWT Token client ID : "
                    + validationContext.getValidationInfoDTO().getConsumerKey() + " when getting oAuth App " +
                    "information. This may result in not having the audience claim in the backend jwt token.", e);
        }
        boolean disableAudClaim = Boolean.parseBoolean(System.getProperty(DISABLE_AUD_CLAIM));
        if (oAuthAppDO != null && oAuthAppDO.getAudiences() != null) {
            String[] audience = oAuthAppDO.getAudiences();
            if (!disableAudClaim && audience != null && audience.length > 0) {
                String parsedClaims = "[\"" + StringUtils.join(audience, "\",\"") + "\"]";
                claims.put("aud", parsedClaims);
            }
        }

        claims.put("iss", API_GATEWAY_ID);
        claims.put("exp", String.valueOf(expireIn));
        claims.put("iat", String.valueOf(currentTime));
        claims.put("sub", usernameWithoutTenantDomain);
        claims.put(dialect + "/subscriber", subscriber);
        claims.put(dialect + "/applicationid", applicationId);
        claims.put(dialect + "/applicationname", applicationName);
        claims.put(dialect + "/applicationtier", applicationTier);
        claims.put(dialect + "/apicontext", validationContext.getContext());
        claims.put(dialect + "/version", validationContext.getVersion());
        claims.put(dialect + "/tier", tier);
        claims.put(dialect + "/keytype", keyType);
        claims.put(dialect + "/usertype", userType);
        claims.put(dialect + "/enduser", APIUtil.getUserNameWithTenantSuffix(endUserName));
        claims.put(dialect + "/enduserTenantId", enduserTenantId);
        claims.put(dialect + "/applicationUUId", uuid);
        try {
            if (appAttributes != null && !appAttributes.isEmpty()) {
                String stringAppAttributes = new ObjectMapper().writeValueAsString(appAttributes);
                claims.put(dialect + "/applicationAttributes", stringAppAttributes);
            }

        } catch (JsonProcessingException e) {
            log.error("Error in converting Map to String");
        }

        return claims;
    }

    @Override
    public Map<String, String> populateCustomClaims(TokenValidationContext validationContext)
            throws APIManagementException {
        ClaimsRetriever claimsRetriever = getClaimsRetriever();
        if (claimsRetriever != null) {

            String accessToken = validationContext.getAccessToken();
            AuthorizationGrantCacheKey cacheKey = new AuthorizationGrantCacheKey(accessToken);

            String username = validationContext.getValidationInfoDTO().getEndUserName();
            int tenantId = APIUtil.getTenantId(username);

            Map<String, String> customClaims = getClaimsFromCache(cacheKey, username);
            if (isNotEmpty(customClaims)) {
                if (log.isDebugEnabled()) {
                    log.debug("The custom claims are retrieved from AuthorizationGrantCache for user : "
                            + validationContext.getValidationInfoDTO().getEndUserName());
                }
                return customClaims;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Custom claims are not available in the AuthorizationGrantCache. Hence will be "
                            + "retrieved from the user store for user : " + validationContext.getValidationInfoDTO()
                            .getEndUserName());
                }
            }

            // If claims are not found in AuthorizationGrantCache, they will be retrieved from the userstore.
            try {
                if (tenantId != -1) {
                    UserStoreManager manager = ServiceReferenceHolder.getInstance().
                            getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();

                    String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(username);

                    if (manager.isExistingUser(tenantAwareUserName)) {
                        customClaims.putAll(claimsRetriever.getClaims(username));
                        return customClaims;
                    } else {
                        if (!customClaims.isEmpty()) {
                            return customClaims;
                        } else {
                            log.warn("User " + tenantAwareUserName + " cannot be found by user store manager");
                        }
                    }
                } else {
                    log.error("Tenant cannot be found for username: " + username);
                }
            } catch (APIManagementException e) {
                log.error("Error while retrieving claims ", e);
            } catch (UserStoreException e) {
                log.error("Error while retrieving user store ", e);
            }
        }
        return null;
    }

    private Map<String, String> getClaimsFromCache(AuthorizationGrantCacheKey cacheKey, String username)
            throws APIManagementException {
        AuthorizationGrantCacheEntry cacheEntry = AuthorizationGrantCache.getInstance()
                .getValueFromCacheByToken(cacheKey);
        if (cacheEntry == null) {
            return new HashMap<String, String>();
        }

        Map<ClaimMapping, String> userAttributes = cacheEntry.getUserAttributes();
        Map<String, String> oidcUserClaims = new HashMap<>();
        Map<String, String> oidcUserClaimsCopy = new HashMap<>();

        for (Map.Entry<ClaimMapping, String> entry : userAttributes.entrySet()) {
            oidcUserClaims.put(entry.getKey().getRemoteClaim().getClaimUri(), entry.getValue());
            oidcUserClaimsCopy.put(entry.getKey().getRemoteClaim().getClaimUri(), entry.getValue());
        }

        String convertClaimsFromOIDCtoConsumerDialect = ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService().getAPIManagerConfiguration()
                .getFirstProperty(APIConstants.CONVERT_CLAIMS_TO_CONSUMER_DIALECT);

        if (convertClaimsFromOIDCtoConsumerDialect != null && !Boolean.parseBoolean(convertClaimsFromOIDCtoConsumerDialect)) {
            return oidcUserClaims;
        }

        int tenantId = APIUtil.getTenantId(username);
        String tenantDomain = APIUtil.getTenantDomainFromTenantId(tenantId);
        String dialect;
        ClaimsRetriever claimsRetriever = getClaimsRetriever();
        if (claimsRetriever != null) {
            dialect = claimsRetriever.getDialectURI(username);
        } else {
            dialect = getDialectURI();
        }

        Map<String, String> configuredDialectToCarbonClaimMapping; // (key) configuredDialectClaimURI -> (value) carbonClaimURI
        Map<String, String> carbonToOIDCclaimMapping; // (key) carbonClaimURI ->  value (oidcClaimURI)

        Set<String> claimUris = new HashSet<>(oidcUserClaims.keySet());
        try {
            carbonToOIDCclaimMapping = new ClaimMetadataHandler().getMappingsMapFromOtherDialectToCarbon("http://wso2.org/oidc/claim",
                    claimUris, tenantDomain, true);
            configuredDialectToCarbonClaimMapping =
                    ClaimManagerHandler.getInstance().getMappingsMapFromCarbonDialectToOther(dialect,
                            carbonToOIDCclaimMapping.keySet(), tenantDomain);
        } catch (ClaimMetadataException e) {
            String error = "Error while mapping claims from Carbon dialect to http://wso2.org/oidc/claim dialect";
            throw new APIManagementException(error, e);
        } catch (ClaimManagementException e) {
            String error = "Error while mapping claims from configured dialect to Carbon dialect";
            throw new APIManagementException(error, e);
        }

        for (Map.Entry<String, String> oidcClaimValEntry : oidcUserClaims.entrySet()) {
            for (Map.Entry<String, String> carbonToOIDCEntry : carbonToOIDCclaimMapping.entrySet()) {
                if (oidcClaimValEntry.getKey().equals(carbonToOIDCEntry.getValue())) {
                    for (Map.Entry<String, String> configuredToCarbonEntry : configuredDialectToCarbonClaimMapping.entrySet()) {
                        if (configuredToCarbonEntry.getValue().equals(carbonToOIDCEntry.getKey())) {
                            oidcUserClaimsCopy.remove(oidcClaimValEntry.getKey());
                            oidcUserClaimsCopy.put(configuredToCarbonEntry.getKey(), oidcClaimValEntry.getValue());
                        }
                    }
                }
            }
        }
        return oidcUserClaimsCopy;
    }
}
