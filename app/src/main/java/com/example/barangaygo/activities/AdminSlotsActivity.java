package com.example.barangaygo.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.adapters.SlotAdapter;
import com.example.barangaygo.models.Service;
import com.example.barangaygo.models.Slot;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminSlotsActivity extends AppCompatActivity {

    private static final String[] TIME_RANGES = {
        "8:00 AM – 10:00 AM",
        "10:00 AM – 12:00 PM",
        "1:00 PM – 3:00 PM",
        "3:00 PM – 5:00 PM"
    };

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration slotsListener;
    private String barangayId = null;

    private RecyclerView rvSlots;
    private LinearLayout layoutEmpty;
    private ProgressBar progressLoading;
    private TextView tvSlotCount;
    private SlotAdapter adapter;

    private final List<Slot>    slots    = new ArrayList<>();
    private final List<Service> services = new ArrayList<>();
    private final List<String>  serviceNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_slots);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadBarangayAndStart();
    }

    private void initViews() {
        rvSlots        = findViewById(R.id.rv_slots);
        layoutEmpty    = findViewById(R.id.layout_empty);
        progressLoading = findViewById(R.id.progress_loading);
        tvSlotCount    = findViewById(R.id.tv_slot_count);

        LinearLayout btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fab_add_slot);
        if (fab != null) fab.setOnClickListener(v -> showSlotDialog(null));

        adapter = new SlotAdapter(this);
        rvSlots.setLayoutManager(new LinearLayoutManager(this));
        rvSlots.setNestedScrollingEnabled(false);
        rvSlots.setAdapter(adapter);

        adapter.setOnEditListener(this::showSlotDialog);
        adapter.setOnDeleteListener(this::confirmDelete);
    }

    // ─── Firestore ──────────────────────────────────────────────────────────

    private void loadBarangayAndStart() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                barangayId = doc.getString("barangayId");
                if (barangayId != null) {
                    loadServices();
                    startSlotsListener();
                }
            });
    }

    private void loadServices() {
        db.collection("services")
            .whereEqualTo("barangayId", barangayId)
            .get()
            .addOnSuccessListener(snap -> {
                services.clear();
                serviceNames.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    Service s = doc.toObject(Service.class);
                    s.setId(doc.getId());
                    services.add(s);
                }
                Collections.sort(services, (a, b) ->
                    a.getName() != null ? a.getName().compareToIgnoreCase(
                        b.getName() != null ? b.getName() : "") : 1);
                for (Service s : services) serviceNames.add(s.getName());
            });
    }

    private void startSlotsListener() {
        slotsListener = db.collection("queue_slots")
            .whereEqualTo("barangayId", barangayId)
            .addSnapshotListener((snaps, err) -> {
                progressLoading.setVisibility(View.GONE);
                if (err != null || snaps == null) return;

                slots.clear();
                for (QueryDocumentSnapshot doc : snaps) {
                    Slot s = doc.toObject(Slot.class);
                    s.setId(doc.getId());
                    slots.add(s);
                }

                sortSlots(slots);
                adapter.setSlots(slots);
                tvSlotCount.setText(slots.size() + " slot" + (slots.size() == 1 ? "" : "s"));

                layoutEmpty.setVisibility(slots.isEmpty() ? View.VISIBLE : View.GONE);
                rvSlots.setVisibility(slots.isEmpty() ? View.GONE : View.VISIBLE);
            });
    }

    private void sortSlots(List<Slot> list) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        list.sort((a, b) -> {
            String dateA = a.getDate() != null ? a.getDate() : "";
            String dateB = b.getDate() != null ? b.getDate() : "";
            boolean aExpired = !dateA.isEmpty() && dateA.compareTo(today) < 0;
            boolean bExpired = !dateB.isEmpty() && dateB.compareTo(today) < 0;

            if (aExpired && !bExpired) return 1;   // a goes to bottom
            if (!aExpired && bExpired) return -1;  // b goes to bottom
            if (aExpired) return dateB.compareTo(dateA); // both expired: most recent first
            return dateA.compareTo(dateB);          // both active: soonest first
        });
    }

    // ─── Add / Edit dialog ──────────────────────────────────────────────────

    private void showSlotDialog(Slot existing) {
        if (services.isEmpty()) {
            Toast.makeText(this, "No services found. Please add a service first.",
                Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_slot, null);

        Spinner  spService  = dialogView.findViewById(R.id.spinner_service);
        EditText etDate     = dialogView.findViewById(R.id.et_slot_date);
        Spinner  spTime     = dialogView.findViewById(R.id.spinner_time_range);
        EditText etCap      = dialogView.findViewById(R.id.et_max_capacity);
        RadioGroup rgStatus = dialogView.findViewById(R.id.rg_slot_status);
        RadioButton rbOpen  = dialogView.findViewById(R.id.rb_status_open);
        RadioButton rbClosed = dialogView.findViewById(R.id.rb_status_closed);
        com.google.android.material.button.MaterialButton btnPickDate =
            dialogView.findViewById(R.id.btn_pick_date);

        // Populate service spinner
        ArrayAdapter<String> svcAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, serviceNames);
        svcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spService.setAdapter(svcAdapter);

        // Populate time range spinner
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, TIME_RANGES);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTime.setAdapter(timeAdapter);

        // Pre-fill for edit
        if (existing != null) {
            for (int i = 0; i < serviceNames.size(); i++) {
                if (serviceNames.get(i).equalsIgnoreCase(existing.getServiceName())) {
                    spService.setSelection(i);
                    break;
                }
            }
            etDate.setText(existing.getDate());
            etCap.setText(String.valueOf(existing.getMaxCapacity()));
            for (int i = 0; i < TIME_RANGES.length; i++) {
                if (TIME_RANGES[i].equals(existing.getTimeRange())) {
                    spTime.setSelection(i);
                    break;
                }
            }
            if ("closed".equals(existing.getStatus())) rbClosed.setChecked(true);
            else rbOpen.setChecked(true);
        }

        // Date picker — past dates greyed out, Sundays blocked
        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(this,
                (picker, y, m, d) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(y, m, d);
                    if (picked.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        Toast.makeText(this,
                            "Sundays are not available. Please choose another day.",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    etDate.setText(String.format(Locale.getDefault(),
                        "%04d-%02d-%02d", y, m + 1, d));
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
            // Grey out all past dates
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dpd.show();
        });

        String title = existing == null ? "Add Slot" : "Edit Slot";
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                int svcPos    = spService.getSelectedItemPosition();
                String date   = etDate.getText().toString().trim();
                String capStr = etCap.getText().toString().trim();
                String timeRange = TIME_RANGES[spTime.getSelectedItemPosition()];
                String status = rbOpen.isChecked() ? "open" : "closed";

                if (date.isEmpty() || capStr.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                long cap;
                try { cap = Long.parseLong(capStr); }
                catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid capacity", Toast.LENGTH_SHORT).show();
                    return;
                }

                Service selected = services.get(svcPos);
                String svcId   = selected.getId();
                String svcName = selected.getName();

                if (existing == null) {
                    createSlot(svcId, svcName, date, timeRange, cap, status);
                } else {
                    updateSlot(existing.getId(), svcId, svcName, date, timeRange, cap, status);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createSlot(String svcId, String svcName, String date,
                            String timeRange, long cap, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("serviceId",    svcId);
        data.put("serviceName",  svcName);
        data.put("date",         date);
        data.put("timeRange",    timeRange);
        data.put("maxCapacity",  cap);
        data.put("currentCount", 0L);
        data.put("status",       status);
        data.put("barangayId",   barangayId);
        data.put("createdAt",    FieldValue.serverTimestamp());

        db.collection("queue_slots").add(data)
            .addOnSuccessListener(ref -> {
                Toast.makeText(this, "Slot created!", Toast.LENGTH_SHORT).show();
                refreshServiceAvailability(svcId);
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateSlot(String id, String svcId, String svcName, String date,
                            String timeRange, long cap, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("serviceId",   svcId);
        data.put("serviceName", svcName);
        data.put("date",        date);
        data.put("timeRange",   timeRange);
        data.put("maxCapacity", cap);
        data.put("status",      status);

        db.collection("queue_slots").document(id).update(data)
            .addOnSuccessListener(unused -> {
                Toast.makeText(this, "Slot updated!", Toast.LENGTH_SHORT).show();
                refreshServiceAvailability(svcId);
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─── Delete ─────────────────────────────────────────────────────────────

    private void confirmDelete(Slot slot) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Slot?")
            .setMessage("Remove " + slot.getServiceName() + " on " + slot.getDate()
                + " (" + slot.getTimeRange() + ")? This cannot be undone.")
            .setPositiveButton("Delete", (d, w) ->
                db.collection("queue_slots").document(slot.getId()).delete()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Slot deleted", Toast.LENGTH_SHORT).show();
                        refreshServiceAvailability(slot.getServiceId());
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ─── Auto-sync service availability ─────────────────────────────────────

    private void refreshServiceAvailability(String serviceId) {
        if (serviceId == null || serviceId.isEmpty()) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("queue_slots")
            .whereEqualTo("barangayId", barangayId)
            .whereEqualTo("serviceId", serviceId)
            .whereEqualTo("status", "open")
            .get()
            .addOnSuccessListener(snap -> {
                boolean hasOpenFutureSlot = false;
                for (QueryDocumentSnapshot doc : snap) {
                    String slotDate = doc.getString("date");
                    if (slotDate != null && slotDate.compareTo(today) >= 0) {
                        hasOpenFutureSlot = true;
                        break;
                    }
                }
                db.collection("services").document(serviceId)
                    .update("isAvailable", hasOpenFutureSlot);
            });
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slotsListener != null) slotsListener.remove();
    }
}