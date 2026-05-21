package com.example.barangaygo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
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
import androidx.core.content.ContextCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.bumptech.glide.Glide;
import com.example.barangaygo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout btnBack, btnEditProfile, btnChangePassword;
    private FrameLayout avatarCircle;
    private ImageView ivAvatarPhoto;
    private TextView tvAvatarInitials, tvProfileName, tvProfileEmail;
    private TextView tvRoleBadge, tvInfoName, tvInfoEmail, tvInfoContact;
    private TextView btnEditContact;
    private LinearLayout bookingsContainer, layoutNoBookings;
    private MaterialButton btnLogout;

    // Bottom nav
    private LinearLayout navHome, navBook, navQueue, navProfile;
    private ImageView navHomeIcon, navBookIcon, navQueueIcon, navProfileIcon;

    private String userId;
    private String currentName = "";
    private String currentContact = "";

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

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
        loadUserProfile();
        loadRecentBookings();
        setupClickListeners();
        setupNavigation();
        setActiveNav(navProfile);
    }

    private void initViews() {
        btnBack           = findViewById(R.id.btn_back);
        btnEditProfile    = findViewById(R.id.btn_edit_profile);
        avatarCircle      = findViewById(R.id.avatar_circle);
        ivAvatarPhoto     = findViewById(R.id.iv_avatar_photo);
        tvAvatarInitials  = findViewById(R.id.tv_avatar_initials);
        tvProfileName     = findViewById(R.id.tv_profile_name);
        tvProfileEmail    = findViewById(R.id.tv_profile_email);
        tvRoleBadge       = findViewById(R.id.tv_role_badge);
        tvInfoName        = findViewById(R.id.tv_info_name);
        tvInfoEmail       = findViewById(R.id.tv_info_email);
        tvInfoContact     = findViewById(R.id.tv_info_contact);
        btnEditContact    = findViewById(R.id.btn_edit_contact);
        btnChangePassword = findViewById(R.id.btn_change_password);
        bookingsContainer = findViewById(R.id.bookings_container);
        layoutNoBookings  = findViewById(R.id.layout_no_bookings);
        btnLogout         = findViewById(R.id.btn_logout);

        navHome    = findViewById(R.id.nav_home);
        navBook    = findViewById(R.id.nav_book);
        navQueue   = findViewById(R.id.nav_queue);
        navProfile = findViewById(R.id.nav_profile);

        navHomeIcon    = findViewById(R.id.nav_home_icon);
        navBookIcon    = findViewById(R.id.nav_book_icon);
        navQueueIcon   = findViewById(R.id.nav_queue_icon);
        navProfileIcon = findViewById(R.id.nav_profile_icon);
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String email = user.getEmail();
        if (email != null) {
            tvProfileEmail.setText(email);
            tvInfoEmail.setText(email);
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name     = doc.getString("name");
                    String contact  = doc.getString("contact");
                    String role     = doc.getString("role");
                    String photoUrl = doc.getString("photoUrl");

                    currentName    = name    != null ? name    : "";
                    currentContact = contact != null ? contact : "";

                    if (!currentName.isEmpty()) {
                        tvProfileName.setText(currentName);
                        tvInfoName.setText(currentName);
                        tvAvatarInitials.setText(buildInitials(currentName));
                    } else if (email != null) {
                        tvProfileName.setText(email);
                        tvAvatarInitials.setText(String.valueOf(email.charAt(0)).toUpperCase());
                    }

                    tvInfoContact.setText(currentContact.isEmpty() ? "Not set" : currentContact);

                    if (role != null) tvRoleBadge.setText(role.toUpperCase());

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

    private void loadRecentBookings() {
        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        layoutNoBookings.setVisibility(View.VISIBLE);
                        return;
                    }
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        addBookingCard(
                                doc.getString("serviceName"),
                                doc.getString("queueNumber"),
                                doc.getString("status"),
                                doc.getString("date"),
                                doc.getString("timeRange"),
                                doc.getId());
                    }
                })
                .addOnFailureListener(e -> layoutNoBookings.setVisibility(View.VISIBLE));
    }

    private void addBookingCard(String serviceName, String queueNumber,
                                String status, String date, String timeRange,
                                String bookingId) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        androidx.cardview.widget.CardView.LayoutParams cardParams =
                new androidx.cardview.widget.CardView.LayoutParams(
                        androidx.cardview.widget.CardView.LayoutParams.MATCH_PARENT,
                        androidx.cardview.widget.CardView.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardParams);
        card.setCardElevation(0);
        card.setRadius(dpToPx(14));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        dotParams.setMargins(0, 0, dpToPx(12), 0);
        dot.setLayoutParams(dotParams);

        int dotColor;
        switch (status != null ? status : "") {
            case "waiting": dotColor = R.color.status_warning; break;
            case "serving": dotColor = R.color.blue_600; break;
            case "done":    dotColor = R.color.status_success; break;
            default:        dotColor = R.color.gray_400;
        }

        android.graphics.drawable.GradientDrawable dotBg =
                new android.graphics.drawable.GradientDrawable();
        dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dotBg.setColor(ContextCompat.getColor(this, dotColor));
        dot.setBackground(dotBg);
        inner.addView(dot);

        LinearLayout infoGroup = new LinearLayout(this);
        infoGroup.setOrientation(LinearLayout.VERTICAL);
        infoGroup.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvService = new TextView(this);
        tvService.setText(serviceName != null ? serviceName : "Unknown service");
        tvService.setTextSize(13);
        tvService.setTypeface(null, android.graphics.Typeface.BOLD);
        tvService.setTextColor(ContextCompat.getColor(this, R.color.gray_800));
        infoGroup.addView(tvService);

        TextView tvDetails = new TextView(this);
        tvDetails.setText((date != null ? date : "") + (timeRange != null ? " · " + timeRange : ""));
        tvDetails.setTextSize(11);
        tvDetails.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
        infoGroup.addView(tvDetails);
        inner.addView(infoGroup);

        LinearLayout rightGroup = new LinearLayout(this);
        rightGroup.setOrientation(LinearLayout.VERTICAL);
        rightGroup.setGravity(Gravity.END);

        TextView tvQueue = new TextView(this);
        tvQueue.setText(queueNumber != null ? queueNumber : "");
        tvQueue.setTextSize(13);
        tvQueue.setTypeface(null, android.graphics.Typeface.BOLD);
        tvQueue.setTextColor(ContextCompat.getColor(this, R.color.blue_600));
        rightGroup.addView(tvQueue);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status != null
                ? status.substring(0, 1).toUpperCase() + status.substring(1) : "");
        tvStatus.setTextSize(11);
        tvStatus.setTextColor(ContextCompat.getColor(this, dotColor));
        rightGroup.addView(tvStatus);

        inner.addView(rightGroup);
        card.addView(inner);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, QueueTicketActivity.class);
            intent.putExtra("bookingId", bookingId);
            startActivity(intent);
        });

        bookingsContainer.addView(card);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        avatarCircle.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnEditContact.setOnClickListener(v -> showEditProfileDialog());

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
    }

    private void showEditProfileDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        container.setPadding(pad, dpToPx(8), pad, 0);

        TextView labelName = new TextView(this);
        labelName.setText("Full Name");
        labelName.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
        labelName.setTextSize(12);
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
        labelContact.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
        labelContact.setTextSize(12);
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

                    tvProfileName.setText(newName);
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
        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        if (navBook != null) navBook.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                .putExtra("show_book", true)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        if (navQueue != null) navQueue.setOnClickListener(v -> {
            startActivity(new Intent(this, MyQueuesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            finish();
        });
        if (navProfile != null) navProfile.setOnClickListener(v -> setActiveNav(navProfile));
    }

    private void setActiveNav(LinearLayout activeNav) {
        LinearLayout[] allNavs   = {navHome, navBook, navQueue, navProfile};
        ImageView[]    allIcons  = {navHomeIcon, navBookIcon, navQueueIcon, navProfileIcon};

        for (int i = 0; i < allNavs.length; i++) {
            LinearLayout nav  = allNavs[i];
            ImageView    icon = allIcons[i];
            if (nav == null) continue;

            LinearLayout iconBg = (LinearLayout) nav.getChildAt(0);
            TextView     label  = (TextView)     nav.getChildAt(1);
            boolean      active = (nav == activeNav);

            if (iconBg != null) {
                if (active) iconBg.setBackgroundResource(R.drawable.bg_nav_active);
                else        iconBg.setBackground(null);
            }
            if (label != null) {
                label.setTextColor(ContextCompat.getColor(this,
                    active ? R.color.blue_600 : R.color.gray_400));
            }
            if (icon != null) {
                icon.setColorFilter(ContextCompat.getColor(this,
                    active ? R.color.blue_600 : R.color.gray_400));
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}