package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;

public class OCDEAuthParams implements IOCAuthParams {

    @Override
    public String getSite() {
        return "www.opencaching.de";
    }

    @Override
    public int getCKResId() {
        return R.string.oc_de_okapi_consumer_key;
    }

    @Override
    public int getCSResId() {
        return R.string.oc_de_okapi_consumer_secret;
    }

    @Override
    public int getAuthTitelResId() {
        return R.string.auth_ocde;
    }

    @Override
    public int getTokenPublicPrefKey() {
        return R.string.pref_ocde_tokenpublic;
    }

    @Override
    public int getTokenSecretPrefKey() {
        return R.string.pref_ocde_tokensecret;
    }

    @Override
    public int getTempTokenPublicPrefKey() {
        return R.string.pref_temp_ocde_token_public;
    }

    @Override
    public int getTempTokenSecretPrefKey() {
        return R.string.pref_temp_ocde_token_secret;
    }
}
