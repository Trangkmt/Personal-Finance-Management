package com.example.expensemanagement.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensemanagement.Activitiy.LoginActivity;
import com.example.expensemanagement.R;
import com.google.firebase.auth.FirebaseAuth;
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE    = "title";
    private static final String ARG_SUBTITLE = "subtitle";

    public static PlaceholderFragment newInstance(String title, String subtitle) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUBTITLE, subtitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvSubtitle);
        Button btnLogout = view.findViewById(R.id.btn_logout_real);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE, "");
            tvTitle.setText(title);
            tvSubtitle.setText(getArguments().getString(ARG_SUBTITLE, ""));


            if (" Khác".equals(title)) {
                btnLogout.setVisibility(View.VISIBLE);

                btnLogout.setOnClickListener(v -> {
                    FirebaseAuth.getInstance().signOut();

                    Toast.makeText(getContext(), "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                });
            } else {
                btnLogout.setVisibility(View.GONE);
            }
        }
        return view;
    }
}