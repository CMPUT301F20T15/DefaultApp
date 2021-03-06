package com.example.bookwormadventuresdeluxe2.Activities;

/**
 * Activity for login. Requires valid account input. Opens MyBooksActivity
 * on successful login and redirects to MyBooksActivity automatically
 * if login session is still valid.
 */

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookwormadventuresdeluxe2.R;
import com.example.bookwormadventuresdeluxe2.Utilities.NotificationUtility.NotificationHandler;
import com.example.bookwormadventuresdeluxe2.Utilities.EditTextValidator;
import com.example.bookwormadventuresdeluxe2.Utilities.UserCredentialAPI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final String TAG = "LoginActivity";
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button loginButton;
    private Button createAccountButton;
    private ImageButton visibilityButton;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        TextView appHeaderTitle = findViewById(R.id.app_header_title);
        appHeaderTitle.setText("Bookworm Adventures Deluxe 2");

        collectionReference = db.collection(getString(R.string.users_collection));
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                currentUser = firebaseAuth.getCurrentUser();

                /* If User logged in redirect directly to MyBooks */
                if (currentUser != null)
                {
                    String currentUserId = currentUser.getUid();

                    /* Get Current User information from firestore */
                    collectionReference
                            .whereEqualTo(getString(R.string.firestore_userId), currentUserId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task)
                                {
                                    for (QueryDocumentSnapshot snapshot : task.getResult())
                                    {
                                        UserCredentialAPI userCredentialAPI = UserCredentialAPI.getInstance();
                                        userCredentialAPI.setUserId(snapshot.getString(getString(R.string.firestore_userId)));
                                        userCredentialAPI.setUsername(snapshot.getString(getString(R.string.firestore_username)));
                                        userCredentialAPI.setNotificationCount((Long) snapshot.get(getString(R.string.firestore_user_notification_count_field)));
                                        /* Update FCM token if invalid */
                                        NotificationHandler.updateFCMToken(snapshot.getString("token"), snapshot.getId());

                                        Intent myBooksIntent = new Intent(LoginActivity.this, MyBooksActivity.class);
                                        startActivity(myBooksIntent);

                                        /* Removes activity from stack so user not brought back here with back  */
                                        finish();
                                        break; // safety measure
                                    }
                                }
                            });
                }
            }
        };

        progressBar = findViewById(R.id.login_progressBar);
        editTextEmail = (EditText) findViewById(R.id.login_email);
        editTextPassword = (EditText) findViewById(R.id.login_password);
        loginButton = (Button) findViewById(R.id.login_button);
        createAccountButton = (Button) findViewById(R.id.create_account_button);
        visibilityButton = (ImageButton) findViewById(R.id.visibility_button);

        /* Set the password to hidden by default */
        editTextPassword.setTransformationMethod(new PasswordTransformationMethod());

        loginButton.setOnClickListener(this);

        createAccountButton.setOnClickListener(this);

        visibilityButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                /* Toggle back and forth between visible and hidden password */

                /* Get the cursor start and end index so we can restore the cursor position later */
                int cursorStart = editTextPassword.getSelectionStart();
                int cursorEnd = editTextPassword.getSelectionEnd();

                /* https://stackoverflow.com/questions/24106904/get-drawable-of-image-button */
                if (visibilityButton.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.ic_visibility_24px).getConstantState()))
                {
                    /* Image is currently set to password visible, so set it to hidden */
                    visibilityButton.setImageResource(R.drawable.ic_visibility_off_24px);
                    /* https://stackoverflow.com/questions/3685790/how-to-switch-between-hide-and-view-password */
                    /* Hide the text as well */
                    editTextPassword.setTransformationMethod(new PasswordTransformationMethod());
                }
                else
                {
                    /* Image is currently set to password hidden, so set it to visible */
                    visibilityButton.setImageResource(R.drawable.ic_visibility_24px);
                    /* https://stackoverflow.com/questions/3685790/how-to-switch-between-hide-and-view-password */
                    /* Show the text again */
                    editTextPassword.setTransformationMethod(null);
                }

                /* Restore cursor position after hiding/showing the password */
                editTextPassword.setSelection(cursorStart, cursorEnd);
            }
        });
    }

    /**
     * Set Current User and attach Auth state listener on Start of Activity
     */
    @Override
    protected void onStart()
    {
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
        super.onStart();
    }

    /**
     * Detach Auth State Listener on idle Activity
     */
    @Override
    protected void onPause()
    {
        if (firebaseAuth != null)
        {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        super.onPause();
    }

    /**
     * Handle click on Login and SignOut button
     *
     * @param view View containing layout resources
     */
    @Override
    public void onClick(View view)
    {
        try
        {
            switch (view.getId())
            {
                case R.id.login_button:
                    loginUser(editTextEmail.getText().toString().trim(),
                            editTextPassword.getText().toString().trim());
                    break;
                case R.id.create_account_button:
                    /* Override to open Create Account Activity */
                    Intent myBooksIntent = new Intent(LoginActivity.this, CreateAccountActivity.class);
                    LoginActivity.this.startActivity(myBooksIntent);
                    break;
                default:
                    /* Unexpected resource id*/
                    throw new Exception("Unexpected resource Id inside click listener."
                            + "Expected: R.id.login_button Or R.id.create_account_button");
            }
        } catch (Exception e)
        {
            /* Log message to debug*/
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * Attempt to login User
     * Take to MyBooksActivity on success
     * Show error message on failure
     *
     * @param email    Email of the user in the format joe@sample.com
     * @param password Password of the user
     */
    private void loginUser(String email, String password)
    {
        editTextEmail.setError(null);
        editTextPassword.setError(null);
        progressBar.setVisibility(View.VISIBLE);

        /* Check email and password parameters length*/
        if (!TextUtils.isEmpty(email)
                && !TextUtils.isEmpty(password))
        {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task)
                        {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (task.isSuccessful())
                            {
                                String currentUserId = user.getUid();

                                /* Get User information from firestore and take to MyBooksActivity */
                                collectionReference
                                        .whereEqualTo(getString(R.string.firestore_userId), currentUserId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>()
                                        {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task)
                                            {
                                                for (QueryDocumentSnapshot snapshot : task.getResult())
                                                {
                                                    UserCredentialAPI userCredentialAPI = UserCredentialAPI.getInstance();
                                                    userCredentialAPI.setUserId(snapshot.getString(getString(R.string.firestore_userId)));
                                                    userCredentialAPI.setUsername(snapshot.getString(getString(R.string.firestore_username)));
                                                    userCredentialAPI.setNotificationCount((Long) snapshot.get(getString(R.string.firestore_user_notification_count_field)));
                                                    /* Update FCM token if invalid */
                                                    NotificationHandler.updateFCMToken(snapshot.getString("token"), snapshot.getId());
                                                    Intent myBooksIntent = new Intent(LoginActivity.this, MyBooksActivity.class);
                                                    startActivity(myBooksIntent);

                                                    /* Removes activity from stack so user not brought back here with back  */
                                                    finish();
                                                    break; // safety measure
                                                }
                                            }
                                        });
                            }
                            else
                            {
                                /* Set EditText Error type from errorCode */
                                try
                                {
                                    /* Extract Firebase Error Code */
                                    String errorCode = "";
                                    errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();

                                    switch (errorCode)
                                    {
                                        case "ERROR_WRONG_PASSWORD":
                                            EditTextValidator.wrongPassword(editTextPassword);
                                            break;

                                        case "ERROR_INVALID_EMAIL":
                                            /* Merged with next case as both set error to editTextEmail*/
                                        case "ERROR_USER_NOT_FOUND":
                                            EditTextValidator.emailNotFound(editTextEmail);
                                            break;

                                        default:
                                            /* Unexpected Error code*/
                                            editTextEmail.setError(task.getException().getMessage());
                                            editTextEmail.requestFocus();
                                    }
                                } catch (Exception e)
                                {
                                    /* Different type from errorCode, cannot be cast to the same object.
                                     * Sets EditText error to new type.
                                     *
                                     * Log message to debug
                                     */
                                    editTextEmail.setError(task.getException().getMessage());
                                    editTextEmail.requestFocus();
                                    Log.d(TAG, e.getMessage());
                                }

                                /* Hide progress bar*/
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
        }
        else
        {
            /* Hide progress bar*/
            progressBar.setVisibility(View.INVISIBLE);

            /* Set Password Edit Text error */
            if (TextUtils.isEmpty(password))
            {
                EditTextValidator.isEmpty(editTextPassword);
            }

            /* Set Email Edit Text error */
            if (TextUtils.isEmpty(email))
            {
                EditTextValidator.isEmpty(editTextEmail);
            }
            return;
        }
    }
}
