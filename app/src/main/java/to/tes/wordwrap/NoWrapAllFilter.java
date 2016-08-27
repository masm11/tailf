/*-*- coding:utf-8 -*-*/
package to.tes.wordwrap;

import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.widget.TextView;

// TextViewの幅に合わせて改行を挿入するInputFilter - 英単語の途中でも強制改行
public class NoWrapAllFilter implements InputFilter {
    private final TextView view;

    public NoWrapAllFilter(TextView view) {
        this.view = view;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
            Spanned dest, int dstart, int dend) {
        // 関連付けられたTextViewのTextPaintと幅を取得
        TextPaint paint = view.getPaint();
        int width = view.getWidth() - view.getCompoundPaddingLeft()
                - view.getCompoundPaddingRight();

	// よく解らんけど… 2016/08/27 by masm
	if (width == 0)
	    return source.subSequence(start, end);

        // TextView#setText()から呼ばれるだけの前提なので dest 以降の引数は使わない
        SpannableStringBuilder result = new SpannableStringBuilder();
        for (int index = start; index < end; index++) {
            // 幅チェック
            if (Layout.getDesiredWidth(source, start, index + 1, paint) > width) {
                // 行を越えた ⇒ ここまでを出力して改行を挿入
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
}
