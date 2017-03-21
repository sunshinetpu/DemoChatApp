package com.sunshinetpu.demochatandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.bassaer.chatmessageview.views.ChatView;


public class ChatActivity extends AppCompatActivity {
    private static final String TAG ="ChatActivity";

    private String contactJid;
    private ChatView mChatView;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mToGroup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_test);
        mChatView =(ChatView) findViewById(R.id.chat_view);
        /*
        mChatView.setEventListener(new ChatViewEventListener() {
            @Override
            public void userIsTyping() {
                //Here you know that the user is typing
            }

            @Override
            public void userHasStoppedTyping() {
                //Here you know that the user has stopped typing.
            }
        });


        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Only send the message if the client is connected
                //to the server.

                if (RoosterConnectionService.getState().equals(RoosterConnection.ConnectionState.CONNECTED)) {
                    Log.d(TAG, "The client is connected to the server,Sendint Message");
                    //Send the message to the server

                    Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
                    intent.putExtra("toGroup",mToGroup);
                    intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,
                            mChatView.getTypedString());
                    intent.putExtra(RoosterConnectionService.BUNDLE_TO, contactJid);

                    sendBroadcast(intent);

                    //Update the chat view.
                    mChatView.sendMessage();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Client not connected to server ,Message not sent!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        Intent intent = getIntent();
        contactJid = intent.getStringExtra("EXTRA_CONTACT_JID");
        setTitle(contactJid);
        mToGroup = false;
        if(contactJid.equals("FSI")) {
            Intent intent2 = new Intent(RoosterConnectionService.JOIN_GROUP);
            sendBroadcast(intent2);
            mToGroup = true;
        }
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action)
                {
                    case RoosterConnectionService.NEW_MESSAGE:
                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
                        String body = intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY);

                        if ( from.equals(contactJid) || mToGroup)
                        {
                           // mChatView.receiveMessage(body);

                        }else
                        {
                            Log.d(TAG,"Got a message from jid :"+from);
                        }

                        return;
                }

            }
        };

        IntentFilter filter = new IntentFilter(RoosterConnectionService.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiver,filter);


    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(RoosterConnectionService.LEAVE_GROUP);
        sendBroadcast(intent);
        super.onDestroy();
    }
}
