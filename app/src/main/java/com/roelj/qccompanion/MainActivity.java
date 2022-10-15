/*
 * Copyright (C) 2022  Roel Janssen <roel@roelj.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.roelj.qccompanion;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer player;
    private TextView statusText;
    private TextView timerText;
    private HashMap<String, Uri> buttonSoundSettings;
    private HashMap<String, Button> toggleButtons;
    private Button playerStatusButton;
    private Button currentlyPlayingButton;
    private Button lastButtonClicked;
    private Button qcConnectionStateButton;
    private PowerManager.WakeLock wakeLock;
    private MidiService.MidiServiceBinder midiServiceBinder;
    private MidiService midiService;

    /* --------------------------------------------------------------------
     * UTILITY PROCEDURES
     * -------------------------------------------------------------------- */

    private String filenameFromUri (Uri uri) {
        String path = uri.getLastPathSegment();
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize state variables.
        this.statusText            = (TextView) findViewById(R.id.statusText);
        this.timerText             = (TextView) findViewById(R.id.timerText);
        this.player                = new MediaPlayer();
        this.toggleButtons         = new HashMap<>();
        this.buttonSoundSettings   = new HashMap<>();
        this.playerStatusButton    = (Button) findViewById(R.id.playerStatusButton);
        this.qcConnectionStateButton = (Button) findViewById(R.id.qcConnectionStateButton);

        this.toggleButtons.put("A", (Button) findViewById(R.id.buttonA));
        this.toggleButtons.put("B", (Button) findViewById(R.id.buttonB));
        this.toggleButtons.put("C", (Button) findViewById(R.id.buttonC));
        this.toggleButtons.put("D", (Button) findViewById(R.id.buttonD));
        this.toggleButtons.put("E", (Button) findViewById(R.id.buttonE));
        this.toggleButtons.put("F", (Button) findViewById(R.id.buttonF));
        this.toggleButtons.put("G", (Button) findViewById(R.id.buttonG));
        this.toggleButtons.put("H", (Button) findViewById(R.id.buttonH));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "QCCompanion::AudioPlayingWakeLock");

        Context context = getApplicationContext();
        if (! context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            statusText.setText(getText(R.string.no_midi_support));
        }
        else {
            Log.i("MainActivity::onCreate", "Starting MidiService");
            Intent serviceIntent = new Intent(MainActivity.this, MidiService.class);
            bindService (serviceIntent, connection, Context.BIND_AUTO_CREATE);
            startService(serviceIntent);

        }
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected (ComponentName className, IBinder service) {
            MidiService.MidiServiceBinder binder = (MidiService.MidiServiceBinder) service;
            midiService = binder.getService();
            midiService.setMainActivity(MainActivity.this);
            //setQcConnectionState (true);
        }

        @Override
        public void onServiceDisconnected (ComponentName arg0) {
            setQcConnectionState (false);
        }
    };

    public void setQcConnectionState (boolean isConnected) {
        Log.i("MainActivity::setQcConnectionState", String.format("Called with %b", isConnected));
        int state = R.color.qc_disconnected;
        if (isConnected) state = R.color.qc_connected;
        if (qcConnectionStateButton != null) {
            qcConnectionStateButton.setBackgroundColor(
                    getResources().getColor(state, getBaseContext().getTheme()));
        }
    }
    public void setStatusText (String text) {
        statusText.setText(text);
    }

    /* --------------------------------------------------------------------
     * FILE DIALOG
     * -------------------------------------------------------------------- */

    private final ActivityResultLauncher<Intent> fileChooserActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult (ActivityResult result) {
                    if (result.getResultCode() != Activity.RESULT_OK) return;

                    Intent data = result.getData();
                    if (data == null) return;

                    Uri fileUri = data.getData();
                    if (fileUri == null) return;

                    if (lastButtonClicked == null) return;

                    lastButtonClicked.setBackgroundColor(
                            getResources().getColor(R.color.assigned_button,
                            getBaseContext().getTheme()));
                    String buttonName = lastButtonClicked.getText().toString();
                    buttonSoundSettings.put(buttonName, fileUri);
                    statusText.setText(getString(R.string.button_assigned,
                            filenameFromUri(fileUri), buttonName));

                }
            });

    public void openFileDialog (View view) {
        Intent data = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        data.setType("audio/*");
        data = Intent.createChooser(data, getText(R.string.filechooser_title));
        lastButtonClicked = (Button) view;
        fileChooserActivity.launch(data);
    }

    public void connectToQuadCortex (View view) {
        midiService.attemptToConnectToQuadCortex();
    }

    /* --------------------------------------------------------------------
     * AUDIO PLAYER
     * -------------------------------------------------------------------- */

    public void togglePlayerForButton (String buttonName) {
        Uri fileUri = this.buttonSoundSettings.get(buttonName);
        // Don't do anything without sound file selected for the button.
        if (fileUri == null) return;

        try {
            if (! player.isPlaying()) {
                player.setDataSource(getApplicationContext(), fileUri);
                player.prepare();
                player.start();
                currentlyPlayingButton = toggleButtons.get(buttonName);
                if (currentlyPlayingButton != null) {
                    currentlyPlayingButton.setBackgroundColor(
                            getResources().getColor(R.color.playing_button,
                                    getBaseContext().getTheme()));
                }
                statusText.setText(getString(R.string.playing, filenameFromUri(fileUri)));
                timerText.postDelayed(updateTimerText, 0);
                if (playerStatusButton != null) {
                    playerStatusButton.setBackgroundColor(
                            getResources().getColor(R.color.button_player_status_playing,
                                    getBaseContext().getTheme()));
                    playerStatusButton.setText(getText(R.string.player_status_playing));
                }
                wakeLock.acquire(600000);
            } else if (currentlyPlayingButton != null
                    && currentlyPlayingButton != toggleButtons.get(buttonName)) {
                // Stop playing current audio.
                togglePlayerForButton(currentlyPlayingButton.getText().toString());
                // Start playing new audio.
                togglePlayerForButton(buttonName);
            } else {
                statusText.setText(getString(R.string.stopped, filenameFromUri(fileUri)));
                player.stop();
                player.reset();
                currentlyPlayingButton.setBackgroundColor(
                        getResources().getColor(R.color.assigned_button,
                                getBaseContext().getTheme()));
                currentlyPlayingButton = null;
                if (playerStatusButton != null) {
                    playerStatusButton.setBackgroundColor(
                            getResources().getColor(R.color.button_player_status_stopped,
                                    getBaseContext().getTheme()));
                    playerStatusButton.setText(getText(R.string.player_status_stopped));
                }
                wakeLock.release();
            }
        }
        catch (Exception error) {
            Log.e("togglePlayerForButton", "Toggling the player failed.");
        }
    }

    public final Runnable updateTimerText = new Runnable() {
        public void run() {
            if (! player.isPlaying()) {
                timerText.setText("");
                return;
            }
            int current_position = (int)(player.getCurrentPosition() / 1000);
            timerText.setText(getString(R.string.timing, current_position));
            timerText.postDelayed(this, 15);
        }
    };
}