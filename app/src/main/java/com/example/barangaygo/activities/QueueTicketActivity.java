package com.example.barangaygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.barangaygo.R;
import com.example.barangaygo.utils.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class QueueTicketActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration queueListener;
    private ListenerRegistration slotListener;
    private String bookingId;
    private String lastKnownStatus = "";
    private String myQueueNumber = "";
    private String currentSlotId = "";
    private String bookingDateField = "";

    // Ticket header (changes colour with status)
    private RelativeLayout ticketHeader;

    // Ticket views
    private TextView tvTicketNumber, tvTicketService;
    private TextView tvStatusBadge, tvNowServing;
    private TextView tvAhead, tvEstWait, tvResidentName, tvDate;
    private TextView tvProgressStart, tvProgressCurrent, tvProgressEnd;
    private ProgressBar progressQueue;
    private SwitchMaterial switchNotification;

    // Empty-state book-now
    private CardView cardBookNowPrompt;
    private MaterialButton btnGoBook;

    // Nav
    private LinearLayout navHome, navBook, navQueue, navProfile;
    private android.widget.ImageView navHomeIcon, navBookIcon, navQueueIcon, navProfileIcon;
    private LinearLayout btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_ticket);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        bookingId = getIntent().getStringExtra("bookingId");

        NotificationHelper.createChannels(this);

        initViews();
        setupBackButton();
        setupNavigation();
        setActiveNav(navQueue);

        if (bookingId != null && !bookingId.isEmpty()) {
            loadBookingData();
            startRealTimeListener();
        } else {
            showEmptyState();
        }
    }

    private void initViews() {
        ticketHeader = findViewById(R.id.ticket_header);

        tvTicketNumber = findViewById(R.id.tv_ticket_number);
        tvTicketService = findViewById(R.id.tv_ticket_service);
        tvStatusBadge = findViewById(R.id.tv_status_badge);
        tvNowServing = findViewById(R.id.tv_now_serving);
        tvAhead = findViewById(R.id.tv_ahead);
        tvEstWait = findViewById(R.id.tv_est_wait);
        tvResidentName = findViewById(R.id.tv_resident_name);
        tvDate = findViewById(R.id.tv_date);
        progressQueue = findViewById(R.id.progress_queue);
        tvProgressStart = findViewById(R.id.tv_progress_start);
        tvProgressCurrent = findViewById(R.id.tv_progress_current);
        tvProgressEnd = findViewById(R.id.tv_progress_end);
        switchNotification = findViewById(R.id.switch_notification);

        cardBookNowPrompt = findViewById(R.id.card_book_now_prompt);
        btnGoBook = findViewById(R.id.btn_go_book);

        btnBack = findViewById(R.id.btn_back);

        navHome    = findViewById(R.id.nav_home);
        navBook    = findViewById(R.id.nav_book);
        navQueue   = findViewById(R.id.nav_queue);
        navProfile = findViewById(R.id.nav_profile);

        navHomeIcon    = findViewById(R.id.nav_home_icon);
        navBookIcon    = findViewById(R.id.nav_book_icon);
        navQueueIcon   = findViewById(R.id.nav_queue_icon);
        navProfileIcon = findViewById(R.id.nav_profile_icon);

        // Clear default XML placeholder text
        tvNowServing.setText("—");
        tvAhead.setText("—");
        tvEstWait.setText("—");
    }

    private void setupBackButton() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    // ─── Empty state (no booking at all) ─────────────────────────────────────

    private void showEmptyState() {
        tvTicketNumber.setText("—");
        tvTicketService.setText("No Queue Booked");
        tvResidentName.setText("—");
        tvDate.setText("—");
        tvNowServing.setText("—");
        tvAhead.setText("—");
        tvEstWait.setText("—");
        progressQueue.setProgress(0);
        tvProgressStart.setText("A-001");
        tvProgressCurrent.setText("Now: —");
        tvProgressEnd.setText("—");

        tvStatusBadge.setText(getString(R.string.status_no_booking));
        tvStatusBadge.setTextColor(0xFF6B5548);
        tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_skipped);

        if (ticketHeader != null) {
            ticketHeader.setBackgroundResource(R.drawable.bg_header_gradient_grey);
        }

        if (cardBookNowPrompt != null) cardBookNowPrompt.setVisibility(View.VISIBLE);
        if (btnGoBook != null) {
            btnGoBook.setOnClickListener(v -> openBookTab());
        }
    }

    // ─── Booking data ─────────────────────────────────────────────────────────

    private void loadBookingData() {
        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String qNum = doc.getString("queueNumber");
                        String service = doc.getString("serviceName");
                        String resident = doc.getString("residentName");
                        Long ahead = doc.getLong("aheadCount");
                        String slotId = doc.getString("slotId");
                        String status = doc.getString("status");

                        if (qNum != null) {
                            myQueueNumber = qNum;
                            tvTicketNumber.setText(qNum);
                            tvProgressEnd.setText(qNum);
                        }
                        if (service != null) tvTicketService.setText(service);
                        if (resident != null) tvResidentName.setText(resident);

                        if (ahead != null) {
                            tvAhead.setText(ahead + " " + getString(R.string.people));
                            long waitMins = ahead * 5;
                            tvEstWait.setText("~" + waitMins + " " + getString(R.string.minutes));
                        }

                        String bookingDate = doc.getString("date");
                        if (bookingDate != null) {
                            bookingDateField = bookingDate;
                            try {
                                SimpleDateFormat inSdf  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                SimpleDateFormat outSdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                                Date parsed = inSdf.parse(bookingDate);
                                if (parsed != null) tvDate.setText(outSdf.format(parsed));
                            } catch (ParseException e) {
                                tvDate.setText(bookingDate);
                            }
                        }

                        // If still waiting but the booking date is in the past, treat as skipped
                        if ("waiting".equals(status) && isPastDate(bookingDate)) {
                            status = "skipped";
                            doc.getReference().update("status", "skipped");
                        }

                        // If waiting or serving but the slot's time window has already ended
                        // today, the resident was never actually called — mark as skipped.
                        String bookingTimeRange = doc.getString("timeRange");
                        if (("waiting".equals(status) || "serving".equals(status))
                                && isSlotTimePastForDate(bookingDate, bookingTimeRange)) {
                            status = "skipped";
                            doc.getReference().update("status", "skipped");
                        }

                        if (status != null) {
                            updateStatusBadge(status);
                            lastKnownStatus = status;
                        }

                        if (slotId != null) {
                            currentSlotId = slotId;
                            startSlotListener(slotId);
                        }
                    }
                });
    }

    private void startSlotListener(String slotId) {
        slotListener = db.collection("queue_slots").document(slotId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    String timeRange = doc.getString("timeRange");
                    String slotDate  = doc.getString("date");

                    // Use bookingDateField as fallback if slot has no date
                    String effectiveDate = (slotDate != null && !slotDate.isEmpty())
                            ? slotDate : bookingDateField;

                    boolean isSlotActive = isSlotCurrentlyActive(effectiveDate, timeRange);

                    if (!isSlotActive) {
                        tvNowServing.setText("—");
                        tvProgressCurrent.setText("Waiting to start");
                        tvProgressStart.setText(formatQueueNum(1));
                        progressQueue.setProgress(0);

                        // Show when the queue starts
                        String startLabel = buildStartLabel(effectiveDate, timeRange);
                        tvEstWait.setText(startLabel);
                        return;
                    }

                    String serving = doc.getString("currentServing");
                    tvNowServing.setText(serving != null ? serving : "—");
                    tvProgressCurrent.setText("Now: " + (serving != null ? serving : "—"));

                    int myNum = parseQueueNum(myQueueNumber);
                    int servingNum = serving != null ? parseQueueNum(serving) : 0;
                    tvProgressStart.setText(formatQueueNum(1));
                    if (myNum > 1 && servingNum >= 1) {
                        int progress = (int) Math.min(100, (servingNum * 100) / myNum);
                        progressQueue.setProgress(progress);
                    }
                });
    }

    private boolean isSlotCurrentlyActive(String dateStr, String timeRange) {
        if (dateStr == null || timeRange == null) return false;
        try {
            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date slotDate = dateSdf.parse(dateStr);
            Calendar todayCal = Calendar.getInstance();
            Calendar slotCal  = Calendar.getInstance();
            if (slotDate != null) slotCal.setTime(slotDate);
            if (slotCal.get(Calendar.YEAR)         != todayCal.get(Calendar.YEAR)  ||
                    slotCal.get(Calendar.DAY_OF_YEAR)  != todayCal.get(Calendar.DAY_OF_YEAR)) {
                return false;
            }
        } catch (ParseException e) {
            return false;
        }
        int[] range = parseTimeRange(timeRange);
        if (range == null) return false;
        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return nowMin >= range[0] && nowMin < range[1];
    }

    private int[] parseTimeRange(String timeRange) {
        if (timeRange == null) return null;
        try {
            // Split on any dash variant: hyphen (-), en-dash (–), em-dash (—)
            String[] parts = timeRange.split("\\s*[\\-\u2013\u2014]\\s*");
            if (parts.length < 2) return null;
            return new int[]{ parseTimePart(parts[0].trim()), parseTimePart(parts[1].trim()) };
        } catch (Exception e) {
            return null;
        }
    }

    private int parseTimePart(String t) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(t.contains(":") ? "h:mm a" : "h a", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        c.setTime(sdf.parse(t));
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    private String buildStartLabel(String dateStr, String timeRange) {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        if (dateStr != null && !dateStr.equals(todayStr)) {
            try {
                SimpleDateFormat inSdf  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outSdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                Date d = inSdf.parse(dateStr);
                String datePart = (d != null) ? outSdf.format(d) : dateStr;
                String startTime = timeRange != null ? timeRange.split("\\s*[–-]\\s*")[0].trim() : "";
                return "Starts " + datePart + (startTime.isEmpty() ? "" : " at " + startTime);
            } catch (ParseException e) {
                return "Starts " + dateStr;
            }
        }
        if (timeRange != null) {
            String startTime = timeRange.split("\\s*[–-]\\s*")[0].trim();
            return "Starts at " + startTime;
        }
        return "Not started yet";
    }

    // ─── Real-time booking status listener ───────────────────────────────────

    private void startRealTimeListener() {
        DocumentReference bookingRef = db.collection("bookings").document(bookingId);
        queueListener = bookingRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            Long ahead = snapshot.getLong("aheadCount");
            String status = snapshot.getString("status");

            // If the booking is still waiting/serving but the slot time has already
            // passed, override the status to skipped so the UI reflects reality.
            // Important: do NOT mark "serving" as skipped — the admin may have
            // called this resident slightly after the slot window ended.
            if ("waiting".equals(status)) {
                String liveTimeRange = snapshot.getString("timeRange");
                String liveDate      = snapshot.getString("date");
                if (isSlotTimePastForDate(liveDate, liveTimeRange)) {
                    status = "skipped";
                    snapshot.getReference().update("status", "skipped");
                }
            }

            if (ahead != null && !"skipped".equals(status)) {
                tvAhead.setText(ahead + " " + getString(R.string.people));
                long waitMins = ahead * 5;
                tvEstWait.setText("~" + waitMins + " " + getString(R.string.minutes));
            }

            if (status != null) {
                updateStatusBadge(status);

                if ("serving".equals(status) && !"serving".equals(lastKnownStatus)) {
                    NotificationHelper.showQueueNotification(this,
                            "It's your turn!",
                            "You are now being called. Please proceed to the service window.");
                } else if ("done".equals(status) && !"done".equals(lastKnownStatus)) {
                    NotificationHelper.showQueueNotification(this,
                            "Transaction complete",
                            "Your queue transaction has been marked as done. Thank you!");
                } else if ("skipped".equals(status) && !"skipped".equals(lastKnownStatus)) {
                    NotificationHelper.showQueueNotification(this,
                            "Missed your turn",
                            "You were marked as missed. Please rebook to get a new queue slot.");
                    tvAhead.setText("—");
                    tvEstWait.setText("Please rebook");
                }

                lastKnownStatus = status;
            }
        });
    }

    // ─── Status badge + header colour ────────────────────────────────────────

    private void updateStatusBadge(String status) {
        switch (status) {
            case "waiting":
                tvStatusBadge.setText(getString(R.string.status_waiting));
                tvStatusBadge.setTextColor(0xFF92400E);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_waiting);
                if (ticketHeader != null)
                    ticketHeader.setBackgroundResource(R.drawable.bg_header_gradient);
                if (cardBookNowPrompt != null) cardBookNowPrompt.setVisibility(View.GONE);
                break;

            case "serving":
                tvStatusBadge.setText(getString(R.string.status_serving));
                tvStatusBadge.setTextColor(0xFF065F46);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active);
                if (ticketHeader != null)
                    ticketHeader.setBackgroundResource(R.drawable.bg_header_gradient_green);
                if (cardBookNowPrompt != null) cardBookNowPrompt.setVisibility(View.GONE);
                break;

            case "done":
                tvStatusBadge.setText(getString(R.string.status_done));
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.blue_800));
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_done);
                if (ticketHeader != null)
                    ticketHeader.setBackgroundResource(R.drawable.bg_header_gradient);
                if (cardBookNowPrompt != null) cardBookNowPrompt.setVisibility(View.GONE);
                break;

            case "skipped":
                tvStatusBadge.setText(getString(R.string.status_skipped));
                tvStatusBadge.setTextColor(0xFF6B5548);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_skipped);
                if (ticketHeader != null)
                    ticketHeader.setBackgroundResource(R.drawable.bg_header_gradient_grey);
                if (cardBookNowPrompt != null) {
                    cardBookNowPrompt.setVisibility(View.VISIBLE);
                    if (btnGoBook != null) btnGoBook.setOnClickListener(v -> openBookTab());
                }
                // Update card to show rescheduling message
                tvAhead.setText("—");
                tvEstWait.setText("Please rebook");
                break;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void openBookTab() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("show_book", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private boolean isPastDate(String dateStr) {
        if (dateStr == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date bookingDate = sdf.parse(dateStr);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            return bookingDate != null && bookingDate.before(today.getTime());
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Returns true when the booking's slot time window has already ended.
     * Handles two cases:
     *   1. The booking date is a past day  → always expired.
     *   2. The booking date is today       → expired only when current time ≥ slot end time.
     */
    private boolean isSlotTimePastForDate(String dateStr, String timeRange) {
        if (dateStr == null || timeRange == null) return false;
        // Past day: unconditionally expired
        if (isPastDate(dateStr)) return true;
        // Same day: check whether the end time has already passed
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        if (!dateStr.equals(today)) return false;
        int[] range = parseTimeRange(timeRange);
        if (range == null) return false;
        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return nowMin >= range[1]; // current time is at or past the slot's end time
    }

    private int parseQueueNum(String queueNumber) {
        if (queueNumber == null) return 0;
        try {
            return Integer.parseInt(queueNumber.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatQueueNum(int num) {
        return String.format(Locale.getDefault(), "A-%03d", num);
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void setupNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            });
        }
        if (navBook != null) {
            navBook.setOnClickListener(v -> openBookTab());
        }
        if (navQueue != null) {
            navQueue.setOnClickListener(v -> {
                startActivity(new Intent(this, MyQueuesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                finish();
            });
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            });
        }
    }

    private void setActiveNav(LinearLayout activeNav) {
        LinearLayout[]             allNavs  = {navHome,    navBook,    navQueue,    navProfile};
        android.widget.ImageView[] allIcons = {navHomeIcon, navBookIcon, navQueueIcon, navProfileIcon};

        for (int i = 0; i < allNavs.length; i++) {
            LinearLayout nav  = allNavs[i];
            android.widget.ImageView icon = allIcons[i];
            if (nav == null) continue;

            LinearLayout iconBg = (LinearLayout) nav.getChildAt(0);
            TextView label      = (TextView)     nav.getChildAt(1);
            boolean isActive    = (nav == activeNav);

            if (iconBg != null) {
                if (isActive) iconBg.setBackgroundResource(R.drawable.bg_nav_active);
                else          iconBg.setBackground(null);
            }
            if (label != null) {
                label.setTextColor(ContextCompat.getColor(this,
                        isActive ? R.color.blue_600 : R.color.gray_400));
            }
            if (icon != null) {
                icon.setColorFilter(ContextCompat.getColor(this,
                        isActive ? R.color.blue_600 : R.color.gray_400));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueListener != null) queueListener.remove();
        if (slotListener != null) slotListener.remove();
    }
}