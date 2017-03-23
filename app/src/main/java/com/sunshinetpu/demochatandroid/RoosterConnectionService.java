package com.sunshinetpu.demochatandroid;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

/**
 * Created by gakwaya on 4/28/2016.
 */
public class RoosterConnectionService extends Service {
    private static final String TAG ="RoosterService";

    public static final String UI_AUTHENTICATED = "com.blikoon.rooster.uiauthenticated";
    public static final String SEND_MESSAGE = "com.blikoon.rooster.sendmessage";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_TO = "b_to";
    public static final String BUNDLE_TYPE = "b_type";
    public static final String MESSAGE_TYPE_FILE = "FILE";
    public static final String MESSAGE_TYPE_TEXT = "TEXT";
    public static final String BUNDLE_FILE_PATH = "b_path";

    public static final String NEW_MESSAGE = "com.blikoon.rooster.newmessage";
    public static final String BUNDLE_FROM_JID = "b_from";
    public static final String JOIN_GROUP = "join.group";
    public static final String LEAVE_GROUP = "leave.group";
    public static final String NEW_FILE = "new.file";
    public static final String RECEIVE_FILE = "receive.file";
    public static final String CANCEL_FILE = "cancel.file";
    public static final String FILE_TRANSFER_RESULT = "file.transfer.result";
    public static final String BUNDLE_SUCCESS = "transfer.success";

    public static final String SEND_OFFER = "send.offer";

    public static RoosterConnection.ConnectionState sConnectionState;
    public static RoosterConnection.LoggedInState sLoggedInState;
    private boolean mActive;//Stores whether or not the thread is active
    private Thread mThread;
    private Handler mTHandler;//We use this handler to post messages to
    //the background thread.
    private RoosterConnection mConnection;

    public RoosterConnectionService() {

    }
    public static RoosterConnection.ConnectionState getState()
    {
        if (sConnectionState == null)
        {
            return RoosterConnection.ConnectionState.DISCONNECTED;
        }
        return sConnectionState;
    }

    public static RoosterConnection.LoggedInState getLoggedInState()
    {
        if (sLoggedInState == null)
        {
            return RoosterConnection.LoggedInState.LOGGED_OUT;
        }
        return sLoggedInState;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate()");
    }

    private void initConnection()
    {
        Log.d(TAG,"initConnection()");
        if( mConnection == null)
        {
            mConnection = new RoosterConnection(this,userName,passWord);
        }
        try
        {
            mConnection.connect();

        }catch (IOException |SmackException |XMPPException e)
        {
            Log.d(TAG,"Something went wrong while connecting ,make sure the credentials are right and try again");
            e.printStackTrace();
            //Stop the service all together.
            stopSelf();
        }

    }


    public void start()
    {
        Log.d(TAG," Service Start() function called.");
        if(!mActive)
        {
            mActive = true;
            if( mThread ==null || !mThread.isAlive())
            {
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Looper.prepare();
                        mTHandler = new Handler();
                        initConnection();
                        //THE CODE HERE RUNS IN A BACKGROUND THREAD.
                        Looper.loop();

                    }
                });
                mThread.start();
            }


        }

    }

    public void stop()
    {
        Log.d(TAG,"stop()");
        mActive = false;
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                if( mConnection != null)
                {
                    mConnection.disconnect();
                }
            }
        });

    }

    String userName;
    String passWord;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand()");
        String cmd = intent.getStringExtra("cmd");
        if(cmd.equals("start")) {
            userName = intent.getStringExtra("user");
            passWord = intent.getStringExtra("pwd");
            start();
        }else{
            stopSelf();
        }
        return Service.START_STICKY;
        //RETURNING START_STICKY CAUSES OUR CODE TO STICK AROUND WHEN THE APP ACTIVITY HAS DIED.
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        stop();
    }
}
