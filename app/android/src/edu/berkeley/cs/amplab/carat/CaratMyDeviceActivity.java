package edu.berkeley.cs.amplab.carat;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cs.amplab.carat.lists.ProcessInfoAdapter;
import edu.berkeley.cs.amplab.carat.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.thrift.DetailScreenReport;
import edu.berkeley.cs.amplab.carat.thrift.HogsBugs;
import edu.berkeley.cs.amplab.carat.thrift.Reports;
import edu.berkeley.cs.amplab.carat.ui.BaseVFActivity;
import edu.berkeley.cs.amplab.carat.ui.DrawView;
import edu.berkeley.cs.amplab.carat.ui.FlipperBackListener;
import edu.berkeley.cs.amplab.carat.ui.SwipeListener;
import edu.berkeley.cs.amplab.carat.ui.UiRefreshThread;
import edu.berkeley.cs.amplab.carat.ui.DrawView.Type;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 
 * @author Eemil Lagerspetz
 * 
 */
public class CaratMyDeviceActivity extends BaseVFActivity {

    private CaratApplication app = null;
    
    private DrawView osView = null;
    private DrawView modelView = null;
    private DrawView appsView = null;
    

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mydevice);
        app = (CaratApplication) this.getApplication();

        vf = (ViewFlipper) findViewById(R.id.viewFlipper);
        View baseView = findViewById(R.id.scrollView1);
        baseView.setOnTouchListener(SwipeListener.instance);
        baseViewIndex = vf.indexOfChild(baseView);
        initJscoreView();
        initMemoryView();
        initProcessListView();
        initOsView();
        initModelView();
        initAppsView();
        setModelAndVersion();
        
        Object o = getLastNonConfigurationInstance();
        if (o != null){
            CaratMyDeviceActivity previous = (CaratMyDeviceActivity) o;
            List<DrawView> views = new ArrayList<DrawView>();
            views.add(previous.osView);
            views.add(previous.modelView);
            views.add(previous.appsView);
            for (DrawView v : views) {
                List<Double> xVals = v.getXVals();
                List<Double> yVals = v.getYVals();
                Type t = v.getType();
                List<Double> xValsWithout = v.getXValsWithout();
                List<Double> yValsWithout = v.getYValsWithout();
                String appName = v.getAppName();
                if (v == previous.osView){
                osView.setParams(t, appName, xVals, yVals, xValsWithout,
                        yValsWithout);
                osView.postInvalidate();
                }else if (v == previous.modelView){
                    modelView.setParams(t, appName, xVals, yVals, xValsWithout,
                            yValsWithout);
                    modelView.postInvalidate();
                }else if (v == previous.appsView){
                    appsView.setParams(t, appName, xVals, yVals, xValsWithout,
                            yValsWithout);
                    appsView.postInvalidate();
                }
            }
        }

        
        if (viewIndex == 0)
            vf.setDisplayedChild(baseViewIndex);
        else
            vf.setDisplayedChild(viewIndex);
    }

    private void initJscoreView() {
        WebView webview = (WebView) findViewById(R.id.jscoreView);
        // Fixes the white flash when showing the page for the first time.
        if (getString(R.string.blackBackground).equals("true"))
            webview.setBackgroundColor(0);
        /*
         * 
         * 
         * webview.getSettings().setJavaScriptEnabled(true);
         */
        /*
         * To display the amplab_logo, we need to have it stored in assets as
         * well. If we don't want to do that, the loadConvoluted method below
         * avoids it.
         */
        webview.loadUrl("file:///android_asset/jscoreinfo.html");
        webview.setOnTouchListener(new FlipperBackListener(this, vf, vf
                .indexOfChild(findViewById(R.id.scrollView1))));
    }
    
    private void initMemoryView() {
        WebView webview = (WebView) findViewById(R.id.memoryView);
        // Fixes the white flash when showing the page for the first time.
        if (getString(R.string.blackBackground).equals("true"))
            webview.setBackgroundColor(0);
        /*
         * 
         * 
         * webview.getSettings().setJavaScriptEnabled(true);
         */
        /*
         * To display the amplab_logo, we need to have it stored in assets as
         * well. If we don't want to do that, the loadConvoluted method below
         * avoids it.
         */
        webview.loadUrl("file:///android_asset/memoryinfo.html");
        webview.setOnTouchListener(new FlipperBackListener(this, vf, vf
                .indexOfChild(findViewById(R.id.scrollView1))));
    }

    private void initProcessListView() {
        final ListView lv = (ListView) findViewById(R.id.processList);
        lv.setCacheColorHint(0);
        // Ignore clicks here.
        /*
         * lv.setOnItemClickListener(new OnItemClickListener() {
         * 
         * @Override public void onItemClick(AdapterView<?> a, View v, int
         * position, long id) { Object o = lv.getItemAtPosition(position);
         * RunningAppProcessInfo fullObject = (RunningAppProcessInfo) o;
         * Toast.makeText(CaratMyDeviceActivity.this, "You have chosen: " + " "
         * + fullObject.processName, Toast.LENGTH_LONG).show(); } });
         */
        List<RunningAppProcessInfo> searchResults = SamplingLibrary
                .getRunningProcessInfo(getApplicationContext());
        lv.setAdapter(new ProcessInfoAdapter(this, searchResults, app));
        lv.setOnTouchListener(new FlipperBackListener(this, vf, vf
                .indexOfChild(findViewById(R.id.scrollView1))));
    }
    
    private DrawView construct(){
        DrawView w = new DrawView(getApplicationContext());
        vf.addView(w);
        w.setOnTouchListener(new FlipperBackListener(this, vf, baseViewIndex, true));
        return w;
    }
    
    private void initOsView(){
        osView = construct();
    }
    
    private void initModelView(){
        modelView = construct();
    }
    
    private void initAppsView(){
        appsView = construct();
    }

    /**
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        CaratApplication.setMyDevice(this);
        UiRefreshThread.setReportData();
        new Thread() {
            public void run() {
                synchronized (UiRefreshThread.getInstance()) {
                    UiRefreshThread.getInstance().appResumed();
                }
            }
        }.start();

        setMemory();
        super.onResume();
    }
    
    
    /**
     * Called when OS additional info button is clicked.
     * 
     * @param v
     *            The source of the click.
     */
    public void showOsInfo(View v) {
        Reports r = app.s.getReports();
        if (r != null){
            DetailScreenReport os = r.getOs();
            DetailScreenReport osWithout = r.getOsWithout();
            osView.setParams(Type.OS, SamplingLibrary.getOsVersion(), os.getXVals(), os.getYVals(), osWithout.getXVals(), osWithout.getYVals());
        }
        switchView(osView);
    }
    
    /**
     * Called when Device additional info button is clicked.
     * 
     * @param v
     *            The source of the click.
     */
    public void showDeviceInfo(View v) {
        Reports r = app.s.getReports();
        if (r != null){
            DetailScreenReport model = r.getModel();
            DetailScreenReport modelWithout = r.getModelWithout();
            modelView.setParams(Type.MODEL, SamplingLibrary.getModel(), model.getXVals(), model.getYVals(), modelWithout.getXVals(), modelWithout.getYVals());
        }
        switchView(modelView);
    }
    
    /**
     * Called when App list additional info button is clicked.
     * 
     * @param v
     *            The source of the click.
     */
    public void showAppInfo(View v) {
        Reports r = app.s.getReports();
        if (r != null){
            DetailScreenReport similar = r.getSimilarApps();
            DetailScreenReport similarWithout = r.getSimilarAppsWithout();
            appsView.setParams(Type.SIMILAR, SamplingLibrary.getModel(), similar.getXVals(), similar.getYVals(), similarWithout.getXVals(), similarWithout.getYVals());
        }
        switchView(appsView);
    }

    
    /**
     * Called when Memory additional info button is clicked.
     * 
     * @param v
     *            The source of the click.
     */
    public void showMemoryInfo(View v) {
        switchView(R.id.memoryView);
    }
    
    /**
     * Called when J-Score additional info button is clicked.
     * 
     * @param v
     *            The source of the click.
     */
    public void viewJscoreInfo(View v) {
        switchView(R.id.jscoreView);
    }

    /**
     * Called when View Process List is clicked.
     * 
     * @param v
     *            The source of the click.
     */
    public void viewProcessList(View v) {
        // prepare content:
        ListView lv = (ListView) findViewById(R.id.processList);
        List<RunningAppProcessInfo> searchResults = SamplingLibrary
                .getRunningProcessInfo(getApplicationContext());
        lv.setAdapter(new ProcessInfoAdapter(this, searchResults, app));
        // switch views:
        switchView(R.id.processList);
    }

    private void setModelAndVersion() {
        // Device model
        String model = SamplingLibrary.getModel();

        // Android version
        String version = SamplingLibrary.getOsVersion();

        Window win = this.getWindow();
        // The info icon needs to change from dark to light.
        TextView mText = (TextView) win.findViewById(R.id.dev_value);
        mText.setText(model);
        mText = (TextView) win.findViewById(R.id.os_ver_value);
        mText.setText(version);
    }

    private void setMemory() {
        final Window win = this.getWindow();
        // Set memory values to the progress bar.
        ProgressBar mText = (ProgressBar) win.findViewById(R.id.progressBar1);
        int[] totalAndUsed = SamplingLibrary.readMeminfo();
        mText.setMax(totalAndUsed[0] + totalAndUsed[1]);
        mText.setProgress(totalAndUsed[0]);
        mText = (ProgressBar) win.findViewById(R.id.progressBar2);

        if (totalAndUsed.length > 2) {
            mText.setMax(totalAndUsed[2] + totalAndUsed[3]);
            mText.setProgress(totalAndUsed[2]);
        }

        runOnUiThread(new Runnable() {
            public void run() {
                final double cpu = SamplingLibrary.readUsage();
                /* CPU usage */
                ProgressBar mText = (ProgressBar) win.findViewById(R.id.cpubar);
                mText.setMax(100);
                mText.setProgress((int) (cpu * 100));
            }
        });
    }
}