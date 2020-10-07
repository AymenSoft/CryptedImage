package com.aymensoft.cryptedimage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private Bitmap bitmap;
    private String selectedImagePath;
    private ExifInterface exifObject;

    private ImageView imgPicture;
    private Button btnGallery, btnCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgPicture = findViewById(R.id.img_picture);
        btnGallery = findViewById(R.id.btn_gallery);
        btnCamera = findViewById(R.id.btn_camera);

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionsGranted()) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                }
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionsGranted()) {
                    DialogFragment camera = new DialogueCameraActivity();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", imgPicture.getId());
                    camera.setArguments(bundle);
                    camera.show(getSupportFragmentManager(), "camera");
                }
            }
        });

    }

    private boolean permissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(MainActivity.this, READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(MainActivity.this, WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{CAMERA, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},1);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 2) {
                Uri capturedImageUri = data.getData();
                selectedImagePath = new GetBitmap().getRealPathFromURIPath(capturedImageUri, MainActivity.this);
                try {
                    exifObject = new ExifInterface(selectedImagePath);// NOPMD
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int orientations = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);// NOPMD
                bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(selectedImagePath), 700, 700);// NOPMD
                Bitmap imageRotates = new GetBitmap().rotateBitmap(bitmap, orientations);
                Variables.CRYPTED_IMAGE = new GetBitmap().getStringImage(imageRotates);
                imgPicture.setImageBitmap(imageRotates);
            }

        }
    }

}