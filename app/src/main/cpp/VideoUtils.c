#include "VideoUtils.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <sys/time.h>

void NV21ToNV12(jbyte *nv21, int width, int height) {
    int framesize = width * height;
    int j = 0;
    int end = framesize + framesize / 2;
    jbyte temp = 0;
    for (j = framesize; j < end; j += 2)//u
    {
        temp = nv21[j];
        nv21[j] = nv21[j + 1];
        nv21[j + 1] = temp;
    }

}

const int mark_size = 8;

int g_mark_off_x[mark_size] = {0};
int g_mark_off_y[mark_size] = {0};

int g_mark_width[mark_size] = {0};
int g_mark_height[mark_size] = {0};

jbyte *g_mark_value[mark_size];

int rotation;
int frame_width, frame_height;

const uint16_t ascii[][32] = {
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//0
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//1
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//2
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//3
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//4
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//5
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//6
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//7
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//8
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//9
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//10
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//11
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//12
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//13
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//14
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//15
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//16
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//17
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//18
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//19
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//20
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//21
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//22
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//23
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//24
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//25
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//26
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//27
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//28
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//29
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//30
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//31
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},//32
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x03, 0xC0, 0x03, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x00, 0x80, 0x00, 0x00, 0x03, 0xC0, 0x03, 0xC0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x03, 0x9C, 0x07, 0xB8, 0x0E, 0xF0, 0x19, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x18, 0x0C, 0x18, 0x3F, 0xFE, 0x7F, 0xFE, 0x0C, 0x18, 0x0C, 0x18, 0x0C, 0x10, 0x7F, 0xFE, 0x3F, 0xFE, 0x18, 0x30, 0x18, 0x30, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x01, 0x80, 0x07, 0xF0, 0x1D, 0x9C, 0x19, 0xBC, 0x1F, 0x80, 0x0F, 0xC0, 0x01, 0xF0, 0x01, 0xF8, 0x19, 0x9C, 0x3D, 0x9C, 0x19, 0x98, 0x0F, 0xF0, 0x01, 0x80, 0x00, 0x80},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x18, 0x66, 0x10, 0xE7, 0x20, 0xE7, 0x60, 0x66, 0xC0, 0x3D, 0xBC, 0x03, 0x66, 0x02, 0x63, 0x04, 0xE3, 0x0C, 0x67, 0x18, 0x3E, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xC0, 0x18, 0xC0, 0x18, 0xC0, 0x19, 0xC0, 0x1F, 0x3C, 0x3E, 0x3C, 0x67, 0x18, 0xE3, 0xB0, 0xE1, 0xE0, 0x70, 0xF3, 0x3F, 0xBE, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x1C, 0x00, 0x1E, 0x00, 0x0C, 0x00, 0x38, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x06, 0x00, 0x1C, 0x00, 0x30, 0x00, 0x60, 0x00, 0xC0, 0x01, 0xC0, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0xC0, 0x00, 0xC0, 0x00, 0x60, 0x00, 0x38, 0x00, 0x0C, 0x00, 0x02},
        {0x00, 0x00, 0x30, 0x00, 0x18, 0x00, 0x0E, 0x00, 0x07, 0x00, 0x03, 0x80, 0x01, 0x80, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0x80, 0x01, 0x80, 0x03, 0x00, 0x06, 0x00, 0x0C, 0x00, 0x38, 0x00, 0x20, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xC0, 0x01, 0xC0, 0x79, 0x9F, 0x3F, 0xFC, 0x07, 0xF0, 0x3F, 0xBE, 0x31, 0xCE, 0x01, 0xC0, 0x01, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x3F, 0xFE, 0x3F, 0xFE, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x3C, 0x00, 0x1E, 0x00, 0x0C, 0x00, 0x38, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xFE, 0x7F, 0xFE, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3C, 0x00, 0x1C, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x02, 0x00, 0x06, 0x00, 0x0C, 0x00, 0x18, 0x00, 0x30, 0x00, 0x60, 0x00, 0xC0, 0x01, 0x80, 0x03, 0x00, 0x06, 0x00, 0x0C, 0x00, 0x18, 0x00, 0x30, 0x00, 0x20, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xF0, 0x1C, 0x18, 0x38, 0x1C, 0x38, 0x0E, 0x38, 0x0E, 0x78, 0x0E, 0x38, 0x0E, 0x38, 0x0E, 0x38, 0x1C, 0x1C, 0x18, 0x07, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xC0, 0x0F, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x0F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xF8, 0x18, 0x1C, 0x38, 0x1C, 0x18, 0x1C, 0x00, 0x38, 0x00, 0x70, 0x01, 0xC0, 0x07, 0x00, 0x0C, 0x06, 0x3F, 0xFC, 0x3F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xF0, 0x38, 0x38, 0x38, 0x1C, 0x00, 0x38, 0x03, 0xF0, 0x03, 0xF0, 0x00, 0x1C, 0x00, 0x0E, 0x38, 0x1C, 0x38, 0x1C, 0x1F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x70, 0x00, 0xF0, 0x01, 0xF0, 0x03, 0x70, 0x0C, 0x70, 0x18, 0x70, 0x30, 0x70, 0x3F, 0xFE, 0x00, 0x70, 0x00, 0x70, 0x03, 0xFE, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0xFC, 0x18, 0x00, 0x18, 0x00, 0x18, 0xC0, 0x1F, 0xF8, 0x18, 0x1C, 0x00, 0x1E, 0x10, 0x0E, 0x38, 0x1C, 0x38, 0x18, 0x0F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xF8, 0x0C, 0x3C, 0x18, 0x08, 0x38, 0x00, 0x3F, 0xF8, 0x7C, 0x1C, 0x78, 0x0E, 0x38, 0x0E, 0x38, 0x0E, 0x1C, 0x1C, 0x07, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0xFE, 0x3C, 0x0C, 0x30, 0x18, 0x00, 0x30, 0x00, 0x60, 0x00, 0xC0, 0x01, 0xC0, 0x03, 0x80, 0x03, 0x80, 0x03, 0x80, 0x03, 0x80, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xF8, 0x38, 0x0C, 0x30, 0x0E, 0x3C, 0x0C, 0x1F, 0xF8, 0x0F, 0xF0, 0x38, 0x3C, 0x30, 0x0E, 0x70, 0x0E, 0x38, 0x0C, 0x0F, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xF0, 0x38, 0x18, 0x30, 0x0C, 0x70, 0x0E, 0x30, 0x0E, 0x38, 0x3E, 0x1F, 0xEE, 0x00, 0x1C, 0x18, 0x1C, 0x3C, 0x38, 0x1F, 0xE0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xC0, 0x03, 0xC0, 0x01, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xC0, 0x03, 0xC0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x80, 0x03, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xC0, 0x03, 0xC0, 0x01, 0x80, 0x01, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x1C, 0x00, 0x70, 0x01, 0xC0, 0x07, 0x00, 0x1C, 0x00, 0x38, 0x00, 0x0E, 0x00, 0x03, 0x80, 0x00, 0xE0, 0x00, 0x38, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xFE, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1C, 0x00, 0x07, 0x00, 0x01, 0xC0, 0x00, 0x70, 0x00, 0x1C, 0x00, 0x0C, 0x00, 0x38, 0x00, 0xE0, 0x03, 0x80, 0x0E, 0x00, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x01, 0xC0, 0x0F, 0xF8, 0x30, 0x0C, 0x38, 0x0E, 0x38, 0x0E, 0x00, 0x3C, 0x00, 0xF0, 0x01, 0x80, 0x01, 0x80, 0x01, 0x00, 0x03, 0xC0, 0x03, 0xC0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xF8, 0x1C, 0x0E, 0x31, 0xFA, 0x33, 0x3B, 0x76, 0x33, 0x76, 0x33, 0x76, 0x72, 0x36, 0xFC, 0x3B, 0xBE, 0x1C, 0x0E, 0x07, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xC0, 0x03, 0xC0, 0x06, 0xE0, 0x06, 0xE0, 0x0C, 0x70, 0x0C, 0x70, 0x0F, 0xF8, 0x18, 0x38, 0x10, 0x1C, 0x30, 0x1C, 0xFC, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xF8, 0x18, 0x1C, 0x18, 0x1E, 0x18, 0x1C, 0x1F, 0xF8, 0x1F, 0xFC, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x1E, 0xFF, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xFE, 0x1C, 0x0E, 0x38, 0x02, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x02, 0x38, 0x02, 0x1C, 0x0C, 0x07, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xF0, 0x18, 0x3C, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x0F, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x0C, 0x18, 0x38, 0x7F, 0xE0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xFC, 0x1C, 0x0E, 0x1C, 0x02, 0x1C, 0x10, 0x1F, 0xF0, 0x1F, 0xF0, 0x1C, 0x10, 0x1C, 0x00, 0x1C, 0x03, 0x1C, 0x0E, 0x7F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xFE, 0x1C, 0x06, 0x1C, 0x01, 0x1C, 0x18, 0x1C, 0x38, 0x1F, 0xF8, 0x1C, 0x18, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x7F, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xF8, 0x1C, 0x1C, 0x38, 0x0C, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x3F, 0x70, 0x1C, 0x38, 0x1C, 0x1C, 0x1C, 0x0F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7C, 0x3F, 0x38, 0x1C, 0x38, 0x1C, 0x38, 0x1C, 0x38, 0x1C, 0x3F, 0xFC, 0x38, 0x1C, 0x38, 0x1C, 0x38, 0x1C, 0x38, 0x1C, 0xFE, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0xF8, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x1F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xFE, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x00, 0x70, 0x30, 0x70, 0x78, 0xE0, 0x3F, 0x80},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7E, 0x3E, 0x18, 0x30, 0x18, 0x60, 0x19, 0xC0, 0x1B, 0x80, 0x1F, 0xC0, 0x18, 0xE0, 0x18, 0x70, 0x18, 0x38, 0x18, 0x1C, 0x7E, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7E, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x03, 0x1C, 0x06, 0x7F, 0xFE, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x78, 0x1F, 0x38, 0x1E, 0x3C, 0x3E, 0x3C, 0x3E, 0x2C, 0x6E, 0x2E, 0x6E, 0x26, 0xCE, 0x27, 0xCE, 0x23, 0x8E, 0x23, 0x8E, 0xFB, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x78, 0x1F, 0x3C, 0x04, 0x3E, 0x04, 0x37, 0x04, 0x33, 0x84, 0x31, 0xC4, 0x30, 0xE4, 0x30, 0x74, 0x30, 0x1C, 0x30, 0x0C, 0xFC, 0x04, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xF0, 0x1C, 0x1C, 0x38, 0x0E, 0x70, 0x0E, 0x70, 0x07, 0x70, 0x07, 0x70, 0x07, 0x30, 0x0E, 0x38, 0x0E, 0x1C, 0x1C, 0x07, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0xF8, 0x1C, 0x0E, 0x1C, 0x0E, 0x1C, 0x0E, 0x1C, 0x1E, 0x1F, 0xF8, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x1C, 0x00, 0x7F, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xF0, 0x1C, 0x1C, 0x38, 0x0E, 0x70, 0x0E, 0x70, 0x07, 0x70, 0x07, 0x70, 0x07, 0x73, 0x8E, 0x3F, 0xEE, 0x1C, 0x7C, 0x0F, 0xFA, 0x00, 0x3E, 0x00, 0x08},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3F, 0xF8, 0x1C, 0x1C, 0x1C, 0x0E, 0x1C, 0x0C, 0x1C, 0x7C, 0x1F, 0xE0, 0x1C, 0xE0, 0x1C, 0x70, 0x1C, 0x38, 0x1C, 0x1C, 0x7E, 0x0F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xFC, 0x38, 0x1C, 0x30, 0x04, 0x38, 0x00, 0x1F, 0x80, 0x03, 0xF0, 0x00, 0x7C, 0x20, 0x0E, 0x20, 0x0E, 0x38, 0x0C, 0x3F, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3F, 0xFE, 0x61, 0xC6, 0x41, 0xC2, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x0F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7C, 0x1E, 0x38, 0x04, 0x38, 0x04, 0x38, 0x04, 0x38, 0x04, 0x38, 0x04, 0x38, 0x04, 0x38, 0x04, 0x38, 0x04, 0x1C, 0x18, 0x0F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7C, 0x1E, 0x18, 0x0C, 0x1C, 0x18, 0x1C, 0x10, 0x0E, 0x30, 0x0E, 0x20, 0x07, 0x60, 0x07, 0x40, 0x03, 0xC0, 0x03, 0x80, 0x01, 0x80, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7B, 0xCF, 0x31, 0xC6, 0x31, 0xC4, 0x39, 0xCC, 0x3B, 0xEC, 0x1B, 0xF8, 0x1F, 0x78, 0x1E, 0x78, 0x0E, 0x70, 0x0C, 0x30, 0x0C, 0x30, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x3E, 0x0C, 0x18, 0x0E, 0x30, 0x07, 0x60, 0x03, 0xC0, 0x01, 0xC0, 0x03, 0xE0, 0x06, 0x70, 0x0C, 0x38, 0x18, 0x1C, 0x7C, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7C, 0x3E, 0x1C, 0x18, 0x1C, 0x10, 0x0E, 0x30, 0x07, 0x60, 0x03, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x0F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0xFE, 0x38, 0x1C, 0x20, 0x38, 0x00, 0x70, 0x00, 0xE0, 0x01, 0xC0, 0x03, 0x80, 0x06, 0x00, 0x1C, 0x06, 0x38, 0x0C, 0x7F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x01, 0xFC, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0xFC, 0x01, 0xFC},
        {0x00, 0x00, 0x00, 0x00, 0x18, 0x00, 0x18, 0x00, 0x0C, 0x00, 0x06, 0x00, 0x03, 0x00, 0x03, 0x80, 0x01, 0xC0, 0x00, 0xC0, 0x00, 0x60, 0x00, 0x30, 0x00, 0x38, 0x00, 0x18, 0x00, 0x0C, 0x00, 0x06},
        {0x00, 0x00, 0x1F, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0, 0x1F, 0xC0, 0x1F, 0xC0},
        {0x00, 0x00, 0x03, 0xE0, 0x0E, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF},
        {0x00, 0x00, 0x1F, 0x00, 0x03, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xE0, 0x1E, 0x38, 0x38, 0x1C, 0x07, 0xFC, 0x1E, 0x1C, 0x30, 0x1C, 0x30, 0x3D, 0x1F, 0xFE, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x78, 0x00, 0x18, 0x00, 0x18, 0x00, 0x19, 0xF0, 0x1F, 0xFC, 0x1C, 0x0C, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x1C, 0x1F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE0, 0x0F, 0x38, 0x18, 0x1C, 0x38, 0x00, 0x38, 0x00, 0x38, 0x04, 0x1C, 0x0C, 0x0F, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3C, 0x00, 0x1C, 0x00, 0x1C, 0x03, 0xDC, 0x0E, 0x7C, 0x38, 0x1C, 0x38, 0x1C, 0x38, 0x1C, 0x38, 0x1C, 0x1C, 0x3C, 0x0F, 0xFE, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE0, 0x0F, 0x38, 0x18, 0x0C, 0x3F, 0xFE, 0x3F, 0xFC, 0x38, 0x04, 0x1C, 0x0C, 0x07, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xFE, 0x03, 0x07, 0x07, 0x00, 0x3F, 0xF8, 0x3F, 0xF8, 0x07, 0x00, 0x07, 0x00, 0x07, 0x00, 0x07, 0x00, 0x07, 0x00, 0x3F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xEE, 0x0E, 0x7E, 0x18, 0x18, 0x18, 0x18, 0x0F, 0xF0, 0x19, 0x80, 0x1F, 0xFC, 0x38, 0x3E, 0x30, 0x0E, 0x1F, 0xF8},
        {0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x78, 0x00, 0x18, 0x00, 0x18, 0x00, 0x19, 0xF0, 0x1F, 0xF8, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x7E, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xC0, 0x01, 0xC0, 0x00, 0x00, 0x0F, 0xC0, 0x0F, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x1F, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3C, 0x00, 0x38, 0x00, 0x00, 0x01, 0xF8, 0x01, 0xF8, 0x00, 0x18, 0x00, 0x18, 0x00, 0x18, 0x00, 0x18, 0x00, 0x18, 0x00, 0x18, 0x18, 0x30, 0x1F, 0xE0},
        {0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x38, 0x00, 0x18, 0x00, 0x18, 0x00, 0x18, 0x7C, 0x18, 0x7C, 0x18, 0xE0, 0x1B, 0xC0, 0x1E, 0xE0, 0x18, 0x70, 0x18, 0x38, 0x7E, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x0F, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x01, 0xC0, 0x1F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x77, 0x1C, 0x7F, 0xFE, 0x31, 0xC6, 0x31, 0xC6, 0x31, 0xC6, 0x31, 0xC6, 0x31, 0xC6, 0xFB, 0xFF, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x79, 0xF0, 0x7F, 0xF8, 0x1C, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x7E, 0x3F, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE0, 0x0E, 0x78, 0x38, 0x0C, 0x30, 0x0E, 0x70, 0x0E, 0x38, 0x0E, 0x1C, 0x1C, 0x0F, 0xF0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x79, 0xE0, 0x7F, 0xFC, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x0E, 0x18, 0x0E, 0x1C, 0x1C, 0x1F, 0xF8, 0x18, 0x00, 0x7E, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xC0, 0x1E, 0x7C, 0x38, 0x1C, 0x38, 0x1C, 0x30, 0x1C, 0x38, 0x1C, 0x18, 0x1C, 0x0F, 0xFC, 0x00, 0x1C, 0x00, 0x3E},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7E, 0x3C, 0x7E, 0xFE, 0x0F, 0x04, 0x0E, 0x00, 0x0E, 0x00, 0x0E, 0x00, 0x0E, 0x00, 0x7F, 0xE0, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE8, 0x0E, 0x7C, 0x18, 0x0C, 0x0F, 0x80, 0x03, 0xF8, 0x10, 0x1C, 0x18, 0x0C, 0x1F, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x03, 0x00, 0x3F, 0xF8, 0x3F, 0xF8, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x04, 0x03, 0x0C, 0x01, 0xF8, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x78, 0x3C, 0x78, 0x3C, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x1C, 0x18, 0x3C, 0x0F, 0xFE, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x1E, 0x3E, 0x1E, 0x0C, 0x18, 0x0E, 0x30, 0x07, 0x20, 0x03, 0x60, 0x03, 0xC0, 0x01, 0x80, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7B, 0xCF, 0x7B, 0xCF, 0x39, 0xCC, 0x39, 0xCC, 0x1B, 0xE8, 0x1E, 0x78, 0x0E, 0x70, 0x0C, 0x30, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x3C, 0x3E, 0x3C, 0x07, 0x30, 0x03, 0xC0, 0x01, 0xC0, 0x07, 0x70, 0x0C, 0x38, 0x7E, 0x7E, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x3E, 0x3E, 0x3E, 0x0E, 0x18, 0x06, 0x30, 0x07, 0x60, 0x03, 0xE0, 0x01, 0xC0, 0x01, 0x80, 0x19, 0x00, 0x3E, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0xF8, 0x3F, 0xF8, 0x30, 0x70, 0x01, 0xC0, 0x03, 0x80, 0x07, 0x04, 0x1C, 0x0C, 0x3F, 0xFC, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x1C, 0x00, 0x20, 0x00, 0x60, 0x00, 0x60, 0x00, 0x60, 0x00, 0x60, 0x00, 0xE0, 0x01, 0xC0, 0x00, 0x60, 0x00, 0x60, 0x00, 0x60, 0x00, 0x60, 0x00, 0x60, 0x00, 0x38, 0x00, 0x1C},
        {0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80, 0x01, 0x80},
        {0x00, 0x00, 0x3C, 0x00, 0x06, 0x00, 0x06, 0x00, 0x06, 0x00, 0x06, 0x00, 0x06, 0x00, 0x03, 0x00, 0x03, 0x80, 0x06, 0x00, 0x06, 0x00, 0x06, 0x00, 0x06, 0x00, 0x06, 0x00, 0x0E, 0x00, 0x18, 0x00},
        {0x0E, 0x00, 0x3F, 0x82, 0x61, 0xE6, 0x00, 0x7C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
};

