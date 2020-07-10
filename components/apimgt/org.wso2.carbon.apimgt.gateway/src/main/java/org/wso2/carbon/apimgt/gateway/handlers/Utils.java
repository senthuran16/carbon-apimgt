/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.gateway.handlers;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.rest.VersionStrategyFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.apimgt.gateway.handlers.security.APISecurityConstants;
import org.wso2.carbon.apimgt.gateway.handlers.security.APISecurityException;
import org.wso2.carbon.apimgt.gateway.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.*;

public class Utils {
    
    private static final Log log = LogFactory.getLog(Utils.class);
    
    public static void sendFault(MessageContext messageContext, int status) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();

        axis2MC.setProperty(NhttpConstants.HTTP_SC, status);
        messageContext.setResponse(true);
        messageContext.setProperty("RESPONSE", "true");
        messageContext.setTo(null);        
        axis2MC.removeProperty("NO_ENTITY_BODY");

        // Always remove the ContentType - Let the formatter do its thing
        axis2MC.removeProperty(Constants.Configuration.CONTENT_TYPE);
        Map headers = (Map) axis2MC.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null) {
            headers.remove(HttpHeaders.AUTHORIZATION);
            headers.remove(HttpHeaders.AUTHORIZATION);

            headers.remove(HttpHeaders.HOST);
        }
        Axis2Sender.sendBack(messageContext);
    }
    
    public static void setFaultPayload(MessageContext messageContext, OMElement payload) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        JsonUtil.removeJsonPayload(axis2MC);
        messageContext.getEnvelope().getBody().addChild(payload);
        Map headers = (Map) axis2MC.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String acceptType = (String) headers.get(HttpHeaders.ACCEPT);
        Set<String> supportedMimes = new HashSet<String>(Arrays.asList("application/x-www-form-urlencoded",
                                                                       "multipart/form-data",
                                                                       "text/html",
                                                                       "application/xml",
                                                                       "text/xml",
                                                                       "application/soap+xml",
                                                                       "text/plain",
                                                                       "application/json",
                                                                       "application/json/badgerfish",
                                                                       "text/javascript"));

        // If an Accept header has been provided and is supported by the Gateway
        if(!StringUtils.isEmpty(acceptType) && supportedMimes.contains(acceptType)){
            axis2MC.setProperty(Constants.Configuration.MESSAGE_TYPE, acceptType);
        } else {
            // If there isn't Accept Header in the request, will use error_message_type property
            // from _auth_failure_handler_.xml file
            if (messageContext.getProperty("error_message_type") != null) {
                axis2MC.setProperty(Constants.Configuration.MESSAGE_TYPE,
                                    messageContext.getProperty("error_message_type"));
            }
        }
    }
    
    public static void setSOAPFault(MessageContext messageContext, String code, 
                                    String reason, String detail) {
        SOAPFactory factory = (messageContext.isSOAP11() ?
                OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory());

        OMDocument soapFaultDocument = factory.createOMDocument();
        SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
        soapFaultDocument.addChild(faultEnvelope);

        SOAPFault fault = faultEnvelope.getBody().getFault();
        if (fault == null) {
            fault = factory.createSOAPFault();
        }

        SOAPFaultCode faultCode = factory.createSOAPFaultCode();
        if (messageContext.isSOAP11()) {
            faultCode.setText(new QName(fault.getNamespace().getNamespaceURI(), code));
        } else {
            SOAPFaultValue value = factory.createSOAPFaultValue(faultCode);
            value.setText(new QName(fault.getNamespace().getNamespaceURI(), code));            
        }
        fault.setCode(faultCode);

        SOAPFaultReason faultReason = factory.createSOAPFaultReason();
        if (messageContext.isSOAP11()) {
            faultReason.setText(reason);
        } else {
            SOAPFaultText text = factory.createSOAPFaultText();
            text.setText(reason);
            text.setLang("en");
            faultReason.addSOAPText(text);            
        }
        fault.setReason(faultReason);

        SOAPFaultDetail soapFaultDetail = factory.createSOAPFaultDetail();
        soapFaultDetail.setText(detail);
        fault.setDetail(soapFaultDetail);
        
        // set the all headers of original SOAP Envelope to the Fault Envelope
        if (messageContext.getEnvelope() != null) {
            SOAPHeader soapHeader = messageContext.getEnvelope().getHeader();
            if (soapHeader != null) {
                for (Iterator iterator = soapHeader.examineAllHeaderBlocks(); iterator.hasNext();) {
                    Object o = iterator.next();
                    if (o instanceof SOAPHeaderBlock) {
                        SOAPHeaderBlock header = (SOAPHeaderBlock) o;
                        faultEnvelope.getHeader().addChild(header);
                    } else if (o instanceof OMElement) {
                        faultEnvelope.getHeader().addChild((OMElement) o);
                    }
                }
            }
        }

        try {
            messageContext.setEnvelope(faultEnvelope);
        } catch (AxisFault af) {
            log.error("Error while setting SOAP fault as payload", af);
            return;
        }

        if (messageContext.getFaultTo() != null) {
            messageContext.setTo(messageContext.getFaultTo());
        } else if (messageContext.getReplyTo() != null) {
            messageContext.setTo(messageContext.getReplyTo());
        } else {
            messageContext.setTo(null);
        }

        // set original messageID as relatesTo
        if (messageContext.getMessageID() != null) {
            RelatesTo relatesTo = new RelatesTo(messageContext.getMessageID());
            messageContext.setRelatesTo(new RelatesTo[] { relatesTo });
        }
    }
