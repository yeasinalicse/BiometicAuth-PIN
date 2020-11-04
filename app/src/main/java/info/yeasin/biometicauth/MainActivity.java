package info.yeasin.biometicauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.crypto.KeyGenerator;

import static android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

public class MainActivity extends AppCompatActivity {
    Toolbar toolbar;
    Executor executor;
    BiometricPrompt biometricPrompt;
    BiometricPrompt.PromptInfo promptInfo;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    Switch swFingerPrint, swDevicePinLock, swOwnSetPinLock;
    boolean isFinger = true;
    SharedPreferences preferences;
    AlertDialog alertDialog;
    boolean isChangePass = false;
    private ProgressDialog progress;
    public final static String SHAREDPREFERENCE_NAME = "Yeasin";
    static String IS_FINGERPRINT = "IS_FINGERPRINT";
    static  String IS_DEVICE_PIN = "IS_DEVICE_PIN";
    static String IS_OWN_SET_PIN = "IS_OWN_SET_PIN";
    static  String OWN_SET_PIN = "OWN_SET_PIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = this.getSharedPreferences(SHAREDPREFERENCE_NAME, this.MODE_PRIVATE);
          setTitle("Setting");
        swFingerPrint = findViewById(R.id.sw_finger_print);
        swDevicePinLock = findViewById(R.id.sw_device_pin_lock);
        swOwnSetPinLock = findViewById(R.id.sw_own_set_pin_lock);
        boolean isSaveFinger = preferences.getBoolean(IS_FINGERPRINT, false);
        boolean isDevicePin = preferences.getBoolean(IS_DEVICE_PIN, false);
        boolean isOwnSetPin = preferences.getBoolean(IS_OWN_SET_PIN, false);
        if (isOwnSetPin) {
            swOwnSetPinLock.setChecked(true);
        } else {
            swOwnSetPinLock.setChecked(false);
        }
        if (isDevicePin) {
            swDevicePinLock.setChecked(true);
        } else {
            swDevicePinLock.setChecked(false);
        }
        if (isSaveFinger) {
            swFingerPrint.setChecked(true);
        } else {
            swFingerPrint.setChecked(false);
        }


