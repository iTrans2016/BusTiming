package com.example.ben.itrans;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{

    Button homeBtn;
    JsonObjectRequest jsonObjectRequest;
    int count;
    RequestQueue requestQueue;
    int label;
    boolean dontCall;
    boolean end;
    private List<Bus> busService = new ArrayList<>();
    private RecyclerView recyclerView;
    public static homeAdapter hAdapter;
    Bus bus;
    int selection;
    boolean select;
    int ending;
    public int toggle;
    LatLng mLocation = new LatLng(1.29684825487647, 103.85253591654006);
    public static List<Integer> toggleCounter = new ArrayList<>();

    public static String busStop;
    SharedPreferences homePrefs;
    SharedPreferences.Editor homeEditor;
    Geocoder geocoder;
    List<Address> addresses;
    double location_stopX;
    double location_stopY;
    String[] locationVar;
    String address;
    //private String[] homeOptions;
    //private DrawerLayout mDrawerLayout;
    //private ListView mDrawerList;
    //public int count = 0;
    /*public boolean callAgain = true;
    public boolean end= false;*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        count = -50;
        label = 1;
        dontCall = false;
        end = false;
        select = false;
        selection = 0;
        toggle = 0;
        Toolbar homeToolBar = (Toolbar) findViewById(R.id.homeToolBar);
        setSupportActionBar(homeToolBar);
        requestQueue = VolleySingleton.getInstance().getRequestQueue();
        homeBtn = (Button) findViewById(R.id.searchBus);
        recyclerView = (RecyclerView) findViewById(R.id.busServices);
        hAdapter = new homeAdapter(busService);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(hAdapter);
        MapFragment mapFragment =(MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Bundle b = getIntent().getExtras();
        homePrefs = getSharedPreferences("bus_stop_info",0);
        busStop = "01012";
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        if(b !=null) {
            busStop = b.getString("busStopNo");
            mLocation = b.getParcelable("busStopPt");
            homeEditor = homePrefs.edit();
            homeEditor.putString("bus_stop", busStop).commit();
            homeEditor.putString("bus_stop_location", String.valueOf(mLocation)).commit();
        }
        if(homePrefs.getString("bus_stop",null)!=null){
            busStop = homePrefs.getString("bus_stop", "");
            locationVar = homePrefs.getString("bus_stop_location","").split(",");
            Toast.makeText(getApplicationContext(),locationVar[0].replace("lat/lng: (",""),Toast.LENGTH_LONG).show();
            location_stopX = Double.parseDouble(locationVar[0].replace("lat/lng: (",""));
            location_stopY = Double.parseDouble(locationVar[1].replace(")",""));
            mLocation = new LatLng(location_stopX,location_stopY);
        }else{
            locationVar = String.valueOf(mLocation).split(",");
            location_stopX = Double.parseDouble(locationVar[0].replace("lat/lng: (",""));
            location_stopY = Double.parseDouble(locationVar[1].replace(")",""));
        }
        try {
            addresses = geocoder.getFromLocation(location_stopX, location_stopY, 1);
            if(addresses!=null){
                Toast.makeText(getApplicationContext(),String.valueOf(addresses),Toast.LENGTH_LONG).show();
                address = addresses.get(0).getAddressLine(2);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        //homeOptions = new String[]{"Apples","Bananas","Oranges"};
        //mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //mDrawerList = (ListView) findViewById(R.id.left_drawer);

        //mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.activity_main, homeOptions));
        //mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        /*for(int i =0;i<busList.size();i++){
            ma.deleteFromDatabase(i,getApplicationContext());
        }
        busList = ma.retrieveAll(getApplicationContext());*/

        homeBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent searchNo = new Intent(getApplicationContext(),searchBusNo.class);
                startActivity(searchNo);
            }
        });

        homeBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent searchStop = new Intent(getApplicationContext(), searchBusStop.class);
                startActivity(searchStop);
                return true;
            }
        });

        //Toast.makeText(getApplicationContext(), "Processing data...", Toast.LENGTH_SHORT).show();
        call(busStop, 0);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener(){
            @Override
            public void onClick(View view, int position){
                select = true;
                selection = position;
                if(toggleCounter.get(selection)==0) {
                    toggleCounter.set(selection,1);
                    call(busStop, toggleCounter.get(selection));
                }else if(toggleCounter.get(selection)==1){
                    toggleCounter.set(selection,0);
                    call(busStop, toggleCounter.get(selection));
                }
            }
        }));
    }

    @Override
    public void onMapReady(GoogleMap map){
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mLocation,17));
        map.addMarker(new MarkerOptions().position(mLocation)
                                        .title(address)
                                        .snippet(addresses.get(0).getAddressLine(5))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_black_24dp)));
    }


    public void call(String busStop, final int toggle){
        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID="+busStop+"&SST=True", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("Services");
                            if(select){
                               ending = selection+1;
                            } else{
                                ending = jsonArray.length();
                            }
                            for (int i = selection; i < ending; i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                String inService = services.getString("Status");
                                if(inService.equals("In Operation")) {
                                    JSONObject nextBus = services.getJSONObject("NextBus");
                                    String eta = nextBus.getString("EstimatedArrival");
                                    String wheelC = nextBus.getString("Feature");
                                    String load = nextBus.getString("Load");
                                    if (select && toggle == 1) {
                                        JSONObject subBus = services.getJSONObject("SubsequentBus");
                                        String NEta = subBus.getString("EstimatedArrival");
                                        String NWheelC = subBus.getString("Feature");
                                        String NLoad = subBus.getString("Load");
                                        if (!NEta.equals("")) {
                                            bus = busService.get(selection);
                                            bus.setBF(NWheelC);
                                            bus.setBusT(NEta);
                                            bus.setSpace(NLoad);
                                            hAdapter.notifyItemChanged(selection);
                                        }
                                    } else if (select && toggle == 0) {
                                        bus = busService.get(selection);
                                        bus.setBF(wheelC);
                                        bus.setBusT(eta);
                                        bus.setSpace(load);
                                        hAdapter.notifyItemChanged(selection);
                                    } else {
                                        bus = new Bus(busNo, eta, wheelC, load);
                                        busService.add(bus);
                                        toggleCounter.add(0);
                                    }
                                }else{
                                    if(!select) {
                                        bus = new Bus(busNo, "Not in operation", "", "Limited Standing");
                                        busService.add(bus);
                                        toggleCounter.add(0);
                                    }else{
                                        bus = busService.get(selection);
                                        bus.setBF("");
                                        bus.setBusT("Not in operation");
                                        bus.setSpace("Limited Standing");
                                        hAdapter.notifyItemChanged(selection);
                                    }
                                }
                            }
                            if(!select){
                                hAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                        Toast.makeText(getApplicationContext(), "That did not work:(", Toast.LENGTH_LONG).show();
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

    public interface ClickListener{
        void onClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{
        private GestureDetector gestureDetector;
        private MainActivity.ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final MainActivity.ClickListener clickListener){
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onSingleTapUp(MotionEvent e){
                    return true;
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e){
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if(child != null && clickListener != null && gestureDetector.onTouchEvent(e)){
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e){

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept){

        }
    }


    /*private class DrawerItemClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id){
            selectItem(position);
        }
    }

    private void selectItem(int position){
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }*/
    //Insert drawer functions

    public String getBusStop(){
        return busStop;
    }
    public void busIsHere(int position){
        toggleCounter.set(position,0);
        System.out.println(String.valueOf(toggleCounter));
    }

    public void notifyData(int pos){
        hAdapter.notifyItemChanged(pos);
    }
}