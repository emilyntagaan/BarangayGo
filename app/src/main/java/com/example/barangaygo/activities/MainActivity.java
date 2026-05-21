package com.example.barangaygo.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.adapters.AnnouncementAdapter;
import com.example.barangaygo.adapters.ServiceAdapter;
import com.example.barangaygo.models.Announcement;
import com.example.barangaygo.models.Service;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration announcementsListener;
    private ListenerRegistration servicesListener;
    private ListenerRegistration queueListener;

    // Header views
    private TextView tvGreeting, tvUserName;

    // Active queue card views
    private LinearLayout cardActiveQueue;
    private LinearLayout layoutNoQueue, layoutHasQueue, layoutServingMsg;
    private TextView tvQueueNumber, tvQueueService, tvAheadInfo, tvQueueStatusBadge;

    // Active booking tracking
    private String activeBookingId = null;
    private String barangayId = null;

    // Nav views
    private LinearLayout navHome, navBook, navQueue, navProfile;

    // Home scroll container
    private NestedScrollView nestedScrollView;

    // Book tab container
    private NestedScrollView viewBookContainer;
    private RecyclerView rvBookServices;
    private TextView tvBookNoServices;
    private BookAdapter bookAdapter;

    // Dynamic services grid (home tab)
    private RecyclerView rvServices;
    private TextView tvNoServices;
    private ServiceAdapter serviceAdapter;

    // Announcements
    private RecyclerView rvHomeAnnouncements;
    private TextView tvNoAnnouncements;
    private AnnouncementAdapter announcementAdapter;

    // Track when leaving to a sub-activity so we can reset nav on return
    private boolean leavingToSubActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setGreeting();
        loadUserData();
        setupNavigation();
        setActiveNav(navHome);
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvUserName = findViewById(R.id.tv_user_name);

        cardActiveQueue    = findViewById(R.id.card_active_queue);
        layoutNoQueue      = findViewById(R.id.layout_no_queue);
        layoutHasQueue     = findViewById(R.id.layout_has_queue);
        layoutServingMsg   = findViewById(R.id.layout_serving_msg);
        tvQueueNumber      = findViewById(R.id.tv_queue_number);
        tvQueueService     = findViewById(R.id.tv_queue_service);
        tvAheadInfo        = findViewById(R.id.tv_ahead_info);
        tvQueueStatusBadge = findViewById(R.id.tv_queue_status_badge);

        navHome    = findViewById(R.id.nav_home);
        navBook    = findViewById(R.id.nav_book);
        navQueue   = findViewById(R.id.nav_queue);
        navProfile = findViewById(R.id.nav_profile);

        nestedScrollView    = findViewById(R.id.nested_scroll_view);
        rvServices          = findViewById(R.id.rv_services);
        tvNoServices        = findViewById(R.id.tv_no_services);
        rvHomeAnnouncements = findViewById(R.id.rv_home_announcements);
        tvNoAnnouncements   = findViewById(R.id.tv_no_announcements);

        viewBookContainer = findViewById(R.id.view_book_container);
        rvBookServices    = findViewById(R.id.rv_book_services);
        tvBookNoServices  = findViewById(R.id.tv_book_no_services);

        bookAdapter = new BookAdapter(this);
        if (rvBookServices != null) {
            rvBookServices.setLayoutManager(new LinearLayoutManager(this));
            rvBookServices.setNestedScrollingEnabled(false);
            rvBookServices.setAdapter(bookAdapter);
        }

        TextView tvSeeAll = findViewById(R.id.tv_see_all);
        if (tvSeeAll != null)
            tvSeeAll.setOnClickListener(v ->
                startActivity(new Intent(this, AnnouncementsActivity.class)));

        LinearLayout btnNotification = findViewById(R.id.btn_notification);
        if (btnNotification != null)
            btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, AnnouncementsActivity.class)));
    }

    // ─── View switching ──────────────────────────────────────────────────────

    private void showHomeView() {
        if (nestedScrollView != null) nestedScrollView.setVisibility(View.VISIBLE);
        if (viewBookContainer != null) viewBookContainer.setVisibility(View.GONE);
    }

    private void showBookView() {
        if (nestedScrollView != null) nestedScrollView.setVisibility(View.GONE);
        if (viewBookContainer != null) viewBookContainer.setVisibility(View.VISIBLE);
    }

    // ─── Greeting ────────────────────────────────────────────────────────────

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = getString(R.string.good_morning);
        else if (hour < 18) greeting = getString(R.string.good_afternoon);
        else                greeting = getString(R.string.good_evening);
        tvGreeting.setText(greeting);
    }

    // ─── User data ───────────────────────────────────────────────────────────

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) tvUserName.setText(name);
                    else if (user.getDisplayName() != null) tvUserName.setText(user.getDisplayName());

                    barangayId = doc.getString("barangayId");
                    if (barangayId != null) {
                        setupServicesGrid();
                        setupAnnouncements();
                    }
                }
            });
    }

    // ─── Active queue real-time listener ─────────────────────────────────────

    private void startQueueListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { showNoQueue(); return; }

        if (queueListener != null) queueListener.remove();

        queueListener = db.collection("bookings")
            .whereEqualTo("userId", user.getUid())
            .whereIn("status", Arrays.asList("waiting", "serving"))
            .addSnapshotListener((snaps, err) -> {
                if (err != null || snaps == null || snaps.isEmpty()) {
                    showNoQueue();
                    activeBookingId = null;
                    return;
                }

                // Find the nearest upcoming booking (earliest future date + time)
                QueryDocumentSnapshot nearest = null;
                String nearestDate = null;
                int nearestStartMin = Integer.MAX_VALUE;

                for (com.google.firebase.firestore.DocumentSnapshot raw : snaps.getDocuments()) {
                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) raw;
                    String bookingDate = doc.getString("date");
                    if (isPastDate(bookingDate)) continue;

                    int startMin = timeRangeStartMinutes(doc.getString("timeRange"));
                    boolean earlier = nearestDate == null
                        || bookingDate.compareTo(nearestDate) < 0
                        || (bookingDate.equals(nearestDate) && startMin < nearestStartMin);

                    if (earlier) {
                        nearest        = doc;
                        nearestDate    = bookingDate;
                        nearestStartMin = startMin;
                    }
                }

                if (nearest == null) {
                    showNoQueue();
                    activeBookingId = null;
                    return;
                }

                activeBookingId = nearest.getId();
                showActiveQueue(
                    nearest.getString("queueNumber"),
                    nearest.getString("serviceName"),
                    nearest.getString("status"),
                    nearest.getLong("aheadCount"),
                    nearest.getString("date"),
                    nearest.getString("timeRange"));
            });
    }

    private int timeRangeStartMinutes(String timeRange) {
        if (timeRange == null) return 0;
        try {
            String start = timeRange.split("\\s*[–-]\\s*")[0].trim();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                start.contains(":") ? "h:mm a" : "h a", java.util.Locale.getDefault());
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(sdf.parse(start));
            return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isPastDate(String dateStr) {
        if (dateStr == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
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

    private boolean isSlotActive(String dateStr, String timeRange) {
        if (dateStr == null || timeRange == null) return false;
        try {
            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            Date slotDate = dateSdf.parse(dateStr);
            Calendar todayCal = Calendar.getInstance();
            Calendar slotCal  = Calendar.getInstance();
            if (slotDate != null) slotCal.setTime(slotDate);
            if (slotCal.get(Calendar.YEAR)        != todayCal.get(Calendar.YEAR) ||
                slotCal.get(Calendar.DAY_OF_YEAR) != todayCal.get(Calendar.DAY_OF_YEAR)) {
                return false;
            }
        } catch (ParseException e) {
            return false;
        }
        int startMin = timeRangeStartMinutes(timeRange);
        Calendar now = Calendar.getInstance();
        int nowMin   = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return nowMin >= startMin;
    }

    private String buildUpcomingLabel(String dateStr, String timeRange) {
        String startTime = "";
        if (timeRange != null) {
            startTime = timeRange.split("\\s*[–-]\\s*")[0].trim();
        }
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(new Date());
        if (dateStr != null && dateStr.equals(todayStr)) {
            return "Starts today at " + startTime;
        }
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM d", java.util.Locale.getDefault());
            Date d = in.parse(dateStr);
            String datePart = (d != null) ? out.format(d) : dateStr;
            return "Starts " + datePart + (startTime.isEmpty() ? "" : " at " + startTime);
        } catch (Exception e) {
            return "Upcoming queue";
        }
    }

    private void showNoQueue() {
        layoutNoQueue.setVisibility(View.VISIBLE);
        layoutHasQueue.setVisibility(View.GONE);
        cardActiveQueue.setOnClickListener(null);
    }

    private void showActiveQueue(String queueNum, String svcName, String status,
                                  Long ahead, String date, String timeRange) {
        layoutNoQueue.setVisibility(View.GONE);
        layoutHasQueue.setVisibility(View.VISIBLE);

        if (queueNum != null) tvQueueNumber.setText(queueNum);
        if (svcName != null)  tvQueueService.setText(svcName);

        boolean isServing   = "serving".equals(status);
        boolean slotActive  = isSlotActive(date, timeRange);

        if (isServing) {
            tvQueueStatusBadge.setText("NOW SERVING");
            tvQueueStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_success));
            tvQueueStatusBadge.setBackgroundResource(R.drawable.bg_badge_active);
            tvAheadInfo.setText("You are being called!");
            layoutServingMsg.setVisibility(View.VISIBLE);
        } else {
            tvQueueStatusBadge.setText("WAITING");
            tvQueueStatusBadge.setTextColor(0xFF92400E);
            tvQueueStatusBadge.setBackgroundResource(R.drawable.bg_badge_waiting);
            layoutServingMsg.setVisibility(View.GONE);

            if (!slotActive) {
                // Queue hasn't started yet — show when it will start
                tvAheadInfo.setText(buildUpcomingLabel(date, timeRange));
            } else if (ahead != null) {
                long waitMins = ahead * 5;
                tvAheadInfo.setText(ahead == 0
                    ? "You're next!"
                    : ahead + " ahead · ~" + waitMins + " min");
            } else {
                tvAheadInfo.setText("");
            }
        }

        cardActiveQueue.setOnClickListener(v -> {
            if (activeBookingId != null) {
                leavingToSubActivity = true;
                startActivity(new Intent(this, QueueTicketActivity.class)
                    .putExtra("bookingId", activeBookingId));
            }
        });
    }

    // ─── Dynamic Services Grid ────────────────────────────────────────────────

    private void setupServicesGrid() {
        serviceAdapter = new ServiceAdapter(this);

        rvServices.setLayoutManager(new LinearLayoutManager(this));
        rvServices.setNestedScrollingEnabled(false);
        rvServices.setAdapter(serviceAdapter);

        if (tvNoServices != null) {
            tvNoServices.setText("Loading services…");
            tvNoServices.setVisibility(View.VISIBLE);
        }

        servicesListener = db.collection("services")
            .whereEqualTo("barangayId", barangayId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) {
                    rvServices.setVisibility(View.GONE);
                    if (tvNoServices != null) {
                        tvNoServices.setText("No available services yet.");
                        tvNoServices.setVisibility(View.VISIBLE);
                    }
                    updateBookEmptyState(new ArrayList<>());
                    return;
                }

                List<Service> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Service s = doc.toObject(Service.class);
                    s.setId(doc.getId());
                    list.add(s);
                }
                Collections.sort(list, (a, b) ->
                    a.getName() != null ? a.getName().compareToIgnoreCase(
                        b.getName() != null ? b.getName() : "") : 1);

                serviceAdapter.setServices(list);
                bookAdapter.setServices(list);

                if (list.isEmpty()) {
                    rvServices.setVisibility(View.GONE);
                    if (tvNoServices != null) {
                        tvNoServices.setText("No available services yet.");
                        tvNoServices.setVisibility(View.VISIBLE);
                    }
                } else {
                    rvServices.setVisibility(View.VISIBLE);
                    if (tvNoServices != null) tvNoServices.setVisibility(View.GONE);
                }

                updateBookEmptyState(list);
            });
    }

    private void updateBookEmptyState(List<Service> list) {
        if (rvBookServices == null || tvBookNoServices == null) return;
        if (list.isEmpty()) {
            rvBookServices.setVisibility(View.GONE);
            tvBookNoServices.setText("No available services yet.");
            tvBookNoServices.setVisibility(View.VISIBLE);
        } else {
            rvBookServices.setVisibility(View.VISIBLE);
            tvBookNoServices.setVisibility(View.GONE);
        }
    }

    // ─── Announcements ───────────────────────────────────────────────────────

    private void setupAnnouncements() {
        announcementAdapter = new AnnouncementAdapter(this);
        rvHomeAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        rvHomeAnnouncements.setNestedScrollingEnabled(false);
        rvHomeAnnouncements.setAdapter(announcementAdapter);

        announcementsListener = db.collection("announcements")
            .whereEqualTo("barangayId", barangayId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) {
                    rvHomeAnnouncements.setVisibility(View.GONE);
                    if (tvNoAnnouncements != null) {
                        tvNoAnnouncements.setText("Nothing to announce for now. Check back later!");
                        tvNoAnnouncements.setVisibility(View.VISIBLE);
                    }
                    return;
                }

                List<Announcement> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Announcement a = doc.toObject(Announcement.class);
                    a.setId(doc.getId());
                    list.add(a);
                }
                list.sort((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                if (list.size() > 1) list = list.subList(0, 1);

                announcementAdapter.setItems(list);

                if (list.isEmpty()) {
                    rvHomeAnnouncements.setVisibility(View.GONE);
                    if (tvNoAnnouncements != null) {
                        tvNoAnnouncements.setText("Nothing to announce for now. Check back later!");
                        tvNoAnnouncements.setVisibility(View.VISIBLE);
                    }
                } else {
                    rvHomeAnnouncements.setVisibility(View.VISIBLE);
                    if (tvNoAnnouncements != null) tvNoAnnouncements.setVisibility(View.GONE);
                }
            });
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            leavingToSubActivity = false;
            showHomeView();
            setActiveNav(navHome);
        });

        navBook.setOnClickListener(v -> {
            leavingToSubActivity = false;
            showBookView();
            setActiveNav(navBook);
        });

        navQueue.setOnClickListener(v -> {
            leavingToSubActivity = true;
            setActiveNav(navQueue);
            startActivity(new Intent(this, MyQueuesActivity.class));
        });

        navProfile.setOnClickListener(v -> {
            leavingToSubActivity = true;
            setActiveNav(navProfile);
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    private void openActiveQueueTicket() {
        // If we already have an active booking cached, go directly
        if (activeBookingId != null) {
            startActivity(new Intent(this, QueueTicketActivity.class)
                .putExtra("bookingId", activeBookingId));
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // Show empty ticket for unauthenticated state
            startActivity(new Intent(this, QueueTicketActivity.class));
            return;
        }

        // First try to find an active (waiting/serving) booking
        db.collection("bookings")
            .whereEqualTo("userId", user.getUid())
            .whereIn("status", Arrays.asList("waiting", "serving"))
            .limit(1)
            .get()
            .addOnSuccessListener(snap -> {
                if (!snap.isEmpty()) {
                    startActivity(new Intent(this, QueueTicketActivity.class)
                        .putExtra("bookingId", snap.getDocuments().get(0).getId()));
                } else {
                    // No active booking — look for most recent booking (e.g. skipped/done) today
                    findMostRecentBookingAndOpen(user.getUid());
                }
            })
            .addOnFailureListener(e -> {
                // On error, still open ticket (will show empty state)
                startActivity(new Intent(this, QueueTicketActivity.class));
            });
    }

    private void findMostRecentBookingAndOpen(String userId) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(new java.util.Date());

        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", today)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener(snap -> {
                if (!snap.isEmpty()) {
                    startActivity(new Intent(this, QueueTicketActivity.class)
                        .putExtra("bookingId", snap.getDocuments().get(0).getId()));
                } else {
                    // No booking at all — open empty state ticket
                    startActivity(new Intent(this, QueueTicketActivity.class));
                }
            })
            .addOnFailureListener(e ->
                startActivity(new Intent(this, QueueTicketActivity.class)));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        leavingToSubActivity = false;
        if (intent.getBooleanExtra("show_book", false)) {
            showBookView();
            setActiveNav(navBook);
        } else {
            showHomeView();
            setActiveNav(navHome);
        }
    }

    private void setActiveNav(LinearLayout activeNav) {
        LinearLayout[] allNavs = {navHome, navBook, navQueue, navProfile};
        for (LinearLayout nav : allNavs) {
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
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        startQueueListener();
        if (leavingToSubActivity) {
            leavingToSubActivity = false;
            showHomeView();
            setActiveNav(navHome);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (queueListener != null) {
            queueListener.remove();
            queueListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (announcementsListener != null) announcementsListener.remove();
        if (servicesListener != null) servicesListener.remove();
    }

    // ─── Book tab adapter ────────────────────────────────────────────────────

    static class BookAdapter extends RecyclerView.Adapter<BookAdapter.VH> {

        private static final int[] BG_COLORS = {
            R.color.accent_blue_bg, R.color.accent_green_bg,
            R.color.accent_orange_bg, R.color.accent_purple_bg
        };
        private static final int[] ICON_COLORS = {
            R.color.accent_blue, R.color.accent_green,
            R.color.accent_orange, R.color.accent_purple
        };
        private static final int[] ICONS = {
            R.drawable.ic_services, R.drawable.ic_profile,
            R.drawable.ic_check, R.drawable.ic_dashboard
        };

        private final Context ctx;
        private final List<Service> list = new ArrayList<>();

        BookAdapter(Context ctx) { this.ctx = ctx; }

        void setServices(List<Service> s) {
            list.clear();
            if (s != null) list.addAll(s);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_service_book_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Service s = list.get(pos);
            int palette = pos % 4;

            h.tvName.setText(s.getName());

            String desc = s.getDescription();
            h.tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "");
            h.tvDesc.setVisibility(desc != null && !desc.isEmpty() ? View.VISIBLE : View.GONE);

            h.tvTime.setText(s.getFormattedTime());

            List<String> reqs = s.getRequirements();
            int reqCount = reqs != null ? reqs.size() : 0;
            h.tvReqs.setText(reqCount + (reqCount == 1 ? " requirement" : " requirements"));

            boolean avail = s.isAvailable();
            h.tvAvail.setText(avail ? "Available" : "Unavailable");
            h.tvAvail.setTextColor(ContextCompat.getColor(ctx,
                avail ? R.color.status_success : R.color.status_danger));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(ctx, BG_COLORS[palette]));
            bg.setCornerRadius(ctx.getResources().getDisplayMetrics().density * 10);
            h.iconBg.setBackground(bg);
            h.icon.setImageResource(ICONS[palette]);
            h.icon.setColorFilter(ContextCompat.getColor(ctx, ICON_COLORS[palette]));

            h.itemView.setAlpha(avail ? 1f : 0.5f);
            h.itemView.setOnClickListener(v -> {
                if (!avail) return;
                ctx.startActivity(new Intent(ctx, ServiceDetailActivity.class)
                    .putExtra("serviceId", s.getId())
                    .putExtra("serviceName", s.getName()));
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvTime, tvReqs, tvAvail;
            LinearLayout iconBg;
            ImageView icon;

            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_service_name);
                tvDesc = v.findViewById(R.id.tv_service_description);
                tvTime = v.findViewById(R.id.tv_service_time);
                tvReqs = v.findViewById(R.id.tv_service_requirements);
                tvAvail = v.findViewById(R.id.tv_availability);
                iconBg = v.findViewById(R.id.iv_service_icon_bg);
                icon   = v.findViewById(R.id.iv_service_icon);
            }
        }
    }
}