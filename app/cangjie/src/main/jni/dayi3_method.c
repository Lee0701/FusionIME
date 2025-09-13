#include <stdio.h>
#include <string.h>
#include "dayi3_method.h"
#include "dayi3.h"
#ifndef X86
#include <android/log.h>
#define  LOG_TAG    "Cangjie"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#define  LOGE(...)
#endif

void dayi3_init(char *path)
{
  int clear = 0;
  int count = 0;

  dayi3_func.mSaved = 0;
  strncpy(dayi3_func.mPath,           path, sizeof(dayi3_func.mPath));
  strncat(dayi3_func.mPath, "/dayi3.dat", sizeof(dayi3_func.mPath));

  for (count = 0; count < sizeof(dayi3_index) / sizeof(jint); count++) {
    dayi3_index[count] = -1;
  }

  FILE *file = fopen(dayi3_func.mPath, "r");
  if (file != 0) {
    int read = fread(dayi3_frequency, 1, sizeof(dayi3_frequency), file);
    fclose(file);
    if (read != sizeof(dayi3_frequency))
      clear = 1;
  } else {
    clear = 1;
  }
   
  if (clear != 0) {
    for (count = 0; count < sizeof(dayi3_frequency) / sizeof(jint); count++) {
      dayi3_frequency[count] = 0;
    }
  }
}

int dayi3_maxKey(void)
{
  return 4;
}

void dayi3_searchWord(jchar key0, jchar key1, jchar key2, jchar key3, jchar key4)
{
  jchar src[4];
  int total = sizeof(dayi3) / (sizeof(jchar) * DAYI3_COLUMN);
  int count = 0;
  int loop  = 0;
  int i = 0;
  int j = 0;
  int found = 0;
  int offset = 0;
  int match = 0;
  int count0 = 0, count1 = 0;

  src[0] = key0;
  src[1] = key1;
  src[2] = key2;
  src[3] = key3;
  
  for (count = 0; count < sizeof(dayi3_index) / sizeof(jint); count++) {
    dayi3_index[count] = 0;
  }

  for (count0 = 0; count0 < total; count0++) {
    if (dayi3[count0][0] != src[0]) { // First code does not matched, skip it
      if (found == 1)
	break;
      continue;
    }

    match = 1;
    for (count1 = 1; count1 < 4; count1++) {
      if (src[count1] == 0)
	break;
      if (dayi3[count0][count1] == src[count1] && (dayi3_func.mEnableHK != 0 || dayi3[count0][7] == 0))
	match = 1;
      else {
	match = 0;
	break;
      }
    }
    /* LOGE("Cangjie : %02x %02x %02x %02x %02x, %02x %02x %02x %02x %02x, Match %d\n", */
    /* 	 cangjie[count0][0], */
    /* 	 cangjie[count0][1], */
    /* 	 cangjie[count0][2], */
    /* 	 cangjie[count0][3], */
    /* 	 cangjie[count0][4], */
    /* 	 key0, */
    /* 	 key1, */
    /* 	 key2, */
    /* 	 key3, */
    /* 	 key4, */
    /* 	 match); */
    if (match != 0) {
      dayi3_index[loop] = count0;
      loop++;
    }

    found = 1;
  }

  dayi3_func.mTotalMatch = loop;
}

jboolean dayi3_tryMatchWord(jchar key0, jchar key1, jchar key2, jchar key3, jchar key4)
{
  jchar src[4];
  int total = sizeof(dayi3) / (sizeof(jchar) * DAYI3_COLUMN);
  int count = 0;
  int loop  = 0;
  int i = 0;
  int j = 0;
  int found = 0;
  int offset = 0;
  int match = 0;
  int count0 = 0, count1 = 0;

  src[0] = key0;
  src[1] = key1;
  src[2] = key2;
  src[3] = key3;
  
  for (count0 = 0; count0 < total; count0++) {
    if (dayi3[count0][0] != src[0]) { // First code does not matched, skip it
      if (found == 1)
	break;
      continue;
    }

    match = 1;
    for (count1 = 1; count1 < 4; count1++) {
      if (src[count1] == 0)
	break;
      if (dayi3[count0][count1] == src[count1] && (dayi3_func.mEnableHK != 0 || dayi3[count0][7] == 0))
	match = 1;
      else {
	match = 0;
	break;
      }
    }
    if (match != 0) {
      loop++;
    }

    found = 1;
  }

  return (loop > 0) ? 1 : 0;
}

int dayi3_totalMatch(void)
{
  return dayi3_func.mTotalMatch;
}

int dayi3_updateFrequency(jchar ch)
{
  int index = 0;

  return dayi3_frequency[index];
}

void dayi3_clearFrequency(void)
{
  int count = 0;
  
  for (count = 0; count < sizeof(dayi3_frequency) / sizeof(jint); count++) {
    dayi3_frequency[count] = 0;
  }

  remove(dayi3_func.mPath);
}

jchar dayi3_getMatchChar(int index)
{
  int total = sizeof(dayi3) / (sizeof(jchar) * DAYI3_COLUMN);

  if (index >= total) return 0;
  if (dayi3_index[index] < 0) return 0;

  return dayi3[dayi3_index[index]][6];
}

void dayi3_reset(void)
{
  dayi3_func.mTotalMatch = 0;
}

void dayi3_saveMatch(void)
{
  if (dayi3_func.mSaved == 0) return;
  dayi3_func.mSaved = 0;
  FILE *file = fopen(dayi3_func.mPath, "w");
  if (file != NULL) {
    fwrite(dayi3_frequency, 1, sizeof(dayi3_frequency), file);
    fclose(file);
  }
}

void dayi3_enableHongKongChar(jboolean hk)
{
  dayi3_func.mEnableHK = (hk != 0);
}

struct _input_method dayi3_func =
{
  .init            = dayi3_init,
  .maxKey          = dayi3_maxKey,
  .searchWord      = dayi3_searchWord,
  .tryMatchWord    = dayi3_tryMatchWord,
  .enableHongKongChar = dayi3_enableHongKongChar,
  .totalMatch      = dayi3_totalMatch,
  .updateFrequency = dayi3_updateFrequency,
  .clearFrequency  = dayi3_clearFrequency,
  .getMatchChar    = dayi3_getMatchChar,
  .reset           = dayi3_reset,
  .saveMatch       = dayi3_saveMatch
};
