package com.anng6.usbkit.ui.fragment;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;

import com.anng6.usbkit.App;
import com.anng6.usbkit.BuildConfig;
import com.anng6.usbkit.R;
import com.anng6.usbkit.databinding.DialogAboutBinding;
import com.anng6.usbkit.databinding.DialogUsbinfoBinding;
import com.anng6.usbkit.databinding.FragmentSettingsBinding;
import com.anng6.usbkit.ui.dialog.BlurBehindDialogBuilder;
import com.anng6.usbkit.util.AppUtil;
import com.anng6.usbkit.util.GadgetUtil;
import com.anng6.usbkit.util.LangList;
import com.anng6.usbkit.util.LinkTransformationMethod;
import com.anng6.usbkit.util.ThemeUtil;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.nio.FileSystemManager;

import rikka.core.util.ResourceUtils;
import rikka.material.app.LocaleDelegate;
import rikka.material.preference.MaterialSwitchPreference;
import rikka.preference.SimpleMenuPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class SettingsFragment extends BaseFragment implements MenuProvider {
    FragmentSettingsBinding binding;
    static FileSystemManager remoteFS;
    static GadgetUtil.Gadget gadget;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        setupToolbar(binding.toolbar, R.string.settings, R.menu.menu_settings);
        binding.toolbar.setNavigationIcon(null);

        refreshUI();
        AppUtil.remoteFS.observe(getViewLifecycleOwner(), (remoteFS) -> refreshUI());
        return binding.getRoot();
    }

    public void refreshUI() {
        if (binding == null) return;
        remoteFS = AppUtil.remoteFS.getValue();
        gadget = GadgetUtil.getGadget(remoteFS, AppUtil.useGadget);
        binding.toolbar.setSubtitle(
                String.format(
                        "%s (%d) - %s",
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        remoteFS != null ? "Root" : "NoRoot"));
        binding.toolbarLayout.setSubtitle(binding.toolbar.getSubtitle());

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.setting_container, new PreferenceFragment())
                .commitNow();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        var itemId = item.getItemId();
        if (itemId == R.id.menu_about) new AboutDialog().show(getChildFragmentManager(), "about");

        return true;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    public static class SettingsPreferenceDataStore extends PreferenceDataStore {
        SharedPreferences pref = App.getPreferences();

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return switch (key) {
                case "enable_udc" -> gadget != null ? gadget.getUDC() : false;
                default -> pref.getBoolean(key, defValue);
            };
        }

        @Override
        public void putBoolean(String key, boolean value) {
            switch (key) {
                case "enable_udc" -> {
                    if (gadget != null) gadget.setUDC(value);
                }
                default -> pref.edit().putBoolean(key, value).apply();
            }
        }

        @Override
        public String getString(String key, String defValue) {
            return switch (key) {
                case "use_gadget" -> AppUtil.useGadget;
                default -> pref.getString(key, defValue);
            };
        }

        @Override
        public void putString(String key, String value) {
            switch (key) {
                case "use_gadget" -> AppUtil.useGadget = value;
                default -> pref.edit().putString(key, value).apply();
            }
        }

        @Override
        public Set<String> getStringSet(String key, Set defValue) {
            return switch (key) {
                case "enable_funs" -> gadget != null
                        ? new LinkedHashSet<>(Arrays.asList(gadget.getEnabledFunctions()))
                        : new HashSet<>();
                default -> null;
            };
        }

        @Override
        public void putStringSet(String key, Set<String> value) {
            switch (key) {
                case "enable_funs" -> {
                    gadget.removeAllConf();
                    for (var fun : value) {
                        gadget.setFunctionEnable(fun, true);
                    }
                }
            }
        }
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setPreferenceDataStore(new SettingsPreferenceDataStore());
            addPreferencesFromResource(R.xml.settings);

            PreferenceCategory configfs_group = findPreference("configfs_group");
            if (configfs_group != null && gadget != null) configfs_group.setEnabled(true);

            MaterialSwitchPreference enable_udc = findPreference("enable_udc");
            if (enable_udc != null && gadget != null) enable_udc.setSummary(gadget.getController());

            MultiSelectListPreference enable_funs = findPreference("enable_funs");
            if (enable_funs != null && gadget != null) {
                var funs = gadget.getFunctions();
                if (funs != null) {
                    Arrays.sort(funs);
                    Log.e(App.TAG, Arrays.asList(funs).toString());
                    enable_funs.setEntries(funs);
                    enable_funs.setEntryValues(funs);
                    enable_funs.setPositiveButtonText(android.R.string.ok);
                    enable_funs.setNegativeButtonText(android.R.string.cancel);
                    enable_funs.setSummary(
                            String.format("%d/%d", enable_funs.getValues().size(), funs.length));
                    enable_funs.setOnPreferenceChangeListener(
                            (preference, newValue) -> {
                                enable_funs.setSummary(
                                        String.format(
                                                "%d/%d", ((Set) newValue).size(), funs.length));
                                return true;
                            });
                }
            }

            SimpleMenuPreference use_gadget = findPreference("use_gadget");
            var list = GadgetUtil.getGadgetList(remoteFS);
            if (use_gadget != null && list != null) {
                Arrays.sort(list);
                use_gadget.setEntries(list);
                use_gadget.setEntryValues(list);
                use_gadget.setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            requireActivity().recreate();
                            return true;
                        });
            }

            Preference usb_info = findPreference("usb_info");
            if (usb_info != null && gadget != null) {
                var info = gadget.getInfo();
                usb_info.setSummary(String.format("%s / %s", info[2], info[3]));
                usb_info.setEnabled(true);
                usb_info.setOnPreferenceClickListener(
                        (v) -> {
                            var dialogBinding =
                                    DialogUsbinfoBinding.inflate(
                                            LayoutInflater.from(requireActivity()));
                            dialogBinding.idVendor.setText(info[0]);
                            dialogBinding.idProduct.setText(info[1]);
                            dialogBinding.manufacturer.setText(info[2]);
                            dialogBinding.product.setText(info[3]);
                            dialogBinding.serialnumber.setText(info[4]);
                            new MaterialAlertDialogBuilder(requireActivity())
                                    .setTitle(R.string.usb_info)
                                    .setView(dialogBinding.getRoot())
                                    .setPositiveButton(
                                            R.string.save,
                                            (d, w) -> {
                                                info[0] =
                                                        dialogBinding.idVendor.getText().toString();
                                                info[1] =
                                                        dialogBinding
                                                                .idProduct
                                                                .getText()
                                                                .toString();
                                                info[2] =
                                                        dialogBinding
                                                                .manufacturer
                                                                .getText()
                                                                .toString();
                                                info[3] =
                                                        dialogBinding.product.getText().toString();
                                                info[4] =
                                                        dialogBinding
                                                                .serialnumber
                                                                .getText()
                                                                .toString();
                                                gadget.setInfo(info);
                                                usb_info.setSummary(
                                                        String.format("%s / %s", info[2], info[3]));
                                            })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show();
                            return true;
                        });
            }

            SimpleMenuPreference language = findPreference("language");
            if (language != null) {
                var tag = language.getValue();
                var userLocale = App.getLocale();
                var entries = new ArrayList<CharSequence>();
                var lstLang = LangList.LOCALES;
                for (var lang : lstLang) {
                    if ("SYSTEM".equals(lang)) {
                        entries.add(getString(rikka.core.R.string.follow_system));
                        continue;
                    }
                    var locale = Locale.forLanguageTag(lang);
                    entries.add(
                            HtmlCompat.fromHtml(
                                    locale.getDisplayName(locale),
                                    HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
                language.setEntries(entries.toArray(new CharSequence[0]));
                language.setEntryValues(lstLang);
                if (TextUtils.isEmpty(tag) || "SYSTEM".equals(tag)) {
                    language.setSummary(getString(rikka.core.R.string.follow_system));
                } else {
                    var locale = Locale.forLanguageTag(tag);
                    language.setSummary(
                            !TextUtils.isEmpty(locale.getScript())
                                    ? locale.getDisplayScript(userLocale)
                                    : locale.getDisplayName(userLocale));
                }
                language.setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            var app = App.getInstance();
                            var locale = App.getLocale((String) newValue);
                            var res = app.getResources();
                            var config = res.getConfiguration();
                            config.setLocale(locale);
                            LocaleDelegate.setDefaultLocale(locale);
                            //noinspection deprecation
                            res.updateConfiguration(config, res.getDisplayMetrics());
                            requireActivity().recreate();
                            return true;
                        });
            }

            Preference primary_color = findPreference("theme_color");
            if (primary_color != null) {
                primary_color.setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            requireActivity().recreate();
                            return true;
                        });
            }

            MaterialSwitchPreference prefFollowSystemAccent =
                    findPreference("follow_system_accent");
            if (prefFollowSystemAccent != null) {
                if (DynamicColors.isDynamicColorAvailable()) {
                    prefFollowSystemAccent.setVisible(true);
                    prefFollowSystemAccent.setOnPreferenceChangeListener(
                            (preference, newValue) -> {
                                requireActivity().recreate();
                                return true;
                            });
                } else {
                    prefFollowSystemAccent.setChecked(false);
                }
            }

            Preference theme = findPreference("dark_theme");
            if (theme != null) {
                theme.setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            if (!App.getPreferences()
                                    .getString("dark_theme", ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM)
                                    .equals(newValue))
                                AppCompatDelegate.setDefaultNightMode(
                                        ThemeUtil.getDarkTheme((String) newValue));
                            return true;
                        });
            }

            Preference black_dark_theme = findPreference("black_dark_theme");
            if (black_dark_theme != null) {
                black_dark_theme.setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            if (ResourceUtils.isNightMode(getResources().getConfiguration()))
                                requireActivity().recreate();
                            return true;
                        });
            }
        }
    }

    public static class AboutDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            DialogAboutBinding binding =
                    DialogAboutBinding.inflate(getLayoutInflater(), null, false);
            binding.designAboutTitle.setText(R.string.app_name);
            binding.designAboutInfo.setMovementMethod(LinkMovementMethod.getInstance());
            binding.designAboutInfo.setTransformationMethod(
                    new LinkTransformationMethod(requireActivity()));
            binding.designAboutInfo.setText(
                    HtmlCompat.fromHtml(
                            getString(
                                    R.string.about_view_source_code,
                                    "<b><a href=\"https://github.com/Anng6/USBKIT\">GitHub</a></b>"),
                            HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.designAboutVersion.setText(
                    String.format(
                            LocaleDelegate.getDefaultLocale(),
                            "%s (%d)",
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE));
            return new BlurBehindDialogBuilder(requireContext())
                    .setView(binding.getRoot())
                    .create();
        }
    }
}
