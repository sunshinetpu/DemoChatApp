package com.sunshinetpu.demochatandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by gakwaya on 4/28/2016.
 */
public class RoosterConnection implements ConnectionListener {
    private static final String DOMAIN = "192.168.6.240";
    private static final String HOST = "192.168.6.240";
    private static final int PORT = 5222;
    private static final String TAG = "RoosterConnection";
    private static final String ROOM_NAME = "fsi";

    private  final Context mApplicationContext;
    private  final String mUsername;
    private  final String mPassword;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;//Receives messages from the ui thread.
    private ChatMessageListener messageListener;
    private MultiUserChatManager multiUserChatManager;
    private FileTransferManager mFileTransferManager;
    private FileTransferRequest mFileTransferRequest;

    public static enum ConnectionState
    {
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED;
    }

    public static enum LoggedInState
    {
        LOGGED_IN , LOGGED_OUT;
    }


    public RoosterConnection( Context context, String useName, String password)
    {
        Log.d(TAG,"RoosterConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        this.mUsername = useName;
        this.mPassword = password;
    }


    public void connect() throws IOException,XMPPException,SmackException
    {
        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
        configBuilder.setUsernameAndPassword(mUsername, mPassword);
        configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        configBuilder.setResource("Android");
        configBuilder.setServiceName(DOMAIN);
        configBuilder.setHost(HOST);
        configBuilder.setPort(PORT);
        //configBuilder.setDebuggerEnabled(true);
        mConnection = new XMPPTCPConnection(configBuilder.build());

        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver();
        mConnection.addConnectionListener(this);
        mConnection.connect();
        mConnection.login();
        messageListener = new ChatMessageListener() {
            @Override
            public void processMessage(Chat chat, Message message) {
                ///ADDED

                        ///ADDED
                Log.i("listen","chat message received");
                    processReceivedMessage(false,message);
            }
        };

        //The snippet below is necessary for the message listener to be attached to our connection.

        ChatManager.getInstanceFor(mConnection).addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {

                //If the line below is missing ,processMessage won't be triggered and you won't receive messages.
                chat.addMessageListener(messageListener);

            }


        });


        final ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

        //for controlling group chat
        multiUserChatManager = MultiUserChatManager.getInstanceFor(mConnection);

