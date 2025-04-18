package com.example.androidproject;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidproject.adapter.DetailAdapter;
import com.example.androidproject.databinding.ActivityDetailBinding;
import com.example.androidproject.db.DBHelper;
import com.example.androidproject.model.Student;
import com.example.androidproject.util.BitmapUtil;
import com.example.androidproject.view.MyCanvasView;

import java.text.SimpleDateFormat;
import java.util.*;

public class DetailActivity extends AppCompatActivity {

    ActivityDetailBinding binding;
    Student student;
    ArrayList<Map<String, String>> scoreList;
    DetailAdapter adapter;
    ActivityResultLauncher<Intent> requestGalleryLauncher;
    public ActivityResultLauncher<Intent> editScoreLauncher;

    String filePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        binding.detailAddScoreBtn.setBackgroundTintList(null);

        int id = getIntent().getIntExtra("id", 0);
        boolean editMode = getIntent().getBooleanExtra("editMode", false);

        ActivityResultLauncher<Intent> addScoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent intent = result.getData();
                    String score = intent.getStringExtra("score");
                    long date = intent.getLongExtra("date", 0);

                    HashMap<String, String> map = new HashMap<>();
                    map.put("score", score);
                    Date d = new Date(date);
                    SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
                    map.put("date", sd.format(d));
                    map.put("dateLong", String.valueOf(date));

