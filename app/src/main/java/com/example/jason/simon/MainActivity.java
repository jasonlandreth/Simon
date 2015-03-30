package com.example.jason.simon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    private Player simon;
    private boolean game_finished=false;
    private UpdateTask updateTask = null;
    private int round;
    private int playerRound;
    private TextView roundTv;
    private boolean playersTurn;
    private int highScore;
    private int roundSpeed = 1000;
    private int[] demo = { 0,1,2,3,0,1,2,3,0,1,2,3,0,1,2,3,3,0,1,2,1,0,2,3,1,0,1,0,1};
    private boolean run_demo;

    SoundPool soundPool;
    private Set<Integer> soundsLoaded;
    private int[] bp_sound;

    // Countdown timmer
    private boolean isCounterRunning  = false;
    final int start_time = 6000;
    final int interval = 1000;
    private CountDownTimer mCountDownTimer;
    private int[] soundsId = {R.raw.meow, R.raw.swooshr, R.raw.creakydoor, R.raw.special_razz, R.raw.lose,R.raw.sob, R.raw.poop_to_much,R.raw.death_is_your_only_escape, R.raw.dammit};

    private int[] imageButtonId ={R.id.red_off, R.id.blue_off, R.id.yellow_off, R.id.green_off};
    private int[] imagesOff ={R.drawable.red_off, R.drawable.blue_off, R.drawable.yellow_off, R.drawable.green_off};
    private int[] imagesOn = {R.drawable.red_on, R.drawable.blue_on, R.drawable.yellow_on, R.drawable.green_on};

    ImageButton[] imgBtn;

    // Dialog layouts for which round you use loose in
    private int[] dialog_layout = {R.layout.coleman_dialog, R.layout.lee_dialog, R.layout.nicholson_dialog};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Textview for updating round
        roundTv = (TextView) findViewById(R.id.round);
        imgBtn = new ImageButton[4];
        for(int i=0; i< 4; i++) {
            imgBtn[i] = (ImageButton) findViewById(imageButtonId[i]);
        }

        // Sets the 4 simon buttons when app created
        for(int i=0; i< 4; i++) {
            findViewById(imageButtonId[i]).setBackgroundResource(imagesOff[i]);

        }

        findViewById(R.id.startBtn).setOnClickListener(new StartButtonListener());
        findViewById(R.id.stopBtn).setOnClickListener(new StopButtonListener());
        findViewById(R.id.demoBtn).setOnClickListener(new DemoButtonListener());

        for(int i=0; i< 4;i++){
            findViewById(imageButtonId[i]).setOnClickListener(new GameButtonListener());
        }

        // Read file in that stores high scores
        try {
            FileInputStream in = openFileInput("highscore.txt");
            Scanner scanner = new Scanner(in);
            highScore = scanner.nextInt();
            TextView hs = (TextView)findViewById(R.id.highScore);
            hs.setText("High Score:" + Integer.toString(highScore));
            scanner.close();
        } catch (FileNotFoundException e){
            highScore = 0;
        }

        // Initializes SoundPool
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                                                @Override
                                                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                                                    if (status == 0) { // success
                                                        soundsLoaded.add(sampleId);
                                                    } else {
                                                        Log.w("SOUND_POOL", "WARNING: status is " + status + "???????");
                                                    }
                                                }
                                            }
        );
        bp_sound = new int[20];
        soundsLoaded = new HashSet<>();
        // load sounds
        for(int i =0; i< soundsId.length; i++) {
            bp_sound[i] = soundPool.load(getApplicationContext(), soundsId[i], 0);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    class GameButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(playersTurn == true) {
                int choice = -1;

                if (v.getId() == imageButtonId[0]) {
                    choice = 0;
                } else if (v.getId() == imageButtonId[1]) {
                    choice = 1;
                } else if (v.getId() == imageButtonId[2]) {
                    choice = 2;
                } else if (v.getId() == imageButtonId[3]) {
                    choice = 3;
                }

                Log.i("PLAYER", Integer.toString(choice));
                //  player.storeChoice(choice);
                if(simon.getChoice(playerRound-1) != choice){
                    onLose();
                    TextView countdown = (TextView) findViewById(R.id.countdown);
                    countdown.setText("");
                    return;
                }else{
                    setImageToOn(choice);
                    soundPool.play(bp_sound[choice], 1f, .5f, 0, 0, 1.f);
                    setImagesToOff();
                    mCountDownTimer.cancel(); // cancel
                    mCountDownTimer.start();  // then restart
                }
                playerRound++;
                if(playerRound == round){
                    TextView countdown = (TextView) findViewById(R.id.countdown);
                    countdown.setText(" ");
                    mCountDownTimer.cancel();
                    updateTask = new UpdateTask();
                    setButtonClickableStatus(false);
                    updateTask.execute();
                }
            }
        }
    }
    public void onLose(){
        int sound_id;
        Toast.makeText(getApplicationContext(), "LOST", Toast.LENGTH_LONG).show();
        round--;
        setButtonClickableStatus(false);
        mCountDownTimer.cancel(); // cancel
        //use the sound_id to get different sounds for each teacher
        sound_id= youLostDialog(round);
         soundPool.play(bp_sound[sound_id], 1f, .5f, 0, 0, 1.f);

        if(round > highScore){
            TextView hs = (TextView)findViewById(R.id.highScore);
            hs.setText("High Score:" + Integer.toString(round));

            try {
                FileOutputStream out = null;
                out = openFileOutput("highscore.txt", Context.MODE_PRIVATE);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                BufferedWriter bw = new BufferedWriter(osw);
                PrintWriter pw = new PrintWriter(bw);
                pw.print(Integer.toString(round));
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        playersTurn = false;
        round = 1;
        roundTv.setText("Round: 1");
        simon = null;
        mCountDownTimer = null;
    }
    class StartButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(simon == null){
                simon = new Player();
                round = 1;
                roundSpeed = 1000;
                playerRound  = 1;
                simon.storeChoice(new Random().nextInt(4) + 0);
                // Countdown timer
                mCountDownTimer = new CountDownTimer(start_time, interval) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        TextView countdown = (TextView) findViewById(R.id.countdown);
                        countdown.setText("Count down timer " + millisUntilFinished/interval );
                    }

                    @Override
                    public void onFinish() {
                        isCounterRunning = false;

                        onLose();
                        soundPool.play(bp_sound[6], 1f, .5f, 0, 0, 1.f);
                        TextView countdown = (TextView) findViewById(R.id.countdown);
                        countdown.setText("Out of time!!!");

                    }
                };
                if (updateTask == null) {
                    updateTask = new UpdateTask();
                    updateTask.execute();
                } else {
                    Log.i("INFO", "Update Task already running");
                }
            }

        }
    }
    class StopButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(updateTask != null){
                updateTask.cancel(true);
            }
            updateTask = null;
            playersTurn = false;
            mCountDownTimer.cancel(); // cancel
            mCountDownTimer = null;
            TextView countdown = (TextView) findViewById(R.id.countdown);
            countdown.setText("");
            round = 1;
            roundTv.setText("Round: 1");
            simon = null;

        }
    }
    class DemoButtonListener implements View.OnClickListener, View.OnLongClickListener{
        int count =0;

         @Override
        public void onClick(View v) {
              run_demo =true;
            if (updateTask !=null) {
                updateTask.cancel(true);
                updateTask = new UpdateTask();
                updateTask.execute();
            }else{//done it this way to so if the demo button is pushed it will override the game and go straight to demo mode
                updateTask = new UpdateTask();
                updateTask.execute();
            }
        }

        // Want to implement the RAVE sequence that goes crazy when pushed
        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }

    class UpdateTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            playersTurn = false;
            try {
                if(run_demo==false) {
                    Thread.sleep(2000);
                    int i = 0;
                    while (round > i) {
                        if (simon.getRound() > i) {
                            int choice = simon.getChoice(i);
                            publishProgress(choice, i);
                        } else {
                            Random generator = new Random();
                            int choice = generator.nextInt(4) + 0;
                            simon.storeChoice(choice);
                            publishProgress(choice, i);
                        }
                        i++;
                        if (simon.getRound() < 9) {
                            roundSpeed = 1000;
                        } else if (simon.getRound() <= 18) {
                            roundSpeed = 700;
                        } else if (simon.getRound() < 27) {
                            roundSpeed = 500;
                        } else {
                            roundSpeed = 300;
                        }
                        Thread.sleep(roundSpeed);

                    }
                    round++;

                }else{
                    int j=0;
                    while(demo.length > j){
                        while(demo.length<12){
                            publishProgress(demo[j], j);
                            Thread.sleep(50);
                            j++;
                        }
                        publishProgress(demo[j], j);
                        Log.i("CHOICE", ""+demo[j]+" POSITIION "+j);

                        Thread.sleep(50);
                        j++;
                        // THis will not allow buttons to be clicked during demo mode
                        setButtonClickableStatus(false);
                        findViewById(R.id.stopBtn).setClickable(false);
                        findViewById(R.id.startBtn).setClickable(false);

                    }
                }
            } catch (InterruptedException e) {
                Log.i("SIMON", "----- INTERRUPTED -----");
            } finally {
                playersTurn = true;
                playerRound = 1;
                publishProgress(-1, round);
                updateTask = null;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            if(values[0]  == -1){
                roundTv.setText("Round: "+Integer.toString(values[1]-1));
                for (int i=0; i< 4; i++) {
                    findViewById(imageButtonId[i]).setClickable(true);
                }
                if(run_demo==false) {
                    mCountDownTimer.start();
                }
            }else{
                setImageToOn(values[0]);
                soundPool.play(bp_sound[values[0]], 1f, .5f, 0, 0, 1.f);
                setImagesToOff();
                Log.i("SIMON", values[0].toString());
            }
        }
    }

    /* Not sure why this needs to be a separate method? It adds more code overall. */
    public void setImageToOn(int num){
        findViewById(imageButtonId[num]).setBackgroundResource(imagesOn[num]);
    }
    //The following sets all simon buttons to an off state after 2/3rds of a second.
    public void setImagesToOff(){
        new CountDownTimer(roundSpeed-100, 1){

            @Override
            public void onTick(long millisUntilFinished){}
            @Override
            public void onFinish() {
                for(int i=0; i< 4; i++) {
                    findViewById(imageButtonId[i]).setBackgroundResource(imagesOff[i]);

                }
            }
        }.start();
    }
    public void setButtonClickableStatus(boolean test){
        for (int i=0; i<4;i++) {
            findViewById(imageButtonId[i]).setClickable(test);
        }
    }
    public int youLostDialog(int round){
        int timer_length = 5000;
        int sound_id_num;
        final LinearLayout contentView;
        final ImageView image;
        if(round<3) {
            contentView = (LinearLayout) (this)
                    .getLayoutInflater().inflate(R.layout.coleman_dialog, null);
            image = (ImageView) contentView.findViewById(R.id.coleman_animation);
            sound_id_num = 5;

        }else if(round < 6){
            contentView = (LinearLayout) (this)
                    .getLayoutInflater().inflate(R.layout.lee_dialog, null);
            image = (ImageView) contentView.findViewById(R.id.lee_animation);
            sound_id_num = 6;

        }else{
            contentView = (LinearLayout) (this)
                    .getLayoutInflater().inflate(R.layout.nicholson_dialog, null);
            image = (ImageView) contentView.findViewById(R.id.nich_animation);
            sound_id_num = 7;

        }

        final AnimationDrawable animation = (AnimationDrawable) image.getDrawable();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(contentView);

        builder.setCancelable(true);

        final AlertDialog dialog = builder.create();
        // Sets the background to transparent in the aler dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
        animation.start();

        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                dialog.dismiss(); // when the task active then close the dialog
                t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
            }
        }, timer_length); // after 5 second (or 5000 miliseconds), the task will be active.

        return sound_id_num;
    }
    /* I was having problems with these two methods. So they aren't currently being used above. */
    private void setCountDownTimer() {
        if( !isCounterRunning ){
            isCounterRunning = true;
            mCountDownTimer.start();
        }
        else{
            mCountDownTimer.cancel(); // cancel
            mCountDownTimer.start();  // then restart
        }

    }
    private void cancelCountDownTimer(){
        TextView countdown = (TextView) findViewById(R.id.countdown);
        countdown.setText(" ");
        mCountDownTimer.cancel();
    }


}