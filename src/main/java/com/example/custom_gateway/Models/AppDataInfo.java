package com.example.custom_gateway.Models;

import java.util.ArrayList;

public class AppDataInfo {

    private Frontend frontend;
    private ArrayList<ApiInfo> apisInfo;

    public ArrayList<ApiInfo> getApisInfo() {
        return apisInfo;
    }

    public void setApisInfo(ArrayList<ApiInfo> apisInfo) {
        this.apisInfo = apisInfo;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public void setFrontend(Frontend frontend) {
        this.frontend = frontend;
    }


    

}
