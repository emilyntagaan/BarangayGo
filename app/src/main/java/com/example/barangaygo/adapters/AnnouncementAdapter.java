package com.example.barangaygo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barangaygo.R;
import com.example.barangaygo.models.Announcement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    public interface OnDeleteListener { void onDelete(Announcement announcement); }
    public interface OnEditListener   { void onEdit(Announcement announcement); }

    private static final String INFO_BG      = "#DBEAFE";
    private static final String INFO_ICON    = "#2563EB";
    private static final String INFO_BADGE   = "#1D4ED8";

    private static final String WARN_BG      = "#FFEDD5";
    private static final String WARN_ICON    = "#EA580C";
    private static final String WARN_BADGE   = "#C2410C";

    private static final String URGENT_BG    = "#FEE2E2";
    private static final String URGENT_ICON  = "#DC2626";
    private static final String URGENT_BADGE = "#B91C1C";

    private final Context context;
    private final List<Announcement> items = new ArrayList<>();
    private final SimpleDateFormat sdf =
        new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    private boolean adminMode = false;
    private OnDeleteListener deleteListener;
    private OnEditListener   editListener;

    public AnnouncementAdapter(Context context) {
        this.context = context;
    }

    public void setAdminMode(boolean admin) {
        this.adminMode = admin;
        notifyDataSetChanged();
    }

    public void setOnDeleteListener(OnDeleteListener l) { this.deleteListener = l; }
    public void setOnEditListener(OnEditListener l)     { this.editListener   = l; }

    public void setItems(List<Announcement> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_announcement_feed, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement a = items.get(position);

        holder.tvTitle.setText(a.getTitle() != null ? a.getTitle() : "");
        holder.tvBody.setText(a.getBody() != null ? a.getBody() : "");
        holder.tvTime.setText(a.getCreatedAt() != null
            ? sdf.format(a.getCreatedAt().toDate()) : "");

        String type = a.getType() != null ? a.getType() : "info";
        applyType(holder, type);

        if (holder.layoutAdminActions != null) {
            holder.layoutAdminActions.setVisibility(adminMode ? View.VISIBLE : View.GONE);
        }

        if (holder.btnEdit != null) {
            holder.btnEdit.setOnClickListener(v -> {
                if (editListener != null) editListener.onEdit(a);
            });
        }

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(a);
            });
        }
    }

    private void applyType(ViewHolder holder, String type) {
        String circleBgHex, iconHex, badgeBgHex, badgeText;
        int iconRes;

        switch (type) {
            case "warning":
                circleBgHex = WARN_BG;
                iconHex     = WARN_ICON;
                badgeBgHex  = WARN_BG;
                badgeText   = "WARNING";
                iconRes     = R.drawable.ic_info;
                break;
            case "urgent":
                circleBgHex = URGENT_BG;
                iconHex     = URGENT_ICON;
                badgeBgHex  = URGENT_BG;
                badgeText   = "URGENT";
                iconRes     = R.drawable.ic_bell;
                break;
            default: // "info"
                circleBgHex = INFO_BG;
                iconHex     = INFO_ICON;
                badgeBgHex  = INFO_BG;
                badgeText   = "INFO";
                iconRes     = R.drawable.ic_bell;
                break;
        }

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor(circleBgHex));
        holder.iconBg.setBackground(circle);

        holder.icon.setImageResource(iconRes);
        holder.icon.setColorFilter(Color.parseColor(iconHex));

        holder.tvType.setText(badgeText);
        holder.tvType.setTextColor(Color.parseColor(badgeBgHex.equals(INFO_BG) ? INFO_BADGE
            : badgeBgHex.equals(WARN_BG) ? WARN_BADGE : URGENT_BADGE));

        GradientDrawable badge = new GradientDrawable();
        badge.setColor(Color.parseColor(badgeBgHex));
        badge.setCornerRadius(dpToPx(6));
        holder.tvType.setBackground(badge);
    }

    @Override
    public int getItemCount() { return items.size(); }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout iconBg, layoutAdminActions;
        ImageView icon;
        TextView tvType, tvTitle, tvBody, tvTime;
        View btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBg             = itemView.findViewById(R.id.ll_icon_bg);
            icon               = itemView.findViewById(R.id.iv_announce_icon);
            tvType             = itemView.findViewById(R.id.tv_announce_type);
            tvTitle            = itemView.findViewById(R.id.tv_announce_title);
            tvBody             = itemView.findViewById(R.id.tv_announce_body);
            tvTime             = itemView.findViewById(R.id.tv_announce_time);
            layoutAdminActions = itemView.findViewById(R.id.layout_admin_actions);
            btnEdit            = itemView.findViewById(R.id.btn_edit_announce);
            btnDelete          = itemView.findViewById(R.id.btn_delete_announce);
        }
    }
}