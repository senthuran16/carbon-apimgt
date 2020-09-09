/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.apimgt.keymgt.token;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.keymgt.service.TokenValidationContext;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCache;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.FileInputStream;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceReferenceHolder.class, MultitenantUtils.class, APIUtil.class, KeyStoreManager.class,
        MessageDigest.class, AuthorizationGrantCache.class, OAuth2Util.class, IdentityUtil.class, OAuthServerConfiguration.class })
@SuppressStaticInitializationFor("org.wso2.carbon.identity.oauth2.util.OAuth2Util")
public class JWTGeneratorTestCase {

    private void mockAPIMConfiguration(Map<String, String> configMap) {

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }
    }

    @Test
    public void testAbstractJWTGeneratorConstructor() throws Exception {

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "SHA256withRSA");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");
        mockAPIMConfiguration(configMap);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertEquals(dialectUri, jwtGenerator.getDialectURI());
    }

    @Test
    public void testAbstractJWTGeneratorConstructorForSignatureAlgoWhenNull() throws Exception {

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, null);
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");
        mockAPIMConfiguration(configMap);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertEquals(dialectUri, jwtGenerator.getDialectURI());
    }

    @Test
    public void testAbstractJWTGeneratorConstructorForSignatureAlgoNotNull() throws Exception {

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");
        mockAPIMConfiguration(configMap);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertEquals(dialectUri, jwtGenerator.getDialectURI());
    }

    @Test
    public void testAddToCert() throws Exception {

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");
        mockAPIMConfiguration(configMap);

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);

        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.when(MultitenantUtils.getTenantDomain(endUserName)).thenReturn("godaddyclass2ca");

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1234);

        KeyStoreManager keyStoreManager = Mockito.mock(KeyStoreManager.class);
        PowerMockito.mockStatic(KeyStoreManager.class);
        PowerMockito.when(KeyStoreManager.getInstance(Mockito.anyInt())).thenReturn(keyStoreManager);

        String jksFilePath = System.getProperty("WSO2CarbonJksFilePath");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(jksFilePath), "wso2carbon".toCharArray());
        Mockito.when(keyStoreManager.getKeyStore(Mockito.anyString())).thenReturn(keyStore);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        String jsonString = jwtGenerator.buildHeader(tokenValidationContext);
        Assert.assertTrue(jsonString.contains("JWT"));
    }

    @Test
    public void testAddToCertForTenants() throws Exception {

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");
        mockAPIMConfiguration(configMap);

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);

        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.when(MultitenantUtils.getTenantDomain(endUserName)).thenReturn("godaddyclass2ca");

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1234);

        KeyStoreManager keyStoreManager = Mockito.mock(KeyStoreManager.class);
        PowerMockito.mockStatic(KeyStoreManager.class);
        PowerMockito.when(KeyStoreManager.getInstance(Mockito.anyInt())).thenReturn(keyStoreManager);

        String jksFilePath = System.getProperty("WSO2CarbonJksFilePath");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(jksFilePath), "wso2carbon".toCharArray());
        Mockito.when(keyStoreManager.getKeyStore(Mockito.anyString())).thenReturn(keyStore);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        String jsonString = jwtGenerator.buildHeader(tokenValidationContext);
        String jsForExistTenant = jwtGenerator.buildHeader(tokenValidationContext);

        Assert.assertTrue(jsonString.contains("JWT"));
        Assert.assertTrue(jsForExistTenant.contains("JWT"));
    }

    @Test
    public void testGetMultiAttributeSeparator() throws Exception {

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        RealmService realmService = Mockito.mock(org.wso2.carbon.user.core.service.RealmService.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        PowerMockito.mockStatic(AuthorizationGrantCache.class);
        AuthorizationGrantCache authorizationGrantCache = Mockito.mock(AuthorizationGrantCache.class);
        PowerMockito.when(AuthorizationGrantCache.getInstance()).thenReturn(authorizationGrantCache);

        OAuthServerConfiguration oauthServerConfigurationMock = Mockito
                .mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oauthServerConfigurationMock);
        PowerMockito.mockStatic(OAuth2Util.class);
        OAuth2Util oAuth2Util = Mockito.mock(OAuth2Util.class);
        OAuthAppDO oAuthAppDO = Mockito.mock(OAuthAppDO.class);
        String[] audiences = {"aud1", "aud2"};
        PowerMockito.when(oAuth2Util.getAppInformationByClientId(Mockito.anyString())).thenReturn(oAuthAppDO);
        PowerMockito.when(oAuthAppDO.getAudiences()).thenReturn(audiences);

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        apiKeyValidationInfoDTO.setApplicationId("1");
        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1234);

        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);
        RealmConfiguration realmConfiguration = Mockito.mock(RealmConfiguration.class);

        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfiguration);
        Mockito.when(realmConfiguration.getUserStoreProperty(Mockito.anyString())).thenReturn(",");

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertNotNull(jwtGenerator.buildBody(tokenValidationContext));
    }

    @Test
    public void testGetMultiAttributeSeparatorForInvalidTenant() throws Exception {

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        RealmService realmService = Mockito.mock(org.wso2.carbon.user.core.service.RealmService.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        PowerMockito.mockStatic(AuthorizationGrantCache.class);
        AuthorizationGrantCache authorizationGrantCache = Mockito.mock(AuthorizationGrantCache.class);
        PowerMockito.when(AuthorizationGrantCache.getInstance()).thenReturn(authorizationGrantCache);

        OAuthServerConfiguration oauthServerConfigurationMock = Mockito
                .mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oauthServerConfigurationMock);
        PowerMockito.mockStatic(OAuth2Util.class);
        OAuth2Util oAuth2Util = Mockito.mock(OAuth2Util.class);
        OAuthAppDO oAuthAppDO = Mockito.mock(OAuthAppDO.class);
        String[] audiences = {"aud1", "aud2"};
        PowerMockito.when(oAuth2Util.getAppInformationByClientId(Mockito.anyString())).thenReturn(oAuthAppDO);
        PowerMockito.when(oAuthAppDO.getAudiences()).thenReturn(audiences);

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        apiKeyValidationInfoDTO.setApplicationId("1");
        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertNotNull(jwtGenerator.buildBody(tokenValidationContext));
    }

    @Test
    public void testGetMultiAttributeSeparatorForClaimSeparator() throws Exception {

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        RealmService realmService = Mockito.mock(org.wso2.carbon.user.core.service.RealmService.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        PowerMockito.mockStatic(AuthorizationGrantCache.class);
        AuthorizationGrantCache authorizationGrantCache = Mockito.mock(AuthorizationGrantCache.class);
        PowerMockito.when(AuthorizationGrantCache.getInstance()).thenReturn(authorizationGrantCache);

        OAuthServerConfiguration oauthServerConfigurationMock = Mockito
                .mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oauthServerConfigurationMock);
        PowerMockito.mockStatic(OAuth2Util.class);
        OAuth2Util oAuth2Util = Mockito.mock(OAuth2Util.class);
        OAuthAppDO oAuthAppDO = Mockito.mock(OAuthAppDO.class);
        String[] audiences = {"aud1", "aud2"};
        PowerMockito.when(oAuth2Util.getAppInformationByClientId(Mockito.anyString())).thenReturn(oAuthAppDO);
        PowerMockito.when(oAuthAppDO.getAudiences()).thenReturn(audiences);


        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        apiKeyValidationInfoDTO.setApplicationId("1");

        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1234);

        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);
        RealmConfiguration realmConfiguration = Mockito.mock(RealmConfiguration.class);

        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfiguration);
        Mockito.when(realmConfiguration.getUserStoreProperty(Mockito.anyString())).thenReturn(null);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertNotNull(jwtGenerator.buildBody(tokenValidationContext));
    }

    @Test
    public void testGetMultiAttributeSeparatorForClaimSeparatorWhenEmpty() throws Exception {

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        RealmService realmService = Mockito.mock(org.wso2.carbon.user.core.service.RealmService.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        PowerMockito.mockStatic(AuthorizationGrantCache.class);
        AuthorizationGrantCache authorizationGrantCache = Mockito.mock(AuthorizationGrantCache.class);
        PowerMockito.when(AuthorizationGrantCache.getInstance()).thenReturn(authorizationGrantCache);

        OAuthServerConfiguration oauthServerConfigurationMock = Mockito
                .mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oauthServerConfigurationMock);
        PowerMockito.mockStatic(OAuth2Util.class);
        OAuth2Util oAuth2Util = Mockito.mock(OAuth2Util.class);
        OAuthAppDO oAuthAppDO = Mockito.mock(OAuthAppDO.class);
        String[] audiences = {"aud1", "aud2"};
        PowerMockito.when(oAuth2Util.getAppInformationByClientId(Mockito.anyString())).thenReturn(oAuthAppDO);
        PowerMockito.when(oAuthAppDO.getAudiences()).thenReturn(audiences);

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        apiKeyValidationInfoDTO.setApplicationId("1");

        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1234);

        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);
        RealmConfiguration realmConfiguration = Mockito.mock(RealmConfiguration.class);

        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfiguration);
        Mockito.when(realmConfiguration.getUserStoreProperty(Mockito.anyString())).thenReturn("");

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertNotNull(jwtGenerator.buildBody(tokenValidationContext));
    }

    @Test
    public void testGetMultiAttributeSeparatorForUserStoreException() throws Exception {

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        RealmService realmService = Mockito.mock(org.wso2.carbon.user.core.service.RealmService.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        PowerMockito.mockStatic(AuthorizationGrantCache.class);
        AuthorizationGrantCache authorizationGrantCache = Mockito.mock(AuthorizationGrantCache.class);
        PowerMockito.when(AuthorizationGrantCache.getInstance()).thenReturn(authorizationGrantCache);

        OAuthServerConfiguration oauthServerConfigurationMock = Mockito
                .mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oauthServerConfigurationMock);
        PowerMockito.mockStatic(OAuth2Util.class);
        OAuth2Util oAuth2Util = Mockito.mock(OAuth2Util.class);
        OAuthAppDO oAuthAppDO = Mockito.mock(OAuthAppDO.class);
        String[] audiences = {"aud1", "aud2"};
        PowerMockito.when(oAuth2Util.getAppInformationByClientId(Mockito.anyString())).thenReturn(oAuthAppDO);
        PowerMockito.when(oAuthAppDO.getAudiences()).thenReturn(audiences);

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        apiKeyValidationInfoDTO.setApplicationId("1");

        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1234);
        Mockito.doThrow(UserStoreException.class).when(realmService).getTenantUserRealm(Mockito.anyInt());

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertNotNull(jwtGenerator.buildBody(tokenValidationContext));
    }

    @Test
    public void testBuildBody() throws Exception {

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        RealmService realmService = Mockito.mock(org.wso2.carbon.user.core.service.RealmService.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        PowerMockito.mockStatic(AuthorizationGrantCache.class);
        AuthorizationGrantCache authorizationGrantCache = Mockito.mock(AuthorizationGrantCache.class);
        PowerMockito.when(AuthorizationGrantCache.getInstance()).thenReturn(authorizationGrantCache);

        OAuthServerConfiguration oauthServerConfigurationMock = Mockito
                .mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oauthServerConfigurationMock);
        PowerMockito.mockStatic(OAuth2Util.class);
        OAuth2Util oAuth2Util = Mockito.mock(OAuth2Util.class);
        OAuthAppDO oAuthAppDO = Mockito.mock(OAuthAppDO.class);
        String[] audiences = {"aud1", "aud2"};
        PowerMockito.when(oAuth2Util.getAppInformationByClientId(Mockito.anyString())).thenReturn(oAuthAppDO);
        PowerMockito.when(oAuthAppDO.getAudiences()).thenReturn(audiences);

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "signature_algo");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        apiKeyValidationInfoDTO.setApplicationTier("Unlimited,test");
        apiKeyValidationInfoDTO.setApplicationId("1");
        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(-1234);

        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);
        RealmConfiguration realmConfiguration = Mockito.mock(RealmConfiguration.class);

        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfiguration);
        Mockito.when(realmConfiguration.getUserStoreProperty(Mockito.anyString())).thenReturn(",");

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertNotNull(jwtGenerator.buildBody(tokenValidationContext));
    }

    @Test
    public void testGenerateTokenForTenants() throws Exception {

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        String endUserName = "admin";
        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        apiKeyValidationInfoDTO.setApplicationId("1");

        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.when(MultitenantUtils.getTenantDomain(endUserName)).thenReturn("godaddyclass2ca");

        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getTenantId(endUserName)).thenReturn(12);

        KeyStoreManager keyStoreManager = Mockito.mock(KeyStoreManager.class);
        PowerMockito.mockStatic(KeyStoreManager.class);
        PowerMockito.when(KeyStoreManager.getInstance(Mockito.anyInt())).thenReturn(keyStoreManager);

        String jksFilePath = System.getProperty("WSO2CarbonJksFilePath");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(jksFilePath), "wso2carbon".toCharArray());
        Mockito.when(keyStoreManager.getKeyStore(Mockito.anyString())).thenReturn(keyStore);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        RealmService realmService = Mockito.mock(org.wso2.carbon.user.core.service.RealmService.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);

        PowerMockito.mockStatic(AuthorizationGrantCache.class);
        AuthorizationGrantCache authorizationGrantCache = Mockito.mock(AuthorizationGrantCache.class);
        PowerMockito.when(AuthorizationGrantCache.getInstance()).thenReturn(authorizationGrantCache);

        OAuthServerConfiguration oauthServerConfigurationMock = Mockito
                .mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oauthServerConfigurationMock);
        PowerMockito.mockStatic(OAuth2Util.class);
        OAuth2Util oAuth2Util = Mockito.mock(OAuth2Util.class);
        OAuthAppDO oAuthAppDO = Mockito.mock(OAuthAppDO.class);
        String[] audiences = {"aud1", "aud2"};
        PowerMockito.when(oAuth2Util.getAppInformationByClientId(Mockito.anyString())).thenReturn(oAuthAppDO);
        PowerMockito.when(oAuthAppDO.getAudiences()).thenReturn(audiences);

        String dialectUri = "dialect_uri";
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(APIConstants.CONSUMER_DIALECT_URI, dialectUri);
        configMap.put(APIConstants.JWT_SIGNATURE_ALGORITHM, "SHA256withRSA");
        configMap.put(APIConstants.CLAIMS_RETRIEVER_CLASS, "claims_impl");

        for (String key : configMap.keySet()) {
            Mockito.when(apiManagerConfiguration.getFirstProperty(key))
                    .thenReturn(configMap.get(key));
        }

        apiKeyValidationInfoDTO.setEndUserName(endUserName);
        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);
        RealmConfiguration realmConfiguration = Mockito.mock(RealmConfiguration.class);

        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfiguration);
        Mockito.when(realmConfiguration.getUserStoreProperty(Mockito.anyString())).thenReturn(",");

        Signature signature = Mockito.mock(Signature.class);
        PowerMockito.mockStatic(Signature.class);
        PowerMockito.when(Signature.getInstance(Mockito.anyString())).thenReturn(signature);
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(1024);
        PrivateKey privateKey = keygen.generateKeyPair().getPrivate();
        Mockito.when(keyStoreManager.getPrivateKey(Mockito.anyString(), Mockito.anyString())).thenReturn(privateKey);

        JWTGenerator jwtGenerator = new JWTGeneratorWrapper();
        Assert.assertNotNull(jwtGenerator.generateToken(tokenValidationContext));
    }


}


