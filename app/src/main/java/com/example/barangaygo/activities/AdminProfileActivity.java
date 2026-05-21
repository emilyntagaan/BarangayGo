package com.example.barangaygo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.bumptech.glide.Glide;
import com.example.barangaygo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private FrameLayout avatarCircle;
    private ImageView ivAvatarPhoto;
    private TextView tvAvatarInitials, tvAdminName, tvAdminEmail, tvAdminRole;
    private TextView tvInfoName, tvInfoEmail, tvInfoRole, tvInfoContact;
    private TextView tvTodayServed, tvTodayWaiting;
    private MaterialButton btnLogout;
    private LinearLayout btnBack, btnEditProfile, btnChangePassword;

    private String userId;
    private String currentName = "";
    private String currentContact = "";

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = user.getUid();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) uploadProfilePhoto(uri); }
        );

        initViews();
        loadAdminProfile(user);
        loadTodayStats();
        setupClickListeners();
    }

    private void initViews() {
        btnBack           = findViewById(R.id.btn_back);
        btnEditProfile    = findViewById(R.id.btn_edit_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);
        avatarCircle      = findViewById(R.id.avatar_circle);
        ivAvatarPhoto     = findViewById(R.id.iv_avatar_photo);
        tvAvatarInitials  = findViewById(R.id.tv_avatar_initials);
        tvAdminName       = findViewById(R.id.tv_admin_name);
        tvAdminEmail      = findViewById(R.id.tv_admin_email);
        tvAdminRole       = findViewById(R.id.tv_admin_role);
        tvInfoName        = findViewById(R.id.tv_info_name);
        tvInfoEmail       = findViewById(R.id.tv_info_email);
        tvInfoRole        = findViewById(R.id.tv_info_role);
        tvInfoContact     = findViewById(R.id.tv_info_contact);
        tvTodayServed     = findViewById(R.id.tv_today_served);
        tvTodayWaiting    = findViewById(R.id.tv_today_waiting);
        btnLogout         = findViewById(R.id.btn_logout);
    }

    private void loadAdminProfile(FirebaseUser user) {
        String email = user.getEmail();
        if (email != null) {
            tvAdminEmail.setText(email);
            tvInfoEmail.setText(email);
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener(doc -> {
                String name     = doc.getString("name");
                String contact  = doc.getString("contact");
                String role     = doc.getString("role");
                String photoUrl = doc.getString("photoUrl");

                currentName    = name    != null ? name    : "";
                currentContact = contact != null ? contact : "";

                if (!currentName.isEmpty()) {
                    tvAdminName.setText(currentName);
                    tvInfoName.setText(currentName);
                    tvAvatarInitials.setText(buildInitials(currentName));
                } else if (email != null) {
                    tvAdminName.setText(email);
                    tvAvatarInitials.setText(String.valueOf(email.charAt(0)).toUpperCase());
                }

                tvInfoContact.setText(currentContact.isEmpty() ? "Not set" : currentContact);

                if (role != null && !role.isEmpty()) {
                    String roleDisplay = role.substring(0, 1).toUpperCase()
                                      + role.substring(1).toLowerCase();
                    tvAdminRole.setText(role.toUpperCase());
                    tvInfoRole.setText(roleDisplay);
                }

                String photoBase64 = doc.getString("photoBase64");
                if (photoBase64 != null && !photoBase64.isEmpty()) {
                    loadAvatarBase64(photoBase64);
                } else if (photoUrl != null && !photoUrl.isEmpty()) {
                    loadAvatarPhoto(photoUrl);
                }
            });
    }

    private void loadAvatarPhoto(String url) {
        Glide.with(this)
                .load(url)
                .circleCrop()
                .into(ivAvatarPhoto);
        ivAvatarPhoto.setVisibility(View.VISIBLE);
        tvAvatarInitials.setVisibility(View.GONE);
    }

    private void loadAvatarBase64(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Glide.with(this).load(bitmap).circleCrop().into(ivAvatarPhoto);
        ivAvatarPhoto.setVisibility(View.VISIBLE);
        tvAvatarInitials.setVisibility(View.GONE);
    }

    private void uploadProfilePhoto(Uri uri) {
        Toast.makeText(this, "Saving photo…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                InputStream in = getContentResolver().openInputStream(uri);
                Bitmap original = BitmapFactory.decodeStream(in);
                if (in != null) in.close();

                Bitmap scaled = Bitmap.createScaledBitmap(original, 150, 150, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                runOnUiThread(() ->
                    db.collection("users").document(userId)
                        .update("photoBase64", base64)
                        .addOnSuccessListener(aVoid -> {
                            loadAvatarBase64(base64);
                            Toast.makeText(this, "Photo updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save photo: " + e.getMessage(), Toast.LENGTH_LONG).show())
                );
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String buildInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (String.valueOf(parts[0].charAt(0))
                    + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase();
        }
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }

    private void loadTodayStats() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Timestamp tsStart = new Timestamp(start.getTimeInMillis() / 1000L, 0);

        db.collection("bookings")
            .whereEqualTo("status", "done")
            .whereGreaterThanOrEqualTo("createdAt", tsStart)
            .get()
            .addOnSuccessListener(snap -> tvTodayServed.setText(String.valueOf(snap.size())));

        db.collection("bookings")
            .whereEqualTo("status", "waiting")
            .get()
            .addOnSuccessListener(snap -> tvTodayWaiting.setText(String.valueOf(snap.size())));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        avatarCircle.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnChangePassword.setOnClickListener(v -> showChangePasswordConfirm());

        btnLogout.setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show()
        );

        setupNavigation();
    }

    private void showEditProfileDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        container.setPadding(pad, dpToPx(8), pad, 0);

        TextView labelName = new TextView(this);
        labelName.setText("Full Name");
        labelName.setTextSize(12);
        labelName.setTextColor(getResources().getColor(R.color.gray_400, getTheme()));
        container.addView(labelName);

        EditText etName = new EditText(this);
        etName.setHint("Enter full name");
        etName.setText(currentName);
        etName.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etName.setPadding(0, dpToPx(8), 0, dpToPx(16));
        container.addView(etName);

        TextView labelContact = new TextView(this);
        labelContact.setText("Contact Number");
        labelContact.setTextSize(12);
        labelContact.setTextColor(getResources().getColor(R.color.gray_400, getTheme()));
        container.addView(labelContact);

        EditText etContact = new EditText(this);
        etContact.setHint("09XXXXXXXXX");
        etContact.setText(currentContact);
        etContact.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        etContact.setPadding(0, dpToPx(8), 0, 0);
        container.addView(etContact);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName    = etName.getText().toString().trim();
                    String newContact = etContact.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveProfileChanges(newName, newContact);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveProfileChanges(String newName, String newContact) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("contact", newContact);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    currentName    = newName;
                    currentContact = newContact;

                    tvAdminName.setText(newName);
                    tvInfoName.setText(newName);
                    tvAvatarInitials.setText(buildInitials(newName));
                    tvInfoContact.setText(newContact.isEmpty() ? "Not set" : newContact);

                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show());
    }

    private void showChangePasswordConfirm() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String email = user.getEmail();

        new android.app.AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("A password reset link will be sent to:\n\n" + email)
                .setPositiveButton("Send Link", (dialog, which) ->
                        mAuth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this,
                                                "Reset link sent to " + email,
                                                Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Failed to send reset email.",
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupNavigation() {
        LinearLayout navDashboard = findViewById(R.id.nav_dashboard);
        LinearLayout navQueue     = findViewById(R.id.nav_queue);
        LinearLayout navServices  = findViewById(R.id.nav_services);
        LinearLayout navProfile   = findViewById(R.id.nav_profile);

        if (navDashboard != null)
            navDashboard.setOnClickListener(v ->
                startActivity(new Intent(this, AdminActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP)));

        if (navQueue != null)
            navQueue.setOnClickListener(v ->
                startActivity(new Intent(this, AdminActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("show_queue", true)));

        if (navServices != null)
            navServices.setOnClickListener(v ->
                startActivity(new Intent(this, AdminServicesActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));

        if (navProfile != null)
            navProfile.setOnClickListener(v -> { /* already here */ });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}