        swFingerPrint.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                swFingerPrint.setClickable(false);
                if (checkBiometric(MainActivity.this)) {
                    conFromBiometricAuth();
                }
                return false;
            }
        });

        swDevicePinLock.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        ShowConfirmLockPINActivity();
                        break;
                    }
                }
                return true;
            }
        });


        swOwnSetPinLock.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        dialogOwnPin();
                        break;
                    }
                }
                return true;
            }
        });

    }

    public void dialogOwnPin() {
        SharedPreferences.Editor editor = preferences.edit();
        boolean isOwnSetPin = preferences.getBoolean(IS_OWN_SET_PIN, false);
        String oldPassword = preferences.getString(OWN_SET_PIN, "");
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialog_set_own_password, viewGroup, false);
        builder.setView(dialogView);
        showSoftKeyboard();
        alertDialog = builder.create();
        alertDialog.setCancelable(false);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        EditText etOldPassword = dialogView.findViewById(R.id.et_old_password);

        AppCompatButton btnSave = dialogView.findViewById(R.id.btn_save);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        CheckBox changeCheck = dialogView.findViewById(R.id.item_check);
        changeCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    etOldPassword.setVisibility(View.VISIBLE);
                    etPassword.setHint("New PIN");
                    isChangePass = true;
                } else {
                    isChangePass = false;
                    etOldPassword.setVisibility(View.GONE);
                    etPassword.setHint("Conform PIN");
                }
            }
        });
        if (!oldPassword.equalsIgnoreCase("")) {
            etPassword.setHint("Conform PIN");
            if (isOwnSetPin) {
                btnSave.setText("Disable");
                btnSave.setBackgroundColor(MainActivity.this.getResources().getColor(R.color.red));
            } else {
                btnSave.setText("Enable");
                btnSave.setBackgroundColor(MainActivity.this.getResources().getColor(R.color.blue));
            }
        } else {
            etPassword.setHint("Create new PIN");
        }


        editor.commit();
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChangePass) {
                    //change password
                    if (!etOldPassword.getText().toString().trim().equals("")) {
                        String oldPass = etOldPassword.getText().toString().trim();
                        if (oldPass.equalsIgnoreCase(oldPassword)) {
                            if (!etPassword.getText().toString().trim().equalsIgnoreCase("")) {
                                boolean isOwnSetPin = preferences.getBoolean(IS_OWN_SET_PIN, false);
                                String newPass = etPassword.getText().toString().trim();
                                editor.putString(OWN_SET_PIN, "" + newPass);
                                if (isOwnSetPin) {
                                    editor.putBoolean(IS_OWN_SET_PIN, false);
                                    editor.putString(OWN_SET_PIN, newPass);
                                    swOwnSetPinLock.setChecked(false);
                                } else {
                                    editor.putBoolean(IS_OWN_SET_PIN, true);
                                    editor.putString(OWN_SET_PIN, newPass);
                                    swOwnSetPinLock.setChecked(true);
                                }

                                editor.commit();
                                alertDialog.dismiss();
                                isChangePass = false;
                                hidenSoftKeyboard();
                            } else {
                                etPassword.setError("Please set new PIN");
                            }

                        } else {
                            etOldPassword.setError("Please set validate PIN");
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Please set your old PIN", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    //check  password
                    if (!oldPassword.equalsIgnoreCase("")) {
                        if (!etPassword.getText().toString().trim().equals("")) {
                            String conformPass = etPassword.getText().toString().trim();
                            if (conformPass.equalsIgnoreCase(oldPassword)) {
                                boolean checkPin = preferences.getBoolean(IS_OWN_SET_PIN, false);
                                if (checkPin) {
                                    swOwnSetPinLock.setChecked(false);
                                    editor.putBoolean(IS_OWN_SET_PIN, false);
                                    editor.commit();
                                } else {
                                    swOwnSetPinLock.setChecked(true);
                                    editor.putBoolean(IS_OWN_SET_PIN, true);
                                    editor.commit();
                                }
                                hidenSoftKeyboard();


                                alertDialog.dismiss();

                            } else {
                                etPassword.setError("Please set validate PIN");
                            }
                        } else {
                            etPassword.setError("Please Conform PIN");
                        }

                    } else {
                        //new  password
                        if (!etPassword.getText().toString().trim().equals("")) {
                            hidenSoftKeyboard();
                            editor.putString(OWN_SET_PIN, "" + etPassword.getText().toString().trim());
                            editor.putBoolean(IS_OWN_SET_PIN, true);
                            editor.commit();
                            swOwnSetPinLock.setChecked(true);
                            alertDialog.dismiss();
                        } else {
                            Toast.makeText(MainActivity.this, "Please set your PIN", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                hidenSoftKeyboard();
            }
        });
        alertDialog.show();
    }

    public void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void hidenSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void conFromBiometricAuth() {

        SharedPreferences.Editor editor = preferences.edit();
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                boolean isSaveFingerCheck = preferences.getBoolean(IS_FINGERPRINT, false);
                if (isSaveFingerCheck) {
                    editor.putBoolean(IS_FINGERPRINT, false);
                    editor.commit();
                    swFingerPrint.setChecked(false);
                } else {
                    editor.putBoolean(IS_FINGERPRINT, true);
                    editor.commit();
                    swFingerPrint.setChecked(true);
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                swFingerPrint.setChecked(false);
                Toast.makeText(MainActivity.this, "FAILED", Toast.LENGTH_LONG).show();
            }
        });


        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm fingerprint")
                .setDescription("Touch the touch id sensor")
                .setNegativeButtonText("Cancel")
                .build();
//         .setNegativeButton(activity.getResources().getString(R.string.biometric_dialog_use_password),
//                activity.getMainExecutor(), new DialogInterface.OnClickListener() {
//                    @Override
//                            .build();


        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                SharedPreferences.Editor editor = preferences.edit();
                boolean isDevicePins = preferences.getBoolean(IS_DEVICE_PIN, false);
                if (isDevicePins) {
                    swDevicePinLock.setChecked(false);
                    editor.putBoolean(IS_DEVICE_PIN, false);
                } else {
                    editor.putBoolean(IS_DEVICE_PIN, true);
                    swDevicePinLock.setChecked(true);
                }
                editor.commit();
            } else {
                Toast.makeText(this, "Password not match", Toast.LENGTH_SHORT).show();

            }
        }
    }

    void ShowConfirmLockPINActivity() {
        Intent intent = new Intent(Intent.ACTION_RUN);
        intent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.ConfirmLockPassword"));
        startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
    }

    public static boolean checkBiometric(Context ctx) {
        BiometricManager biometricManager = BiometricManager.from(ctx);
        boolean isBiometric = false;
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                isBiometric = true;
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(ctx, "Device don't have a Fingerprint..", Toast.LENGTH_SHORT).show();
                isBiometric = false;
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(ctx, "Biometric sensor is currently unavailable...", Toast.LENGTH_SHORT).show();
                isBiometric = false;
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(ctx, "Device have don't Fingerprint saved,please check yout setting..", Toast.LENGTH_SHORT).show();
                isBiometric = false;
                break;
        }
        return isBiometric;
    }
}