package com.sky.casttotv.screenmirroring.ads

import android.content.Context
import android.util.Log
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.ads.admob.admob.TevoAdmobFactory
import com.ads.admob.data.ContentAd
import com.ads.admob.listener.NativeAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.sky.casttotv.screenmirroring.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object NativeAdsManager {
    private val nativeMap = mutableMapOf<PreloadKey, NativeAdsContainer>()

    fun preload(
        context: Context,
        adUnit: String,
        enableAd: Boolean,
        @LayoutRes layout: Int,
        adPlacement: String,
        buffer: Int = 1,
    ) {

        val preloadKey = PreloadKey(adPlacement = adPlacement, adUnit = adUnit, layout = layout)
        val nativeContainer = nativeMap.getOrPut(preloadKey) {
            NativeAdsContainer(adUnit = adUnit, adPlacement, enableAd)
        }
        nativeContainer.preload(context, buffer)
    }

    fun getNative(adUnit: String, @LayoutRes layout: Int, adPlacement: String): ContentAd? {
        val preloadKey = PreloadKey(adPlacement = adPlacement, adUnit = adUnit, layout = layout)
        return nativeMap[preloadKey]?.pollContentAd()
    }

    fun getOrAwaitNative(
        lifecycleOwner: LifecycleOwner,
        adUnit: String,
        @LayoutRes layout: Int,
        adPlacement: String,
        onResult: (ContentAd?) -> Unit
    ) {
        val preloadKey = PreloadKey(adPlacement = adPlacement, adUnit = adUnit, layout = layout)
        nativeMap[preloadKey]?.pollOrAwaitContentAd(lifecycleOwner, onResult)
    }

    fun getLayoutNative(isSmall: Boolean, isCtaTop: Boolean): Int {
        return if (isCtaTop) {
            if (isSmall) R.layout.layout_native_small_cta_top
            else R.layout.layout_native_medium_cta_top
        } else {
            if (isSmall) R.layout.layout_native_small
            else R.layout.layout_native_medium
        }

    }

}


data class PreloadKey(val adPlacement: String, val adUnit: String, val layout: Int)
class NativeAdsContainer(
    val adUnit: String, val adPlacement: String, val enableAd: Boolean
) {
    companion object {
        private const val TAG = "NativePreloadManager"
    }

    private val inProgress = AtomicBoolean(false)
    private val totalBuffer: AtomicInteger = AtomicInteger(0)
    val counter = AtomicInteger(0)
    private val nativeQueue: ArrayDeque<ContentAd> = ArrayDeque()
    private val _nativeAdPreloadState: MutableStateFlow<NativePreloadState> =
        MutableStateFlow(NativePreloadState.Idle)
    val nativeAdPreloadState = _nativeAdPreloadState.asStateFlow()
    val nativeAdPreloadStateLiveData: LiveData<NativePreloadState> =
        _nativeAdPreloadState.asLiveData()

    fun pollContentAd(): ContentAd? {
        Log.i("NativePreloadManager", "ContentAd $adPlacement before size queue = ${nativeQueue.size}")

        return nativeQueue.removeFirstOrNull().also {
            Log.i("NativePreloadManager", "ContentAd $adPlacement after size queue = ${nativeQueue.size}")
        }
    }

    fun pollOrAwaitContentAd(
        lifecycleOwner: LifecycleOwner,
        onResult: (ContentAd?) -> Unit
    ) {
        if (inProgress.get()) {
            val observer = object : Observer<NativePreloadState> {
                override fun onChanged(value: NativePreloadState) {
                    if (value is NativePreloadState.Complete
                        || value is NativePreloadState.Consume
                    ) {
                        onResult(pollContentAd())
                        nativeAdPreloadStateLiveData.removeObserver(this)
                    }
                }
            }
            nativeAdPreloadStateLiveData.observe(lifecycleOwner, observer)
        } else {
            onResult(pollContentAd())
        }
    }

    fun preload(
        context: Context,
        buffer: Int,
    ) {
        if (enableAd.not()) {
            _nativeAdPreloadState.value = NativePreloadState.Complete
            return
        }
        totalBuffer.addAndGet(buffer)
        inProgress.set(true)
        if (totalBuffer.get() == 0) {
            _nativeAdPreloadState.value = NativePreloadState.Start
        }
        repeat(buffer) {
            preloadNativeAd(
                context = context,
                idAd = adUnit,
                onAdLoaded = { nativeAd ->
                    if (counter.get() <= totalBuffer.get()) {
                        nativeQueue.addLast(nativeAd)
                        Log.i(
                            TAG,
                            "queue size of native $adPlacement = ${nativeQueue.size}"
                        )
                        _nativeAdPreloadState.value = NativePreloadState.Consume(nativeAd)
                        if (counter.incrementAndGet() == totalBuffer.get()) {
                            _nativeAdPreloadState.value = NativePreloadState.Complete
                            inProgress.set(false)
                        }
                    }
                },
                onFailedToLoad = {
                    if (counter.get() <= totalBuffer.get()) {
                        if (counter.incrementAndGet() == totalBuffer.get()) {
                            _nativeAdPreloadState.value = NativePreloadState.Complete
                            inProgress.set(false)
                        }
                    }
                },
            )
        }
    }

    private fun preloadNativeAd(
        context: Context,
        idAd: String,
        onAdLoaded: (ContentAd) -> Unit,
        onFailedToLoad: () -> Unit = {},
    ) {
        TevoAdmobFactory.INSTANCE.requestNativeAd(
            context,
            idAd,
            adPlacement,
            object : NativeAdCallback {
                override fun populateNativeAd() {}

                override fun onAdLoaded(data: ContentAd) {
                    onAdLoaded(data)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onFailedToLoad()
                }

                override fun onAdClicked() {}

                override fun onAdImpression() {}

                override fun onAdFailedToShow(adError: AdError) {}
            })
    }

}


