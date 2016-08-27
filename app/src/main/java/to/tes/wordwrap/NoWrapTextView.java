/*-*- coding:utf-8 -*-*/
package to.tes.wordwrap;

import android.content.Context;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.widget.TextView;
import java.util.Locale;

public class NoWrapTextView extends TextView {
    private CharSequence mOrgText = "";
    private BufferType mOrgBufferType = BufferType.NORMAL;

    public NoWrapTextView(Context context) {
        super(context);
        setFilters(new InputFilter[] { new NoWrapAllFilter(this) });
	setTextLocale(Locale.ROOT);
    }

    public NoWrapTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFilters(new InputFilter[] { new NoWrapAllFilter(this) });
	setTextLocale(Locale.ROOT);
    }

    public NoWrapTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFilters(new InputFilter[] { new NoWrapAllFilter(this) });
	setTextLocale(Locale.ROOT);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        // レイアウト時に文字を設定し直して幅に合わせた改行を入れる
        setText(mOrgText, mOrgBufferType);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        // setText()をラップして改行を入れる前の文字列(設定値)を保存しておく
        mOrgText = text;
        mOrgBufferType = type;
        super.setText(text, type);
    }

    // 使いがちなインタフェイスだけ適切に設定した文字列が使われるよう調整する
    @Override
    public CharSequence getText() {
        return mOrgText;
    }

    @Override
    public int length() {
        return mOrgText.length();
    }
}
