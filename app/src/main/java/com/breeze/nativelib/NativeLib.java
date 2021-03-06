/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.breeze.nativelib;

// Wrapper for native library

import java.nio.ByteBuffer;

public class NativeLib {

     static {
         System.loadLibrary("native-lib");
     }
	 public static native String getVersion();
    /**
     * @param width the current view width
     * @param height the current view height
     */
     public static native void init(int width, int height);
     public static native void step();
     public static native void setYUVBuffer(int width, int height, int stride, byte[] buffer);
     public static native void decodeNal(ByteBuffer nalUnits, int numBytes, long timeInSec);

    /** return handle of playback function **/
    public static native long pbOpen(String path);
    /** return current frame number **/
    public static native int pbNextFrame(long handle);
    /** return current frame number **/
    public static native int pbPrevFrame(long handle);
    /** return current frame number **/
    public static native int pbSetPosition(long handle, int pos);
    public static native void pbClose(long handle);
}
