package lab.artista.buyhutkejabong;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.widget.ProgressBar;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Disclaimer extends AppCompatActivity {

    private int i=0;
    private Random rand;
    private boolean flag=true;
    private ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);

        rand=new Random();
        pb=(ProgressBar) findViewById(R.id.disclaimer_progress);
        pb.setMax(500);
        t.start();

        SharedPreferences sf = getSharedPreferences("shared_prefs",MODE_PRIVATE);
        long id = sf.getLong("userId",10);
        if (id == 10){
            Date dNow = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("yyMMddhhmmss");
            String datetime = ft.format(dNow);
            id = Long.parseLong(datetime);
            sf.edit().putLong("userId",id).apply();
        }
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(""+id);
        Date d = new Date();
        CharSequence s  = DateFormat.format("MMMM d, yyyy, hh:mm a", d.getTime());
        mDatabase.push().setValue("In : "+s.toString());
    }

    Thread t=new Thread(){
        public void run(){

            while(flag){
                int n=rand.nextInt(25)+1;
                i+=n;
                pb.setProgress(i);
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(i>=500){
                    flag=false;
                    startActivity(new Intent(Disclaimer.this, MainActivity.class));
                    finish();
                }
            }

        }
    };
}
