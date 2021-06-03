package com.example.coin.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FirebaseRTCClient implements AppRTCClient, ValueEventListener {
    private static final String TAG = "FirebaseRTCClient";
    private DatabaseReference database;
    private SignalingEvents events;
    private String myId, roomID;

    private boolean is_initiator = false;


    private final Handler handler;

    private Hashtable<String, Boolean> sdpAdded = new Hashtable<String, Boolean>();

    public FirebaseRTCClient(SignalingEvents events, String roomID) {
        database = FirebaseDatabase.getInstance().getReference();
        this.events = events;
        this.roomID = roomID;

        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Log.d(TAG, "onDataChangedddddddddddddddddddddddd" + dataSnapshot);
        if (!dataSnapshot.exists() && !is_initiator) {
            is_initiator = true;
        }

        if (!dataSnapshot.hasChild(myId)) {
            database.child("channels").child(roomID).child(myId).child("connected").setValue(true);
            //Connected
            handler.post(() -> {
                List<PeerConnection.IceServer> iceServerList = null;
                try {
                    //iceServerList = requestTurnServers("https://networktraversal.googleapis.com/v1alpha/iceconfig?key=AIzaSyAJdh2HkajseEIltlZ3SIXO02Tze9sO3NY"); // 구글의 IceServer 주소
                    iceServerList = new LinkedList<PeerConnection.IceServer>();
                    SignalingParameters parameters = new SignalingParameters(
                            // Ice servers are not needed for direct connections.
                            iceServerList,
                            is_initiator, // Server side acts as the initiator on direct connections.
                            null, // clientId
                            null, // wssUrl
                            null, // wwsPostUrl
                            null, // offerSdp
                            null // iceCandidates
                    );
                    events.onConnectedToRoom(parameters);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }

        if (dataSnapshot.hasChildren()) {
            Iterator<DataSnapshot> children = dataSnapshot.getChildren().iterator();

            while (children.hasNext()) {
                DataSnapshot child = children.next();

                if (child.getKey() != myId) {
                    if (child.hasChild("sdp")) {
                        child.getChildren();
                        final SessionDescription sdp = getSdp(child.child("sdp"));
                        Log.d(TAG, "onRemoteDescription: " + sdp.description);
                        handler.post(() -> {
                            if (sdpAdded.get(sdp.type + sdp.description) == null) {
                                events.onRemoteDescription(sdp);
                                sdpAdded.put(sdp.type + sdp.description, true);
                            }
                        });

                    }

                    Iterator<DataSnapshot> iceList = child.child("icecandidate").getChildren().iterator();

                    while (iceList.hasNext()) {
                        DataSnapshot iceChild = iceList.next();
                        final IceCandidate candidate = getIceCandidate(iceChild);

                        Log.d(TAG, "onRemoteIceCandidate");
                        handler.post(() -> events.onRemoteIceCandidate(candidate));
                    }
                }
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }


    @Override
    public void connectToRoom() {

        myId = FirebaseAuth.getInstance().getUid();
        database.child("channels").child(roomID).addValueEventListener(this);
    }

    private SessionDescription getSdp(DataSnapshot db) {
        String type = db.child("type").getValue().toString();
        String desc = db.child("description").getValue().toString();

        return new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), desc);
    }

    @Override
    public void sendOfferSdp(SessionDescription sdp) {
        Log.d(TAG, "send offer sdp");
        database.child("channels").child(roomID).child(myId).child("sdp").setValue(sdp);
    }

    @Override
    public void sendAnswerSdp(SessionDescription sdp) {
        Log.d(TAG, "send answer sdp");
        database.child("channels").child(roomID).child(myId).child("sdp").setValue(sdp);
    }

    private IceCandidate getIceCandidate(DataSnapshot db) {
        String sdp = db.child("sdp").getValue(String.class);
        int sdpMLineIndex = db.child("sdpMLineIndex").getValue(Integer.class);
        String sdpMid = db.child("sdpMid").getValue(String.class);

        Log.d(TAG, "my check point!!!!!!!!!"+ sdpMid + ", " + sdpMLineIndex + ", " + sdp);

        return new IceCandidate(sdpMid, sdpMLineIndex, sdp);
    }

    @Override
    public void sendLocalIceCandidate(IceCandidate candidate) {
        Log.d(TAG, "send local ice candidate: " + candidate);
        database.child("channels").child(roomID).child(myId).child("icecandidate").child("" + candidate.hashCode()).setValue(candidate);
    }

    @Override
    public void sendLocalIceCandidateRemovals(IceCandidate[] candidates) {
        Log.d(TAG, "send local ice candidate removal");

        for (IceCandidate candidate : candidates) {
            database.child("channels").child(roomID).child(myId).child("icecandidate").child("" + candidate.hashCode()).removeValue();
        }
    }

    @Override
    public void disconnectFromRoom() {
        Log.d(TAG, "disconnect from room");
        database.child("channels").child(roomID).child(myId).removeValue();
        database.onDisconnect();
        sdpAdded.clear();
        is_initiator = true;
    }
}
