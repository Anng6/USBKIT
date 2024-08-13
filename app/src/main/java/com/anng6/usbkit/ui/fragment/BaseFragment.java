/*
 * <!--This file is part of LSPosed.
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
 * Copyright (C) 2021 LSPosed Contributors-->
 */

package com.anng6.usbkit.ui.fragment;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.anng6.usbkit.ui.activity.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.anng6.usbkit.App;
import com.anng6.usbkit.R;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public abstract class BaseFragment extends Fragment {

    public void navigateUp() {
        getNavController().navigateUp();
    }

    public NavController getNavController() {
        return NavHostFragment.findNavController(this);
    }

    public boolean safeNavigate(@IdRes int resId) {
        try {
            getNavController().navigate(resId);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public boolean safeNavigate(NavDirections direction) {
        try {
            getNavController().navigate(direction);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public void setupToolbar(Toolbar toolbar, int title) {
        setupToolbar(toolbar, getString(title), -1);
    }

    public void setupToolbar(Toolbar toolbar, String title) {
        setupToolbar(toolbar, title, -1);
    }

    public void setupToolbar(Toolbar toolbar, int title, int menu) {
        setupToolbar(toolbar, getString(title), menu, null);
    }

    public void setupToolbar(Toolbar toolbar, String title, int menu) {
        setupToolbar(toolbar, title, menu, null);
    }

    public void setupToolbar(
            Toolbar toolbar,
            String title,
            int menu,
            View.OnClickListener navigationOnClickListener) {
        toolbar.setNavigationOnClickListener(
                navigationOnClickListener == null
                        ? (v -> navigateUp())
                        : navigationOnClickListener);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back);
        toolbar.setTitle(title);
        toolbar.setTooltipText(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            if (this instanceof MenuProvider self) {
                toolbar.setOnMenuItemClickListener(self::onMenuItemSelected);
                self.onPrepareMenu(toolbar.getMenu());
            }
        }
    }

    public void runAsync(Runnable runnable) {
        App.getExecutorService().submit(runnable);
    }

    public <T> Future<T> runAsync(Callable<T> callable) {
        return App.getExecutorService().submit(callable);
    }

    public void runOnUiThread(Runnable runnable) {
        App.getMainHandler().post(runnable);
    }

    public <T> Future<T> runOnUiThread(Callable<T> callable) {
        var task = new FutureTask<>(callable);
        runOnUiThread(task);
        return task;
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
        var activity = requireActivity();
        var snackbar = Snackbar.make(activity.findViewById(android.R.id.content), str, length);
        var nav = activity.findViewById(R.id.nav_view);
        if (nav instanceof BottomNavigationView) snackbar.setAnchorView(nav);
        if (actionStr != null && action != null) snackbar.setAction(actionStr, action);
        snackbar.show();
        return snackbar;
    }
}
