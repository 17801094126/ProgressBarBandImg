package com.kz.circleprogressbarbandimg;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private CircleProgressBar circle_progressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        //设置当前进度条百分比
        circle_progressbar.setProgress(20);
        //点击完进度条之后进度条重新执行动画
        circle_progressbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                circle_progressbar.setProgress(40);
            }
        });
    }

    private void initView() {
        circle_progressbar = (CircleProgressBar) findViewById(R.id.circle_progressbar);
    }
}
