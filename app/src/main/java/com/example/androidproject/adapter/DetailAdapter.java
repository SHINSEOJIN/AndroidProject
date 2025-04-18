package com.example.androidproject.adapter;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidproject.ScoreAddActivity;
import com.example.androidproject.databinding.ItemDetailBinding;
import com.example.androidproject.db.DBHelper;
import com.example.androidproject.DetailActivity;

import java.util.ArrayList;
import java.util.Map;

class DetailViewHolder extends RecyclerView.ViewHolder {
    ItemDetailBinding binding;
    DetailViewHolder(ItemDetailBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}

public class DetailAdapter extends RecyclerView.Adapter<DetailViewHolder> {
    ArrayList<Map<String, String>> datas;
    Activity context;

    public DetailAdapter(Activity context, ArrayList<Map<String, String>> datas) {
        this.context = context;
        this.datas = datas;
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    @NonNull
    @Override
    public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDetailBinding binding = ItemDetailBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new DetailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
        Map<String, String> score = datas.get(position);
        String scoreValue = score.get("score");
        String dateText = score.get("date");
        String dateLong = score.get("dateLong");

        holder.binding.detailItemScore.setText(scoreValue);
        holder.binding.detailItemDate.setText(dateText);

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("옵션 선택")
                    .setItems(new CharSequence[]{"수정", "삭제"}, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(context, ScoreAddActivity.class);
                            intent.putExtra("mode", "edit");
                            intent.putExtra("score", scoreValue);
                            intent.putExtra("date", Long.parseLong(dateLong));
                            intent.putExtra("position", position);
                            ((DetailActivity) context).editScoreLauncher.launch(intent);
                        } else {
                            try {
                                DBHelper helper = new DBHelper(context);
                                SQLiteDatabase db = helper.getWritableDatabase();
                                db.execSQL("DELETE FROM tb_score WHERE date = ?", new String[]{dateLong});
                                db.close();

                                int correctIndex = -1;
                                for (int i = 0; i < datas.size(); i++) {
                                    if (datas.get(i).get("dateLong").equals(dateLong)) {
                                        correctIndex = i;
                                        break;
                                    }
                                }

                                if (correctIndex != -1) {
                                    datas.remove(correctIndex);
                                    notifyItemRemoved(correctIndex);
                                    ((DetailActivity) context).updateDonutWithLatestScore();
                                } else {
                                    Toast.makeText(context, "삭제할 점수를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(context, "점수 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).show();
            return true;
        });
    }
}