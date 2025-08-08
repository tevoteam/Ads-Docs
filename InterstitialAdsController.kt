package com.sky.casttotv.screenmirroring.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.interstitial.InterstitialAdConfig
import com.ads.admob.helper.interstitial.test.InterstitialAdsHelper
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.sky.casttotv.screenmirroring.BuildConfig
import com.sky.casttotv.screenmirroring.utils.SharePrefUtils

object InterstitialAdsController {
    var isCloseInterSplash = MutableLiveData(false)
    const val INTER_ALL = "inter_home"
    fun configInterAds(context: Context?) {
        if (context == null) return
        InterstitialAdsHelper.getInstance(INTER_ALL).setInterstitialAdConfig(
            InterstitialAdConfig(
                idAds = BuildConfig.I001,
                canShowAds = SharePrefUtils.getBoolean(AdsConstant.I001, true),
                canReloadAds = false,
                showByTime = 1,
                adPlacement = INTER_ALL,
            )
        )
    }

    fun requestInter(context: Context) {
        InterstitialAdsHelper.getInstance(INTER_ALL)
            .requestInterAds(context, object : InterstitialAdRequestCallBack {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                }

                override fun onAdLoaded(data: ContentAd) {
                    Log.i("WTFLOG", "ad inter loaded")
                }
            })
    }

    var lastTimeInterShowed = 0L
    private val INTERVAL = 25_000L

    fun forceShowInter(
        activity: Activity?,
        lifecycleOwner: LifecycleOwner,
        onAction: () -> Unit
    ) {
        val isElapsed = lastTimeInterShowed == 0L || System.currentTimeMillis() - lastTimeInterShowed >= INTERVAL

        if (isElapsed) {
            activity?.let {
                InterstitialAdsHelper.getInstance(INTER_ALL)
                    .forceShowInterstitial(
                        it,
                        lifecycleOwner,
                        object : InterstitialAdShowCallBack {
                            override fun onAdClicked() {}

                            override fun onAdClose() {
                                Log.i("WTFLOG", "on close interstitial")
                                lastTimeInterShowed = System.currentTimeMillis()
                                requestInter(activity)
                                onAction()
                            }
                            override fun onAdFailedToShow(adError: AdError) {
                            }

                            override fun onAdImpression() {
                                Log.i("WTFLOG", "on impression interstitial")
                            }

                            override fun onInterstitialShow() {
                                Log.i("WTFLOG", "on show interstitial")
                            }

                            override fun onNextAction() {
                                Log.i("WTFLOG", "on next action interstitial")
                                onAction()
                            }

                        }
                    )
            } ?: run {
                onAction()
            }
        } else {
            onAction()
        }
    }

}