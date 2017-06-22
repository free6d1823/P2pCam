/*
 * Copyright (C) 2009 The Android Open Source Project
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

// OpenGL ES 2.0 code

#include <jni.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "RingBuffer.h"
 

#define  LOG_TAG    "RingBuffer"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct _RB_HANDLE{
    int width;
    int height;
	int nextWp; //write pointer
    int nextRp; //
    int maxBuffer;
    int frameSize;
	RbFrame* pListRbFrame;
}RbCb;

RbHandle initRbBuffer(int width, int height, int count)
{
    RbCb* h = (RbCb*)malloc(sizeof(RbCb));
    memset(h, 0, sizeof(RbCb));
	h->maxBuffer = count;
    h->width = width;
    h->height = height;
    h->frameSize = width* height * 3/2;
    h->pListRbFrame = (RbFrame*)malloc(sizeof(RbFrame)*count);
    memset(h->pListRbFrame, 0, sizeof(RbFrame)*count);
    unsigned char* pPool = (unsigned char*)malloc(h->frameSize*count);
    memset(pPool, 0, h->frameSize*count);
    for(int i=0;i <count; i++){
        h->pListRbFrame[i].pBuffer = (RbFrame*)(pPool + h->frameSize*i);
        h->pListRbFrame[i].state = RB_STATE_EMPTY;
    }

	return (RbHandle)h;
}
void freeRbBuffer(RbHandle h)
{
    RbCb* pCb = (RbCb*)h;
	if(pCb){
        if(pCb->pListRbFrame) {
            if (pCb->pListRbFrame)
                free (pCb->pListRbFrame[0].pBuffer);
            free (pCb->pListRbFrame);
        }
		free(h);
	}
}

RbFrame* getNextEmptyFrame(RbHandle h)
{
    RbCb* pCb = (RbCb*)h;
    RbFrame* pHit = NULL;

    if(pCb->pListRbFrame[pCb->nextWp].state == RB_STATE_EMPTY) {
        pHit = &pCb->pListRbFrame[pCb->nextWp];
        pHit->state = RB_STATE_WRITE;
        pCb->nextWp ++;

        if(pCb->nextWp >= pCb->maxBuffer) pCb->nextWp = 0;

    }
	return pHit;
}
RbFrame* getNextFullFrame(RbHandle h)
{
    RbCb* pCb = (RbCb*)h;
    RbFrame* pHit = NULL;
    if(pCb->pListRbFrame[pCb->nextRp].state == RB_STATE_FULL) {
        pHit = &pCb->pListRbFrame[pCb->nextRp];

        pHit->state = RB_STATE_READ;
        pCb->nextRp++;
        if(pCb->nextRp >= pCb->maxBuffer) pCb->nextRp = 0;
    }
	return pHit;
}
void returnBuffer(RbHandle h, RbFrame* pFrame)
{
    RbCb* pCb = (RbCb*)h;
    if(pCb == NULL || pFrame == NULL)
        return;
    if(pFrame->state == RB_STATE_WRITE)
        pFrame->state = RB_STATE_FULL;
    else if(pFrame->state == RB_STATE_READ)
        pFrame->state = RB_STATE_EMPTY;

}
int getWidth(RbHandle h){
    return ((RbCb*)h)->width;
}
int getHeight(RbHandle h){
    return ((RbCb*)h)->height;
}
int getFrameState(RbFrame* pFrame){
    return pFrame->state;
}