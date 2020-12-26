package com.rearview.timechart;

import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    PieChart pieChart;
    PieData pieData;
    List<PieEntry> pieEntryList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pieChart = findViewById(R.id.pieChart);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setText("WebPages visited last 24 hours.");
        // These values would come from a web proxy https://github.com/SuppSandroB/sandrop/blob/master/projects/SandroProxyLib
        pieEntryList.add(new PieEntry(10,"facebook.com"));
        pieEntryList.add(new PieEntry(5,"gmail.com"));
        pieEntryList.add(new PieEntry(7,"news.google.com"));
        pieEntryList.add(new PieEntry(3,"linkedin.com"));
        PieDataSet pieDataSet = new PieDataSet(pieEntryList,"pages visited");
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }
}