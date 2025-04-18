package com.example.androidproject.adapter;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidproject.DetailActivity;
import com.example.androidproject.R;
import com.example.androidproject.databinding.ItemMainBinding;
import com.example.androidproject.db.DBHelper;
import com.example.androidproject.model.Student;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainViewHolder> {

    private final List<Student> originalList;
    private List<Student> filteredList;
    private final Activity context;

    public MainAdapter(Activity context, List<Student> datas) {
        this.context = context;
        this.originalList = datas;
        this.filteredList = new ArrayList<>(datas);
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMainBinding binding = ItemMainBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new MainViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        Student student = filteredList.get(position);
        holder.binding.itemNameView.setText(student.getName());

        if (student.getPhoto() != null && !student.getPhoto().isEmpty()) {
            File file = new File(student.getPhoto());
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    holder.binding.itemImageView.setImageBitmap(bitmap.copy(bitmap.getConfig(), true));
                } else {
                    holder.binding.itemImageView.setImageResource(R.drawable.ic_student_small);
                }
            } else {
                holder.binding.itemImageView.setImageResource(R.drawable.ic_student_small);
            }
        } else {
            holder.binding.itemImageView.setImageResource(R.drawable.ic_student_small);
        }

        holder.binding.itemNameView.setOnClickListener(view -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("id", student.getId());
            ((Activity) context).startActivityForResult(intent, 100);
        });

        holder.binding.itemEditView.setOnClickListener(view -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("id", student.getId());
            intent.putExtra("editMode", true); // 수정 모드 활성화
            ((Activity) context).startActivityForResult(intent, 100);
        });

        holder.binding.itemCallView.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + student.getPhone()));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "전화 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.binding.itemNameView.setOnLongClickListener(view -> {
            new AlertDialog.Builder(context)
                    .setTitle("삭제 확인")
                    .setMessage(student.getName() + " 학생을 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        DBHelper helper = new DBHelper(context);
                        SQLiteDatabase db = helper.getWritableDatabase();
                        db.execSQL("DELETE FROM tb_student WHERE _id = ?", new String[]{String.valueOf(student.getId())});
                        db.close();

                        originalList.removeIf(s -> s.getId() == student.getId());
                        filteredList.removeIf(s -> s.getId() == student.getId());
                        notifyDataSetChanged();

                        Toast.makeText(context, student.getName() + " 학생이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    })


                    .setNegativeButton("취소", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void setFilteredList(List<Student> list) {
        this.filteredList = list;
        notifyDataSetChanged();
    }

    public static class MainViewHolder extends RecyclerView.ViewHolder {
        ItemMainBinding binding;

        public MainViewHolder(ItemMainBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
