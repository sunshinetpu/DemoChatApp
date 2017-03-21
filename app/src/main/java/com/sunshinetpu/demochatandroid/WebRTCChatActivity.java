package com.sunshinetpu.demochatandroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL;

/**
 * Created by sunshine on 3/17/17.
 */

public class WebRTCChatActivity extends Activity {
    private final String USER1_ID = "minh2@192.168.8.182";
    private final String USER2_ID = "minh3@192.168.8.182";
    private String mMyId;
    private String mPartnerId;
    private Button mButtonSetup;
    private Button mButtonCall;
    private PeerConnectionFactory mPeerConnectionFactory;
    private GLSurfaceView mViewMe, mViewPartner;
    private PeerConnection mPeerConnection;
    BroadcastReceiver mBroadcastReceiver;
    private GLSurfaceView mVideoView;


    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 25;
    private static final int REMOTE_HEIGHT = 25;
    private RendererCommon.ScalingType scalingType = SCALE_ASPECT_FILL;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc_chat);
        mVideoView = (GLSurfaceView)findViewById(R.id.video_view);
        String id = getIntent().getStringExtra("id");
        Log.i("test","receive id " + id);
        if(id.contains(USER1_ID)){
            mMyId = USER1_ID;
            mPartnerId = USER2_ID;
        }else{
            mMyId = USER2_ID;
            mPartnerId = USER1_ID;
        }
        Log.i("test","partner Id is " + mPartnerId);
        initPeerClient();
        mButtonCall = (Button)findViewById(R.id.btn_call);
        mButtonSetup = (Button) findViewById(R.id.btn_setup);
        mButtonSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPeerConnection.createOffer(offerObserver,new MediaConstraints());
            }
        });

    }
   // VideoRenderer.Callbacks localVideo = VideoRendererGui.create(0,0,0,0,scalingType,true);

    public static String getDeviceName(int index) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        try {
            Camera.getCameraInfo(index, info);
        } catch (Exception e) {
            Log.e("test", "getCameraInfo failed on index " + index,e);
            return null;
        }

        String facing =
                (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) ? "front" : "back";
        return "Camera " + index + ", Facing " + facing
                + ", Orientation " + info.orientation;
    }

    // Returns the name of the front facing camera. Returns null if the
    // camera can not be used or does not exist.
    public static String getNameOfFrontFacingDevice() {
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            try {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    return getDeviceName(i);
            } catch (Exception e) {
                Log.e("test", "getCameraInfo failed on index " + i, e);
            }
        }
        return null;
    }
    private void initPeerClient(){
        PeerConnectionFactory.initializeAndroidGlobals(WebRTCChatActivity.this,true,true,true);
        mPeerConnectionFactory = new PeerConnectionFactory();

        VideoRendererGui.setView( mVideoView, new Runnable() {
            @Override
            public void run() {

            }
        });
        VideoCapturer videoCapturer = VideoCapturerAndroid.create(getNameOfFrontFacingDevice());

        MediaConstraints videoConstraints = new MediaConstraints();

         VideoSource videoSource = mPeerConnectionFactory.createVideoSource(videoCapturer,videoConstraints);
        AudioSource audioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
        VideoTrack videoTrack = mPeerConnectionFactory.createVideoTrack("1",videoSource);
        AudioTrack audioTrack = mPeerConnectionFactory.createAudioTrack("2",audioSource);


        try {
            VideoRenderer renderer = VideoRendererGui.createGui(LOCAL_X_CONNECTED,LOCAL_Y_CONNECTED,LOCAL_WIDTH_CONNECTED,LOCAL_HEIGHT_CONNECTED, scalingType,false);
            videoTrack.addRenderer(renderer);
            MediaStream mediaStream = mPeerConnectionFactory.createLocalMediaStream("streamlocal");
            mediaStream.addTrack(videoTrack);
            mediaStream.addTrack(audioTrack);
           // List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
            List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>(25);
            iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.services.mozilla.com"));
            iceServers.add(new PeerConnection.IceServer("turn:turn.bistri.com:80", "homeo", "homeo"));
            iceServers.add(new PeerConnection.IceServer("turn:turn.anyfirewall.com:443?transport=tcp", "webrtc", "webrtc"));

            // Extra Defaults - 19 STUN servers + 4 initial = 23 severs (+2 padding) = Array cap 25
            iceServers.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
          /*  iceServers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
            iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
            iceServers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
            iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
            iceServers.add(new PeerConnection.IceServer("stun:stun01.sipphone.com"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.ekiga.net"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.fwdnet.net"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.ideasip.com"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.iptel.org"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.rixtelecom.se"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.schlund.de"));
            iceServers.add(new PeerConnection.IceServer("stun:stunserver.org"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.softjoys.com"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.voiparound.com"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.voipbuster.com"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.voipstunt.com"));
            iceServers.add(new PeerConnection.IceServer("stun:stun.voxgratia.org"));*/
            iceServers.add(new PeerConnection.IceServer("stun:stun.xten.com"));
            MediaConstraints mediaConstraints = new MediaConstraints();
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
            //pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

            mPeerConnection = mPeerConnectionFactory.createPeerConnection(iceServers,mediaConstraints ,peerConnectionObserver );
            mPeerConnection.addStream(mediaStream);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case RoosterConnectionService.NEW_MESSAGE:
                        Log.i("test", "receive new message");
                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
                        String type = intent.getStringExtra(RoosterConnectionService.BUNDLE_TYPE);
                        String time = intent.getStringExtra("time");
                        Log.i("test", "time is " + time);
                        if (type.equals(RoosterConnectionService.MESSAGE_TYPE_TEXT)) {
                            String body = intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY);
                            Log.i("test", "receive new message " + body);
                            processOffer(body);
                        }

                }
            }
        };
        IntentFilter filter = new IntentFilter(RoosterConnectionService.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiver,filter);
    }

    private void processOffer(String msg){
        try {
            JSONObject jsonObject = new JSONObject(msg);
            String type = jsonObject.getString("type");
            if(type.equals("offer")){
                String des = jsonObject.getString("des");
                Log.i("test","des offer is " + des);
                SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER,des);
                mPeerConnection.setRemoteDescription(setRemoteDesObserver,sessionDescription);
                mRemoteSdpIsSet = true;
                mPeerConnection.createAnswer(createAnswerObserver,new MediaConstraints());
            }else if(type.equals("answer")){
                String des = jsonObject.getString("des");
                Log.i("test","des answer is " + des);
                SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER,des);
                mPeerConnection.setRemoteDescription(setRemoteDesObserver,sessionDescription);
                mRemoteSdpIsSet = true;
                //mPeerConnection.addIceCandidate(new IceCandidate(
            }else{
                String sdpMid = jsonObject.getString("sdpMid");
                int sdpMLineIndex = jsonObject.getInt("lineIndex");
                String sdp = jsonObject.getString("sdp");
                if(mRemoteSdpIsSet){
                    mPeerConnection.addIceCandidate(new IceCandidate(sdpMid,sdpMLineIndex,sdp));
                    Log.i("test","add new IceCandidate " + sdp);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendAnswerDescription(SessionDescription sessionDescription){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", "answer");
            jsonObject.put("des", sessionDescription.description);

            Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
            intent.putExtra("toGroup",false);
            intent.putExtra(RoosterConnectionService.BUNDLE_TYPE,RoosterConnectionService.MESSAGE_TYPE_TEXT);
            intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY, jsonObject.toString());
            intent.putExtra(RoosterConnectionService.BUNDLE_TO, mPartnerId);
            sendBroadcast(intent);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    SdpObserver createAnswerObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.i("test","local description :" + sessionDescription.description);
            mPeerConnection.setLocalDescription(this,sessionDescription);
            sendAnswerDescription(sessionDescription);

        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    };

    SdpObserver setRemoteDesObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.i("test","create remote des success");
        }

        @Override
        public void onSetSuccess() {
            Log.i("test","set remote des success");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.i("test","create remote des fail");
        }

        @Override
        public void onSetFailure(String s) {
            Log.i("test","set remote des fail");
        }
    };

    SdpObserver offerObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.i("test","local description :" + sessionDescription.description);
            mPeerConnection.setLocalDescription(this,sessionDescription);
            sendOfferPackage(sessionDescription);
        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    };

    private void sendOfferPackage(SessionDescription sessionDescription){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", "offer");
            jsonObject.put("des", sessionDescription.description);

            Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
            intent.putExtra("toGroup",false);
            intent.putExtra(RoosterConnectionService.BUNDLE_TYPE,RoosterConnectionService.MESSAGE_TYPE_TEXT);
            intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY, jsonObject.toString());
            intent.putExtra(RoosterConnectionService.BUNDLE_TO, mPartnerId);
            sendBroadcast(intent);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }


    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.i("test","onSignalingChange");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.i("test","onIceConnection change " + iceConnectionState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.i("test","onIceConnection receivingChange");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.i("test","onIceGathering change");
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.i("test","onIceCandidate");
            sendIceCandidateToOtherPeer(iceCandidate);

        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.i("test","onAddStream");
          //  mediaStream.videoTracks.get(0).
            try {
                VideoRenderer renderer = VideoRendererGui.createGui(REMOTE_X,REMOTE_Y,REMOTE_WIDTH,REMOTE_HEIGHT, scalingType,true);
                mediaStream.videoTracks.get(0).addRenderer(renderer);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.i("test","onRemoveStream");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.i("test","onDataChanel changed");

        }

        @Override
        public void onRenegotiationNeeded() {
            Log.i("test","onRenegotiationNeeded");
        }

    };

    private boolean mRemoteSdpIsSet = false;
    private void sendIceCandidateToOtherPeer(IceCandidate iceCandidate){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type","ice");
            jsonObject.put("sdpMid",iceCandidate.sdpMid);
            jsonObject.put("lineIndex",iceCandidate.sdpMLineIndex);
            jsonObject.put("sdp",iceCandidate.sdp);
            Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
            intent.putExtra("toGroup",false);
            intent.putExtra(RoosterConnectionService.BUNDLE_TYPE,RoosterConnectionService.MESSAGE_TYPE_TEXT);
            intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY, jsonObject.toString());
            intent.putExtra(RoosterConnectionService.BUNDLE_TO, mPartnerId);
            sendBroadcast(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }
}