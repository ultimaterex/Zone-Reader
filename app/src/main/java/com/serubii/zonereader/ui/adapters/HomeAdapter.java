package com.serubii.zonereader.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.serubii.zonereader.R;
import com.serubii.zonereader.room.entities.Document;
import com.serubii.zonereader.ui.interfaces.SimpleInterface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {

    private final LayoutInflater layoutInflater;
    private List<Document> items;
    private SimpleInterface anInterface;


    public HomeAdapter(Context context, ArrayList<Document> allItems, SimpleInterface anInterface) {
        this.items = allItems;
        this.anInterface = anInterface;
        this.layoutInflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.items_home, parent, false);
        return new ViewHolder(view, anInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Document model = items.get(position);

        String names = (model.getGiven_name() + " " + model.getSurname());


        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm", Locale.getDefault());
        String number = formatter.format(new Date(model.getScan_time()));


        holder.titlePrimary.setText(names);
        holder.titleSecondary.setText(number);

        // give random colour if image didn't save
        try {
            Uri uri = Uri.parse(model.getInitial_uri());
            holder.titleImage.setImageURI(uri);
        } catch (Exception e) {
            holder.titleImage.setImageResource(R.drawable.ic_perm_identity_white);
            Random rnd = new Random();
            holder.titleImage.setColorFilter(Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));

        }

    }

    @Override
    public int getItemCount() {
        if (items != null) {
            return items.size();
        }
        return 0;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private SimpleInterface anInterface;


        private TextView titlePrimary, titleSecondary;

        private ImageView titleImage;


        ViewHolder(View itemView, final SimpleInterface anInterface) {
            super(itemView);

            View titleItemGroup = itemView.findViewById(R.id.item_parent);
            titlePrimary = itemView.findViewById(R.id.item_name);
            titleSecondary = itemView.findViewById(R.id.item_number);
            titleImage = itemView.findViewById(R.id.item_picture);


            titleItemGroup.setOnClickListener(this);
            this.anInterface = anInterface;

        }

        @Override
        public void onClick(View view) {
            anInterface.didTapAt(getAdapterPosition());
        }


    }
}
