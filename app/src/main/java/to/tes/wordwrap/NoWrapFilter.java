/*-*- coding:utf-8 -*-*/
package to.tes.wordwrap;

import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.widget.TextView;

// TextViewの幅に合わせて改行を挿入するInputFilter - 英単語を途中で分断しない
class NoWrapFilter implements InputFilter {
    private final TextView view;

    public NoWrapFilter(TextView view) {
        this.view = view;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
            Spanned dest, int dstart, int dend) {
        // 関連付けられたTextViewのTextPaintと幅を取得
        TextPaint paint = view.getPaint();
        int width = view.getWidth() - view.getCompoundPaddingLeft()
                - view.getCompoundPaddingRight();

        // TextView#setText()から呼ばれるだけの前提なので dest 以降の引数は使わない
        SpannableStringBuilder result = new SpannableStringBuilder();
        int wordstart = -1;
        for (int index = start; index < end; index++) {
            // ワードラップ対象の文字?
            if (isWrapLetter(source.charAt(index))) {
                if (wordstart < 0) {
                    // 単語の開始位置
                    wordstart = index;
                }
            } else {
                wordstart = -1;
            }

            // 幅チェック
            if (Layout.getDesiredWidth(source, start, index + 1, paint) > width) {
                // 行を越えた
                if (wordstart > start) { // wordstart==startのときは結局ここまでを出力せざるをえない
                    if ((index + 1 < source.length())
                            && isWrapLetter(source.charAt(index + 1))) {
                        // 次の文字もワードラップ対象 ⇒ 単語の先頭まで巻き戻す
                        index = wordstart;
                    }
                }

                // ここまでを出力して改行を挿入
                result.append(source.subSequence(start, index));
                result.append("\n");
                start = index;

            } else if (source.charAt(index) == '\n') {
                // 改行文字 ⇒ ここまでを出力
                result.append(source.subSequence(start, index));
                start = index;
            }
        }
        if (start < end) {
            // 残りを格納(最後の行)
            result.append(source.subSequence(start, end));
        }
        return result;
    }

    private boolean isWrapLetter(char c) {
        return (WORDWRAP_LETTERS.indexOf(c) >= 0);
    }

    private static final String WORDWRAP_LETTERS = "0123456789"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
}
