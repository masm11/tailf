/*-*- coding:utf-8 -*-*/
package to.tes.wordwrap;

import android.content.Context;
import android.util.AttributeSet;

public class NoWrapButton extends NoWrapTextView {
    public NoWrapButton(Context context) {
        this(context, null);
    }

    public NoWrapButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public NoWrapButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
