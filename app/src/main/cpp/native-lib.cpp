#include <jni.h>



// OpenGL ES 2.0 code

#include <jni.h>
#include <android/log.h>
#include <string>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "vdecoder.h"
#include "RingBuffer.h"

#define  LOG_TAG    "native-lib"
extern "C" {
    JNIEXPORT jstring JNICALL Java_com_breeze_nativelib_NativeLib_getVersion(JNIEnv * env, jobject obj);
    JNIEXPORT void JNICALL Java_com_breeze_nativelib_NativeLib_init(JNIEnv * env, jobject obj,  jint width, jint height);
    JNIEXPORT void Java_com_breeze_nativelib_NativeLib_step(JNIEnv * env, jobject obj);
    JNIEXPORT void Java_com_breeze_nativelib_NativeLib_setYUVBuffer(JNIEnv * env, jobject obj,
                                            jint width, jint height, jint stride, jbyteArray array);
    JNIEXPORT void Java_com_breeze_nativelib_NativeLib_decodeNal(JNIEnv * env, jobject obj,
                                                                       jobject nal_units, jint num_bytes, jlong timeInSec);

};
JNIEXPORT jstring JNICALL
Java_com_breeze_nativelib_NativeLib_getVersion(JNIEnv *env, jobject /* this */) {
    std::string ver = "NativeLib v. 1.0.0.0";
    return env->NewStringUTF(ver.c_str());
}


#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


DECODE_HANDLE g_decoderHandle = NULL;

static void printGLString(const char *name, GLenum s) {
    const char *v = (const char *) glGetString(s);
    LOGI("GL %s = %s\n", name, v);
}

static void checkGlError(const char* op) {
    for (GLint error = glGetError(); error; error
            = glGetError()) {
        LOGI("after %s() glError (0x%x)\n", op, error);
    }
}

auto gVertexShader =
    "attribute vec4 a_position;\n"
            "attribute vec2 a_texcoord;\n"
            "varying vec2 v_texcoord;\n"
            "void main() {\n"
            "  gl_Position = a_position;\n"
            "  v_texcoord = a_texcoord;\n"
            "}\n";

auto gFragmentShader =
    "precision mediump float;\n"
            "uniform sampler2D tex_samplerY;\n"
            "uniform sampler2D tex_samplerU;\n"
            "uniform sampler2D tex_samplerV;\n"

            "varying vec2 v_texcoord;\n"
            "void main() {\n"
            "mediump vec3 yuv;\n"
            "lowp vec3 rgb;\n"
            "yuv.x = texture2D(tex_samplerY, v_texcoord).r;\n"
            "yuv.y = texture2D(tex_samplerU, v_texcoord).r-0.5;\n"
            "yuv.z = texture2D(tex_samplerV, v_texcoord).r-0.5;\n"
"rgb = mat3(1,1,1, 0, -.21482, 2.12798, 1.28033, -.38059, 0) * yuv;\n"
"gl_FragColor = vec4(rgb, 1);"
            "}\n";

GLuint loadShader(GLenum shaderType, const char* pSource) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, &pSource, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n",
                            shaderType, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }
    return shader;
}

GLuint createProgram(const char* pVertexSource, const char* pFragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexSource);
    if (!vertexShader) {
        return 0;
    }

    GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, pFragmentSource);
    if (!pixelShader) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        glLinkProgram(program);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            GLint bufLength = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*) malloc(bufLength);
                if (buf) {
                    glGetProgramInfoLog(program, bufLength, NULL, buf);
                    LOGE("Could not link program:\n%s\n", buf);
                    free(buf);
                }
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

