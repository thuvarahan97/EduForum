package com.thuvarahan.eduforum.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.huawei.hms.image.vision.crop.CropLayoutView;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.post.Post;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageActivity extends AppCompatActivity {

    private LinearLayout btnCrop;
    private LinearLayout btnFlipVertically;
    private LinearLayout btnFlipHorizontally;
    private LinearLayout btnRotateClockwise;
    private LinearLayout btnRotateAntiClockwise;
    private CropLayoutView cropLayoutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        cropLayoutView = findViewById(R.id.cropImageView);
        btnCrop = findViewById(R.id.btnCrop);
        btnFlipVertically = findViewById(R.id.btnFlipVertically);
        btnFlipHorizontally = findViewById(R.id.btnFlipHorizontally);
        btnRotateClockwise = findViewById(R.id.btnRotateClockwise);
//        btnRotateAntiClockwise = findViewById(R.id.btnRotateAntiClockwise);

        /*byte[] inputByteArray = getIntent().getByteArrayExtra("image");
        Bitmap bitmap = BitmapFactory.decodeByteArray(inputByteArray, 0, inputByteArray.length);*/

        Bundle bundle = getIntent().getExtras();
        Uri filePath = Uri.parse(bundle.getString("imagePath"));

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore
            .Images
            .Media
            .getBitmap(
                getContentResolver(),
                filePath
            );
            cropLayoutView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to load the image", Toast.LENGTH_LONG).show();
            Intent cancelledIntent = new Intent();
            setResult(RESULT_CANCELED, cancelledIntent);
            finish();
        }

        btnFlipVertically.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropLayoutView.flipImageVertically();
            }
        });

        btnFlipHorizontally.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropLayoutView.flipImageHorizontally();
            }
        });

        /*btnRotateAntiClockwise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });*/

        btnRotateClockwise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropLayoutView.rotateClockwise();
            }
        });

        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap croppedImage = cropLayoutView.getCroppedImage();
                Bitmap resizedImage = getResizedBitmap(croppedImage);
                String outputImagePath = createImageFromBitmap(resizedImage);

                /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
                resizedImage.compress(Bitmap.CompressFormat.JPEG, 5, stream);
                byte[] outputByteArray = stream.toByteArray();*/

                Intent output = new Intent();
                output.putExtra("imagePath", outputImagePath);
                setResult(RESULT_OK, output);
                finish();
            }
        });
    }

    public Bitmap getResizedBitmap(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int maxSize = 1080;

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public String createImageFromBitmap(Bitmap image) {
        String fileName = "temp_img";
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            FileOutputStream fo = openFileOutput(fileName, Context.MODE_PRIVATE);
            fo.write(bytes.toByteArray());
            // remember close file output
            fo.close();
        } catch (Exception e) {
            e.printStackTrace();
            fileName = null;
        }
        return fileName;
    }
}