JNIEXPORT void JNICALL
Java_com_cmccpoc_video_YuvWaterMark_init(JNIEnv *env, jclass type, jint frameWidth,
                                         jint frameHeight, jint rotation_angle) {

    frame_width = frameWidth;
    frame_height = frameHeight;
    rotation = rotation_angle;
};

//int getIndex(jchar c) {
//    if (c >= 32 && c <= 128)
//        return c;
//    else
//        return 32;
//}
//
//void printfArr(char *arrs, int width, int height) {
//    for (int i = 0; i < height; i++) {
//        char line[width + 1];
//        for (int j = 0; j < width; j++) {
//            line[j] = '0' + *(arrs + i * width + j);
//        }
//        line[width] = '\0';
//        LOGD("%s", line);
//    }
//}

JNIEXPORT void JNICALL
Java_com_cmccpoc_video_YuvWaterMark_addMark(
        JNIEnv *env, jclass type, jbyteArray yuv_in_data, jbyteArray yvu_out_data) {

    jbyte *nv21Src = (*env)->GetByteArrayElements(env, yuv_in_data, NULL);
    jbyte *destData = (*env)->GetByteArrayElements(env, yvu_out_data, NULL);

//    LOGD("date_size=%d date=%d %s",date_size,date_len,date);
    int frame_size = frame_width * frame_height;

    int frameW;
    if (rotation == 0) {//不旋转
        memcpy(destData, nv21Src, frame_size);//copy y数据

        int end = frame_size + frame_size / 2;
        for (int j = frame_size; j < end; j += 2) {//copy uv
            destData[j] = nv21Src[j + 1];
            destData[j + 1] = nv21Src[j];
        }

        frameW = frame_width;
    } else if (rotation == 90) {//顺时针旋转90
        int k = 0;
        for (int i = 0; i < frame_width; i++) {//旋转Y
            for (int j = frame_height - 1; j >= 0; j--) {
                destData[k++] = nv21Src[j * frame_width + i];
            }
        }
        //旋转UV
        int uvHeight = frame_height >> 1;

        for (int i = 0; i < frame_width; i += 2) {
            for (int j = uvHeight - 1; j >= 0; j--) {
                destData[k] = nv21Src[frame_size + frame_width * j + i + 1];
                destData[k + 1] = nv21Src[frame_size + frame_width * j + i];
                k += 2;
            }
        }
        frameW = frame_height;
    } else if (rotation == 270) {//顺时针旋转270,=逆方向90
        int k = 0;
        for (int i = frame_width - 1; i >= 0; i--) {//旋转Y
            for (int j = 0; j < frame_height; j++) {
                destData[k++] = nv21Src[j * frame_width + i];
            }
        }
        //旋转UV
        int uvHeight = frame_height >> 1;

        for (int i = frame_width - 1; i >= 0; i -= 2) {
            for (int j = 0; j < uvHeight; j++) {
                destData[k] = nv21Src[frame_size + frame_width * j + i];
                destData[k + 1] = nv21Src[frame_size + frame_width * j + i - 1];
                k += 2;
            }
        }
        frameW = frame_height;
    } else {
        frameW = frame_height;
    }

//    //添加时间水印
//    uint16_t mask = 0x8000;
//    uint16_t temp;
//    jbyte *dest = destData;
//    jbyte *start = dest + off_y * frameW;
//    for (int i = 0; i < date_len; i++) {
//        int index = getIndex(*(date + i));
////        char *num = mNumArrays + size * index;
//        jbyte *column = start + i * num_width;
////        if (!num)
////            continue;
//        for (int j = 0; j < num_height; j++) {
//            jbyte *destIndex = column + j * frameW + off_x;
//            temp = ascii[index][j * 2] << 8 | ascii[index][j * 2 + 1];
////            char *src = num + j * num_width;
//            for (int k = 0; k < num_width; k++) {
////                if (*(src + k) != 0) {//黑色背景色
//                if (mask & temp) {
//                    *(destIndex + k) = -1;//水印文字颜色，-21为白色，0为黑色
//                }
//                mask = mask >> 1;
//                if (mask == 0) {
//                    mask = 0x8000;
//                }
////                }
//            }
//        }
//    }
//    //添加文字水印
//    int index = getIndex(*(date + i));
//    char *num = mNumArrays + size * index;
//    jbyte *column = start + i * num_width;
//    for (int j = 0; j < num_height; j++) {
//        jbyte *destIndex = column + j * frameW + off_x;
//        temp = ascii[index][j * 2] << 8 | ascii[index][j * 2 + 1];
//        char *src = num + j * num_width;
//        for (int k = 0; k < num_width; k++) {
//            if (*(src + k) != 0) {//黑色背景色
//                *(destIndex + k) = -1;//水印文字颜色，-21为白色，0为黑色
//            }
//        }
//    }


    jbyte *dest = destData;
    for (int k = 0; k < mark_size; k++) {
        int mark_off_x = g_mark_off_x[k];
        int mark_off_y = g_mark_off_y[k];
        int mark_width = g_mark_width[k];
        int mark_height = g_mark_height[k];
        jbyte *mark_value = g_mark_value[k];

        if (mark_width != 0 && mark_height != 0) {
            jbyte *start = dest + mark_off_y * frameW;
            for (int i = 0; i < mark_height; i++) {
                jbyte *column = start + i * frameW;
                jbyte *src = mark_value + i * mark_width;
                jbyte *destIndex = column + mark_off_x;
                for (int j = 0; j < mark_width; j++) {
                    if (*(src + j) != 16) {//黑色背景色
                        *(destIndex + j) = -1;//水印文字颜色，-1为白色，0为黑色
                    }
                }
            }
        }
    }

    (*env)->ReleaseByteArrayElements(env, yuv_in_data, nv21Src, 0);
    (*env)->ReleaseByteArrayElements(env, yvu_out_data, destData, 0);
}


