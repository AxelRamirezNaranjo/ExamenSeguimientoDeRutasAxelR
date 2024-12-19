package com.example.seguimientoderutas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RutaAdapter extends RecyclerView.Adapter<RutaAdapter.RutaViewHolder> {

    private List<String> rutas;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(String rutaId);
    }

    public RutaAdapter(List<String> rutas, OnItemClickListener onItemClickListener) {
        this.rutas = rutas;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public RutaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new RutaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RutaViewHolder holder, int position) {
        String ruta = rutas.get(position);
        holder.textView.setText(ruta);
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(ruta));
    }

    @Override
    public int getItemCount() {
        return rutas.size();
    }

    public static class RutaViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public RutaViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
