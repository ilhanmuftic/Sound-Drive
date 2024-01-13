package com.example.sounddrive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.media.AudioFocusRequest;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private AudioManager audioManager;
    private TextView speedTextView;
    private MediaPlayer mediaPlayer;
    private boolean isSongPlaying = false;
    private int originalMusicVolume;
    private AudioFocusRequest audioFocusRequest;

    private Switch speedSwitch;
    private SeekBar lowThresholdSeekBar;
    private SeekBar highThresholdSeekBar;
    private SeekBar speedThresholdSeekBar;

    private TextView lowThresholdLabel;
    private TextView highThresholdLabel;
    private TextView speedThresholdLabel;

    // Adjustable parameters
    private boolean isSpeedControlEnabled = true;
    private float lowSpeedThreshold = 3.0f;
    private float highSpeedThreshold = 9.0f;
    private float speedThreshold = 12.0f;
    private float KMH = 2.2f;

    // Adjust this constant to control sensitivity (lower value = less sensitive)
    private static final float SENSITIVITY_SCALE = 0.5f;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        speedTextView = findViewById(R.id.speedTextView); // Assuming you have a TextView with this id
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // UI components initialization
        speedSwitch = findViewById(R.id.speedSwitch);
        lowThresholdSeekBar = findViewById(R.id.lowThresholdSeekBar);
        highThresholdSeekBar = findViewById(R.id.highThresholdSeekBar);
        speedThresholdSeekBar = findViewById(R.id.speedThresholdSeekBar);

        lowThresholdLabel = findViewById(R.id.lowThresholdLabel);
        highThresholdLabel = findViewById(R.id.highThresholdLabel);
        speedThresholdLabel = findViewById(R.id.speedThresholdLabel);


        // MediaPlayer initialization
        mediaPlayer = MediaPlayer.create(this, R.raw.speed); // Replace 'your_song' with the actual resource ID
        mediaPlayer.setLooping(true); // Loop the song

        // Set initial values for SeekBars
        lowThresholdSeekBar.setProgress((int) (lowSpeedThreshold * 2)); // Example scaling for better user experience
        highThresholdSeekBar.setProgress((int) (highSpeedThreshold * 2));
        speedThresholdSeekBar.setProgress((int) (speedThreshold * 2));

        // Set listeners for SeekBars
        lowThresholdSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        highThresholdSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        speedThresholdSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        // Set listener for the speed switch
        speedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSpeedControlEnabled = isChecked;
            if (!isSpeedControlEnabled) {
                // If speed control is turned off, stop playing the song
                mediaPlayer.pause();
                isSongPlaying = false;
            }
        });

        registerReceiver(new SpeedReceiver(), new IntentFilter("SPEED_UPDATE"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build())
                    .build();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Request location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates
        locationManager.removeUpdates(locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Update volume, speed display, and handle song playback based on speed
            float speed = location.getSpeed();
            adjustVolume(speed);
            updateSpeedDisplay(speed);
            handleSongPlayback(speed);
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Handle provider enabled
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Handle provider disabled
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Handle status changes if needed
        }
    };

    private final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // Update thresholds based on SeekBar progress
            if (seekBar == lowThresholdSeekBar) {
                lowSpeedThreshold = progress / 2.0f; // Example scaling for better user experience
                updateThresholdLabel(lowThresholdLabel, lowSpeedThreshold, "Low Speed Threshold");
            } else if (seekBar == highThresholdSeekBar) {
                highSpeedThreshold = progress / 2.0f;
                updateThresholdLabel(highThresholdLabel, highSpeedThreshold, "High Speed Threshold");
            } else if (seekBar == speedThresholdSeekBar) {
                speedThreshold = progress / 2.0f;
                updateThresholdLabel( speedThresholdLabel, speedThreshold, "Speed Threshold");
            }
        }

        private void updateThresholdLabel(TextView labelTextView, float threshold, String name) {
            // Update the TextView to display the current threshold value
            labelTextView.setText(name + " : " + convertToKMH(threshold));
        }

        public String convertToKMH(float speed){
            float kmh = speed * KMH;
            return kmh + " KMH";
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Handle tracking start if needed
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Handle tracking stop if needed
        }
    };

    private void adjustVolume(float speed) {
        // Define speed thresholds
        float lowSpeedThreshold = this.lowSpeedThreshold; // Use instance variable
        float highSpeedThreshold = this.highSpeedThreshold;

        // Map speed to volume level
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        // Ensure the volume is within valid bounds
        int adjustedVolume = (int) ((speed - lowSpeedThreshold) / (highSpeedThreshold - lowSpeedThreshold) * maxVolume);

        // Make it less sensitive by adding a multiplier (adjust as needed)
        adjustedVolume *= SENSITIVITY_SCALE; // You can adjust this multiplier

        // Ensure the adjusted volume is within valid bounds
        adjustedVolume = Math.max(1, Math.min(maxVolume, adjustedVolume));

        // Set the volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, adjustedVolume, 0);
    }

    private void handleSongPlayback(float speed) {
        // Adjust this speed threshold based on when you want the song to start playing
        float songStartThreshold = speedThreshold;

        if (isSpeedControlEnabled && speed >= songStartThreshold && !isSongPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Request audio focus using AudioFocusRequest
                if (speed >= songStartThreshold && !isSongPlaying) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Request audio focus using AudioFocusRequest
                        int result = audioManager.requestAudioFocus(audioFocusRequest);
                        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            // Start playing the song
                            mediaPlayer.start();
                            isSongPlaying = true;
                        }
                    } else {
                        // Request audio focus using deprecated method (for devices below Oreo)
                        int result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            // Start playing the song
                            mediaPlayer.start();
                            isSongPlaying = true;
                        }
                    }
                } else if (isSpeedControlEnabled && speed < songStartThreshold && isSongPlaying) {
                    // Resume the music playing from the device
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioManager.abandonAudioFocusRequest(audioFocusRequest);
                    } else {
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                    }

                    // Stop playing the song
                    mediaPlayer.pause();
                    isSongPlaying = false;
                }
            }
        }
    }

        private void updateSpeedDisplay ( float speed){
            // Update the TextView to display the current speed
            speedTextView.setText(getString(R.string.speed_format, speed));
        }

        // Broadcast receiver to receive speed updates
        public class SpeedReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                float speed = intent.getFloatExtra("SPEED", 0.4f);
                float kmh = speed * KMH;
                adjustVolume(speed);
                updateSpeedDisplay(kmh);
                handleSongPlayback(speed);
            }

        }


    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        // Handle audio focus changes if needed
    };

}
