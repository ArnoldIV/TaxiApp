package com.arnoldiii.taxiapp;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DriverSignInActivity extends AppCompatActivity {

    private static final String TAG = "DriverSignInActivity";

    private TextInputLayout textInputEmail, textInputPassword, textInputConfirmPassword,
            textInputName;

    private Button loginSignUpButton;
    private TextView toggleLoginSignUpTextView;

    private boolean isloginModeActive;

    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_sign_in);

        //initialization of firebase auth
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() !=null){
            startActivity(new Intent(DriverSignInActivity.this,
                    DriverMapsActivity.class));
        }

        textInputEmail = findViewById(R.id.textInputEmail);
        textInputPassword = findViewById(R.id.textInputPassword);
        textInputConfirmPassword = findViewById(R.id.textInputConfirmPassword);
        textInputName = findViewById(R.id.textInputName);

        loginSignUpButton = findViewById(R.id.loginSignUpButton);
        toggleLoginSignUpTextView = findViewById(R.id.toggleLoginSignUpTextView);


    }
    //email validation and error output when the field is empty
    private boolean validateEmail() {
        String emailInput = textInputEmail.getEditText().getText().toString().trim();
        if (emailInput.isEmpty()) {
            textInputEmail.setError("Please,input your email");
            return false;
        } else {
            textInputEmail.setError("");
            return true;
        }
    }
    //name validation and error output when the field is empty,max length of name is 15
    private boolean validateName() {
        String nameInput = textInputName.getEditText().getText().toString().trim();
        if (nameInput.isEmpty()) {
            textInputName.setError("Please,input your name");
            return false;
        } else if (nameInput.length() > 15) {
            textInputName.setError("Name length have to be less then 15");
            return false;
        } else {
            textInputName.setError("");
            return true;
        }
    }
    //password validation and error output when the field is empty,min length of password is 5
    private boolean validatePassword() {
        String passwordInput = textInputPassword.getEditText().getText().toString().trim();

        if (passwordInput.isEmpty()) {
            textInputPassword.setError("Please,input your password");
            return false;
        } else if (passwordInput.length() < 5) {
            textInputPassword.setError("Password length have to be more then 5");
            return false;
        } else {
            textInputPassword.setError("");
            return true;
        }
    }

    //password confirm validation and error output when the field doesn't match
    private boolean validateConfirmPassword() {
        String passwordInput = textInputPassword.getEditText().getText().toString().trim();
        String confirmPasswordInput = textInputConfirmPassword.getEditText().getText().toString().trim();
        if (!passwordInput.equals(confirmPasswordInput)) {
            textInputPassword.setError("Passwords have to match");
            return false;
        } else {
            textInputPassword.setError("");
            return true;
        }
    }

    public void loginSingUpUser(View view) {
        //if the user does not pass the full validation for logging, then user will receive errors about this
        if (!validateEmail() | !validateName() | !validatePassword()) {
            return;
        }

        //log in for user
        if (isloginModeActive) {
            auth.signInWithEmailAndPassword(
                    //getting email and password for registration
                    textInputEmail.getEditText().getText().toString().trim(),
                    textInputPassword.getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                /* Sign in success, update UI with the signed-in user's information
                                and directing the user to the next activity*/
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = auth.getCurrentUser();
                                startActivity(new Intent(
                                        DriverSignInActivity.this, DriverMapsActivity.class
                                ));
                                //updateUI(user);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(DriverSignInActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                //updateUI(null);
                            }
                        }
                    });
        } else {
            //full validation before creating a new user
            if (!validateEmail() | !validateName() | !validatePassword() |
                    !validateConfirmPassword()) {
                return;
            }

            //registration of a new user
            auth.createUserWithEmailAndPassword(
                    textInputEmail.getEditText().getText().toString().trim(),
                    textInputPassword.getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete( Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                /* Sign in success, update UI with the signed-in user's information
                                and directing the user to the next activity*/
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = auth.getCurrentUser();
                                startActivity(new Intent(
                                        DriverSignInActivity.this, DriverMapsActivity.class
                                ));
                                // updateUI(user);

                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(DriverSignInActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                //updateUI(null);
                            }
                        }
                    });
        }


    }
    //change text in button and toggleTextView depending on registration or logging
    public void toggleLoginSignUp(View view) {
        if (isloginModeActive) {
            isloginModeActive = false;
            loginSignUpButton.setText("Sign up");
            toggleLoginSignUpTextView.setText("Or, log in");
            textInputConfirmPassword.setVisibility(View.VISIBLE);
        } else {
            isloginModeActive = true;
            loginSignUpButton.setText("Log in");
            toggleLoginSignUpTextView.setText("Or, sign up");
            textInputConfirmPassword.setVisibility(View.GONE);
        }
    }
}