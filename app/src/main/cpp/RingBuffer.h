#ifndef RINGBUFFER_H
#define RINGBUFFER_H

#define RB_STATE_EMPTY	0
#define RB_STATE_WRITE	1
#define RB_STATE_FULL	2
#define RB_STATE_READ	3

typedef struct _RB_FRAME{
	int state;
	unsigned long timestamp;
	void* pBuffer;
}RbFrame;

typedef void* RbHandle;


RbHandle initRbBuffer(int width, int height, int count);
void freeRbBuffer(RbHandle h);

RbFrame* getNextEmptyFrame(RbHandle h);
RbFrame* getNextFullFrame(RbHandle h);
void returnBuffer(RbHandle h, RbFrame* pFrame);
int getWidth(RbHandle h);
int getHeight(RbHandle h);
int getFrameState(RbFrame* pFrame);
#endif //RINGBUFFER_H