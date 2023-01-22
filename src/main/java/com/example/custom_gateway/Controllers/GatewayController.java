package com.example.custom_gateway.Controllers;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.custom_gateway.Services.GatewayService;
import com.example.custom_gateway.Utils.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class GatewayController {

    GatewayService service;

    @Autowired
    public GatewayController(GatewayService service) {
        super();
        this.service = service;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> sendRequestToSPM(@RequestBody(required = false) String body,
            HttpMethod method, HttpServletRequest request, HttpServletResponse response)
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        
        service.printDogName();
        service.printURL(request);
        service.printQueryString(request);
        service.printHeaders(request);
        service.printDecodedBody(body);
        service.printCookies(request);

        if (!service.isFrontEndAllowed(request)) {
            throw new ResourceNotFoundException(); 
        }

        var forwardedResponse = service.resolveResource(body, method, request);

        

        return forwardedResponse;
    }
    
}
