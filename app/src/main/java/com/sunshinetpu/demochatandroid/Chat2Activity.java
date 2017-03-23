package com.sunshinetpu.demochatandroid;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.bassaer.chatmessageview.models.Message;
import com.github.bassaer.chatmessageview.models.User;
import com.github.bassaer.chatmessageview.views.ChatView;

/**
 * Created by sunshine on 3/15/17.
 */

public class Chat2Activity extends AppCompatActivity {
    private static final String TAG ="ChatActivity";

    private String mContactJid;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mToGroup = false;
    private ChatView mChatView;
    private User mUserMe,mUserYou;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_test);
        mChatView = (ChatView) findViewById(R.id.chat_view);
        /**
         * Here we use User model from ChatView UI library.
         * When develop a production app we have to create our own chat view UI with our custom model.
         */
        //User id
        int myId = 0;
        //User icon
        Bitmap myIcon = BitmapFactory.decodeResource(getResources(), R.drawable.face_2);
        //User name
        String myName = "Minh";

        int yourId = 1;
        Bitmap yourIcon = BitmapFactory.decodeResource(getResources(), R.drawable.face_1);
        String yourName = "Friend";

        mUserMe = new User(myId, myName, myIcon);
        mUserYou = new User(yourId, yourName, yourIcon);


        mChatView.setOnClickSendButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Only send the message if the client is connected
                //to the server.

                if (RoosterConnectionService.getState().equals(RoosterConnection.ConnectionState.CONNECTED)) {
                    Log.d(TAG, "The client is connected to the server,Sendint Message");
                    //Send the message to the server

                    Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
                    intent.putExtra("toGroup",mToGroup);
                    intent.putExtra(RoosterConnectionService.BUNDLE_TYPE,RoosterConnectionService.MESSAGE_TYPE_TEXT);
                    intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,
                            mChatView.getInputText());
                    intent.putExtra(RoosterConnectionService.BUNDLE_TO, mContactJid);

                    sendBroadcast(intent);

                    //Update the chat view.
                    Message message = new Message.Builder()
                            .setUser(mUserMe)
                            .setRightMessage(true)
                            .setMessageText(mChatView.getInputText())
                            .hideIcon(true)
                            .build();
                    mChatView.send(message);
                    mChatView.setInputText("");
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Client not connected to server ,Message not sent!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        mChatView.setOnClickOptionButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(Intent.ACTION_OPEN_DOCUMENT, null);
                intent2.setType("*/*");
                String[] extraMimeTypes = {"image/*", "video/*"};
                intent2.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
                intent2.addCategory(Intent.CATEGORY_OPENABLE);

                startActivityForResult(intent2, 1234);
            }
        });

        Intent intent = getIntent();
        mContactJid = intent.getStringExtra("EXTRA_CONTACT_JID");
        setTitle(mContactJid);
        mToGroup = false;

        //For testing group chat
        if(mContactJid.equals("FSI")) {
            Intent intent2 = new Intent(RoosterConnectionService.JOIN_GROUP);
            sendBroadcast(intent2);
            mToGroup = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1234){
            if(data != null){
                Uri originalUri = data.getData();

                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                getContentResolver().takePersistableUriPermission(
                        originalUri, takeFlags);
                String path =  UriHelper.getPath(this,originalUri).getString(UriHelper.KEY_EXTRA_PATH);

                boolean isImage = UriHelper.isImageFile(path);
                //Selected image will be displayed immediately in chatview. Video currently is not shown. Just show messaging about state of transfer.
                if(isImage) {
                    Message message = new Message.Builder()
                            .setUser(mUserMe)
                            .setRightMessage(true)
                            .setType(Message.Type.PICTURE)
                            .setPicture(BitmapFactory.decodeFile(path))
                            .hideIcon(true)
                            .build();
                    mChatView.send(message);
                }else{
                    addInfoMessage("Sending a video");
                    Toast.makeText(this,"Sending a video",Toast.LENGTH_LONG).show();
                }
                Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
                intent.putExtra("toGroup",mToGroup);
                intent.putExtra(RoosterConnectionService.BUNDLE_TYPE,RoosterConnectionService.MESSAGE_TYPE_FILE);
                intent.putExtra(RoosterConnectionService.BUNDLE_FILE_PATH,
                        path);
                intent.putExtra(RoosterConnectionService.BUNDLE_TO, mContactJid);

                sendBroadcast(intent);
            }
        }
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
                        Log.i("test","receive new message");
                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
                        String type = intent.getStringExtra(RoosterConnectionService.BUNDLE_TYPE);
                        String time = intent.getStringExtra("time");
                        Log.i("test","time is " + time);
                        if(type.equals(RoosterConnectionService.MESSAGE_TYPE_TEXT)) {
                            String body = intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY);

                            if (from.equals(mContactJid) || mToGroup) {
                                // mChatView.receiveMessage(body);
                                Message receivedMessage = new Message.Builder()
                                        .setUser(mUserYou)
                                        .setRightMessage(false)
                                        .setMessageText(body)
                                        .build();
                                mChatView.receive(receivedMessage);

                            } else {
                                Log.d(TAG, "Got a message from jid :" + from);
                            }
                        }else{
                            String path = intent.getStringExtra(RoosterConnectionService.BUNDLE_FILE_PATH);
                            boolean isImage = UriHelper.isImageFile(path);
                            //Received image will be dislayed immediately.
                            if(isImage) {
                                Message receivedMessage = new Message.Builder()
                                        .setUser(mUserMe)
                                        .setRightMessage(false)
                                        .setType(Message.Type.PICTURE)
                                        .setPicture(BitmapFactory.decodeFile(path))
                                        .build();
                                mChatView.receive(receivedMessage);
                            }else{
                                Toast.makeText(getApplicationContext(),"Received a video",Toast.LENGTH_LONG).show();
                                addInfoMessage("File is saved");
                            }

                        }

                        return;
                    case RoosterConnectionService.NEW_FILE:
                        boolean isImage = intent.getBooleanExtra("isImage",true);
                        if(isImage){
                            addInfoMessage("Receiving a picture");
                        }else {
                            long size = intent.getLongExtra("size", 0);
                            String name = intent.getStringExtra("name");
                            showDialogReceiveFile(size, name);
                        }
                        break;
                    case RoosterConnectionService.FILE_TRANSFER_RESULT:
                        boolean success = intent.getBooleanExtra(RoosterConnectionService.BUNDLE_SUCCESS,false);
                        if(success){
                            addInfoMessage("File is sent successfully");
                        }else{
                            addInfoMessage("File is not sent");
                        }
                }

            }
        };

        IntentFilter filter = new IntentFilter(RoosterConnectionService.NEW_MESSAGE);
        filter.addAction(RoosterConnectionService.NEW_FILE);
        filter.addAction(RoosterConnectionService.FILE_TRANSFER_RESULT);
        registerReceiver(mBroadcastReceiver,filter);


    }

    private void showDialogReceiveFile(long size, String name){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Receive new file");
        String msg = "New file: " + name + System.getProperty("line.separator") + "Size: " + size
                + System.getProperty("line.separator") + "Do you want to save it?";
        builder.setMessage(msg);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(RoosterConnectionService.RECEIVE_FILE);
                sendBroadcast(intent);
                addInfoMessage("Receiving file");
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(RoosterConnectionService.CANCEL_FILE);
                sendBroadcast(intent);
                addInfoMessage("File is ignored");
            }
        });
        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void addInfoMessage(String msg){
        Message infoMessage = new Message.Builder()
                .setUser(mUserYou)
                .hideIcon(true)
                .setUsernameVisibility(false)
                .setRightMessage(true)
                .setType(Message.Type.TEXT)
                .setMessageText(msg)
                .build();
        mChatView.send(infoMessage);

    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(RoosterConnectionService.LEAVE_GROUP);
        sendBroadcast(intent);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }
}
