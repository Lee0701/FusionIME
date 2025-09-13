package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object LayoutCangjie {

    val TABLE_QWERTY = mapOf(
        KeyEvent.KEYCODE_Q to listOf('手'.code, 'Q'.code),
        KeyEvent.KEYCODE_W to listOf('田'.code, 'W'.code),
        KeyEvent.KEYCODE_E to listOf('水'.code, 'E'.code),
        KeyEvent.KEYCODE_R to listOf('口'.code, 'R'.code),
        KeyEvent.KEYCODE_T to listOf('廿'.code, 'T'.code),
        KeyEvent.KEYCODE_Y to listOf('卜'.code, 'Y'.code),
        KeyEvent.KEYCODE_U to listOf('山'.code, 'U'.code),
        KeyEvent.KEYCODE_I to listOf('戈'.code, 'I'.code),
        KeyEvent.KEYCODE_O to listOf('人'.code, 'O'.code),
        KeyEvent.KEYCODE_P to listOf('心'.code, 'P'.code),

        KeyEvent.KEYCODE_A to listOf('日'.code, 'A'.code),
        KeyEvent.KEYCODE_S to listOf('尸'.code, 'S'.code),
        KeyEvent.KEYCODE_D to listOf('木'.code, 'D'.code),
        KeyEvent.KEYCODE_F to listOf('火'.code, 'F'.code),
        KeyEvent.KEYCODE_G to listOf('土'.code, 'G'.code),
        KeyEvent.KEYCODE_H to listOf('竹'.code, 'H'.code),
        KeyEvent.KEYCODE_J to listOf('十'.code, 'J'.code),
        KeyEvent.KEYCODE_K to listOf('大'.code, 'K'.code),
        KeyEvent.KEYCODE_L to listOf('中'.code, 'L'.code),

        KeyEvent.KEYCODE_Z to listOf('重'.code, 'Z'.code),
        KeyEvent.KEYCODE_X to listOf('難'.code, 'X'.code),
        KeyEvent.KEYCODE_C to listOf('金'.code, 'C'.code),
        KeyEvent.KEYCODE_V to listOf('女'.code, 'V'.code),
        KeyEvent.KEYCODE_B to listOf('月'.code, 'B'.code),
        KeyEvent.KEYCODE_N to listOf('弓'.code, 'N'.code),
        KeyEvent.KEYCODE_M to listOf('一'.code, 'M'.code)
    )

    val KEY_MAP_CANGJIE = mapOf(
        '日' to 'a',
        '月' to 'b',
        '金' to 'c',
        '木' to 'd',
        '水' to 'e',
        '火' to 'f',
        '土' to 'g',
        '竹' to 'h',
        '戈' to 'i',
        '十' to 'j',
        '大' to 'k',
        '中' to 'l',
        '一' to 'm',
        '弓' to 'n',
        '人' to 'o',
        '心' to 'p',
        '手' to 'q',
        '口' to 'r',
        '尸' to 's',
        '廿' to 't',
        '山' to 'u',
        '女' to 'v',
        '田' to 'w',
        '難' to 'x',
        '卜' to 'y',
        '重' to 'z'
    )

    val ROWS_DAYI3: List<String> = listOf(
        "言牛目四王門田米足金",
        "石山一工糸火艸木口耳",
        "人革日土手鳥月立女虫",
        "心水鹿禾馬魚雨力舟竹",
    )

    val KEY_MAP_DAYI3 = mapOf(
        '巷' to '`',
        '言' to '1',
        '牛' to '2',
        '目' to '3',
        '四' to '4',
        '王' to '5',
        '門' to '6',
        '田' to '7',
        '米' to '8',
        '足' to '9',
        '金' to '0',
        '郷' to '-',
        '石' to 'Q',
        '山' to 'W',
        '一' to 'E',
        '工' to 'R',
        '糸' to 'T',
        '火' to 'Y',
        '艸' to 'U',
        '木' to 'I',
        '口' to 'O',
        '耳' to 'P',
        '路' to '[',
        '街' to ']',
        '鎮' to '\\',
        '人' to 'A',
        '革' to 'S',
        '日' to 'D',
        '土' to 'F',
        '手' to 'G',
        '鳥' to 'H',
        '月' to 'J',
        '立' to 'K',
        '女' to 'L',
        '虫' to ';',
        '號' to '\'',
        '心' to 'Z',
        '水' to 'X',
        '鹿' to 'C',
        '禾' to 'V',
        '馬' to 'B',
        '魚' to 'N',
        '雨' to 'M',
        '力' to ',',
        '舟' to '.',
        '竹' to '/'
    )

}