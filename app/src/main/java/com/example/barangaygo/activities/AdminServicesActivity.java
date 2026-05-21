package com.example.barangaygo.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.models.Service;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.RadioGroup;
import com.google.android.material.textfield.TextInputLayout;
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

public class AdminServicesActivity extends AppCompatActivity {

    // Preset barangay service icons
    private static final String[] ICON_KEYS = {
        "document", "certificate", "health", "id_card",
        "shield",   "building",   "people", "business",
        "water",    "tree",       "tools",  "star"
    };
    private static final int[] ICON_DRAWABLES = {
        R.drawable.ic_svc_document,    R.drawable.ic_svc_certificate,
        R.drawable.ic_svc_health,      R.drawable.ic_svc_id_card,
        R.drawable.ic_svc_shield,      R.drawable.ic_svc_building,
        R.drawable.ic_svc_people,      R.drawable.ic_svc_business,
        R.drawable.ic_svc_water,       R.drawable.ic_svc_tree,
        R.drawable.ic_svc_tools,       R.drawable.ic_svc_star
    };
    private static final String[] ICON_LABELS = {
        "Clearance", "Certificate", "Health",   "ID Card",
        "Blotter",   "Permits",     "Social",   "Business",
        "Utilities", "Environment", "Infra",    "Award"
    };

    // 4-color accent palette, cycles per icon index
    private static final int[] BG_COLORS = {
        R.color.accent_blue_bg, R.color.accent_green_bg,
        R.color.accent_orange_bg, R.color.accent_purple_bg
    };
    private static final int[] ICON_COLORS = {
        R.color.accent_blue, R.color.accent_green,
        R.color.accent_orange, R.color.accent_purple
    };

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration servicesListener;
    private String barangayId = null;

    private RecyclerView rvServices;
    private LinearLayout layoutEmpty;
    private ProgressBar progressLoading;
    private TextView tvServiceCount;

