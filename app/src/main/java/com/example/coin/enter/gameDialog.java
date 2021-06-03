package com.example.coin.enter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.example.coin.R;

import java.util.ArrayList;

import androidx.core.content.ContextCompat;

public class gameDialog extends Activity {
    private RelativeLayout layoutRoulette;

    private Button BtnStart;
    private ArrayList<String> items;
    private float initAngle = 0.0f;

    private String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        BtnStart = findViewById(R.id.btnRotate); // Rotate button
        layoutRoulette = findViewById(R.id.layoutRoulette); // pick

        items = new ArrayList<>();
        items.add("소 주 1잔");
        items.add("맥 주 2잔");
        items.add("소 주 3잔");
        items.add("소 주 2잔");
        items.add("맥 주 1잔");

        layoutRoulette.addView(new CircleManager(this, items.size()));

        BtnStart.setOnClickListener(view -> {
            rotateLayout(layoutRoulette, items.size());
        });


    }

    public void rotateLayout(final RelativeLayout layout, final int num) {  // when rotation begins
        final float fromAngle = getRandom(360) + 3600 + initAngle;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getResult(fromAngle, num); // start when animation complete
                Intent intent = new Intent();
                intent.putExtra("sendText", text); //사용자에게 입력받은값 넣기
                setResult(RESULT_OK, intent); //결과를 저장
                gameDialog.this.finish();
            }
        }, 3000);

        RotateAnimation rotateAnimation = new RotateAnimation(initAngle, fromAngle,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        rotateAnimation.setInterpolator(AnimationUtils.loadInterpolator(this, android.R.anim.accelerate_decelerate_interpolator));
        rotateAnimation.setDuration(3000);
        rotateAnimation.setFillEnabled(true);
        rotateAnimation.setFillAfter(true);
        layout.startAnimation(rotateAnimation);
    }


    // get Angle to random
    private int getRandom(int maxNumber) {
        double r = Math.random();
        return (int) (r * maxNumber);
    }

    private void getResult(float angle, int num_roulette) { //queek change

        angle = angle % 360;

        for (int i = 1; i < num_roulette; i++) {
            if (angle < (360 / num_roulette) * i) {
                text = items.get(i - 1);
            }

        }
    }


    public class CircleManager extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //  private int[] COLORS = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.GRAY};
        private ArrayList<Integer> color = new ArrayList<Integer>();
        private int num;

        public CircleManager(Context context, int num) {
            super(context);
            this.num = num;
            this.color.add(ContextCompat.getColor(context, R.color.col1));
            this.color.add(ContextCompat.getColor(context, R.color.col2));
            this.color.add(ContextCompat.getColor(context, R.color.col3));
            this.color.add(ContextCompat.getColor(context, R.color.col4));
            this.color.add(ContextCompat.getColor(context, R.color.col5));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = layoutRoulette.getWidth();
            int height = layoutRoulette.getHeight();
            int sweepAngle = 360 / num;

            RectF rectF = new RectF(0, 0, width, height);
            Rect rect = new Rect(0, 0, width, height);

            int centerX = (rect.left + rect.right) / 2;
            int centerY = (rect.top + rect.bottom) / 2;
            int radius = (rect.right - rect.left) / 2;

            int rectLeft = rect.left + getPaddingLeft();
            int rectRight = rect.right - getPaddingRight();
            // int rectTop = rect.height() / 2f - rectRight / 2f + getPaddingTop();
            ///int rectBottom = rect.height() / 2f + rectRight / 2f - getPaddingRight();
            //rectF = new RectF(rectLeft, rectTop, rectRight, rectBottom);


            int temp = 0;

            for (int i = 0; i < num; i++) {
                paint.setColor(this.color.get(i));
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setAntiAlias(true);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawArc(rectF, temp, sweepAngle, true, paint);

                float medianAngle = (temp + (sweepAngle / 2f)) * (float) Math.PI / 180f;

                paint.setColor(Color.BLACK);
                paint.setTextSize(45);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);

                float arcCenterX = (float) (centerX + (radius * Math.cos(medianAngle))); // Arc's center X
                float arcCenterY = (float) (centerY + (radius * Math.sin(medianAngle))); // Arc's center Y

                // put text at middle of Arc's center point and Circle's center point
                float textX = (centerX + arcCenterX) / 2;
                float textY = (centerY + arcCenterY) / 2;

                canvas.drawText(items.get(i), textX, textY, paint);
                temp += sweepAngle;
            }
        }
    }
}