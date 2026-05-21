package com.example.barangaygo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.models.Booking;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

    public interface OnCallClickListener {
        void onCallClicked(Booking booking);
    }

    private final Context context;
    private final List<Booking> bookings = new ArrayList<>();
    private OnCallClickListener listener;

    public QueueAdapter(Context context) {
        this.context = context;
    }

    public void setOnCallClickListener(OnCallClickListener listener) {
        this.listener = listener;
    }

    public void setBookings(List<Booking> newBookings) {
        bookings.clear();
        bookings.addAll(newBookings);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_queue_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        // Show only the numeric part of the queue number (e.g. "A-042" → "42")
        String qNum = booking.getQueueNumber();
        if (qNum != null && qNum.contains("-")) {
            qNum = qNum.substring(qNum.lastIndexOf('-') + 1);
        }
        holder.tvNum.setText(qNum != null ? qNum : "-");
        holder.tvName.setText(booking.getResidentName() != null ? booking.getResidentName() : "");
        holder.tvService.setText(booking.getServiceName() != null ? booking.getServiceName() : "");

        boolean isDone = "done".equals(booking.getStatus());

        if (isDone) {
            holder.numBg.setBackgroundResource(R.drawable.bg_announce_icon_success);
            holder.tvNum.setTextColor(ContextCompat.getColor(context, R.color.status_success));
            holder.btnAction.setText(context.getString(R.string.done));
            holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.status_success));
            holder.btnAction.setEnabled(false);
        } else {
            holder.numBg.setBackgroundResource(R.drawable.bg_queue_num);
            holder.tvNum.setTextColor(ContextCompat.getColor(context, R.color.blue_600));
            holder.btnAction.setText(context.getString(R.string.call));
            holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.blue_600));
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onCallClicked(booking);
            });
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout numBg;
        TextView tvNum, tvName, tvService;
        MaterialButton btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            numBg = itemView.findViewById(R.id.queue_num_bg);
            tvNum = itemView.findViewById(R.id.tv_queue_num);
            tvName = itemView.findViewById(R.id.tv_resident_name);
            tvService = itemView.findViewById(R.id.tv_service_type);
            btnAction = itemView.findViewById(R.id.btn_queue_action);
        }
    }
}