package com.example.custom_gateway.Models;

public class ApiInfo {
    
    private String key;
    private ApiDetails apiDetails;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public ApiDetails getApiDetails() {
        return apiDetails;
    }
    public void setApiDetails(ApiDetails apiDetails) {
        this.apiDetails = apiDetails;
    }
}
