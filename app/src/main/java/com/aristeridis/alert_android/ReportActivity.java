package com.aristeridis.alert_android;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int LOCATION_PERMISSION_REQUEST = 102;

    private Spinner riskTypeSpinner;
    private EditText commentEditText;
    private ImageView photoImageView;
    private Button selectPhotoButton, submitButton;

    private Uri selectedImageUri;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        riskTypeSpinner = findViewById(R.id.spinnerRiskType);
        commentEditText = findViewById(R.id.editTextComment);
        photoImageView = findViewById(R.id.imageViewPhoto);
        selectPhotoButton = findViewById(R.id.buttonSelectPhoto);
        submitButton = findViewById(R.id.buttonSubmit);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermission();

        selectPhotoButton.setOnClickListener(v -> openGallery());
        submitButton.setOnClickListener(v -> submitReport());
    }
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                        }
                    });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            photoImageView.setImageURI(selectedImageUri);
        }
    }

    private void submitReport() {
        String riskType = riskTypeSpinner.getSelectedItem().toString();
        String comment = commentEditText.getText().toString();
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a photo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentLocation == null) {
            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting report...");
        progressDialog.show();

        String uid = mAuth.getCurrentUser().getUid();
        String fileName = "reports/" + uid + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Map<String, Object> reportData = new HashMap<>();
                    reportData.put("userId", uid);
                    reportData.put("riskType", riskType);
                    reportData.put("comment", comment);
                    reportData.put("photoUrl", uri.toString());
                    reportData.put("latitude", currentLocation.getLatitude());
                    reportData.put("longitude", currentLocation.getLongitude());
                    reportData.put("timestamp", System.currentTimeMillis());

                    db.collection("reports")
                            .add(reportData)
                            .addOnSuccessListener(documentReference -> {
                                progressDialog.dismiss();
                                Toast.makeText(ReportActivity.this, "Report submitted!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(ReportActivity.this, "Error submitting report", Toast.LENGTH_SHORT).show();
                            });

                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportActivity.this, "Error uploading photo", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
