package com.webmyne.riteway_driver.my_orders;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.webmyne.riteway_driver.CustomViews.CallWebService;
import com.webmyne.riteway_driver.CustomViews.CircleDialog;
import com.webmyne.riteway_driver.CustomViews.ComplexPreferences;
import com.webmyne.riteway_driver.R;
import com.webmyne.riteway_driver.home.DriverProfile;
import com.webmyne.riteway_driver.model.API;
import com.webmyne.riteway_driver.model.AppConstants;
import com.webmyne.riteway_driver.model.GPSTracker;
import com.webmyne.riteway_driver.model.PagerSlidingTabStrip;
import com.webmyne.riteway_driver.model.ResponseMessage;
import com.webmyne.riteway_driver.model.SharedPreferenceNotification;
import com.webmyne.riteway_driver.model.SharedPreferenceTrips;
import com.webmyne.riteway_driver.notifications.DriverNotification;
import com.webmyne.riteway_driver.settings.SettingsFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MyOrdersFragment extends Fragment {

    private SharedPreferenceTrips sharedPreferenceTrips;
    private SharedPreferenceNotification sharedPreferenceNotification;
    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private int badgeValue=0;
    private MyPagerAdapter adapter;
//    private ProgressDialog progressDialog;
    private CircleDialog circleDialog;
    private ArrayList<Trip> tripArrayList;
    public static Timer timer;
    private ArrayList<DriverNotification> notificationList;
    private String latituteValue;
    private String longitudeValue;
    private double updatedDriverLatitude;
    private double updatedDriverLongitude;
    private LocationManager locationManager;
    private String provider;
    private GPSTracker gpsTracker;

    public static MyOrdersFragment newInstance(String param1, String param2) {
        MyOrdersFragment fragment = new MyOrdersFragment();
        return fragment;
    }

    public MyOrdersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View convertView=inflater.inflate(R.layout.fragment_my_orders, container, false);
        tabs = (PagerSlidingTabStrip)convertView.findViewById(R.id.my_order_tabs);
        pager = (ViewPager) convertView.findViewById(R.id.pager);
        return convertView;
    }

    @Override
    public void onResume() {
        super.onResume();
        gpsTracker = new GPSTracker(getActivity());
        sharedPreferenceTrips=new SharedPreferenceTrips();
        sharedPreferenceNotification=new SharedPreferenceNotification();

        getCurrentRate();
//        try {
//            if (SettingsFragment.timer != null) {
//                SettingsFragment.timer.cancel();
////                Log.e("SettingsFragment.timer", "canceled");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        if(isConnected()==true) {
            getTripList();
        } else {
            Toast.makeText(getActivity(), "Internet Connection Unavailable", Toast.LENGTH_SHORT).show();
        }

    }


    public void getCurrentRate() {

        new CallWebService(AppConstants.CurrentRate, CallWebService.TYPE_JSONOBJECT) {

            @Override
            public void response(String response) {

                CurrentRate currentRates= new GsonBuilder().create().fromJson(response, CurrentRate.class);

//                Log.e("Rate: ",currentRates.Rate+"");
//                Log.e("TripFee: ",currentRates.TripFee+"");

                SharedPreferences preferences = getActivity().getSharedPreferences("current_rate",getActivity().MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("rate", currentRates.Rate+"");
                editor.putString("tripFee", currentRates.TripFee+"");
                if(currentRates.TimeInterval !=null) {
                    editor.putString("timeInterval", currentRates.TimeInterval + "");
                } else {
                    editor.putString("timeInterval", "3");
                }
                editor.commit();

                final SharedPreferences preferencesRate = getActivity().getSharedPreferences("current_rate",getActivity().MODE_PRIVATE);
                String updatedTimeInterval=preferencesRate.getString("timeInterval","3");
                Log.e("","");
//                String updatedTimeInterval="3";
                try {
                    timer=new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {

                            updateDriverLocation();
                            //updatedTimeInterval from API
                            //TODO

                        }
                    },0,1000*60*Integer.parseInt(updatedTimeInterval));

                }catch (NullPointerException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void error(VolleyError error) {

            }
        }.start();
    }

    public  boolean isConnected() {

        ConnectivityManager cm =(ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return  isConnected;
    }

    public void updateDriverLocation() {

        if (gpsTracker.canGetLocation()) {
            updatedDriverLatitude=gpsTracker.latitude;
            updatedDriverLongitude=gpsTracker.longitude;
        }
        try {
//            Log.e("timer: ", "timer cancelled");

            SettingsFragment.timer.cancel();
        } catch (Exception e){
            e.printStackTrace();
        }
        Log.e("latitude myorder: ",updatedDriverLatitude+"");
        Log.e("Longitude myorder: ",updatedDriverLongitude+"");

        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                JSONObject driverCurrentLocation = new JSONObject();
                try {
                    ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(getActivity(), "driver_data", 0);
                    DriverProfile driverProfile=complexPreferences.getObject("driver_data", DriverProfile.class);
                    driverCurrentLocation.put("DriverID", driverProfile.DriverID);
                    driverCurrentLocation.put("Webmyne_Latitude", updatedDriverLatitude+"");
                    driverCurrentLocation.put("Webmyne_Longitude",updatedDriverLongitude+"");

                }catch(JSONException e) {
                    e.printStackTrace();
                }
                Reader reader = API.callWebservicePost(AppConstants.DriverCurrentLocation, driverCurrentLocation.toString());
                ResponseMessage responseMessage = new GsonBuilder().create().fromJson(reader, ResponseMessage.class);
                Log.e("responseMessage:",responseMessage.Response+"");
                return null;

            }


        }.execute();

    }



    public void getTripList() {

        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(getActivity(), "driver_data", 0);
        DriverProfile driverProfile=complexPreferences.getObject("driver_data", DriverProfile.class);

//        Log.e("url: ", AppConstants.DriverTrips+driverProfile.DriverID );
        circleDialog=new CircleDialog(getActivity(),0);
        circleDialog.setCancelable(true);
        circleDialog.show();

        new CallWebService(AppConstants.DriverTrips+driverProfile.DriverID , CallWebService.TYPE_JSONARRAY) {

            @Override
            public void response(String response) {

                Type listType=new TypeToken<List<Trip>>(){
                }.getType();

                tripArrayList = new GsonBuilder().create().fromJson(response, listType);

                sharedPreferenceTrips.clearTrip(getActivity());

                for(int i=0;i<tripArrayList.size();i++){
                    sharedPreferenceTrips.saveTrip(getActivity(),tripArrayList.get(i));
                }

                adapter = new MyPagerAdapter(getActivity().getSupportFragmentManager());
                pager.setAdapter(adapter);
                final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
                pager.setPageMargin(pageMargin);
                tabs.setViewPager(pager);
                if(isConnected()==true) {
                    getNotificationList();
                } else {
                    Toast.makeText(getActivity(), "Internet Connection Unavailable", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void error(VolleyError error) {

                Log.e("error: ",error+"");
            }
        }.start();

    }

    public void getNotificationList() {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(getActivity(), "driver_data", 0);
        final DriverProfile driverProfile=complexPreferences.getObject("driver_data", DriverProfile.class);

        new CallWebService(AppConstants.GetDriverNotifications+driverProfile.DriverID , CallWebService.TYPE_JSONARRAY) {

            @Override
            public void response(String response) {

                Type listType=new TypeToken<List<DriverNotification>>(){
                }.getType();

                notificationList = new GsonBuilder().create().fromJson(response, listType);
//                Log.e("notification list size:",notificationList.size()+"");
                sharedPreferenceNotification.clearNotification(getActivity());
                for(int i=0;i<notificationList.size();i++){

                    if(notificationList.get(i).notificationStatus.equalsIgnoreCase("false")){
                        badgeValue=badgeValue+1;
                    }
                    sharedPreferenceNotification.saveNotification(getActivity(),notificationList.get(i));
//                    Log.e("notification id: ",notificationList.get(i).notificationStatus+"");
                }
//                Log.e("Badge value: ",badgeValue+"");
                SharedPreferences preferencesTimeInterval = getActivity().getSharedPreferences("badge_value",getActivity().MODE_PRIVATE);
                SharedPreferences.Editor editor=preferencesTimeInterval.edit();
                editor.putString("badge_value",badgeValue+"");
                editor.commit();
                circleDialog.dismiss();
            }

            @Override
            public void error(VolleyError error) {

                Log.e("error: ",error+"");

            }
        }.start();


    }

    public class MyPagerAdapter extends FragmentStatePagerAdapter {

        private final String[] TITLES = { "Current", "History", "Cancelled" };

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment=null;
            if(i==0) {
                fragment=CurrentOrdersFragment.newInstance("","");
            } else if(i==1) {
                fragment=OrdersHistoryFragment.newInstance("","");
            } else if(i==2) {
                fragment=CanceledOrdersFragment.newInstance("","");
            }
            return fragment;
        }
    }

}
