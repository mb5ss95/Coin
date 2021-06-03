package com.example.coin;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.coin.enter.gameDialog;
import com.example.coin.util.AppRTCAudioManager;
import com.example.coin.util.AppRTCClient;
import com.example.coin.util.FirebaseRTCClient;
import com.example.coin.util.PeerConnectionClient;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class RoomActivity extends AppCompatActivity implements CallFragment.OnCallEvents {
    private static final String TAG = "RoomActivity";

    public static final String EXTRA_VIDEO_CALL = "true";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED = "false";

    private long callStartedTimeMs = 0;

    private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer();
    private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
    private final List<VideoRenderer.Callbacks> remoteRenderers = new ArrayList<>();

    private PeerConnectionClient peerConnectionClient = null;
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;

    private AppRTCClient appRtcClient;
    private AppRTCClient.SignalingParameters signalingParameters;
    private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    private AppRTCAudioManager audioManager;

    private SurfaceViewRenderer sur2;
    private SurfaceViewRenderer sur1;


    private boolean activityRunning;
    private boolean iceConnected;
    private boolean isError;
    private boolean micEnabled = true;
    private boolean isSwappedFeeds;


    // Controls
    private CallFragment callFragment;
    private boolean callControlFragmentVisible = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InitTitle();
        setContentView(R.layout.activity_room);

        iceConnected = false;
        signalingParameters = null;

        Intent intent = getIntent();
        String EXTRA_ROOMID = intent.getStringExtra("RoomID");

        setTitle("  " + EXTRA_ROOMID);
        InitWiget();


        connectVideoCall(EXTRA_ROOMID);
        connetPeerConnection();
        startCall();
    }

    private void startCall() {
        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();

        appRtcClient.connectToRoom();

        audioManager = AppRTCAudioManager.create(getApplicationContext());
        audioManager.start((audioDevice, availableAudioDevices) -> onAudioManagerDevicesChanged(audioDevice, availableAudioDevices));
    }

    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
    }


    private void connetPeerConnection() {
        peerConnectionClient
                .createPeerConnectionFactory(
                        getApplicationContext(),
                        peerConnectionParameters,
                        new PeerConnectionClient.PeerConnectionEvents() {
                            @Override
                            public void onLocalDescription(final SessionDescription sdp) {
                                final long delta = System.currentTimeMillis() - callStartedTimeMs;
                                runOnUiThread(() -> {
                                    if (appRtcClient != null) {
                                        logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                                        if (signalingParameters.initiator) {
                                            appRtcClient.sendOfferSdp(sdp);
                                        } else {
                                            appRtcClient.sendAnswerSdp(sdp);
                                        }
                                    }
                                    if (peerConnectionParameters.videoMaxBitrate > 0) {
                                        Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                                        peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
                                    }
                                });
                            }

                            @Override
                            public void onIceCandidate(final IceCandidate candidate) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (appRtcClient != null) {
                                            appRtcClient.sendLocalIceCandidate(candidate);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (appRtcClient != null) {
                                            appRtcClient.sendLocalIceCandidateRemovals(candidates);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onIceConnected() {
                                final long delta = System.currentTimeMillis() - callStartedTimeMs;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        logAndToast("ICE connected, delay=" + delta + "ms");
                                        iceConnected = true;
                                        callConnected();
                                    }
                                });
                            }

                            @Override
                            public void onIceDisconnected() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        logAndToast("ICE disconnected");
                                        iceConnected = false;
                                        disconnect();
                                    }
                                });
                            }

                            @Override
                            public void onPeerConnectionClosed() {
                            }

                            @Override
                            public void onPeerConnectionStatsReady(final StatsReport[] reports) {
                            }

                            @Override
                            public void onPeerConnectionError(final String description) {
                                reportError(description);
                            }

                        });
    }

    private void InitWiget() {
        callFragment = new CallFragment();

        // Create UI controls.
        sur2 = findViewById(R.id.ac_room_surfaceView2);
        sur1 = findViewById(R.id.ac_room_surfaceView);

        sur1.setOnClickListener(v -> toggleCallControlFragmentVisibility());
        sur2.setOnClickListener(v -> setSwappedFeeds(!isSwappedFeeds));

        remoteRenderers.add(remoteProxyRenderer);

        // Create peer connection client.
        peerConnectionClient = new PeerConnectionClient();

        // Create video renderers.
        sur2.init(peerConnectionClient.getRenderContext(), null);
        sur2.setScalingType(ScalingType.SCALE_ASPECT_FIT);

        sur1.init(peerConnectionClient.getRenderContext(), null);
        sur1.setScalingType(ScalingType.SCALE_ASPECT_FILL);

        sur2.setZOrderMediaOverlay(true);
        sur2.setEnableHardwareScaler(true /* enabled */);
        sur1.setEnableHardwareScaler(true /* enabled */);
        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        setSwappedFeeds(true /* isSwappedFeeds */);

        // Activate call and HUD fragments and start the call.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.commit();
    }


    // Join video call with randomly generated roomId
    private void connectVideoCall(String roomId) {
        Uri roomUri = Uri.parse("https://appr.tc");

        peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(true,
                        false,
                        false,
                        0,
                        0,
                        0,
                        1700,
                        "VP8",
                        true,
                        false,
                        32,
                        "OPUS",
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        null);

        // Create connection client. Use the standard WebSocketRTCClient.
        // DirectRTCClient could be used for point-to-point connection
        appRtcClient = new FirebaseRTCClient(new AppRTCClient.SignalingEvents() {
            @Override
            public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {
                runOnUiThread(() -> onConnectedToRoomInternal(params));
            }

            @Override
            public void onRemoteDescription(final SessionDescription sdp) {
                final long delta = System.currentTimeMillis() - callStartedTimeMs;
                runOnUiThread(() -> {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                        return;
                    }
                    logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                    peerConnectionClient.setRemoteDescription(sdp);
                    if (!signalingParameters.initiator) {
                        logAndToast("Creating ANSWER...");
                        // Create answer. Answer SDP will be sent to offering client in
                        // PeerConnectionEvents.onLocalDescription event.
                        peerConnectionClient.createAnswer();
                    }
                });
            }

            @Override
            public void onRemoteIceCandidate(final IceCandidate candidate) {
                runOnUiThread(() -> {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                        return;
                    }
                    peerConnectionClient.addRemoteIceCandidate(candidate);
                });
            }

            @Override
            public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
                runOnUiThread(() -> {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                        return;
                    }
                    peerConnectionClient.removeRemoteIceCandidates(candidates);
                });
            }

            @Override
            public void onChannelClose() {
                runOnUiThread(() -> {
                    logAndToast("Remote end hung up; dropping PeerConnection");
                    disconnect();
                });
            }

            @Override
            public void onChannelError(final String description) {
                reportError(description);
            }

        }, roomId);

        roomConnectionParameters =
                new AppRTCClient.RoomConnectionParameters(
                        roomUri.toString(),
                        roomId,
                        false,
                        null);
    }

    @Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {

    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {

    }

    @Override
    public boolean onToggleMic() {
        if (peerConnectionClient != null) {
            micEnabled = !micEnabled;
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
        } else {
            ft.hide(callFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    @UiThread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, 1000);
        setSwappedFeeds(false /* isSwappedFeeds */);
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;
        remoteProxyRenderer.setTarget(null);
        localProxyVideoSink.setTarget(null);
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (sur2 != null) {
            sur2.release();
            sur2 = null;
        }
        if (sur1 != null) {
            sur1.release();
            sur1 = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (iceConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (!activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    // Create VideoCapturer
    private VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;
        Logging.d(TAG, "Creating capturer using camera2 API.");
        videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    // Create VideoCapturer from camera
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyVideoSink.setTarget(isSwappedFeeds ? sur1 : sur2);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? sur2 : sur1);
        sur1.setMirror(isSwappedFeeds);
        sur2.setMirror(!isSwappedFeeds);
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        peerConnectionClient.createPeerConnection(
                localProxyVideoSink, remoteRenderers, videoCapturer, signalingParameters);

        if (signalingParameters.initiator) {
            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    // Activity interfaces
    @Override
    public void onStop() {
        super.onStop();
        activityRunning = false;
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        disconnect();
        activityRunning = false;
        super.onDestroy();
    }

    private static class ProxyRenderer implements VideoRenderer.Callbacks {
        private VideoRenderer.Callbacks target;

        @Override
        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                VideoRenderer.renderFrameDone(frame);
                return;
            }

            target.renderFrame(frame);
        }

        synchronized public void setTarget(VideoRenderer.Callbacks target) {
            this.target = target;
        }
    }

    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                return;
            }

            target.onFrame(frame);
        }

        synchronized public void setTarget(VideoSink target) {
            this.target = target;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invitebf:
                return true;
            case R.id.chatting:
                return true;
            case R.id.RouletSelection:
                Intent intent = new Intent(this, gameDialog.class);
                startActivityForResult(intent, 100);
                return true;
            case R.id.LadderSelection:
                Intent intent2 = new Intent(this, MainActivity.class);
                startActivityForResult(intent2, 100);
                return true;
            case R.id.settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void InitTitle() {
        ActionBar ab = getSupportActionBar();
        ab.show();
        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayUseLogoEnabled(true);
        ab.setIcon(R.drawable.emotion);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "request, result, data : " + requestCode + ", " + resultCode + ", " + data);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations!!")
                .setMessage(data.getExtras().getString("sendText") + " 쭉 들이키세요~")
                .create().show();
    }
}
