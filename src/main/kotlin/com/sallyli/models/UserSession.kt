package com.sallyli.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val accessToken: String)