JNIEXPORT jbyteArray JNICALL
Java_com_cmccpoc_video_YuvWaterMark_argbIntToNV21Byte(JNIEnv *env, jclass jclazz, jintArray ints,
                                                      jint width, jint height) {
    int frameSize = width * height;
    jint *argb = (*env)->GetIntArrayElements(env, ints, NULL);
    int resLength = frameSize * 3 / 2;
    jbyte *yuv420sp = (jbyte *) malloc(resLength + 1 * sizeof(jbyte));
    int yIndex = 0;
    int uvIndex = frameSize;
    int a, R, G, B, Y, U, V;
    int index = 0;
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
            R = (argb[index] & 0xff0000) >> 16;
            G = (argb[index] & 0xff00) >> 8;
            B = argb[index] & 0xff;

            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
            U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
            V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

            yuv420sp[yIndex++] = (jbyte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (jbyte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                yuv420sp[uvIndex++] = (jbyte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
            }
            index++;
        }
    }
    (*env)->ReleaseIntArrayElements(env, ints, argb, JNI_ABORT);
    jbyteArray res = (*env)->NewByteArray(env, resLength);
    (*env)->SetByteArrayRegion(env, res, 0, resLength, yuv420sp);

    free(yuv420sp);
    return res;
}

JNIEXPORT jbyteArray JNICALL
Java_com_cmccpoc_video_YuvWaterMark_argbIntToNV12Byte(JNIEnv *env, jclass jclazz, jintArray ints,
                                                      jint width, jint height) {
    int frameSize = width * height;
    jint *argb = (*env)->GetIntArrayElements(env, ints, NULL);
    int resLength = frameSize * 3 / 2;
    jbyte *yuv420sp = (jbyte *) malloc(resLength + 1 * sizeof(jbyte));
    int yIndex = 0;
    int uvIndex = frameSize;
    int a, R, G, B, Y, U, V;
    int index = 0;
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
            R = (argb[index] & 0xff0000) >> 16;
            G = (argb[index] & 0xff00) >> 8;
            B = argb[index] & 0xff;

            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
            U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
            V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

            yuv420sp[yIndex++] = (jbyte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (jbyte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                yuv420sp[uvIndex++] = (jbyte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
            }
            index++;
        }
    }
    (*env)->ReleaseIntArrayElements(env, ints, argb, JNI_ABORT);
    jbyteArray res = (*env)->NewByteArray(env, resLength);
    (*env)->SetByteArrayRegion(env, res, 0, resLength, yuv420sp);

    free(yuv420sp);
    return res;
}

