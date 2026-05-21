package com.example.barangaygo.activities;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyQueuesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration listener;

    private RecyclerView rvQueues;
    private LinearLayout layoutEmpty;
    private QueueListAdapter adapter;

    private LinearLayout navHome, navBook, navQueue, navProfile;
    private ImageView navHomeIcon, navBookIcon, navQueueIcon, navProfileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_queues);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvQueues    = findViewById(R.id.rv_my_queues);
        layoutEmpty = findViewById(R.id.layout_empty);

        LinearLayout btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        navHome    = findViewById(R.id.nav_home);
        navBook    = findViewById(R.id.nav_book);
        navQueue   = findViewById(R.id.nav_queue);
        navProfile = findViewById(R.id.nav_profile);

        navHomeIcon    = findViewById(R.id.nav_home_icon);
        navBookIcon    = findViewById(R.id.nav_book_icon);
        navQueueIcon   = findViewById(R.id.nav_queue_icon);
        navProfileIcon = findViewById(R.id.nav_profile_icon);

        setupNav();
        setActiveNav(navQueue);

        adapter = new QueueListAdapter(item ->
            startActivity(new Intent(this, QueueTicketActivity.class)
                .putExtra("bookingId", item.id)));

        rvQueues.setLayoutManager(new LinearLayoutManager(this));
        rvQueues.setAdapter(adapter);

        startListener();
    }

    private void startListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        listener = db.collection("bookings")
            .whereEqualTo("userId", user.getUid())
            .whereIn("status", Arrays.asList("waiting", "serving"))
            .addSnapshotListener((snaps, err) -> {
                if (err != null || snaps == null) return;

                List<BookingItem> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snaps) {
                    String dateStr = doc.getString("date");
                    if (isPastDate(dateStr)) continue;

                    BookingItem item  = new BookingItem();
                    item.id           = doc.getId();
                    item.queueNumber  = doc.getString("queueNumber");
                    item.serviceName  = doc.getString("serviceName");
                    item.date         = dateStr;
                    item.timeRange    = doc.getString("timeRange");
                    item.status       = doc.getString("status");
                    Long ahead        = doc.getLong("aheadCount");
                    item.aheadCount   = ahead != null ? ahead : 0;
                    list.add(item);
                }

                Collections.sort(list, (a, b) -> {
                    int dc = compareDates(a.date, b.date);
                    return dc != 0 ? dc : compareTimeRangeStarts(a.timeRange, b.timeRange);
                });

                adapter.setItems(list);
                boolean empty = list.isEmpty();
                layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvQueues.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
    }

    // ─── Sorting helpers ─────────────────────────────────────────────────────

    private int compareDates(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    private int compareTimeRangeStarts(String a, String b) {
        return Integer.compare(timeRangeStartMinutes(a), timeRangeStartMinutes(b));
    }

    private int timeRangeStartMinutes(String timeRange) {
        if (timeRange == null) return 0;
        try {
            String start = timeRange.split("\\s*[–-]\\s*")[0].trim();
            SimpleDateFormat sdf = new SimpleDateFormat(
                start.contains(":") ? "h:mm a" : "h a", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(start));
            return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isPastDate(String dateStr) {
        if (dateStr == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(dateStr);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            return d != null && d.before(today.getTime());
        } catch (ParseException e) {
            return false;
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    private void setupNav() {
        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        if (navBook != null) navBook.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                .putExtra("show_book", true)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        if (navQueue   != null) navQueue.setOnClickListener(v -> setActiveNav(navQueue));
        if (navProfile != null) navProfile.setOnClickListener(v ->
            startActivity(new Intent(this, ProfileActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
    }

    private void setActiveNav(LinearLayout active) {
        LinearLayout[] navs  = {navHome, navBook, navQueue, navProfile};
        ImageView[]    icons = {navHomeIcon, navBookIcon, navQueueIcon, navProfileIcon};
        for (int i = 0; i < navs.length; i++) {
            if (navs[i] == null) continue;
            boolean isActive = navs[i] == active;
            LinearLayout iconBg = (LinearLayout) navs[i].getChildAt(0);
            TextView     label  = (TextView)     navs[i].getChildAt(1);
            if (iconBg != null) {
                if (isActive) iconBg.setBackgroundResource(R.drawable.bg_nav_active);
                else          iconBg.setBackground(null);
            }
            if (label != null) {
                label.setTextColor(ContextCompat.getColor(this,
                    isActive ? R.color.blue_600 : R.color.gray_400));
            }
            if (icons[i] != null) {
                icons[i].setColorFilter(ContextCompat.getColor(this,
                    isActive ? R.color.blue_600 : R.color.gray_400));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }

    // ─── Data model ──────────────────────────────────────────────────────────

    static class BookingItem {
        String id, queueNumber, serviceName, date, timeRange, status;
        long aheadCount;
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────

    interface OnItemClickListener { void onClick(BookingItem item); }

    static class QueueListAdapter extends RecyclerView.Adapter<QueueListAdapter.VH> {

        private final List<BookingItem> items = new ArrayList<>();
        private final OnItemClickListener listener;

        QueueListAdapter(OnItemClickListener listener) {
            this.listener = listener;
        }

        void setItems(List<BookingItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_queue_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            BookingItem item = items.get(position);

            h.tvQueueNum.setText(item.queueNumber != null ? item.queueNumber : "—");
            h.tvServiceName.setText(item.serviceName != null ? item.serviceName : "");

            // Format date + time
            String datePart = formatDate(item.date);
            String timePart = item.timeRange != null ? item.timeRange : "";
            h.tvDateTime.setText(datePart + (timePart.isEmpty() ? "" : " · " + timePart));

            // Ahead info
            boolean isServing = "serving".equals(item.status);
            if (isServing) {
                h.tvAheadInfo.setText("You are being called!");
            } else if (item.aheadCount == 0) {
                h.tvAheadInfo.setText("You're next!");
            } else {
                long wait = item.aheadCount * 5;
                h.tvAheadInfo.setText(item.aheadCount + " ahead · ~" + wait + " min");
            }

            // Status badge
            applyStatusBadge(h.tvStatusBadge, item.status, h.itemView);

            h.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        private String formatDate(String dateStr) {
            if (dateStr == null) return "";
            try {
                SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat out = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                Date d = in.parse(dateStr);
                return d != null ? out.format(d) : dateStr;
            } catch (ParseException e) {
                return dateStr;
            }
        }

        private void applyStatusBadge(TextView badge, String status, View root) {
            boolean isServing = "serving".equals(status);
            String  text      = isServing ? "SERVING" : "WAITING";
            int     textColor = isServing ? 0xFF065F46 : 0xFF92400E;
            int     bgColor   = isServing ? 0xFFD1FAE5 : 0xFFFEF3C7;

            badge.setText(text);
            badge.setTextColor(textColor);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(bgColor);
            bg.setCornerRadius(root.getResources().getDisplayMetrics().density * 6);
            badge.setBackground(bg);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvQueueNum, tvServiceName, tvDateTime, tvAheadInfo, tvStatusBadge;

            VH(@NonNull View v) {
                super(v);
                tvQueueNum    = v.findViewById(R.id.tv_queue_num);
                tvServiceName = v.findViewById(R.id.tv_service_name);
                tvDateTime    = v.findViewById(R.id.tv_date_time);
                tvAheadInfo   = v.findViewById(R.id.tv_ahead_info);
                tvStatusBadge = v.findViewById(R.id.tv_status_badge);
            }
        }
    }
}