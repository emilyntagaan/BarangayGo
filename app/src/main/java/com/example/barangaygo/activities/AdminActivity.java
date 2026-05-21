package com.example.barangaygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.adapters.QueueAdapter;
import com.example.barangaygo.models.Booking;
import com.example.barangaygo.models.Slot;
import com.example.barangaygo.utils.NotificationHelper;
import com.example.barangaygo.views.PeakHoursChartView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration bookingsListener, todaySlotsListener;

    private TextView tvBarangayName;

    // Stats
    private TextView tvStatInQueue, tvStatServed, tvStatRemaining, tvWaitingCount;

    // Active slot banner
    private TextView tvActiveSlotBanner;

    // Now serving card
    private TextView tvServingNumber, tvServingName, tvServingService;
    private MaterialButton btnNext, btnSkip;

    // Quick action cards
    private CardView btnCallNext, btnMarkDone, btnAnnounce;

    // Queue list
    private RecyclerView rvQueue;
    private TextView tvEmptyQueue;
    private QueueAdapter queueAdapter;

    // Bottom nav
    private LinearLayout navDashboard, navQueue, navServices, navProfile;

    // Analytics
    private PeakHoursChartView chartPeakHours;

    // Scroll container + section toggle views
    private androidx.core.widget.NestedScrollView adminNestedScroll;
    private CardView cardQueueSection;
    private LinearLayout layoutQuickActions;
    private CardView cardPeakHours;

    // Barangay scope
    private String barangayId = null;
    private boolean leavingToSubActivity = false;

    // Runtime state — separated into "all today" vs "active slot only"
    private Booking currentServingBooking = null;
    private final List<Booking> allTodayWaiting  = new ArrayList<>(); // all today's waiting
    private final List<Booking> waitingBookings   = new ArrayList<>(); // filtered by active slots
    private int servedCount = 0;

    // Today's date (set once at startup)
    private String todayStr;

    // Today's open slots from Firestore + derived active-slot set
    private final List<Slot>  todaySlots    = new ArrayList<>();
    private final Set<String> activeSlotIds = new HashSet<>();

    // Slot time monitoring
    private final Handler slotTimeHandler = new Handler(Looper.getMainLooper());
    private Runnable slotTimeChecker;
    private final Set<String> notifiedActiveSlots   = new HashSet<>();
    private final Set<String> processedExpiredSlots = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        NotificationHelper.createChannels(this);

        todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupNavigation();
        setActiveNav(navDashboard);
        showDashboard();
        loadBarangayName();
    }

    // ─── Views ───────────────────────────────────────────────────────────────

    private void initViews() {
        tvStatInQueue    = findViewById(R.id.tv_stat_in_queue);
        tvStatServed     = findViewById(R.id.tv_stat_served);
        tvStatRemaining  = findViewById(R.id.tv_stat_remaining);
        tvWaitingCount   = findViewById(R.id.tv_waiting_count);

        tvActiveSlotBanner = findViewById(R.id.tv_active_slot_banner);

        tvServingNumber  = findViewById(R.id.tv_serving_number);
        tvServingName    = findViewById(R.id.tv_serving_name);
        tvServingService = findViewById(R.id.tv_serving_service);
        btnNext          = findViewById(R.id.btn_next);
        btnSkip          = findViewById(R.id.btn_skip);

        btnCallNext = findViewById(R.id.btn_call_next);
        btnMarkDone = findViewById(R.id.btn_mark_done);
        btnAnnounce = findViewById(R.id.btn_announce);

        rvQueue      = findViewById(R.id.rv_queue);
        tvEmptyQueue = findViewById(R.id.tv_empty_queue);

        navDashboard   = findViewById(R.id.nav_dashboard);
        navQueue       = findViewById(R.id.nav_queue);
        navServices    = findViewById(R.id.nav_services);
        navProfile     = findViewById(R.id.nav_profile);

        chartPeakHours    = findViewById(R.id.chart_peak_hours);
        adminNestedScroll = findViewById(R.id.admin_nested_scroll);
        cardQueueSection  = findViewById(R.id.card_queue_section);
        layoutQuickActions = findViewById(R.id.layout_quick_actions);
        cardPeakHours     = findViewById(R.id.card_peak_hours);
        tvBarangayName    = findViewById(R.id.tv_barangay_name);

        // Bell icon → announcements management
        LinearLayout btnNotif = findViewById(R.id.btn_admin_notif);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> openAnnouncementsManagement());
        }
    }

    private void setupRecyclerView() {
        queueAdapter = new QueueAdapter(this);
        rvQueue.setLayoutManager(new LinearLayoutManager(this));
        rvQueue.setNestedScrollingEnabled(false);
        rvQueue.setAdapter(queueAdapter);
        queueAdapter.setOnCallClickListener(this::callSpecific);
    }

    // ─── Click Listeners ─────────────────────────────────────────────────────

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> advanceQueue());

        if (btnSkip != null)
            btnSkip.setOnClickListener(v -> skipCurrentBooking());

        if (btnCallNext != null)
            btnCallNext.setOnClickListener(v -> advanceQueue());

        if (btnMarkDone != null)
            btnMarkDone.setOnClickListener(v -> markCurrentAsDone());

        // "Announce" quick action → full management screen
        if (btnAnnounce != null)
            btnAnnounce.setOnClickListener(v -> openAnnouncementsManagement());
    }

    private void openAnnouncementsManagement() {
        Intent intent = new Intent(this, AnnouncementsActivity.class);
        intent.putExtra(AnnouncementsActivity.EXTRA_ADMIN_MODE, true);
        startActivity(intent);
    }

    // ─── Barangay Name ───────────────────────────────────────────────────────

    private void loadBarangayName() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    barangayId = userDoc.getString("barangayId");
                    if (barangayId == null || barangayId.isEmpty()) return;
                    startListeners();
                    startTodaySlotsListener();
                    loadAnalytics();
                    checkPastDateSlots();
                    startSlotTimeMonitoring();
                    if (tvBarangayName == null) return;
                    db.collection("barangays").document(barangayId).get()
                            .addOnSuccessListener(brgyDoc -> {
                                String name = brgyDoc.getString("name");
                                if (name != null && !name.isEmpty())
                                    tvBarangayName.setText("Brgy. " + name);
                            });
                });
    }

    // ─── Firestore Listeners ─────────────────────────────────────────────────

    private void startListeners() {
        // Single listener on date only — avoids composite index requirement.
        // barangayId and status are filtered client-side so existing Firestore
        // documents that pre-date the barangayId field are handled correctly.
        bookingsListener = db.collection("bookings")
                .whereEqualTo("barangayId", barangayId)
                .whereEqualTo("date", todayStr)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e("AdminActivity", "bookingsListener error — will retry", error);
                        bookingsListener = null;
                        slotTimeHandler.postDelayed(() -> {
                            if (barangayId != null && bookingsListener == null) startListeners();
                        }, 5_000);
                        return;
                    }
                    if (snapshots == null) return;

                    allTodayWaiting.clear();
                    currentServingBooking = null;
                    servedCount = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        // Filter barangayId client-side — field may be absent in older docs
                        String docBarangayId = doc.getString("barangayId");
                        if (docBarangayId != null && !docBarangayId.isEmpty()
                                && !docBarangayId.equals(barangayId)) continue;

                        Booking b = doc.toObject(Booking.class);
                        b.setId(doc.getId());

                        String status = b.getStatus();
                        if ("waiting".equals(status)) {
                            allTodayWaiting.add(b);
                        } else if ("serving".equals(status)) {
                            // Always capture the serving booking regardless of slot active
                            // window — the admin must be able to Mark Done at any point.
                            currentServingBooking = b;
                        } else if ("done".equals(status)) {
                            servedCount++;
                        }
                    }

                    Collections.sort(allTodayWaiting, (a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    });

                    refreshWaitingDisplay();
                    updateServingCard();
                    updateStats();
                    updateQueueInteractability();
                });
    }

    /** Listens to today's queue slots to know which are currently active. */
    private void startTodaySlotsListener() {
        // Query by date only — avoids composite index and handles slots created
        // before the barangayId field was added. barangayId filtered client-side.
        todaySlotsListener = db.collection("queue_slots")
                .whereEqualTo("barangayId", barangayId)
                .whereEqualTo("date", todayStr)
                .addSnapshotListener((snaps, err) -> {
                    if (err != null) {
                        android.util.Log.e("AdminActivity", "todaySlotsListener error", err);
                        return;
                    }
                    if (snaps == null) return;
                    todaySlots.clear();
                    for (QueryDocumentSnapshot doc : snaps) {
                        if (!"open".equals(doc.getString("status"))) continue;
                        // Filter barangayId client-side — field may be absent in older docs
                        String docBarangayId = doc.getString("barangayId");
                        if (docBarangayId != null && !docBarangayId.isEmpty()
                                && !docBarangayId.equals(barangayId)) continue;
                        Slot s = doc.toObject(Slot.class);
                        s.setId(doc.getId());
                        todaySlots.add(s);
                        android.util.Log.d("AdminActivity", "Slot loaded: " + s.getTimeRange()
                                + " status=" + s.getStatus() + " id=" + s.getId());
                    }
                    android.util.Log.d("AdminActivity", "todaySlots count: " + todaySlots.size());
                    updateActiveSlots();
                });
    }

    // ─── Active Slot Logic ───────────────────────────────────────────────────

    /**
     * Recalculates which of today's slots fall within the current clock time,
     * then refreshes the queue display and button states.
     */
    private void updateActiveSlots() {
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        android.util.Log.d("AdminActivity", "updateActiveSlots: nowMinutes=" + nowMinutes
                + " todaySlots=" + todaySlots.size());

        Set<String> newActive = new HashSet<>();
        for (Slot slot : todaySlots) {
            int[] range = parseTimeRange(slot.getTimeRange());
            // A slot is active only between its start time and end time.
            // Once the end time has passed the slot is over and must leave
            // the active set so the banner/buttons update correctly.
            boolean active = range != null && nowMinutes >= range[0] && nowMinutes < range[1];
            android.util.Log.d("AdminActivity", "  slot=" + slot.getTimeRange()
                    + " range=" + (range != null ? range[0] + "-" + range[1] : "null")
                    + " active=" + active);
            if (active) {
                newActive.add(slot.getId());
            }
        }

        boolean changed = !newActive.equals(activeSlotIds);
        activeSlotIds.clear();
        activeSlotIds.addAll(newActive);

        if (changed) {
            refreshWaitingDisplay();
        }
        updateServingCard();
        updateActiveSlotBanner();
        updateQueueInteractability();
    }

    /**
     * Applies activeSlotIds filter to allTodayWaiting and updates the queue adapter.
     * Only bookings belonging to a currently-active slot are shown.
     */
    private void refreshWaitingDisplay() {
        waitingBookings.clear();
        for (Booking b : allTodayWaiting) {
            if (b.getSlotId() != null && activeSlotIds.contains(b.getSlotId())) {
                waitingBookings.add(b);
            }
        }

        queueAdapter.setBookings(waitingBookings);
        updateEmptyState();
        updateStats();
        updateQueueInteractability();

        int count = waitingBookings.size();
        if (tvWaitingCount != null)
            tvWaitingCount.setText(count + " waiting");
    }

    /** Updates the active slot info banner shown to the admin. */
    private void updateActiveSlotBanner() {
        if (tvActiveSlotBanner == null) return;
        if (activeSlotIds.isEmpty()) {
            tvActiveSlotBanner.setVisibility(View.GONE);
            return;
        }
        // Find the first active slot and display its service + time range
        for (Slot slot : todaySlots) {
            if (activeSlotIds.contains(slot.getId())) {
                String service   = slot.getServiceName() != null ? slot.getServiceName() : "Queue";
                String timeRange = slot.getTimeRange()   != null ? slot.getTimeRange()   : "";
                tvActiveSlotBanner.setText("ACTIVE: " + service + "  •  " + timeRange);
                tvActiveSlotBanner.setVisibility(View.VISIBLE);
                return;
            }
        }
        tvActiveSlotBanner.setVisibility(View.GONE);
    }

    /** Enables or disables queue action buttons based on active slot + serving state. */
    private void updateQueueInteractability() {
        boolean hasActiveSlot = !activeSlotIds.isEmpty();
        boolean hasServing    = currentServingBooking != null;
        boolean hasWaiting    = !waitingBookings.isEmpty();

        // "Next" / "Call Next" — can advance if already serving someone OR
        // there is an active slot with people waiting.
        boolean canAdvance = hasServing || (hasActiveSlot && hasWaiting);
        btnNext.setEnabled(canAdvance);
        btnNext.setAlpha(canAdvance ? 1f : 0.4f);

        if (btnSkip != null) {
            // Skip is always available when someone is being served,
            // regardless of whether the slot time window is currently active.
            btnSkip.setEnabled(hasServing);
            btnSkip.setAlpha(hasServing ? 1f : 0.4f);
        }

        if (btnCallNext != null) {
            btnCallNext.setEnabled(hasActiveSlot && hasWaiting);
            btnCallNext.setAlpha(hasActiveSlot && hasWaiting ? 1f : 0.4f);
        }

        if (btnMarkDone != null) {
            // Mark Done must always be enabled when someone is in "serving" state,
            // even if the slot time window is no longer active — the admin needs
            // to be able to close out the current ticket.
            btnMarkDone.setEnabled(hasServing);
            btnMarkDone.setAlpha(hasServing ? 1f : 0.4f);
        }

        // Only show "No active slot" message when truly nothing is happening.
        // If someone is already being served, keep the serving card populated.
        if (!hasActiveSlot && !hasServing) {
            tvServingNumber.setText("—");
            if (tvServingName != null) tvServingName.setText("");
            tvServingService.setText("No active slot right now");
        }

        updateActiveSlotBanner();
    }

    // ─── Queue Actions ───────────────────────────────────────────────────────

    private void advanceQueue() {
        if (currentServingBooking != null) {
            String doneId = currentServingBooking.getId();
            db.collection("bookings").document(doneId)
                    .update("status", "done")
                    .addOnSuccessListener(unused -> callNextWaiting())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to advance queue", Toast.LENGTH_SHORT).show());
        } else {
            callNextWaiting();
        }
    }

    private void skipCurrentBooking() {
        if (currentServingBooking == null) {
            Toast.makeText(this, "No one is being served", Toast.LENGTH_SHORT).show();
            return;
        }
        String skippedId = currentServingBooking.getId();
        String queueNum  = currentServingBooking.getQueueNumber();
        db.collection("bookings").document(skippedId)
                .update("status", "skipped")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, queueNum + " skipped", Toast.LENGTH_SHORT).show();
                    callNextWaiting();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to skip", Toast.LENGTH_SHORT).show());
    }

    private void callNextWaiting() {
        if (waitingBookings.isEmpty()) {
            Toast.makeText(this, "No one in queue", Toast.LENGTH_SHORT).show();
            return;
        }
        Booking next = waitingBookings.get(0);
        db.collection("bookings").document(next.getId())
                .update("status", "serving")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Now calling " + next.getQueueNumber(),
                            Toast.LENGTH_SHORT).show();
                    if (next.getSlotId() != null) {
                        db.collection("queue_slots").document(next.getSlotId())
                                .update("currentServing", next.getQueueNumber());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to call next", Toast.LENGTH_SHORT).show());
    }

    private void callSpecific(Booking booking) {
        if (currentServingBooking != null
                && !currentServingBooking.getId().equals(booking.getId())) {
            db.collection("bookings").document(currentServingBooking.getId())
                    .update("status", "done")
                    .addOnSuccessListener(unused -> setServing(booking));
        } else {
            setServing(booking);
        }
    }

    private void setServing(Booking booking) {
        db.collection("bookings").document(booking.getId())
                .update("status", "serving")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Now calling " + booking.getQueueNumber(),
                            Toast.LENGTH_SHORT).show();
                    if (booking.getSlotId() != null) {
                        db.collection("queue_slots").document(booking.getSlotId())
                                .update("currentServing", booking.getQueueNumber());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to call", Toast.LENGTH_SHORT).show());
    }

    private void markCurrentAsDone() {
        if (currentServingBooking == null) {
            Toast.makeText(this, "No one is being served", Toast.LENGTH_SHORT).show();
            return;
        }
        String queueNum = currentServingBooking.getQueueNumber();
        db.collection("bookings").document(currentServingBooking.getId())
                .update("status", "done")
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, queueNum + " marked as done", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
    }

    // ─── Slot Time Monitoring ────────────────────────────────────────────────

    private void checkPastDateSlots() {
        if (barangayId == null) return;
        // Query by status only — avoids composite index; filter barangayId + date client-side.
        db.collection("queue_slots")
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot slotDoc : snap) {
                        String date = slotDoc.getString("date");
                        if (date == null) continue;
                        String docBarangayId = slotDoc.getString("barangayId");
                        if (docBarangayId != null && !docBarangayId.isEmpty()
                                && !docBarangayId.equals(barangayId)) continue;
                        if (date.compareTo(todayStr) < 0) {
                            String slotId = slotDoc.getId();
                            processedExpiredSlots.add(slotId);
                            slotDoc.getReference().update("status", "closed");
                            markExpiredSlotBookingsAsSkipped(slotId);
                        }
                    }
                });
    }

    private void startSlotTimeMonitoring() {
        slotTimeChecker = new Runnable() {
            @Override
            public void run() {
                checkSlotTimes();
                slotTimeHandler.postDelayed(this, 60_000);
            }
        };
        slotTimeChecker.run();
    }

    private void checkSlotTimes() {
        if (barangayId == null) return;

        // Re-evaluate which of today's slots are active based on current clock
        updateActiveSlots();

        // Separately: check for expired slots and send notifications / close them
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (Slot slot : new ArrayList<>(todaySlots)) {
            String slotId   = slot.getId();
            String timeRange = slot.getTimeRange();
            String serviceNm = slot.getServiceName();
            if (timeRange == null) continue;

            int[] range = parseTimeRange(timeRange);
            if (range == null) continue;

            int startMin = range[0];
            int endMin   = range[1];

            if (nowMinutes >= startMin && nowMinutes < endMin) {
                if (!notifiedActiveSlots.contains(slotId)) {
                    notifiedActiveSlots.add(slotId);
                    NotificationHelper.showQueueNotification(
                            AdminActivity.this,
                            "Queue Slot Now Active",
                            (serviceNm != null ? serviceNm : "Queue") +
                                    " slot " + timeRange + " is now active. Start serving!");
                }
            } else if (nowMinutes >= endMin) {
                if (!processedExpiredSlots.contains(slotId)) {
                    processedExpiredSlots.add(slotId);
                    NotificationHelper.showQueueNotification(
                            AdminActivity.this,
                            "Queue Slot Ended",
                            (serviceNm != null ? serviceNm : "Queue") +
                                    " slot " + timeRange + " has ended.");
                    markExpiredSlotBookingsAsSkipped(slotId);
                }
            }
        }
    }

    private void markExpiredSlotBookingsAsSkipped(String slotId) {
        // Single-field query to avoid composite index; filter status client-side.
        db.collection("bookings")
                .whereEqualTo("slotId", slotId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        if ("waiting".equals(doc.getString("status"))) {
                            doc.getReference().update("status", "skipped");
                        }
                    }
                });
    }

    private int[] parseTimeRange(String timeRange) {
        if (timeRange == null) return null;
        try {
            // Split on any dash variant: hyphen (-), en-dash (–), em-dash (—)
            String[] parts = timeRange.split("\\s*[\\-\u2013\u2014]\\s*");
            if (parts.length < 2) return null;
            int start = parseTimeToMinutes(parts[0].trim());
            int end   = parseTimeToMinutes(parts[1].trim());
            if (start < 0 || end < 0) return null;
            return new int[]{start, end};
        } catch (Exception e) {
            android.util.Log.e("AdminActivity", "parseTimeRange failed for: " + timeRange, e);
            return null;
        }
    }

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return -1;
        // Try h:mm a first, then h a (e.g. "8 AM")
        String[] formats = { "h:mm a", "h a", "hh:mm a" };
        for (String fmt : formats) {
            for (Locale locale : new Locale[]{ Locale.getDefault(), Locale.US, Locale.ENGLISH }) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(fmt, locale);
                    sdf.setLenient(false);
                    Date d = sdf.parse(timeStr);
                    if (d == null) continue;
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
                } catch (ParseException ignored) {}
            }
        }
        android.util.Log.e("AdminActivity", "parseTimeToMinutes failed for: " + timeStr);
        return -1;
    }

    // ─── Analytics ───────────────────────────────────────────────────────────

    private void loadAnalytics() {
        if (chartPeakHours == null) return;

        // Filter server-side to today's date — same field used by queue listeners.
        // Group by the slot's start hour so the chart reflects actual scheduled
        // bookings for today, not when residents happened to tap "Book".
        db.collection("bookings")
                .whereEqualTo("barangayId", barangayId)
                .whereEqualTo("date", todayStr)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<Integer, Integer> hourMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String timeRange = doc.getString("timeRange");
                        if (timeRange == null) continue;
                        int[] range = parseTimeRange(timeRange);
                        if (range == null) continue;
                        int startHour = range[0] / 60; // convert minutes → hour
                        hourMap.put(startHour, hourMap.getOrDefault(startHour, 0) + 1);
                    }
                    chartPeakHours.setData(hourMap);
                });
    }

    // ─── UI Updates ──────────────────────────────────────────────────────────

    private void updateServingCard() {
        if (currentServingBooking != null) {
            String qNum = currentServingBooking.getQueueNumber();
            tvServingNumber.setText(qNum != null ? qNum : "—");
            if (tvServingName != null)
                tvServingName.setText(currentServingBooking.getResidentName() != null
                        ? currentServingBooking.getResidentName() : "");
            tvServingService.setText(currentServingBooking.getServiceName() != null
                    ? currentServingBooking.getServiceName() : "");
        } else {
            tvServingNumber.setText("—");
            if (tvServingName != null) tvServingName.setText("");
            boolean hasActiveSlot = !activeSlotIds.isEmpty();
            tvServingService.setText(hasActiveSlot
                    ? "No one being served"
                    : "No active slot right now");
        }
    }

    private void updateStats() {
        int inQueue = waitingBookings.size();
        tvStatInQueue.setText(String.valueOf(inQueue));
        tvStatServed.setText(String.valueOf(servedCount));
        tvStatRemaining.setText(String.valueOf(inQueue));
    }

    private void updateEmptyState() {
        if (waitingBookings.isEmpty()) {
            rvQueue.setVisibility(View.GONE);
            tvEmptyQueue.setVisibility(View.VISIBLE);
        } else {
            rvQueue.setVisibility(View.VISIBLE);
            tvEmptyQueue.setVisibility(View.GONE);
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    private void setupNavigation() {
        if (navDashboard != null)
            navDashboard.setOnClickListener(v -> {
                setActiveNav(navDashboard);
                showDashboard();
            });

        if (navQueue != null)
            navQueue.setOnClickListener(v -> {
                setActiveNav(navQueue);
                showQueue();
            });

        if (navServices != null)
            navServices.setOnClickListener(v -> {
                leavingToSubActivity = true;
                setActiveNav(navServices);
                startActivity(new Intent(this, AdminServicesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            });

        if (navProfile != null)
            navProfile.setOnClickListener(v -> {
                leavingToSubActivity = true;
                setActiveNav(navProfile);
                startActivity(new Intent(this, AdminProfileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            });
    }

    private void showDashboard() {
        if (layoutQuickActions != null) layoutQuickActions.setVisibility(View.VISIBLE);
        if (cardPeakHours != null)      cardPeakHours.setVisibility(View.VISIBLE);
        if (cardQueueSection != null)   cardQueueSection.setVisibility(View.GONE);
        if (adminNestedScroll != null)  adminNestedScroll.smoothScrollTo(0, 0);
    }

    private void showQueue() {
        if (layoutQuickActions != null) layoutQuickActions.setVisibility(View.GONE);
        if (cardPeakHours != null)      cardPeakHours.setVisibility(View.GONE);
        if (cardQueueSection != null)   cardQueueSection.setVisibility(View.VISIBLE);
        if (adminNestedScroll != null)  adminNestedScroll.post(() -> adminNestedScroll.smoothScrollTo(0, 0));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("show_queue", false)) {
            setActiveNav(navQueue);
            showQueue();
        } else {
            setActiveNav(navDashboard);
            showDashboard();
        }
    }

    private void setActiveNav(LinearLayout activeNav) {
        LinearLayout[] allNavs = {navDashboard, navQueue, navServices, navProfile};
        for (LinearLayout nav : allNavs) {
            if (nav == null) continue;
            LinearLayout iconBg = (LinearLayout) nav.getChildAt(0);
            TextView label = (TextView) nav.getChildAt(1);
            if (nav == activeNav) {
                if (iconBg != null) iconBg.setBackgroundResource(R.drawable.bg_nav_active);
                if (label != null) label.setTextColor(ContextCompat.getColor(this, R.color.blue_600));
            } else {
                if (iconBg != null) iconBg.setBackground(null);
                if (label != null) label.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
            }
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        if (barangayId != null) {
            if (bookingsListener == null)    startListeners();
            if (todaySlotsListener == null)  startTodaySlotsListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bookingsListener != null) {
            bookingsListener.remove();
            bookingsListener = null;
        }
        if (todaySlotsListener != null) {
            todaySlotsListener.remove();
            todaySlotsListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (leavingToSubActivity) {
            leavingToSubActivity = false;
            setActiveNav(navDashboard);
            showDashboard();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slotTimeChecker != null) slotTimeHandler.removeCallbacks(slotTimeChecker);
    }
}