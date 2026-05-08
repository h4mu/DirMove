package com.dirmove;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.ViewHolder> {

    private final List<DirectoryModel> directories;

    public DirectoryAdapter(List<DirectoryModel> directories) {
        this.directories = directories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_directory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DirectoryModel directory = directories.get(position);
        holder.tvDirName.setText(directory.getName());
        holder.tvDirPath.setText(directory.getPath());

        // Unbind previous listener to avoid triggering it when calling setChecked
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(directory.isSelected());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            directory.setSelected(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return directories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDirName;
        TextView tvDirPath;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDirName = itemView.findViewById(R.id.tvDirName);
            tvDirPath = itemView.findViewById(R.id.tvDirPath);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
