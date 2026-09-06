package com.rksolution.urdusikhe;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final String LIVE_REWARDED_AD_UNIT_ID =
            "ca-app-pub-3822431624321367/6028340340";

    private static final String TEST_REWARDED_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/5224354917";

    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private RewardedAd rewardedAd;
    private boolean isLoadingRewardedAd = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        initTextToSpeech();

        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );

        webView.addJavascriptInterface(new AndroidVoice(), "AndroidVoice");
        webView.addJavascriptInterface(new AndroidAds(), "AndroidAds");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        webView.loadUrl("file:///android_asset/index.html");

        initializeAds();
    }

    private void initializeAds() {

        RequestConfiguration requestConfiguration =
                new RequestConfiguration.Builder()
                        .setTagForChildDirectedTreatment(
                                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                        )
                        .setMaxAdContentRating(
                                RequestConfiguration.MAX_AD_CONTENT_RATING_G
                        )
                        .build();

        MobileAds.setRequestConfiguration(requestConfiguration);

        MobileAds.initialize(this, initializationStatus -> {
            Log.d("URDU_SIKHE_ADS", "AdMob initialized");
            loadRewardedAd();
        });
    }

    private void loadRewardedAd() {

        if (isLoadingRewardedAd || rewardedAd != null) {
            return;
        }

        isLoadingRewardedAd = true;

        AdRequest adRequest = new AdRequest.Builder().build();

        /*
         * DEVELOPMENT / TESTING:
         * Test ad unit is used here.
         *
         * Before publishing the final release, change this to:
         *
         * LIVE_REWARDED_AD_UNIT_ID
         */

        String adUnitId = TEST_REWARDED_AD_UNIT_ID;

        RewardedAd.load(
                this,
                adUnitId,
                adRequest,
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(RewardedAd ad) {

                        isLoadingRewardedAd = false;
                        rewardedAd = ad;

                        Log.d(
                                "URDU_SIKHE_ADS",
                                "Rewarded ad loaded"
                        );

                        setRewardedAdCallbacks(ad);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {

                        isLoadingRewardedAd = false;
                        rewardedAd = null;

                        Log.e(
                                "URDU_SIKHE_ADS",
                                "Rewarded ad failed: "
                                        + loadAdError.getMessage()
                        );
                    }
                }
        );
    }

    private void setRewardedAdCallbacks(RewardedAd ad) {

        ad.setFullScreenContentCallback(
                new FullScreenContentCallback() {

                    @Override
                    public void onAdShowedFullScreenContent() {

                        Log.d(
                                "URDU_SIKHE_ADS",
                                "Rewarded ad shown"
                        );
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {

                        Log.d(
                                "URDU_SIKHE_ADS",
                                "Rewarded ad dismissed"
                        );

                        rewardedAd = null;

                        loadRewardedAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(
                            AdError adError
                    ) {

                        Log.e(
                                "URDU_SIKHE_ADS",
                                "Rewarded ad failed to show: "
                                        + adError.getMessage()
                        );

                        rewardedAd = null;

                        loadRewardedAd();
                    }
                }
        );
    }

    private void showRewardedAd() {

        if (rewardedAd == null) {

            Toast.makeText(
                    this,
                    "वीडियो अभी तैयार नहीं है। थोड़ी देर बाद कोशिश करें।",
                    Toast.LENGTH_SHORT
            ).show();

            loadRewardedAd();
            return;
        }

        RewardedAd adToShow = rewardedAd;

        rewardedAd = null;

        adToShow.show(
                this,
                rewardItem -> {

                    Log.d(
                            "URDU_SIKHE_ADS",
                            "User earned reward: "
                                    + rewardItem.getAmount()
                                    + " "
                                    + rewardItem.getType()
                    );

                    giveGameReward();
                }
        );
    }

    private void giveGameReward() {

        if (webView == null) {
            return;
        }

        runOnUiThread(() -> {

            webView.evaluateJavascript(
                    "if(window.onRewardedAdEarned){" +
                            "window.onRewardedAdEarned();" +
                            "}",
                    null
            );

            Toast.makeText(
                    this,
                    "🎁 Reward मिल गया!",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void initTextToSpeech() {

        tts = new TextToSpeech(
                this,
                status -> {

                    if (status == TextToSpeech.SUCCESS) {

                        int result =
                                tts.setLanguage(
                                        new Locale("ur", "PK")
                                );

                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                            result =
                                    tts.setLanguage(
                                            new Locale("ur", "IN")
                                    );
                        }

                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                            tts.setLanguage(
                                    new Locale("hi", "IN")
                            );
                        }

                        tts.setSpeechRate(0.72f);
                        tts.setPitch(1.0f);

                        tts.setOnUtteranceProgressListener(
                                new UtteranceProgressListener() {

                                    @Override
                                    public void onStart(
                                            String utteranceId
                                    ) {
                                    }

                                    @Override
                                    public void onDone(
                                            String utteranceId
                                    ) {
                                    }

                                    @Override
                                    public void onError(
                                            String utteranceId
                                    ) {
                                    }
                                }
                        );

                        ttsReady = true;
                    }
                }
        );
    }

    public class AndroidVoice {

        @JavascriptInterface
        public void speak(String text) {

            if (text == null || text.trim().isEmpty()) {
                return;
            }

            runOnUiThread(() -> {

                if (!ttsReady || tts == null) {
                    return;
                }

                tts.stop();

                tts.speak(
                        text.trim(),
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "urdu_sikhe_voice"
                );
            });
        }

        @JavascriptInterface
        public void stop() {

            runOnUiThread(() -> {

                if (tts != null) {
                    tts.stop();
                }
            });
        }
    }

    public class AndroidAds {

        @JavascriptInterface
        public void showRewardedAd() {

            runOnUiThread(() -> {
                showRewardedAd();
            });
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }

        rewardedAd = null;

        super.onDestroy();
    }
}
