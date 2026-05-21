package com.example.barangaygo.activities;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barangaygo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnSignIn;
    private TextView tvForgotPassword;
    private TextView tabSignIn, tabRegister;
    private LinearLayout tabContainer;

    // Panels
    private LinearLayout panelSignIn, panelRegister;

    // Register cards
    private MaterialCardView cardBarangay, cardResident;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        styleTabContainer();
        setupTabSwitching();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        btnSignIn = findViewById(R.id.btn_signin);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tabSignIn = findViewById(R.id.tab_signin);
        tabRegister = findViewById(R.id.tab_register);
        tabContainer = findViewById(R.id.tab_container);
        panelSignIn = findViewById(R.id.panel_signin);
        panelRegister = findViewById(R.id.panel_register);
        cardBarangay = findViewById(R.id.card_barangay);
        cardResident = findViewById(R.id.card_resident);
    }

    private void styleTabContainer() {
        GradientDrawable tabBg = new GradientDrawable();
        tabBg.setColor(getResources().getColor(R.color.gray_100, getTheme()));
        tabBg.setCornerRadius(dpToPx(12));
        tabContainer.setBackground(tabBg);
    }

    private void setupTabSwitching() {
        tabSignIn.setOnClickListener(v -> switchToSignIn());
        tabRegister.setOnClickListener(v -> switchToRegister());
    }

    private void switchToSignIn() {
        // Update tab visuals
        tabSignIn.setBackgroundResource(R.drawable.bg_tab_active);
        tabSignIn.setTextColor(getResources().getColor(R.color.white, getTheme()));
        tabRegister.setBackground(null);
        tabRegister.setTextColor(getResources().getColor(R.color.gray_600, getTheme()));

        // Show/hide panels
        panelSignIn.setVisibility(View.VISIBLE);
        panelRegister.setVisibility(View.GONE);

        // Update header
        updateAuthHeader(getString(R.string.welcome_back), getString(R.string.sign_in_subtitle));
    }

    private void switchToRegister() {
        // Update tab visuals
        tabRegister.setBackgroundResource(R.drawable.bg_tab_active);
        tabRegister.setTextColor(getResources().getColor(R.color.white, getTheme()));
        tabSignIn.setBackground(null);
        tabSignIn.setTextColor(getResources().getColor(R.color.gray_600, getTheme()));

        // Show/hide panels
        panelSignIn.setVisibility(View.GONE);
        panelRegister.setVisibility(View.VISIBLE);

        // Update header
        updateAuthHeader("Create Account", "Choose how you'd like to join BarangayGo");
    }

    private void updateAuthHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.tv_auth_title);
        TextView tvSubtitle = findViewById(R.id.tv_auth_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> handleSignIn());

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (TextUtils.isEmpty(email)) {
                tilEmail.setError("Enter your email first, then tap Forgot Password");
                return;
            }
            tilEmail.setError(null);
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // Register card clicks → launch their respective registration activities
        cardBarangay.setOnClickListener(v ->
                startActivity(new Intent(this, BarangayRegisterActivity.class))
        );

        cardResident.setOnClickListener(v ->
                startActivity(new Intent(this, ResidentRegisterActivity.class))
        );
    }

    private void handleSignIn() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (!validateInputs(email, password)) return;

        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText(R.string.sign_in);
                    if (task.isSuccessful()) {
                        checkUserRoleAndNavigate();
                    } else {
                        showAuthError(task.getException());
                    }
                });
    }

    private void showAuthError(Exception exception) {
        String code = (exception instanceof FirebaseAuthException)
                ? ((FirebaseAuthException) exception).getErrorCode() : "";
        String msg;
        switch (code) {
            case "ERROR_USER_NOT_FOUND":
            case "ERROR_INVALID_CREDENTIAL":
                msg = "No account found with this email. Please register.";
                tilEmail.setError("Account not found");
                break;
            case "ERROR_WRONG_PASSWORD":
                msg = "Incorrect password. Please try again.";
                tilPassword.setError("Wrong password");
                break;
            case "ERROR_INVALID_EMAIL":
                msg = "Invalid email address.";
                tilEmail.setError("Invalid email");
                break;
            case "ERROR_USER_DISABLED":
                msg = "This account has been disabled. Contact your barangay admin.";
                break;
            case "ERROR_TOO_MANY_REQUESTS":
                msg = "Too many failed attempts. Please wait a moment and try again.";
                break;
            case "ERROR_NETWORK_REQUEST_FAILED":
                msg = "Network error. Please check your internet connection.";
                break;
            default:
                msg = "Sign in failed. Please check your email and password.";
                break;
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            valid = false;
        } else {
            tilEmail.setError(null);
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else {
            tilPassword.setError(null);
        }
        return valid;
    }

    private void checkUserRoleAndNavigate() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if ("admin".equals(role)) {
                            startActivity(new Intent(this, AdminActivity.class));
                        } else {
                            navigateToMain();
                        }
                    } else {
                        navigateToMain();
                    }
                    finish();
                })
                .addOnFailureListener(e -> navigateToMain());
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}