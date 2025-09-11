package com.aristeridis.alert_android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Spinner spinnerType;
    private EditText editComment;
    private Button buttonSelectPhoto, buttonSubmit;
    private Uri photoUri;

    private FusedLocationProviderClient fusedLocationClient;
    private double latitude, longitude;

    private FirebaseFirestore db;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.danger_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        spinnerType = findViewById(R.id.spinnerType);
        editComment = findViewById(R.id.editComment);
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        getLocation();

        buttonSelectPhoto.setOnClickListener(v -> openFileChooser());
        buttonSubmit.setOnClickListener(v -> submitReport());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            photoUri = data.getData();
            Toast.makeText(this, "Φωτογραφία επιλέχθηκε!", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void getLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                });
    }

    private void submitReport() {
        String type = spinnerType.getSelectedItem().toString();
        String comment = editComment.getText().toString();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> report = new HashMap<>();
        report.put("type", type);
        report.put("comment", comment);
        report.put("latitude", latitude);
        report.put("longitude", longitude);
        report.put("userId", userId);

        if (photoUri != null) {
            StorageReference fileRef = storageRef.child("reports/" + System.currentTimeMillis() + ".jpg");
            fileRef.putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                report.put("photoUrl", uri.toString());
                                saveReport(report);
                            }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Σφάλμα ανέβασματος φωτογραφίας", Toast.LENGTH_SHORT).show());
        } else {
            saveReport(report);
        }
    }

    private void saveReport(Map<String, Object> report) {
        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Το περιστατικό υποβλήθηκε!", Toast.LENGTH_SHORT).show();
                    editComment.setText("");
                    spinnerType.setSelection(0);
                    photoUri = null;
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Σφάλμα υποβολής", Toast.LENGTH_SHORT).show());
    }
}
