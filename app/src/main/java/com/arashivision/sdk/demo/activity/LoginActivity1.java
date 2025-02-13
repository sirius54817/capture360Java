package com.arashivision.sdk.demo.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.arashivision.sdk.demo.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity1 extends AppCompatActivity {

    private static final String TAG = "LoginActivity1";

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity1);

        editTextUsername = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUsername.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                if (!username.isEmpty() && !password.isEmpty()) {
                    new LoginTask().execute(username, password);
                } else {
                    Toast.makeText(LoginActivity1.this, "Username and password are required", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            String urlString = "https://fd84-59-97-51-97.ngrok-free.app/building/login/";
            String result = "";

            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");

                // Set parameters for POST request
                String postData = "username=" + username + "&password=" + password;
                urlConnection.setDoOutput(true);
                OutputStream outputStream = urlConnection.getOutputStream();
                outputStream.write(postData.getBytes());
                outputStream.flush();
                outputStream.close();

                // Read response
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                result = stringBuilder.toString();
            } catch (IOException e) {
                Log.e(TAG, "Login Error: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (!result.isEmpty()) {
                Log.d(TAG, "Login Response: " + result);

                try {
                    // Assuming the response is a JSON object with fields "user_id" (as a number)
                    JSONObject jsonResponse = new JSONObject(result);
                    int userId = jsonResponse.getInt("user_id");  // Get user_id as integer


                    // Show the dialog with user id
                    String message = "User ID: " + userId ;
                    showDialog("Login Successful!", message);
                    Log.d(TAG,"Login Success : User ID: " + userId);

                    // Pass the user_id to the next activity (IntroPageActivity)
                    navigateToHomeActivity(userId);

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                    showDialog("Error", "Failed to parse login response.");
                }

            } else {
                // Handle login failure
                showDialog("Login Failed", "Invalid username or password");
            }
        }
    }

    private void navigateToHomeActivity(int userId) {
        // Create an intent to navigate to the next activity
        Intent intent = new Intent(LoginActivity1.this, IntroPageActivity.class);
        intent.putExtra("user_id", userId);  // Pass user_id as an integer
        startActivity(intent);
        finish(); // Finish LoginActivity to prevent returning back to it with back button
    }

    private void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity1.this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
