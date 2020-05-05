package com.example.food_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class Pop extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.popwindow);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        TextView heading = findViewById(R.id.Heading);
        heading.setText(getIntent().getStringExtra("Heading"));
        heading.setTextSize(24);

        TextView text = findViewById(R.id.details);
        text.setText(getIntent().getStringExtra("Info"));

        TextView linkText = findViewById(R.id.link);
        linkText.setClickable(true);
        linkText.setMovementMethod(LinkMovementMethod.getInstance());
        getWindow().setLayout((int)(width*.8), (int) (height*.6));
        String link = "<a href='" + getIntent().getStringExtra("Link") + "'> Learn More </a>";
        linkText.setText(Html.fromHtml(link));


    }
}

