package com.yourapp.webrtc;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SdpObserver;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

public class WebRtcManager {
    private static final String TAG = "CallWebRtc";

    public interface Listener {
        void onLocalIceCandidate(IceCandidate candidate);
        void onConnectionEstablished();
        void onConnectionClosed();
        void onError(String message);
    }

    public interface SdpCallback {
        void onSuccess(String sdp);
        void onError(String error);
    }

    private static final String AUDIO_TRACK_ID = "audio0";
    private static final String AUDIO_STREAM_ID = "call_audio";

    private static boolean factoryInitialized;

    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private AudioSource localAudioSource;
    private AudioTrack localAudioTrack;
    private AudioDeviceModule audioDeviceModule;
    private AudioManager audioManager;
    private int previousAudioMode = AudioManager.MODE_NORMAL;
    private boolean previousSpeakerphoneOn;
    private boolean previousMicrophoneMute;
    private Listener listener;
    private boolean remoteDescriptionSet;
    private final List<IceCandidate> pendingRemoteIceCandidates = new ArrayList<>();

    public void init(Context context, Listener listener) {
        this.listener = listener;
        if (!factoryInitialized) {
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context.getApplicationContext())
                            .createInitializationOptions());
            factoryInitialized = true;
        }

        configureAudioForCall(context);

        audioDeviceModule = JavaAudioDeviceModule.builder(context.getApplicationContext())
                .setUseHardwareAcousticEchoCanceler(false)
                .setUseHardwareNoiseSuppressor(false)
                .createAudioDeviceModule();

        factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory();

        createLocalAudioStream();
        createPeerConnection(context);
    }

    public void createLocalAudioStream() {
        if (factory == null) {
            return;
        }
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        AudioSource audioSource = factory.createAudioSource(constraints);
        localAudioSource = audioSource;
        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);
        Log.d(TAG, "Local audio track created, enabled=" + localAudioTrack.enabled());
    }

    public void createPeerConnection(Context context) {
        if (factory == null) {
            return;
        }
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(getIceServers());
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        peerConnection = factory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) { }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "ICE connection state=" + iceConnectionState);
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED && listener != null) {
                    listener.onConnectionEstablished();
                }
                if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED
                        || iceConnectionState == PeerConnection.IceConnectionState.FAILED
                        || iceConnectionState == PeerConnection.IceConnectionState.CLOSED) {
                    if (listener != null) {
                        listener.onConnectionClosed();
                    }
                }
            }

            @Override public void onIceConnectionReceivingChange(boolean b) { }
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "ICE gathering state=" + iceGatheringState);
            }
            @Override public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "Local ICE candidate mid=" + (iceCandidate != null ? iceCandidate.sdpMid : null));
                if (listener != null) listener.onLocalIceCandidate(iceCandidate);
            }
            @Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) { }
            @Override public void onAddStream(org.webrtc.MediaStream mediaStream) { }
            @Override public void onRemoveStream(org.webrtc.MediaStream mediaStream) { }
            @Override public void onDataChannel(org.webrtc.DataChannel dataChannel) { }
            @Override public void onRenegotiationNeeded() { }
            @Override public void onAddTrack(org.webrtc.RtpReceiver rtpReceiver, org.webrtc.MediaStream[] mediaStreams) {
                if (rtpReceiver != null && rtpReceiver.track() instanceof AudioTrack) {
                    ((AudioTrack) rtpReceiver.track()).setEnabled(true);
                    Log.d(TAG, "Remote audio track added, enabled=" + ((AudioTrack) rtpReceiver.track()).enabled());
                }
            }
        });
        if (peerConnection != null && localAudioTrack != null) {
            peerConnection.addTrack(localAudioTrack, Collections.singletonList(AUDIO_STREAM_ID));
            Log.d(TAG, "Local audio track added to PeerConnection");
        }
    }

    public void createOffer(SdpCallback callback) {
        if (peerConnection == null) {
            callback.onError("PeerConnection chua san sang");
            return;
        }
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        callback.onSuccess(sessionDescription.description);
                    }

                    @Override
                    public void onSetFailure(String s) {
                        callback.onError(s);
                    }
                }, sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                callback.onError(s);
            }
        }, createAudioSdpConstraints());
    }

    public void createAnswer(SdpCallback callback) {
        if (peerConnection == null) {
            callback.onError("PeerConnection chua san sang");
            return;
        }
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        callback.onSuccess(sessionDescription.description);
                    }

                    @Override
                    public void onSetFailure(String s) {
                        callback.onError(s);
                    }
                }, sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                callback.onError(s);
            }
        }, createAudioSdpConstraints());
    }

    public void setRemoteDescription(String sdp, boolean isOffer, SdpCallback callback) {
        if (peerConnection == null) {
            callback.onError("PeerConnection chua san sang");
            return;
        }
        SessionDescription.Type type = isOffer ? SessionDescription.Type.OFFER : SessionDescription.Type.ANSWER;
        SessionDescription description = new SessionDescription(type, sdp);
        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                remoteDescriptionSet = true;
                drainPendingRemoteIceCandidates();
                Log.d(TAG, "Remote description set, isOffer=" + isOffer);
                callback.onSuccess(sdp);
            }

            @Override
            public void onSetFailure(String s) {
                callback.onError(s);
            }
        }, description);
    }

    public void addIceCandidate(String candidate, String sdpMid, int sdpMLineIndex) {
        if (peerConnection == null || candidate == null) {
            return;
        }
        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
        if (!remoteDescriptionSet) {
            pendingRemoteIceCandidates.add(iceCandidate);
            Log.d(TAG, "Queued remote ICE before remote description");
            return;
        }
        Log.d(TAG, "Adding remote ICE candidate mid=" + sdpMid);
        peerConnection.addIceCandidate(iceCandidate);
    }

    private void drainPendingRemoteIceCandidates() {
        if (peerConnection == null || pendingRemoteIceCandidates.isEmpty()) {
            return;
        }
        for (IceCandidate candidate : new ArrayList<>(pendingRemoteIceCandidates)) {
            Log.d(TAG, "Draining queued remote ICE candidate mid=" + candidate.sdpMid);
            peerConnection.addIceCandidate(candidate);
        }
        pendingRemoteIceCandidates.clear();
    }

    public void toggleMute(boolean mute) {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(!mute);
        }
    }

    public boolean isLocalAudioReady() {
        return localAudioTrack != null && localAudioTrack.enabled();
    }

    public void toggleSpeaker(boolean useSpeaker, Context context) {
        AudioManager manager = audioManager != null ? audioManager : (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (manager != null) {
            manager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            manager.setMicrophoneMute(false);
            manager.setSpeakerphoneOn(useSpeaker);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void configureAudioForCall(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            previousAudioMode = audioManager.getMode();
            previousSpeakerphoneOn = audioManager.isSpeakerphoneOn();
            previousMicrophoneMute = audioManager.isMicrophoneMute();
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setMicrophoneMute(false);
            audioManager.setSpeakerphoneOn(true);
        }
    }

    public void release() {
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(previousSpeakerphoneOn);
            audioManager.setMicrophoneMute(previousMicrophoneMute);
            audioManager.setMode(previousAudioMode);
            audioManager.abandonAudioFocus(null);
            audioManager = null;
        }
        pendingRemoteIceCandidates.clear();
        remoteDescriptionSet = false;
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }
        if (localAudioSource != null) {
            localAudioSource.dispose();
            localAudioSource = null;
        }
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
        if (audioDeviceModule != null) {
            audioDeviceModule.release();
            audioDeviceModule = null;
        }
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    private List<PeerConnection.IceServer> getIceServers() {
        List<PeerConnection.IceServer> servers = new ArrayList<>();
        servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        servers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        servers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
        servers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
        return servers;
    }

    private MediaConstraints createAudioSdpConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        return constraints;
    }

    private abstract static class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sessionDescription) { }
        @Override public void onSetSuccess() { }
        @Override public void onCreateFailure(String s) { }
        @Override public void onSetFailure(String s) { }
    }
}
