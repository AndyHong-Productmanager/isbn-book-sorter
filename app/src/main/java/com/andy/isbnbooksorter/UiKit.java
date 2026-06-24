package com.andy.isbnbooksorter;

import android.content.Context;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
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
    static final int BORDER_DEFAULT = Color.rgb(217, 213, 202);
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
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return button;
    }

    Button secondaryButton(String label) {
        Button button = button(label);
        button.setTextColor(ACCENT_PRIMARY);
        button.setBackground(surfaceBackground(SURFACE_SECONDARY));
        return button;
    }

    Button iconButton(String label, String description) {
        Button button = button(label);
        button.setContentDescription(description);
        button.setTextColor(ACCENT_PRIMARY);
        button.setTextSize(22);
        button.setBackground(surfaceBackground(SURFACE_SECONDARY));
        button.setPadding(0, 0, 0, dp(2));
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        return button;
    }

    EditText input(String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextColor(TEXT_PRIMARY);
        input.setHintTextColor(TEXT_SECONDARY);
        input.setSingleLine(true);
        input.setMinHeight(dp(48));
        input.setTextSize(15);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return input;
    }

    TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setSingleLine(false);
        textView.setHorizontallyScrolling(false);
        textView.setIncludeFontPadding(true);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
            textView.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL);
        }
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
            layout.setDividerDrawable(new GapDrawable(0, dp(gap)));
        }
        return layout;
    }

    LinearLayout row(int gap) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if (gap > 0) {
            layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            layout.setDividerDrawable(new GapDrawable(dp(gap), 0));
        }
        return layout;
    }

    int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    private GradientDrawable surfaceBackground(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setStroke(dp(1), BORDER_DEFAULT);
        return background;
    }

    private static final class GapDrawable extends ColorDrawable {
        private final int width;
        private final int height;

        GapDrawable(int width, int height) {
            super(Color.TRANSPARENT);
            this.width = width;
            this.height = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return width;
        }

        @Override
        public int getIntrinsicHeight() {
            return height;
        }
    }
}
