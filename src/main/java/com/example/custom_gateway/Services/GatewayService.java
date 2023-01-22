package com.example.custom_gateway.Services;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.custom_gateway.Configuration.ConfigInfo;
import com.example.custom_gateway.Models.ApiDetails;
import com.example.custom_gateway.WebModels.ResolverInfo;
import com.example.custom_gateway.WebModels.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class GatewayService {

    private ConfigInfo configInfo;

    private Hashtable<String, ApiDetails> lookupTable;

    @Autowired
    public GatewayService(ConfigInfo configInfo) {
        this.configInfo = configInfo;
        this.initializeLookUp();

    }

    private void initializeLookUp() {

        lookupTable = new Hashtable<>();
        var apis = configInfo.readAppDataInfo()
                .getApisInfo();
        for (var item : apis) {
            this.lookupTable.put(item.getKey(), item.getApiDetails());
        }
    }

    
    public boolean isFrontEndAllowed(HttpServletRequest request) {
        String requestHost = request.getRemoteHost();
        return configInfo.readAppDataInfo()
                .getFrontend()
                .getHost()
                .equals(requestHost);
    }

    public ResponseEntity<String> resolveResource(String body, HttpMethod method, HttpServletRequest request) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException {

        String requestedURI = request.getRequestURI();

        ResolverInfo resolverInfo = this.buildResolverInfo(requestedURI);

        if (!resolverInfo.isIsAvailable()) {
            throw new ResourceNotFoundException();
        }

        ResponseEntity<String> forwardedResponse = processProxyRequest(body, method, request, resolverInfo, UUID.randomUUID().toString());

        System.out.println(forwardedResponse.getBody());
        return forwardedResponse;

    }

    
    

    private ResolverInfo buildResolverInfo(String requestURI) {
        if (!this.validateResourceAvailability(requestURI)) {
            var info = new ResolverInfo();
            info.setIsAvailable(false);
            return info;
        }

        return this.getInfoForResource(requestURI);
    }

    private ResolverInfo getInfoForResource(String requestURI) {
        var info = new ResolverInfo();
        info.setIsAvailable(true);
        var apiInfo = this.lookupTable.get(requestURI);
        info.setApiDetails(apiInfo);

        return info;

    }

    private boolean validateResourceAvailability(String requestURI) {
        return this.lookupTable.containsKey(requestURI);
    }

    @Retryable(exclude = {
            HttpStatusCodeException.class }, include = Exception.class, backoff = @Backoff(delay = 5000, multiplier = 4.0), maxAttempts = 4)
    private ResponseEntity<String> processProxyRequest(String body,
            HttpMethod method, HttpServletRequest request, ResolverInfo resolverInfo, String traceId)
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        ThreadContext.put("traceId", traceId);

        URI uri = this.buildURI(request, resolverInfo);

        HttpHeaders headers = loadHeaders(request);
        SetAdditionalHeaders(headers, traceId);
        RemoveExtraHeaders(headers);

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        CloseableHttpClient httpClient = buildClient(resolverInfo);

        HttpComponentsClientHttpRequestFactory clientrequestFactory = new HttpComponentsClientHttpRequestFactory();

        clientrequestFactory.setHttpClient(httpClient);

        RestTemplate restTemplate = new RestTemplate(clientrequestFactory);

        try {

            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, method, httpEntity, String.class);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE));

            return serverResponse;

        } catch (HttpStatusCodeException e) {

            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        }

    }

    private CloseableHttpClient buildClient(ResolverInfo resolverInfo) {
        // version 5.0
        try {
            final SSLContext sslcontext = this.configureSSLContext(resolverInfo);

            final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslcontext)
                    .build();
            final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();
            return HttpClients.custom()
                    .setConnectionManager(cm)
                    .evictExpiredConnections()
                    .build();

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {

        }
        return null;
    }

    private SSLContext configureSSLContext(ResolverInfo resolverInfo)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        String protocolToUse = resolverInfo.getApiDetails().getProtocol();

        if (!protocolToUse.equals("HTTPS")) {
            return SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .build();
        }
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                .build();
        return sslContext;
    }

    private void RemoveExtraHeaders(HttpHeaders headers) {
        headers.remove(HttpHeaders.ACCEPT_ENCODING);
    }

    private void SetAdditionalHeaders(HttpHeaders headers, String traceId) {
        headers.set("TRACE", traceId);
    }

    private HttpHeaders loadHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
        }

        return headers;
    }

    private URI buildURI(HttpServletRequest request, ResolverInfo resolverInfo) throws URISyntaxException {
        String requestUrl = request.getRequestURI();

        String protocolToUse = resolverInfo.getApiDetails()
                .getProtocol();

        String hostToUSe = resolverInfo.getApiDetails()
                .getHost();

        int portToUse = resolverInfo.getApiDetails()
                .getPort();

        URI uri = new URI(protocolToUse, null, hostToUSe, portToUse, null, null, null);

        // replacing context path form urI to match actual gateway URI
        uri = UriComponentsBuilder.fromUri(uri)
                .path(requestUrl)
                .query(request.getQueryString())
                .build(true).toUri();
        return uri;
    }

    @Recover
    public ResponseEntity<String> recoverFromRestClientErrors(Exception e, String body,
            HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) {
        System.out.println(
                "retry method for the following url " + request.getRequestURI() + " has failed" + e.getMessage());
        System.out.println(e.getStackTrace());
        throw new RuntimeException("There was an error trying to process you request. Please try again later");

    }

    public void pringAPIInformation() {

        var apis = configInfo.readAppDataInfo()
                .getApisInfo();
        for (var item : apis) {
            System.out.println(item.getKey() + "_" + item.getApiDetails().getPort());

        }
    }

    public void printURL(HttpServletRequest request) {
    }

    public void printQueryString(HttpServletRequest request) {
    }

    public void printHeaders(HttpServletRequest request) {
    }

    public void printDecodedBody(String body) {
    }

    public void printCookies(HttpServletRequest request) {
    }

}
