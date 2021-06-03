package com.example.coin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ConnectActivity";
    private static final int CONNECTION_REQUEST = 1;
    private static final int REMOVE_FAVORITE_INDEX = 0;

    private ListView roomListView;
    private SharedPreferences sharedPref;

    private String keyprefRoomList;
    private ArrayList<custom> roomList;
    private MyAdapter adapter;
    private String roomID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_start);
        getSupportActionBar().hide();
        InitWiget();
    }

    private String randomString(int length) {
        String characterSet = getString(R.string.upper_alpha_digit);

        StringBuilder sb = new StringBuilder(); //consider using StringBuffer if needed
        for (int i = 0; i < length; i++) {
            int randomInt = new SecureRandom().nextInt(characterSet.length());
            sb.append(characterSet.substring(randomInt, randomInt + 1));
        }
        //return sb.toString();
        EditText editText = findViewById(R.id.edit);

        return editText.getText().toString();
    }

    private void InitWiget() {
        roomListView = findViewById(R.id.ac_start_room_list);
        roomListView.setEmptyView(findViewById(android.R.id.empty));
        roomListView.setOnItemClickListener((adapterView, view, i, l) -> {
            String roomId = ((TextView) view).getText().toString();
            connectToRoom(roomId);
        });
        registerForContextMenu(roomListView);
        findViewById(R.id.ac_start_user_next).setOnClickListener(view -> {
            roomID = randomString(8);
            if (roomID.length() > 0 && !roomList.contains(roomID)) {
                roomList.add(new custom(R.drawable.ic_launcher_foreground, roomID));
                adapter.notifyDataSetChanged();
            }
            connectToRoom(roomID);
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.ac_start_room_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(roomList.get(info.position).txt);
            String[] menuItems = getResources().getStringArray(R.array.roomListContextMenu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        } else {
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == REMOVE_FAVORITE_INDEX) {
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            roomList.remove(info.position);
            adapter.notifyDataSetChanged();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        String roomListJson = new JSONArray(roomList).toString();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(keyprefRoomList, roomListJson);
        editor.commit();
    }


    @Override
    public void onResume() {
        super.onResume();
        roomList = new ArrayList<>();
        String roomListJson = sharedPref.getString(keyprefRoomList, null);
        if (roomListJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(roomListJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    roomList.add(new custom(R.drawable.ic_launcher_foreground, jsonArray.get(i).toString()));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to load room list: " + e.toString());
            }
        }
        adapter = new MyAdapter(this, android.R.layout.simple_list_item_1, roomList);
        roomListView.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            roomListView.requestFocus();
            roomListView.setItemChecked(0, true);
        }
    }


    @SuppressWarnings("StringSplitter")
    private void connectToRoom(String roomId) {

        Intent intent = new Intent(this, RoomActivity.class);
        intent.putExtra("RoomID", roomId);

        startActivity(intent);
    }

    class custom{
        public int img;
        public String txt;

        public custom(int img, String msg) {
            this.img = img;
            this.txt = msg;
        }

        public int getImg() {
            return img;
        }

        public void setImg(int img) {
            this.img = img;
        }

        public String getTxt() {
            return txt;
        }

        public void setTxt(String txt) {
            this.txt = txt;
        }
    }

    class MyAdapter extends BaseAdapter {
        Context context;
        int layout;
        ArrayList<custom> al;
        LayoutInflater inf;

        public MyAdapter(Context context, int layout, ArrayList<custom> al) {
            this.context = context;
            this.layout = layout;
            this.al = al;
            this.inf = (LayoutInflater) context.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() { // 총 데이터의 개수
            return al.size();
        }

        @Override
        public Object getItem(int position) { // 해당 행의 데이터
            return al.get(position);
        }

        @Override
        public long getItemId(int position) { // 해당 행의 유니크한 id
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = inf.inflate(layout, null);


            TextView tv = convertView.findViewById(R.id.adapter_txt);
            ImageView iv = convertView.findViewById(R.id.adapter_img);

            custom c = al.get(position);
            tv.setText(c.txt);
            iv.setImageResource(c.img);


            return convertView;
        }

        public AdapterView.OnItemClickListener mItemClickListner = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // parent는 AdapterView의 속성의 모두 사용 할 수 있다.
                //String tv = (String) parent.getAdapter().getItem(position);
                Toast.makeText(context.getApplicationContext(), (position+1)+"번 나코를 선택 하셨습니다.", Toast.LENGTH_SHORT).show();
                //view.setSelected(false);
                //https://recipes4dev.tistory.com/47?category=605791
                //리스트 뷰에 관한것
            /*ImageView iv = view.findViewById(R.id.imageView1);
            view.setBackgroundColor(Color.RED);
            notifyDataSetChanged();*/
            }
        };
    }
}
