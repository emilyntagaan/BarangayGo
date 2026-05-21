package com.example.barangaygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.barangaygo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookQueueActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Views
    private LinearLayout btnBack, layoutStep1, layoutStep2, layoutStep3;
    private LinearLayout step1Circle, step2Circle, step3Circle;
    private TextView step1Label, step2Label, step3Label;
    private CalendarView calendarView;
    private TextView tvSelectedDate, tvConfirmService, tvConfirmDate;
    private TextView tvConfirmTime, tvConfirmSlots;
    private LinearLayout slotsContainer, layoutNoSlots;
    private LinearLayout layoutDateAvailability;
    private ProgressBar slotsLoading, availabilityLoading;
    private android.widget.ImageView availabilityIcon;
    private TextView tvDateAvailability;
    private MaterialButton btnNextStep, btnBackStep;

    // State
    private String barangayId = null;
    private int currentStep = 1;
    private String selectedDate = "";
    private String selectedDateDisplay = "";
    private String selectedSlotId = "";
    private String selectedTimeRange = "";
    private long selectedSlotsRemaining = 0;
    private String serviceId = "";
    private String serviceName = "";
    private String selectedSlotView = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_queue);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        serviceId = getIntent().getStringExtra("serviceId");
        serviceName = getIntent().getStringExtra("serviceName");

        initViews();
        setupCalendar();
        setupButtons();
        updateStepUI();
        loadBarangayId();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        layoutStep1 = findViewById(R.id.layout_step1);
        layoutStep2 = findViewById(R.id.layout_step2);
        layoutStep3 = findViewById(R.id.layout_step3);

        step1Circle = findViewById(R.id.step1_circle);
        step2Circle = findViewById(R.id.step2_circle);
        step3Circle = findViewById(R.id.step3_circle);
        step1Label = findViewById(R.id.step1_label);
        step2Label = findViewById(R.id.step2_label);
        step3Label = findViewById(R.id.step3_label);

        calendarView = findViewById(R.id.calendar_view);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvConfirmService = findViewById(R.id.tv_confirm_service);
        tvConfirmDate = findViewById(R.id.tv_confirm_date);
        tvConfirmTime = findViewById(R.id.tv_confirm_time);
        tvConfirmSlots = findViewById(R.id.tv_confirm_slots);

        slotsContainer = findViewById(R.id.slots_container);
        layoutNoSlots = findViewById(R.id.layout_no_slots);
        slotsLoading = findViewById(R.id.slots_loading);
        layoutDateAvailability = findViewById(R.id.layout_date_availability);
        availabilityLoading = findViewById(R.id.availability_loading);
        availabilityIcon = findViewById(R.id.availability_icon);
        tvDateAvailability = findViewById(R.id.tv_date_availability);

        btnNextStep = findViewById(R.id.btn_next_step);
        btnBackStep = findViewById(R.id.btn_back_step);

        // Set today's date as default — using explicit fields to avoid timezone issues
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
        selectedDateDisplay = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                .format(cal.getTime());

        // Set minimum date to today
        calendarView.setMinDate(cal.getTimeInMillis());

        if (serviceName != null) tvConfirmService.setText(serviceName);
    }

    private void loadBarangayId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                barangayId = doc.getString("barangayId");
                checkDateAvailability(selectedDate);
            });
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                android.widget.Toast.makeText(this,
                        "Sorry, the office is closed on Sundays",
                        android.widget.Toast.LENGTH_SHORT).show();
                showAvailabilityResult(false, 0, true);
                return;
            }

            selectedDate = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            selectedDateDisplay = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(cal.getTime());

            android.util.Log.d("SLOTS_DEBUG", "Date selected: " + selectedDate);
            checkDateAvailability(selectedDate);
        });
    }

    private void checkDateAvailability(String date) {
        if (barangayId == null) return;

        layoutDateAvailability.setVisibility(View.VISIBLE);
        availabilityLoading.setVisibility(View.VISIBLE);
        availabilityIcon.setVisibility(View.GONE);
        tvDateAvailability.setText("Checking availability...");
        tvDateAvailability.setTextColor(ContextCompat.getColor(this, R.color.blue_600));
        layoutDateAvailability.setBackgroundResource(R.drawable.bg_announce_info);

        db.collection("queue_slots")
                .whereEqualTo("barangayId", barangayId)
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("date", date)
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int totalRemaining = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Object maxRaw = doc.get("maxCapacity");
                        Object countRaw = doc.get("currentCount");
                        long max = maxRaw instanceof Number ? ((Number) maxRaw).longValue() : 30;
                        long count = countRaw instanceof Number ? ((Number) countRaw).longValue() : 0;
                        totalRemaining += Math.max(0, max - count);
                    }
                    showAvailabilityResult(totalRemaining > 0, totalRemaining, false);
                })
                .addOnFailureListener(e -> {
                    availabilityLoading.setVisibility(View.GONE);
                    availabilityIcon.setVisibility(View.GONE);
                    tvDateAvailability.setText("Could not check availability");
                    tvDateAvailability.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
                });
    }

    private void showAvailabilityResult(boolean hasSlots, int total, boolean isSunday) {
        availabilityLoading.setVisibility(View.GONE);
        availabilityIcon.setVisibility(View.VISIBLE);
        layoutDateAvailability.setVisibility(View.VISIBLE);

        if (isSunday) {
            layoutDateAvailability.setBackgroundResource(R.drawable.bg_announce_warning);
            availabilityIcon.setImageResource(R.drawable.ic_bell_blue);
            tvDateAvailability.setText("Office is closed on Sundays");
            tvDateAvailability.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
        } else if (hasSlots) {
            layoutDateAvailability.setBackgroundResource(R.drawable.bg_announce_info);
            availabilityIcon.setImageResource(R.drawable.ic_bell_blue);
            tvDateAvailability.setText(total + " slot" + (total == 1 ? "" : "s") + " available on this date");
            tvDateAvailability.setTextColor(ContextCompat.getColor(this, R.color.status_success));
        } else {
            layoutDateAvailability.setBackgroundResource(R.drawable.bg_announce_warning);
            availabilityIcon.setImageResource(R.drawable.ic_bell_blue);
            tvDateAvailability.setText("No available slots on this date");
            tvDateAvailability.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
        }
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnNextStep.setOnClickListener(v -> {
            if (currentStep == 1) {
                goToStep2();
            } else if (currentStep == 2) {
                if (selectedSlotId.isEmpty()) {
                    android.widget.Toast.makeText(this,
                            "Please select a time slot", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                goToStep3();
            } else if (currentStep == 3) {
                confirmBooking();
            }
        });

        btnBackStep.setOnClickListener(v -> {
            if (currentStep == 2) {
                currentStep = 1;
                updateStepUI();
            } else if (currentStep == 3) {
                currentStep = 2;
                updateStepUI();
            }
        });
    }

    private void goToStep2() {
        currentStep = 2;
        updateStepUI();

        tvSelectedDate.setText("Available slots for " + selectedDateDisplay);
        loadSlots();
    }

    private void goToStep3() {
        currentStep = 3;
        updateStepUI();

        tvConfirmDate.setText(selectedDateDisplay);
        tvConfirmTime.setText(selectedTimeRange);
        tvConfirmSlots.setText(selectedSlotsRemaining + " slots remaining");
        if (serviceName != null) tvConfirmService.setText(serviceName);
    }

    private void loadSlots() {
        if (barangayId == null) return;
        slotsLoading.setVisibility(View.VISIBLE);
        slotsContainer.setVisibility(View.GONE);
        layoutNoSlots.setVisibility(View.GONE);
        slotsContainer.removeAllViews();
        selectedSlotId = "";

        // Debug — show what we're querying
        android.util.Log.d("SLOTS_DEBUG", "Querying slots:");
        android.util.Log.d("SLOTS_DEBUG", "  serviceId = '" + serviceId + "'");
        android.util.Log.d("SLOTS_DEBUG", "  date = '" + selectedDate + "'");

        db.collection("queue_slots")
                .whereEqualTo("barangayId", barangayId)
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    slotsLoading.setVisibility(View.GONE);
                    android.util.Log.d("SLOTS_DEBUG", "Filtered results: " + querySnapshot.size());

                    if (querySnapshot.isEmpty()) {
                        layoutNoSlots.setVisibility(View.VISIBLE);
                        return;
                    }

                    slotsContainer.setVisibility(View.VISIBLE);

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String slotId = doc.getId();
                        String timeRange = doc.getString("timeRange");
                        Object maxCapRaw = doc.get("maxCapacity");
                        Object currentCountRaw = doc.get("currentCount");

                        long maxCap = maxCapRaw instanceof Number ?
                                ((Number) maxCapRaw).longValue() : 30;
                        long currentCount = currentCountRaw instanceof Number ?
                                ((Number) currentCountRaw).longValue() : 0;
                        long remaining = maxCap - currentCount;

                        // If the selected date is today, skip slots whose end time
                        // has already passed — residents should not be able to book them.
                        String today = new SimpleDateFormat("yyyy-MM-dd",
                                Locale.getDefault()).format(new Date());
                        if (selectedDate.equals(today) && isSlotTimePast(timeRange)) {
                            android.util.Log.d("SLOTS_DEBUG",
                                    "Skipping past slot: " + timeRange);
                            continue;
                        }

                        addSlotCard(slotId, timeRange, remaining, maxCap, remaining <= 0);
                    }

                    if (slotsContainer.getChildCount() == 0) {
                        layoutNoSlots.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    slotsLoading.setVisibility(View.GONE);
                    layoutNoSlots.setVisibility(View.VISIBLE);
                    android.util.Log.e("SLOTS_DEBUG", "Query failed: " + e.getMessage());
                });
    }

    private void addSlotCard(String slotId, String timeRange,
                             long remaining, long maxCap, boolean isFull) {
        com.google.android.material.card.MaterialCardView card =
                new com.google.android.material.card.MaterialCardView(this);
        com.google.android.material.card.MaterialCardView.LayoutParams cardParams =
                new com.google.android.material.card.MaterialCardView.LayoutParams(
                        com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
                        com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardParams);
        card.setCardElevation(0);
        card.setRadius(dpToPx(14));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // Time range text
        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textGroup.setLayoutParams(textParams);

        TextView tvTime = new TextView(this);
        tvTime.setText(timeRange != null ? timeRange : "Unknown time");
        tvTime.setTextSize(14);
        tvTime.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTime.setTextColor(ContextCompat.getColor(this, R.color.gray_800));
        textGroup.addView(tvTime);

        TextView tvRemaining = new TextView(this);
        if (isFull) {
            tvRemaining.setText("Fully booked");
            tvRemaining.setTextColor(ContextCompat.getColor(this, R.color.status_danger));
        } else {
            tvRemaining.setText(remaining + " of " + maxCap + " slots available");
            tvRemaining.setTextColor(ContextCompat.getColor(this,
                    remaining <= 5 ? R.color.status_warning : R.color.status_success));
        }
        tvRemaining.setTextSize(12);
        textGroup.addView(tvRemaining);

        inner.addView(textGroup);

        if (isFull) {
            // Show FULL badge — not selectable
            card.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));

            TextView tvFull = new TextView(this);
            tvFull.setText("FULL");
            tvFull.setTextSize(11);
            tvFull.setTextColor(0xFFB91C1C);
            tvFull.setTypeface(null, android.graphics.Typeface.BOLD);
            tvFull.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            android.graphics.drawable.GradientDrawable fullBg = new android.graphics.drawable.GradientDrawable();
            fullBg.setColor(0xFFFEE2E2);
            fullBg.setCornerRadius(dpToPx(6));
            tvFull.setBackground(fullBg);
            inner.addView(tvFull);
        } else {
            MaterialButton btnSelect = new MaterialButton(this,
                    null, com.google.android.material.R.attr.borderlessButtonStyle);
            btnSelect.setText("Select");
            btnSelect.setTextSize(12);
            btnSelect.setTextColor(ContextCompat.getColor(this, R.color.blue_600));
            btnSelect.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_50));
            btnSelect.setCornerRadius(dpToPx(8));
            btnSelect.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

            final long finalRemaining = remaining;
            btnSelect.setOnClickListener(v -> {
                for (int i = 0; i < slotsContainer.getChildCount(); i++) {
                    View child = slotsContainer.getChildAt(i);
                    if (child instanceof com.google.android.material.card.MaterialCardView) {
                        ((com.google.android.material.card.MaterialCardView) child).setStrokeColor(
                                ContextCompat.getColor(this, R.color.gray_200));
                        ((com.google.android.material.card.MaterialCardView) child).setCardBackgroundColor(
                                ContextCompat.getColor(this, R.color.white));
                    }
                }
                card.setStrokeColor(ContextCompat.getColor(this, R.color.blue_400));
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_50));

                selectedSlotId        = slotId;
                selectedTimeRange     = timeRange != null ? timeRange : "";
                selectedSlotsRemaining = finalRemaining;

                android.widget.Toast.makeText(this,
                        "Slot selected: " + timeRange, android.widget.Toast.LENGTH_SHORT).show();
            });
            inner.addView(btnSelect);
        }
        card.addView(inner);
        slotsContainer.addView(card);
    }

    private void confirmBooking() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            android.widget.Toast.makeText(this,
                    "You must be logged in to book", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        btnNextStep.setEnabled(false);
        btnNextStep.setText("Booking...");

        // Get user name from Firestore first
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String residentName = userDoc.getString("name");
                    if (residentName == null || residentName.isEmpty()) {
                        residentName = user.getEmail();
                    }
                    createBooking(user.getUid(), residentName);
                })
                .addOnFailureListener(e ->
                        createBooking(user.getUid(), user.getEmail())
                );
    }

    private void createBooking(String userId, String residentName) {
        // Check for time conflicts — only filter by userId + date server-side
        // to avoid requiring a composite Firestore index for whereIn + whereEqualTo.
        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", selectedDate)
            .get()
            .addOnSuccessListener(snap -> {
                for (QueryDocumentSnapshot doc : snap) {
                    String status = doc.getString("status");
                    if (!"waiting".equals(status) && !"serving".equals(status)) continue;
                    String existingRange = doc.getString("timeRange");
                    if (timesOverlap(existingRange, selectedTimeRange)) {
                        btnNextStep.setEnabled(true);
                        btnNextStep.setText("Confirm Booking");
                        android.widget.Toast.makeText(this,
                            "You already have a booking that overlaps this time slot. " +
                            "Please choose a different date or time.",
                            android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                doAtomicBooking(userId, residentName);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("BookQueue", "Overlap check query failed", e);
                doAtomicBooking(userId, residentName);
            });
    }

    private void doAtomicBooking(String userId, String residentName) {
        com.google.firebase.firestore.DocumentReference slotRef =
            db.collection("queue_slots").document(selectedSlotId);
        com.google.firebase.firestore.DocumentReference bookingRef =
            db.collection("bookings").document();

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot slot = transaction.get(slotRef);
            Object maxRaw   = slot.get("maxCapacity");
            Object countRaw = slot.get("currentCount");
            long maxCap      = maxRaw   instanceof Number ? ((Number) maxRaw).longValue()   : 30;
            long currentCount = countRaw instanceof Number ? ((Number) countRaw).longValue() : 0;

            if (currentCount >= maxCap) {
                throw new RuntimeException("SLOT_FULL");
            }

            long queuePosition = currentCount + 1;
            String queueNumber = String.format(Locale.getDefault(), "A-%03d", queuePosition);

            Map<String, Object> booking = new HashMap<>();
            booking.put("userId",       userId);
            booking.put("barangayId",   barangayId);
            booking.put("serviceId",    serviceId);
            booking.put("serviceName",  serviceName);
            booking.put("slotId",       selectedSlotId);
            booking.put("queueNumber",  queueNumber);
            booking.put("residentName", residentName);
            booking.put("status",       "waiting");
            booking.put("aheadCount",   queuePosition - 1);
            booking.put("date",         selectedDate);
            booking.put("timeRange",    selectedTimeRange);
            booking.put("createdAt",    Timestamp.now());

            transaction.set(bookingRef, booking);
            transaction.update(slotRef, "currentCount", FieldValue.increment(1));
            return queueNumber;
        }).addOnSuccessListener(queueNumber -> {
            android.widget.Toast.makeText(this,
                "Booking confirmed! Queue #" + queueNumber,
                android.widget.Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, QueueTicketActivity.class)
                .putExtra("bookingId", bookingRef.getId())
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }).addOnFailureListener(e -> {
            btnNextStep.setEnabled(true);
            btnNextStep.setText("Confirm Booking");
            android.util.Log.e("BookQueue", "Booking transaction failed", e);
            boolean isSlotFull = isSlotFullException(e);
            String msg = isSlotFull
                ? "This slot just became full. Please choose another time."
                : "Booking failed. Please try again.";
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
        });
    }

    private boolean isSlotFullException(Exception e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg != null && msg.contains("SLOT_FULL")) return true;
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null
                && cause.getMessage().contains("SLOT_FULL")) return true;
        return false;
    }

    private boolean timesOverlap(String r1, String r2) {
        int[] a = parseTimeRange(r1);
        int[] b = parseTimeRange(r2);
        if (a == null || b == null) return false;
        return a[0] < b[1] && b[0] < a[1];
    }

    /**
     * Returns true when the slot's end time has already passed today.
     * Used to hide stale slots from the booking screen on the current day.
     */
    private boolean isSlotTimePast(String timeRange) {
        int[] range = parseTimeRange(timeRange);
        if (range == null) return false;
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return nowMinutes >= range[1]; // past the slot's end time
    }

    private int[] parseTimeRange(String timeRange) {
        if (timeRange == null) return null;
        try {
            String[] parts = timeRange.split("\\s*[–—-]\\s*");
            if (parts.length < 2) return null;
            return new int[]{ parseTimePart(parts[0].trim()), parseTimePart(parts[1].trim()) };
        } catch (Exception e) {
            return null;
        }
    }

    private int parseTimePart(String t) throws ParseException {
        String fmt = t.contains(":") ? "h:mm a" : "h a";
        for (Locale locale : new Locale[]{ Locale.getDefault(), Locale.US, Locale.ENGLISH }) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, locale);
                sdf.setLenient(false);
                Date d = sdf.parse(t);
                if (d == null) continue;
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            } catch (ParseException ignored) {}
        }
        throw new ParseException("Cannot parse time: " + t, 0);
    }

    private void updateStepUI() {
        // Show/hide step layouts
        layoutStep1.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);

        // Back button visibility
        btnBackStep.setVisibility(currentStep > 1 ? View.VISIBLE : View.GONE);

        // Next button text
        if (currentStep == 3) {
            btnNextStep.setText("Confirm Booking");
        } else {
            btnNextStep.setText("Next");
        }

        // Step indicator colors
        updateStepCircle(step1Circle, step1Label, currentStep >= 1);
        updateStepCircle(step2Circle, step2Label, currentStep >= 2);
        updateStepCircle(step3Circle, step3Label, currentStep >= 3);
    }

    private void updateStepCircle(LinearLayout circle, TextView label,
                                  boolean isActive) {
        if (isActive) {
            circle.setBackgroundResource(R.drawable.bg_step_active);
            ((TextView) circle.getChildAt(0)).setTextColor(
                    ContextCompat.getColor(this, R.color.white));
            label.setTextColor(ContextCompat.getColor(this, R.color.blue_600));
        } else {
            circle.setBackgroundResource(R.drawable.bg_step_inactive);
            ((TextView) circle.getChildAt(0)).setTextColor(
                    ContextCompat.getColor(this, R.color.gray_400));
            label.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}