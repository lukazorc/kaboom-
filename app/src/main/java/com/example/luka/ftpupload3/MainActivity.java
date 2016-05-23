package com.example.luka.ftpupload3;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Calendar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.TextView;
import java.text.DecimalFormat;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.BarGraphSeries;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private MyFTPClientFunctions ftpclient = null;
    private Context cntx = null;
    public boolean stanje = false;

    //graphview
    private LineGraphSeries<DataPoint> series;
    private BarGraphSeries<DataPoint> series2;
    private int lastX = 0;
    private double o=0;
    private int k=0;
    private int stZapisov = 0;

    private static final Random RANDOM = new Random();
    private static final String TAG1 = MainActivity.class.getName();

    //tukaj bomo pisali samo zadnjo vrednost za jo prikazat v aplikaciji. Ta file je interen aplikaciji, ne mores dostopat od zunaj
    private static final String FILENAME = "mojFile1.txt";

    private static final String TAG2 = MainActivity.class.getSimpleName();
    private PowerManager.WakeLock mWakeLock = null; //to rabi da se ne ustavi aplikacija po 5 min.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //to rabi da se ne ustavi aplikacija po 5 min.
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG2);
        mWakeLock.acquire();

        TextView tv = (TextView) findViewById(R.id.textView);
        TextView tv2 = (TextView) findViewById(R.id.textView4);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        // data
        series = new LineGraphSeries<DataPoint>();
        series2 = new BarGraphSeries<DataPoint>();

        graph.addSeries(series2);
        graph.addSeries(series);

        series2.setDrawValuesOnTop(true);
        series2.setValuesOnTopColor(Color.RED);
        series2.setValuesOnTopSize(12);
        series2.setColor(Color.RED);

        graph.setTitle("Prikaz temperature");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("čas");
        graph.getGridLabelRenderer().setVerticalAxisTitle("temperatura");
        series.setDrawDataPoints(true);
        series.setThickness(3);
        series.setDataPointsRadius(5);

        //series2.appendData(new DataPoint(0, 0),true, 20);
        series.appendData(new DataPoint(0, 0), true, 20);

        // customize a little bit viewport
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(-45);//mrzlo, prikaze na grafu najmanj -40C
        viewport.setMaxY(45); //toplo +40C
        viewport.setXAxisBoundsManual(true);//s tem se stevilke na x osi spreminjajo (x++)
        viewport.setMinX(0);
        viewport.setMaxX(20);
        viewport.setScrollable(true);

        cntx = this.getBaseContext();
        //createDummyFile();
        ftpclient = new MyFTPClientFunctions();

        connectToFTPAddress(); // tukaj se povezemo na FTP server

        new Thread(new Runnable() {

            @Override
            public void run() {

                while (true) {

                    runOnUiThread(new Runnable() { //ta nit lahko pise na prvotno nit: graficni vmestnik

                        @Override
                        public void run() {

                            double c = ((RANDOM.nextDouble() * 2) - 1);
                            double b=c*40;
                            DecimalFormat df = new DecimalFormat("#.##"); //format zapisa

                            //napisemo random vrednost v  2 datoteki
                            writeToFile(df.format(b));

                            //beremo iz datoteke zadnji napisan podatek in ga prikazemo v aplikaciji
                            /*String textFromFileString = readFromFile().substring(16);
                           // double value = Double.parseDouble(textFromFileString);
                            double value = Double.valueOf(textFromFileString);


                            if(k < 4) { //izpisemo zadnjo temp
                                series2.appendData(new DataPoint(lastX++, (value/40) * 40d), true, 20);
                                TextView tv = (TextView) findViewById(R.id.textView);
                                tv.setText(df.format(b) + " \u00b0C");
                                o=o+(value/40);
                                k=k+1;
                            }

                            else { //izpisemo zadnjo temp
                                series2.appendData(new DataPoint(lastX, (value / 40) * 40d), true, 20);
                                TextView tv = (TextView) findViewById(R.id.textView);
                                tv.setText(df.format(b) + " \u00b0C");
                                o = o + (value / 40);
                                k = k + 1;
                                // in izpisemo tudi povprecno temp na zadnjo
                                series.appendData(new DataPoint(lastX++, (o / k) * 40d), true, 20);
                                TextView tv2 = (TextView) findViewById(R.id.textView4);
                                tv2.setText(df.format(40 * o / k) + " \u00b0C");
                                o = 0;
                                k = 0;
                            }*/
                        }

                    });
                    // Vsakih 10 sekund nov podatek
                    try {
                        Thread.sleep(10000); //10sekund
                    } catch (InterruptedException e) {
                        // manage error ...
                    }

                    while ( stanje == false) { //caka da se poveze, da ne bi uploadalo preden je sploh povezano.
                    }
                    UploadD();
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onDestroy() {
        super.onDestroy();
        DisconnectD();
    }
    //Metode za FTP
    //--------------------------------------------------------------------------------------------------
    private void connectToFTPAddress() {

        new Thread(new Runnable() {
            public void run() {
                boolean status = false;
                status = ftpclient.ftpConnect("ftp.cgsplus.si", "cgs", "papuaplus", 21);
                if (status == true) {
                    stanje=true;
                    Log.d(TAG, "Connection Success");
                    //handler.sendEmptyMessage(0);
                } else {
                    Log.d(TAG, "Connection failed");
                    // handler.sendEmptyMessage(-1);
                }
            }
        }).start();
    }

    public void UploadD() {
        new Thread(new Runnable() {
            public void run() {
                String timeStamp2 = new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime());

                boolean status = false;
                status = ftpclient.ftpUpload(
                        Environment.getExternalStorageDirectory()
                                + "/TAGFtp/" + timeStamp2+".txt",
                        timeStamp2+".txt", "/Public/KABOOM/", cntx);
                if (status == true) {
                    stanje=true;
                    Log.d(TAG, "Upload success");
                    //handler.sendEmptyMessage(2);
                } else {
                    Log.d(TAG, "Upload failed");
                    //handler.sendEmptyMessage(-1);
                }
            }
        }).start();
    }
    public void DisconnectD() {
        new Thread(new Runnable() {
            public void run() {
                ftpclient.ftpDisconnect();
            }
        }).start();
    }

    //Metode za graficni prikaz
    //--------------------------------------------------------------------------------------------------
    private void writeToFile(String data) {
        try {
           /* File root = Environment.getExternalStorageDirectory();
            File dir = new File (root.getAbsolutePath() + "/TAGFtp/");
            dir.mkdirs();
            File file = new File(dir, "prova4.txt");*/

            //File sdCard = Environment.getExternalStorageDirectory();
            //File directory = new File (sdCard.getAbsolutePath() + "/MyFiles");
            //directory.mkdirs();

            //Generira datum
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            String timeStamp2 = new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime());
            //V datoteko potatki.txt pisemo izmerjene podatke.
            File outFile = new File(Environment.getExternalStorageDirectory() +"/TAGFtp/" ,timeStamp2+".txt");
            FileWriter fw = new FileWriter(outFile, true);
            fw.append(timeStamp+" ");
            fw.append(data);
            fw.append("\n");
            fw.close();

            //V datoteko FILENAME pišemo zadnji podatek, ki bo prekril prejsnjega. To rabi samo za prikaz v aplikaciji.
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(FILENAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(timeStamp+" ");
            outputStreamWriter.write(data);
            outputStreamWriter.write("\n");
            outputStreamWriter.close();

        }
        catch (IOException e) {
            Log.e(TAG1, "File write failed: " + e.toString());
        }
    }

    private String readFromFile() {

        String ret = "";
        try {
            InputStream inputStream = openFileInput(FILENAME);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                    stringBuilder.append("\n");
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG1, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG1, "Can not read file: " + e.toString());
        }

        return ret;
    }
}
