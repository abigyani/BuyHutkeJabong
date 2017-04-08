package lab.artista.buyhutkejabong;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * Created by Abhishek on 22-Mar-17.
 */

public class BuyHutkeJabong extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
