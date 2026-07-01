package com.yourapp.ui.chat;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.yourapp.R;

public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(0xFF000000);
        getWindow().setNavigationBarColor(0xFF000000);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_photo_viewer);

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        ImageView imageView = findViewById(R.id.iv_photo_viewer);
        Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .into(imageView);

        findViewById(R.id.btn_close_photo).setOnClickListener(v -> finish());
        imageView.setOnClickListener(v -> finish());
    }
}
