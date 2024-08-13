/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package com.anng6.usbkit.ui.activity;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.anng6.usbkit.App;
import com.anng6.usbkit.R;
import com.anng6.usbkit.util.ThemeUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;

import com.google.android.material.snackbar.Snackbar;
import rikka.material.app.MaterialActivity;

public class BaseActivity extends MaterialActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onApplyUserThemeResource(@NonNull Resources.Theme theme, boolean isDecorView) {
        if (ThemeUtil.isSystemAccent()) {
            DynamicColors.applyToActivityIfAvailable(this);
        } else {
            theme.applyStyle(ThemeUtil.getColorThemeStyleRes(), true);
        }
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true);
        theme.applyStyle(
                rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true);
    }

    @Override
    public String computeUserThemeKey() {
        return ThemeUtil.getColorTheme() + ThemeUtil.getNightTheme(this);
    }

    @Override
    public void onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars();
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    public Snackbar showHint(
            String str, int length, @StringRes int actionRes, View.OnClickListener action) {
        return showHint(str, length, App.getInstance().getString(actionRes), action);
    }

    public Snackbar showHint(
            @StringRes int res, int length, @StringRes int actionRes, View.OnClickListener action) {
        return showHint(
                App.getInstance().getString(res),
                length,
                App.getInstance().getString(actionRes),
                action);
    }

    public Snackbar showHint(@StringRes int res, int length) {
        return showHint(App.getInstance().getString(res), length, null, null);
    }

    public Snackbar showHint(CharSequence str, int length) {
        return showHint(str, length, null, null);
    }

    public Snackbar showHint(
            CharSequence str, int length, CharSequence actionStr, View.OnClickListener action) {
        var snackbar = Snackbar.make(findViewById(android.R.id.content), str, length);
        var nav = findViewById(R.id.nav_view);
        if (nav instanceof BottomNavigationView) snackbar.setAnchorView(nav);
        if (actionStr != null && action != null) snackbar.setAction(actionStr, action);
        snackbar.show();
        return snackbar;
    }
}
