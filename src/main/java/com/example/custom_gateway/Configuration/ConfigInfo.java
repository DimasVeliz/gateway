package com.example.custom_gateway.Configuration;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.custom_gateway.Models.AppDataInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ConfigInfo {

    @Value("classpath:data.json")
    Resource resourceFile;

    public AppDataInfo readAppDataInfo()
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            var appInfo = mapper.readValue( resourceFile.getFile(),AppDataInfo.class);
            return appInfo;

        } catch (IOException e) {
           
            e.printStackTrace();
        }
        return null;
    }

}
