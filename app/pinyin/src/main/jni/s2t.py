import sys
import opencc
from pypinyin import lazy_pinyin

pinyin_sep = ' '
pinyin_fixes = {
    'n': 'en',
}

def main(args):
    in_file, out_file = args

    s2tw = opencc.OpenCC('s2tw.json')
    s2twp = opencc.OpenCC('s2twp.json')

    tw_set = set()

    with open(in_file, 'r', encoding='utf-16le') as fin:
        for line in fin:
            tokens = line.strip().split(' ')
            word = tokens[0]
            tw = s2tw.convert(word)
            tw_set.add(tw)

    with open(in_file, 'r', encoding='utf-16le') as fin, open(out_file, 'w', encoding='utf-16le') as fout:
        for line in fin:
            tokens = line.strip().split(' ')
            word, freq, flag = tokens[:3]
            tw = s2tw.convert(word)
            twp = s2twp.convert(word)
            # Do not convert phrases if converted phrase already exists.
            if tw != twp and twp in tw_set:
                print(twp, '=>', tw)
                twp = tw
            # Update pinyin only if entire phrase has been changed.
            if tw != twp:
                pinyin = [pinyin_fixes.get(p, p) for p in lazy_pinyin(twp)]
            else:
                pinyin = tokens[3:]
            pinyin = pinyin_sep.join(pinyin)
            if pinyin != ' '.join(tokens[3:]):
                print(tokens[0], ' '.join(tokens[3:]), '->', twp, pinyin)
            fout.write(f'{twp} {freq} {flag} {pinyin}\n')

    return 0

if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
