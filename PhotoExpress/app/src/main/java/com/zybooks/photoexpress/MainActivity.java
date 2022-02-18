package com.zybooks.photoexpress;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private File mPhotoFile;
    private ImageView mPhotoImageView;
    private SeekBar mSeekBar;
    private Button mSaveButton;

    // For adding brightness
    private int mMultColor = 0xffffffff;
    private int mAddColor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPhotoImageView = findViewById(R.id.photo);

        mSaveButton = findViewById(R.id.saveButton);
        mSaveButton.setEnabled(false);

        mSeekBar = findViewById(R.id.brightnessSeekBar);
        mSeekBar.setVisibility(View.INVISIBLE);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                changeBrightness(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void takePhotoClick(View view) {

        // Create the File for saving the photo
        mPhotoFile = createImageFile();

        // Create a content URI to grant camera app write permission to mPhotoFile
        Uri photoUri = FileProvider.getUriForFile(this,
                "com.zybooks.photoexpress.fileprovider", mPhotoFile);

        // Start camera app
        mTakePicture.launch(photoUri);
    }

    private final ActivityResultLauncher<Uri> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    displayPhoto();
                    mSeekBar.setProgress(100);
                    mSeekBar.setVisibility(View.VISIBLE);
                    mSaveButton.setEnabled(true);
                }
            });

    private File createImageFile() {
        // Create a unique image filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFilename = "photo_" + timeStamp + ".jpg";

        // Get file path where the app can save a private image
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(storageDir, imageFilename);
    }

    private void displayPhoto() {
        // Get ImageView dimensions
        int targetWidth = mPhotoImageView.getWidth();
        int targetHeight = mPhotoImageView.getHeight();

        // Get bitmap dimensions
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath(), bmOptions);
        int photoWidth = bmOptions.outWidth;
        int photoHeight = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);

        // Decode the image file into a smaller bitmap that fills the ImageView
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath(), bmOptions);

        // Display smaller bitmap
        mPhotoImageView.setImageBitmap(bitmap);
    }

    private void changeBrightness(int brightness) {
        // 100 is the middle value
        if (brightness > 100) {
            // Add color
            float addMult = brightness / 100.0f - 1;
            mAddColor = Color.argb(255, (int) (255 * addMult), (int) (255 * addMult),
                    (int) (255 * addMult));
            mMultColor = 0xffffffff;
        }
        else {
            // Scale color down
            float brightMult = brightness / 100.0f;
            mMultColor = Color.argb(255, (int) (255 * brightMult), (int) (255 * brightMult),
                    (int) (255 * brightMult));
            mAddColor = 0;
        }

        LightingColorFilter colorFilter = new LightingColorFilter(mMultColor, mAddColor);
        mPhotoImageView.setColorFilter(colorFilter);
    }

    public void savePhotoClick(View view) {

        // Don't allow Save button to be pressed while image is saving
        mSaveButton.setEnabled(false);

        // Save image in background thread
        ImageSaver imageSaver = new ImageSaver(this);
        imageSaver.saveAlteredPhotoAsync(mPhotoFile, mMultColor, mAddColor, result -> {

            // Show appropriate message
            int message = result ? R.string.photo_saved : R.string.photo_not_saved;
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

            // Allow Save button to be clicked again
            mSaveButton.setEnabled(true);
        });
    }
}