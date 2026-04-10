package com.example.expensemanagement.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.Adapters.StockAdapter;
import com.example.expensemanagement.R;
import com.example.expensemanagement.model.StockItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StockFragment extends Fragment {

    private StockAdapter adapter;
    private Random random;

    public static StockFragment newInstance() { return new StockFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stock, container, false);

        random = new Random();

        // Ngày hiện tại
        TextView tvDate = view.findViewById(R.id.tvDate);
        String date = new SimpleDateFormat("d 'tháng' M", new Locale("vi")).format(new Date());
        tvDate.setText(date);

        // RecyclerView
        RecyclerView recycler = view.findViewById(R.id.recyclerStocks);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StockAdapter();
        recycler.setAdapter(adapter);

        // Refresh button
        view.findViewById(R.id.btnRefresh).setOnClickListener(v -> loadStocks());

        loadStocks();
        return view;
    }

    private void loadStocks() {
        try {
            // Đọc file JSON từ assets
            InputStream is = requireContext().getAssets().open("stocks.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONArray arr = new JSONArray(json);
            List<StockItem> allStocks = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String symbol   = obj.getString("symbol");
                String fullName = obj.getString("fullName");
                double basePrice = obj.getDouble("basePrice");
                String currency  = obj.getString("currency");

                // Random thay đổi giá: -3% đến +3%
                double changePct = (random.nextDouble() * 6 - 3);
                double change    = basePrice * changePct / 100;
                double price     = basePrice + change;

                // Random chart points (20 điểm)
                List<Float> chartPoints = generateChartPoints(basePrice, changePct > 0);

                allStocks.add(new StockItem(symbol, fullName, price, change, changePct, currency, chartPoints));
            }

            // Shuffle để mỗi lần load trông khác
            Collections.shuffle(allStocks, random);

            adapter.setItems(allStocks);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sinh ngẫu nhiên 20 điểm chart có xu hướng tăng hoặc giảm
     */
    private List<Float> generateChartPoints(double basePrice, boolean trending) {
        List<Float> points = new ArrayList<>();
        double val = basePrice;
        for (int i = 0; i < 20; i++) {
            double noise = (random.nextDouble() - 0.5) * basePrice * 0.01;
            double trend = trending ? basePrice * 0.001 : -basePrice * 0.001;
            val += noise + trend;
            points.add((float) val);
        }
        return points;
    }
}