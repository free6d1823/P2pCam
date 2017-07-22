package com.breeze.tools;

import java.nio.ByteBuffer;

/**
 * Created by jammy.chang on 2017/7/18.
 */

public class Utility {
    public static     byte[] intToByteArray(int a) {
        return ByteBuffer.allocate(4).putInt(a).array();
    }
    public static int byteArrayToInt(byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }
    public static long byteArrayToLong(byte[] b) {
        return ByteBuffer.wrap(b).getLong();
    }

}
