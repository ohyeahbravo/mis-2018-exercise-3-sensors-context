package com.example.mis.sensor;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mis.sensor.FFT;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    Sensor accelerometer;

    LineChart chart;
    LineData lineData;
    LineDataSet xSet, ySet, zSet, mSet;
    float labelCount = -1.0f;    // refresh the graph after 15 datasets;

    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            // accelerometer available
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(MainActivity.this, accelerometer, 1000000);
        } else {
            // accelerometer not available
            Toast.makeText(getApplicationContext(), "No Accelerometer", Toast.LENGTH_SHORT).show();
        }

        // adding chart
        chart = (LineChart) findViewById(R.id.chart);
        chart.setBackgroundColor(Color.LTGRAY);
        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMaximum(15.0f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawLabels(false);
        YAxis left = chart.getAxisLeft();
        left.setDrawLabels(false);
        left.setDrawAxisLine(false);
        left.setDrawGridLines(false);
        left.setDrawZeroLine(true);
        YAxis right = chart.getAxisRight();
        right.setEnabled(false);
        chart.getDescription().setText("");

        // initializing datasets
        initDatasets();

        //initiate and fill example array with random values
        rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues);
    }

    public void initDatasets(){
        List<Entry> xList = new ArrayList<Entry>();
        List<Entry> yList = new ArrayList<Entry>();
        List<Entry> zList = new ArrayList<>();
        List<Entry> mList = new ArrayList<>();

        xSet = new LineDataSet(xList, "x");
        ySet = new LineDataSet(yList, "y");
        zSet = new LineDataSet(zList, "z");
        mSet = new LineDataSet(mList, "magnitude");

        xSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        ySet.setAxisDependency(YAxis.AxisDependency.LEFT);
        zSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        xSet.setColor(Color.RED);
        ySet.setColor(Color.GREEN);
        zSet.setColor(Color.BLUE);
        mSet.setColor(Color.WHITE);

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(xSet);
        dataSets.add(ySet);
        dataSets.add(zSet);
        dataSets.add(mSet);

        lineData = new LineData(dataSets);
        lineData.setDrawValues(false);
        chart.setData(lineData);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i){

    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent){

        // vars
        float magnitude = 0.0f;
        float[] xyz = new float[3];
        xyz[0] = sensorEvent.values[0];
        xyz[1] = sensorEvent.values[1];
        xyz[2] = sensorEvent.values[2];

        // The Magnitude of a Vector V ; v2 = x2 + y2 + z2
        // reference: http://members.tripod.com/~Paul_Kirby/vector/VLintro.html
        magnitude = (float)Math.sqrt(xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2]);
        magnitude = Math.abs(magnitude);

        // add data to the chart
        if(labelCount == 15.0f) {
            // refresh the view
            labelCount = 0.0f;
            xSet.clear();
            ySet.clear();
            zSet.clear();
            mSet.clear();
            chart.invalidate();
            chart.moveViewToX(0.0f);
        } else {
            labelCount += 1.0f;
        }
        xSet.addEntry(new Entry(labelCount, xyz[0]));
        ySet.addEntry(new Entry(labelCount, xyz[1]));
        zSet.addEntry(new Entry(labelCount, xyz[2]));
        mSet.addEntry(new Entry(labelCount, magnitude));

        // refresh the chart
        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */

    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
        }
    }




    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }



}
