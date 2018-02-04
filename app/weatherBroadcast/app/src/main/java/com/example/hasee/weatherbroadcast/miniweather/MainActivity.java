package com.example.hasee.weatherbroadcast.miniweather;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import com.example.hasee.weatherbroadcast.R;
import com.example.hasee.weatherbroadcast.bean.TodayWeather;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.example.hasee.weatherbroadcast.bls.MyLocation;
import android.text.ClipboardManager;
import com.example.hasee.weatherbroadcast.database.DBManager;
import com.example.hasee.weatherbroadcast.util.NetUtil;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final int UPDATE_TODAY_WEATHER = 1;
    private BDLocationListener BaiDuListener = new MyLocation(this);
    private LocationClient mLocationClient = null;
    private LocationClientOption option = new LocationClientOption();
    private ImageView mUpdateBtn;
    private ImageView lbs_btn;
    private ImageView mCitySelect;
    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv, city_name_Tv;
    private ImageView weatherImg, pmImg;

    private String code="";
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what){
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                default:
                    break;
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info_port);
        new DBManager(getApplicationContext()).writeData();
        init();
    }
    void init(){
        //判断网络状态
        if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d("myWeather", "网络OK");
            //Toast.makeText(MainActivity.this, "网络OK！", Toast.LENGTH_LONG).show();
        } else {
            Log.d("myWeather", "网络挂了");
            //Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
        }
        initView();
        JudgePermission();
    }

    void initView(){        //初始化对象
        mLocationClient = new LocationClient(getApplicationContext());//声明LocationClient类
        mLocationClient.registerLocationListener(BaiDuListener);    //注册监听函数
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        lbs_btn=(ImageView)findViewById(R.id.title_location);
        lbs_btn.setOnClickListener(this);
        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        pmImg=(ImageView)findViewById(R.id.pm2_5_img);
        city_name_Tv.setText("");
        cityTv.setText("");
        timeTv.setText("");
        humidityTv.setText("");
        pmDataTv.setText("");
        pmQualityTv.setText("");
        weekTv.setText("");
        temperatureTv.setText("");
        climateTv.setText("");
        windTv.setText("");
        weatherImg.setImageResource(R.drawable.na);
        updateWeatherData();
    }

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.title_city_manager){        //点击切换城市按钮
            Intent i = new Intent(this, SelectCity.class);
            i.putExtra("keycode",code);
            startActivity(i);

        }
        else if (view.getId() == R.id.title_update_btn) {        //点击更新按钮
            updateWeatherData();
        }
        else if(view.getId()==R.id.title_location){
            onRefresh();
        }
        else if(view.getId()==R.id.title_share){
            /*copyWeatherMessage();*/
        }
    }

    private void queryWeatherCode(String cityCode) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("myWeather", address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather = null;
                try {
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();    //建立连接
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);
                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {     //读取网页上的内容
                        response.append(str);
                        Log.d("myWeather", str);
                    }
                    String responseStr = response.toString();       //保存内容
                    Log.d("myWeather", responseStr);

                    todayWeather = parseXML(responseStr);           //解析内容
                    if (todayWeather != null) {
                        Log.d("myWeather", todayWeather.toString());
                        Message msg =new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj=todayWeather;
                        mHandler.sendMessage(msg);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
        }).start();
    }

    private TodayWeather parseXML(String xmldata){  //解析XML
        TodayWeather todayWeather = null;
        int fengxiangCount=0;
        int fengliCount =0;
        int dateCount=0;
        int highCount =0;
        int lowCount=0;
        int typeCount =0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {       //读取xml相应内容
                switch (eventType) {
                    // 判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    // 判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if(xmlPullParser.getName().equals("resp")){
                            todayWeather= new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                        }
                        break;


                    // 判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                // 进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    void updateTodayWeather(TodayWeather todayWeather){         //更新当前天气信息,在Handler中被调用
        city_name_Tv.setText(todayWeather.getCity()+"天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime()+ "发布");
        humidityTv.setText("湿度："+todayWeather.getShidu());
        pmDataTv.setText(todayWeather.getPm25());
        pmQualityTv.setText(todayWeather.getQuality());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getLow()+" ~ "+todayWeather.getHigh());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:"+todayWeather.getFengli());
        chooseWeatherImg(todayWeather);
        //Toast.makeText(MainActivity.this,"更新成功！",Toast.LENGTH_SHORT).show();

    }
    void chooseWeatherImg(TodayWeather todayWeather){
        String updatetime=todayWeather.getUpdatetime();
        int pm=0;
        if(null!=todayWeather.getPm25())
            pm=Integer.parseInt(todayWeather.getPm25());
        updatetime=updatetime.substring(0,updatetime.indexOf(":"));
        int nowTime=Integer.parseInt(updatetime);
        if(nowTime>=6&&nowTime<19){
            switch(todayWeather.getType()){
                case "多云转晴":
                    weatherImg.setImageResource(R.drawable.cloudy_with_rain);
                    break;
                case "晴":
                    weatherImg.setImageResource(R.drawable.sun);
                    break;
                case "多云":
                    weatherImg.setImageResource(R.drawable.cloudy);
                default:
            }
        }else{
            switch(todayWeather.getType()){
                case "多云转晴":
                    weatherImg.setImageResource(R.drawable.cloudy_with_rain_night);
                    break;
                case "晴":
                    weatherImg.setImageResource(R.drawable.sun_night);
                    break;
                case "多云":
                    weatherImg.setImageResource(R.drawable.cloudy_night);
                default:
            }
        }
        if(pm<=50){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
        }else if(pm<=100){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
        }else if(pm<=150){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
        }else if(pm<=200){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
        }else if(pm<=300){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
        }else{
            pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
        }
    }
    void updateWeatherData(){
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        if(null!=SelectCity.spinner2){
            int pos=SelectCity.spinner2.getSelectedItemPosition();
            code=SelectCity.codes.get(pos);
        }else if(!"".equals(MyLocation.cityCode)){
            code=MyLocation.cityCode;
        }
        else{
            code="101010100";
        }
        if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d("myWeather", "网络OK");
            queryWeatherCode(code);
        } else {
            Log.d("myWeather", "网络挂了");
            Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
        }
    }

    protected void onRefresh(){
        finish();
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void initLocation() {
        option.setCoorType("bd09ll");        //可选，默认gcj02，设置返回的定位结果坐标系
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setOpenGps(true);        //可选，默认false,设置是否使用gps
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        mLocationClient.setLocOption(option);
    }

    private void  requestLocation(){
        initLocation();
        Log.d("map requestLocation:","Run requestLocation ...");
        mLocationClient.start();
    }

    @Override
    public  void  onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length >0 ){
                    for (int result : grantResults){
                        if(result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"all  permission  !!!",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(this,"Unkonw error.",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
    }

    private void JudgePermission(){     //权限判断
        List<String> permissionList = new ArrayList<>();// Permission array list , request permissions in one array.
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=  PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);//request all permissions
        }else {
            requestLocation();// Request Location information
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == this.getResources().getConfiguration().ORIENTATION_PORTRAIT) {             //切换为竖屏
            setContentView(R.layout.weather_info_port);
            init();
        }else if (newConfig.orientation == this.getResources().getConfiguration().ORIENTATION_LANDSCAPE) {      //切换为横屏
            setContentView(R.layout.weather_info_land);
            init();
        }
    }

/*    private void copyWeatherMessage(){
        if(null!=cityTv||!"".equals(cityTv)){
            StringBuffer s=new StringBuffer();
            s.append(cityTv.getText()+"\n").append(humidityTv.getText()+"\n").append(pmDataTv.getText()+"\n").append(temperatureTv.getText()+"\n").append(climateTv.getText()+"\n");
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        }
    }*/
}