package com.rearview.timechart;

import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    PieChart pieChart;
    PieData pieData;
    List<PieEntry> pieEntryList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectionFragment fragment = new ConnectionFragment();
        Map<Integer, String> topMap = new TreeMap(Collections.reverseOrder());
        if (fragment.getCountMap().size() > 0) {
            for(int i = 0; i < fragment.getCountMap().size(); i++) {
                topMap.put(fragment.getCountMap().valueAt(i), fragment.getCountMap().keyAt(i));
            }
        }
        pieChart = findViewById(R.id.pieChart);
        pieChart.setUsePercentValues(true);
        if (topMap.size() > 0) {
            List<Integer> keys = new ArrayList<>(topMap.keySet());
            List<String> values = new ArrayList<>(topMap.values());
            for (int i = 0; i < 20 && i < topMap.size(); i++) {
                pieEntryList.add(new PieEntry(keys.get(i), values.get(i)));
            }
            PieDataSet pieDataSet = new PieDataSet(pieEntryList,"pages visited");
            pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
            pieChart.getDescription().setText("WebPages visited last 24 hours.");
            pieData = new PieData(pieDataSet);
            pieChart.setData(pieData);
        } else {
            pieEntryList.add(new PieEntry(10, "facebook.com"));
            pieEntryList.add(new PieEntry(5, "gmail.com"));
            pieEntryList.add(new PieEntry(7, "news.google.com"));
            pieEntryList.add(new PieEntry(3, "linkedin.com"));
            PieDataSet pieDataSet = new PieDataSet(pieEntryList,"Sample Chart. Please allow 24 hours to collect data.");
            pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
            pieChart.getDescription().setText("Sample Data. Please allow 24 hours to collect data from your phone.");
            pieData = new PieData(pieDataSet);
            pieChart.setData(pieData);
        }
        pieChart.invalidate();
    }
}