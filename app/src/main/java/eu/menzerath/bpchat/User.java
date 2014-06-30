package eu.menzerath.bpchat;

import android.content.SharedPreferences;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import eu.menzerath.bpchat.chat.ChatMessage;
import eu.menzerath.bpchat.chat.Helper;

/**
 * Ein Nutzer, der mit der API interagieren kann
 * TODO: Klasse aufräumen!
 */
public class User {
    // TODO: HTTPS verwenden!!!
    private static final String LOGIN_URL = "http://chat.blackphantom.de/api/authentication.php";
    private static final String SEND_MESSAGE_URL = "http://chat.blackphantom.de/api/sendMessage.php";
    private static final String GET_MESSAGES_URL = "http://chat.blackphantom.de/api/loadLastMessages.php";

    private final SharedPreferences prefs;
    public final String username;
    public final String password;
    private String lastError;
    private HttpContext localContext;

    /**
     * Konstruktor des Nutzers
     *
     * @param username Nutzername des Users
     * @param password Passwort des Users
     * @param prefs    Kontext der Activity, damit auf die Einstellungen zugegriffen werden kann
     */
    public User(String username, String password, SharedPreferences prefs) {
        this.username = username;
        this.password = password;
        this.prefs = prefs;

        // Cookies müssen bei jedem Request erhalten beleiben, damit der User eingeloggt bleibt
        CookieStore cookieStore = new BasicCookieStore();
        localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    /**
     * Meldet den User beim Server an
     *
     * @return Ob der Login-Versuch erfolgreich war
     */
    public boolean login() {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(LOGIN_URL);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("username", this.username));
            nameValuePairs.add(new BasicNameValuePair("password", this.password));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            HttpResponse response = httpclient.execute(httppost, localContext);

            HttpEntity entity = response.getEntity();
            String jsonResponse = EntityUtils.toString(entity, "UTF-8");

            JSONObject json = new JSONObject(jsonResponse);

            if (json.getBoolean("success")) {
                return true;
            } else {
                JSONArray errors = json.getJSONArray("errors");
                lastError = "";

                for (int i = 0; i < errors.length(); i++) {
                    lastError += errors.getString(i);
                }
                return false;
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }

    /**
     * Sendet eine Nachricht an den Server
     *
     * @param message Nachricht
     * @return Ob der Sendevorgang erfolgreich war
     */
    public int sendMessage(String message) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(SEND_MESSAGE_URL);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("body", message.trim()));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            HttpResponse response = httpclient.execute(httppost, localContext);

            HttpEntity entity = response.getEntity();
            String jsonResponse = EntityUtils.toString(entity, "UTF-8");

            JSONObject json = new JSONObject(jsonResponse);

            if (json.getBoolean("success")) {
                return Integer.parseInt(json.getString("payload"));
            } else {
                JSONArray errors = json.getJSONArray("errors");
                lastError = "";

                for (int i = 0; i < errors.length(); i++) {
                    lastError += errors.getString(i);
                }
                return 0;
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            return 0;
        }
    }

    /**
     * Ruft die letzten Nachrichten vom Server ab
     *
     * @return Die 100 letzten Nachrichten
     */
    public ArrayList<ChatMessage> getMessages() {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(GET_MESSAGES_URL);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("limit", "100"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            HttpResponse response = httpclient.execute(httppost, localContext);

            HttpEntity entity = response.getEntity();
            String jsonResponse = EntityUtils.toString(entity, "UTF-8");

            JSONObject json = new JSONObject(jsonResponse);

            if (json.getBoolean("success")) {
                ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();

                JSONObject payloadData = json.getJSONObject("payload");
                Iterator<?> keys = payloadData.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    JSONArray messageData = payloadData.getJSONArray(key);

                    boolean ownMessage = false;
                    if (prefs.getBoolean("twoBubbles", true) && messageData.getString(0).equalsIgnoreCase(this.username)) {
                        ownMessage = true;
                    }

                    messages.add(new ChatMessage(Integer.parseInt(key), messageData.getString(0), Helper.serverTimestampToDate(messageData.getString(1)), messageData.getString(2), !ownMessage));
                }
                Collections.sort(messages);
                return messages;
            } else {
                JSONArray errors = json.getJSONArray("errors");
                lastError = "";

                for (int i = 0; i < errors.length(); i++) {
                    lastError += errors.getString(i);
                }
                return null;
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            return null;
        }
    }

    /**
     * Gibt den letzten aufgetretenen Fehler zurück
     *
     * @return Letzter Fehler
     */
    public String getLastError() {
        return lastError;
    }
}