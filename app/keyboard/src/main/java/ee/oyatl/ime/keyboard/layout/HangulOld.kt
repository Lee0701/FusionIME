package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object HangulOld {
    val TABLE_OLD_2SET = mapOf(
        KeyEvent.KEYCODE_Q to listOf(0x3142, 0x3143),
        KeyEvent.KEYCODE_W to listOf(0x3148, 0x3149),
        KeyEvent.KEYCODE_E to listOf(0x3137, 0x3138),
        KeyEvent.KEYCODE_R to listOf(0x3131, 0x3132),
        KeyEvent.KEYCODE_T to listOf(0x3145, 0x3146),
        KeyEvent.KEYCODE_Y to listOf(0x315b, 0x302f),
        KeyEvent.KEYCODE_U to listOf(0x3155, 0x302e),
        KeyEvent.KEYCODE_I to listOf(0x3151, 0x3151),
        KeyEvent.KEYCODE_O to listOf(0x3150, 0x3152),
        KeyEvent.KEYCODE_P to listOf(0x3154, 0x3156),

        KeyEvent.KEYCODE_A to listOf(0x3141, 0x317f),
        KeyEvent.KEYCODE_S to listOf(0x3134, 0x3136),
        KeyEvent.KEYCODE_D to listOf(0x3147, 0x3181),
        KeyEvent.KEYCODE_F to listOf(0x3139, 0x3140),
        KeyEvent.KEYCODE_G to listOf(0x314e, 0x3186),
        KeyEvent.KEYCODE_H to listOf(0x3157, 0x1183),
        KeyEvent.KEYCODE_J to listOf(0x3153, 0x3164),
        KeyEvent.KEYCODE_K to listOf(0x314f, 0x318d),
        KeyEvent.KEYCODE_L to listOf(0x3163, 0x318c),

        KeyEvent.KEYCODE_Z to listOf(0x314b, 0x113c),
        KeyEvent.KEYCODE_X to listOf(0x314c, 0x113e),
        KeyEvent.KEYCODE_C to listOf(0x314a, 0x114e),
        KeyEvent.KEYCODE_V to listOf(0x314d, 0x1150),
        KeyEvent.KEYCODE_B to listOf(0x3160, 0x1154),
        KeyEvent.KEYCODE_N to listOf(0x315c, 0x1155),
        KeyEvent.KEYCODE_M to listOf(0x3161, 0x3161)
    )

    val COMB_FULL = mapOf(
        0x1100 to 0x1100 to 0x1101, /* choseong kiyeok + kiyeok          = ssangkiyeok */
        0x1100 to 0x1103 to 0x115a, /* choseong kiyeok + tikeut          = kiyeok-tikeut */
        0x1102 to 0x1100 to 0x1113, /* choseong nieun + kiyeok           = nieun-kiyeok */
        0x1102 to 0x1102 to 0x1114, /* choseong nieun + nieun            = ssangnieun */
        0x1102 to 0x1103 to 0x1115, /* choseong nieun + tikeut           = nieun-tikeut */
        0x1102 to 0x1107 to 0x1116, /* choseong nieun + pieup            = nieun-pieup */
        0x1102 to 0x1109 to 0x115b, /* choseong nieun + sios             = nieun-sios */
        0x1102 to 0x110c to 0x115c, /* choseong nieun + cieuc            = nieun-cieuc */
        0x1102 to 0x1112 to 0x115d, /* choseong nieun + hieuh            = nieun-hieuh */
        0x1103 to 0x1100 to 0x1117, /* choseong tikeut + kiyeok          = tikeut-kiyeok */
        0x1103 to 0x1103 to 0x1104, /* choseong tikeut + tikeut          = ssangtikeut */
        0x1103 to 0x1105 to 0x115e, /* choseong tikeut + rieul           = tikeut-rieul */
        0x1103 to 0x1106 to 0xa960, /* choseong tikeut + mieum           = tikeut-mieum */
        0x1103 to 0x1107 to 0xa961, /* choseong tikeut + pieup           = tikeut-pieup */
        0x1103 to 0x1109 to 0xa962, /* choseong tikeut + sios            = tikeut-sios */
        0x1103 to 0x110c to 0xa963, /* choseong tikeut + cieuc           = tikeut-cieuc */
        0x1105 to 0x1100 to 0xa964, /* choseong rieul + kiyeok           = rieul-kiyeok */
        0x1105 to 0x1101 to 0xa965, /* choseong rieul + ssangkiyeok      = rieul-ssangkiyeok */
        0x1105 to 0x1102 to 0x1118, /* choseong rieul + nieun            = rieul-nieun */
        0x1105 to 0x1103 to 0xa966, /* choseong rieul + tikeut           = rieul-tikeut */
        0x1105 to 0x1104 to 0xa967, /* choseong rieul + ssangtikeut      = rieul-ssangtikeut */
        0x1105 to 0x1105 to 0x1119, /* choseong rieul + rieul            = ssangrieul */
        0x1105 to 0x1106 to 0xa968, /* choseong rieul + mieum            = rieul-mieum */
        0x1105 to 0x1107 to 0xa969, /* choseong rieul + pieup            = rieul-pieup */
        0x1105 to 0x1108 to 0xa96a, /* choseong rieul + ssangpieup       = rieul-ssangpieup */
        0x1105 to 0x1109 to 0xa96c, /* choseong rieul + sios             = rieul-sios */
        0x1105 to 0x110b to 0x111b, /* choseong rieul + ieung            = kapyeounrieul */
        0x1105 to 0x110c to 0xa96d, /* choseong rieul + cieuc            = rieul-cieuc */
        0x1105 to 0x110f to 0xa96e, /* choseong rieul + khieukh          = rieul-khieukh */
        0x1105 to 0x1112 to 0x111a, /* choseong rieul + hieuh            = rieul-hieuh */
        0x1105 to 0x112b to 0xa96b, /* choseong rieul + kapyeounpieup    = rieul-kapyeounpieup */
        0x1106 to 0x1100 to 0xa96f, /* choseong mieum + kiyeok           = mieum-kiyeok */
        0x1106 to 0x1103 to 0xa970, /* choseong mieum + tikeut           = mieum-tikeut */
        0x1106 to 0x1107 to 0x111c, /* choseong mieum + pieup            = mieum-pieup */
        0x1106 to 0x1109 to 0xa971, /* choseong mieum + sios             = mieum-sios */
        0x1106 to 0x110b to 0x111d, /* choseong mieum + ieung            = kapyeounmieum */
        0x1107 to 0x1100 to 0x111e, /* choseong pieup + kiyeok           = pieup-kiyeok */
        0x1107 to 0x1102 to 0x111f, /* choseong pieup + nieun            = pieup-nieun */
        0x1107 to 0x1103 to 0x1120, /* choseong pieup + tikeut           = pieup-tikeut */
        0x1107 to 0x1107 to 0x1108, /* choseong pieup + pieup            = ssangpieup */
        0x1107 to 0x1109 to 0x1121, /* choseong pieup + sios             = pieup-sios */
        0x1107 to 0x110a to 0x1125, /* choseong pieup + ssangsios        = pieup-ssangsios */
        0x1107 to 0x110b to 0x112b, /* choseong pieup + ieung            = kapyeounpieup */
        0x1107 to 0x110c to 0x1127, /* choseong pieup + cieuc            = pieup-cieuc */
        0x1107 to 0x110e to 0x1128, /* choseong pieup + chieuch          = pieup-chieuch */
        0x1107 to 0x110f to 0xa973, /* choseong pieup + khieukh          = pieup-khieukh */
        0x1107 to 0x1110 to 0x1129, /* choseong pieup + thieuth          = pieup-thieuth */
        0x1107 to 0x1111 to 0x112a, /* choseong pieup + phieuph          = pieup-phieuph */
        0x1107 to 0x1112 to 0xa974, /* choseong pieup + hieuh            = pieup-hieuh */
        0x1107 to 0x112b to 0x112c, /* choseong pieup + kapyeounpieup    = kapyeounssangpieup */
        0x1107 to 0x112d to 0x1122, /* choseong pieup + sios-kiyeok      = pieup-sios-kiyeok */
        0x1107 to 0x112f to 0x1123, /* choseong pieup + sios-tikeut      = pieup-sios-tikeut */
        0x1107 to 0x1132 to 0x1124, /* choseong pieup + sios-pieup       = pieup-sios-pieup */
        0x1107 to 0x1136 to 0x1126, /* choseong pieup + sios-cieuc       = pieup-sios-cieuc */
        0x1107 to 0x1139 to 0xa972, /* choseong pieup + sios-thieuth     = pieup-sios-thieuth */
        0x1108 to 0x110b to 0x112c, /* choseong ssangpieup + ieung       = kapyeounssangpieup */
        0x1109 to 0x1100 to 0x112d, /* choseong sios + kiyeok            = sios-kiyeok */
        0x1109 to 0x1102 to 0x112e, /* choseong sios + nieun             = sios-nieun */
        0x1109 to 0x1103 to 0x112f, /* choseong sios + tikeut            = sios-tikeut */
        0x1109 to 0x1105 to 0x1130, /* choseong sios + rieul             = sios-rieul */
        0x1109 to 0x1106 to 0x1131, /* choseong sios + mieum             = sios-mieum */
        0x1109 to 0x1107 to 0x1132, /* choseong sios + pieup             = sios-pieup */
        0x1109 to 0x1109 to 0x110a, /* choseong sios + sios              = ssangsios */
        0x1109 to 0x110a to 0x1134, /* choseong sios + ssangsios         = sios-ssangsios */
        0x1109 to 0x110b to 0x1135, /* choseong sios + ieung             = sios-ieung */
        0x1109 to 0x110c to 0x1136, /* choseong sios + cieuc             = sios-cieuc */
        0x1109 to 0x110e to 0x1137, /* choseong sios + chieuch           = sios-chieuch */
        0x1109 to 0x110f to 0x1138, /* choseong sios + khieukh           = sios-khieukh */
        0x1109 to 0x1110 to 0x1139, /* choseong sios + thieuth           = sios-thieuth */
        0x1109 to 0x1111 to 0x113a, /* choseong sios + phieuph           = sios-phieuph */
        0x1109 to 0x1112 to 0x113b, /* choseong sios + hieuh             = sios-hieuh */
        0x1109 to 0x111e to 0x1133, /* choseong sios + pieup-kiyeok      = sios-pieup-kiyeok */
        0x1109 to 0x1132 to 0xa975, /* choseong sios + sios-pieup        = ssangsios-pieup */
        0x110a to 0x1107 to 0xa975, /* choseong ssangsios + pieup        = ssangsios-pieup */
        0x110a to 0x1109 to 0x1134, /* choseong ssangsios + sios         = sios-ssangsios */
        0x110b to 0x1100 to 0x1141, /* choseong ieung + kiyeok           = ieung-kiyeok */
        0x110b to 0x1103 to 0x1142, /* choseong ieung + tikeut           = ieung-tikeut */
        0x110b to 0x1105 to 0xa976, /* choseong ieung + rieul            = ieung-rieul */
        0x110b to 0x1106 to 0x1143, /* choseong ieung + mieum            = ieung-mieum */
        0x110b to 0x1107 to 0x1144, /* choseong ieung + pieup            = ieung-pieup */
        0x110b to 0x1109 to 0x1145, /* choseong ieung + sios             = ieung-sios */
        0x110b to 0x110b to 0x1147, /* choseong ieung + ieung            = ssangieung */
        0x110b to 0x110c to 0x1148, /* choseong ieung + cieuc            = ieung-cieuc */
        0x110b to 0x110e to 0x1149, /* choseong ieung + chieuch          = ieung-chieuch */
        0x110b to 0x1110 to 0x114a, /* choseong ieung + thieuth          = ieung-thieuth */
        0x110b to 0x1111 to 0x114b, /* choseong ieung + phieuph          = ieung-phieuph */
        0x110b to 0x1112 to 0xa977, /* choseong ieung + hieuh            = ieung-hieuh */
        0x110b to 0x1140 to 0x1146, /* choseong ieung + pansios          = ieung-pansios */
        0x110c to 0x110b to 0x114d, /* choseong cieuc + ieung            = cieuc-ieung */
        0x110c to 0x110c to 0x110d, /* choseong cieuc + cieuc            = ssangcieuc */
        0x110d to 0x1112 to 0xa978, /* choseong ssangcieuc + hieuh       = ssangcieuc-hieuh */
        0x110e to 0x110f to 0x1152, /* choseong chieuch + khieukh        = chieuch-khieukh */
        0x110e to 0x1112 to 0x1153, /* choseong chieuch + hieuh          = chieuch-hieuh */
        0x1110 to 0x1110 to 0xa979, /* choseong thieuth + thieuth        = ssangthieuth */
        0x1111 to 0x1107 to 0x1156, /* choseong phieuph + pieup          = phieuph-pieup */
        0x1111 to 0x110b to 0x1157, /* choseong phieuph + ieung          = kapyeounphieuph */
        0x1111 to 0x1112 to 0xa97a, /* choseong phieuph + hieuh          = phieuph-hieuh */
        0x1112 to 0x1109 to 0xa97b, /* choseong hieuh + sios             = hieuh-sios */
        0x1112 to 0x1112 to 0x1158, /* choseong hieuh + hieuh            = ssanghieuh */
        0x1121 to 0x1100 to 0x1122, /* choseong pieup-sios + kiyeok      = pieup-sios-kiyeok */
        0x1121 to 0x1103 to 0x1123, /* choseong pieup-sios + tikeut      = pieup-sios-tikeut */
        0x1121 to 0x1107 to 0x1124, /* choseong pieup-sios + pieup       = pieup-sios-pieup */
        0x1121 to 0x1109 to 0x1125, /* choseong pieup-sios + sios        = pieup-ssangsios */
        0x1121 to 0x110c to 0x1126, /* choseong pieup-sios + cieuc       = pieup-sios-cieuc */
        0x1121 to 0x1110 to 0xa972, /* choseong pieup-sios + thieuth     = pieup-sios-thieuth */
        0x1132 to 0x1100 to 0x1133, /* choseong sios-pieup + kiyeok      = sios-pieup-kiyeok */
        0x113c to 0x113c to 0x113d, /* choseong chitueumsios + chitueumsios = chitueumssangsios */
        0x113e to 0x113e to 0x113f, /* choseong ceongchieumsios + ceongchieumsios = ceongchieumssangsios */
        0x114e to 0x114e to 0x114f, /* choseong chitueumcieuc + chitueumcieuc = chitueumssangcieuc */
        0x1150 to 0x1150 to 0x1151, /* choseong ceongchieumcieuc + ceongchieumcieuc = ceongchieumssangcieuc */
        0x1159 to 0x1159 to 0xa97c, /* choseong yeorinhieuh + yeorinhieuh = ssangyeorinhieuh */

        0x1161 to 0x1161 to 0x119e, /* jungseong a + a                   = arae-a */
        0x1161 to 0x1169 to 0x1176, /* jungseong a + o                   = a-o */
        0x1161 to 0x116e to 0x1177, /* jungseong a + u                   = a-u */
        0x1161 to 0x1173 to 0x11a3, /* jungseong a + eu                  = a-eu */
        0x1161 to 0x1175 to 0x1162, /* jungseong a + i                   = ae */
        0x1163 to 0x1169 to 0x1178, /* jungseong ya + o                  = ya-o */
        0x1163 to 0x116d to 0x1179, /* jungseong ya + yo                 = ya-yo */
        0x1163 to 0x116e to 0x11a4, /* jungseong ya + u                  = ya-u */
        0x1163 to 0x1175 to 0x1164, /* jungseong ya + i                  = yae */
        0x1165 to 0x1169 to 0x117a, /* jungseong eo + o                  = eo-o */
        0x1165 to 0x116e to 0x117b, /* jungseong eo + u                  = eo-u */
        0x1165 to 0x1173 to 0x117c, /* jungseong eo + eu                 = eo-eu */
        0x1165 to 0x1175 to 0x1166, /* jungseong eo + i                  = e */
        0x1167 to 0x1163 to 0x11a5, /* jungseong yeo + ya                = yeo-ya */
        0x1167 to 0x1169 to 0x117d, /* jungseong yeo + o                 = yeo-o */
        0x1167 to 0x116e to 0x117e, /* jungseong yeo + u                 = yeo-u */
        0x1167 to 0x1175 to 0x1168, /* jungseong yeo + i                 = ye */
        0x1169 to 0x1161 to 0x116a, /* jungseong o + a                   = wa */
        0x1169 to 0x1162 to 0x116b, /* jungseong o + ae                  = wae */
        0x1169 to 0x1163 to 0x11a6, /* jungseong o + ya                  = o-ya */
        0x1169 to 0x1164 to 0x11a7, /* jungseong o + yae                 = o-yae */
        0x1169 to 0x1165 to 0x117f, /* jungseong o + eo                  = o-eo */
        0x1169 to 0x1166 to 0x1180, /* jungseong o + e                   = o-e */
        0x1169 to 0x1167 to 0xd7b0, /* jungseong o + yeo                 = o-yeo */
        0x1169 to 0x1168 to 0x1181, /* jungseong o + ye                  = o-ye */
        0x1169 to 0x1169 to 0x1182, /* jungseong o + o                   = o-o */
        0x1169 to 0x116e to 0x1183, /* jungseong o + u                   = o-u */
        0x1169 to 0x1175 to 0x116c, /* jungseong o + i                   = oe */
        0x116a to 0x1175 to 0x116b, /* jungseong wa + i                  = wae */
        0x116d to 0x1161 to 0xd7b2, /* jungseong yo + a                  = yo-a */
        0x116d to 0x1162 to 0xd7b3, /* jungseong yo + ae                 = yo-ae */
        0x116d to 0x1163 to 0x1184, /* jungseong yo + ya                 = yo-ya */
        0x116d to 0x1164 to 0x1185, /* jungseong yo + yae                = yo-yae */
        0x116d to 0x1165 to 0xd7b4, /* jungseong yo + eo                 = yo-eo */
        0x116d to 0x1167 to 0x1186, /* jungseong yo + yeo                = yo-yeo */
        0x116d to 0x1169 to 0x1187, /* jungseong yo + o                  = yo-o */
        0x116d to 0x1175 to 0x1188, /* jungseong yo + i                  = yo-i */
        0x116e to 0x1161 to 0x1189, /* jungseong u + a                   = u-a */
        0x116e to 0x1162 to 0x118a, /* jungseong u + ae                  = u-ae */
        0x116e to 0x1165 to 0x116f, /* jungseong u + eo                  = weo */
        0x116e to 0x1166 to 0x1170, /* jungseong u + e                   = we */
        0x116e to 0x1167 to 0xd7b5, /* jungseong u + yeo                 = u-yeo */
        0x116e to 0x1168 to 0x118c, /* jungseong u + ye                  = u-ye */
        0x116e to 0x116e to 0x118d, /* jungseong u + u                   = u-u */
        0x116e to 0x1175 to 0x1171, /* jungseong u + i                   = wi */
        0x116e to 0x117c to 0x118b, /* jungseong u + eo-eu               = u-eo-eu */
        0x116e to 0xd7c4 to 0xd7b6, /* jungseong u + i-i                 = u-i-i */
        0x116f to 0x1173 to 0x118b, /* jungseong weo + eu                = u-eo-eu */
        0x116f to 0x1175 to 0x1170, /* jungseong weo + i                 = we */
        0x1171 to 0x1175 to 0xd7b6, /* jungseong wi + i                  = u-i-i */
        0x1172 to 0x1161 to 0x118e, /* jungseong yu + a                  = yu-a */
        0x1172 to 0x1162 to 0xd7b7, /* jungseong yu + ae                 = yu-ae */
        0x1172 to 0x1165 to 0x118f, /* jungseong yu + eo                 = yu-eo */
        0x1172 to 0x1166 to 0x1190, /* jungseong yu + e                  = yu-e */
        0x1172 to 0x1167 to 0x1191, /* jungseong yu + yeo                = yu-yeo */
        0x1172 to 0x1168 to 0x1192, /* jungseong yu + ye                 = yu-ye */
        0x1172 to 0x1169 to 0xd7b8, /* jungseong yu + o                  = yu-o */
        0x1172 to 0x116e to 0x1193, /* jungseong yu + u                  = yu-u */
        0x1172 to 0x1175 to 0x1194, /* jungseong yu + i                  = yu-i */
        0x1173 to 0x1161 to 0xd7b9, /* jungseong eu + a                  = eu-a */
        0x1173 to 0x1165 to 0xd7ba, /* jungseong eu + eo                 = eu-eo */
        0x1173 to 0x1166 to 0xd7bb, /* jungseong eu + e                  = eu-e */
        0x1173 to 0x1169 to 0xd7bc, /* jungseong eu + o                  = eu-o */
        0x1173 to 0x116e to 0x1195, /* jungseong eu + u                  = eu-u */
        0x1173 to 0x1173 to 0x1196, /* jungseong eu + eu                 = eu-eu */
        0x1173 to 0x1175 to 0x1174, /* jungseong eu + i                  = yi */
        0x1174 to 0x116e to 0x1197, /* jungseong yi + u                  = yi-u */
        0x1175 to 0x1161 to 0x1198, /* jungseong i + a                   = i-a */
        0x1175 to 0x1163 to 0x1199, /* jungseong i + ya                  = i-ya */
        0x1175 to 0x1164 to 0xd7be, /* jungseong i + yae                 = i-yae */
        0x1175 to 0x1167 to 0xd7bf, /* jungseong i + yeo                 = i-yeo */
        0x1175 to 0x1168 to 0xd7c0, /* jungseong i + ye                  = i-ye */
        0x1175 to 0x1169 to 0x119a, /* jungseong i + o                   = i-o */
        0x1175 to 0x116d to 0xd7c2, /* jungseong i + yo                  = i-yo */
        0x1175 to 0x116e to 0x119b, /* jungseong i + u                   = i-u */
        0x1175 to 0x1172 to 0xd7c3, /* jungseong i + yu                  = i-yu */
        0x1175 to 0x1173 to 0x119c, /* jungseong i + eu                  = i-eu */
        0x1175 to 0x1175 to 0xd7c4, /* jungseong i + i                   = i-i */
        0x1175 to 0x1178 to 0xd7bd, /* jungseong i + ya-o                = i-ya-o */
        0x1175 to 0x119e to 0x119d, /* jungseong i + araea               = i-araea */
        0x1182 to 0x1175 to 0xd7b1, /* jungseong o-o + i                 = o-o-i */
        0x1199 to 0x1169 to 0xd7bd, /* jungseong i-ya + o                = i-ya-o */
        0x119a to 0x1175 to 0xd7c1, /* jungseong i-o + i                 = i-o-i */
        0x119e to 0x1161 to 0xd7c5, /* jungseong araea + a               = araea-a */
        0x119e to 0x1165 to 0x119f, /* jungseong araea + eo              = araea-eo */
        0x119e to 0x1166 to 0xd7c6, /* jungseong araea + e               = araea-e */
        0x119e to 0x116e to 0x11a0, /* jungseong araea + u               = araea-u */
        0x119e to 0x1175 to 0x11a1, /* jungseong araea + i               = araea-i */
        0x119e to 0x119e to 0x11a2, /* jungseong araea + araea           = ssangaraea */

        0x11a8 to 0x11a8 to 0x11a9, /* jongseong kiyeok + kiyeok         = ssangkiyeok */
        0x11a8 to 0x11ab to 0x11fa, /* jongseong kiyeok + nieun          = kiyeok-nieun */
        0x11a8 to 0x11af to 0x11c3, /* jongseong kiyeok + rieul          = kiyeok-rieul */
        0x11a8 to 0x11b8 to 0x11fb, /* jongseong kiyeok + pieup          = kiyeok-pieup */
        0x11a8 to 0x11ba to 0x11aa, /* jongseong kiyeok + sios           = kiyeok-sios */
        0x11a8 to 0x11be to 0x11fc, /* jongseong kiyeok + chieuch        = kiyeok-chieuch */
        0x11a8 to 0x11bf to 0x11fd, /* jongseong kiyeok + khieukh        = kiyeok-khieukh */
        0x11a8 to 0x11c2 to 0x11fe, /* jongseong kiyeok + hieuh          = kiyeok-hieuh */
        0x11a8 to 0x11e7 to 0x11c4, /* jongseong kiyeok + sios-kiyeok    = kiyeok-sios-kiyeok */
        0x11aa to 0x11a8 to 0x11c4, /* jongseong kiyeok-sios + kiyeok    = kiyeok-sios-kiyeok */
        0x11ab to 0x11a8 to 0x11c5, /* jongseong nieun + kiyeok          = nieun-kiyeok */
        0x11ab to 0x11ab to 0x11ff, /* jongseong nieun + nieun           = ssangnieun */
        0x11ab to 0x11ae to 0x11c6, /* jongseong nieun + tikeut          = nieun-tikeut */
        0x11ab to 0x11af to 0xd7cb, /* jongseong nieun + rieul           = nieun-rieul */
        0x11ab to 0x11ba to 0x11c7, /* jongseong nieun + sios            = nieun-sios */
        0x11ab to 0x11bd to 0x11ac, /* jongseong nieun + cieuc           = nieun-cieuc */
        0x11ab to 0x11be to 0xd7cc, /* jongseong nieun + chieuch         = nieun-chieuch */
        0x11ab to 0x11c0 to 0x11c9, /* jongseong nieun + thieuth         = nieun-thieuth */
        0x11ab to 0x11c2 to 0x11ad, /* jongseong nieun + hieuh           = nieun-hieuh */
        0x11ab to 0x11eb to 0x11c8, /* jongseong nieun + pansios         = nieun-pansios */
        0x11ae to 0x11a8 to 0x11ca, /* jongseong tikeut + kiyeok         = tikeut-kiyeok */
        0x11ae to 0x11ae to 0xd7cd, /* jongseong tikeut + tikeut         = ssangtikeut */
        0x11ae to 0x11af to 0x11cb, /* jongseong tikeut + rieul          = tikeut-rieul */
        0x11ae to 0x11b8 to 0xd7cf, /* jongseong tikeut + pieup          = tikeut-pieup */
        0x11ae to 0x11ba to 0xd7d0, /* jongseong tikeut + sios           = tikeut-sios */
        0x11ae to 0x11bd to 0xd7d2, /* jongseong tikeut + cieuc          = tikeut-cieuc */
        0x11ae to 0x11be to 0xd7d3, /* jongseong tikeut + chieuch        = tikeut-chieuch */
        0x11ae to 0x11c0 to 0xd7d4, /* jongseong tikeut + thieuth        = tikeut-thieuth */
        0x11ae to 0x11e7 to 0xd7d1, /* jongseong tikeut + sios-kiyeok    = tikeut-sios-kiyeok */
        0x11ae to 0xd7cf to 0xd7ce, /* jongseong tikeut + tikeut-pieup   = ssangtikeut-pieup */
        0x11af to 0x11a8 to 0x11b0, /* jongseong rieul + kiyeok          = rieul-kiyeok */
        0x11af to 0x11a9 to 0xd7d5, /* jongseong rieul + ssangkiyeok     = rieul-ssangkiyeok */
        0x11af to 0x11aa to 0x11cc, /* jongseong rieul + kiyeok-sios     = rieul-kiyeok-sios */
        0x11af to 0x11ab to 0x11cd, /* jongseong rieul + nieun           = rieul-nieun */
        0x11af to 0x11ae to 0x11ce, /* jongseong rieul + tikeut          = rieul-tikeut */
        0x11af to 0x11af to 0x11d0, /* jongseong rieul + rieul           = ssangrieul */
        0x11af to 0x11b7 to 0x11b1, /* jongseong rieul + mieum           = rieul-mieum */
        0x11af to 0x11b8 to 0x11b2, /* jongseong rieul + pieup           = rieul-pieup */
        0x11af to 0x11b9 to 0x11d3, /* jongseong rieul + pieup-sios      = rieul-pieup-sios */
        0x11af to 0x11ba to 0x11b3, /* jongseong rieul + sios            = rieul-sios */
        0x11af to 0x11bb to 0x11d6, /* jongseong rieul + ssangsios       = rieul-ssangsios */
        0x11af to 0x11bc to 0xd7dd, /* jongseong rieul + ieung           = kapyeounrieul */
        0x11af to 0x11bf to 0x11d8, /* jongseong rieul + khieukh         = rieul-khieukh */
        0x11af to 0x11c0 to 0x11b4, /* jongseong rieul + thieuth         = rieul-thieuth */
        0x11af to 0x11c1 to 0x11b5, /* jongseong rieul + phieuph         = rieul-phieuph */
        0x11af to 0x11c2 to 0x11b6, /* jongseong rieul + hieuh           = rieul-hieuh */
        0x11af to 0x11d8 to 0xd7d7, /* jongseong rieul + rieul-khieukh   = ssangrieul-khieukh */
        0x11af to 0x11da to 0x11d1, /* jongseong rieul + mieum-kiyeok    = rieul-mieum-kiyeok */
        0x11af to 0x11dd to 0x11d2, /* jongseong rieul + mieum-sios      = rieul-mieum-sios */
        0x11af to 0x11e1 to 0xd7d8, /* jongseong rieul + mieum-hieuh     = rieul-mieum-hieuh */
        0x11af to 0x11e4 to 0xd7da, /* jongseong rieul + pieup-phieuph   = rieul-pieup-phieuph */
        0x11af to 0x11e5 to 0x11d4, /* jongseong rieul + pieup-hieuh     = rieul-pieup-hieuh */
        0x11af to 0x11e6 to 0x11d5, /* jongseong rieul + kapyeounpieup   = rieul-kapyeounpieup */
        0x11af to 0x11eb to 0x11d7, /* jongseong rieul + pansios         = rieul-pansios */
        0x11af to 0x11f0 to 0xd7db, /* jongseong rieul + yesieung        = rieul-yesieung */
        0x11af to 0x11f9 to 0x11d9, /* jongseong rieul + yeorinhieuh     = rieul-yeorinhieuh */
        0x11af to 0x11fe to 0xd7d6, /* jongseong rieul + kiyeok-hieuh    = rieul-kiyeok-hieuh */
        0x11af to 0xd7e3 to 0xd7d9, /* jongseong rieul + pieup-tikeut    = rieul-pieup-tikeut */
        0x11b0 to 0x11a8 to 0xd7d5, /* jongseong rieul-kiyeok + kiyeok   = rieul-ssangkiyeok */
        0x11b0 to 0x11ba to 0x11cc, /* jongseong rieul-kiyeok + sios     = rieul-kiyeok-sios */
        0x11b0 to 0x11c2 to 0xd7d6, /* jongseong rieul-kiyeok + hieuh    = rieul-kiyeok-hieuh */
        0x11b1 to 0x11a8 to 0x11d1, /* jongseong rieul-mieum + kiyeok    = rieul-mieum-kiyeok */
        0x11b1 to 0x11ba to 0x11d2, /* jongseong rieul-mieum + sios      = rieul-mieum-sios */
        0x11b1 to 0x11c2 to 0xd7d8, /* jongseong rieul-mieum + hieuh     = rieul-mieum-hieuh */
        0x11b2 to 0x11ae to 0xd7d9, /* jongseong rieul-pieup + tikeut    = rieul-pieup-tikeut */
        0x11b2 to 0x11ba to 0x11d3, /* jongseong rieul-pieup + sios      = rieul-pieup-sios */
        0x11b2 to 0x11bc to 0x11d5, /* jongseong rieul-pieup + ieung     = rieul-kapyeounpieup */
        0x11b2 to 0x11c1 to 0xd7da, /* jongseong rieul-pieup + phieuph   = rieul-pieup-phieuph */
        0x11b2 to 0x11c2 to 0x11d4, /* jongseong rieul-pieup + hieuh     = rieul-pieup-hieuh */
        0x11b3 to 0x11ba to 0x11d6, /* jongseong rieul-sios + sios       = rieul-ssangsios */
        0x11b7 to 0x11a8 to 0x11da, /* jongseong mieum + kiyeok          = mieum-kiyeok */
        0x11b7 to 0x11ab to 0xd7de, /* jongseong mieum + nieun           = mieum-nieun */
        0x11b7 to 0x11af to 0x11db, /* jongseong mieum + rieul           = mieum-rieul */
        0x11b7 to 0x11b7 to 0xd7e0, /* jongseong mieum + mieum           = ssangmieum */
        0x11b7 to 0x11b8 to 0x11dc, /* jongseong mieum + pieup           = mieum-pieup */
        0x11b7 to 0x11b9 to 0xd7e1, /* jongseong mieum + pieup-sios      = mieum-pieup-sios */
        0x11b7 to 0x11ba to 0x11dd, /* jongseong mieum + sios            = mieum-sios */
        0x11b7 to 0x11bb to 0x11de, /* jongseong mieum + ssangsios       = mieum-ssangsios */
        0x11b7 to 0x11bc to 0x11e2, /* jongseong mieum + ieung           = kapyeounmieum */
        0x11b7 to 0x11bd to 0xd7e2, /* jongseong mieum + cieuc           = mieum-cieuc */
        0x11b7 to 0x11be to 0x11e0, /* jongseong mieum + chieuch         = mieum-chieuch */
        0x11b7 to 0x11c2 to 0x11e1, /* jongseong mieum + hieuh           = mieum-hieuh */
        0x11b7 to 0x11eb to 0x11df, /* jongseong mieum + pansios         = mieum-pansios */
        0x11b7 to 0x11ff to 0xd7df, /* jongseong mieum + ssangnieun      = mieum-ssangnieun */
        0x11b8 to 0x11ae to 0xd7e3, /* jongseong pieup + tikeut          = pieup-tikeut */
        0x11b8 to 0x11af to 0x11e3, /* jongseong pieup + rieul           = pieup-rieul */
        0x11b8 to 0x11b5 to 0xd7e4, /* jongseong pieup + rieul-phieuph   = pieup-rieul-phieuph */
        0x11b8 to 0x11b7 to 0xd7e5, /* jongseong pieup + mieum           = pieup-mieum */
        0x11b8 to 0x11b8 to 0xd7e6, /* jongseong pieup + pieup           = ssangpieup */
        0x11b8 to 0x11ba to 0x11b9, /* jongseong pieup + sios            = pieup-sios */
        0x11b8 to 0x11bc to 0x11e6, /* jongseong pieup + ieung           = kapyeounpieup */
        0x11b8 to 0x11bd to 0xd7e8, /* jongseong pieup + cieuc           = pieup-cieuc */
        0x11b8 to 0x11be to 0xd7e9, /* jongseong pieup + chieuch         = pieup-chieuch */
        0x11b8 to 0x11c1 to 0x11e4, /* jongseong pieup + phieuph         = pieup-phieuph */
        0x11b8 to 0x11c2 to 0x11e5, /* jongseong pieup + hieuh           = pieup-hieuh */
        0x11b8 to 0x11e8 to 0xd7e7, /* jongseong pieup + sios-tikeut     = pieup-sios-tikeut */
        0x11b9 to 0x11ae to 0xd7e7, /* jongseong pieup-sios + tikeut     = pieup-sios-tikeut */
        0x11ba to 0x11a8 to 0x11e7, /* jongseong sios + kiyeok           = sios-kiyeok */
        0x11ba to 0x11ae to 0x11e8, /* jongseong sios + tikeut           = sios-tikeut */
        0x11ba to 0x11af to 0x11e9, /* jongseong sios + rieul            = sios-rieul */
        0x11ba to 0x11b7 to 0xd7ea, /* jongseong sios + mieum            = sios-mieum */
        0x11ba to 0x11b8 to 0x11ea, /* jongseong sios + pieup            = sios-pieup */
        0x11ba to 0x11ba to 0x11bb, /* jongseong sios + sios             = ssangsios */
        0x11ba to 0x11bd to 0xd7ef, /* jongseong sios + cieuc            = sios-cieuc */
        0x11ba to 0x11be to 0xd7f0, /* jongseong sios + chieuch          = sios-chieuch */
        0x11ba to 0x11c0 to 0xd7f1, /* jongseong sios + thieuth          = sios-thieuth */
        0x11ba to 0x11c2 to 0xd7f2, /* jongseong sios + hieuh            = sios-hieuh */
        0x11ba to 0x11e6 to 0xd7eb, /* jongseong sios + kapyeounpieup    = sios-kapyeounpieup */
        0x11ba to 0x11e7 to 0xd7ec, /* jongseong sios + sios-kiyeok      = ssangsios-kiyeok */
        0x11ba to 0x11e8 to 0xd7ed, /* jongseong sios + sios-tikeut      = ssangsios-tikeut */
        0x11ba to 0x11eb to 0xd7ee, /* jongseong sios + pansios          = sios-pansios */
        0x11bb to 0x11a8 to 0xd7ec, /* jongseong ssangsios + kiyeok      = ssangsios-kiyeok */
        0x11bb to 0x11ae to 0xd7ed, /* jongseong ssangsios + tikeut      = ssangsios-tikeut */
        0x11bd to 0x11b8 to 0xd7f7, /* jongseong cieuc + pieup           = cieuc-pieup */
        0x11bd to 0x11bd to 0xd7f9, /* jongseong cieuc + cieuc           = ssangcieuc */
        0x11bd to 0xd7e6 to 0xd7f8, /* jongseong cieuc + ssangpieup      = cieuc-ssangpieup */
        0x11c1 to 0x11b8 to 0x11f3, /* jongseong phieuph + pieup         = phieuph-pieup */
        0x11c1 to 0x11ba to 0xd7fa, /* jongseong phieuph + sios          = phieuph-sios */
        0x11c1 to 0x11bc to 0x11f4, /* jongseong phieuph + ieung         = kapyeounphieuph */
        0x11c1 to 0x11c0 to 0xd7fb, /* jongseong phieuph + thieuth       = phieuph-thieuth */
        0x11c2 to 0x11ab to 0x11f5, /* jongseong hieuh + nieun           = hieuh-nieun */
        0x11c2 to 0x11af to 0x11f6, /* jongseong hieuh + rieul           = hieuh-rieul */
        0x11c2 to 0x11b7 to 0x11f7, /* jongseong hieuh + mieum           = hieuh-mieum */
        0x11c2 to 0x11b8 to 0x11f8, /* jongseong hieuh + pieup           = hieuh-pieup */
        0x11ce to 0x11c2 to 0x11cf, /* jongseong rieul-tikeut + hieuh    = rieul-tikeut-hieuh */
        0x11d0 to 0x11bf to 0xd7d7, /* jongseong ssangrieul + khieukh    = ssangrieul-khieukh */
        0x11d9 to 0x11c2 to 0xd7dc, /* jongseong rieul-yeorinhieuh + hieuh = rieul-yeorinhieuh-hieuh */
        0x11dc to 0x11ba to 0xd7e1, /* jongseong mieum-pieup + sios      = mieum-pieup-sios */
        0x11dd to 0x11ba to 0x11de, /* jongseong mieum-sios + sios       = mieum-ssangsios */
        0x11e3 to 0x11c1 to 0xd7e4, /* jongseong pieup-rieul + phieuph   = pieup-rieul-phieuph */
        0x11ea to 0x11bc to 0xd7eb, /* jongseong sios-pieup + ieung      = sios-kapyeounpieup */
        0x11eb to 0x11b8 to 0xd7f3, /* jongseong pansios + pieup         = pansios-pieup */
        0x11eb to 0x11e6 to 0xd7f4, /* jongseong pansios + kapyeounpieup = pansios-kapyeounpieup */
        0x11ec to 0x11a8 to 0x11ed, /* jongseong ieung-kiyeok + kiyeok   = ieung-ssangkiyeok */
        0x11f0 to 0x11a8 to 0x11ec, /* jongseong yesieung + kiyeok       = yesieung-kiyeok */
        0x11f0 to 0x11a9 to 0x11ed, /* jongseong yesieung + ssangkiyeok  = yesieung-ssangkiyeok */
        0x11f0 to 0x11b7 to 0xd7f5, /* jongseong yesieung + mieum        = yesieung-mieum */
        0x11f0 to 0x11ba to 0x11f1, /* jongseong yesieung + sios         = yesieung-sios */
        0x11f0 to 0x11bf to 0x11ef, /* jongseong yesieung + khieukh      = yesieung-khieukh */
        0x11f0 to 0x11c2 to 0xd7f6, /* jongseong yesieung + hieuh        = yesieung-hieuh */
        0x11f0 to 0x11eb to 0x11f2, /* jongseong yesieung + pansios      = yesieung-pansios */
        0x11f0 to 0x11f0 to 0x11ee, /* jongseong yesieung + yesieung     = ssangyesieung */

        0xa964 to 0x1100 to 0xa965, /* choseong rieul-kiyeok + kiyeok    = rieul-ssangkiyeok */
        0xa966 to 0x1103 to 0xa967, /* choseong rieul-tikeut + tikeut    = rieul-ssangtikeut */
        0xa969 to 0x1107 to 0xa96a, /* choseong rieul-pieup + pieup      = rieul-ssangpieup */
        0xa969 to 0x110b to 0xa96b, /* choseong rieul-pieup + ieung      = rieul-kapyeounpieup */

        0xd7c5 to 0x1161 to 0x11a2, /* jungseong araea-a + a             = ssangaraea */

        0xd7cd to 0x11b8 to 0xd7ce, /* jongseong ssangtikeut + pieup     = ssangtikeut-pieup */
        0xd7d0 to 0x11a8 to 0xd7d1, /* jongseong tikeut-sios + kiyeok    = tikeut-sios-kiyeok */
        0xd7de to 0x11ab to 0xd7df, /* jongseong mieum-nieun + nieun     = mieum-ssangnieun */
        0xd7f3 to 0x11bc to 0xd7f4, /* jongseong pansios-pieup + ieung   = pansios-kapyeounpieup */
        0xd7f7 to 0x11b8 to 0xd7f8  /* jongseong cieuc-pieup + pieup     = cieuc-ssangpieup */
    )
}