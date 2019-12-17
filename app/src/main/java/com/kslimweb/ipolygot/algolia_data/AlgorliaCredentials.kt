package com.kslimweb.ipolygot.algolia_data


import com.google.gson.annotations.SerializedName

data class AlgorliaCredentials(
    @SerializedName("api_search_key")
    val apiSearchKey: String,
    @SerializedName("app_id")
    val appId: String
)