/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.igalia.wolvic.ui.widgets.settings;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.igalia.wolvic.R;
import com.igalia.wolvic.databinding.OptionsSavedLoginsBinding;
import com.igalia.wolvic.ui.adapters.LoginsAdapter;
import com.igalia.wolvic.ui.widgets.WidgetManagerDelegate;
import com.igalia.wolvic.ui.widgets.WidgetPlacement;
import com.igalia.wolvic.utils.SystemUtils;

import java.util.Comparator;
import java.util.concurrent.Executor;

import mozilla.components.concept.storage.Login;

class SavedLoginsOptionsView extends SettingsView {

    static final String LOGTAG = SystemUtils.createLogtag(SavedLoginsOptionsView.class);

    private OptionsSavedLoginsBinding mBinding;
    private LoginsAdapter mAdapter;
    private WidgetManagerDelegate mWidgetManager;
    private Executor mMainExecutor;

    public SavedLoginsOptionsView(Context aContext, WidgetManagerDelegate aWidgetManager) {
        super(aContext, aWidgetManager);
        initialize(aContext);
    }

    protected void initialize(Context aContext) {
        mWidgetManager = (WidgetManagerDelegate)aContext;

        if(mWidgetManager!=null){
            mMainExecutor = mWidgetManager.getServicesProvider().getExecutors().mainThread();
        }else{
            Log.e(LOGTAG, "Null pointer, SavedLoginsOptionsView::initialize mWidgetManager");
        }

        mAdapter = new LoginsAdapter(getContext(), mCallback, LoginsAdapter.SAVED_LOGINS_LIST);

        updateUI();
    }

    @Override
    protected void updateUI() {
        super.updateUI();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Inflate this data binding layout
        mBinding = DataBindingUtil.inflate(inflater, R.layout.options_saved_logins, this, true);

        // Header
        mBinding.headerLayout.setBackClickListener(view -> onDismiss());

        // Adapters
        mBinding.loginsList.setAdapter(mAdapter);

        // Footer
        mBinding.footerLayout.setFooterButtonClickListener(mClearAllListener);

        mBinding.setIsEmpty(true);

        mBinding.executePendingBindings();
    }

    protected OnClickListener mClearAllListener = (view) -> {
        reset();
    };

    @Override
    public Point getDimensions() {
        return new Point( WidgetPlacement.dpDimension(getContext(), R.dimen.settings_dialog_width),
                WidgetPlacement.dpDimension(getContext(), R.dimen.settings_dialog_height));
    }


    @Override
    protected boolean reset() {
        if(mWidgetManager!=null){
            mWidgetManager.getServicesProvider().getLoginStorage().deleteEverything();
        }else{
            Log.e(LOGTAG, "Null pointer, SavedLoginsOptionsView::reset mWidgetManager");
        }
        updateAdapter();
        return true;
    }

    @Override
    public void onShown() {
        super.onShown();

        updateAdapter();
    }

    private Comparator<Login> mAZOriginComparator = (o1, o2) -> o1.getOrigin().compareTo(o2.getOrigin());

    private void updateAdapter() {
        if(mWidgetManager!=null){
            mWidgetManager.getServicesProvider().getLoginStorage().getLogins().thenAcceptAsync(logins -> {
                if (logins.isEmpty()) {
                    mBinding.setIsEmpty(true);

                } else {
                    mBinding.setIsEmpty(false);
                    logins.sort(mAZOriginComparator);
                    mAdapter.setItems(logins);
                }
                mBinding.loginsList.scrollToPosition(0);

            }, mMainExecutor).exceptionally(throwable -> {
                Log.d(LOGTAG, String.valueOf(throwable.getLocalizedMessage()));
                return null;
            });
        }else{
            Log.e(LOGTAG, "Null pointer, SavedLoginsOptionsView::updateAdapter mWidgetManager");
        }
    }

    @Override
    public void onHidden() {
        super.onHidden();
    }

    @Override
    protected SettingViewType getType() {
        return SettingViewType.SAVED_LOGINS;
    }

    protected LoginsAdapter.Delegate mCallback = new LoginsAdapter.Delegate() {
        @Override
        public void onLoginSelected(@NonNull View view, @NonNull Login login) {
            mDelegate.showView(SettingViewType.LOGIN_EDIT, login);
        }

        @Override
        public void onLoginDeleted(@NonNull View view, @NonNull Login login) {
            if(mWidgetManager!=null){
                mWidgetManager.getServicesProvider().getLoginStorage().delete(login).thenAcceptAsync(aBoolean -> updateAdapter(), mMainExecutor).exceptionally(throwable -> {
                    Log.d(LOGTAG, String.valueOf(throwable.getLocalizedMessage()));
                    return null;
                });
            }else{
                Log.e(LOGTAG, "Null pointer, SavedLoginsOptionsView::onLoginDeleted mWidgetManager");
            }
        }
    };
}
