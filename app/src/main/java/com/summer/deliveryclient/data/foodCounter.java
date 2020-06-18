package com.summer.deliveryclient.data;

import java.io.Serializable;

public class foodCounter implements Serializable {
    private String foodName;
    private int foodCount;
    public foodCounter(String n,int c){
        this.foodName=n;
        this.foodCount=c;
    }
    public String getName(){return this.foodName;}
    public int getCount(){return this.foodCount;}


}
