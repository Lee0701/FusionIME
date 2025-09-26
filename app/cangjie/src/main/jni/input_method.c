#include "quick_method.h"
#include "cangjie_method.h"
#include "dayi3_method.h"

struct _input_method *input_method[4] = {
  &quick_func,
  &cangjie_func,
  &dayi3_func,
  0
};

