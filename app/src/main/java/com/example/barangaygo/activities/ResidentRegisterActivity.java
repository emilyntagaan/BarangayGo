package com.example.barangaygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.adapters.BarangayPickerAdapter;
import com.example.barangaygo.models.Barangay;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResidentRegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private int currentStep = 1;
    private Barangay selectedBarangay = null;

    private LinearLayout layoutStep1;
    private ScrollView layoutStep2;
    private TextView tvLoading, tvEmpty, tvStepLabel, tvSelectedBarangay;
    private RecyclerView rvBarangays;
    private View dotStep1, dotStep2;

    private MaterialButton btnNext, btnBackStep;
    private TextInputEditText etSearch;

    // Step 2 fields
    private TextInputLayout tilName, tilContact, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etName, etContact, etAddress, etEmail, etPassword, etConfirm;

    private BarangayPickerAdapter adapter;
    private final List<Barangay> barangayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resident_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        loadBarangays();
    }

    private void initViews() {
        layoutStep1 = findViewById(R.id.layout_step1);
        layoutStep2 = findViewById(R.id.layout_step2);
        tvLoading = findViewById(R.id.tv_loading);
        tvEmpty = findViewById(R.id.tv_empty);
        tvStepLabel = findViewById(R.id.tv_step_label);
        tvSelectedBarangay = findViewById(R.id.tv_selected_barangay);
        rvBarangays = findViewById(R.id.rv_barangays);
        dotStep1 = findViewById(R.id.dot_step1);
        dotStep2 = findViewById(R.id.dot_step2);
        btnNext = findViewById(R.id.btn_next);
        btnNext.setEnabled(false);
        btnBackStep = findViewById(R.id.btn_back_step);
        etSearch = findViewById(R.id.et_search);

        tilName = findViewById(R.id.til_name);
        tilContact = findViewById(R.id.til_contact);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirm = findViewById(R.id.til_confirm);
        etName = findViewById(R.id.et_name);
        etContact = findViewById(R.id.et_contact);
        etAddress = findViewById(R.id.et_address);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirm = findViewById(R.id.et_confirm);

        adapter = new BarangayPickerAdapter(this, barangayList, barangay -> {
            selectedBarangay = barangay;
            btnNext.setEnabled(true);
        });
        rvBarangays.setLayoutManager(new LinearLayoutManager(this));
        rvBarangays.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {
                if (selectedBarangay != null) goToStep2();
                else Toast.makeText(this, "Please select a barangay", Toast.LENGTH_SHORT).show();
            } else {
                if (validateStep2()) submitRegistration();
            }
        });

        btnBackStep.setOnClickListener(v -> goToStep1());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString());
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBarangays() {
        tvLoading.setVisibility(View.VISIBLE);
        rvBarangays.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously()
                .addOnSuccessListener(result -> fetchBarangaysFromFirestore())
                .addOnFailureListener(e -> fetchBarangaysFromFirestore()); // barangays are public; works even if anon auth is off
        } else {
            fetchBarangaysFromFirestore();
        }
    }

    private void fetchBarangaysFromFirestore() {
        db.collection("barangays")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(query -> {
                    barangayList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Barangay b = doc.toObject(Barangay.class);
                        b.setId(doc.getId());
                        barangayList.add(b);
                    }
                    tvLoading.setVisibility(View.GONE);
                    if (barangayList.isEmpty()) {
                        tvEmpty.setText("No registered barangays yet");
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.updateData(barangayList);
                        rvBarangays.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvLoading.setVisibility(View.GONE);
                    tvEmpty.setText("Failed to load barangays: " + e.getMessage());
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            rvBarangays.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvBarangays.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void goToStep2() {
        currentStep = 2;
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.VISIBLE);
        dotStep1.setBackgroundResource(R.drawable.bg_step_inactive);
        dotStep2.setBackgroundResource(R.drawable.bg_step_active);
        tvStepLabel.setText("Step 2 of 2 — Personal Info");
        tvSelectedBarangay.setText(selectedBarangay.getName()
                + " — " + selectedBarangay.getDisplayLocation());
        btnNext.setEnabled(true);
        btnNext.setText("Create Account");
        btnBackStep.setVisibility(View.VISIBLE);
    }

    private void goToStep1() {
        currentStep = 1;
        layoutStep1.setVisibility(View.VISIBLE);
        layoutStep2.setVisibility(View.GONE);
        dotStep1.setBackgroundResource(R.drawable.bg_step_active);
        dotStep2.setBackgroundResource(R.drawable.bg_step_inactive);
        tvStepLabel.setText("Step 1 of 2 — Select Barangay");
        btnNext.setEnabled(selectedBarangay != null);
        btnNext.setText("Next →");
        btnBackStep.setVisibility(View.GONE);
    }

    private boolean validateStep2() {
        String name = text(etName);
        String contact = text(etContact);
        String email = text(etEmail);
        String password = text(etPassword);
        String confirm = text(etConfirm);

        clearErrors(tilName, tilContact, tilEmail, tilPassword, tilConfirm);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) { tilName.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(contact)) { tilContact.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(email)) { tilEmail.setError("Required"); valid = false; }
        if (password.length() < 6) { tilPassword.setError("At least 6 characters"); valid = false; }
        if (!password.equals(confirm)) { tilConfirm.setError("Passwords do not match"); valid = false; }
        return valid;
    }

    private void submitRegistration() {
        btnNext.setEnabled(false);
        btnNext.setText("Creating...");

        String email = text(etEmail);
        String password = text(etPassword);

        // Sign out the anonymous session used for barangay loading before creating the real account
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isAnonymous()) {
            mAuth.signOut();
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || mAuth.getCurrentUser() == null) {
                        btnNext.setEnabled(true);
                        btnNext.setText("Create Account");
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String uid = mAuth.getCurrentUser().getUid();
                    saveUserData(uid);
                });
    }

    private void saveUserData(String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", text(etName));
        user.put("email", text(etEmail));
        user.put("contact", text(etContact));
        user.put("address", text(etAddress));
        user.put("role", "resident");
        user.put("barangayId", selectedBarangay.getId());
        user.put("createdAt", Timestamp.now());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account created! Welcome to " + selectedBarangay.getName(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnNext.setEnabled(true);
                    btnNext.setText("Create Account");
                    Toast.makeText(this, "Error saving account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors(TextInputLayout... tils) {
        for (TextInputLayout til : tils) til.setError(null);
    }
}