                    scoreList.add(map);
                    adapter.notifyDataSetChanged();
                    updateDonutWithLatestScore();
                }
        );

        binding.btnWeb.setOnClickListener(view -> {
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra("scoreList", scoreList);
            startActivity(intent);
        });

        binding.btnShare.setOnClickListener(view -> {
            if (student.getPhone() == null || student.getPhone().trim().isEmpty()) {
                Toast.makeText(this, student.getName() + "님의 성적을 받으실 분의 연락처를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            String message;
            if (scoreList != null && !scoreList.isEmpty()) {
                Map<String, String> latest = scoreList.get(scoreList.size() - 1);
                String date = latest.get("date");
                String score = latest.get("score") + "점";
                message = date + " - " + score;
            } else {
                message = "최근 시험 점수 정보가 없습니다.";
            }

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + student.getPhone()));
            intent.putExtra("sms_body", message);

            if (intent.resolveActivity(getPackageManager()) != null) {
                Toast.makeText(this, student.getName() + "님에게 문자를 전송합니다.", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("점수 메시지", message);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "문자앱이 없거나 자동 입력이 불가합니다. 클립보드에 복사되었습니다.", Toast.LENGTH_LONG).show();
            }
        });

        binding.detailAddScoreBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, ScoreAddActivity.class);
            intent.putExtra("id", id);
            addScoreLauncher.launch(intent);
        });

        if (editMode) {
            binding.donutView.setVisibility(View.GONE);
            binding.btnSave.setVisibility(View.VISIBLE);
            binding.btnWeb.setVisibility(View.GONE);
            binding.btnShare.setVisibility(View.GONE);
            binding.detailAddScoreBtn.setVisibility(View.GONE);
            binding.detailRecyclerView.setVisibility(View.GONE);
            binding.detailName.setEnabled(true);
            binding.detailEmail.setEnabled(true);
            binding.detailPhone.setEnabled(true);
            binding.detailMemo.setEnabled(true);
        } else {
            binding.donutView.setVisibility(View.VISIBLE);
            binding.btnSave.setVisibility(View.GONE);
            binding.btnWeb.setVisibility(View.VISIBLE);
            binding.btnShare.setVisibility(View.VISIBLE);
            binding.detailAddScoreBtn.setVisibility(View.VISIBLE);
            binding.detailRecyclerView.setVisibility(View.VISIBLE);
            binding.detailName.setEnabled(false);
            binding.detailEmail.setEnabled(false);
            binding.detailPhone.setEnabled(false);
            binding.detailMemo.setEnabled(false);
        }

        binding.btnSave.setOnClickListener(v -> {
            String name = binding.detailName.getText().toString();
            String email = binding.detailEmail.getText().toString();
            String phone = binding.detailPhone.getText().toString();
            String memo = binding.detailMemo.getText().toString();

            DBHelper helper = new DBHelper(this);
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("UPDATE tb_student SET name=?, email=?, phone=?, memo=? WHERE _id=?",
                    new String[]{name, email, phone, memo, String.valueOf(student.getId())});
            db.close();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("photo", filePath);
            resultIntent.putExtra("id", student.getId());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        requestGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        Uri uri = result.getData().getData();
                        String[] proj = new String[]{MediaStore.Images.Media.DATA};
                        Cursor galleryCursor = getContentResolver().query(uri, proj, null, null, null);
                        if (galleryCursor != null && galleryCursor.moveToFirst()) {
                            filePath = galleryCursor.getString(0);
                            DBHelper helper = new DBHelper(this);
                            SQLiteDatabase db = helper.getWritableDatabase();
                            db.execSQL("update tb_student set photo=? where _id=?",
                                    new String[]{filePath, String.valueOf(id)});
                            db.close();
                        }

                        Bitmap bitmap = BitmapUtil.getGalleryBitmapFromStream(this, uri);
                        if (bitmap != null) {
                            binding.detailImage.setImageBitmap(bitmap);
                            setResult(RESULT_OK);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );

        editScoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent intent = result.getData();
                        String editedScore = intent.getStringExtra("score");
                        long editedDate = intent.getLongExtra("date", 0);

                        for (int i = 0; i < scoreList.size(); i++) {
                            if (scoreList.get(i).get("dateLong").equals(String.valueOf(editedDate))) {
                                scoreList.get(i).put("score", editedScore);
                                scoreList.get(i).put("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date(editedDate)));
                                scoreList.get(i).put("dateLong", String.valueOf(editedDate));
                                adapter.notifyItemChanged(i);
                                updateDonutWithLatestScore();
                                break;
                            }
                        }
                    }
                }
        );

        setInitStudentData(id);
        setInitScoreData(id);
    }

    private void setInitStudentData(int id) {
        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from tb_student where _id=?", new String[]{String.valueOf(id)});

        String photoFilePath = null;

        if (cursor.moveToFirst()) {
            String name = cursor.getString(1);
            String email = cursor.getString(2);
            String phone = cursor.getString(3);

            binding.detailName.setText(name);
            binding.detailEmail.setText(email);
            binding.detailPhone.setText(phone);
            binding.detailMemo.setText(cursor.getString(5));

            photoFilePath = cursor.getString(4);
            student = new Student(cursor.getInt(0), name, email, phone, cursor.getString(5), photoFilePath);
        }

        db.close();

        Bitmap bitmap = BitmapUtil.getGalleryBitmapFromFile(this, photoFilePath);
        if (bitmap != null) {
            binding.detailImage.setImageBitmap(bitmap);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("photo", filePath);
            resultIntent.putExtra("id", student.getId());
            setResult(RESULT_OK, resultIntent);
        }

        binding.detailImage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            requestGalleryLauncher.launch(intent);
        });
    }
    private void setInitScoreData(int id) {
        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("select score, date from tb_score where student_id = ? order by date",
                new String[]{String.valueOf(id)});
        scoreList = new ArrayList<>();
        while (c.moveToNext()) {
            HashMap<String, String> map = new HashMap<>();
            map.put("score", c.getString(0));
            map.put("dateLong", c.getString(1));
            Date d = new Date(Long.parseLong(c.getString(1)));
            SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
            map.put("date", sd.format(d));
            scoreList.add(map);
        }
        db.close();

        binding.detailRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DetailAdapter(this, scoreList);
        binding.detailRecyclerView.setAdapter(adapter);
        binding.detailRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        updateDonutWithLatestScore();
    }

    public void updateDonutWithLatestScore() {
        if (scoreList != null && !scoreList.isEmpty()) {
            Map<String, String> latest = scoreList.get(scoreList.size() - 1);
            int latestScore = Integer.parseInt(latest.get("score"));
            MyCanvasView donutView = findViewById(R.id.donutView);
            donutView.setScore(latestScore);
        }
    }
}
