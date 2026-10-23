package com.example.smartpantry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Custom RecyclerView adapter: turns each PantryItem into a card row. */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(PantryItem item);
        void onDelete(PantryItem item);
    }

    private List<PantryItem> items = new ArrayList<>();
    private final Listener listener;

    public PantryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<PantryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PantryItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvQty.setText(RecipeMatcher.formatQty(item.getQuantity()) + " " + item.getUnit());

        String expiry = item.getExpiryDate();
        if (expiry == null || expiry.isEmpty()) {
            holder.tvExpiry.setText("No expiry date");
        } else {
            String text = "Expires: " + expiry;
            boolean alertsOn = holder.itemView.getContext()
                    .getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
                    .getBoolean(SettingsActivity.KEY_EXPIRY, true);
            if (alertsOn && isExpiringSoon(expiry)) {
                text += "  (EXPIRING SOON)";
            }
            holder.tvExpiry.setText(text);
        }

        holder.itemView.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    /** True if the date is within 3 days from now, or already past. */
    private boolean isExpiringSoon(String expiry) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(expiry);
            if (d == null) return false;
            long days = (d.getTime() - System.currentTimeMillis()) / (24L * 60 * 60 * 1000);
            return days <= 3;
        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQty, tvExpiry;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}