    private final List<Service> services = new ArrayList<>();
    private ServiceRowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_services);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadBarangayAndStart();
    }

    private void initViews() {
        rvServices      = findViewById(R.id.rv_services);
        layoutEmpty     = findViewById(R.id.layout_empty);
        progressLoading = findViewById(R.id.progress_loading);
        tvServiceCount  = findViewById(R.id.tv_service_count);

        LinearLayout btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fab_add_service);
        if (fab != null) fab.setOnClickListener(v -> showServiceDialog(null));

        LinearLayout btnManageSlots = findViewById(R.id.btn_manage_slots);
        if (btnManageSlots != null)
            btnManageSlots.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, AdminSlotsActivity.class)));

        adapter = new ServiceRowAdapter();
        rvServices.setLayoutManager(new LinearLayoutManager(this));
        rvServices.setNestedScrollingEnabled(false);
        rvServices.setAdapter(adapter);

        setupNavigation();
    }

    private void setupNavigation() {
        LinearLayout navDashboard = findViewById(R.id.nav_dashboard);
        LinearLayout navQueue     = findViewById(R.id.nav_queue);
        LinearLayout navServices  = findViewById(R.id.nav_services);
        LinearLayout navProfile   = findViewById(R.id.nav_profile);

        if (navDashboard != null)
            navDashboard.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, AdminActivity.class)
                    .setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)));

        if (navQueue != null)
            navQueue.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, AdminActivity.class)
                    .setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("show_queue", true)));

        if (navServices != null)
            navServices.setOnClickListener(v -> { /* already here */ });

        if (navProfile != null)
            navProfile.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, AdminProfileActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
    }

    // ─── Firestore ───────────────────────────────────────────────────────────

    private void loadBarangayAndStart() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                barangayId = doc.getString("barangayId");
                if (barangayId != null) startServicesListener();
            });
    }

    private void startServicesListener() {
        servicesListener = db.collection("services")
            .whereEqualTo("barangayId", barangayId)
            .addSnapshotListener((snaps, err) -> {
                progressLoading.setVisibility(View.GONE);
                if (err != null || snaps == null) return;

                services.clear();
                for (QueryDocumentSnapshot doc : snaps) {
                    Service s = doc.toObject(Service.class);
                    s.setId(doc.getId());
                    services.add(s);
                }
                Collections.sort(services, (a, b) ->
                    a.getName() != null ? a.getName().compareToIgnoreCase(
                        b.getName() != null ? b.getName() : "") : 1);

                adapter.notifyDataSetChanged();
                int count = services.size();
                tvServiceCount.setText(count + " service" + (count == 1 ? "" : "s"));

                layoutEmpty.setVisibility(services.isEmpty() ? View.VISIBLE : View.GONE);
                rvServices.setVisibility(services.isEmpty() ? View.GONE : View.VISIBLE);
            });
    }

    // ─── Icon helper ─────────────────────────────────────────────────────────

    private int iconIndexForKey(String key) {
        if (key != null) {
            for (int i = 0; i < ICON_KEYS.length; i++) {
                if (ICON_KEYS[i].equals(key)) return i;
            }
        }
        return 0;
    }

    private void applyIconPickerState(LinearLayout iconBg, ImageView iconImg,
                                      int iconIdx, boolean selected) {
        int colorIdx = iconIdx % 4;
        if (selected) {
            iconBg.setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_icon_picker_selected));
            iconImg.setColorFilter(
                ContextCompat.getColor(this, ICON_COLORS[colorIdx]));
        } else {
            iconBg.setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_icon_picker_item));
            iconImg.setColorFilter(
                ContextCompat.getColor(this, R.color.gray_400));
        }
    }

    // ─── Add / Edit dialog ───────────────────────────────────────────────────

    private void showServiceDialog(Service existing) {
        View dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_manage_service, null);

        TextInputLayout tilName    = dialogView.findViewById(R.id.til_service_name);
        TextInputLayout tilMinutes = dialogView.findViewById(R.id.til_service_minutes);
        EditText etName     = dialogView.findViewById(R.id.et_service_name);
        EditText etDesc     = dialogView.findViewById(R.id.et_service_desc);
        EditText etMinutes  = dialogView.findViewById(R.id.et_service_minutes);
        EditText etReqs     = dialogView.findViewById(R.id.et_service_requirements);
        SwitchCompat swAvail = dialogView.findViewById(R.id.switch_available);
        RadioGroup rgUnit   = dialogView.findViewById(R.id.rg_time_unit);

        // ── Icon picker setup ─────────────────────────────────────────────
        final String[] selectedIconKey = {
            (existing != null && existing.getIconKey() != null && !existing.getIconKey().isEmpty())
                ? existing.getIconKey() : ICON_KEYS[0]
        };

        LinearLayout row1 = dialogView.findViewById(R.id.container_icon_row_1);
        LinearLayout row2 = dialogView.findViewById(R.id.container_icon_row_2);
        LinearLayout row3 = dialogView.findViewById(R.id.container_icon_row_3);
        LinearLayout[] rows = { row1, row2, row3 };

        // Keep references to all icon items for toggling selection state
        final LinearLayout[] iconBgs   = new LinearLayout[ICON_KEYS.length];
        final ImageView[]    iconImgs  = new ImageView[ICON_KEYS.length];

        for (int i = 0; i < ICON_KEYS.length; i++) {
            final int idx = i;
            View item = LayoutInflater.from(this)
                .inflate(R.layout.item_icon_picker, rows[i / 4], false);

            LinearLayout iconBg  = item.findViewById(R.id.icon_bg);
            ImageView    iconImg = item.findViewById(R.id.icon_image);
            TextView     label   = item.findViewById(R.id.icon_label);

            iconImg.setImageResource(ICON_DRAWABLES[i]);
            label.setText(ICON_LABELS[i]);

            boolean selected = ICON_KEYS[i].equals(selectedIconKey[0]);
            applyIconPickerState(iconBg, iconImg, i, selected);

            iconBg.setOnClickListener(v -> {
                for (int j = 0; j < ICON_KEYS.length; j++) {
                    applyIconPickerState(iconBgs[j], iconImgs[j], j, j == idx);
                }
                selectedIconKey[0] = ICON_KEYS[idx];
            });

            iconBgs[i]  = iconBg;
            iconImgs[i] = iconImg;
            rows[i / 4].addView(item);
        }
        // ─────────────────────────────────────────────────────────────────

        if (existing != null) {
            etName.setText(existing.getName());
            etDesc.setText(existing.getDescription());
            boolean isHr = "hr".equals(existing.getTimeUnit());
            if (existing.getEstimatedMinutes() > 0) {
                int displayVal = isHr ? existing.getEstimatedMinutes() / 60
                                      : existing.getEstimatedMinutes();
                etMinutes.setText(String.valueOf(displayVal));
            }
            if (isHr) rgUnit.check(R.id.rb_unit_hr);
            else      rgUnit.check(R.id.rb_unit_min);
            swAvail.setChecked(existing.isAvailable());
            if (existing.getRequirements() != null && !existing.getRequirements().isEmpty()) {
                etReqs.setText(android.text.TextUtils.join("\n", existing.getRequirements()));
            }
        }

        String title = existing == null ? "Add Service" : "Edit Service";
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String name    = etName.getText().toString().trim();
                String desc    = etDesc.getText().toString().trim();
                String minsStr = etMinutes.getText().toString().trim();
                String reqsRaw = etReqs.getText().toString().trim();
                boolean avail  = swAvail.isChecked();

                tilName.setError(null);
                tilMinutes.setError(null);

                if (name.isEmpty()) {
                    tilName.setError("Service name is required");
                    return;
                }
                if (minsStr.isEmpty()) {
                    tilMinutes.setError("Required");
                    return;
                }

                int rawVal;
                try { rawVal = Integer.parseInt(minsStr); }
                catch (NumberFormatException e) {
                    tilMinutes.setError("Enter a valid number");
                    return;
                }

                boolean isHr = rgUnit.getCheckedRadioButtonId() == R.id.rb_unit_hr;
                String unit = isHr ? "hr" : "min";
                int mins = isHr ? rawVal * 60 : rawVal;

                List<String> reqs = new ArrayList<>();
                for (String line : reqsRaw.split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) reqs.add(trimmed);
                }

                if (existing == null) {
                    createService(name, desc, mins, unit, reqs, avail, selectedIconKey[0]);
                } else {
                    updateService(existing.getId(), name, desc, mins, unit, reqs, avail,
                                  selectedIconKey[0]);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createService(String name, String desc, int mins, String unit,
                               List<String> reqs, boolean avail, String iconKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("name",             name);
        data.put("description",      desc);
        data.put("estimatedMinutes", mins);
        data.put("timeUnit",         unit);
        data.put("requirements",     reqs);
        data.put("isAvailable",      avail);
        data.put("iconKey",          iconKey);
        data.put("barangayId",       barangayId);
        data.put("createdAt",        FieldValue.serverTimestamp());

        db.collection("services").add(data)
            .addOnSuccessListener(ref ->
                Toast.makeText(this, "Service added!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateService(String id, String name, String desc, int mins, String unit,
                               List<String> reqs, boolean avail, String iconKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("name",             name);
        data.put("description",      desc);
        data.put("estimatedMinutes", mins);
        data.put("timeUnit",         unit);
        data.put("requirements",     reqs);
        data.put("isAvailable",      avail);
        data.put("iconKey",          iconKey);

        db.collection("services").document(id).update(data)
            .addOnSuccessListener(unused ->
                Toast.makeText(this, "Service updated!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    private void confirmDelete(Service service) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Service?")
            .setMessage("Remove \"" + service.getName() + "\"? "
                + "Residents will no longer be able to book this service.")
            .setPositiveButton("Delete", (d, w) ->
                db.collection("services").document(service.getId()).delete()
                    .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Service deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ─── Inner RecyclerView adapter ──────────────────────────────────────────

    private class ServiceRowAdapter extends RecyclerView.Adapter<ServiceRowAdapter.VH> {

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_service_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            Service s = services.get(position);

            int iconIdx = iconIndexForKey(s.getIconKey());
            int palette = iconIdx % 4;

            h.tvName.setText(s.getName());
            h.tvTime.setText(s.getFormattedTime());
            h.tvStatus.setText(s.isAvailable() ? "Available" : "Unavailable");
            h.tvStatus.setTextColor(ContextCompat.getColor(
                AdminServicesActivity.this,
                s.isAvailable() ? R.color.status_success : R.color.status_danger));

            if (s.getDescription() != null && !s.getDescription().isEmpty()) {
                h.tvDesc.setText(s.getDescription());
                h.tvDesc.setVisibility(View.VISIBLE);
            } else {
                h.tvDesc.setVisibility(View.GONE);
            }

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(AdminServicesActivity.this, BG_COLORS[palette]));
            bg.setCornerRadius(dpToPx(11));
            h.iconBg.setBackground(bg);
            h.icon.setImageResource(ICON_DRAWABLES[iconIdx]);
            h.icon.setColorFilter(
                ContextCompat.getColor(AdminServicesActivity.this, ICON_COLORS[palette]));

            h.btnEdit.setOnClickListener(v -> showServiceDialog(s));
            h.btnDelete.setOnClickListener(v -> confirmDelete(s));
        }

        @Override
        public int getItemCount() { return services.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvTime, tvStatus, tvDesc;
            LinearLayout iconBg;
            ImageView icon, btnEdit, btnDelete;

            VH(View itemView) {
                super(itemView);
                tvName    = itemView.findViewById(R.id.tv_svc_name);
                tvTime    = itemView.findViewById(R.id.tv_svc_time);
                tvStatus  = itemView.findViewById(R.id.tv_svc_status);
                tvDesc    = itemView.findViewById(R.id.tv_svc_desc);
                iconBg    = itemView.findViewById(R.id.svc_icon_bg);
                icon      = itemView.findViewById(R.id.svc_icon);
                btnEdit   = itemView.findViewById(R.id.btn_edit_service);
                btnDelete = itemView.findViewById(R.id.btn_delete_service);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (servicesListener != null) servicesListener.remove();
    }
}