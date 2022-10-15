package com.roelj.qccompanion;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class MidiService extends android.app.Service implements MidiCommandProcessor {

    private boolean quadCortexIsConnected;
    private MidiManager midiManager;
    private MainActivity mainActivity;

    private final IBinder binder = new MidiServiceBinder();

    public class MidiServiceBinder extends Binder {
        MidiService getService() {
            return MidiService.this;
        }
    }

    public void onCreate (MainActivity activity) {
        super.onCreate();

        quadCortexIsConnected = false;
        mainActivity = activity;
    }

    public void setMainActivity (MainActivity activity) {
        mainActivity = activity;
    }

    public void attemptToConnectToQuadCortex () {
        if (! quadCortexIsConnected) {
            for (MidiDeviceInfo info : midiManager.getDevices()) {
                if (deviceIsQuadCortex(info)) {
                    connectQuadCortexMidiDevice(info);
                }
            }
        }
    }

    private boolean deviceIsQuadCortex (MidiDeviceInfo info) {
        Bundle properties = info.getProperties();
        String manufacturer = properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER);
        String product = properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT);

        return (manufacturer.equals("Neural DSP") && product.equals("Quad Cortex"));
    }

    private void connectQuadCortexMidiDevice (MidiDeviceInfo info) {
        if (deviceIsQuadCortex(info)) {
            if (info.getOutputPortCount() < 1) return;

            midiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
                @Override
                public void onDeviceOpened (MidiDevice device) {
                    if (device == null) {
                        Log.e("MainActivity", getString(R.string.qc_cant_open));
                        return;
                    }
                    MidiOutputPort outputPort = device.openOutputPort(0);
                    if (outputPort != null) {
                        QuadCortexMidiReceiver receiver = new QuadCortexMidiReceiver();
                        receiver.registerCallback(MidiService.this);
                        outputPort.connect(receiver);
                        mainActivity.setStatusText(getString(R.string.qc_connected));
                        mainActivity.setQcConnectionState (true);
                        quadCortexIsConnected = true;
                    }
                }
            }, new Handler(Looper.getMainLooper()));
        }
    }

    public int onStartCommand (Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);

        // Make sure the app reflects MIDI device changes.
        midiManager.registerDeviceCallback(new MidiManager.DeviceCallback() {
            public void onDeviceAdded (MidiDeviceInfo info) {
                if (! quadCortexIsConnected) {
                    connectQuadCortexMidiDevice(info);
                }
            }

            public void onDeviceRemoved (MidiDeviceInfo info) {
                if (deviceIsQuadCortex(info)) {
                    mainActivity.setStatusText(getString(R.string.qc_disconnected));
                    mainActivity.setQcConnectionState (false);
                    quadCortexIsConnected = false;
                }
            }

            public void onDeviceStatusChanged (MidiDeviceStatus status) {
                Log.i("MIDIDeviceChanged", status.toString());
            }
        }, new Handler(Looper.getMainLooper()));

        attemptToConnectToQuadCortex();
        return Service.START_STICKY;
    }

    /* --------------------------------------------------------------------
     * MIDI INPUT
     * -------------------------------------------------------------------- */

    @Override
    public void processMidiCommand (int channel_index, int value) {
        // Use channel 1 for A-H foot switches.
        if (channel_index == 1) {
            switch (value) {
                case 1: mainActivity.togglePlayerForButton("A"); break;
                case 2: mainActivity.togglePlayerForButton("B"); break;
                case 3: mainActivity.togglePlayerForButton("C"); break;
                case 4: mainActivity.togglePlayerForButton("D"); break;
                case 5: mainActivity.togglePlayerForButton("E"); break;
                case 6: mainActivity.togglePlayerForButton("F"); break;
                case 7: mainActivity.togglePlayerForButton("G"); break;
                case 8: mainActivity.togglePlayerForButton("H"); break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
