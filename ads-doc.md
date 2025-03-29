# Ads-Document <Tevo-Ads-Libs> 
A powerful Android advertising library that supports multiple ad networks including AdMob, Facebook Ads, and AppLovin.
## Features
- Multiple ad format support (Interstitial, Reward, Native, App Open Ads)
- Sequential loading of ad IDs
- Priority-based ad loading
- Customizable timeout and delay settings
- Built-in loading dialog
- Lifecycle-aware components
- Coroutines support for async operations

## Installation
###  Build.gradle
```groovy
//Build.gradle(project)
repositories {
    maven {
        url "https://maven.pkg.github.com/JsonDevLib/lib-ads"
        credentials(HttpHeaderCredentials) {
            name = "thanhluu"
            value = "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"
        }
        authentication {
            header(HttpHeaderAuthentication)
        }
    }
}

Build.gradle(app)
dependencies {
    implementation 'com.tevo:tevo-ads:0.0.2'
}
```
### init() Adslib 
```kotlin
 val tevoAdjustConfig =
            TevoAdjustConfig.Build("my adjust token", false, adRevenueKey = "my adjust reve").build()

 val tevoAdConfig = TevoAdConfig.Builder(tevoAdjustConfig = tevoAdjustConfig)
            .intervalBetweenInterstitial(1000)
            .buildVariantProduce(false)
            .mediationProvider(NetworkProvider.ADMOB)
            .listTestDevices(arrayListOf("FBDA72C75E0671544A38367B5AACCEC7"))
            .eventConfig(EventConfig(exchangeRate = 1))// for usd
            .build()
        AdmobFactory.INSTANCE.initAdmob(this, tevoAdConfig)
```
    - tevoAdjustConfig:
        + environmentProduct : (false) log to sanbox, (true) log to product
        + adRevenueKey: event key revenue
    - tevoAdConfig:
        + tevoAdjustConfig : must be init()
        + intervalBetweenInterstitial : interval time interAds (1000= 1s)
        + mediationProvider : Applovin or Admod
        + eventConfig: AdsPrice ( 1usd)
        + listTestDevices: device test id to show ads test
### Config Billing (IAP, SUB)
```kotlin
    val listPurchaseItem: MutableList<IapItem> = ArrayList()
    listPurchaseItem.add(IapItem("test_iap_ad", IapType.PURCHASE))
    listPurchaseItem.add(IapItem("test_sub_ad", IapType.SUBSCRIPTION))
    IapFactory.initialize(this, listPurchaseItem, true)

    // callback
    IapFactory.getInstance()
   .registerBillingClientConnectionListener(object : BillingClientConnectionListener {
       override fun onConnected(status: Boolean, billingResponseCode: Int) {
           Log.e(TAG, "onConnected: $status $billingResponseCode")
       }
   })
```
### Load Ads 3ids
```kotlin
// listAdsID must have to > 3 (< 3 -> throw exception)
private val nativeAdHelper by lazy {
   val config = NativeAdConfig(
       idAds = "ca-app-pub-3940256099942544/2247696110",//don't worry you can pass empty
       listAdsID = listOf(
           "ca-app-pub-5508627725309496/7973164258", //id_high_1
           "ca-app-pub-5508627725309496/4261984100",//id_high_2
           "ca-app-pub-5508627725309496/9072700921"// id_normal
       ),
       canShowAds = true,
       canReloadAds = true,
       layoutId = com.ads.admob.R.layout.native_exit1,
       adPlacement = "native_exit"
   )


   NativeAdHelper(
       this,
       this,
       config
   )
}
```
### Show Inter Splash
```kotlin
private val interAdSplashHelper by lazy { initInterAdSplash() }
private fun initInterAdSplash(): InterstitialAdSplashHelper {
   val config = InterstitialAdSplashConfig(
       idAds = "ca-app-pub-3940256099942544/1033173712", // not null 
       idAdsPriority = "ca-app-pub-3940256099942544/1033173712", // id highFloor
       canShowAds = true, // remote config to show ads 
       canReloadAds = true, // reload ads after back to 
       timeDelay = 5000L, // delay to show Ads 
       timeOut = 30000L, // if ads load more 30s to -> nextAction()
       showReady = true,// show ads to ads was loaded 
       adPlacement = "inter_splash" // config placement to log event 
   )
   return InterstitialAdSplashHelper(
       activity = this,
       lifecycleOwner = this,
       config = config
   ).apply {
       registerAdListener(interAdCallBack)


   }
}

private val interAdCallBack = object : InterstitialAdCallback {
   override fun onNextAction() {
   }


   override fun onAdClose() {

   }


   override fun onInterstitialShow() {
   }


   override fun onAdLoaded(data: ContentAd) {
   }


   override fun onAdFailedToLoad(loadAdError: LoadAdError) {
   }


   override fun onAdClicked() {
   }


   override fun onAdImpression() {
   }


   override fun onAdFailedToShow(adError: AdError) {
   }
}


```
### App Open Resume (AOA)
```kotlin
// should be initAppOpenAd() in Application
private fun initAppOpenAd(): AppResumeAdHelper {
   val listClassInValid = mutableListOf<Class<*>>()
   listClassInValid.add(AdActivity::class.java)
   listClassInValid.add(SplashFragment::class.java)
   listClassInValid.add(IAPFragment::class.java)
   val config = AppResumeAdConfig(
       idAds = BuildConfig.AppOpen_resume,
       listClassInValid = listClassInValid,
       canShowAds = ConfigPreferences.getInstance(this).isShowAppOpenResume == true,
       adPlacement = "AppOpen_resume"
   )
   return AppResumeAdHelper(
       application = this,
       lifecycleOwner = ProcessLifecycleOwner.get(),
       config = config
   )
}

//disable in curent screen 
App.appResumeAdHelper?.setDisableAppResumeOnScreen()

//enable in curent screen 
App.appResumeAdHelper?.setEnableAppResumeOnScreen()

// request ads. should be call when exits splash screen 
App.appResumeAdHelper?.requestAppOpenResume()

// Exit the app when opening settings, browser,.... If you don't want to display, call
App.appResumeAdHelper?.setDisableAppResumeByClickAction()


```

