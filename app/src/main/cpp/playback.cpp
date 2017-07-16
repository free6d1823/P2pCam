/*
 * Copyright (C) 2017  
 *
 */

#include <jni.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>


#define  LOG_TAG    "libgl2jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

/****************************************************************************************/
/**                                     playback API                                   **/
/****************************************************************************************/
typedef struct {
    FILE* fp;
	int fps;
	long tmStart;
	long tmStop;
	unsigned long totalFrames;
	long tmNow;
	unsigned long currentFrame;
	int msTimeDiff;
}PlaybackContext;

JNIEXPORT jlong Java_com_breeze_nativelib_NativeLib_pbOpen(JNIEnv *env, jobject obj, jstring path)
{
	const char *fileName = env->GetStringUTFChars(path, 0);
	FILE* fp = fopen(fileName, "rb");
	if(!fp)
		return 0;
	PlaybackContext *handle = (PlaybackContext *) calloc(1, sizeof(PlaybackContext));
	memset(handle, 0, sizeof(PlaybackContext));
	handle->fp = fp;
	
	env->ReleaseStringUTFChars(path, fileName);
    return (jlong)handle;
}
JNIEXPORT jint
Java_com_breeze_nativelib_NativeLib_pbNextFrame(JNIEnv *env, jobject obj, jlong handle)
{
    jint curFrame = 0;
    return curFrame;
}
JNIEXPORT jint
Java_com_breeze_nativelib_NativeLib_pbPrevFrame(JNIEnv *env, jobject obj, jlong handle)
{
    jint curFrame = 0;
    return curFrame;
}
JNIEXPORT jint
Java_com_breeze_nativelib_NativeLib_pbSetPosition(JNIEnv *env, jobject obj, jlong handle, jint pos)
{
    jint curFrame = 0;
    return curFrame;
}
JNIEXPORT void
Java_com_breeze_nativelib_NativeLib_pbClose(JNIEnv *env, jobject obj, jlong handle)
{
	PlaybackContext *pContent = (PlaybackContext *)handle;
	
	free(pContent);
}

