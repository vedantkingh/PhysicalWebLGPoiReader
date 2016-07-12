/*
 * Copyright 2016 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 j*
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gsoc.google.com.physicalweblgpoireader.PW;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.uribeacon.scan.util.RangingUtils;
import org.uribeacon.scan.util.RegionResolver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import gsoc.google.com.physicalweblgpoireader.PW.collection.PhysicalWebCollection;
import gsoc.google.com.physicalweblgpoireader.PW.collection.PwPair;
import gsoc.google.com.physicalweblgpoireader.PW.collection.PwsClient;
import gsoc.google.com.physicalweblgpoireader.PW.collection.PwsResult;
import gsoc.google.com.physicalweblgpoireader.PW.collection.UrlDevice;
import gsoc.google.com.physicalweblgpoireader.R;

/**
 * This class is for various static utilities, largely for manipulation of data structures provided
 * by the collections library.
 */
class Utils {

    public static final String PROD_ENDPOINT = "https://url-caster.appspot.com";
    public static final int PROD_ENDPOINT_VERSION = 1;
    public static final String DEV_ENDPOINT = "https://url-caster-dev.appspot.com";
    public static final int DEV_ENDPOINT_VERSION = 1;
    public static final String GOOGLE_ENDPOINT = "https://physicalweb.googleapis.com";
    public static final int GOOGLE_ENDPOINT_VERSION = 2;
    private static final String SCANTIME_KEY = "scantime";
    private static final String PUBLIC_KEY = "public";
    private static final String RSSI_KEY = "rssi";
    private static final String TXPOWER_KEY = "tx";
    private static final String PWSTRIPTIME_KEY = "pwstriptime";
    private static final RegionResolver REGION_RESOLVER = new RegionResolver();
    private static final String SEPARATOR = "\0";

    public static Comparator<PwPair> newDistanceComparator() {
        return new Comparator<PwPair>() {
            public Map<String, Double> cachedDistances = new HashMap<>();

            public double getDistance(UrlDevice urlDevice) {
                if (cachedDistances.containsKey(urlDevice.getId())) {
                    return cachedDistances.get(urlDevice.getId());
                }
                double distance = Utils.getDistance(urlDevice);
                cachedDistances.put(urlDevice.getId(), distance);
                return distance;
            }

            @Override
            public int compare(PwPair lhs, PwPair rhs) {
                return Double.compare(getDistance(lhs.getUrlDevice()),
                        getDistance(rhs.getUrlDevice()));
            }
        };
    }

    public static String formatEndpointForSharedPrefernces(String pwsUrl, int pwsVersion,
                                                           String apiKey) {
        return pwsUrl + SEPARATOR + pwsVersion + SEPARATOR + apiKey;
    }

    private static int getGoogleApiKeyResourceId(Context context) {
        return context.getResources().getIdentifier("google_api_key", "string",
                context.getPackageName());
    }

    public static boolean isGoogleApiKeyAvailable(Context context) {
        return getGoogleApiKeyResourceId(context) != 0;
    }

    public static String getGoogleApiKey(Context context) {
        int resourceId = getGoogleApiKeyResourceId(context);
        return resourceId != 0 ? context.getString(resourceId) : "";
    }

    public static String getDefaultPwsEndpointPreferenceString(Context context) {
        if (isGoogleApiKeyAvailable(context)) {
            return formatEndpointForSharedPrefernces(GOOGLE_ENDPOINT, GOOGLE_ENDPOINT_VERSION,
                    getGoogleApiKey(context));
        }
        return formatEndpointForSharedPrefernces(PROD_ENDPOINT, PROD_ENDPOINT_VERSION, "");
    }

    private static PwsEndpoint getCurrentPwsEndpoint(Context context) {
        return new PwsEndpoint(getCurrentPwsEndpointUrl(context),
                getCurrentPwsEndpointVersion(context),
                getCurrentPwsEndpointApiKey(context));
    }

    private static String getCurrentPwsEndpointString(Context context) {
        String defaultEndpoint = getDefaultPwsEndpointPreferenceString(context);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(context.getString(R.string.pws_endpoint_setting_key),
                defaultEndpoint);
    }

    private static String getCurrentPwsEndpointUrl(Context context) {
        String endpoint = getCurrentPwsEndpointString(context);
        return endpoint.split(SEPARATOR)[0];
    }

    private static int getCurrentPwsEndpointVersion(Context context) {
        String endpoint = getCurrentPwsEndpointString(context);
        return Integer.parseInt(endpoint.split(SEPARATOR)[1]);
    }

    private static String getCurrentPwsEndpointApiKey(Context context) {
        String endpoint = getCurrentPwsEndpointString(context);
        if (endpoint.endsWith(SEPARATOR) || endpoint.endsWith("null")) {
            return "";
        }
        return endpoint.split(SEPARATOR)[2];
    }

    public static boolean setPwsEndpoint(Context context,
                                         PhysicalWebCollection physicalWebCollection) {
        PwsEndpoint endpoint = getCurrentPwsEndpoint(context);
        physicalWebCollection.setPwsEndpoint(endpoint.url, endpoint.apiVersion, endpoint.apiKey);
        return !endpoint.neededApiKey;
    }

    public static boolean setPwsEndpoint(Context context, PwsClient pwsClient) {
        PwsEndpoint endpoint = getCurrentPwsEndpoint(context);
        pwsClient.setEndpoint(endpoint.url, endpoint.apiVersion, endpoint.apiKey);
        return !endpoint.neededApiKey;
    }

    private static void throwEncodeException(JSONException e) {
        throw new RuntimeException("Could not encode JSON", e);
    }

