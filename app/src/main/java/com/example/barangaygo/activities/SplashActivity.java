package com.example.barangaygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barangaygo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_splash);

        // Animate logo and text in
        animateSplashContent();

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                checkRoleAndNavigate(currentUser.getUid());
            } else {
                navigateToLogin();
            }
        }, SPLASH_DURATION);
    }

    private void animateSplashContent() {
        LinearLayout logoContainer = findViewById(R.id.logo_container);
        TextView tvAppName = findViewById(R.id.tv_app_name);
        TextView tvDescription = findViewById(R.id.tv_description);
        LinearLayout loadingGroup = findViewById(R.id.loading_group);

        // Fade + slide up for logo
        AnimationSet logoAnim = new AnimationSet(true);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        TranslateAnimation slideUp = new TranslateAnimation(0, 0, 60f, 0f);
        logoAnim.addAnimation(fadeIn);
        logoAnim.addAnimation(slideUp);
        logoAnim.setDuration(700);
        logoAnim.setFillAfter(true);
        logoContainer.startAnimation(logoAnim);

        // App name - delayed
        AnimationSet nameAnim = new AnimationSet(true);
        AlphaAnimation fadeIn2 = new AlphaAnimation(0f, 1f);
        TranslateAnimation slideUp2 = new TranslateAnimation(0, 0, 40f, 0f);
        nameAnim.addAnimation(fadeIn2);
        nameAnim.addAnimation(slideUp2);
        nameAnim.setDuration(600);
        nameAnim.setStartOffset(300);
        nameAnim.setFillAfter(true);
        tvAppName.startAnimation(nameAnim);

        // Description - more delayed
        AlphaAnimation fadeIn3 = new AlphaAnimation(0f, 1f);
        fadeIn3.setDuration(600);
        fadeIn3.setStartOffset(600);
        fadeIn3.setFillAfter(true);
        tvDescription.startAnimation(fadeIn3);

        // Loading - last
        AlphaAnimation fadeIn4 = new AlphaAnimation(0f, 1f);
        fadeIn4.setDuration(500);
        fadeIn4.setStartOffset(1000);
        fadeIn4.setFillAfter(true);
        loadingGroup.startAnimation(fadeIn4);
    }

    private void checkRoleAndNavigate(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && "admin".equals(doc.getString("role"))) {
                        startActivity(new Intent(this, AdminActivity.class));
                    } else {
                        navigateToMain();
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    navigateToMain();
                    finish();
                });
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
