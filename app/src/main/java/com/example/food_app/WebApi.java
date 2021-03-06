package com.example.food_app;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Provides methods for contacting the server. Used starting in Checkpoint 2.
 * <p>
 * STOP! Do not modify this file. Changes will be overwritten during official grading.
 */
public final class WebApi {

    /** Tag for logged messages. */
    private static final String TAG = "WebApi";

    /** The URL at which the server is hosted. */
    public static final String API_BASE = "https://developers.zomato.com/api/v2.1";

    /** The URL at which the webserver socket is hosted. */
    public static final String WEBSOCKET_BASE = "wss://cs125-cloud.cs.illinois.edu/Fall2019-MP";

    /** The HTTP status code for Bad Request. */
    private static final int HTTP_BAD_REQUEST = 400;

    /** Timeout (milliseconds) for connecting to a game websocket. */
    private static final int WEBSOCKET_CONNECTION_TIMEOUT = 5000;

    /** Interval (milliseconds) at which to make sure the websocket is still connected. */
    private static final int WEBSOCKET_PING_INTERVAL = 60000;

    /** The Volley request queue for the application, or null if the queue hasn't been set up yet. */
    private static RequestQueue requestQueue;

    /** Private constructor to prevent creating instances. */
    private WebApi() { }

    /**
     * Starts an HTTP GET request.
     * @param context an Android context
     * @param url the URL to contact
     * @param listener callback to run with response data
     * @param errorListener callback to run if an error occurs
     */
    public static void startRequest(final Context context, final String url,
                                    final Response.Listener<JsonObject> listener, final Response.ErrorListener errorListener) {
        startRequest(context, url, Request.Method.GET, null, listener, errorListener);
    }

    /**
     * Starts a network request with a JSON object as the payload.
     * @param context an Android context
     * @param url the URL to contact
     * @param method the HTTP method (GET or POST)
     * @param body the JSON object to include in the body
     * @param listener callback to run with response data
     * @param errorListener callback to run if an error occurs
     */
    public static void startRequest(final Context context, final String url, final int method, final JsonElement body,
                                    final Response.Listener<JsonObject> listener, final Response.ErrorListener errorListener) {
        if (requestQueue == null) {
            Log.v(TAG, "Creating request queue");
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        Response.Listener<String> serverResponseListener = stringResponse -> {
            if (stringResponse == null || stringResponse.isEmpty()) {
                Log.v(TAG, "Delivering empty response from " + url);
                listener.onResponse(null);
            } else {
                Log.v(TAG, "Delivering parsed response from " + url);
                listener.onResponse(JsonParser.parseString(stringResponse).getAsJsonObject());
            }
        };
        Response.ErrorListener serverErrorListener = error -> {
            if (error.networkResponse != null && error.networkResponse.data != null
                    && error.networkResponse.statusCode == HTTP_BAD_REQUEST) {
                String responseData = new String(error.networkResponse.data);
                try {
                    JsonObject errObject = JsonParser.parseString(responseData).getAsJsonObject();
                    Log.v(TAG, "Delivering application-level error from " + url);
                    errorListener.onErrorResponse(new VolleyError(errObject.get("error").getAsString()));
                } catch (Exception e) {
                    Log.e(TAG, "Delivering 400 error from " + url);
                    errorListener.onErrorResponse(error);
                }
            } else {
                Log.v(TAG, "Delivering Volley error response from " + url);
                errorListener.onErrorResponse(error);
            }
        };
        requestQueue.add(new StringRequest(method, url, serverResponseListener, serverErrorListener) {
            {
                Log.v(TAG, "startRequest creating Volley request (received Firebase ID token)");
            }
            @Override
            public byte[] getBody() throws AuthFailureError {
                if (body == null) {
                    return super.getBody();
                } else {
                    return body.toString().getBytes();
                }
            }
            @Override
            public String getBodyContentType() {
                if (body == null) {
                    return super.getBodyContentType();
                } else {
                    return "application/json";
                }
            }
            @Override
            public Map<String, String> getHeaders() {
                return Collections.singletonMap("user-key", "3ee2fd24bf2ae7079e57350abb9c2643");
                //return Collections.singletonMap("user-key", "3e84faad91d942fb24f09347117932db");
            }
        });
    }

    /**
     * Connects to a websocket.
     * @param url the websocket endpoint
     * @param dataListener receiver for data messages
     * @param onCreatedListener callback to run with the websocket when it is created
     * @param connectionLostListener callback to run if the connection is lost
     * @param errorListener callback to run if an error occurs during the initial connection
     */
    public static void connectWebSocket(final String url, final Consumer<JsonObject> dataListener,
                                        final Consumer<WebSocket> onCreatedListener,
                                        final Runnable connectionLostListener,
                                        final Consumer<Throwable> errorListener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "connectWebSocket called before Firebase Authentication login");
            throw new IllegalStateException("No user is logged in");
        }
        user.getIdToken(false).addOnSuccessListener(result -> {
            Log.v(TAG, "connectWebSocket received Firebase ID token");
            WebSocketFactory factory = new WebSocketFactory();
            factory.setConnectionTimeout(WEBSOCKET_CONNECTION_TIMEOUT);
            try {
                WebSocket socket = factory.createSocket(url);
                socket.setPingInterval(WEBSOCKET_PING_INTERVAL);
                socket.addHeader("Firebase-Token", result.getToken());
                socket.addListener(new WebSocketAdapter() {
                    private boolean disconnectedDueToError = false;
                    @Override
                    public void onTextMessage(final WebSocket websocket, final String text) {
                        dataListener.accept(JsonParser.parseString(text).getAsJsonObject());
                    }
                    @Override
                    public void onError(final WebSocket websocket, final WebSocketException cause) {
                        disconnectedDueToError = true;
                    }
                    @Override
                    public void onDisconnected(final WebSocket websocket, final WebSocketFrame serverCloseFrame,
                                               final WebSocketFrame clientCloseFrame, final boolean closedByServer) {
                        if (disconnectedDueToError) {
                            Log.v(TAG, "Delivering connection-lost error for " + url);
                            connectionLostListener.run();
                        } else {
                            Log.v(TAG, "Websocket closed to " + url);
                        }
                    }
                    @Override
                    public void onConnectError(final WebSocket websocket, final WebSocketException exception) {
                        disconnectedDueToError = false;
                        Log.v(TAG, "Delivering websocket connection error for " + url);
                        errorListener.accept(exception);
                    }
                });
                socket.connectAsynchronously();
                Log.v(TAG, "Delivering websocket instance for " + url);
                onCreatedListener.accept(socket);
            } catch (IOException e) {
                Log.v(TAG, "Delivering websocket setup error for " + url);
                errorListener.accept(e);
            }
        }).addOnFailureListener(errorListener::accept);
        Log.v(TAG, "connectWebSocket started getIdToken");
    }

}