GLuint gProgram;
GLuint mTexSamplerHandleY;
GLuint mTexSamplerHandleU;
GLuint mTexSamplerHandleV;
GLuint mTexCoordHandle;
GLuint mPosCoordHandle;
GLuint mTextures[3] = {0};
//view size
int mViewWidth;
int mViewHeight;
//texture size
int mTexWidth = 0;
int mTexHeight = 0;
GLfloat gPosVertices[] = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f };
GLfloat gTexVertices[] = {  0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
void computeOutputVertices() {
    float imgAspectRatio = mTexWidth / (float)mTexHeight;
    float viewAspectRatio = mViewWidth / (float)mViewHeight;
    float relativeAspectRatio = viewAspectRatio / imgAspectRatio;
    float x0, y0, x1, y1;
    if (relativeAspectRatio > 1.0f) {
        x0 = -1.0f / relativeAspectRatio;
        y0 = -1.0f;
        x1 = 1.0f / relativeAspectRatio;
        y1 = 1.0f;
    } else {
        x0 = -1.0f;
        y0 = -relativeAspectRatio;
        x1 = 1.0f;
        y1 = relativeAspectRatio;
    }
    gPosVertices[0] = x0;gPosVertices[1]=y0;
    gPosVertices[2] = x1;gPosVertices[3]=y0;
    gPosVertices[4] = x0;gPosVertices[5]=y1;
    gPosVertices[6] = x1;gPosVertices[7]=y1;

}

bool setupGraphics(int w, int h) {
    printGLString("Version", GL_VERSION);
    printGLString("Vendor", GL_VENDOR);
    printGLString("Renderer", GL_RENDERER);
    printGLString("Extensions", GL_EXTENSIONS);

    LOGI("setupGraphics(%d, %d)", w, h);
    gProgram = createProgram(gVertexShader, gFragmentShader);
    if (!gProgram) {
        LOGE("Could not create program.");
        return false;
    }
    mTexSamplerHandleY = glGetUniformLocation(gProgram, "tex_samplerY");
    mTexSamplerHandleU = glGetUniformLocation(gProgram, "tex_samplerU");
    mTexSamplerHandleV = glGetUniformLocation(gProgram, "tex_samplerV");
    mTexCoordHandle = glGetAttribLocation(gProgram, "a_texcoord");
    mPosCoordHandle = glGetAttribLocation(gProgram, "a_position");
    checkGlError("glGetAttribLocation");
    LOGI("glGetAttribLocation(\"a_position\") = %d\n",
         mPosCoordHandle);
    glGenTextures(3, mTextures);
    checkGlError("glGenTextures");

    mViewWidth = w;
    mViewHeight = h;


    return true;
}

void renderFrame() {
    static float grey=0.35;

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glUseProgram(gProgram);
    checkGlError("glUseProgram");
    glViewport(0, 0, mViewWidth, mViewHeight);//bitmap size
    glDisable(GL_BLEND);

// Set the vertex attributes
    //GL_INVALID_VALUE, 0x0501
    //Given when a value parameter is not a legal value for that function. This is only given for local problems; if the spec allows the value in certain circumstances, where other parameters or state dictate those circumstances, then GL_INVALID_OPERATION is the result instead.
    glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, GL_FALSE, 0, gTexVertices);
    glEnableVertexAttribArray(mTexCoordHandle);
    glVertexAttribPointer(mPosCoordHandle, 2, GL_FLOAT, GL_FALSE, 0, gPosVertices);
    glEnableVertexAttribArray(mPosCoordHandle);

// Set the input texture

    glUniform1i(mTexSamplerHandleY, 0);
    glUniform1i(mTexSamplerHandleU, 1);
    glUniform1i(mTexSamplerHandleV, 2);

    glClearColor(0.0, grey, 0.0, 1.0f);
    glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    checkGlError("glDrawArrays");
}


JNIEXPORT void Java_com_breeze_nativelib_NativeLib_init(JNIEnv * env, jobject obj,  jint width, jint height)
{
    setupGraphics(width, height);
}



