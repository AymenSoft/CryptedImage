package com.aymensoft.cryptedimage;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;

import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.configuration.CameraConfiguration;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.result.WhenDoneListener;
import io.fotoapparat.view.CameraView;

import static io.fotoapparat.log.LoggersKt.fileLogger;
import static io.fotoapparat.log.LoggersKt.logcat;
import static io.fotoapparat.log.LoggersKt.loggers;
import static io.fotoapparat.result.transformer.ResolutionTransformersKt.scaled;
import static io.fotoapparat.selector.AspectRatioSelectorsKt.standardRatio;
import static io.fotoapparat.selector.FlashSelectorsKt.autoFlash;
import static io.fotoapparat.selector.FlashSelectorsKt.autoRedEye;
import static io.fotoapparat.selector.FlashSelectorsKt.off;
import static io.fotoapparat.selector.FlashSelectorsKt.torch;
import static io.fotoapparat.selector.FocusModeSelectorsKt.autoFocus;
import static io.fotoapparat.selector.FocusModeSelectorsKt.continuousFocusPicture;
import static io.fotoapparat.selector.FocusModeSelectorsKt.fixed;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;
import static io.fotoapparat.selector.LensPositionSelectorsKt.front;
import static io.fotoapparat.selector.PreviewFpsRangeSelectorsKt.highestFps;
import static io.fotoapparat.selector.ResolutionSelectorsKt.highestResolution;
import static io.fotoapparat.selector.SelectorsKt.firstAvailable;
import static io.fotoapparat.selector.SensorSensitivitySelectorsKt.highestSensorSensitivity;

public class DialogueCameraActivity extends DialogFragment {



    public Bitmap bitmap;
    private String selectedImagePath;
    private ExifInterface exifObject;
    ImageView imgResult;

    private ImageView imgTakePicture, imgRotateLens, imgCloseCamera;
    CameraView cameraView;
    private Fotoapparat fotoapparat;

    boolean activeCameraBack = true;

    private CameraConfiguration cameraConfiguration = CameraConfiguration
            .builder()
            .photoResolution(standardRatio(
                    highestResolution()
            ))
            .focusMode(firstAvailable(
                    continuousFocusPicture(),
                    autoFocus(),
                    fixed()
            ))
            .flash(firstAvailable(
                    autoRedEye(),
                    autoFlash(),
                    torch(),
                    off()
            ))
            .previewFpsRange(highestFps())
            .sensorSensitivity(highestSensorSensitivity())
            .frameProcessor(new SampleFrameProcessor())
            .build();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_dialogue_camera, container, false);

        cameraView= view.findViewById(R.id.camera_view);
        imgTakePicture= view.findViewById(R.id.img_take_picture);
        imgRotateLens= view.findViewById(R.id.img_rotate_lens);
        imgCloseCamera= view.findViewById(R.id.img_close_camera);

        imgResult= getActivity().findViewById(getArguments().getInt("id"));

        //setup camera
        fotoapparat=createFotoapparat();

        imgRotateLens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
        imgTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        imgCloseCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        return view;
    }

    private Fotoapparat createFotoapparat() {
        return Fotoapparat
                .with(getActivity())
                .into(cameraView)
                .previewScaleType(ScaleType.CenterCrop)
                .lensPosition(back())
                .frameProcessor(new SampleFrameProcessor())
                .logger(loggers(
                        logcat(),
                        fileLogger(getActivity())
                ))
                .cameraErrorCallback(new CameraErrorListener() {
                    @Override
                    public void onError(@NotNull CameraException e) {
                        Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                    }
                })
                .build();
    }

    private class SampleFrameProcessor implements FrameProcessor {
        @Override
        public void process(@NotNull Frame frame) {
            // Perform frame processing, if needed
        }
    }

    private void switchCamera() {
        activeCameraBack = !activeCameraBack;
        fotoapparat.switchTo(
                activeCameraBack ? back() : front(),
                cameraConfiguration
        );
    }

    private void takePicture() {
        PhotoResult photoResult = fotoapparat.takePicture();

        File file = null;
        try {
            file = createImagineFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        photoResult.saveToFile(file);

        Log.e("path",selectedImagePath);

        photoResult
                .toBitmap(scaled(0.25f))
                .whenDone(new WhenDoneListener<BitmapPhoto>() {
                    @Override
                    public void whenDone(@org.jetbrains.annotations.Nullable BitmapPhoto bitmapPhoto) {
                        try {
                            exifObject = new ExifInterface(selectedImagePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        int orientation = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                        bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(selectedImagePath), 700, 700);
                        Bitmap imageRotate = new GetBitmap().rotateBitmap(bitmap,orientation);
                        imgResult.setScaleType(ImageView.ScaleType.FIT_XY);
                        imgResult.setImageBitmap(imageRotate);
                        SavetoGallery();
                        Variables.CRYPTED_IMAGE = new GetBitmap().getStringImage(imageRotate);
                        getDialog().dismiss();
                    }
                });
    }

    private File createImagineFile() throws IOException {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "MyAppName";
        File outputDir= new File(path);
        outputDir.mkdir();

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(new Date());
        String fileName = "MYAPP_" + timeStamp + ".jpg";

        File image = new File(path+File.separator+fileName);

        selectedImagePath = image.getAbsolutePath();

        return image;
    }

    public void SavetoGallery(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, selectedImagePath);
        getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    @Override
    public void onStart() {
        super.onStart();
        fotoapparat.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        fotoapparat.stop();
    }
}
