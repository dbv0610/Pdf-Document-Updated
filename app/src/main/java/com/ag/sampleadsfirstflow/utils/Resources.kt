package com.ag.sampleadsfirstflow.utils

import com.ag.sampleadsfirstflow.R
import com.ag.sampleadsfirstflow.model.FeatureModel

object Resources {
    val listFeature = listOf(
        FeatureModel(icon = R.drawable.img_app_whatsapp, packageName = "com.whatsapp", name = "WA"),
        FeatureModel(icon = R.drawable.img_app_snapchat, packageName = "com.snapchat.android", name = "Snap"),
        FeatureModel(icon = R.drawable.img_app_messenger, packageName = "com.facebook.orca", name = "FB Mess"),
        FeatureModel(icon = R.drawable.img_app_telegram, packageName = "org.telegram.messenger", name = "Tele"),
        FeatureModel(icon = R.drawable.img_app_discord, packageName = "com.discord", name = "Discor"),
        FeatureModel(icon = R.drawable.img_app_instagram, packageName = "com.instagram.android", name = "Instag"),
        FeatureModel(icon = R.drawable.img_home_photo, packageName = "com.az.photo", name = "Photo"),
        FeatureModel(icon = R.drawable.img_home_video, packageName = "com.az.video", name = "Video"),
        FeatureModel(icon = R.drawable.img_home_audio, packageName = "com.az.audio", name = "Audio"),
        FeatureModel(icon = R.drawable.img_home_other, packageName = "com.az.other", name = "Other Files"),
    )
}
