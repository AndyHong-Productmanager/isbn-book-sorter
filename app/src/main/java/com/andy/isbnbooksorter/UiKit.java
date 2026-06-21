package com.andy.isbnbooksorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class UiKit {
    static final int SURFACE_PRIMARY = Color.rgb(247, 245, 240);
    static final int SURFACE_SECONDARY = Color.WHITE;
    static final int SCANNER_SURFACE = Color.rgb(36, 33, 29);
    static final int TEXT_PRIMARY = Color.rgb(31, 37, 33);
    static final int TEXT_SECONDARY = Color.rgb(102, 112, 105);
    static final int ACCENT_PRIMARY = Color.rgb(47, 111, 78);
    static final int STATUS_WARNING = Color.rgb(163, 107, 31);
    static final int STATUS_ERROR = Color.rgb(184, 74, 58);

    private final Context context;

    UiKit(Context context) {
        this.context = context;
    }

    Button button(String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(ACCENT_PRIMARY);
        button.setMinHeight(dp(48));
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    EditText input(String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextColor(TEXT_PRIMARY);
        input.setHintTextColor(TEXT_SECONDARY);
        input.setSingleLine(true);
        input.setMinHeight(dp(48));
        return input;
    }

    TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    LinearLayout column(int gap) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if (gap > 0) {
            layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            layout.setDividerDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            layout.setDividerPadding(dp(gap));
        }
        return layout;
    }

    LinearLayout row(int gap) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if (gap > 0) {
            layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            layout.setDividerDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            layout.setDividerPadding(dp(gap));
        }
        return layout;
    }

    int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
