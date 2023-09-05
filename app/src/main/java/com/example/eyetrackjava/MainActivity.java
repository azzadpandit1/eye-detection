package com.example.eyetrackjava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.eyetrackjava.tracker.FaceTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_HANDLE_GMS = 9001;

    private SurfaceView mCameraView;
    private TextView mFaceReactionTextView;

    private CameraSource mCameraSource = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.surfaceView);
        mFaceReactionTextView = findViewById(R.id.textView);

        int rc = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (rc != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available. Error code: " + rc);
            return;
        }

        createCameraSource();

    }

    private void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(getApplicationContext(), detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker();
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            // Perform face reaction and eye tracking here
            boolean isBothEyesOpen = face.getIsLeftEyeOpenProbability() > 0.5f && face.getIsRightEyeOpenProbability() > 0.5f;
            boolean isLeftEyeOpen = face.getIsLeftEyeOpenProbability() > 0.5f;
            boolean isRightEyeOpen = face.getIsRightEyeOpenProbability() > 0.5f;

            // Update UI with face reaction information
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isBothEyesOpen) {
                        mFaceReactionTextView.setText("Both eyes are open");
                    } else if (isLeftEyeOpen && isRightEyeOpen) {
                        mFaceReactionTextView.setText("Both eyes are closed");
                    } else if (isLeftEyeOpen) {
                        mFaceReactionTextView.setText("Left eye is open, right eye is closed");
                    } else if (isRightEyeOpen) {
                        mFaceReactionTextView.setText("Right eye is open, left eye is closed");
                    } else {
                        mFaceReactionTextView.setText("No face reaction detected");
                    }
                }
            });
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            // Handle missing face detection
        }

        @Override
        public void onDone() {
            // Handle face tracking done
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    private void startCameraSource() {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available. Error code: " + code);
            return;
        }

        if (mCameraSource != null) {
            try {
                mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder surfaceHolder) {
                        try {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            mCameraSource.start(mCameraView.getHolder());
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to start camera source.", e);
                            mCameraSource.release();
                            mCameraSource = null;
                        }
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                        // Handle surface changes if needed
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                        mCameraSource.stop();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


}