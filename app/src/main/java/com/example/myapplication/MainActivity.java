package com.example.myapplication;

import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.myapplication.databinding.ActivityMainBinding;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Scanner;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> itemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Fetching data...", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
                new Fetch().execute("https://fetch-hiring.s3.amazonaws.com/hiring.json");
            }
        });

        listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private class Fetch extends AsyncTask<String, Void, List<String>> {
        @Override
        protected List<String> bgOp(String... Url) {
            ArrayList<String> list = new ArrayList<>();
            try {
                URL url = new URL(Url[0]);
                HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                connect.setRequestMethod("GET"); //only view data
                Scanner scanner = new Scanner(connect.getInputStream());
                StringBuilder json = new StringBuilder();
                while (scanner.hasNextLine()) { //parse lines individually
                    json.append(scanner.nextLine());
                }
                scanner.close();
                JSONArray arr = new JSONArray(json);
                Map<Integer, List<String>> items = new TreeMap<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    int id = obj.getInt("listId");
                    if (id < 1 || id > 4) { // Valid list ids lie within the set {1, 2, 3, 4}
                        throw new RuntimeException("Invalid list id" + "\n" + id);
                    } else {
                        items.put(id, new ArrayList<>());
                    }
                    String name = obj.optString("name", "");
                    if (name.isBlank()) { // Missing name entry, throw exception
                        throw new RuntimeException("Missing name");
                    } else {
                        items.get(id).add(name);
                    }
                }
                for (Map.Entry<Integer, List<String>> item : items.entrySet()) {
                    Collections.sort(item.getValue());
                    list.add("id: " + item.getKey());
                    list.addAll(item.getValue());
                }
            } catch (Exception exception) {
                throw new RuntimeException("Couldn't fetch data properly" + "\n" + exception.getMessage());
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<String> data) {
            itemList.clear();
            itemList.addAll(data);
            adapter.notifyDataSetChanged();
        }
    }
}
