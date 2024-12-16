package com.prostologik.lv12

object Util {

    fun square(number: Int): Int {
        return number * number
    }

    fun stringToInteger(s: String, num: Int = 0): Int {
        return try {
            s.toInt()
        } catch (nfe: NumberFormatException) {
            num
        }
    }

}