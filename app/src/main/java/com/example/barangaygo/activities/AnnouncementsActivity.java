package com.example.barangaygo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.adapters.AnnouncementAdapter;
import com.example.barangaygo.models.Announcement;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnouncementsActivity extends AppCompatActivity {

    public static final String EXTRA_ADMIN_MODE = "adminMode";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration listener;
    private String barangayId = null;

    private RecyclerView rvAnnouncements;
    private LinearLayout layoutEmpty;
    private TextView tvEmptySub;
    private AnnouncementAdapter adapter;
    private boolean adminMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        adminMode = getIntent().getBooleanExtra(EXTRA_ADMIN_MODE, false);

        rvAnnouncements = findViewById(R.id.rv_announcements);
        layoutEmpty     = findViewById(R.id.layout_empty);
        tvEmptySub      = findViewById(R.id.tv_empty_sub);

        LinearLayout btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // FAB — only in admin mode
        FloatingActionButton fab = findViewById(R.id.fab_add_announcement);
        if (fab != null) {
            if (adminMode) {
                fab.setVisibility(View.VISIBLE);
                fab.setOnClickListener(v -> showAnnouncementDialog(null));
            } else {
                fab.setVisibility(View.GONE);
            }
        }

        // Update empty state subtitle for admin
        if (adminMode && tvEmptySub != null) {
            tvEmptySub.setText("Tap the + button below to post your first announcement.");
        }

        setupRecyclerView();
        loadBarangayAndListen();
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter(this);
        adapter.setAdminMode(adminMode);
        adapter.setOnDeleteListener(this::confirmDelete);
        adapter.setOnEditListener(this::showAnnouncementDialog);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        rvAnnouncements.setNestedScrollingEnabled(false);
        rvAnnouncements.setAdapter(adapter);
    }

    // ─── Firestore ───────────────────────────────────────────────────────────

    private void loadBarangayAndListen() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                barangayId = doc.getString("barangayId");
                if (barangayId != null) startListener();
            });
    }

    private void startListener() {
        listener = db.collection("announcements")
            .whereEqualTo("barangayId", barangayId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;

                List<Announcement> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Announcement a = doc.toObject(Announcement.class);
                    a.setId(doc.getId());
                    list.add(a);
                }
                Collections.sort(list, (a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });

                adapter.setItems(list);
                layoutEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                rvAnnouncements.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
            });
    }

    // ─── Add / Edit dialog ───────────────────────────────────────────────────

    private void showAnnouncementDialog(Announcement existing) {
        View dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_post_announcement, null);

        EditText etTitle = dialogView.findViewById(R.id.et_announce_title);
        EditText etBody  = dialogView.findViewById(R.id.et_announce_body);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_type);

        if (existing != null) {
            etTitle.setText(existing.getTitle());
            etBody.setText(existing.getBody());
            String t = existing.getType() != null ? existing.getType() : "info";
            switch (t) {
                case "warning": rgType.check(R.id.rb_warning); break;
                case "urgent":  rgType.check(R.id.rb_urgent);  break;
                default:        rgType.check(R.id.rb_info);    break;
            }
        }

        String dialogTitle = existing == null ? "Post Announcement" : "Edit Announcement";
        String positiveBtn = existing == null ? "Post" : "Save";

        new MaterialAlertDialogBuilder(this)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton(positiveBtn, (dialog, which) -> {
                String title = etTitle.getText().toString().trim();
                String body  = etBody.getText().toString().trim();

                if (title.isEmpty() || body.isEmpty()) {
                    Toast.makeText(this, "Title and message are required",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedId = rgType.getCheckedRadioButtonId();
                String type;
                if (selectedId == R.id.rb_warning)     type = "warning";
                else if (selectedId == R.id.rb_urgent) type = "urgent";
                else                                   type = "info";

                if (existing == null) {
                    postAnnouncement(title, body, type);
                } else {
                    updateAnnouncement(existing.getId(), title, body, type);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void postAnnouncement(String title, String body, String type) {
        Map<String, Object> data = new HashMap<>();
        data.put("title",     title);
        data.put("body",      body);
        data.put("type",      type);
        data.put("barangayId", barangayId);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("createdBy", mAuth.getCurrentUser() != null
            ? mAuth.getCurrentUser().getUid() : "admin");

        db.collection("announcements").add(data)
            .addOnSuccessListener(ref ->
                Toast.makeText(this, "Announcement posted!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateAnnouncement(String id, String title, String body, String type) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("body",  body);
        data.put("type",  type);

        db.collection("announcements").document(id).update(data)
            .addOnSuccessListener(unused ->
                Toast.makeText(this, "Announcement updated!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    private void confirmDelete(Announcement a) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Announcement?")
            .setMessage("\"" + a.getTitle() + "\" will be permanently removed.")
            .setPositiveButton("Delete", (d, w) ->
                db.collection("announcements").document(a.getId()).delete()
                    .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}