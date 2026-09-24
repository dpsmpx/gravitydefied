package org.happysanta.gd.API;

import android.os.AsyncTask;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.happysanta.gd.Helpers.getAppVersion;
import static org.happysanta.gd.Helpers.logDebug;

public class Request {

    private final List<Pair<String, String>> params;
    private final ResponseHandler handler;
    private AsyncRequestTask task;
    private final String apiURL;

    public Request(String method, List<Pair<String, String>> params, ResponseHandler handler, boolean useDebugURL) {
        this(method, params, handler, useDebugURL ? API.DEBUG_URL : API.URL);
    }

    public Request(String method, List<Pair<String, String>> params, ResponseHandler handler) {
        this(method, params, handler, API.URL);
    }

    private Request(String method, List<Pair<String, String>> params, ResponseHandler handler, String apiURL) {
        this.apiURL = apiURL;

        params.add(new Pair<String, String>("v", String.valueOf(API.VERSION)));
        params.add(new Pair<String, String>("method", method));
        params.add(new Pair<String, String>("app_version", getAppVersion()));
        params.add(new Pair<String, String>("app_lang", Locale.getDefault().getDisplayLanguage()));

        this.params = params;
        this.handler = handler;
        go();
    }

    private void go() {
        task = new AsyncRequestTask();
        task.execute(apiURL);
    }

    public void cancel() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    private void onDone(String result) {
        logDebug("API.Request.onDone()");

        Response response;
        try {
            response = new Response(result);
        } catch (APIException e) {
            handler.onError(e);
            return;
        } catch (Exception e) {
            handler.onError(new APIException(result == null ? "Network error" : "JSON parsing error"));
            return;
        }

        if (response != null) {
            handler.onResponse(response);
        } else {
            handler.onError(new APIException("JSON parsing error"));
        }
    }

    protected class AsyncRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... objects) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(objects[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                );

                StringBuilder encoded = new StringBuilder();
                for (Pair<String, String> pair : params) {
                    if (encoded.length() > 0) {
                        encoded.append('&');
                    }
                    encoded.append(URLEncoder.encode(
                            pair.first == null ? "" : pair.first,
                            "UTF-8"
                    ));
                    encoded.append('=');
                    encoded.append(URLEncoder.encode(
                            pair.second == null ? "" : pair.second,
                            "UTF-8"
                    ));
                }

                byte[] body = encoded.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }

                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 400
                        ? connection.getErrorStream()
                        : connection.getInputStream();

                if (stream == null) {
                    return null;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (isCancelled()) {
                            return null;
                        }
                        sb.append(line).append('\n');
                    }
                }

                if (responseCode < 200 || responseCode >= 300) {
                    logDebug("API request returned HTTP " + responseCode);
                    return null;
                }

                return sb.toString();
            } catch (Exception e) {
                logDebug("API request failed: " + e.getMessage());
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!isCancelled()) {
                onDone(result);
            }
        }
    }
}
