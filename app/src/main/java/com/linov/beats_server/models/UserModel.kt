package com.linov.beats_server.models

import java.util.*

/**
 * Created by Hayi Nukman at 2019-11-27
 * https://github.com/ha-yi
 */

data class UserModel(
    var name: String? = null,
    var email: String? = null,
    var age: String? = null,
    var gender: String? = null,
    var uuid: String? = null,
    var sessionID: String? = null
)