### Interstitial Ads: 
```kotlin
//com.ads.admob.helper.interstitial.test.InterstitialAdsHelper
//Only call config once, show when remote config is done
InterstitialAdsHelper.getInstance("inter_home").setInterstitialAdConfig(
   InterstitialAdConfig(
       idAds = "ca-app-pub-3940256099942544/1033173712",
       canShowAds = true,
       canReloadAds = false,
       adPlacement = "inter_home"
   )
)


// show ads call this:
InterstitialAdsHelper.getInstance("inter_home")
   .forceShowInterstitial(this, this, object : InterstitialAdShowCallBack {
       override fun onNextAction() {
       }


       override fun onAdClose() {
       }


       override fun onInterstitialShow() {
           Log.e(TAG, "onInterstitialShow: ")
       }


       override fun onAdClicked() {


       }


       override fun onAdImpression() {
           Log.e(TAG, "onAdImpression: ")
       }


       override fun onAdFailedToShow(adError: AdError) {
           Log.e(TAG, "onAdFailedToShow:  ${adError.message}")
       }


   })

```
### Rerward ads 
```kotlin
RewardAdHelper.getInstance("reward_coin")
   .setRewardAdConfig(
       RewardAdConfig(
           "ca-app-pub-3940256099942544/5224354917",
           canShowAds = true,
           canReloadAds = false,
           adPlacement = "reward_home"
       )
   )

//show Rerward
RewardAdHelper.getInstance("reward_home").requestInterAds(this, object : RewardAdRequestCallBack{
   override fun onAdLoaded(data: ContentAd) {
   }


   override fun onAdFailedToLoad(loadAdError: LoadAdError) {


   }


})
```

### Native ads
```kotlin
private val nativeAdHelper by lazy {
   val config = NativeAdConfig(
       idAds = "ca-app-pub-3940256099942544/2247696110",
       idAdsPriority = "ca-app-pub-3940256099942544/1044960115",
       canShowAds = true,
       canReloadAds = true,
       layoutId = com.ads.admob.R.layout.native_exit1,
       adPlacement = "native_exit"
   )


   NativeAdHelper(
       this,
       lifecycleOwner,
       config
   ).apply {
       adVisibility = AdOptionVisibility.INVISIBLE // load failse -> visible config 
   }
}

//set up xml layout and
<FrameLayout
   android:id="@+id/frAdsNative"
   android:layout_width="match_parent"
   android:layout_height="wrap_content"
   app:layout_constraintBottom_toBottomOf="parent">


   <include
       android:id="@+id/shimmerContainerNative"
       layout="@layout/shimmer_native_big" />
</FrameLayout>

//config view to show ads 
nativeAdHelper.setNativeContentView(frAdsNative)
nativeAdHelper.setShimmerLayoutView(shimmerContainerNative.shimmerContainerNative)

// request ads
nativeAdHelper.requestAds(NativeAdParam.Request)
```

