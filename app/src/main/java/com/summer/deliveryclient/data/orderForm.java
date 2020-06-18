package com.summer.deliveryclient.data;

import java.util.List;

public class orderForm {
    private int ID;
    private List<foodCounter> foods;
    private String targetAddress;
    private String restAddress;
    public orderForm(int ID,List<foodCounter> foods,String tAddress,String rAddress){
        this.ID=ID;
        this.foods=foods;
        this.targetAddress=tAddress;
        this.restAddress=rAddress;
    }

    public int getID() {
        return ID;
    }

    public List<foodCounter> getFoods() {
        return foods;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public String getRestAddress() {
        return restAddress;
    }
}