//// moving methods to Util
    /**
     * validates if an accessToken has expired or not
     *
     * @param accessTokenDO
     * @return
     */
    public static boolean hasAccessTokenExpired(APIKeyValidationInfoDTO accessTokenDO) {
        long currentTime;
        long validityPeriod;
        if (accessTokenDO.getValidityPeriod() != Long.MAX_VALUE) {
            validityPeriod = accessTokenDO.getValidityPeriod() * 1000;
        } else {
            validityPeriod = accessTokenDO.getValidityPeriod();
        }
        long issuedTime = accessTokenDO.getIssuedTime();
        //long issuedTime = accessTokenDO.getIssuedTime().getTime();
        currentTime = System.currentTimeMillis();

        //If the validity period is not an never expiring value
        if (validityPeriod != Long.MAX_VALUE) {
            //check the validity of cached OAuth2AccessToken Response
            if ((currentTime) > (issuedTime + validityPeriod)) {
                accessTokenDO.setValidationStatus(
                        APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
                if (accessTokenDO.getEndUserToken() != null) {
                    log.info("Token " + accessTokenDO.getEndUserToken() + " expired.");
                }
                return true;
            }
        }


        return false;
    }

    public static String getRequestPath(MessageContext synCtx, String fullRequestPath, String apiContext, String
            apiVersion) {
        String requestPath;
        String versionStrategy = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION_STRATEGY);

        if(VersionStrategyFactory.TYPE_URL.equals(versionStrategy)){
            // most used strategy. server:port/context/version/resource
            requestPath = fullRequestPath.substring((apiContext + apiVersion).length() + 1, fullRequestPath.length());
         }else{
            // default version. assume there is no version is used
            requestPath = fullRequestPath.substring(apiContext.length(), fullRequestPath.length());
        }
        return requestPath;
    }

    /**
     * This method used to send the response back from the request.
     *
     * @param messageContext messageContext of the request
     * @param status         HTTP Status to return from the response
     */
    public static void send(MessageContext messageContext, int status) {
        org.apache.axis2.context.MessageContext axis2MC =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        axis2MC.setProperty(NhttpConstants.HTTP_SC, status);
        messageContext.setResponse(true);
        messageContext.setProperty(SynapseConstants.RESPONSE, "true");
        messageContext.setTo(null);
        axis2MC.removeProperty(Constants.Configuration.CONTENT_TYPE);
        Axis2Sender.sendBack(messageContext);
    }

    public static String getClientCertificateHeader() {

        APIManagerConfiguration apiManagerConfiguration =
                ServiceReferenceHolder.getInstance().getAPIManagerConfiguration();
        if (apiManagerConfiguration != null) {
            String clientCertificateHeader =
                    apiManagerConfiguration.getFirstProperty(APIConstants.MutualSSL.CLIENT_CERTIFICATE_HEADER);
            if (StringUtils.isNotEmpty(clientCertificateHeader)) {
                return clientCertificateHeader;
            }
        }
        return APIMgtGatewayConstants.BASE64_ENCODED_CLIENT_CERTIFICATE_HEADER;
    }

    public static X509Certificate getClientCertificate(org.apache.axis2.context.MessageContext axis2MessageContext)
            throws APIManagementException {

        Map headers =
                (Map) axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        Object sslCertObject = axis2MessageContext.getProperty(NhttpConstants.SSL_CLIENT_AUTH_CERT_X509);
        X509Certificate certificateFromMessageContext = null;
        if (sslCertObject != null) {
            X509Certificate[] certs = (X509Certificate[]) sslCertObject;
            certificateFromMessageContext = certs[0];
        }
        if (headers.containsKey(Utils.getClientCertificateHeader())) {

            try {
                if (!isClientCertificateValidationEnabled() || APIUtil
                        .isCertificateExistsInTrustStore(certificateFromMessageContext)){
                    String base64EncodedCertificate = (String) headers.get(Utils.getClientCertificateHeader());
                    if (base64EncodedCertificate != null) {
                        base64EncodedCertificate = URLDecoder.decode(base64EncodedCertificate).
                                replaceAll(APIMgtGatewayConstants.BEGIN_CERTIFICATE_STRING, "")
                                .replaceAll(APIMgtGatewayConstants.END_CERTIFICATE_STRING, "");

                        byte[] bytes = Base64.decodeBase64(base64EncodedCertificate);
                        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                            X509Certificate x509Certificate = X509Certificate.getInstance(inputStream);
                            if (APIUtil.isCertificateExistsInTrustStore(x509Certificate)) {
                                return x509Certificate;
                            }else{
                                log.debug("Certificate in Header didn't exist in truststore");
                                return null;
                            }
                        } catch (IOException | CertificateException | APIManagementException e) {
                            String msg = "Error while converting into X509Certificate";
                            log.error(msg, e);
                            throw new APIManagementException(msg, e);
                        }
                    }
                }
            } catch (APIManagementException e) {
                String msg = "Error while validating into Certificate Existence";
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }

        }
        return certificateFromMessageContext;
    }
    private static boolean isClientCertificateValidationEnabled() {

        APIManagerConfiguration apiManagerConfiguration =
                ServiceReferenceHolder.getInstance().getAPIManagerConfiguration();
        if (apiManagerConfiguration != null) {
            String firstProperty = apiManagerConfiguration
                    .getFirstProperty(APIConstants.MutualSSL.ENABLE_CLIENT_CERTIFICATE_VALIDATION);
            return Boolean.parseBoolean(firstProperty);
        }
        return false;
    }

}
