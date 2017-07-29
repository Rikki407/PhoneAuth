package com.darklightning.phoneauth;

import android.content.Context;
import android.icu.util.TimeUnit;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class RegisterUserActivity extends AppCompatActivity implements View.OnClickListener
{

    EditText phoneNumText,otpText,emailIdText,userPassword;
    Button acceptPhoneButton,registerButton,resendOtpButton,verificationButton,recoveryMailButton;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mToken;
    FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener  mAuthListener;
    Context mContext;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.register_user_activity);
        phoneNumText = (EditText)findViewById(R.id.phone_number_edittext);
        otpText = (EditText)findViewById(R.id.otp_edittext);
        emailIdText = (EditText)findViewById(R.id.email_id);
        userPassword = (EditText) findViewById(R.id.user_password);

        acceptPhoneButton = (Button) findViewById(R.id.accept_phone_num_button);
        registerButton = (Button) findViewById(R.id.register_button);
        resendOtpButton = (Button) findViewById(R.id.resend_otp);
        verificationButton = (Button) findViewById(R.id.send_verification_mail);
        recoveryMailButton = (Button) findViewById(R.id.password_recovery_mail);

        verificationButton.setOnClickListener(this);
        recoveryMailButton.setOnClickListener(this);
        acceptPhoneButton.setOnClickListener(this);
        registerButton.setOnClickListener(this);
        resendOtpButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                if(e instanceof FirebaseAuthInvalidCredentialsException)
                {
                    phoneNumText.setError("Invalid phone number.");
                }
                else if(e instanceof FirebaseTooManyRequestsException)
                {
                    Toast.makeText(mContext,"Quota exceeded",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                mVerificationId = verificationId;
                mToken = forceResendingToken;
            }
        };

    //for email verification
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    sendVerificationEmail();
                } else
                    {
                }

            }
        };


    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();

    }





//sending otp
    public void startPhoneNumberVerification(String phoneNumber)
    {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,60, java.util.concurrent.TimeUnit.SECONDS,this,mCallBacks);
    }
    //sending otp again
    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                java.util.concurrent.TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallBacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }
    //verifying phone number
    private void verifyPhoneNumberWithCode(String verificationId,String code)
    {
        PhoneAuthCredential credential = new PhoneAuthCredential(verificationId,code);
        signInWithPhoneAuthCredential(credential);
    }


    private void signInWithPhoneAuthCredential(final PhoneAuthCredential phoneAuthCredential)
    {

        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful())
                {
                    FirebaseUser user = task.getResult().getUser();

                    //linking phone auth with email auth

                    linkAccount();

                }
                else
                {
                    if(task.getException() instanceof FirebaseAuthInvalidCredentialsException)
                    {
                        Toast.makeText(mContext,"Invalid Code",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void signOut()
    {
        mAuth.signOut();
        Toast.makeText(mContext,"User has Signed Out",Toast.LENGTH_LONG).show();
    }

    private void linkAccount()
    {




        String email = emailIdText.getText().toString();
        String password = userPassword.getText().toString();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            FirebaseUser user = task.getResult().getUser();

                        } else {

                        }


                    }
                });
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.accept_phone_num_button :
                if(phoneNumText.getText()==null)
                {
                    phoneNumText.setError("phone number field cant be empty");
                    return;
                }
                startPhoneNumberVerification(phoneNumText.getText().toString());

                break;
            case R.id.register_button :
                String code = otpText.getText().toString();
                verifyPhoneNumberWithCode(mVerificationId,code);
                Toast.makeText(mContext,"User has been registered",Toast.LENGTH_LONG).show();
                break;
            case R.id.resend_otp :
                resendVerificationCode(phoneNumText.getText().toString(),mToken);
                break;
            case R.id.send_verification_mail:
                sendVerificationEmail();
                break;
            case R.id.password_recovery_mail:
                sendPasswordChangeMail();
                break;
        }
    }

    private void sendPasswordChangeMail()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String emailAddress = emailIdText.getText().toString();

        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(mContext, "email sent to ur id", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void sendVerificationEmail()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(mContext, "email has been sent", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(mContext, "nope", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


}
