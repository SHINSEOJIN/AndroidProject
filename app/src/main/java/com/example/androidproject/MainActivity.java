package com.example.androidproject;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidproject.adapter.MainAdapter;
import com.example.androidproject.callback.DialogCallback;
import com.example.androidproject.databinding.ActivityMainBinding;
import com.example.androidproject.db.DBHelper;
import com.example.androidproject.model.Student;
import com.example.androidproject.util.DialogUtil;
import com.example.androidproject.util.PermissionUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    ActivityResultLauncher<Intent> addLauncher;
    List<Student> datas = new ArrayList<>();
    MainAdapter adapter;
    private long backKeyPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        binding.etSearch.setVisibility(View.GONE);

        addLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent intent = result.getData();
                    if (result.getResultCode() == RESULT_OK && intent != null) {
                        int id = (int) intent.getLongExtra("id", 0);
                        String photo = intent.getStringExtra("photo");

                        for (Student s : datas) {
                            if (s.getId() == id) {
                                s.setPhoto(photo);
                                break;
                            }
                        }

                        adapter.setFilteredList(datas);
                        adapter.notifyDataSetChanged();
                    }
                }
        );

        PermissionUtil.checkAllPermission(this, isAllGranted -> {
            if (isAllGranted) {
                makeRecyclerView();
                binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterList(s.toString());
                    }

                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });
            } else {
                showDialog();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuMainAdd) {
            Intent intent = new Intent(this, AddStudentActivity.class);
            addLauncher.launch(intent);
        }
        if (item.getItemId() == R.id.action_search) {
            if (binding.etSearch.getVisibility() == View.VISIBLE) {
                binding.etSearch.setVisibility(View.GONE);
            } else {
                binding.etSearch.setVisibility(View.VISIBLE);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 3000) {
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(this, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 3000) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadStudentData();
        }
    }

    private void loadStudentData() {
        datas.clear();
        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM tb_student", null);

        while (cursor.moveToNext()) {
            Student student = new Student(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(5),
                    cursor.getString(4)
            );
            datas.add(student);
        }

        cursor.close();
        db.close();

        adapter.setFilteredList(datas);
        adapter.notifyDataSetChanged();
    }

    private void getListData() {
        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from tb_student order by name", null);

        while (cursor.moveToNext()) {
            Student student = new Student();
            student.setId(cursor.getInt(0));
            student.setName(cursor.getString(1));
            student.setEmail(cursor.getString(2));
            student.setPhone(cursor.getString(3));
            student.setPhoto(cursor.getString(4));
            student.setMemo(cursor.getString(5));

            datas.add(student);
        }

        cursor.close();
        db.close();
    }

    private void makeRecyclerView() {
        getListData();
        adapter = new MainAdapter(this, datas);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void showDialog() {
        DialogUtil.showMessageDialog(this, getString(R.string.permission_denied),
                "확인", null, new DialogCallback() {
                    @Override
                    public void onPositiveCallback() {}

                    @Override
                    public void onNegativeCallback() {}
                });
    }

    private void filterList(String query) {
        if (query.isEmpty()) {
            adapter.setFilteredList(datas);
            return;
        }

        List<Student> filteredList = new ArrayList<>();
        for (Student student : datas) {
            if (student.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(student);
            }
        }
        adapter.setFilteredList(filteredList);
    }
}