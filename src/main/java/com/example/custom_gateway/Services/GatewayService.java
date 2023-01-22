package com.example.custom_gateway.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.custom_gateway.Configuration.ConfigInfo;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class GatewayService {

    private ConfigInfo configInfo;

    @Autowired
    public GatewayService(ConfigInfo configInfo) {
        this.configInfo = configInfo;
        
    }
    public void printDogName()
    {
       
        var apis = configInfo.readAppDataInfo()
                              .getApisInfo();
        for (var item : apis) {
            System.out.println(item.getKey()+"_"+item.getApiDetails().getPort());
    
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

    public boolean isFrontEndAllowed(HttpServletRequest request) {
        return false;
    }

    public ResponseEntity<String> resolveResource(String body, HttpMethod method, HttpServletRequest request) {
        return null;
    }
    
}
