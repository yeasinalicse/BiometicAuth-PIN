package info.yeasin.biometicauth;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class SplashScreenActivity extends AppCompatActivity {
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 1000;
    private SharedPreferences prefs;
    SharedPreferences preferences;
    Executor executor;
    BiometricPrompt biometricPrompt;
    BiometricPrompt.PromptInfo promptInfo;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen_activity);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferences = this.getSharedPreferences(MainActivity.SHAREDPREFERENCE_NAME, this.MODE_PRIVATE);
        boolean isFingerPrintLock = preferences.getBoolean(MainActivity.IS_FINGERPRINT, false);
        boolean isDevicePinLock = preferences.getBoolean(MainActivity.IS_DEVICE_PIN, false);
        boolean isOwnSetPinLock = preferences.getBoolean(MainActivity.IS_OWN_SET_PIN, false);

        if (isFingerPrintLock || isDevicePinLock || isOwnSetPinLock) {
            if (isFingerPrintLock && isDevicePinLock) {
                biometricAuth(isFingerPrintLock, isDevicePinLock);
            } else if (isFingerPrintLock) {
                biometricAuth(isFingerPrintLock, isDevicePinLock);
            } else if (isDevicePinLock) {
                ShowConfirmLockPINActivity();
            } else if (isOwnSetPinLock) {
                dialogOwnPin();
            }

        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    intentData();
                }
            }, SPLASH_TIME_OUT);

        }


    }

    public void intentData() {
        Intent mainActivity = new Intent(this, MainActivity.class);
        startActivity(mainActivity);
        finish();
    }

    void ShowConfirmLockPINActivity() {
        Intent intent = new Intent(Intent.ACTION_RUN);
        intent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.ConfirmLockPassword"));
        startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
    }

    public void biometricAuth(boolean isFingerPrint, boolean isDevicePin) {
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                intentData();

            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                if (isFingerPrint && isDevicePin) {
                    ShowConfirmLockPINActivity();
                } else {
                    SplashScreenActivity.this.finish();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();

                Toast.makeText(SplashScreenActivity.this, "FAILED", Toast.LENGTH_LONG).show();
            }
        });

        if (isFingerPrint && isDevicePin) {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Touch id required")
                    .setNegativeButtonText("use password")
                    .build();
        } else {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Touch id required")
                    .setNegativeButtonText("exit")
                    .build();
        }
        biometricPrompt.authenticate(promptInfo);
    }


    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                intentData();
            } else {
                Toast.makeText(this, "Password not match", Toast.LENGTH_SHORT).show();

            }
        }
    }


    public void dialogOwnPin() {
        SharedPreferences.Editor editor = preferences.edit();
        String oldPassword = preferences.getString(MainActivity.OWN_SET_PIN, "");
        AlertDialog.Builder builder = new AlertDialog.Builder(SplashScreenActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialog_set_own_password, viewGroup, false);
        builder.setView(dialogView);
        alertDialog = builder.create();
        alertDialog.setCancelable(false);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        showSoftKeyboard();
        TextView tvChangePass = dialogView.findViewById(R.id.tv_change_pass);
        tvChangePass.setVisibility(View.GONE);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btn_save);
        btnSave.setText("Confirm");
        etPassword.setHint("Confirm Pin");
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currntPassword = etPassword.getText().toString().trim();
                if (oldPassword.equalsIgnoreCase("" + currntPassword)) {
                    intentData();
                } else {
                    etPassword.setError("Please set validate PIN");
                }

                hidenSoftKeyboard();
            }
        });

        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setText("Exit");
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SplashScreenActivity.this.finish();
                hidenSoftKeyboard();
            }
        });
        CheckBox changeCheck = dialogView.findViewById(R.id.item_check);
        changeCheck.setVisibility(View.GONE);
        if (!alertDialog.isShowing()) {
            alertDialog.show();
        }

    }


    public void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void hidenSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
    }

}
