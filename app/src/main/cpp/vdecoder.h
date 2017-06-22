#ifndef VDECODER_H
#define VDECODER_H

#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>

#define COLOR_FORMAT_YUV420 0
#define COLOR_FORMAT_RGB565LE 1
#define COLOR_FORMAT_BGR32 2
typedef void* DECODE_HANDLE;

DECODE_HANDLE vdecoder_init(jint color_format);
void vdecoder_destroy(DECODE_HANDLE h);
jint vdecoder_consumeNalUnitsFromDirectBuffer(DECODE_HANDLE h, void *buf,
				jint num_bytes, jlong pkt_pts);

jboolean vdecoder_isFrameReady(DECODE_HANDLE h);

jint vdecoder_getWidth(DECODE_HANDLE h);
jint vdecoder_getHeight(DECODE_HANDLE h);
jint vdecoder_getOutputByteSize(DECODE_HANDLE h);
jlong vdecoder_decodeFrameToDirectBuffer(DECODE_HANDLE h, void *out_buf, long out_buf_len);

#endif //VDECODER_H