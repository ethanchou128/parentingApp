package cmpt276.as3.cmpt276hydrogenproject;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * activity that allows user to set timer or choose from a selection
 * of preset times with buttons on the screen.
 */
public class TimeoutActivity extends AppCompatActivity {

    private final int CONVERT_MILLIS_TO_SECONDS = 60000;
    private final int COUNTDOWN_INTERVAL = 1000;
    private final int SECONDS_PER_HOUR = 3600;
    private final int SECONDS_PER_MINUTE = 60;

    private final int DEFAULT_SETTING_1 = 1;
    private final int DEFAULT_SETTING_2 = 2;
    private final int DEFAULT_SETTING_3 = 3;
    private final int DEFAULT_SETTING_4 = 5;
    private final int DEFAULT_SETTING_5 = 10;

    private final int INITIAL_DEFAULT = 600000;

    private Button startTimerBtn;
    private Button setTimeBtn;
    private Button resetTimerBtn;
    private TextView displayTimerField;
    private EditText editTextInput;
    private CountDownTimer backgroundTimerCountDown;
    private MaterialProgressBar materialProgressBar;

    private long startTimeInMilli;
    private long endOfTime;
    private boolean timerWorkingState;
    private boolean isFirstTime;
    private long leftTimeInMilli;
    private double timeModifier = 1;

