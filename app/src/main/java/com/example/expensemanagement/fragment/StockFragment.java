package com.example.expensemanagement.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crazzyghost.alphavantage.AlphaVantage;
import com.crazzyghost.alphavantage.Config;
import com.crazzyghost.alphavantage.timeseries.response.QuoteResponse;
import com.example.expensemanagement.Adapters.StockAdapter;
import com.example.expensemanagement.R;
import com.example.expensemanagement.model.StockItem;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class StockFragment extends Fragment {

    private static final String TAG = "StockFragment";
    private static final String API_KEY = "E7QTZG71WW81RL6A";

    private StockAdapter adapter;
    private final List<StockItem> displayList = new ArrayList<>();
    private final Map<String, List<Float>> chartCache = new HashMap<>();
    
    private final Random random = new Random();
    private final Handler apiHandler = new Handler(Looper.getMainLooper());

    private static boolean isApiInitialized = false;

    public static StockFragment newInstance() {
        return new StockFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isApiInitialized) {
            try {
                Config cfg = Config.builder()
                        .key(API_KEY)
                        .timeOut(10)
                        .build();
                AlphaVantage.api().init(cfg);
                isApiInitialized = true;
            } catch (Exception e) {
                Log.e(TAG, "AlphaVantage Init Error", e);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stock, container, false);

        TextView tvDate = view.findViewById(R.id.tvDate);
        if (tvDate != null) {
            String date = new SimpleDateFormat("d 'tháng' M", new Locale("vi")).format(new Date());
            tvDate.setText(date);
        }

        RecyclerView recycler = view.findViewById(R.id.recyclerStocks);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StockAdapter();
        recycler.setAdapter(adapter);

        adapter.setOnItemClickListener(this::showStockDetailDialog);

        View btnRefresh = view.findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                // Random và xáo trộn danh sách khi bấm refresh
                Collections.shuffle(displayList);
                adapter.setItems(new ArrayList<>(displayList));
                Toast.makeText(getContext(), "Đã làm mới và xáo trộn danh mục", Toast.LENGTH_SHORT).show();
                
                // Vẫn giữ cơ chế cập nhật giá từ API sau đó
                startApiUpdates();
            });
        }

        loadStocks();
        return view;
    }

    private void showStockDetailDialog(StockItem item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_stock_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Fix: Use correct IDs from dialog_stock_detail.xml
        TextView tvSymbol = dialogView.findViewById(R.id.tvDetailSymbol);
        TextView tvName = dialogView.findViewById(R.id.tvDetailName);
        TextView tvPrice = dialogView.findViewById(R.id.tvDetailPrice);
        TextView tvChange = dialogView.findViewById(R.id.tvDetailChange);
        LineChart chart = dialogView.findViewById(R.id.detailChart);

        TextView tvOpen = dialogView.findViewById(R.id.tvDetailOpen);
        TextView tvHigh = dialogView.findViewById(R.id.tvDetailHigh);
        TextView tvLow = dialogView.findViewById(R.id.tvDetailLow);
        TextView tvVolume = dialogView.findViewById(R.id.tvDetailVolume);
        TextView tvPrevClose = dialogView.findViewById(R.id.tvDetailPrevClose);

        if (tvSymbol != null) tvSymbol.setText(item.symbol);
        if (tvName != null) tvName.setText(item.fullName);

        Locale vnLocale = new Locale("vi", "VN");
        boolean isVND = "VND".equals(item.currency);

        if (tvPrice != null) {
            if (isVND) {
                tvPrice.setText(String.format(vnLocale, "%,.0f đ", item.price));
            } else {
                tvPrice.setText(String.format(Locale.US, "$%.2f", item.price));
            }
        }

        if (tvChange != null) {
            if (isVND) {
                tvChange.setText(String.format(vnLocale, "%+,.0f (%+.2f%%)", item.change, item.changePct));
            } else {
                tvChange.setText(String.format(Locale.US, "%+.2f (%.2f%%)", item.change, item.changePct));
            }
            tvChange.setTextColor(item.isPositive ? Color.parseColor("#30D158") : Color.parseColor("#FF453A"));
        }

        setupMockDetails(item, tvOpen, tvHigh, tvLow, tvVolume, tvPrevClose, isVND, vnLocale);
        setupDetailChart(chart, item.chartPoints, item.isPositive);

        View btnClose = dialogView.findViewById(R.id.btnCloseDetail);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (!isVND) {
            try {
                AlphaVantage.api().timeSeries()
                        .quote()
                        .forSymbol(item.symbol)
                        .onSuccess((QuoteResponse response) -> {
                            if (response != null && response.getErrorMessage() == null && isAdded()) {
                                getActivity().runOnUiThread(() -> {
                                    String realFmt = "$%.2f";
                                    if (tvOpen != null) tvOpen.setText(String.format(Locale.US, realFmt, response.getOpen()));
                                    if (tvHigh != null) tvHigh.setText(String.format(Locale.US, realFmt, response.getHigh()));
                                    if (tvLow != null) tvLow.setText(String.format(Locale.US, realFmt, response.getLow()));
                                    if (tvVolume != null) tvVolume.setText(String.format(Locale.US, "%,d", (long)response.getVolume()));
                                    if (tvPrevClose != null) tvPrevClose.setText(String.format(Locale.US, realFmt, response.getPreviousClose()));
                                });
                            }
                        })
                        .fetch();
            } catch (Exception e) {
                Log.e(TAG, "API detail fetch error", e);
            }
        }

        dialog.show();
    }

    private void setupMockDetails(TextView tvOpen, TextView tvHigh, TextView tvLow, TextView tvVolume, TextView tvPrevClose, StockItem item, boolean isVND, Locale locale) {
        // Keeping this method signature to avoid breaking logic, but actually item was the first param in original.
        // Re-aligning to match original call order if possible or just fixing logic.
    }

    // Re-implementing with correct parameter order used in the call
    private void setupMockDetails(StockItem item, TextView tvOpen, TextView tvHigh, TextView tvLow, TextView tvVolume, TextView tvPrevClose, boolean isVND, Locale locale) {
        double mockOpen = item.price * (1 + (random.nextDouble() - 0.5) * 0.01);
        double mockHigh = Math.max(mockOpen, item.price) * (1 + random.nextDouble() * 0.005);
        double mockLow = Math.min(mockOpen, item.price) * (1 - random.nextDouble() * 0.005);
        long mockVol = 500000 + random.nextInt(10000000);
        double mockPrev = item.price - item.change;

        String fmt = isVND ? "%,.0f" : "$%.2f";
        if (tvOpen != null) tvOpen.setText(String.format(locale, fmt, mockOpen));
        if (tvHigh != null) tvHigh.setText(String.format(locale, fmt, mockHigh));
        if (tvLow != null) tvLow.setText(String.format(locale, fmt, mockLow));
        if (tvVolume != null) tvVolume.setText(String.format(Locale.US, "%,d", mockVol));
        if (tvPrevClose != null) tvPrevClose.setText(String.format(locale, fmt, mockPrev));
    }

    private void setupDetailChart(LineChart chart, List<Float> data, boolean isPositive) {
        if (chart == null || data == null || data.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }
        LineDataSet dataSet = new LineDataSet(entries, "Price");
        int color = isPositive ? Color.parseColor("#30D158") : Color.parseColor("#FF453A");
        dataSet.setColor(color);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(40);
        chart.setData(new LineData(dataSet));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.animateX(800);
        chart.invalidate();
    }

    private void loadStocks() {
        if (displayList.isEmpty()) {
            loadInitialDataFromJson();
        }
        startApiUpdates();
    }

    private void startApiUpdates() {
        apiHandler.removeCallbacksAndMessages(null);
        int callCount = 0;
        for (StockItem item : displayList) {
            if ("USD".equals(item.currency) && callCount < 5) {
                final String symbol = item.symbol;
                apiHandler.postDelayed(() -> fetchStockData(symbol), callCount * 13000L);
                callCount++;
            }
        }
    }

    private void loadInitialDataFromJson() {
        try {
            InputStream is = requireContext().getAssets().open("StockData.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);

            displayList.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String symbol = obj.getString("symbol");
                String fullName = obj.getString("fullName");
                double price = obj.getDouble("price");
                double change = obj.getDouble("change");
                double changePct = obj.getDouble("changePct");
                String currency = obj.getString("currency");

                List<Float> points = chartCache.get(symbol);
                if (points == null) {
                    points = generateDummyChart(price, change >= 0, 20);
                    chartCache.put(symbol, points);
                }
                
                displayList.add(new StockItem(symbol, fullName, price, change, changePct, currency, points));
            }
            // Shuffle lần đầu khi load
            Collections.shuffle(displayList);
            adapter.setItems(new ArrayList<>(displayList));
        } catch (Exception e) {
            Log.e(TAG, "Error loading StockData.json", e);
        }
    }

    private void fetchStockData(String symbol) {
        try {
            AlphaVantage.api().timeSeries()
                    .quote()
                    .forSymbol(symbol)
                    .onSuccess((QuoteResponse response) -> {

                        // 🔥 Check null + lỗi API
                        if (response == null) {
                            Log.e(TAG, "Response null for: " + symbol);
                            fallbackToLocalData(symbol);
                            return;
                        }

                        if (response.getErrorMessage() != null) {
                            Log.e(TAG, "API error for " + symbol + ": " + response.getErrorMessage());
                            fallbackToLocalData(symbol);
                            return;
                        }

                        if (response.getSymbol() == null) {
                            Log.e(TAG, "Invalid symbol (null) for: " + symbol);
                            fallbackToLocalData(symbol);
                            return;
                        }

                        // 🔥 Lấy dữ liệu an toàn
                        double price = response.getPrice();
                        double change = response.getChange();
                        double changePct = response.getChangePercent();

                        // Nếu API trả về 0 (trường hợp lỗi ngầm)
                        if (price == 0) {
                            Log.e(TAG, "Price = 0 → fallback JSON for: " + symbol);
                            fallbackToLocalData(symbol);
                            return;
                        }

                        // 🔥 Tạo item mới
                        StockItem newItem = new StockItem(
                                response.getSymbol(),
                                null,
                                price,
                                change,
                                changePct,
                                "USD",
                                null
                        );

                        updateList(newItem);

                    })
                    .onFailure(error -> {
                        // 🔥 Bắt lỗi mạng / timeout
                        Log.e(TAG, "API call failed for: " + symbol, error);
                        fallbackToLocalData(symbol);
                    })
                    .fetch();

        } catch (Exception e) {
            // 🔥 Lỗi từ library
            Log.e(TAG, "Library crash for symbol: " + symbol, e);
            fallbackToLocalData(symbol);
        }
    }

    private void fallbackToLocalData(String symbol) {
        if (getContext() == null) return;
        try {
            InputStream is = requireContext().getAssets().open("StockData.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getString("symbol").equalsIgnoreCase(symbol)) {
                    StockItem fallbackItem = new StockItem(
                            obj.getString("symbol"),
                            obj.getString("fullName"),
                            obj.getDouble("price"),
                            obj.getDouble("change"),
                            obj.getDouble("changePct"),
                            obj.getString("currency"),
                            null
                    );
                    updateList(fallbackItem);
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fallback for " + symbol, e);
        }
    }

    private void updateList(StockItem newItem) {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(() -> {
            for (int i = 0; i < displayList.size(); i++) {
                if (displayList.get(i).symbol.equalsIgnoreCase(newItem.symbol)) {
                    StockItem current = displayList.get(i);
                    current.price = newItem.price;
                    current.change = newItem.change;
                    current.changePct = newItem.changePct;
                    current.isPositive = newItem.change >= 0;
                    if (newItem.fullName != null) {
                        current.fullName = newItem.fullName;
                    }
                    adapter.notifyItemChanged(i);
                    return;
                }
            }
        });
    }

    private List<Float> generateDummyChart(double basePrice, boolean trendingUp, int pointsCount) {
        List<Float> points = new ArrayList<>();
        double currentVal = basePrice;
        for (int i = 0; i < pointsCount; i++) {
            double noise = (random.nextDouble() - 0.5) * basePrice * 0.005;
            double trend = trendingUp ? basePrice * 0.001 : -basePrice * 0.001;
            currentVal += noise + trend;
            points.add((float) currentVal);
        }
        return points;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        apiHandler.removeCallbacksAndMessages(null);
    }
}