RbHandle ghRingBuffer = NULL;
void setYUVBuffer(unsigned char* buffer, jint width, jint height, jint stride)
{
    int dwSrcBufferSize;

    dwSrcBufferSize = stride * height * 3 / 2;
    if (ghRingBuffer) {
        if (getWidth(ghRingBuffer) != width || getHeight(ghRingBuffer) != height) {
            freeRbBuffer(ghRingBuffer);
            ghRingBuffer = NULL;
        }
    }
    if(ghRingBuffer == NULL) {
        ghRingBuffer = initRbBuffer(width, height, 4);
        LOGI("initRbBuffer");
    }
    RbFrame* pFrame;
    pFrame = getNextEmptyFrame( ghRingBuffer);
    unsigned char * pDest = (unsigned char *)pFrame->pBuffer;
    memcpy(pDest, buffer, dwSrcBufferSize);
    returnBuffer(ghRingBuffer, pFrame);
}
JNIEXPORT void Java_com_breeze_nativelib_NativeLib_setYUVBuffer(JNIEnv * env, jobject obj, jint width, jint height, jint stride, jbyteArray array)
{
    int len = env->GetArrayLength(array);

    int dwSrcBufferSize;

    dwSrcBufferSize = stride * height * 3 / 2;
    if (len < dwSrcBufferSize)
    {
        LOGE("Java_com_android_gl2jni_GL2JNILib_setYUVBuffer invalid array input.\n");
        return;
    }
    if (ghRingBuffer) {
        if (getWidth(ghRingBuffer) != width || getHeight(ghRingBuffer) != height) {
            freeRbBuffer(ghRingBuffer);
            ghRingBuffer = NULL;
        }
    }
    if(ghRingBuffer == NULL){
        LOGI("initRbBuffer");
        ghRingBuffer = initRbBuffer (width, height, 4);
    }

    RbFrame* pFrame;
    pFrame = getNextEmptyFrame( ghRingBuffer);
    if(pFrame) {
        unsigned char *pDest = (unsigned char *) pFrame->pBuffer;
        // copies a region of a primitive array into a buffer.
        env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(pDest));
        returnBuffer(ghRingBuffer, pFrame);
    }
}
JNIEXPORT void Java_com_breeze_nativelib_NativeLib_decodeNal(JNIEnv * env, jobject obj, jobject nal_units, jint num_bytes, jlong timeInSec)
{
    unsigned char *buf = NULL;

    if (nal_units == NULL) {
        LOGE("Received null buffer, sending empty packet to decoder");
    }
    else {
        buf = (unsigned char *) env->GetDirectBufferAddress(nal_units);
        if (buf == NULL) {
            LOGE("Error getting direct buffer address");
            return;
        }
    }
    if (g_decoderHandle == NULL){
        g_decoderHandle= vdecoder_init(COLOR_FORMAT_YUV420);
    }

    int ret = vdecoder_consumeNalUnitsFromDirectBuffer(g_decoderHandle, buf, num_bytes, timeInSec);
    if (ret <0) {
        LOGE("consumeNalUnitsFromDirectBuffer ret %d", ret);
        return;
    }
    if(vdecoder_isFrameReady(g_decoderHandle)) {
        int width = vdecoder_getWidth(g_decoderHandle);
        int height = vdecoder_getHeight(g_decoderHandle);
        int length = vdecoder_getOutputByteSize(g_decoderHandle);

		if (ghRingBuffer) {
			if (getWidth(ghRingBuffer) != width || getHeight(ghRingBuffer) != height) {
					freeRbBuffer(ghRingBuffer);
					ghRingBuffer = NULL;
				}
		}
		if(ghRingBuffer == NULL){
			LOGI("initRbBuffer");
			ghRingBuffer = initRbBuffer (width, height, 4);
		}

		RbFrame* pFrame;
		pFrame = getNextEmptyFrame( ghRingBuffer);
		if(pFrame) {
			unsigned char *pDest = (unsigned char *) pFrame->pBuffer;
			long ret = vdecoder_decodeFrameToDirectBuffer(g_decoderHandle, pDest, length);	
			
			returnBuffer(ghRingBuffer, pFrame);
		}
	
		
    }
}

void updateTexture(unsigned char* buffer, jint width, jint height, jint stride)
{

    unsigned char* yBuffer = buffer;
    unsigned char* uBuffer = buffer + stride * height;
    unsigned char* vBuffer = uBuffer + stride * height/4;

    if(mTexWidth != width || mTexHeight!=height){
        mTexWidth = width;
        mTexHeight = height;


        checkGlError("glGenTextures");
        computeOutputVertices();
    }


    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mTextures[0]); //textid
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,mTexWidth, mTexHeight,
                 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, yBuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
                    GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
                    GL_CLAMP_TO_EDGE);



    /*
    * Prepare the U channel texture
    */
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, mTextures[1]); //textid
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,mTexWidth/2, mTexHeight/2,
                 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, uBuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
                    GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
                    GL_CLAMP_TO_EDGE);
    /*
    * Prepare the V channel texture
    */
    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D, mTextures[2]); //textid
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,mTexWidth/2, mTexHeight/2,
                 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, vBuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
                    GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
                    GL_CLAMP_TO_EDGE);
}

JNIEXPORT void Java_com_breeze_nativelib_NativeLib_step(JNIEnv * env, jobject obj)
{
    if(ghRingBuffer){
        RbFrame* pFrame;

        pFrame = getNextFullFrame( ghRingBuffer);
        if(pFrame) {
            updateTexture((unsigned char *) pFrame->pBuffer, getWidth(ghRingBuffer),
                          getHeight(ghRingBuffer), getWidth(ghRingBuffer));
            returnBuffer(ghRingBuffer, pFrame);
        }
    }

    renderFrame();
}