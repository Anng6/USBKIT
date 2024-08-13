package com.anng6.usbkit.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.anng6.usbkit.databinding.FragmentWebcamBinding;
import rikka.recyclerview.RecyclerViewKt;

public class WebcamFragment extends BaseFragment {

    private FragmentWebcamBinding binding;

    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWebcamBinding.inflate(inflater, container, false);

        setupToolbar(binding.toolbar, "Webcam");
        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setSubtitle("TODO");

        binding.appBar.setLiftable(true);
        binding.recyclerView
                .getBorderViewDelegate()
                .setBorderVisibilityChangedListener(
                        (top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
