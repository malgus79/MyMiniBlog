package com.myminiblog

data class Snapshot(
    var id: String = "",
    var title: String = "",
    var photoUrl: String = "",
    var likeList: Map<String, Boolean> = mutableMapOf(),
)