JNIEXPORT jbyteArray JNICALL
Java_com_cmccpoc_video_YuvWaterMark_argbIntToGrayNVByte(JNIEnv *env, jclass jclazz,
                                                        jintArray ints,
                                                        jint width, jint height) {
    int frameSize = width * height;
    jint *argb = (*env)->GetIntArrayElements(env, ints, NULL);
    int resLength = frameSize;
    jbyte *yuv420sp = (jbyte *) malloc(resLength + 1 * sizeof(jbyte));
    int yIndex = 0;
    int R, G, B, Y;
    int index = 0;
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            R = (argb[index] & 0xff0000) >> 16;
            G = (argb[index] & 0xff00) >> 8;
            B = argb[index] & 0xff;

            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;

            yuv420sp[yIndex++] = (jbyte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));

            index++;
        }
    }
    (*env)->ReleaseIntArrayElements(env, ints, argb, JNI_ABORT);
    jbyteArray res = (*env)->NewByteArray(env, resLength);
    (*env)->SetByteArrayRegion(env, res, 0, resLength, yuv420sp);

    free(yuv420sp);
    return res;
}

JNIEXPORT void JNICALL
Java_com_cmccpoc_video_YuvWaterMark_nv21ToNv12(JNIEnv *env, jclass type, jbyteArray nv21Src_,
                                               jbyteArray nv12Dest_, jint width, jint height) {
    jbyte *nv21Src = (*env)->GetByteArrayElements(env, nv21Src_, NULL);
    jbyte *nv12Dest = (*env)->GetByteArrayElements(env, nv12Dest_, NULL);

    int frame_size = width * height;
    int end = frame_size + frame_size / 2;
    memcpy(nv21Src, nv12Dest, frame_size);

    for (int j = frame_size; j < end; j += 2)//u
    {
        nv12Dest[j] = nv21Src[j + 1];
        nv12Dest[j + 1] = nv21Src[j];
    }
    (*env)->ReleaseByteArrayElements(env, nv21Src_, nv21Src, 0);
    (*env)->ReleaseByteArrayElements(env, nv12Dest_, nv12Dest, 0);
}

JNIEXPORT void JNICALL
Java_com_cmccpoc_video_YuvWaterMark_release(JNIEnv *env, jclass type) {
//    free(mNumArrays);
}

JNIEXPORT void JNICALL
Java_com_cmccpoc_video_YuvWaterMark_setWaterMarkValueByte(JNIEnv *env, jclass clazz, jint index,
                                                          jint off_x, jint off_y, jint mark_width,
                                                          jint mark_height,
                                                          jbyteArray mark_value) {
    if (index < mark_size) {
        g_mark_off_x[index] = off_x;
        g_mark_off_y[index] = off_y;
        g_mark_value[index] = (*env)->GetByteArrayElements(env, mark_value, NULL);
        g_mark_width[index] = mark_width;
        g_mark_height[index] = mark_height;
    }
}

JNIEXPORT void JNICALL
Java_com_cmccpoc_video_YuvWaterMark_resetWaterMarkValueByte(JNIEnv *env, jclass clazz,
                                                            jint index) {
    if (index < mark_size) {
        g_mark_off_x[index] = 0;
        g_mark_off_y[index] = 0;
        g_mark_value[index] = NULL;
        g_mark_width[index] = 0;
        g_mark_height[index] = 0;
    }
}
