package com.example.barangaygo.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.models.Slot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder> {

    public interface OnEditListener  { void onEdit(Slot slot); }
    public interface OnDeleteListener { void onDelete(Slot slot); }

    private final Context ctx;
    private final List<Slot> slots = new ArrayList<>();
    private OnEditListener   editListener;
    private OnDeleteListener deleteListener;

    public SlotAdapter(Context ctx) { this.ctx = ctx; }

    public void setOnEditListener(OnEditListener l)   { editListener   = l; }
    public void setOnDeleteListener(OnDeleteListener l) { deleteListener = l; }

    public void setSlots(List<Slot> newSlots) {
        slots.clear();
        slots.addAll(newSlots);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_slot_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Slot s = slots.get(pos);

        h.tvService.setText(s.getServiceName() != null ? s.getServiceName() : "");
        h.tvDate.setText(s.getDate() != null ? s.getDate() : "");
        h.tvTime.setText(s.getTimeRange() != null ? s.getTimeRange() : "");
        h.tvCapacity.setText(s.getCurrentCount() + " / " + s.getMaxCapacity() + " booked");

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String slotDate = s.getDate() != null ? s.getDate() : "";

        // A slot is expired if its date is in the past, OR if it's today but its
        // end time has already passed (e.g. an 8–10 AM slot shown at 10 PM).
        boolean isExpired = false;
        if (!slotDate.isEmpty()) {
            int dateCompare = slotDate.compareTo(today);
            if (dateCompare < 0) {
                isExpired = true;          // past day
            } else if (dateCompare == 0) {
                // Same day — check whether the end time has passed
                int[] range = parseTimeRange(s.getTimeRange());
                if (range != null) {
                    Calendar now = Calendar.getInstance();
                    int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                    isExpired = nowMinutes >= range[1]; // past the end time
                }
            }
        }

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(dpToPx(6));

        if (isExpired) {
            h.tvStatus.setText("DONE");
            badge.setColor(ContextCompat.getColor(ctx, R.color.gray_200));
            h.tvStatus.setBackground(badge);
            h.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.gray_600));
            h.itemView.setAlpha(0.5f);
        } else {
            boolean isOpen = "open".equals(s.getStatus());
            h.tvStatus.setText(isOpen ? "OPEN" : "CLOSED");
            badge.setColor(ContextCompat.getColor(ctx,
                isOpen ? R.color.status_success_bg : R.color.status_danger_bg));
            h.tvStatus.setBackground(badge);
            h.tvStatus.setTextColor(ContextCompat.getColor(ctx,
                isOpen ? R.color.status_success : R.color.status_danger));
            h.itemView.setAlpha(1f);
        }

        h.btnEdit.setOnClickListener(v -> { if (editListener != null) editListener.onEdit(s); });
        h.btnDelete.setOnClickListener(v -> { if (deleteListener != null) deleteListener.onDelete(s); });
    }

    @Override public int getItemCount() { return slots.size(); }

    private int dpToPx(int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    /** Returns {startMinutes, endMinutes} or null if the range cannot be parsed. */
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
        SimpleDateFormat sdf = new SimpleDateFormat(
                t.contains(":") ? "h:mm a" : "h a", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        c.setTime(sdf.parse(t));
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvService, tvDate, tvTime, tvCapacity, tvStatus;
        LinearLayout btnEdit, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            tvService  = v.findViewById(R.id.tv_slot_service);
            tvDate     = v.findViewById(R.id.tv_slot_date);
            tvTime     = v.findViewById(R.id.tv_slot_time);
            tvCapacity = v.findViewById(R.id.tv_slot_capacity);
            tvStatus   = v.findViewById(R.id.tv_slot_status);
            btnEdit    = v.findViewById(R.id.btn_slot_edit);
            btnDelete  = v.findViewById(R.id.btn_slot_delete);
        }
    }
}