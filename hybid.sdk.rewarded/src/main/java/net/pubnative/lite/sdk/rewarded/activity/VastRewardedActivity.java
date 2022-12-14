// The MIT License (MIT)
//
// Copyright (c) 2020 PubNative GmbH
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package net.pubnative.lite.sdk.rewarded.activity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import net.pubnative.lite.sdk.HyBid;
import net.pubnative.lite.sdk.presenter.AdPresenter;
import net.pubnative.lite.sdk.rewarded.HyBidRewardedBroadcastReceiver;
import net.pubnative.lite.sdk.utils.Logger;
import net.pubnative.lite.sdk.vpaid.PlayerInfo;
import net.pubnative.lite.sdk.vpaid.VideoAd;
import net.pubnative.lite.sdk.vpaid.VideoAdCacheItem;
import net.pubnative.lite.sdk.vpaid.VideoAdListener;
import net.pubnative.lite.sdk.vpaid.VideoAdView;

public class VastRewardedActivity extends HyBidRewardedActivity implements AdPresenter.ImpressionListener {
    private static final String TAG = VastRewardedActivity.class.getSimpleName();
    private boolean mReady = false;
    private boolean mFinished = false;

    private VideoAdView mVideoPlayer;
    private VideoAd mVideoAd;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        super.onCreate(savedInstanceState);

        try {
            if (getAd() != null) {
                mVideoAd = new VideoAd(this, getAd(), false, true, this);
                mVideoAd.setRewarded(true);
                mVideoAd.bindView(mVideoPlayer);
                mVideoAd.setAdListener(mVideoAdListener);
                setProgressBarVisible();

                VideoAdCacheItem adCacheItem = HyBid.getVideoAdCache().remove(getZoneId());
                if (adCacheItem != null) {
                    mVideoAd.setVideoCacheItem(adCacheItem);
                    if (adCacheItem.getAdParams() != null && adCacheItem.getAdParams().getAdIcon() != null) {
                        setupContentInfo(adCacheItem.getAdParams().getAdIcon());
                    } else {
                        setupContentInfo();
                    }
                } else {
                    setupContentInfo();
                }

                mVideoPlayer.postDelayed(() -> mVideoAd.load(), 1000);
            }
        } catch (Exception exception) {
            Logger.e(TAG, exception.getMessage());
            if (getBroadcastSender() != null) {
                getBroadcastSender().sendBroadcast(HyBidRewardedBroadcastReceiver.Action.ERROR);
            }
            finish();
        }
    }

    @Override
    public View getAdView() {
        if (getAd() != null) {
            mVideoPlayer = new VideoAdView(this);
            return mVideoPlayer;
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoAd != null) {
            mVideoAd.destroy();
            mReady = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeAd();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseAd();
    }

    @Override
    public void onBackPressed() {
        if (mFinished) {
            super.onBackPressed();
        }
    }

    @Override
    protected void pauseAd() {
        if (mReady) {
            mVideoAd.pause();
        }

        if (mFinished) {
            mVideoAd.pauseEndCardCloseButtonTimer();
        }
    }

    @Override
    protected void resumeAd() {
        if (!mIsFeedbackFormOpen && mReady) {
            mVideoAd.resume();
        }

        if (mFinished) {
            mVideoAd.resumeEndCardCloseButtonTimer();
        }
    }

    private final VideoAdListener mVideoAdListener = new VideoAdListener() {
        @Override
        public void onAdLoadSuccess() {
            if (!mReady) {
                mReady = true;

                setProgressBarInvisible();
                mVideoAd.show();
            }
        }

        @Override
        public void onAdLoadFail(PlayerInfo info) {
            setProgressBarInvisible();
            if (getBroadcastSender() != null) {
                getBroadcastSender().sendBroadcast(HyBidRewardedBroadcastReceiver.Action.ERROR);
            }
            finish();
        }

        @Override
        public void onAdClicked() {
            if (getBroadcastSender() != null) {
                getBroadcastSender().sendBroadcast(HyBidRewardedBroadcastReceiver.Action.CLICK);
            }
        }

        @Override
        public void onAdDidReachEnd() {
            mReady = false;
            mFinished = true;
            showRewardedCloseButton();
            if (getBroadcastSender() != null) {
                getBroadcastSender().sendBroadcast(HyBidRewardedBroadcastReceiver.Action.FINISH);
            }
        }

        @Override
        public void onAdDismissed() {
            dismiss();
        }

        @Override
        public void onAdDismissed(int progressPercentage) {
            dismiss();
        }

        @Override
        public void onAdExpired() {

        }

        @Override
        public void onAdSkipped() {
            mFinished = true;
        }

        @Override
        public void onAdStarted() {

        }
    };

    @Override
    public void onImpression() {
        if (getBroadcastSender() != null) {
            getBroadcastSender().sendBroadcast(HyBidRewardedBroadcastReceiver.Action.OPEN);
        }
    }
}
