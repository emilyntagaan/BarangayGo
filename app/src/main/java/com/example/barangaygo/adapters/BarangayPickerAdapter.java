package com.example.barangaygo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;

import com.example.barangaygo.R;
import com.example.barangaygo.models.Barangay;

import java.util.ArrayList;
import java.util.List;

public class BarangayPickerAdapter extends RecyclerView.Adapter<BarangayPickerAdapter.ViewHolder> {

    public interface OnBarangaySelectedListener {
        void onSelected(Barangay barangay);
    }

    private final Context context;
    private final List<Barangay> allBarangays;
    private List<Barangay> filteredList;
    private int selectedPosition = -1;
    private final OnBarangaySelectedListener listener;

    public BarangayPickerAdapter(Context context, List<Barangay> barangays, OnBarangaySelectedListener listener) {
        this.context = context;
        this.allBarangays = new ArrayList<>(barangays);
        this.filteredList = new ArrayList<>(barangays);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_barangay_picker_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Barangay barangay = filteredList.get(position);
        holder.tvName.setText(barangay.getName());
        holder.tvLocation.setText(barangay.getDisplayLocation());

        if (barangay.getCode() != null && !barangay.getCode().isEmpty()) {
            holder.tvCode.setVisibility(View.VISIBLE);
            holder.tvCode.setText(barangay.getCode());
        } else {
            holder.tvCode.setVisibility(View.GONE);
        }

        String logoUrl = barangay.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            holder.layoutLogoFallback.setVisibility(View.GONE);
            holder.ivLogo.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(logoUrl)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(24)))
                    .placeholder(R.drawable.bg_step_active)
                    .into(holder.ivLogo);
        } else {
            holder.ivLogo.setVisibility(View.GONE);
            holder.layoutLogoFallback.setVisibility(View.VISIBLE);
        }

        boolean isSelected = (position == selectedPosition);
        holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        int strokeColor = context.getResources().getColor(
                isSelected ? R.color.blue_600 : R.color.gray_200, context.getTheme());
        int bgColor = context.getResources().getColor(
                isSelected ? R.color.blue_50 : R.color.white, context.getTheme());
        holder.card.setStrokeColor(android.content.res.ColorStateList.valueOf(strokeColor));
        holder.card.setCardBackgroundColor(bgColor);

        int adapterPos = position;
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = adapterPos;
            if (prev != -1) notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onSelected(filteredList.get(selectedPosition));
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        selectedPosition = -1;
        filteredList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(allBarangays);
        } else {
            String lower = query.toLowerCase().trim();
            for (Barangay b : allBarangays) {
                if ((b.getName() != null && b.getName().toLowerCase().contains(lower))
                        || (b.getMunicipality() != null && b.getMunicipality().toLowerCase().contains(lower))
                        || (b.getProvince() != null && b.getProvince().toLowerCase().contains(lower))) {
                    filteredList.add(b);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(List<Barangay> newData) {
        allBarangays.clear();
        allBarangays.addAll(newData);
        filteredList = new ArrayList<>(allBarangays);
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName, tvLocation, tvCode;
        ImageView ivCheck, ivLogo;
        LinearLayout layoutLogoFallback;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_barangay_item);
            tvName = itemView.findViewById(R.id.tv_barangay_name);
            tvLocation = itemView.findViewById(R.id.tv_barangay_location);
            tvCode = itemView.findViewById(R.id.tv_barangay_code);
            ivCheck = itemView.findViewById(R.id.iv_check);
            ivLogo = itemView.findViewById(R.id.iv_barangay_logo);
            layoutLogoFallback = itemView.findViewById(R.id.layout_logo_fallback);
        }
    }
}