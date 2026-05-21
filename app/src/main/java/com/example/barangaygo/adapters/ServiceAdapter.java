package com.example.barangaygo.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.example.barangaygo.activities.ServiceDetailActivity;
import com.example.barangaygo.models.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private static final int TYPE_FEATURED = 0;
    private static final int TYPE_NORMAL   = 1;

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
    private static final int[] BG_COLORS = {
        R.color.accent_blue_bg,
        R.color.accent_green_bg,
        R.color.accent_orange_bg,
        R.color.accent_purple_bg
    };
    private static final int[] ICON_COLORS = {
        R.color.accent_blue,
        R.color.accent_green,
        R.color.accent_orange,
        R.color.accent_purple
    };

    private final Context context;
    private final List<Service> services = new ArrayList<>();
    private int lastAnimatedPosition = -1;

    public ServiceAdapter(Context context) {
        this.context = context;
    }

    public void setServices(List<Service> newServices) {
        services.clear();
        if (newServices != null) services.addAll(newServices);
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_FEATURED : TYPE_NORMAL;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_FEATURED
            ? R.layout.item_service_card_featured
            : R.layout.item_service_card;
        View v = LayoutInflater.from(context).inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    private int iconIndexForKey(String key) {
        if (key != null) {
            for (int i = 0; i < ICON_KEYS.length; i++) {
                if (ICON_KEYS[i].equals(key)) return i;
            }
        }
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Service service = services.get(position);
        int iconIdx = iconIndexForKey(service.getIconKey());
        int palette = iconIdx % 4;

        if (h.tvName != null) h.tvName.setText(service.getName());

        if (h.tvWait != null) {
            int mins = service.getEstimatedMinutes();
            h.tvWait.setText(mins > 0 ? "~" + mins + " min wait" : "Open");
        }

        if (h.iconBg != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(context, BG_COLORS[palette]));
            bg.setCornerRadius(dpToPx(12));
            h.iconBg.setBackground(bg);
        }

        if (h.icon != null) {
            h.icon.setImageResource(ICON_DRAWABLES[iconIdx]);
            h.icon.setColorFilter(ContextCompat.getColor(context, ICON_COLORS[palette]));
        }

        h.itemView.setAlpha(service.isAvailable() ? 1f : 0.45f);

        h.itemView.setOnClickListener(v -> {
            if (!service.isAvailable()) return;
            Intent intent = new Intent(context, ServiceDetailActivity.class);
            intent.putExtra("serviceId", service.getId());
            intent.putExtra("serviceName", service.getName());
            context.startActivity(intent);
        });

        // Staggered entry animation
        if (position > lastAnimatedPosition) {
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(24f);
            h.itemView.animate()
                .alpha(service.isAvailable() ? 1f : 0.45f)
                .translationY(0f)
                .setDuration(250)
                .setStartDelay(position * 50L)
                .start();
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvWait;
        LinearLayout iconBg;
        ImageView icon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_service_name);
            tvWait = itemView.findViewById(R.id.tv_service_wait);
            iconBg = itemView.findViewById(R.id.iv_service_icon_bg);
            icon   = itemView.findViewById(R.id.iv_service_icon);
        }
    }
}