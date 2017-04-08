package lab.artista.buyhutkejabong;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ExpandedMenuView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView wv;
    private ProgressBar pb;
    private RelativeLayout applyCoupon;
    private String[] COUPONS;
    private boolean flag = false;
    private int counter;
    private boolean insideCoupon;
    private String finalCoupon;
    private double finalDiscount;
    private TextView tv_coupon;
    private ProgressBar banner_pb;
    private TextView tv_banner_done, tv_banner_saved, tv_pickMe, tv_banner_show, tv_banner_applied_coupon, tv_show_you_saved;
    private Dialog dialog, pickMe;
    private boolean startTrack = false;
    private ProgressDialog progressDialog;
    private ImageButton ib_remove_banner, ib_remove_applyCoupon;
    private List<String> browse_history;
    private boolean retrievePrice;
    private double ACTUAL_PRICE;
    private boolean couponApplied;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        wv = (WebView) findViewById(R.id.webview);
        pb = (ProgressBar) findViewById(R.id.progressBar);
        applyCoupon = (RelativeLayout) findViewById(R.id.applyCoupon);
        tv_coupon = (TextView) findViewById(R.id.tv_coupon);
        tv_pickMe = (TextView) findViewById(R.id.pickMe);
        ib_remove_applyCoupon = (ImageButton) findViewById(R.id.iv_remove_applyCoupon);
        progressDialog = new ProgressDialog(this);
        browse_history = new ArrayList<>();
        pb.setMax(100);

        wv.setWebViewClient(new MyWebClient());
        wv.getSettings().setLoadsImagesAutomatically(true);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        wv.addJavascriptInterface(new MyInterface(),"MyTag");
        wv.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                pb.setProgress(newProgress);
            }
        });

        wv.loadUrl("http://m.jabong.com/");

        pb.setVisibility(View.VISIBLE);
        applyCoupon.setVisibility(View.INVISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tv_pickMe.setVisibility(View.GONE);
            }
        },10*1000);

        tv_pickMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_pickMe.setVisibility(View.GONE);
                String myUrl = getSharedPreferences("savedWebPage",MODE_PRIVATE).getString("url","");
                if(TextUtils.isEmpty(myUrl))
                    myUrl = "http://m.jabong.com/";
                wv.loadUrl(myUrl);
            }
        });

        applyCoupon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                counter = 0;
                finalDiscount = 1000000.0;
                couponApplied = false;
                new ParseHTML().execute("http://coupons.buyhatke.com/PickCoupon/FreshCoupon/getCoupons.php?pos=1");
                progressDialog.setTitle("Please Wait!");
                progressDialog.setMessage("Getting Coupons...");
                progressDialog.show();

            }
        });

        ib_remove_applyCoupon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //applyCoupon.setVisibility(View.INVISIBLE);
                fadeOut(applyCoupon);
            }
        });

    }

    private void createBanner() {

        dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.banner);
        tv_banner_done = (TextView) dialog.findViewById(R.id.tv_banner_done);
        tv_banner_saved = (TextView) dialog.findViewById(R.id.tv_banner_saved);
        banner_pb = (ProgressBar) dialog.findViewById(R.id.pb_banner_progress);
        tv_banner_show = (TextView) dialog.findViewById(R.id.tv_banner_show);
        ib_remove_banner = (ImageButton) dialog.findViewById(R.id.ib_remove_banner);
        tv_banner_applied_coupon = (TextView) dialog.findViewById(R.id.tv_banner_applied_coupon);
        tv_show_you_saved = (TextView) dialog.findViewById(R.id.tv_show_you_saved);
        banner_pb.setMax(COUPONS.length-1);
        banner_pb.setProgress(1);
        tv_banner_saved.setText("Rs 0.00");
        tv_banner_done.setText("1/"+(COUPONS.length-1));
        dialog.setCancelable(false);

        ib_remove_banner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                flag = false;
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Search Cancelled!", Toast.LENGTH_SHORT).show();
                applyCoupon.setVisibility(View.VISIBLE);
                fadeIn(applyCoupon);

            }
        });
        //dialog.show();

    }

    private class MyWebClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            view.loadUrl(url);
            return true;

        }


        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //super.onPageStarted(view, url, favicon);

            insideCoupon = url.contains("coupon");

            if (startTrack)
                getSharedPreferences("savedWebPage",MODE_PRIVATE).edit().putString("url",url).apply();

            pb.setProgress(0);
            pb.setVisibility(View.VISIBLE);

            if (url.contains("cart")){
                applyCoupon.setVisibility(View.VISIBLE);
                fadeIn(applyCoupon);
            } else {
                applyCoupon.setVisibility(View.INVISIBLE);
            }

            if (!flag)
                browse_history.add(url);

            startTrack = true;

        }

        @Override
        public void onPageFinished(WebView view, final String url) {
            super.onPageFinished(view, url);
            pb.setVisibility(View.GONE);


            if (flag){

                if (counter < COUPONS.length){

                    if (insideCoupon){

                        wv.loadUrl("javascript:var v = document.getElementById('applyCoupon').value='"+COUPONS[counter]+"'");
                        wv.loadUrl("javascript:document.getElementsByClassName('input-group-addon jbApplyCoupon')[0].click()");

                        if (counter == 0){
                            tv_banner_show.setText("APPLYING COUPONS");
                        }

                        if (counter < COUPONS.length-1){
                            tv_banner_done.setText(counter+1+"/"+(COUPONS.length-1));
                            banner_pb.setProgress(counter+1);
                        }

                        new Handler().postDelayed(new Runnable() {

                            @Override
                            public void run() {

                                counter++;
                                if (insideCoupon){

                                    onPageFinished(wv, url);

                                } else {

                                    //Toast.makeText(MainActivity.this, "Not Inside", Toast.LENGTH_SHORT).show();
                                }
                            }

                        }, 5*1000 );

                    } else {

                        wv.loadUrl("javascript:window.MyTag.parseData(document.getElementsByClassName('rupee')[0].innerHTML)");
                        couponApplied = true;
                        //wv.loadUrl("http://m.jabong.com/cart/coupon/");

                    }

                }

                if (counter == COUPONS.length) {

                    banner_pb.setVisibility(View.GONE);
                    if(finalDiscount == 0){
                        tv_banner_done.setText("SORRY! No coupon available.\nPlease try again Later.");
                        tv_banner_show.setVisibility(View.GONE);
                        tv_banner_saved.setVisibility(View.GONE);
                        tv_show_you_saved.setVisibility(View.GONE);
                        tv_banner_applied_coupon.setText("None");
                        tv_banner_done.setTextSize(18);
                    } else {
                        tv_banner_show.setText("CONGRATS!!");
                        float temp_price = Float.parseFloat(tv_banner_saved.getText().toString().substring(3));
                        tv_banner_done.setText("You Saved : "+ String.format("%.2f", (ACTUAL_PRICE - temp_price) ));
                        tv_banner_applied_coupon.setText(finalCoupon);
                        tv_banner_show.setTextSize(22);

                    }

                    flag = false;
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {

                            if (couponApplied){
                                wv.loadUrl("javascript:var v = document.getElementById('applyCoupon').value='"+finalCoupon+"'");
                                wv.loadUrl("javascript:document.getElementsByClassName('input-group-addon jbApplyCoupon')[0].click()");
                            } else {
                                wv.loadUrl("http://m.jabong.com/cart/");
                            }
                            dialog.dismiss();
                            applyCoupon.setVisibility(View.INVISIBLE);

                        }

                    }, 5*1000 );

                }

            }

            if (retrievePrice){

                wv.loadUrl("javascript:window.MyTag.parseData(document.getElementsByClassName('rupee')[0].innerHTML)");
                //retrievePrice = false;

            }

        }
    }

    public void fadeIn(View view){

        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(view, "alpha", 0.0F, 1.0F);
        alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaAnimation.setDuration(1500);
        alphaAnimation.start();

    }

    public void fadeOut(View view){

        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(view, "alpha", 1.0F, 0.0F);
        alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaAnimation.setDuration(1500);
        alphaAnimation.start();

    }

    public class MyInterface{

        @JavascriptInterface
        public void parseData(String str){

            final double temp = Double.parseDouble(str);
            if (retrievePrice){

                flag = true;
                retrievePrice = false;
                ACTUAL_PRICE = temp;
                finalDiscount = 0;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_show_you_saved.setText("Initial Price : ");
                        tv_banner_saved.setText("Rs "+String.format("%.2f",temp));
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        if (!dialog.isShowing()) {
                            dialog.show();
                            tv_banner_show.setText("Make Sure Bag is not empty!");
                        }

                    }
                });

            }

            if ( ACTUAL_PRICE != temp && ((ACTUAL_PRICE - temp) > finalDiscount )){

                finalDiscount = ACTUAL_PRICE - temp;
                if (counter> 0) finalCoupon = COUPONS[counter-1];

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_show_you_saved.setText("Reduced Price : ");
                        tv_banner_saved.setText("Rs "+String.format("%.2f",temp));
                        if (finalCoupon != null) tv_banner_applied_coupon.setText(finalCoupon);
                    }
                });

            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    wv.loadUrl("http://m.jabong.com/cart/coupon/");
                }
            });

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_history) {
            createHistoryList();
            return true;
        } else if (id == android.R.id.home){
            wv.loadUrl("http://m.jabong.com/");
            return true;
        } else if (id == R.id.menu_go_forward){
            if (wv.canGoForward())
                wv.goForward();
            else
                Toast.makeText(this, "Can't Go Forward!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_exit){
            finishAffinity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createHistoryList() {

        final Dialog historyDialog = new Dialog(this);
        historyDialog.setContentView(R.layout.browser_history);
        ListView lv = (ListView) historyDialog.findViewById(R.id.listview_history);
        List<String> tempList = new ArrayList<>(browse_history);
        Collections.reverse(tempList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, tempList);
        lv.setAdapter(adapter);
        historyDialog.show();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                wv.loadUrl(browse_history.get(position));
                historyDialog.dismiss();
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (wv.canGoBack())
            wv.goBack();
        else super.onBackPressed();
    }

    private class ParseHTML extends AsyncTask<String, String, String> {

        private ProgressDialog localPd = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog.isShowing()){
                        progressDialog.setMessage("It's taking longer than usual...");
                    }
                }
            },8*1000);

        }

        @Override
        protected String doInBackground(String... params) {

            BufferedReader buff = null;
            try {

                URL url = new URL(params[0]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                StringBuilder sb = new StringBuilder();
                buff = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;

                while ((line = buff.readLine())!=null){

                    sb.append(line);

                }
                return sb.toString();

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (buff!=null){
                    try {
                        buff.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }

        }

        @Override
        protected void onPostExecute(String result) {

            try{
                String[] temp = result.split("~");
                COUPONS = new String[temp.length+1];
                for (int i=0;i<temp.length;i++)
                    COUPONS[i] = temp[i];
                COUPONS[temp.length] = "FLUSH";
                retrievePrice = true;
                wv.loadUrl("http://m.jabong.com/cart/");
                createBanner();
            } catch (Exception e){
                Toast.makeText(MainActivity.this, "Can't connect to the Server.\nPlease try again...", Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        long id = getSharedPreferences("shared_prefs",MODE_PRIVATE).getLong("userId",10);
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(""+id);
        Date d = new Date();
        CharSequence s  = DateFormat.format("MMMM d, yyyy, hh:mm a", d.getTime());
        mDatabase.push().setValue("Out : "+s.toString());
    }
}
