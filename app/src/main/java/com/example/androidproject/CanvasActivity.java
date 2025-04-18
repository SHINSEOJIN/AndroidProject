package com.example.androidproject;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.androidproject.databinding.ActivityCanvasBinding;

public class CanvasActivity extends AppCompatActivity {

    ActivityCanvasBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCanvasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
