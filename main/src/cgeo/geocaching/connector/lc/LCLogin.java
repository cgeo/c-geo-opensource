package cgeo.geocaching.connector.lc;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;


public class LCLogin extends AbstractLogin {

    private String sessionId = null;

    private LCLogin() {
        // singleton
    }

    private static class SingletonHolder {
        @NonNull
        private static final LCLogin INSTANCE = new LCLogin();
    }

    @NonNull
    public static LCLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry) {
        return login(retry, Settings.getCredentials(LCConnector.getInstance()));
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry, @NonNull final Credentials credentials) {
        if (credentials.isInvalid()) {
            clearLoginInfo();
            Log.w("LCLogin.login: No login information stored");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_working));

        final Parameters params = new Parameters("user", credentials.getUserName(), "pass", credentials.getPassword());

        //final String loginData = Network.getResponseData(Network.postRequest("https://", params));
        final String loginData = "faking a good login"; // TODO baiti

        if (StringUtils.isBlank(loginData)) {
            Log.e("LCLogin.login: Failed to retrieve login data");
            return StatusCode.CONNECTION_FAILED_LC; // no login page
        }

        if (loginData.contains("Wrong username or password")) { // hardcoded in English
            Log.i("Failed to log in labs.geocaching.com as " + credentials.getUserName() + " because of wrong username/password");
            return StatusCode.WRONG_LOGIN_DATA; // wrong login
        }

        if (getLoginStatus(loginData)) {
            Log.i("Successfully logged in labs.geocaching.com as " + credentials.getUserName());

            return StatusCode.NO_ERROR; // logged in
        }

        Log.i("Failed to log in labs.geocaching.com as " + credentials.getUserName() + " for some unknown reason");
        if (retry) {
            return login(false, credentials);
        }

        return StatusCode.UNKNOWN_ERROR; // can't login
    }


    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @return {@code true} if user is logged in, {@code false} otherwise
     */
    private boolean getLoginStatus(@Nullable final String data) {
        if (StringUtils.isBlank(data) || StringUtils.equals(data, "[]")) {
            Log.e("LCLogin.getLoginStatus: No or empty data given");
            return false;
        }

        final Application application = CgeoApplication.getInstance();
        setActualStatus(application.getString(R.string.init_login_popup_ok));

        if (false) { // TODO only for debug
            try {
                final JsonNode json = JsonUtils.reader.readTree(data);

                final String sid = json.get("sid").asText();
                if (!StringUtils.isBlank(sid)) {
                    sessionId = sid;
                    setActualLoginStatus(true);
                    setActualUserName(json.get("username").asText());
                    setActualCachesFound(json.get("found").asInt());
                    return true;
                }
                resetLoginStatus();
            } catch (IOException | NullPointerException e) {
                Log.e("LCLogin.getLoginStatus", e);
            }

            setActualStatus(application.getString(R.string.init_login_popup_failed));
            return false;
        }
        return true; // TODO remove after debug

    }

    @Override
    protected void resetLoginStatus() {
        sessionId = null;
        setActualLoginStatus(false);
    }

    public String getSessionId() {
        if (!StringUtils.isBlank(sessionId) || login() == StatusCode.NO_ERROR) {
            return sessionId;
        }
        return StringUtils.EMPTY;
    }

}
