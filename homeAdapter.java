package com.example.ben.itrans;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by helen_000 on 7/2/2016.
 */
public class homeAdapter extends RecyclerView.Adapter<homeAdapter.MyViewHolder> {

    String timeRemaining;
    private List<Bus> busServices;
    private List<Integer> Positions = new ArrayList<>();
    private List<CountDownTimer> timers = new ArrayList<>();
    SharedPreferences.Editor editor;
    public static final String MY_PREFS_POS = "MyPrefsPos";
    RequestQueue requestQueue;
    Bus bus;

    public class MyViewHolder extends RecyclerView.ViewHolder{
        public TextView ETA, busNum;
        public ImageView BusFeature;

        public MyViewHolder(View view){
            super(view);
            busNum = (TextView) view.findViewById(R.id.busNumber);
            ETA = (TextView) view.findViewById(R.id.busTiming);
            BusFeature = (ImageView) view.findViewById(R.id.wheelCA);
        }
    }

    public homeAdapter(List<Bus> busServices){
        this.busServices = busServices;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bus_home, parent, false);
        if(Positions.isEmpty()){
            for(int i =0;i<busServices.size();i++) {
                Positions.add(0);
            }
            System.out.println(String.valueOf(Positions));
        }
        requestQueue = VolleySingleton.getInstance().getRequestQueue();
        return new MyViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        Bus bus = busServices.get(position);
        holder.busNum.setText(bus.getBusNo());
        if (bus.getBF().isEmpty()) {
            holder.BusFeature.setVisibility(View.INVISIBLE);
        } else {
            holder.BusFeature.setVisibility(View.VISIBLE);
        }

        if (bus.getSpace().equals("Seats Available")) {
            holder.busNum.setTextColor(Color.parseColor("#000000"));
        } else if (bus.getSpace().equals("Standing Available")) {
            holder.busNum.setTextColor(Color.parseColor("#FF808080"));
        } else if (bus.getSpace().equals("Limited Standing")) {
            holder.busNum.setTextColor(Color.parseColor("#ef6b6b"));
        }
        if (bus.getBusT().equals("Not in operation")) {
            holder.ETA.setText(bus.getBusT());
            holder.ETA.setTextColor(Color.parseColor("#ef6b6b"));
            if (Positions.get(holder.getAdapterPosition()) == 0) {
                timers.add(new CountDownTimer(0, 0) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {

                    }
                });
            }
        } else {
            holder.ETA.setTextColor(Color.parseColor("#000000"));
            Calendar c = Calendar.getInstance();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sst = format.format(c.getTime());
            Date eta = null;
            Date current = null;
            long diff = 0;
            try {
                String[] splitString = bus.getBusT().split("T");
                splitString[1].replace("+08:00", "");
                eta = format.parse(splitString[0] + " " + splitString[1]);
                current = format.parse(sst);

                diff = eta.getTime() - current.getTime() + 30000;

            } catch (Exception e) {
                e.printStackTrace();
            }
            if(diff>0) {

                if (Positions.get(holder.getAdapterPosition()) == 0) {
                    timers.add(new CountDownTimer(diff, 10000) {
                        public void onTick(long millisUntilFinished) {
                            if (millisUntilFinished < 60000) {
                                holder.ETA.setText("Arriving");
                            } else {
                                holder.ETA.setText(String.valueOf(millisUntilFinished / 60000) + " min");
                            }
                        }

                        public void onFinish() {
                            MainActivity ma = new MainActivity();
                            //holder.ETA.append(String.valueOf(holder.getAdapterPosition()));
                            String busStopCode = ma.getBusStop();
                            int pos = holder.getAdapterPosition();
                            System.out.println(busStopCode + String.valueOf(pos));
                            call(busStopCode, pos,false);
                        }

                    });
                    Positions.set(holder.getAdapterPosition(), 1);
                } else if (Positions.get(holder.getAdapterPosition()) == 1) {
                    timers.get(holder.getAdapterPosition()).cancel();
                    timers.set(holder.getAdapterPosition(), new CountDownTimer(diff, 10000) {
                        public void onTick(long millisUntilFinished) {
                            if (millisUntilFinished < 60000) {
                                holder.ETA.setText("Arriving");
                            } else {
                                holder.ETA.setText(String.valueOf(millisUntilFinished / 60000) + " min");
                            }
                        }

                        public void onFinish() {
                            MainActivity ma = new MainActivity();
                            //holder.ETA.append(String.valueOf(holder.getAdapterPosition()));
                            String busStopCode = ma.getBusStop();
                            int pos = holder.getAdapterPosition();
                            System.out.println(busStopCode + String.valueOf(pos));
                            call(busStopCode, pos,false);
                        }

                    });
                }
                timers.get(holder.getAdapterPosition()).start();
            }else{
                MainActivity ma = new MainActivity();
                String busStopCode = ma.getBusStop();
                int pos = holder.getAdapterPosition();
                call(busStopCode,pos,true);
            }
        }

    }


    public void onViewDetachedFromWindow(MyViewHolder holder) {
        timers.get(holder.getAdapterPosition()).cancel();
    }

    @Override
    public int getItemCount(){
        return busServices.size();
    }

    public void call(String busStop, final int selection, final  boolean override){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID="+busStop+"&SST=True", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("Services");
                            for (int i = selection; i < selection+1; i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String inService = services.getString("Status");
                                if(inService.equals("In Operation")) {
                                    if (override) {
                                        JSONObject nextBus = services.getJSONObject("SubsequentBus");
                                        String eta = nextBus.getString("EstimatedArrival");
                                        String wheelC = nextBus.getString("Feature");
                                        String load = nextBus.getString("Load");
                                        bus = busServices.get(selection);
                                        if(eta.equals("")){
                                            bus.setBF("");
                                            bus.setBusT("Not in operation");
                                            bus.setSpace("Limited Standing");
                                        }else{
                                            bus.setBF(wheelC);
                                            bus.setBusT(eta);
                                            bus.setSpace(load);
                                        }
                                    } else {
                                        JSONObject nextBus = services.getJSONObject("NextBus");
                                        String eta = nextBus.getString("EstimatedArrival");
                                        bus = busServices.get(selection);
                                        if(eta.equals("")){
                                            bus.setBF("");
                                            bus.setBusT("Not in operation");
                                            bus.setSpace("Limited Standing");
                                        }else {
                                            String wheelC = nextBus.getString("Feature");
                                            String load = nextBus.getString("Load");
                                            bus.setBF(wheelC);
                                            bus.setBusT(eta);
                                            bus.setSpace(load);
                                            System.out.println("New eta: " + String.valueOf(bus.getBusNo()) + String.valueOf(bus.getBusT()));
                                        }
                                    }
                                }
                                }
                            MainActivity ma = new MainActivity();
                            ma.busIsHere(selection);
                            ma.notifyData(selection);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("AccountKey", "3SnRYzr/X0eKp2HvwTYtmg==");
                headers.put("UniqueUserID", "0bf7760d-15ec-4a1b-9c82-93562fcc9798");
                headers.put("accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);

    }

}
