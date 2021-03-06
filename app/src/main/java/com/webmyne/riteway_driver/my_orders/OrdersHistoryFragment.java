package com.webmyne.riteway_driver.my_orders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.webmyne.riteway_driver.CustomViews.ComplexPreferences;
import com.webmyne.riteway_driver.CustomViews.ListDialog;
import com.webmyne.riteway_driver.R;

import com.webmyne.riteway_driver.model.AppConstants;
import com.webmyne.riteway_driver.model.SharedPreferenceTrips;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;


public class OrdersHistoryFragment extends Fragment implements ListDialog.setSelectedListner {

    private SharedPreferenceTrips sharedPreferenceTrips;
    private ListView ordersHistoryListView;
    private OrdersHistoryAdapter ordersHistoryAdapter;
    private TextView txtDateSelection;
    private ArrayList<Trip> ordersHistoryList;
    private ArrayList<Trip> filteredOrderList;
    private ArrayList<String> dateSelectionArray=new ArrayList<String>();

    public static OrdersHistoryFragment newInstance(String param1, String param2) {
        OrdersHistoryFragment fragment = new OrdersHistoryFragment();
        return fragment;
    }

    public OrdersHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dateSelectionArray.add("Current Week");
        dateSelectionArray.add("Last Week");
        dateSelectionArray.add("Current Month");
        dateSelectionArray.add("Last Month");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View convertView=inflater.inflate(R.layout.fragment_orders_history, container, false);
        txtDateSelection=(TextView)convertView.findViewById(R.id.txtDateSelection);
        txtDateSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
        ordersHistoryListView =(ListView)convertView.findViewById(R.id.ordersHistoryList);
        ordersHistoryListView.setEmptyView(convertView.findViewById(R.id.empty));
        return convertView;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {

            sharedPreferenceTrips=new SharedPreferenceTrips();
            ordersHistoryList=sharedPreferenceTrips.loadTrip(getActivity());
            filteredOrderList=new ArrayList<Trip>();

            filterData("Current Week");

            if(filteredOrderList != null) {
                Collections.reverse(filteredOrderList);
                ordersHistoryAdapter = new OrdersHistoryAdapter(getActivity(), filteredOrderList);
                ordersHistoryListView.setAdapter(ordersHistoryAdapter);
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class OrdersHistoryAdapter extends BaseAdapter {

        Context context;
        ArrayList<Trip> currentOrdersList;

        public OrdersHistoryAdapter(Context context, ArrayList<Trip> currentOrdersList) {
            this.context = context;
            this.currentOrdersList = currentOrdersList;
        }

        public int getCount() {
            return currentOrdersList.size();
        }

        public Object getItem(int position) {
            return currentOrdersList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        class ViewHolder {
            TextView orderHistoryCname,orderHistoryDate,orderHistoryPickupLocation,orderHistoryDropoffLocation,orderHistoryStatus
                    ,orderHistoryFareAmount;
        }

        public View getView(final int position, View convertView,
                            ViewGroup parent) {

            final ViewHolder holder;
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_ordered_history, parent, false);
                holder = new ViewHolder();

                holder.orderHistoryCname=(TextView)convertView.findViewById(R.id.orderHistoryCname);
                holder.orderHistoryDate=(TextView)convertView.findViewById(R.id.orderHistoryDate);
                holder.orderHistoryPickupLocation=(TextView)convertView.findViewById(R.id.orderHistoryPickupLocation);
                holder.orderHistoryDropoffLocation=(TextView)convertView.findViewById(R.id.orderHistoryDropoffLocation);
                holder.orderHistoryStatus=(TextView)convertView.findViewById(R.id.orderHistoryStatus);
                holder.orderHistoryFareAmount=(TextView)convertView.findViewById(R.id.currentOrderFareAmount);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.orderHistoryCname.setText(currentOrdersList.get(position).CustomerName);
            holder.orderHistoryDate.setText(getFormatedDate(currentOrdersList.get(position)));
            holder.orderHistoryPickupLocation.setText("pickup: "+currentOrdersList.get(position).PickupAddress);
            holder.orderHistoryDropoffLocation.setText("dropoff: "+currentOrdersList.get(position).DropOffAddress);
            holder.orderHistoryStatus.setText("status: "+currentOrdersList.get(position).TripStatus);
            holder.orderHistoryFareAmount.setText(String.format("$ %.2f", getTotal(currentOrdersList.get(position)))+"");

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(getActivity(), "current_trip_details", 0);
                    complexPreferences.putObject("current_trip_details", currentOrdersList.get(position));
                    complexPreferences.commit();
                    Intent i=new Intent(getActivity(), OrderDetailActivity.class);
                    startActivity(i);


                }
            });
            return convertView;
        }
    }

    private void filterData(String filterType){
        try {

            filteredOrderList.clear();

            SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

            int day = Integer.parseInt(dayFormat.format(new Date()));
            int month = Integer.parseInt(monthFormat.format(new Date()))-1;
            int year = Integer.parseInt(yearFormat.format(new Date()));

            Calendar calendar = Calendar.getInstance();
            calendar.set(calendar.YEAR,year);
            calendar.set(calendar.MONTH,month);
            calendar.set(calendar.DAY_OF_MONTH,day);

            int currentWeekOfyear=calendar.get(calendar.WEEK_OF_YEAR);
            int lastWeekOfYear=currentWeekOfyear-1;
            if(lastWeekOfYear<1){
                Calendar c = Calendar.getInstance();
                c.set(c.YEAR,calendar.YEAR-1);
                c.set(c.MONTH,11);
                c.set(c.DAY_OF_MONTH,31);
                lastWeekOfYear=c.get(c.WEEK_OF_YEAR);
            }

            int currentMonth=calendar.get(calendar.MONTH);
            int lastMonth=currentMonth-1;

            if(lastMonth<0){
                Calendar c = Calendar.getInstance();
                c.set(c.YEAR,calendar.YEAR-1);
                c.set(c.MONTH,11);
                c.set(c.DAY_OF_MONTH,31);
                lastMonth=c.get(c.MONTH);
            }

            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
            String date=format.format(new Date());

            for (int i = 0; i < filteredOrderList.size(); i++) {
                Date loopDate = format.parse(getFormatedDate(filteredOrderList.get(i)));
                int loopDay = Integer.parseInt(dayFormat.format(loopDate));
                int loopMonth = Integer.parseInt(monthFormat.format(loopDate))-1;
                int loopYear = Integer.parseInt(yearFormat.format(loopDate));

                Calendar loopCalendar = Calendar.getInstance();
                loopCalendar.set(loopCalendar.YEAR,loopYear);
                loopCalendar.set(loopCalendar.MONTH,loopMonth);
                loopCalendar.set(loopCalendar.DAY_OF_MONTH,loopDay);

                int loopCurrentWeekOfyear=loopCalendar.get(loopCalendar.WEEK_OF_YEAR);
                int loopCurrentMonth=loopCalendar.get(loopCalendar.MONTH);

                if( filteredOrderList.get(i).TripStatus.contains(AppConstants.tripSuccessStatus)) {
                    if (filterType.equalsIgnoreCase("Current Week")) {
                        if (currentWeekOfyear == loopCurrentWeekOfyear && (!date.equals(getFormatedDate(filteredOrderList.get(i))))) {
                            filteredOrderList.add(filteredOrderList.get(i));
                        }
                    } else if (filterType.equalsIgnoreCase("Last Week")) {
                        if (lastWeekOfYear == loopCurrentWeekOfyear) {
                            filteredOrderList.add(filteredOrderList.get(i));
                        }
                    } else if (filterType.equalsIgnoreCase("Current Month")) {
                        if (currentMonth == loopCurrentMonth) {
                            filteredOrderList.add(filteredOrderList.get(i));
                        }
                    } else if (filterType.equalsIgnoreCase("Last Month")) {
                        if (lastMonth == loopCurrentMonth) {
                            filteredOrderList.add(filteredOrderList.get(i));
                        }
                    }
                }
            }
            if(ordersHistoryAdapter != null) {
                ordersHistoryAdapter.notifyDataSetChanged();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void showDialog() {

        ListDialog listDialog = new ListDialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
        listDialog.setCancelable(true);
        listDialog.setCanceledOnTouchOutside(true);
        listDialog.title("SELECT DATE FILTER");
        listDialog.setItems(dateSelectionArray);
        listDialog.setSelectedListner(this);
        listDialog.show();
    }

    @Override
    public void selected(String value) {

        txtDateSelection.setText("Filtered By "+value);
        filterData(value);
    }

    public String getFormatedDate(Trip currentTrip) {

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        float dateinFloat = Float.parseFloat(currentTrip.TripDate);
        Date date = float2Date(dateinFloat);
        return  format.format(date);
    }

    public  java.util.Date float2Date(float nbSeconds) {
        java.util.Date date_origine;
        java.util.Calendar date = java.util.Calendar.getInstance();
        java.util.Calendar origine = java.util.Calendar.getInstance();
        origine.set(1970, Calendar.JANUARY, 1);
        date_origine = origine.getTime();
        date.setTime(date_origine);
        date.add(java.util.Calendar.SECOND, (int) nbSeconds);
        return date.getTime();
    }

    public double getTotal(Trip currentTrip) {
        Double total;
        String tripFareValue=String.format("%.2f", Double.parseDouble(currentTrip.TripDistance)*0.6214*Double.parseDouble(currentTrip.TripFare));
        if(Integer.parseInt(currentTrip.TipPercentage)>0){

            Double tip=((Double.parseDouble(tripFareValue)*Double.parseDouble(currentTrip.TipPercentage))/100);
            total= Double.parseDouble(tripFareValue)+tip;
        } else {
            total=Double.parseDouble(tripFareValue);
        }
        total=total+Double.parseDouble(currentTrip.TripFee);
        return total;
    }

}