    public static Intent createNavigateToUrlIntent(PwsResult pwsResult) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(pwsResult.getSiteUrl()));
        return intent;
    }

    public static PendingIntent createNavigateToUrlPendingIntent(
            PwsResult pwsResult, Context context) {
        Intent intent = createNavigateToUrlIntent(pwsResult);
        int requestID = (int) System.currentTimeMillis();
        return PendingIntent.getActivity(context, requestID, intent, 0);
    }

    public static Bitmap getBitmapIcon(PhysicalWebCollection pwCollection, PwsResult pwsResult) {
        byte[] iconBytes = pwCollection.getIcon(pwsResult.getIconUrl());
        if (iconBytes == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
    }

    public static long getScanTimeMillis(UrlDevice urlDevice) {
        try {
            return urlDevice.getExtraLong(SCANTIME_KEY);
        } catch (JSONException e) {
            throw new RuntimeException("Scan time not available in device " + urlDevice.getId(), e);
        }
    }

    public static boolean isPublic(UrlDevice urlDevice) {
        return urlDevice.optExtraBoolean(PUBLIC_KEY, true);
    }

    public static boolean isBleUrlDevice(UrlDevice urlDevice) {
        try {
            urlDevice.getExtraInt(RSSI_KEY);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    public static int getRssi(UrlDevice urlDevice) {
        try {
            return urlDevice.getExtraInt(RSSI_KEY);
        } catch (JSONException e) {
            throw new RuntimeException("Tried to get RSSI from non-ble device " + urlDevice.getId(), e);
        }
    }

    public static int getTxPower(UrlDevice urlDevice) {
        try {
            return urlDevice.getExtraInt(TXPOWER_KEY);
        } catch (JSONException e) {
            throw new RuntimeException(
                    "Tried to get TX power from non-ble device " + urlDevice.getId(), e);
        }
    }

    public static long getPwsTripTimeMillis(PwsResult pwsResult) {
        try {
            return pwsResult.getExtraLong(PWSTRIPTIME_KEY);
        } catch (JSONException e) {
            throw new RuntimeException("PWS trip time not recorded in PwsResult");
        }
    }

    public static String getGroupId(PwsResult pwsResult) {
        // The PWS does not always give us a group id yet.
        if (pwsResult.getGroupId() == null || pwsResult.getGroupId().equals("")) {
            try {
                return new URI(pwsResult.getSiteUrl()).getHost() + pwsResult.getTitle();
            } catch (URISyntaxException e) {
                return pwsResult.getSiteUrl();
            }
        }
        return pwsResult.getGroupId();
    }

    public static PwPair getTopRankedPwPairByGroupId(
            PhysicalWebCollection pwCollection, String groupId) {
        // This does the same thing as the PhysicalWebCollection method, only it uses our custom
        // getGroupId method.
        for (PwPair pwPair : pwCollection.getGroupedPwPairsSortedByRank(newDistanceComparator())) {
            if (getGroupId(pwPair.getPwsResult()).equals(groupId)) {
                return pwPair;
            }
        }
        return null;
    }

    public static void updateRegion(UrlDevice urlDevice) {
        REGION_RESOLVER.onUpdate(urlDevice.getId(), getRssi(urlDevice), getTxPower(urlDevice));
    }

    public static double getSmoothedRssi(UrlDevice urlDevice) {
        return REGION_RESOLVER.getSmoothedRssi(urlDevice.getId());
    }

    public static double getDistance(UrlDevice urlDevice) {
        return REGION_RESOLVER.getDistance(urlDevice.getId());
    }

    public static String getRegionString(UrlDevice urlDevice) {
        return RangingUtils.toString(REGION_RESOLVER.getRegion(urlDevice.getId()));
    }

    static class UrlDeviceBuilder extends UrlDevice.Builder {
        public UrlDeviceBuilder(String id, String url) {
            super(id, url);
        }

        public UrlDeviceBuilder setScanTimeMillis(long timeMillis) {
            addExtra(SCANTIME_KEY, timeMillis);
            return this;
        }

        public UrlDeviceBuilder setPrivate() {
            addExtra(PUBLIC_KEY, false);
            return this;
        }

        public UrlDeviceBuilder setPublic() {
            addExtra(PUBLIC_KEY, true);
            return this;
        }

        public UrlDeviceBuilder setRssi(int rssi) {
            addExtra(RSSI_KEY, rssi);
            return this;
        }

        public UrlDeviceBuilder setTxPower(int txPower) {
            addExtra(TXPOWER_KEY, txPower);
            return this;
        }
    }

    static class PwsResultBuilder extends PwsResult.Builder {
        public PwsResultBuilder(PwsResult pwsResult) throws JSONException {
            super(pwsResult);
        }

        public PwsResultBuilder setPwsTripTimeMillis(PwsResult pwsResult, long timeMillis) {
            try {
                addExtra(PWSTRIPTIME_KEY, timeMillis);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }
    }

    private static class PwsEndpoint {
        public String url;
        public int apiVersion;
        public String apiKey;
        public boolean neededApiKey;

        public PwsEndpoint(String url, int apiVersion, String apiKey) {
            this.url = url;
            this.apiVersion = apiVersion;
            this.apiKey = apiKey;
            this.neededApiKey = needApiKey();
            if (this.neededApiKey) {
                this.url = PROD_ENDPOINT;
                this.apiVersion = PROD_ENDPOINT_VERSION;
            }
        }

        private boolean needApiKey() {
            return apiVersion >= 2 && apiKey.isEmpty();
        }

    }
}