        //for controlling file transfer.
        mFileTransferManager = FileTransferManager.getInstanceFor(mConnection);
        mFileTransferManager.addFileTransferListener(new FileTransferListener() {
            @Override
            public void fileTransferRequest(final FileTransferRequest request) {
                Log.i("test","receive file " + request.getFileName());
                boolean isImage = UriHelper.isImageFile(request.getFileName());
                Intent intent = new Intent(RoosterConnectionService.NEW_FILE);
                intent.putExtra("size",request.getFileSize());
                intent.putExtra("name", request.getFileName());
                intent.putExtra("isImage",isImage);
                mApplicationContext.sendBroadcast(intent);
                if(isImage) {
                    downloadFile(request);
                }else{
                    mFileTransferRequest = request;
                }

            }
        });

    }

    private void downloadFile(final FileTransferRequest request){

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("test","receiving file. saving to sd card " + request.getFileSize());
                IncomingFileTransfer incomingFileTransfer = request.accept();
                String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/" + request.getFileName();
                try {
                    incomingFileTransfer.recieveFile(new File(path));
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (incomingFileTransfer.getStatus() != FileTransfer.Status.complete){
                    if (incomingFileTransfer.getStatus().equals(FileTransfer.Status.error)){
                        //TO DO
                        break;
                    }
                }

                String from = request.getRequestor();
                String contactJid="";
                String[] fromArray = from.split("/");
                String fromUser = fromArray[0];

                contactJid = fromUser;

                //Bundle up the intent and send the broadcast.
                Intent intent = new Intent(RoosterConnectionService.NEW_MESSAGE);
                //intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(RoosterConnectionService.BUNDLE_FROM_JID,contactJid);
                intent.putExtra(RoosterConnectionService.BUNDLE_TYPE,RoosterConnectionService.MESSAGE_TYPE_FILE);
                intent.putExtra(RoosterConnectionService.BUNDLE_FILE_PATH,path);
                String currentTime = String.valueOf(System.currentTimeMillis());
                intent.putExtra("time",currentTime);
                Log.i("test","notifying UI");
                mApplicationContext.sendBroadcast(intent);

            }
        }).start();
    }

    private void processReceivedMessage(boolean group, Message message){
        Log.d(TAG,"message.getBody() :"+message.getBody());
        Log.d(TAG,"message.getFrom() :"+message.getFrom());
        Log.d(TAG,"message.getType() :"+message.getType().toString());
        Log.d(TAG,"current user is " + mConnection.getUser());

        String from = message.getFrom();
        String contactJid="";
        String[] fromArray = from.split("/");
        String fromUser = "";
        if(fromArray.length>2){
            fromUser = fromArray[1];
        }else{
            fromUser = fromArray[0];
        }

        if(group && mConnection.getUser().contains(fromUser)){
            return;
        }

        contactJid = fromUser;
        String msg = "";
        if(group){
            msg = fromUser +":" + System.getProperty("line.separator") + message.getBody();
        }else{
            msg = message.getBody();
        }

        //Bundle up the intent and send the broadcast.
        Intent intent = new Intent(RoosterConnectionService.NEW_MESSAGE);
        //intent.setPackage(mApplicationContext.getPackageName());
        intent.putExtra(RoosterConnectionService.BUNDLE_TYPE,RoosterConnectionService.MESSAGE_TYPE_TEXT);
        intent.putExtra(RoosterConnectionService.BUNDLE_FROM_JID,contactJid);
        intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,msg);
        mApplicationContext.sendBroadcast(intent);
        Log.d(TAG,"Received message from :"+contactJid+" broadcast sent.");

    }

    private void setupUiThreadBroadCastMessageReceiver() {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                if( action.equals(RoosterConnectionService.SEND_MESSAGE))
                {
                    boolean toGroup = intent.getBooleanExtra("toGroup",false);
                    String type = intent.getStringExtra(RoosterConnectionService.BUNDLE_TYPE);
                    if(type.equals(RoosterConnectionService.MESSAGE_TYPE_TEXT)) {
                        String msg = intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY);
                        //Send the message.
                        if (toGroup) {
                            sendGroupMessage(msg);
                        } else {
                            sendMessage(msg,
                                    intent.getStringExtra(RoosterConnectionService.BUNDLE_TO));
                        }
                    }else{
                        String path = intent.getStringExtra(RoosterConnectionService.BUNDLE_FILE_PATH);
                        if(toGroup){

                        }else{
                            String toId = intent.getStringExtra(RoosterConnectionService.BUNDLE_TO);
                            transferImage(path,toId);
                        }
                    }
                }else if(action.equals(RoosterConnectionService.JOIN_GROUP)){
                    Log.i("test","starting join group");
                    joinGroup();
                }else if(action.equals(RoosterConnectionService.LEAVE_GROUP)){
                    leaveGroup();
                }else if(action.equals(RoosterConnectionService.RECEIVE_FILE)){
                    if(mFileTransferRequest != null){
                        downloadFile(mFileTransferRequest);
                    }
                }else if(action.equals(RoosterConnectionService.CANCEL_FILE)){
                    if(mFileTransferRequest != null){
                        try {
                            mFileTransferRequest.reject();
                            mFileTransferRequest = null;
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(RoosterConnectionService.SEND_MESSAGE);
        filter.addAction(RoosterConnectionService.JOIN_GROUP);
        filter.addAction(RoosterConnectionService.LEAVE_GROUP);
        filter.addAction(RoosterConnectionService.RECEIVE_FILE);
        filter.addAction(RoosterConnectionService.CANCEL_FILE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver,filter);

    }

    MultiUserChat mMultiUserChat;
    private void joinGroup(){


        mMultiUserChat  = multiUserChatManager.getMultiUserChat(ROOM_NAME + "@conference.192.168.6.240");
        mMultiUserChat.addMessageListener(mGroupchatMessageListener);


        try {
            mMultiUserChat.createOrJoin(mConnection.getUser());
            //mMultiUserChat.invite("minh2@192.168.8.182/Android","hello");
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        }
    }

    MessageListener mGroupchatMessageListener = new MessageListener() {
        @Override
        public void processMessage(Message message) {
            Log.i("test","receive new message of group"  + message.getTo());
            processReceivedMessage(true,message);
        }
    };

    private void sendGroupMessage(String msg){
        Log.i("test","start send group message");
        try {
            mMultiUserChat.sendMessage(msg);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    private void transferImage(final String path, final String toId){
        Log.i("test","transfer file to " + toId);
        new Thread(new Runnable() {
            @Override
            public void run() {
                OutgoingFileTransfer outgoingFileTransfer = mFileTransferManager.createOutgoingFileTransfer(toId + "/Android");
                try {
                    outgoingFileTransfer.sendFile(new File(path),"des");
                    boolean success = true;
                    while(outgoingFileTransfer.getStatus() != FileTransfer.Status.complete){
                        if(outgoingFileTransfer.getStatus() == FileTransfer.Status.cancelled || outgoingFileTransfer.getStatus() == FileTransfer.Status.error ||
                                outgoingFileTransfer.getStatus() == FileTransfer.Status.refused){
                            success = false;
                            break;
                        }
                    }

                    Intent intent = new Intent(RoosterConnectionService.FILE_TRANSFER_RESULT);
                    intent.putExtra(RoosterConnectionService.BUNDLE_SUCCESS, success);
                    mApplicationContext.sendBroadcast(intent);
                } catch (SmackException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void leaveGroup(){
        if(mMultiUserChat != null){
            try {

                mMultiUserChat.removeMessageListener(mGroupchatMessageListener);
                mMultiUserChat.leave();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage ( String body ,String toJid)
    {
        Log.d(TAG,"Sending message to :"+ toJid);
        Chat chat = ChatManager.getInstanceFor(mConnection)
                .createChat(toJid,messageListener);
        try
        {
            //Message message = new Message(toJid);

            chat.sendMessage(body);
        }catch (SmackException.NotConnectedException  e)
        {
            e.printStackTrace();
        }
    }


    public void disconnect()
    {
        try
        {
            if (mConnection != null)
            {
                mConnection.disconnect();
            }

        }catch (Exception e)
        {
            RoosterConnectionService.sConnectionState= ConnectionState.DISCONNECTED;
            e.printStackTrace();

        }
        mConnection = null;
        // Unregister the message broadcast receiver.
        if( uiThreadMessageReceiver != null)
        {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }


    @Override
    public void connected(XMPPConnection connection) {
        RoosterConnectionService.sConnectionState= ConnectionState.CONNECTED;
        Log.d(TAG,"Connected Successfully");

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        RoosterConnectionService.sConnectionState= ConnectionState.CONNECTED;
        Log.d(TAG,"Authenticated Successfully");
        showContactListActivityWhenAuthenticated(connection.getUser());

    }


    @Override
    public void connectionClosed() {
        RoosterConnectionService.sConnectionState= ConnectionState.DISCONNECTED;
        Log.d(TAG,"Connectionclosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        RoosterConnectionService.sConnectionState= ConnectionState.DISCONNECTED;
        Log.d(TAG,"ConnectionClosedOnError, error "+ e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTING;
        Log.d(TAG,"ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG,"ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        RoosterConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG,"ReconnectionFailed()");

    }

    private void showContactListActivityWhenAuthenticated(String currentId)
    {
        Intent i = new Intent(RoosterConnectionService.UI_AUTHENTICATED);
        i.putExtra("id",currentId);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG,"Sent the broadcast that we are authenticated");
    }
}
