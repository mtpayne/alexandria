package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import it.jaschke.alexandria.ui.camera.CameraSource;

/**
 * Fragment for the barcode snanning in the app.
 * The barcode scanner will detect a barcode and send it to the AddBook Fragment.
 * Referenced android-vision project https://github.com/googlesamples/android-vision.git
 * and http://code.tutsplus.com/tutorials/reading-qr-codes-using-the-mobile-vision-api--cms-24680
 */
public class ScanBarcodeFragment extends Fragment {

    private SurfaceView mCameraSurfaceView;
    private CameraSource mCameraSource;

    /**
     * Interface activities must implement when using this fragment.
     */
    public interface Callback {
        void onBarcodeScanned(String barcode);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan_barcode, container, false);

        mCameraSurfaceView = (SurfaceView) rootView.findViewById(R.id.surfaceview_camera);

        // Get display size for preview
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getActivity()).build();

        // Create and start the camera.
        CameraSource.Builder builder = new CameraSource.Builder(getActivity(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(displaySize.x, displaySize.y)
                .setRequestedFps(15.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        mCameraSource = builder.build();

        mCameraSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCameraSource.start(mCameraSurfaceView.getHolder());
                } catch (IOException e) {
                    Log.e(ScanBarcodeFragment.class.getSimpleName(), "Unable to start camera source.", e);
                    mCameraSource.release();
                    mCameraSource = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }

        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {

            boolean barCodeCaptured = false;

            @Override
            public void release() {}

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    // Only need to callback once
                    if (!barCodeCaptured) {
                        barCodeCaptured = true;
                        ((Callback) getActivity()).onBarcodeScanned(barcodes.valueAt(0).displayValue);
                    }

                }
            }
        });

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(ScanBarcodeFragment.class.getSimpleName(), "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = getActivity().registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                messageIntent.putExtra(MainActivity.MESSAGE_KEY,getResources().getString(R.string.low_storage_error));
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(messageIntent);
                Log.w(ScanBarcodeFragment.class.getSimpleName(), getString(R.string.low_storage_error));
            }
        }

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

}
