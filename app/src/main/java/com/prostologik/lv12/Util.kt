package com.prostologik.lv12

object Util {

    const val INT_MIN = -2147483648
    const val INT_MAX = 2147483647

    fun stringToInteger(s: String, num: Int = 0): Int {
        return try {
            s.toInt()
        } catch (nfe: NumberFormatException) {
            num
        }
    }

    fun pixelToByteAsInt(p: Int): Int {
        return if (p < 0) 0
        else if (p > 255) -1
        else if (p > 127) p - 256
        else p
    }

    fun byteToPixel(b: Byte): Int {
        val byteAsInt = b.toInt()
        return if (byteAsInt < -128) 0
        else if (byteAsInt < 0) (256 + byteAsInt)
        else if (byteAsInt > 127) 127
        else byteAsInt
    }

    fun stringByteToPixel(s: String): Int {
        val byteAsInt = stringToInteger(s, 0)
        return if (byteAsInt < -256) 0 // logically < -128, but it is just to avoid errors
        else if (byteAsInt < 0) (256 + byteAsInt)
        else if (byteAsInt > 255) 255 // logically > 127, but it is just to avoid errors
        else byteAsInt
    }

    fun stringByteToColor(s: String, layer: Int = 0): Int {
        // layer: Y = 0, U = 1, V = 2

        val colorM = when (layer) {
            1 -> 1
            2 -> 256
            else -> 65793 // 0
        }

        val colorA = when (layer) {
            1 -> 16777216
            2 -> 65536
            else -> 16777216 // 0
        }

//        val intensity = when (layer) {
//            1 -> 1.37 // 1.73
//            2 -> 1.37
//            else -> 1 // 0
//        }

        return stringByteToPixel(s) * colorM - colorA
    }

    fun limitValue(value: Int, minLimit: Int = INT_MIN, maxLimit: Int = INT_MAX): Int {
        var v = value
        if (value > maxLimit) v = maxLimit
        else if (value < minLimit) v = minLimit
        return v
    }

    fun limitValue(value: Int?, minLimit: Int = INT_MIN, maxLimit: Int = INT_MAX): Int {
        return if (value == null) minLimit
        else limitValue(value, minLimit, maxLimit)
    }

}


//        val r = d0 + (1.370705 * d2);
//        val g = d0 - (0.698001 * d2) - (0.337633 * d1);
//        val b = d0 + (1.732446 * d1);