### Banner ads
```kotlin
private val bannerAdHelper by lazy { initBannerAd() }
private fun initBannerAd(): BannerAdHelper {
   val config = BannerAdConfig(
       idAds = "ca-app-pub-3940256099942544/6300978111",
       idAdsPriority = "ca-app-pub-3940256099942544/9214589741",
       canShowAds = true,
       canReloadAds = true,
       adPlacement = "banner_home"
   ).apply {
       collapsibleGravity = BannerCollapsibleGravity.TOP // just set to Collapsible Banner Ads
   }
   return BannerAdHelper(activity = this, lifecycleOwner = this, config = config)
}

//xml layout
<FrameLayout
   android:id="@+id/frAdsBanner"
   android:layout_width="match_parent"
   android:layout_height="wrap_content"
   android:layout_alignParentBottom="true">


   <include layout="@layout/layout_banner_control" />
</FrameLayout>
// config view 
bannerAdHelper.setBannerContentView(ifrAdsBannert)
// request ads
bannerAdHelper.requestAds(BannerAdParam.Request)
```
## Some mandatory notes
### Admod implent sdk, mediation 
```grovy
    maven to get admod mediation
maven {
   url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
}
maven {
   url = uri("https://artifact.bytedance.com/repository/pangle/")
}

implementation  ("com.google.ads.mediation:applovin:13.0.0.1")
implementation  ("com.google.ads.mediation:vungle:7.4.1.0")
implementation  ("com.google.ads.mediation:facebook:6.18.0.0")
implementation  ("com.google.ads.mediation:pangle:6.2.0.7.0")

// FB sdk 
implementation ("com.facebook.android:facebook-android-sdk:16.0.0")

//firsebase
implementation 'com.google.firebase:firebase-analytics:22.1.2'


// Android manifest config

<meta-data
   android:name="com.facebook.sdk.ApplicationId"
   android:value="@string/app_id" /> // admod id app 
<meta-data android:name="com.facebook.sdk.ClientToken"
   android:value="@string/ClientToken"/> 
<meta-data
   android:name="com.facebook.sdk.AutoInitEnabled"
   android:value="true" />
<meta-data
   android:name="com.facebook.sdk.AutoLogAppEventsEnabled"
   android:value="true" />
<meta-data
   android:name="com.facebook.sdk.AdvertiserIDCollectionEnabled"
   android:value="true" />
```
### applovin
``` grovy
// build gradle
implementation ("com.applovin:applovin-sdk:13.0.0")
// FB sdk 
implementation ("com.facebook.android:facebook-android-sdk:16.0.0")
//firsebase
implementation 'com.google.firebase:firebase-analytics:22.1.2'
// aplovin adapter
implementation("com.applovin.mediation:google-ad-manager-adapter:23.6.0.1")
implementation("androidx.recyclerview:recyclerview:1.2.1")
implementation("com.applovin.mediation:bigoads-adapter:5.1.0.0")
implementation("com.applovin.mediation:ironsource-adapter:8.4.0.0.1")
implementation("com.applovin.mediation:vungle-adapter:7.4.2.1")
implementation("com.applovin.mediation:facebook-adapter:6.18.0.1")
implementation("com.applovin.mediation:mintegral-adapter:16.8.91.0")
implementation("com.applovin.mediation:unityads-adapter:4.12.5.0")
implementation("com.applovin.mediation:mytarget-adapter:5.27.0.0")
implementation("com.applovin.mediation:yandex-adapter:7.8.0.0")
implementation("com.applovin.mediation:google-adapter:23.6.0.1")
implementation("com.applovin.mediation:mytarget-adapter:5.27.0.0")
implementation("com.applovin.mediation:bytedance-adapter:6.5.0.4.0")


// Adroid manifest 
<meta-data
   android:name="applovin.sdk.key"
   android:value="key" />
// speedUp ads load
<meta-data
   android:name="com.google.android.gms.ads.flag.OPTIMIZE_INITIALIZATION"
   android:value="true" />
<meta-data
   android:name="com.google.android.gms.ads.flag.OPTIMIZE_AD_LOADING"
   android:value="true" />
```