sealed class NativePreloadState {
    data object Idle : NativePreloadState()
    data object Start : NativePreloadState()
    data class Consume(val nativeAd: ContentAd) : NativePreloadState()
    data object Complete : NativePreloadState()
    data object Error : NativePreloadState()

}

 // NativeAdsManager.INSTANCE.preload(
 //                this,
 //                BuildConfig.N001,
 //                SharePrefUtils.getBoolean(AdsConstant.N001, true),
 //                NativeAdsManager.INSTANCE.getLayoutNative(isSmallLayout, false),
 //                AdsConstant.AD_PLACEMENT_LANGUAGE_CTA_BOTTOM,
 //                1
 //        );



 // NativeAdConfig config = new NativeAdConfig(
 //                BuildConfig.N001,
 //                "",
 //                SharePrefUtils.getBoolean(AdsConstant.N001, true),
 //                true,
 //                NativeAdsManager.INSTANCE.getLayoutNative(isSmallLayout(), false),
 //                AdsConstant.AD_PLACEMENT_LANGUAGE_CTA_BOTTOM
 //        );
 //        nativeAdHelper = new NativeAdHelper(this,this, config);
 //        nativeAdHelper.setAdVisibility(AdOptionVisibility.GONE);

 //        nativeAdHelper.setNativeContentView(frAds);
 //        nativeAdHelper.setShimmerLayoutView(shimmerFrameLayout);
 //        NativeAdsManager.INSTANCE.getOrAwaitNative(
 //                this,
 //                BuildConfig.N001,
 //                NativeAdsManager.INSTANCE.getLayoutNative(isSmallLayout(), false),
 //                AdsConstant.AD_PLACEMENT_LANGUAGE_CTA_BOTTOM,
 //                ad -> {
 //                    if (ad != null) {
 //                        nativeAdHelper.requestAds(new NativeAdParam.Ready(ad));
 //                    } else {
 //                        nativeAdHelper.requestAds(NativeAdParam.Request.INSTANCE);

 //                    }
 //                    return Unit.INSTANCE;
 //                });
