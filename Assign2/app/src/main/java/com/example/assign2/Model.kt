package com.example.assign2

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class QuizData(
    @SerializedName("id")
    var id: Int,
    @SerializedName("song_title")
    var songTitle: String,
    @SerializedName("artist")
    var artist: String,
    @SerializedName("video_title")
    var video_title: String,
    @SerializedName("date")
    var date: Int,
    @SerializedName("actor1")
    var actor1: String,
    @SerializedName("actor2")
    var actor2: String,
    @SerializedName("actor3")
    var actor3: String,
    @SerializedName("profile_image1")
    var profile_image1: String,
    @SerializedName("profile_image2")
    var profile_image2: String,
    @SerializedName("profile_image3")
    var profile_image3: String,
    @SerializedName("image")
    var image: String,
    @SerializedName("dialogue")
    var dialogue: String,
)

data class Member(
    @SerializedName("nick_name")
    var nickName: String,
    @SerializedName("email")
    var email: String,
    @SerializedName("profile_url")
    var profileURL: String,
    @SerializedName("highest_score")
    var highestScore: Int,
): Serializable
