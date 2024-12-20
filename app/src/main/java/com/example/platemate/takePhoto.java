package com.example.platemate;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import okhttp3.RequestBody;

//YES
public class takePhoto extends BaseActivity {
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageView picture;
    private Uri photoUri;
    private Button cameraButton;
    private TextView desc;
    private String apiKey = BuildConfig.OPENAI_API_KEY;
    private String model;
    private String prompt;
    private String imageUrl;
    private File photoFile;
    private String timeStamp;
    private String imageFileName;
    private FirebaseAuth auth;
    private Button save;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.takephoto);
        setupNavigationHeader();
        picture = findViewById(R.id.pictureID);
        cameraButton = findViewById(R.id.camera_button);
        save = findViewById(R.id.button);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        cameraButton.setOnClickListener(v -> openCameraActivity());
        save.setOnClickListener(v -> saveToFirebase());
        drawerLayout = findViewById(R.id.drawer_layout);


    }
    private void saveToFirebase() {
        /*FirebaseFirestore db = FirebaseFirestore.getInstance();
               Log.d("SAVE_DEBUG", "Starting saveToFirebase...");

        auth = FirebaseAuth.getInstance();
       Log.d("FIRESTORE_INIT", "Firestore instance: " + db);


        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e("AUTH_DEBUG", "No user is signed in.");
            Toast.makeText(this, "User is not signed in. Please sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }else{
            Log.d("SAVE_DEBUG", "user is signed in.");
        }

        String userId = currentUser.getUid();
        Log.d("SAVE_DEBUG", "User ID: " + userId);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", currentUser.getDisplayName());
        userData.put("email", currentUser.getEmail());
        userData.put("phoneNumber", currentUser.getPhoneNumber());
        userData.put("profilePic",currentUser.getPhotoUrl());
        Log.d("SAVE_DEBUG", "Prepared userData: " + userData);
        Log.d("SAVE_DEBUG", "Reached Firestore set() method");

        db.collection(userId).document("Personal Info").set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("SAVE_DEBUG", "Document successfully written!");
                        Toast.makeText(this, "Document saved successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SAVE_DEBUG", "Error writing document: " + e.getMessage());
                        Toast.makeText(this, "Error saving document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });


        Log.d("SAVE_DEBUG", "After calling .set()");*/
        desc = findViewById(R.id.description);
        String whole = desc.getText().toString();
        String calories = whole.substring(whole.indexOf("Calories"), whole.indexOf("kcal")  + 4);
        String protein = whole.substring(whole.indexOf("Protein"), whole.indexOf("g", whole.indexOf("Protein"))  + 1);
        String carbohydrates = whole.substring(whole.indexOf("Carbohydrates"), whole.indexOf("g", whole.indexOf("Carbohydrates"))  + 1);
        String sugars = whole.substring(whole.indexOf("Sugars"), whole.indexOf("g", whole.indexOf("Sugars"))  + 1);
        String fats = whole.substring(whole.indexOf("Fats"), whole.indexOf("g", whole.indexOf("Fats"))  + 1);
        String saturated = whole.substring(whole.indexOf("Saturated Fat"), whole.indexOf("g", whole.indexOf("Saturated Fat"))  + 1);


        Toast.makeText(this, protein, Toast.LENGTH_SHORT).show();
    }



    private void openCameraActivity() {
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        Button takePhotoButton = findViewById(R.id.take_photo_button);
        imageCapture = null;
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error initializing camera", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Error initializing camera", e);
            }
        }, ContextCompat.getMainExecutor(this));

        takePhotoButton.setOnClickListener(v -> takePhoto());
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to bind camera use cases", e);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        photoFile = createImageFile();

        if (photoFile == null) {
            Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap cropped = cropImage(photoFile);
                        File croppedImageFile = null;
                        try {

                            croppedImageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Cropped_Image.jpg");

                            FileOutputStream out = new FileOutputStream(croppedImageFile);
                            cropped.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.flush();
                            out.close();


                        } catch (Exception e) {
                            e.printStackTrace();
                             }


                        photoFile = croppedImageFile;
                        photoUri = FileProvider.getUriForFile(takePhoto.this,
                                "com.example.platemate.fileprovider", photoFile);

                        uploadImageToFirebase(photoUri);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("MainActivity", "Photo capture failed: " + exception.getMessage(), exception);
                        Toast.makeText(takePhoto.this, "Photo capture failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private File createImageFile() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
         timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
         imageFileName = "JPEG_" + timeStamp + "_";
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Log.e("MainActivity", "Failed to create image file", e);
            return null;
        }
    }

    private void displayCapturedImage() {
        setUpNavigationDrawer(R.layout.takephoto);
        setupNavigationHeader();
        picture = findViewById(R.id.pictureID);
        picture.setImageURI(photoUri);
        picture = findViewById(R.id.pictureID);
        cameraButton = findViewById(R.id.camera_button);
        desc = findViewById(R.id.description);
        desc.setMovementMethod(new ScrollingMovementMethod());
        model = "gpt-4o-mini";
        prompt = "You are a nutrition assistant. Analyze food images provided by the user. Identify the food items, estimate portion sizes, and calculate nutritional content. Present the results clearly with headings, bullet points, calories, macronutrients (protein, carbs, fats), and a confidence level. If no food items are detected, respond with: \"⚠\uFE0F No food items detected. Please provide a clear image of food.\"\n";
        //prompt = "Describe the image in few words.";
        //uploadImageToFirebase(photoUri);
        cameraButton.setOnClickListener(v -> openCameraActivity());
        Log.d("MainActivity", "Firebase Image URL GOJO SATORU1: " + imageUrl);
        RequestBody requestBody = OpenAIRequest.createRequestBody(model, prompt, imageUrl);
        OpenAIService service = RetrofitClient.getClient().create(OpenAIService.class);
        String auth = "Bearer " + apiKey;
//        Call<ResponseBody> call = service.createCompletion(auth, requestBody);
//
//
//        call.enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    Toast.makeText(MainActivity.this, "SUCCESS", Toast.LENGTH_SHORT).show();
//                    try {
//
//                        String responseBody = response.body().string();
//                        System.out.println("Response: " + responseBody);
//                        try {
//                            JSONObject jsonObject = new JSONObject(responseBody);
//                            JSONArray choices = jsonObject.getJSONArray("choices");
//                            JSONObject firstChoice = choices.getJSONObject(0);
//                            JSONObject message = firstChoice.getJSONObject("message");
//                            String assistantResponse = message.getString("content");
//                            desc.setText(assistantResponse);
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    try {
//
//                        String errorBody = response.errorBody().string();
//                        Log.e("OpenAIError", "Error Body: INFINITE VOID123 " + errorBody);
//                    } catch (IOException e) {
//                        Log.e("OpenAIError", "Failed to read error body: CHAINSAW " + e.getMessage(), e);
//                    }
//                    Toast.makeText(MainActivity.this, "FAIL", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                t.printStackTrace();
//            }
//        });

    }
    private void uploadImageToFirebase(Uri fileUri) {
        if (fileUri == null) {
            Log.e("MainActivity", "File URI is null");
            Toast.makeText(this, "File URI is null", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        //timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new Date());
        StorageReference imageRef = storageRef.child("images/" +imageFileName + ".jpg");
        Log.d("MainActivity", "Image Reference Path: " + imageRef.getPath());

        UploadTask uploadTask = imageRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                imageUrl = uri.toString();
                displayCapturedImage();
                Log.d("MainActivity", "Image uploaded successfully. ABCDEFG URL: " + imageUrl);
            }).addOnFailureListener(e -> {
                Log.e("MainActivity", "Failed to get download URL", e);
                Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e("MainActivity", "Failed to upload image", e);
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        });
    }
    private Bitmap cropImage(File imageFile) {
        try {

            Bitmap fullImage = BitmapFactory.decodeFile(imageFile.getAbsolutePath());


            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(rotation);
            fullImage = rotateImage(fullImage, rotationInDegrees);


            float density = getResources().getDisplayMetrics().density;


            int boxWidth = (int) (600 * density);
            int boxHeight = (int) (600 * density);


            int boxLeft = (fullImage.getWidth() - boxWidth) / 2;
            int boxTop = (fullImage.getHeight() - boxHeight) / 2;


            boxLeft = Math.max(0, boxLeft);
            boxTop = Math.max(0, boxTop);
            boxWidth = Math.min(boxWidth, fullImage.getWidth() - boxLeft);
            boxHeight = Math.min(boxHeight, fullImage.getHeight() - boxTop);


            Bitmap croppedBitmap = Bitmap.createBitmap(fullImage, boxLeft, boxTop, boxWidth, boxHeight);

            return croppedBitmap;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error cropping image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }


    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }



}
