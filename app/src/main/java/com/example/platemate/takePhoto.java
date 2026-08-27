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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private NutritionData latestNutrition;
    private String latestAnalysisText;



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
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User is not signed in. Please sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latestNutrition == null || latestNutrition.calories == null) {
            Toast.makeText(this, "Take and analyze a food photo first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Image upload is not complete yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> mealData = new HashMap<>();
        mealData.put("timestamp", new Date());
        mealData.put("imageUrl", imageUrl);
        mealData.put("analysisText", latestAnalysisText);
        if (latestNutrition.summary != null) {
            mealData.put("summary", latestNutrition.summary);
        }
        if (latestNutrition.confidence != null) {
            mealData.put("confidence", latestNutrition.confidence);
        }
        addMetric(mealData, "calories", latestNutrition.calories);
        addMetric(mealData, "protein", latestNutrition.protein);
        addMetric(mealData, "carbohydrates", latestNutrition.carbohydrates);
        addMetric(mealData, "sugars", latestNutrition.sugars);
        addMetric(mealData, "fats", latestNutrition.fats);
        addMetric(mealData, "saturatedFat", latestNutrition.saturatedFat);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .collection("meal_entries")
                .add(mealData)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Meal saved to history.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save meal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        prompt = "You are a nutrition assistant. Analyze the food image and return valid JSON only."
                + " Use this schema: {\"summary\":\"short description\",\"confidence\":\"low|medium|high\","
                + "\"nutrition\":{\"calories\":number,\"protein\":number,\"carbohydrates\":number,"
                + "\"sugars\":number,\"fats\":number,\"saturatedFat\":number}}."
                + " Use estimates per visible serving. Do not include markdown or code fences.";
        cameraButton.setOnClickListener(v -> openCameraActivity());
        analyzeImageWithOpenAI();

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
                Log.d("MainActivity", "Image uploaded successfully URL: " + imageUrl);
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



    private void analyzeImageWithOpenAI() {
        if (apiKey == null || apiKey.isEmpty()) {
            desc.setText("OpenAI API key is missing. Add OPENAI_API_KEY to gradle.properties.");
            return;
        }

        if (imageUrl == null || imageUrl.isEmpty()) {
            desc.setText("Image URL missing. Please retake the photo.");
            return;
        }

        desc.setText("Analyzing image...");
        RequestBody requestBody = OpenAIRequest.createRequestBody(model, prompt, imageUrl);
        OpenAIService service = RetrofitClient.getClient().create(OpenAIService.class);
        String authHeader = "Bearer " + apiKey;
        Call<ResponseBody> call = service.createCompletion(authHeader, requestBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String message = "OpenAI request failed.";
                    try {
                        if (response.errorBody() != null) {
                            message = response.errorBody().string();
                        }
                    } catch (IOException ignored) {
                    }
                    desc.setText("Failed to analyze image.\n" + message);
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    NutritionData nutritionData = parseNutritionResponse(responseBody);
                    latestNutrition = nutritionData;
                    latestAnalysisText = nutritionData.rawText;
                    desc.setText(formatNutritionForDisplay(nutritionData));
                } catch (Exception e) {
                    desc.setText("Failed to parse nutrition response.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                desc.setText("Network error while analyzing image.");
            }
        });
    }

    private NutritionData parseNutritionResponse(String responseBody) throws JSONException {
        JSONObject root = new JSONObject(responseBody);
        JSONArray choices = root.getJSONArray("choices");
        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        String assistantResponse = message.getString("content");
        String normalized = normalizeJsonCandidate(assistantResponse);
        NutritionData data = new NutritionData();
        data.rawText = assistantResponse;

        try {
            JSONObject aiJson = new JSONObject(normalized);
            data.summary = aiJson.optString("summary", null);
            data.confidence = aiJson.optString("confidence", null);
            JSONObject nutrition = aiJson.optJSONObject("nutrition");
            if (nutrition != null) {
                data.calories = readDouble(nutrition, "calories");
                data.protein = readDouble(nutrition, "protein");
                data.carbohydrates = readDouble(nutrition, "carbohydrates");
                data.sugars = readDouble(nutrition, "sugars");
                data.fats = readDouble(nutrition, "fats");
                data.saturatedFat = readDouble(nutrition, "saturatedFat");
            }
        } catch (JSONException parseError) {
            data.calories = extractMetric(assistantResponse, "calories");
            data.protein = extractMetric(assistantResponse, "protein");
            data.carbohydrates = extractMetric(assistantResponse, "carbohydrates");
            data.sugars = extractMetric(assistantResponse, "sugars");
            data.fats = extractMetric(assistantResponse, "fats");
            data.saturatedFat = extractMetric(assistantResponse, "saturated\\s*fat");
        }

        return data;
    }

    private String formatNutritionForDisplay(NutritionData data) {
        StringBuilder builder = new StringBuilder();
        if (data.summary != null && !data.summary.isEmpty()) {
            builder.append("Food: ").append(data.summary).append("\n");
        }
        if (data.confidence != null && !data.confidence.isEmpty()) {
            builder.append("Confidence: ").append(data.confidence).append("\n\n");
        }
        builder.append("Estimated Nutrition (per serving)\n");
        builder.append("Calories: ").append(formatValue(data.calories, "kcal")).append("\n");
        builder.append("Protein: ").append(formatValue(data.protein, "g")).append("\n");
        builder.append("Carbohydrates: ").append(formatValue(data.carbohydrates, "g")).append("\n");
        builder.append("Sugars: ").append(formatValue(data.sugars, "g")).append("\n");
        builder.append("Fats: ").append(formatValue(data.fats, "g")).append("\n");
        builder.append("Saturated Fat: ").append(formatValue(data.saturatedFat, "g")).append("\n");
        return builder.toString();
    }

    private String formatValue(Double value, String unit) {
        if (value == null) {
            return "N/A";
        }
        return String.format("%.1f %s", value, unit);
    }

    private Double extractMetric(String text, String keyPattern) {
        Pattern pattern = Pattern.compile(keyPattern + "[^\\d]*([\\d]+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String normalizeJsonCandidate(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        }
        return cleaned;
    }

    private Double readDouble(JSONObject object, String key) {
        if (!object.has(key)) {
            return null;
        }
        try {
            return object.getDouble(key);
        } catch (JSONException e) {
            return null;
        }
    }

    private void addMetric(Map<String, Object> data, String key, Double value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private static class NutritionData {
        String summary;
        String confidence;
        Double calories;
        Double protein;
        Double carbohydrates;
        Double sugars;
        Double fats;
        Double saturatedFat;
        String rawText;
    }
}
