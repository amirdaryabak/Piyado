package com.amirdaryabak.runningapp.storage

interface PrefsUtils {

    fun setFirstName(string: String?)
    fun getFirstName(): String?

    fun setLastName(string: String?)
    fun getLastName(): String?

    fun setWeight(long: Long)
    fun getWeight(): Long

    fun setAge(int: Int)
    fun getAge(): Int

    fun setGender(int: Int)
    fun getGender(): Int

    fun setImage(string: String?)
    fun getImage(): String?

    fun setPhoneNumber(string: String?)
    fun getPhoneNumber(): String?

    fun isLogin(): Boolean

    fun setAskAgainLocationPermission(isReady: Boolean)
    fun getAskAgainLocationPermission(): Boolean

    fun clearData()

}
