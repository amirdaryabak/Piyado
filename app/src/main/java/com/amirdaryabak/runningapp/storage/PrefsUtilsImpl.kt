package com.amirdaryabak.runningapp.storage

import android.content.SharedPreferences

private const val PREFS_FIRST_NAME = "PREFS_FIRST_NAME"
private const val PREFS_LAST_NAME = "PREFS_LAST_NAME"
private const val PREFS_WEIGHT = "PREFS_WEIGHT"
private const val PREFS_AGE = "PREFS_AGE"
private const val PREFS_GENDER = "PREFS_GENDER"
private const val PREFS_IMAGE = "PREFS_IMAGE"
private const val PREFS_PHONE_NUMBER = "PREFS_PHONE_NUMBER"
private const val AskAgainLocationPermission = "AskAgainLocationPermission"

class PrefsUtilsImpl constructor(private val prefs: SharedPreferences) : PrefsUtils {

    override fun setFirstName(string: String?) = prefs.edit().putString(PREFS_FIRST_NAME, string).apply()
    override fun getFirstName(): String? = prefs.getString(PREFS_FIRST_NAME, null)

    override fun setLastName(string: String?) = prefs.edit().putString(PREFS_LAST_NAME, string).apply()
    override fun getLastName(): String? = prefs.getString(PREFS_LAST_NAME, null)

    override fun setWeight(long: Long) = prefs.edit().putLong(PREFS_WEIGHT, long).apply()
    override fun getWeight(): Long = prefs.getLong(PREFS_WEIGHT, -1)

    override fun setAge(int: Int) = prefs.edit().putInt(PREFS_AGE, int).apply()
    override fun getAge(): Int = prefs.getInt(PREFS_AGE, -1)

    override fun setGender(int: Int) = prefs.edit().putInt(PREFS_GENDER, int).apply()
    override fun getGender(): Int = prefs.getInt(PREFS_GENDER, -1)

    override fun setImage(string: String?) = prefs.edit().putString(PREFS_IMAGE, string).apply()
    override fun getImage(): String? = prefs.getString(PREFS_IMAGE, null)

    override fun setPhoneNumber(string: String?) = prefs.edit().putString(PREFS_PHONE_NUMBER, string).apply()
    override fun getPhoneNumber(): String? = prefs.getString(PREFS_PHONE_NUMBER, null)

    override fun isLogin() = getFirstName() != null

    override fun setAskAgainLocationPermission(isReady: Boolean) = prefs.edit().putBoolean(
        AskAgainLocationPermission, isReady).apply()
    override fun getAskAgainLocationPermission(): Boolean = prefs.getBoolean(AskAgainLocationPermission, false)

    override fun clearData() = prefs.edit().clear().apply()

}
