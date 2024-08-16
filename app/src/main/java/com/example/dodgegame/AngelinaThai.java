package com.example.dodgegame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


public class AngelinaThai extends AppCompatActivity {

    int kikiX = 350;
    int score = 0;
    int time = 0;
    MediaPlayer song;
    SoundPool explosion;
    int soundId;
    GameSurface gameSurface;
    int cometX, cometY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSurface = new GameSurface(this);
        setContentView(gameSurface);

        new CountDownTimer(50000, 1000) {
            public void onTick(long millisUntilFinished) {
                time = (int)(millisUntilFinished / 1000);
            }

            public void onFinish() {
                time = 0;
            }
        }.start();

        song = MediaPlayer.create(this, R.raw.song);

        explosion = new SoundPool.Builder().build();
        soundId = explosion.load(this, R.raw.explosion, 1);

        song.setLooping(true);
        song.start();

    }

    @Override
    protected void onPause(){
        super.onPause();
        gameSurface.pause();
        pausePlayer();
    }

    @Override
    protected void onResume(){
        super.onResume();
        gameSurface.resume();
        if (song != null)
            startPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (song != null){
            song.release();
            song = null;
        }
    }

    public void startPlayer(){
        if (!song.isPlaying())
            song.start();
    }

    public void pausePlayer() {
        if (song.isPlaying())
            song.pause();
    }

    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener, View.OnTouchListener{

        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false;
        Bitmap background, kiki, comet, brokenBroomstick;
        Paint paintProperty;
        int screenWidth;//1080
        int screenHeight;//2208
        boolean hit = false;
        int fallingAmount = 10;
        int amountMoved = 0;
        Handler handler = new Handler();

        public GameSurface(Context context) {
            super(context);

            holder = getHolder();

            background = BitmapFactory.decodeResource(getResources(), R.drawable.background);

            kiki = BitmapFactory.decodeResource(getResources(), R.drawable.kiki);
            comet = BitmapFactory.decodeResource(getResources(), R.drawable.comet);

            brokenBroomstick = BitmapFactory.decodeResource(getResources(), R.drawable.brokenbroomstick);

            Display screenDisplay = getWindowManager().getDefaultDisplay(); //how to get size of screen
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);
            screenWidth = sizeOfScreen.x;
            screenHeight = sizeOfScreen.y;

            paintProperty = new Paint();
            paintProperty.setTextSize(50);
            paintProperty.setColor(Color.WHITE);

            cometX = (int)(Math.random()*925)+1;
            cometY = 0;

            SensorManager manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            Sensor mySensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            manager.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);

        }

        @Override
        public void run() {
            setOnTouchListener(this);

            Drawable d = getResources().getDrawable(R.drawable.background, null);

            while (running == true) {

                if (holder.getSurface().isValid() == false)
                    continue; //if in a loop, it skips thing and continues on to next variable in loop

                Canvas canvas = holder.lockCanvas();
                if (time > 0) {

                    song.start();

                    d.setBounds(getLeft(), getTop(), getRight(), getBottom());
                    // draw the Drawable onto the canvas
                    d.draw(canvas);

                    canvas.drawText("Time Remaining: " + time, (screenWidth / 2) - 200, 200, paintProperty);
                    canvas.drawText("Score : " + score, (screenWidth / 2) - 100, 300, paintProperty);

                    if (!hit)
                        canvas.drawBitmap(kiki, kikiX + amountMoved, screenHeight - 600, null);
                    else
                        canvas.drawBitmap(brokenBroomstick, kikiX + amountMoved, screenHeight - 540, null);

                    cometY += fallingAmount; //falling amount is the speed it falls at. clicking on the screen makes it go faster

                    canvas.drawBitmap(comet, cometX, cometY, null);

                    //resetting the comet to the top
                    if (cometY > screenHeight) {
                        if (!hit) {
                            score++;
                        }
                        else {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    hit = false;
                                }
                            }, 450);
                        }
                        cometX = (int) (Math.random() * (screenWidth - comet.getWidth())) + 1;
                        cometY = -100;
                    }

                    checkForCollisions(canvas);

                    if ((kikiX + amountMoved >= 0) && (kikiX + amountMoved <= screenWidth - kiki.getWidth())){
                        kikiX += amountMoved;
                    }

/*
 int kikiWidth = kiki.getWidth();//358 -------
 int kikiHeight = kiki.getHeight();//366 ------ 320
 int cometWidth = comet.getWidth();//289 ------
 int cometHeight = comet.getHeight();//193 ----- 180
*/
                    holder.unlockCanvasAndPost(canvas);
                }
                else if (time == 0){
                    d.draw(canvas);
                    canvas.drawText("GAME OVER!",(screenWidth / 2) - 140,450, paintProperty);
                    canvas.drawText("Score : " + score, (screenWidth / 2) - 120, 300, paintProperty);
                    canvas.drawBitmap(kiki, kikiX + amountMoved, screenHeight - 600, null);
                    holder.unlockCanvasAndPost(canvas);
                    pausePlayer();
                }

            }
        }
        public void resume(){
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void pause() {
            running = false;
            while (true) {
                try {
                    gameThread.join();
                } catch (InterruptedException e) {
                }
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            amountMoved = (int) (event.values[0]*-1);

        }//closes onSensorChanged

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            fallingAmount += 4;
            return false;
        }

        private void checkForCollisions(Canvas canvas) {
            int kikiLeft = kikiX + amountMoved;
            int kikiRight = kikiLeft + kiki.getWidth() - 15;
            int kikiTop = screenHeight - 600;
            int kikiBottom = kikiTop + kiki.getHeight() - 46;

            int cometLeft = cometX;
            int cometRight = cometLeft + comet.getWidth() - 25;
            int cometTop = cometY;
            int cometBottom = cometTop + comet.getHeight() - 13;

            if ((kikiLeft < cometRight) && (kikiRight > cometLeft) && (kikiTop < cometBottom) && (kikiBottom > cometTop)) {
                if (!hit) {
                    hit = true;
                    score--;
                    explosion.play(soundId, 1, 1, 1, 0, 1);
                }
            }

        }

    } //closes GameSurface

}