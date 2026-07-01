package com.yourapp.util;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.yourapp.R;

public class AvatarUtil {

    public static void loadAvatar(String avatarUrl, String displayName, String username, ImageView imageView, TextView textView) {
        String name = displayName != null && !displayName.trim().isEmpty() ? displayName : username;
        String initial = name != null && !name.trim().isEmpty() ? String.valueOf(name.trim().charAt(0)).toUpperCase() : "?";

        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            if (imageView != null) {
                imageView.setVisibility(View.VISIBLE);
                Glide.with(imageView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.bg_avatar_circle)
                        .error(R.drawable.bg_avatar_circle)
                        .into(imageView);
            }
            if (textView != null) {
                textView.setVisibility(View.GONE);
            }
        } else {
            if (imageView != null) {
                imageView.setVisibility(View.GONE);
                imageView.setImageDrawable(null);
            }
            if (textView != null) {
                textView.setVisibility(View.VISIBLE);
                textView.setText(initial);
            }
        }
    }
}