    AlarmManager alarmManager;
    MenuItem menuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.timeout_activity);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        setActionBar();
        materialProgressBar = findViewById(R.id.timerCountdownBar);

        displayTimerField = findViewById(R.id.textDisplayTimer);
        editTextInput = findViewById(R.id.minuteTextInput);
        setTimeBtn = findViewById(R.id.btnSetTimer);
        startTimerBtn = findViewById(R.id.btnStartTimer);
        resetTimerBtn = findViewById(R.id.btnResetTimer);

        if (timerWorkingState) {
            loadRate();
            setRateDisplay();
        }

        startTimerBtn.setOnClickListener(v -> {

            if (timerWorkingState) {
                pauseTimer();
                stopNotification();
            } else {
                setRateDisplay();
                startTimer();
            }
        });

        setTimeBtn.setOnClickListener(v -> {
            String input = editTextInput.getText().toString();
            // Check for no value in the field
            if (input.length() == 0) {
                Toast.makeText(TimeoutActivity.this, "Field is empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Parse string input into long
            long inputInMilli = Long.parseLong(input) * CONVERT_MILLIS_TO_SECONDS;
            if (inputInMilli == 0) {
                Toast.makeText(TimeoutActivity.this, "Invalid: Enter 1 minute or greater", Toast.LENGTH_SHORT).show();
                return;
            }
            editTextInput.setText("");
            setTime(inputInMilli);
        });

        resetTimerBtn.setOnClickListener(v -> resetTimer());

        setAllPresetTimers();
    }

    private void activateNotification() {
        Intent intent = new Intent(TimeoutActivity.this, NotificationBroadcast.class);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getBroadcast(TimeoutActivity.this, 0, intent, 0);
        //code was followed from demo from https://www.youtube.com/watch?v=nl-dheVpt8o
        long timeWhenButtonClicked = System.currentTimeMillis();
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                (long) (timeWhenButtonClicked + leftTimeInMilli/timeModifier),
                pendingIntent);
    }

    private void stopNotification() {
        Intent intent = new Intent(TimeoutActivity.this, NotificationBroadcast.class);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getBroadcast(TimeoutActivity.this, 0, intent, 0);
        alarmManager.cancel(pendingIntent);
    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, TimeoutActivity.class);
    }

    private void setActionBar() {
        Objects.requireNonNull(getSupportActionBar()).setTitle("Timeout Timer");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources()
                .getColor(R.color.darker_navy_blue)));
    }

    private void setTime(long milliseconds) {
        startTimeInMilli = milliseconds;
        closeKeyboard();
        resetTimer();
        startTimerBtn.setText(R.string.btnTextStart);
    }

    private void startTimer() {
        if (menuItem != null) {
            menuItem.setVisible(true);
        }
        endOfTime = (long) (System.currentTimeMillis() + leftTimeInMilli/timeModifier);
        materialProgressBar.setVisibility(MaterialProgressBar.VISIBLE);
        activateNotification();

        backgroundTimerCountDown = new CountDownTimer((long) (leftTimeInMilli/timeModifier), (long) (COUNTDOWN_INTERVAL/timeModifier)) {
            @Override
            public void onTick(long millisUntilFinished) {
                leftTimeInMilli = millisUntilFinished;
                tickVisualTimer();
                updateDisplayTimer();
            }

            private void tickVisualTimer() {
                double timeRemainingPercent = (double)leftTimeInMilli/(double)startTimeInMilli;
                timeRemainingPercent *= timeModifier;
                timeRemainingPercent *= 1000000;
                if (leftTimeInMilli == 0) {
                    materialProgressBar.setVisibility(MaterialProgressBar.INVISIBLE);
                } else {
                    materialProgressBar.setProgress((int) timeRemainingPercent, true);
                }
            }

            @Override
            public void onFinish() {
                timerWorkingState = false;
                timeModifier = 1;
                setRateDisplay();
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
                updateLayoutVisibility();
            }
        }.start();

        timerWorkingState = true;
        updateLayoutVisibility();
    }

    private void resetTimer() {
        Intent intent = new Intent(TimeoutActivity.this, NotificationBroadcast.class);
        timeModifier = 1;
        setRateDisplay();
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getBroadcast(TimeoutActivity.this, 0, intent, 0);
        if (backgroundTimerCountDown != null) {
            pauseTimer();
        }
        materialProgressBar.setVisibility(MaterialProgressBar.INVISIBLE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
        alarmManager.cancel(pendingIntent);
        leftTimeInMilli = startTimeInMilli;
        updateDisplayTimer();
        updateLayoutVisibility();
        startTimerBtn.setText(R.string.btnTextStart);
    }

    private void pauseTimer() {
        menuItem.setVisible(false);
        leftTimeInMilli*=timeModifier;
        if (backgroundTimerCountDown != null) {
            backgroundTimerCountDown.cancel();
        }
        timerWorkingState = false;
        updateLayoutVisibility();
    }

    private void updateDisplayTimer() {
        int hours = (int) (leftTimeInMilli / COUNTDOWN_INTERVAL) / SECONDS_PER_HOUR;
        int minutes = (int) ((leftTimeInMilli / COUNTDOWN_INTERVAL) % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        int seconds = (int) (leftTimeInMilli / COUNTDOWN_INTERVAL) % SECONDS_PER_MINUTE;


        if (timerWorkingState) {
            hours = (int) (leftTimeInMilli*timeModifier / COUNTDOWN_INTERVAL) / SECONDS_PER_HOUR;
            minutes = (int) ((leftTimeInMilli*timeModifier / COUNTDOWN_INTERVAL) % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
            seconds = (int) (leftTimeInMilli*timeModifier / COUNTDOWN_INTERVAL) % SECONDS_PER_MINUTE;
        }

        String timeLeftFormat;
        if (!(hours < 1)) {
            timeLeftFormat = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormat = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }

        displayTimerField.setText(timeLeftFormat);
    }

    /**
     * Method that edits the visibility / invisibility of the activity buttons
     * depending on the current state
     */
    private void updateLayoutVisibility() {
        if (timerWorkingState) {                            // Timer is currently running
            editTextInput.setVisibility(View.INVISIBLE);
            setTimeBtn.setVisibility(View.INVISIBLE);
            resetTimerBtn.setVisibility(View.VISIBLE);
            startTimerBtn.setText(R.string.btnTextPause);
            resetTimerBtn.setText("Stop");
        } else {                                            // Timer is not running
            editTextInput.setVisibility(View.VISIBLE);
            setTimeBtn.setVisibility(View.VISIBLE);
            startTimerBtn.setText(R.string.timerTextResume);
            resetTimerBtn.setText("Reset");

            if (leftTimeInMilli < startTimeInMilli) {
                resetTimerBtn.setVisibility(View.VISIBLE);
            } else {
                resetTimerBtn.setVisibility(View.INVISIBLE);
            }

            if (leftTimeInMilli < COUNTDOWN_INTERVAL) {
                startTimerBtn.setVisibility(View.INVISIBLE);
            } else {
                startTimerBtn.setVisibility(View.VISIBLE);
            }
        }

        if (!isFirstTime) {
            isFirstTime = true;
            startTimerBtn.setText(R.string.btnTextStart);
            resetTimerBtn.setVisibility(View.INVISIBLE);
        } else {
            if (!timerWorkingState) {
                startTimerBtn.setText(R.string.timerTextResume);
            }
        }
    }

    /**
     * Set pre-set timer buttons for the user to easily access in increments of:
     * 1, 2, 3, 5 and 10 minutes.
     */
    private void setAllPresetTimers() {
        Button oneMinBtn = findViewById(R.id.oneMinBtn);
        Button twoMinBtn = findViewById(R.id.twoMinBtn);
        Button threeMinBtn = findViewById(R.id.threeMinBtn);
        Button fiveMinBtn = findViewById(R.id.fiveMinBtn);
        Button tenMinBtn = findViewById(R.id.tenMinBtn);

        setPresetTimer(oneMinBtn, DEFAULT_SETTING_1);
        setPresetTimer(twoMinBtn, DEFAULT_SETTING_2);
        setPresetTimer(threeMinBtn, DEFAULT_SETTING_3);
        setPresetTimer(fiveMinBtn, DEFAULT_SETTING_4);
        setPresetTimer(tenMinBtn, DEFAULT_SETTING_5);
    }

    /**
     * Assigns a time to be set when the button parameter is pressed.
     */
    private void setPresetTimer(Button presetTimeBtn, int inputInMilli) {
        inputInMilli *= CONVERT_MILLIS_TO_SECONDS;
        int finalInputInMilli = inputInMilli;
        presetTimeBtn.setOnClickListener(v -> {
            if (backgroundTimerCountDown != null) {
                pauseTimer();
            }
            setTime(finalInputInMilli);
        });
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("startTimeInMillis", startTimeInMilli);
        editor.putLong("millisLeft", leftTimeInMilli);
        editor.putBoolean("timerRunning", timerWorkingState);
        editor.putLong("endTime", endOfTime);
        saveRate();
        editor.apply();
        if (backgroundTimerCountDown != null) {
            backgroundTimerCountDown.cancel();
            isFirstTime = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        startTimeInMilli = prefs.getLong("startTimeInMillis", INITIAL_DEFAULT);
        timerWorkingState = prefs.getBoolean("timerRunning", false);
        leftTimeInMilli = prefs.getLong("millisLeft", startTimeInMilli);
        loadRate();
        setRateDisplay();
        updateLayoutVisibility();
        updateDisplayTimer();

        if (timerWorkingState) {
            endOfTime = prefs.getLong("endTime", 0);
            leftTimeInMilli = endOfTime - System.currentTimeMillis();

            leftTimeInMilli*=timeModifier;

            if (leftTimeInMilli <= 0) {
                timerWorkingState = false;
                if (backgroundTimerCountDown != null) {
                    backgroundTimerCountDown.cancel();
                }
                leftTimeInMilli = 0;
                updateDisplayTimer();
                updateLayoutVisibility();
                materialProgressBar.setVisibility(MaterialProgressBar.INVISIBLE);
            } else {
                startTimer();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.timeout_rate,menu);
        menuItem = menu.findItem(R.id.speedChanger);
        if (!timerWorkingState) {
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if(timerWorkingState) {
                    moveTaskToBack(true);
                }else {
                    finish();
                }
                return true;
            case R.id.percent25:
                leftTimeInMilli*=timeModifier;
                timeModifier = 0.25;
                break;
            case R.id.percent50:
                leftTimeInMilli*=timeModifier;
                timeModifier = 0.50;
                break;
            case R.id.percent75:
                leftTimeInMilli*=timeModifier;
                timeModifier = 0.75;
                break;
            case R.id.percent100:
                leftTimeInMilli*=timeModifier;
                timeModifier = 1;
                break;
            case R.id.percent200:
                leftTimeInMilli*=timeModifier;
                timeModifier = 2;
                break;
            case R.id.percent300:
                leftTimeInMilli*=timeModifier;
                timeModifier = 3;
                break;
            case R.id.percent400:
                leftTimeInMilli*=timeModifier;
                timeModifier = 4;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        saveRate();
        setRateDisplay();
        changeRateTimer();
        return true;
    }

    private void changeRateTimer() {
        if (backgroundTimerCountDown != null) {
            backgroundTimerCountDown.cancel();
        }
        startTimer();
    }

    private void setRateDisplay() {
        TextView rateDisplay = findViewById(R.id.timeModifierDisplay);
        double rateDouble = timeModifier*100;
        int percentRate = (int) rateDouble;
        rateDisplay.setText("Timer rate " + percentRate + "% ");
    }

    private void saveRate() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int tempInt = (int) (timeModifier*100);
        editor.putInt("TimerRate", tempInt);
        editor.apply();
    }

    private void loadRate() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        int tempInt = prefs.getInt("TimerRate", 100);
        timeModifier = (double) tempInt/100;
    }
}
