package com.henry.iwagenda;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Map;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    private UserResources ur = new UserResources(this);
    protected static Map<String, String> cookiejar;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUserView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private View mNoInternetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Custom color
        paintUI();

        // Set up the login form.
        mUserView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mIWSignInButton = (Button) findViewById(R.id.iw_sign_in_button);
        mIWSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mNoInternetView = findViewById(R.id.no_internet);

        Button mNoInternetRetryButton = (Button) findViewById(R.id.no_internet_retry);
        mNoInternetRetryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showNoInternet(false);
                tryGetSavedLogin();
            }
        });

        // Check internet connection
        if (ur.isConnectedToInternet()) {
            tryGetSavedLogin();
            // startActivity(new Intent(getBaseContext(),MainActivity.class));
            // finish();
        } else {
            // showNoInternet(true);
            startActivity(new Intent(getBaseContext(), MainActivity.class));
            finish();
        }

        // tryGetSavedLogin();
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUserView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUserView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(username)) {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }

    private void tryGetSavedLogin() {
        SharedPreferences sharedPref = LoginActivity.this.getSharedPreferences("auth", Context.MODE_PRIVATE);

        if (sharedPref.contains("username") && sharedPref.contains("password")) {
            String username = sharedPref.getString("username", null);
            String password = sharedPref.getString("password", null);
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showNoInternet(boolean show) {
        mNoInternetView.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUser;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUser = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Connection.Response iwLoginResp;
            Map<String, String> iwCookies;
            String securityTokenKey;
            String securityTokenValue;

            try {
                Connection.Response iwRes = Jsoup.connect(iwAPI.loginURL)
                        .userAgent(iwAPI.userAgent)
                        .method(Connection.Method.GET)
                        .execute();
                Document loginPage = iwRes.parse();
                iwCookies = iwRes.cookies();
                Element csrftoken = loginPage.getElementById("users_login_csrftoken");
                securityTokenKey = csrftoken.attr("name");
                securityTokenValue = csrftoken.attr("value");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            try {
                iwLoginResp = Jsoup.connect(iwAPI.loginURL)
                        .data(securityTokenKey, securityTokenValue)
                        .data("authentication_method[modname]", "Users")
                        .data("authentication_method[method]", "uname")
                        .data("returnpage", "")
                        .data("event_type", "login_screen")
                        .data("rememberme", "1")
                        .data("authentication_info[login_id]", mUser)
                        .data("authentication_info[pass]", mPassword)
                        .cookies(iwCookies)
                        .userAgent(iwAPI.userAgent)
                        .followRedirects(true)
                        .method(Connection.Method.POST).execute();
                iwCookies = iwLoginResp.cookies();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            try {
                if (iwLoginResp.parse().text().contains("Agendes")) {
                    SharedPreferences sharedPref = getSharedPreferences("auth", Context.MODE_PRIVATE);
                    SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
                    cookiejar = iwCookies;
                    sharedPrefEdit.putString("username", mUser);
                    sharedPrefEdit.putString("password", mPassword);
                    sharedPrefEdit.commit();
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            // showProgress(false);

            if (success == null) {
                showNoInternet(true);
                showProgress(false);
                return;
            }

            if (success) {
                startActivity(new Intent(getBaseContext(), MainActivity.class));
                finish();
            } else {
                showProgress(false);
                mUserView.setText(mUser);
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void paintUI() {
        tintUI();
    }

    private void tintUI() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setBackgroundDrawable(new ColorDrawable(ur.getColorPrimary()));
        }

        MaterialProgressBar progressBar = (MaterialProgressBar) findViewById(R.id.login_progress);
        progressBar.setProgressTintList(ColorStateList.valueOf(ur.getColorAccent()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ur.getColorPrimaryDark());
            // window.setNavigationBarColor(ur.getColorPrimaryDark());
        }
    }
}
