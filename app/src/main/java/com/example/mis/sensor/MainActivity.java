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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    Sensor accelerometer;

    TextView xView, yView, zView, magnitudeView;
    LineChart chart;

    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xView = (TextView) findViewById(R.id.first);
        yView = (TextView) findViewById(R.id.second);
        zView = (TextView) findViewById(R.id.third);
        magnitudeView = (TextView) findViewById(R.id.fourth);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // accelerometer available
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // accelerometer not available
            Toast.makeText(getApplicationContext(), "No Accelerometer", Toast.LENGTH_SHORT).show();
        }

        //adding chart
        chart = (LineChart) findViewById(R.id.chart);

        //add data (temporary)
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry(0.0f, 5.0f));
        entries.add(new Entry(1.0f, 3.0f));
        entries.add(new Entry(2.0f, 7.0f));
        entries.add(new Entry(3.0f, 2.0f));
        LineDataSet dataSet = new LineDataSet(entries, "label");
        dataSet.setColor(Color.RED);
        dataSet.setValueTextColor(Color.BLUE);
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();


        //initiate and fill example array with random values
        rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues);
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

        xView.setText("x: " + xyz[0]);
        yView.setText("y: " + xyz[1]);
        zView.setText("z: " + xyz[2]);
        magnitudeView.setText("magnitude: " + magnitude);
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
