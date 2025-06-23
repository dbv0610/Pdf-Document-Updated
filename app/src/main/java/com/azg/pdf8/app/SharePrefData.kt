package com.azg.pdf8.app


import com.dong.baselib.string.toJson

var isFinishFirstFlow by sharedPreference.boolean("isFinishFirstFlow", false)
var firstOpenApp by sharedPreference.int("firstOpenApp", 0)
fun isUfo(): Boolean {
    return firstOpenApp == 1
}

var countGrantedRecognize by sharedPreference.int("countGrantedRecognize", 0)
var countGrantedNotification by sharedPreference.int("countGrantedNotification", 0)
var countGrantedLocation by sharedPreference.int("countGrantedLocation", 0)
var caloriesStep by sharedPreference.float("caloriesStep", 0f)
var avgWalkingSpeed by sharedPreference.float("avgWalkingSpeed", 0f)
var currentTimeWeightInsert  by sharedPreference.string("currentTimeWeightInsert","")

var isCollectServiceRun = sharedPreference.preferenceOf("isCollectServiceRun",false)

var countStepDay  by sharedPreference.int("countStepDay", 0)
var lastSetDataDay by sharedPreference.string("lastSetDataDay","")
