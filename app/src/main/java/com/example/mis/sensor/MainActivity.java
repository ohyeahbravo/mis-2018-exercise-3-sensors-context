package com.example.mis.sensor;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
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

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    TextView testView;

    private SensorManager sensorManager;
    Sensor accelerometer;

    LineChart chart, fftChart;
    LineData lineData, fftData;
    LineDataSet xSet, ySet, zSet, mSet, fSet;
    float labelCount = -1.0f;    // refresh the graph after 15 datasets;

    // fft variables
    private double[] magnitudes;
    private double[] freqCounts;
    int fftCount = -1;

    // seekbar variables
    int samplerate = 50000;
    int winsize = 64;
    boolean winsizeChanged = false;

    // location manager
    LocationManager locationManager;
    LocationListener locationListener;
    float speed = 0.0f;

    // music player
    MediaPlayer joggingPlayer;
    MediaPlayer bikingPlayer;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testView = (TextView) findViewById(R.id.test);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            // accelerometer available
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(MainActivity.this, accelerometer, samplerate);
        } else {
            // accelerometer not available
            Toast.makeText(getApplicationContext(), "No Accelerometer", Toast.LENGTH_SHORT).show();
        }

        // adding the first chart
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

        // adding the fft chart
        fftChart = (LineChart) findViewById(R.id.fftchart);
        fftChart.setBackgroundColor(Color.LTGRAY);
        XAxis xAxisfft = fftChart.getXAxis();
        xAxisfft.setDrawGridLines(false);
        xAxisfft.setDrawAxisLine(false);
        xAxisfft.setDrawLabels(false);
        YAxis leftfft = fftChart.getAxisLeft();
        leftfft.setDrawLabels(false);
        leftfft.setDrawAxisLine(false);
        leftfft.setDrawGridLines(false);
        leftfft.setDrawZeroLine(true);
        YAxis rightfft = fftChart.getAxisRight();
        rightfft.setEnabled(false);
        fftChart.getDescription().setText("");
        fSet = new LineDataSet(new ArrayList<Entry>(), "fft");
        fSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        List<ILineDataSet> fftSets = new ArrayList<ILineDataSet>();
        fftSets.add(fSet);
        fftData = new LineData(fftSets);
        fftData.setDrawValues(false);
        fSet.setCircleRadius(1.5f);
        fSet.setDrawCircleHole(false);
        fSet.setColor(Color.MAGENTA);

        // initiate magnitudes array
        magnitudes = new double[winsize];

        // initialize seekbars
        SeekBar rateBar = (SeekBar) findViewById(R.id.ratebar);
        rateBar.setMax(2000000 - 1000); // value ranges from 1000 to 2000000
        rateBar.setProgress(samplerate);    // default value
        rateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                samplerate = progress + 1000;
                sensorManager.unregisterListener(MainActivity.this);
                sensorManager.registerListener(MainActivity.this, accelerometer, samplerate);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch (SeekBar seekBar) {
            }
        });

        SeekBar wsizeBar = (SeekBar) findViewById(R.id.wsizebar);
        wsizeBar.setMax(6-2);   // exponent value ranges from 2 to 6
        wsizeBar.setProgress(2);
        wsizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            int changedValue;
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                changedValue = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch (SeekBar seekBar) {
                winsize = (int) Math.pow(2, changedValue + 4);
                fftCount = -1;
                magnitudes = new double[winsize];
            }
        });

        // Initializing Location Manager
        // reference: https://developer.android.com/guide/topics/location/strategies
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                speed = (float) location.getSpeed();
                testView.setText("speed: " + speed);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();
        bikingPlayer.release();
        joggingPlayer.release();
    }

    // initialize the chart
    public void initDatasets(){
        // lists of data
        List<Entry> xList = new ArrayList<Entry>();
        List<Entry> yList = new ArrayList<Entry>();
        List<Entry> zList = new ArrayList<>();
        List<Entry> mList = new ArrayList<>();

        // sets of data
        xSet = new LineDataSet(xList, "x");
        ySet = new LineDataSet(yList, "y");
        zSet = new LineDataSet(zList, "z");
        mSet = new LineDataSet(mList, "magnitude");

        // data shown from the left y axis
        xSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        ySet.setAxisDependency(YAxis.AxisDependency.LEFT);
        zSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        // line colors
        xSet.setColor(Color.RED);
        ySet.setColor(Color.GREEN);
        zSet.setColor(Color.BLUE);
        mSet.setColor(Color.WHITE);

        // add a dataset to datasets
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(xSet);
        dataSets.add(ySet);
        dataSets.add(zSet);
        dataSets.add(mSet);

        // styling the chart
        xSet.setCircleRadius(2.0f);
        xSet.setDrawCircleHole(false);
        ySet.setCircleRadius(2.0f);
        ySet.setDrawCircleHole(false);
        zSet.setCircleRadius(2.0f);
        zSet.setDrawCircleHole(false);
        mSet.setCircleRadius(2.0f);
        mSet.setDrawCircleHole(false);

        // set the chart's data
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

        // update the view
        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();

        // update fft data
        magnitudes[++fftCount] = (double) magnitude;
        if(fftCount == winsize-1) {
            new FFTAsynctask(winsize).execute(magnitudes);
            fftCount = -1;
        }

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
            drawFFTchart(); // visualize the data
        }
    }

    // visualize FFT values with a line chart
    public void drawFFTchart() {

        // refresh the view
        fSet.clear();
        double max = 0;

        // add entries
        for(int i = 0; i < winsize; i++) {
            if(freqCounts[i] > max)
                max = freqCounts[i];
            fSet.addEntry(new Entry(i, (float) freqCounts[i]));
        }

        if(max > 300) {
            playMusic(max);
        } else {
            stopMusic();
        }

        // update the view
        fftData.notifyDataChanged();
        fftChart.setData(fftData);
        fftChart.setVisibleXRangeMaximum(winsize-1);
        fftChart.notifyDataSetChanged();
        fftChart.invalidate();

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

    // reference: https://developer.android.com/guide/topics/media/mediaplayer
    public void playMusic(double max) {

        // when the device is not moving too vigorously, we assume the user is biking.
        if(max < 800) {
            if(bikingPlayer == null)
                bikingPlayer = MediaPlayer.create(getApplicationContext(), R.raw.biking);

            if(!bikingPlayer.isPlaying())
                bikingPlayer = MediaPlayer.create(getApplicationContext(), R.raw.biking);

            if(joggingPlayer != null && joggingPlayer.isPlaying()) {
                joggingPlayer.stop();
            }

            if(!bikingPlayer.isPlaying())
                bikingPlayer.start();
        }

        // when the device is moving vigorously, we assume the user is jogging.
        else{
            if(joggingPlayer == null)
                joggingPlayer = MediaPlayer.create(getApplicationContext(), R.raw.jogging);

            if(!joggingPlayer.isPlaying())
                joggingPlayer = MediaPlayer.create(getApplicationContext(), R.raw.jogging);

            if(bikingPlayer != null && bikingPlayer.isPlaying()) {
                bikingPlayer.stop();
            }

            if(!joggingPlayer.isPlaying())
                joggingPlayer.start();
        }
    }

    // reference: https://developer.android.com/guide/topics/media/mediaplayer
    public void stopMusic() {
        if(bikingPlayer!=null && bikingPlayer.isPlaying()){
            bikingPlayer.stop();
        }
        if(joggingPlayer!=null && joggingPlayer.isPlaying()){
            joggingPlayer.stop();
        }
    }
}
