package com.example.barangaygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barangaygo.R;
import com.google.android.material.card.MaterialCardView;

public class ChooseAccountTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_account_type);

        MaterialCardView cardBarangay = findViewById(R.id.card_barangay);
        MaterialCardView cardResident = findViewById(R.id.card_resident);
        TextView tvSignIn = findViewById(R.id.tv_sign_in);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        cardBarangay.setOnClickListener(v ->
                startActivity(new Intent(this, BarangayRegisterActivity.class)));

        cardResident.setOnClickListener(v ->
                startActivity(new Intent(this, ResidentRegisterActivity.class)));

        tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}