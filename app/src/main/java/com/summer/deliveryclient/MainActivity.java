package com.summer.deliveryclient;

import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import com.summer.deliveryclient.data.*;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private Boolean getDish=false;
    private LocationManager lms;
    private Criteria criteria;
    private Location mylocation;
    private Double lat;
    private Double lng;
    private orderForm orderForm;
    private static Socket clientSocket;
    private Thread connectThread;
    private Handler handler=new Handler();
    private String currentAddress;

    private Button btnDelivery;
    private Button btnGetDish;
    private Button btnDirect;
    private TextView tvAddress;
    private TextView tvDistance;
    private TextView tvOrder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDelivery=(Button)findViewById(R.id.btnDelivery);
        btnDirect=(Button)findViewById(R.id.btnDirect);
        btnGetDish=(Button)findViewById(R.id.btnGetDish);
        tvAddress=(TextView)findViewById(R.id.tvAddress);
        tvDistance=(TextView)findViewById(R.id.tvDistance);
        tvOrder=(TextView)findViewById(R.id.tvOrder);


        connectThread=new Thread(socket_server);
        connectThread.start();



    }
    public void direct(View view){
        Uri intentURI=Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+currentAddress);
        Intent mapIntent=new Intent(Intent.ACTION_VIEW,intentURI);
        startActivity(mapIntent);
    }

    public void getDish(View view){
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append("ID:"+orderForm.getID()+"\n");
        stringBuilder.append("Foods:\n");
        for(int i=0;i<orderForm.getFoods().size();i++){
            stringBuilder.append("Name:"+orderForm.getFoods().get(i).getName()+"\n");
            stringBuilder.append("Count:"+orderForm.getFoods().get(i).getCount()+"\n");
        }
        tvOrder.setText(stringBuilder.toString());
        Thread t=new Thread(new Runnable() {
            @Override
            public void run() {
                getGeocode(orderForm.getTargetAddress());
                currentAddress=orderForm.getTargetAddress();
                getDish=true;
            }
        });
        t.start();
        tvAddress.setText("地址："+orderForm.getTargetAddress());
    }

    public void finishDelivery(View view){
        Thread t=new Thread(new Runnable() {
            @Override
            public void run() {
                pushMsg("送餐完畢","restClient");
            }
        });
        t.start();
    }

    private LocationListener myListener=new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mylocation=location;
            float[] result=new float[1];
            Location.distanceBetween(mylocation.getLatitude(),mylocation.getLongitude(),lat,lng,result);
            String distance= NumberFormat.getInstance().format(result[0]);
            tvDistance.setText("距離："+distance);
            if(result[0]<=10){
                btnGetDish.setEnabled(true);
            }
            if(result[0]<=10&&getDish){
                btnDelivery.setEnabled(true);
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void initLocationManager(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},0);
        }
        tvAddress.setText("地址:"+orderForm.getRestAddress());
        lms = (LocationManager) getSystemService(LOCATION_SERVICE);
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String best = lms.getBestProvider(criteria, true);
        Location location = lms.getLastKnownLocation(best);
        mylocation=location;
        lms.requestLocationUpdates(best,1000,10,myListener);
    }
    private Runnable socket_server=new Runnable() {
        @Override
        public void run() {
            try{
                InetAddress serverIP=InetAddress.getByName("10.0.2.2");
                clientSocket=new Socket(serverIP,5050);
                BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"BIG5"));
                BufferedReader br=new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"BIG5"));
                bw.write("deliveryClient\n");
                bw.flush();
                while(clientSocket.isConnected()){
                    String temp=br.readLine();
                    if(temp!=null){
                        parseStringToMyData(temp);
                        currentAddress=orderForm.getRestAddress();
                        getGeocode(orderForm.getRestAddress());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnDirect.setEnabled(true);
                                initLocationManager();
                            }
                        });
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    };
    private void getGeocode(String address){
        String url = "https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyBsweY_YrYNePW_L_VFbRxyLnyVuKtm1FE&address="+address;
        System.out.println(url);
        JSONObject jsonObject = loadJson(url);
        try {
            JSONArray jsonObject1 = jsonObject.getJSONArray("results");
            JSONObject s = (JSONObject) jsonObject1.get(0);
            JSONObject t = (JSONObject) s.get("geometry");
            JSONObject result = (JSONObject) t.get("location");
            lat = result.getDouble("lat");
            lng = result.getDouble("lng");
            System.out.println(lat);
            System.out.println(lng);
        } catch (Exception e) {
        }
    }

    public JSONObject loadJson(String url){
        StringBuilder json=new StringBuilder();
        try{
            URL urlObject=new URL(url);
            URLConnection uc=urlObject.openConnection();
            BufferedReader in=new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String inputLine=null;
            while((inputLine=in.readLine())!=null){
                json.append(inputLine);
            }
            in.close();
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        try{
            JSONObject object= new JSONObject(String.valueOf(json));
            return object;
        }catch (JSONException e){
            e.printStackTrace();
        }
        return null;
    }
    public void parseStringToMyData(String orderString){
        Log.d("test","START!");

        int ID=0;
        int total=0;
        String restAddress="";
        String targetAddress="";
        ArrayList<String> foodName=new ArrayList<>();
        ArrayList<Integer> foodCount=new ArrayList<>();
        String[] temp=orderString.split(" ");
        for(int i=0;i<temp.length;i++){
            String n=temp[i];
            if(n.contains("ID:"))ID=Integer.parseInt(n.substring(n.indexOf("ID:")+3));
            if(n.contains("Name:"))foodName.add(n.substring(n.indexOf("Name:")+5));
            if(n.contains("Count:"))foodCount.add(Integer.parseInt(n.substring(n.indexOf("Count:")+6)));
            if(n.contains("Total:"))total=Integer.parseInt(n.substring(n.indexOf("Total:")+6));
            if(n.contains("restAddress"))restAddress=n.substring(n.indexOf("restAddress:")+12);
            if(n.contains("targetAddress:"))targetAddress=n.substring(n.indexOf("targetAddress:")+14);
        }
        Log.d("Values",String.valueOf(ID));
        Log.d("Values",String.valueOf(total));
        Log.d("Values",restAddress+"\n"+targetAddress);
        ArrayList<foodCounter> foods=new ArrayList<>();
        for(int i=0;i<foodName.size();i++){
            foods.add(new foodCounter(foodName.get(i),foodCount.get(i)));
        }
        orderForm=new orderForm(ID,foods,targetAddress,restAddress);
        System.out.println(orderForm.getRestAddress());

    }
    private void pushMsg(String msg,String client){
        try{
            OutputStream os=clientSocket.getOutputStream();
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(os,"BIG5"));
            while(clientSocket.isConnected()){
                bw.write(msg+" "+client+"\n");
                bw.flush();
                break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
