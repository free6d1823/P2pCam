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
#include "vdecoder.h"

#define  LOG_TAG    "libgl2jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct DecoderContext {
    int color_format;
    struct AVCodec *codec;
    struct AVCodecContext *codec_ctx;
    struct AVFrame *src_frame;
    struct AVFrame *dst_frame;
    struct SwsContext *convert_ctx;
    int frame_ready;
} DecoderContext;
static void av_log_callback(void *ptr, int level, const char *fmt, __va_list vl) {
    LOGI( fmt, vl);
}

DECODE_HANDLE vdecoder_init(jint color_format) {
    av_register_all();
    av_log_set_callback(av_log_callback);

    DecoderContext *ctx = (DecoderContext *) calloc(1, sizeof(DecoderContext));


    switch (color_format) {
        case COLOR_FORMAT_YUV420:
            ctx->color_format = PIX_FMT_YUV420P;
            break;
        case COLOR_FORMAT_RGB565LE:
            ctx->color_format = PIX_FMT_RGB565LE;
            break;
        case COLOR_FORMAT_BGR32:
            ctx->color_format = PIX_FMT_BGR32;
            break;
    }

    ctx->codec = avcodec_find_decoder( CODEC_ID_H264);
    ctx->codec_ctx = avcodec_alloc_context3(ctx->codec);
    ctx->codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
    ctx->codec_ctx->flags2 |= CODEC_FLAG2_CHUNKS;

    ctx->src_frame = av_frame_alloc();
    ctx->dst_frame = av_frame_alloc();

    int ret = avcodec_open2(ctx->codec_ctx, ctx->codec, NULL);
    if (ret != 0){
        av_free(ctx->codec_ctx);
        av_free(ctx->src_frame);
        av_free(ctx->dst_frame);
        free(ctx);
        ctx = NULL;
    }
	return (DECODE_HANDLE) ctx;
}


void vdecoder_destroy(DECODE_HANDLE h) {
    DecoderContext * ctx = (DecoderContext *)h;
    avcodec_close(ctx->codec_ctx);
    av_free(ctx->codec_ctx);
    av_free(ctx->src_frame);
    av_free(ctx->dst_frame);

    free(ctx);
}
jint vdecoder_consumeNalUnitsFromDirectBuffer(DECODE_HANDLE h, void *buf,
				jint num_bytes, jlong pkt_pts) {
    DecoderContext * ctx = (DecoderContext *)h;
    if (buf == NULL) {
      LOGE("Error getting direct buffer address");
      return -1;
    }

  AVPacket packet = {
      .data = (uint8_t*)buf,
      .size = num_bytes,
      .pts = pkt_pts
  };

  int frameFinished = 0;
  int res = avcodec_decode_video2(ctx->codec_ctx, ctx->src_frame, &frameFinished, &packet);
//-rr ?
  if (frameFinished)
    ctx->frame_ready = 1;

  return res;
}

jboolean vdecoder_isFrameReady(DECODE_HANDLE h) {
    DecoderContext * ctx = (DecoderContext *)h;
  return ctx->frame_ready ? JNI_TRUE : JNI_FALSE;
}

jint vdecoder_getWidth(DECODE_HANDLE h) {
    DecoderContext * ctx = (DecoderContext *)h;
  return ctx->codec_ctx->width;
}

jint vdecoder_getHeight(DECODE_HANDLE h) {
    DecoderContext * ctx = (DecoderContext *)h;
  return ctx->codec_ctx->height;
}

jint vdecoder_getOutputByteSize(DECODE_HANDLE h) {
    DecoderContext * ctx = (DecoderContext *)h;
  return avpicture_get_size((enum AVPixelFormat) ctx->color_format, ctx->codec_ctx->width, ctx->codec_ctx->height);
}

jlong vdecoder_decodeFrameToDirectBuffer(DECODE_HANDLE h, void *out_buf, long out_buf_len) {
    DecoderContext *ctx = (DecoderContext *) h;
    if (!ctx->frame_ready) {
        LOGE("Frame is not ready...");
        return -1;
    }

    if (out_buf == NULL) {
        LOGE("Error getting direct buffer address");
        return -2;
    }

    int pic_buf_size = avpicture_get_size((enum AVPixelFormat) ctx->color_format,
                                          ctx->codec_ctx->width, ctx->codec_ctx->height);
    if (out_buf_len < pic_buf_size) {
        LOGE("Input buffer too small");
        return pic_buf_size;
    }

    if (ctx->convert_ctx == NULL) {
        ctx->convert_ctx = sws_getContext(ctx->codec_ctx->width, ctx->codec_ctx->height,
                                          ctx->codec_ctx->pix_fmt,
                                          ctx->codec_ctx->width,
                                          ctx->codec_ctx->height,
                                          (enum AVPixelFormat) ctx->color_format,
                                          SWS_FAST_BILINEAR, NULL, NULL, NULL);
    }

    int ret = avpicture_fill((AVPicture *) ctx->dst_frame, (uint8_t *) out_buf,
                             (enum AVPixelFormat) ctx->color_format, ctx->codec_ctx->width,
                             ctx->codec_ctx->height);


    ret = sws_scale(ctx->convert_ctx, (const uint8_t **) ctx->src_frame->data,
                    ctx->src_frame->linesize, 0, ctx->codec_ctx->height,
                    ctx->dst_frame->data, ctx->dst_frame->linesize);
    if (ret <= 0) {
    LOGE("sws_scale: the height of the output slice = %d", ret);
}
  ctx->frame_ready = 0;

  if (ctx->src_frame->pkt_pts == AV_NOPTS_VALUE) {
    LOGE("No PTS was passed from avcodec_decode!");
  }

  return ctx->src_frame->pkt_pts;
}
