package com.example.barangaygo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barangaygo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class BarangayRegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private int currentStep = 1;
    private Uri selectedLogoUri = null;

    // Step 1 fields
    private TextInputLayout tilBrgyName, tilBrgyCode, tilBrgyAddress, tilMunicipality, tilProvince;
    private TextInputEditText etBrgyName, etBrgyCode, etBrgyAddress, etMunicipality, etProvince;
    private TextInputEditText etBrgyContact, etDescription;
    private ImageView ivLogo;

    // Step 2 fields
    private TextInputLayout tilAdminName, tilAdminEmail, tilAdminPassword, tilAdminConfirm;
    private TextInputEditText etAdminName, etAdminEmail, etAdminPassword, etAdminConfirm;

    private LinearLayout layoutStep1, layoutStep2;
    private MaterialButton btnNext, btnBackStep;
    private View dotStep1, dotStep2;
    private TextView tvStepLabel;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedLogoUri = uri;
                    ivLogo.setImageURI(uri);
                    ivLogo.setPadding(0, 0, 0, 0);
                    ivLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barangay_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        layoutStep1 = findViewById(R.id.layout_step1);
        layoutStep2 = findViewById(R.id.layout_step2);
        btnNext = findViewById(R.id.btn_next);
        btnBackStep = findViewById(R.id.btn_back_step);
        dotStep1 = findViewById(R.id.dot_step1);
        dotStep2 = findViewById(R.id.dot_step2);
        tvStepLabel = findViewById(R.id.tv_step_label);
        ivLogo = findViewById(R.id.iv_logo);

        tilBrgyName = findViewById(R.id.til_brgy_name);
        tilBrgyCode = findViewById(R.id.til_brgy_code);
        tilBrgyAddress = findViewById(R.id.til_brgy_address);
        tilMunicipality = findViewById(R.id.til_municipality);
        tilProvince = findViewById(R.id.til_province);
        etBrgyName = findViewById(R.id.et_brgy_name);
        etBrgyCode = findViewById(R.id.et_brgy_code);
        etBrgyAddress = findViewById(R.id.et_brgy_address);
        etMunicipality = findViewById(R.id.et_municipality);
        etProvince = findViewById(R.id.et_province);
        etBrgyContact = findViewById(R.id.et_brgy_contact);
        etDescription = findViewById(R.id.et_description);

        tilAdminName = findViewById(R.id.til_admin_name);
        tilAdminEmail = findViewById(R.id.til_admin_email);
        tilAdminPassword = findViewById(R.id.til_admin_password);
        tilAdminConfirm = findViewById(R.id.til_admin_confirm);
        etAdminName = findViewById(R.id.et_admin_name);
        etAdminEmail = findViewById(R.id.et_admin_email);
        etAdminPassword = findViewById(R.id.et_admin_password);
        etAdminConfirm = findViewById(R.id.et_admin_confirm);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        FrameLayout frameLogo = findViewById(R.id.frame_logo);
        frameLogo.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {
                if (validateStep1()) goToStep2();
            } else {
                if (validateStep2()) submitRegistration();
            }
        });

        btnBackStep.setOnClickListener(v -> goToStep1());
    }

    private boolean validateStep1() {
        String name = text(etBrgyName);
        String address = text(etBrgyAddress);
        String municipality = text(etMunicipality);
        String province = text(etProvince);

        clearErrors(tilBrgyName, tilBrgyAddress, tilMunicipality, tilProvince);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) { tilBrgyName.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(address)) { tilBrgyAddress.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(municipality)) { tilMunicipality.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(province)) { tilProvince.setError("Required"); valid = false; }
        return valid;
    }

    private boolean validateStep2() {
        String name = text(etAdminName);
        String email = text(etAdminEmail);
        String password = text(etAdminPassword);
        String confirm = text(etAdminConfirm);

        clearErrors(tilAdminName, tilAdminEmail, tilAdminPassword, tilAdminConfirm);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) { tilAdminName.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(email)) { tilAdminEmail.setError("Required"); valid = false; }
        if (password.length() < 6) { tilAdminPassword.setError("At least 6 characters"); valid = false; }
        if (!password.equals(confirm)) { tilAdminConfirm.setError("Passwords do not match"); valid = false; }
        return valid;
    }

    private void goToStep2() {
        currentStep = 2;
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.VISIBLE);
        dotStep1.setBackgroundResource(R.drawable.bg_step_inactive);
        dotStep2.setBackgroundResource(R.drawable.bg_step_active);
        tvStepLabel.setText("Step 2 of 2 — Admin Account");
        btnNext.setText("Create Account");
        btnBackStep.setVisibility(View.VISIBLE);
    }

    private void goToStep1() {
        currentStep = 1;
        layoutStep1.setVisibility(View.VISIBLE);
        layoutStep2.setVisibility(View.GONE);
        dotStep1.setBackgroundResource(R.drawable.bg_step_active);
        dotStep2.setBackgroundResource(R.drawable.bg_step_inactive);
        tvStepLabel.setText("Step 1 of 2 — Barangay Info");
        btnNext.setText("Next →");
        btnBackStep.setVisibility(View.GONE);
    }

    private void submitRegistration() {
        btnNext.setEnabled(false);
        btnNext.setText("Creating...");

        String email = text(etAdminEmail);
        String password = text(etAdminPassword);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || mAuth.getCurrentUser() == null) {
                        btnNext.setEnabled(true);
                        btnNext.setText("Create Account");
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            tilAdminEmail.setError("This email is already registered. Please use a different email.");
                            etAdminEmail.requestFocus();
                        } else if (e instanceof FirebaseAuthWeakPasswordException) {
                            tilAdminPassword.setError("Password is too weak. Use at least 6 characters.");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            tilAdminEmail.setError("Invalid email address format.");
                        } else {
                            Toast.makeText(this, e != null ? e.getMessage() : "Registration failed", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    String uid = mAuth.getCurrentUser().getUid();
                    if (selectedLogoUri != null) {
                        uploadLogoThenSave(uid);
                    } else {
                        saveBarangayData(uid, "");
                    }
                });
    }

    private void uploadLogoThenSave(String uid) {
        StorageReference ref = storage.getReference().child("barangay_logos/" + uid + ".jpg");
        ref.putFile(selectedLogoUri)
                .addOnSuccessListener(snap ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                saveBarangayData(uid, uri.toString()))
                                .addOnFailureListener(e -> saveBarangayData(uid, "")))
                .addOnFailureListener(e -> saveBarangayData(uid, ""));
    }

    private void saveBarangayData(String uid, String logoUrl) {
        Map<String, Object> barangay = new HashMap<>();
        barangay.put("name", text(etBrgyName));
        barangay.put("code", text(etBrgyCode));
        barangay.put("address", text(etBrgyAddress));
        barangay.put("municipality", text(etMunicipality));
        barangay.put("province", text(etProvince));
        barangay.put("contactNumber", text(etBrgyContact));
        barangay.put("email", text(etAdminEmail));
        barangay.put("description", text(etDescription));
        barangay.put("logoUrl", logoUrl);
        barangay.put("adminUid", uid);
        barangay.put("status", "active");
        barangay.put("createdAt", Timestamp.now());

        db.collection("barangays").add(barangay)
                .addOnSuccessListener(docRef -> saveUserData(uid, docRef.getId()))
                .addOnFailureListener(e -> {
                    btnNext.setEnabled(true);
                    btnNext.setText("Create Account");
                    Toast.makeText(this, "Error saving barangay: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserData(String uid, String barangayId) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", text(etAdminName));
        user.put("email", text(etAdminEmail));
        user.put("role", "admin");
        user.put("barangayId", barangayId);
        user.put("createdAt", Timestamp.now());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Barangay registered successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, AdminActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnNext.setEnabled(true);
                    btnNext.setText("Create Account");
                    Toast.makeText(this, "Error saving user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors(TextInputLayout... tils) {
        for (TextInputLayout til : tils) til.setError(null);
    }
}