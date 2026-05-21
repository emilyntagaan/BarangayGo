package com.example.barangaygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.barangaygo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ServiceDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private TextView tvServiceName, tvServiceDescription;
    private TextView tvEstTime, tvAvailability;
    private LinearLayout requirementsContainer;
    private ImageView ivServiceIcon;
    private MaterialButton btnBookNow;
    private LinearLayout btnBack;

    private String serviceId;
    private String serviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        db = FirebaseFirestore.getInstance();

        // Get data passed from MainActivity
        serviceId = getIntent().getStringExtra("serviceId");
        serviceName = getIntent().getStringExtra("serviceName");

        initViews();
        setupBackButton();
        loadServiceDetails();
    }

    private void initViews() {
        tvServiceName = findViewById(R.id.tv_service_name);
        tvServiceDescription = findViewById(R.id.tv_service_description);
        tvEstTime = findViewById(R.id.tv_est_time);
        tvAvailability = findViewById(R.id.tv_availability);
        requirementsContainer = findViewById(R.id.requirements_container);
        ivServiceIcon = findViewById(R.id.iv_service_icon);
        btnBookNow = findViewById(R.id.btn_book_now);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadServiceDetails() {
        if (serviceId == null || serviceId.isEmpty()) {
            // No ID passed — show name at least
            if (serviceName != null) tvServiceName.setText(serviceName);
            return;
        }

        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String description = doc.getString("description");
                        Long estimatedMinutes = doc.getLong("estimatedMinutes");
                        String timeUnit = doc.getString("timeUnit");
                        Boolean isAvailable = doc.getBoolean("isAvailable");
                        List<String> requirements = (List<String>) doc.get("requirements");

                        if (name != null) tvServiceName.setText(name);
                        if (description != null) tvServiceDescription.setText(description);

                        if (estimatedMinutes != null && estimatedMinutes > 0) {
                            String timeStr;
                            if ("hr".equals(timeUnit)) {
                                long hrs = estimatedMinutes / 60;
                                long mins = estimatedMinutes % 60;
                                timeStr = mins > 0 ? hrs + " hr " + mins + " min" : hrs + " hr";
                            } else {
                                timeStr = estimatedMinutes + " min";
                            }
                            tvEstTime.setText(timeStr);
                        }

                        if (isAvailable != null) {
                            if (isAvailable) {
                                tvAvailability.setText("Available");
                                tvAvailability.setTextColor(
                                        ContextCompat.getColor(this, R.color.status_success));
                                btnBookNow.setEnabled(true);
                            } else {
                                tvAvailability.setText("Unavailable");
                                tvAvailability.setTextColor(
                                        ContextCompat.getColor(this, R.color.status_danger));
                                btnBookNow.setEnabled(false);
                                btnBookNow.setText("Currently Unavailable");
                            }
                        }

                        if (requirements != null) {
                            populateRequirements(requirements);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        android.widget.Toast.makeText(this,
                                "Failed to load service details", android.widget.Toast.LENGTH_SHORT).show()
                );

        // Book Now button
        btnBookNow.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookQueueActivity.class);
            intent.putExtra("serviceId", serviceId);
            intent.putExtra("serviceName",
                    tvServiceName.getText().toString());
            startActivity(intent);
        });
    }

    private void populateRequirements(List<String> requirements) {
        requirementsContainer.removeAllViews();

        for (int i = 0; i < requirements.size(); i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dpToPx(10));
            row.setLayoutParams(rowParams);

            // Bullet dot
            View dot = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    dpToPx(7), dpToPx(7));
            dotParams.setMargins(0, 0, dpToPx(12), 0);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundResource(R.drawable.bg_step_circle);
            row.addView(dot);

            // Requirement text
            TextView tv = new TextView(this);
            tv.setText(requirements.get(i));
            tv.setTextSize(13);
            tv.setTextColor(ContextCompat.getColor(this, R.color.gray_800));
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(tvParams);
            row.addView(tv);

            requirementsContainer.addView(row);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}