/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Browser;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.ImageModel;
import com.android.mms.model.MediaModel;
import com.android.mms.model.RegionModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.model.VideoModel;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.MmsMessageSender;
import com.android.mms.util.Recycler;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.mediatek.encapsulation.com.mediatek.CellConnService.EncapsulatedCellConnMgr;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.internal.EncapsulatedR;
import com.mediatek.mms.ext.IMmsTextSizeAdjust;
import com.mediatek.mms.ext.IMmsTextSizeAdjustHost;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSimInfoManager;
import com.mediatek.encapsulation.android.text.style.EncapsulatedBackgroundImageSpan;

//add for attachment enhance
import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
import java.util.ArrayList;

/// M: ALPS00619099, highlight matched search string in mms player @ {
import java.util.regex.Pattern;
/// @}

/** M:
 * Plays the given slideshow in full-screen mode with a common controller.
 */
public class MmsPlayerActivity extends Activity implements Contact.UpdateListener, IMmsTextSizeAdjustHost {
    private static final String TAG = "MmsPlayerActivity";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;
    private MmsPlayerActivityAdapter mListAdapter;
    private static final int MENU_PLAYASSLIDE = 1;
    private Uri mMessageUri;
    private ListView mListView;
    private ContentResolver mContentResolver;
    private Cursor mCursor = null;
    private AsyncQueryHandler mQueryHandler = null;

    // context menu item
    private static final int MENU_COPY_MESSAGE_TEXT         = 1;
    private static final int MENU_ADD_TO_BOOKMARK           = 2;
    private static final int MENU_ADD_TO_CONTACTS           = 3;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS   = 4;
    private static final int MENU_SEND_EMAIL                = 5;
    private static final int MENU_CALL_BACK                 = 6;
    private static final int MENU_SEND_SMS                  = 7;
    private static final int MENU_SELECT_TEXT               = 8; // add for select text copy
    private static final int MENU_LOCK                      = 90;
    private static final int MENU_UNLOCK                    = 100;
    
    public static final int REQUEST_CODE_ADD_CONTACT      = 1;
    private Context mContext;
    private ArrayList<String> mURLs = new ArrayList<String>();
    private String mCopyText;

    private AsyncDialog mAsyncDialog;
    
    //add for cmcc dir ui begin
    private boolean mDirMode = false;
    private TextView mRecipient;
    private TextView mDate;
    private TextView mByCard;
    private TextView mMmsSubject;
    private ImageView mHasFileAttachment;
    private TextView mAttachName;
    private ImageView mLockedInd;
    private boolean mLocked;
    private Long mDateLong;
    private int mSimId;
    private String mNumber;
    private String mName;
    private int mIndex = -1; //the first number index in ContactList can be added to contact 
    private ContactList mContactList;
    private boolean mShowAddContact = false;
    private Uri mMsgUri;
    private int mMsgBox;
    private boolean mShowResend;
    private long mThreadId;
    private long mMsgId;
    private int mDeliveryReport;
    private int mReadReport;
    private String mSubject;
    private SlideshowModel mSlideshow;
    private int mHomeBox = 0;
    private boolean mClearCache = true;
    private boolean mParseMsgUriSuccess = false;
    private static final String[] PDU_PROJECTION = new String[] {
        Mms._ID,
        Mms.MESSAGE_BOX,
        Mms.THREAD_ID,
        Mms.RETRIEVE_TEXT,
        Mms.SUBJECT,
        Mms.SUBJECT_CHARSET,
        Mms.DELIVERY_REPORT,
        Mms.READ_REPORT,
        Mms.DATE,
        EncapsulatedTelephony.Mms.SIM_ID,
        Mms.LOCKED
    };
    private static final int PDU_COLUMN_ID                    = 0;
    private static final int PDU_COLUMN_MESSAGE_BOX           = 1;
    private static final int PDU_COLUMN_THREAD_ID             = 2;
    private static final int PDU_COLUMN_RETRIEVE_TEXT         = 3;
    private static final int PDU_COLUMN_SUBJECT               = 4;
    private static final int PDU_COLUMN_SUBJECT_CHARSET       = 5;
    private static final int COLUMN_MMS_DELIVERY_REPORT       = 6;
    private static final int COLUMN_MMS_READ_REPORT           = 7;    
    private static final int COLUMN_MMS_DATE                  = 8;
    private static final int COLUMN_MMS_SIM_ID                = 9;
    private static final int COLUMN_MMS_LOCKED                = 10;

    // option menu item
    private static final int MENU_REPLY           = 2;
    private static final int MENU_RESEND          = 3;
    private static final int MENU_ADD_CONTACT     = 4;
    private static final int MENU_VIEW_REPORT     = 5;
    private static final int MENU_FORWARD         = 6;
    private static final int MENU_DELETE          = 7;
    private static final int MNEU_DETAIL          = 8;
    private static final int MENU_CALL_RECIPIENT  = 9;
    private static final int MENU_CALL_RECIPIENT_BY_VT  = 10;
    private static final int MENU_COPY_TO_SDCARD        = 11;
    //add for cmcc dir ui end

    ///M: add for Plugins
    private IMmsTextSizeAdjust mMmsTextSizeAdjustPlugin = null; 

    /// M: ALPS00619099, highlight matched search string in mms player @ {
    private Pattern mHighlight = null;
    /// @}

    public static MmsPlayerActivity sCurrentActivity;
    private static EncapsulatedCellConnMgr mCellMgr = null;
    // Whether or not we are currently enabled for SMS. This field is updated in onStart to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initPlugin(this);
        mContext = this;
        mAsyncDialog = new AsyncDialog(this);
        Intent intent = getIntent();
        mMessageUri = intent.getData();
        //add for cmcc dir ui begin
        mDirMode = intent.getBooleanExtra("dirmode", false);
        mHomeBox = intent.getIntExtra("folderbox", 0);

        /// M: ALPS00619099, highlight matched search string in mms player @ {
        String highlightString = getIntent().getStringExtra("highlight");
        mHighlight = highlightString == null
            ? null
            : Pattern.compile(Pattern.quote(highlightString), Pattern.CASE_INSENSITIVE);
        /// @}

        //add for cmcc dir ui end
        setContentView(R.layout.mms_player_activity);
        mListView = (ListView) findViewById(R.id.slide_item_list);
        //add for cmcc dir mode begin
        if (mDirMode) {
            View header = LayoutInflater.from(this).inflate(R.layout.foldermode_mms_header, null);
            mRecipient = (TextView)header.findViewById(R.id.msg_recipent);
            mDate = (TextView)header.findViewById(R.id.msg_date);
            mByCard = (TextView)header.findViewById(R.id.by_card);
            mMmsSubject = (TextView)header.findViewById(R.id.msg_subject);
            mHasFileAttachment = (ImageView)header.findViewById(R.id.has_file_attachment);
            mAttachName = (TextView)header.findViewById(R.id.attach_Name);
            mMmsSubject.setVisibility(View.GONE);
            mHasFileAttachment.setVisibility(View.GONE);
            mAttachName.setVisibility(View.GONE);
            mLockedInd = (ImageView)header.findViewById(R.id.locked_indicator);
            mLockedInd.setVisibility(View.GONE);
            mListView.addHeaderView(header);
        }
        asyncLoadMms();
        if (!mDirMode) {
            sCurrentActivity = this;
        }
    }

    private void initListAdapter(Uri msg) {
        //PduBody body = null;
        SlideshowModel slideshowModel = mSlideshow;
        if (slideshowModel == null) {
            return;
        }

        int slideNum = slideshowModel.size();
        Log.i(TAG, "initListAdapter,getPartsNum:" + slideNum);
        ArrayList<MmsPlayerActivityItemData> attachments = new ArrayList<MmsPlayerActivityItemData>(slideNum);
        for (int i = 0; i < slideNum; i++) {
            SlideModel slideModel = slideshowModel.get(i);
            if (slideModel == null) {
                break;
            }
            String text = null;
            Uri imageUri = null;
            String imageType = null;
            Uri videoUri = null;
            String videoType = null;
            String audioName = null;
            Uri audioUri = null;
            String audioType = null;
            int mImageOrVideoLeft = 0;
            int mImageOrVideoTop = 0;
            int mImageOrVideoWidth = 0;
            int mImageOrVideoHeight = 0;
            int mTextLeft = 0;
            int mTextTop = 0;
            int mTextWidth = 0;
            int mTextHeight = 0;
            for (MediaModel media : slideModel) {
                if (media.isImage()) {
                    imageUri = media.getUri();
                    imageType = media.getContentType();
                    RegionModel imageRegionModel = ((ImageModel) media).getRegion();
                    if (imageRegionModel != null) {
                        mImageOrVideoLeft = imageRegionModel.getLeft();
                        mImageOrVideoTop = imageRegionModel.getTop();
                        mImageOrVideoWidth = imageRegionModel.getWidth();
                        mImageOrVideoHeight = imageRegionModel.getHeight();
                    }
                } else if (media.isVideo()) {
                    videoUri = media.getUri();
                    videoType = media.getContentType();
                    RegionModel videoRegionModel = ((VideoModel) media).getRegion();
                    if (videoRegionModel != null) {
                        mImageOrVideoLeft = videoRegionModel.getLeft();
                        mImageOrVideoTop = videoRegionModel.getTop();
                        mImageOrVideoWidth = videoRegionModel.getWidth();
                        mImageOrVideoHeight = videoRegionModel.getHeight();
                    }
                } else if (media.isAudio()) {
                    audioName = media.getSrc();
                    audioUri = media.getUri();
                    audioType = media.getContentType();
                    Log.i(TAG, " add audio name: " + audioName);
                } else if (media.isText()) {
                    TextModel sm = slideModel.getText();
                    if (sm != null) {
                        text = sm.getText();
                        RegionModel textRegionModel = sm.getRegion();
                        if (textRegionModel != null) {
                            mTextLeft = textRegionModel.getLeft();
                            mTextTop = textRegionModel.getTop();
                            mTextWidth = textRegionModel.getWidth();
                            mTextHeight = textRegionModel.getHeight();
                        }
                    }
                }
            }
            Log.i(TAG, "Add slide: " + i + " imageUri is: " + imageUri + " videoUri is: " + videoUri
                + " audio name is: " + audioName);
            attachments.add(new MmsPlayerActivityItemData(this, imageUri, videoUri, audioName, text, mImageOrVideoLeft,
                    mImageOrVideoTop, mImageOrVideoWidth, mImageOrVideoHeight, mTextLeft, mTextTop, mTextWidth,
                    mTextHeight, imageType, videoType, audioUri, audioType));
        }

        if (mDirMode) {
            if (mSubject != null) {
                mMmsSubject.setText(getString(R.string.subject_label) + mSubject);
                mMmsSubject.setVisibility(View.VISIBLE);
            }
            int fileAttachmentCount = slideshowModel.sizeOfFilesAttach();
            if (fileAttachmentCount > 0) {
                Log.i(TAG, "has file attachment");
                String src = slideshowModel.getAttachFiles().get(0).getSrc();
                mAttachName.setText(src);
                mHasFileAttachment.setVisibility(View.VISIBLE);
                mAttachName.setVisibility(View.VISIBLE);
             /// M: @{

             //add for attachment enhance
                IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
                if (mMmsAttachmentEnhancePlugin != null) {
                    mMmsAttachmentEnhancePlugin.setAttachmentName(mAttachName,fileAttachmentCount);
                }
            /// @}
            }
        }

        attachments.trimToSize();
        /// M: ALPS00619099, highlight matched search string in mms player @ {
        if (mDirMode && (mHighlight != null)) {
            mListAdapter = new MmsPlayerActivityAdapter(this, attachments, mHighlight);
        } else {
            mListAdapter = new MmsPlayerActivityAdapter(this, attachments);
        }
        /// @}
        mListView.setAdapter(mListAdapter);
        mListView.setOnCreateContextMenuListener(mTextMenuCreateListener);
    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        sCurrentActivity = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
    
    @Override
    protected void onPause() {
        if (mListAdapter != null && mClearCache) {
            mListAdapter.clearAllCache();
        }
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Contact.removeListener(this);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if (MmsConfig.getAdjustFontSizeEnabled()) {
            float textSize = MessageUtils.getPreferenceValueFloat(this, SettingListActivity.TEXT_SIZE, 18);
            setTextSize(textSize);
        }
        if (mMmsTextSizeAdjustPlugin != null) {
            mMmsTextSizeAdjustPlugin.refresh();
        }
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mms);
        Contact.addListener(this);

        /// M: ALPS00440523, print mms mem @{
        MmsConfig.printMmsMemStat(this, "MmsPlayerActivity.onStart");
        /// @}
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mDirMode && mParseMsgUriSuccess && mNumber != null && mNumber.length() > 0) {
            updateRecipient();
        }
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
        mClearCache = true;
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (mDirMode && isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PLAYASSLIDE, 0, R.string.play_as_slideshow);
        // add for cmcc dir ui mode begin
        if (mDirMode) {
            if (haveSomethingToCopyToSDCard(mMsgId)) {
                menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard);
            }
            if (mIsSmsEnabled) {
                menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
                menu.add(0, MENU_DELETE, 0, R.string.delete_message);
                // add extra menu option by condition
                if (mMsgBox == Mms.MESSAGE_BOX_INBOX) {
                    menu.add(R.id.slidegroup, MENU_REPLY, 1, R.string.menu_reply);
                } else if ((mMsgBox == Mms.MESSAGE_BOX_OUTBOX) && mShowResend/*send fail*/) {
                    menu.add(R.id.slidegroup, MENU_RESEND, 1, R.string.menu_retry_sending);
                }
            }
            //show report
            if (((mMsgBox == Mms.MESSAGE_BOX_OUTBOX) || (mMsgBox == Mms.MESSAGE_BOX_SENT))
                && (mDeliveryReport == 128 || mReadReport == 128/*show only has been requested*/)) {
                menu.add(R.id.slidegroup, MENU_VIEW_REPORT, 0, R.string.view_delivery_report);
            }
            menu.add(0, MNEU_DETAIL, 0, R.string.slideshow_details);
            if (isRecipientCallable()) {
                MenuItem item = menu.add(0, MENU_CALL_RECIPIENT, 0, R.string.menu_call)
                    .setIcon(R.drawable.ic_menu_call)
                    .setTitle(R.string.menu_call);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                if (EncapsulatedFeatureOption.MTK_VT3G324M_SUPPORT) {
                    menu.add(0, MENU_CALL_RECIPIENT_BY_VT, 0, R.string.call_video_call)
                            .setIcon(R.drawable.ic_video_call)
                            .setTitle(R.string.call_video_call);
                }
            }
        }
        // add for cmcc dir ui mode end
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

     IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance) MmsPluginManager
              .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

        switch(item.getItemId()) {
        case MENU_PLAYASSLIDE:
            Intent intent = new Intent(this, SlideshowActivity.class);
            intent.setData(mMessageUri);
            this.startActivity(intent);
            break;
         // add for cmcc dir ui mode begin
         case MENU_FORWARD:
            // this is a little slow if mms is big.
            new Thread(new Runnable() {
                public void run() {
                    forwardMms();
                }
            }).start();
            break;
        case MENU_DELETE:
            confirmToDeleteMessage(mMsgUri);
            break;
        case MNEU_DETAIL:
            onClickDetails(null);
            break;
        case MENU_ADD_CONTACT:
            addToContact();
            break;
        case MENU_REPLY:
            int simId = -1;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                int slot = EncapsulatedSimInfoManager.getSlotById(this, mSimId);
                MmsLog.d(TAG, "slot is:" + slot + ",simId:" + mSimId);
                if (slot >= 0) {
                    simId = mSimId;
                }
                // if the received message card is not in slot
                // we use system settings to decide, make simId == -1
            }
            MessageUtils.replyMessage(this, mNumber, simId);
            break;
        case MENU_RESEND:
            Uri uri = null;

            /// M: @{ Add for ALPS01092608, when resend a mms, we should check mms size firstly.
            int attachmentSize = 0;
            int messageSize = 0;
            if (!(mMmsAttachmentEnhancePlugin != null && mMmsAttachmentEnhancePlugin
                    .isSupportAttachmentEnhance() == true)) {
                MmsLog.d(TAG, "ResendMessage(): isSupportAttachmentEnhance is false");
                for (FileAttachmentModel fileAttachment : mSlideshow.getAttachFiles()) {
                    attachmentSize += fileAttachment.getAttachSize();
                }
            }
            messageSize = mSlideshow.getCurrentSlideshowSize() + attachmentSize;
            MmsLog.d(TAG, "ResendMessage(): messageSize=" + messageSize);
            if (messageSize > MmsConfig.getUserSetMmsSizeLimit(true)) {
                MessageUtils.showErrorDialog(MmsPlayerActivity.this,
                        getResources().getString(R.string.exceed_message_size_limitation),
                        getResources().getString(R.string.exceed_message_size_limitation));
                break;
            }
            ///@}

            try {
                final PduPersister persister = PduPersister.getPduPersister(this);
                final long threadId = mThreadId;

                int requestType = EncapsulatedCellConnMgr.REQUEST_TYPE_SIMLOCK;
                if (mCellMgr == null) {
                    mCellMgr = new EncapsulatedCellConnMgr();
                }
                mCellMgr.register(getApplication());
                final int mSlotId = EncapsulatedSimInfoManager.getSlotById(this, mSimId);
                MmsLog.d(MmsApp.TXN_TAG, "check pin and...: simId=" + mSimId + "\t slotId=" + mSlotId);
                            
                int mResult = mCellMgr.handleCellConn(mSlotId, requestType,new Runnable() {
                    public void run() {
                        int nRet = mCellMgr.getResult();
                        if (mCellMgr.RESULT_ABORT == nRet || mCellMgr.RESULT_OK == nRet) {
                            MmsLog.d(MmsApp.TXN_TAG, "serviceComplete result = " + nRet);
                            mCellMgr.unregister();
                            return;
                        }
                        MmsLog.d(MmsApp.TXN_TAG, "serviceComplete result = " + nRet);
                        Uri uri = null;
                        try {
                            uri = persister.move(mMsgUri, Mms.Draft.CONTENT_URI);
                            MessageSender sender = new MmsMessageSender(MmsPlayerActivity.this, uri, mSlideshow.getCurrentSlideshowSize());
                            // if(EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                sender.setSimId(mSimId);
                                Log.d(TAG, "MmsPlayerActivity: resendMsg  mSimId ="+mSimId);
                            //}
                            sender.sendMessage(threadId);
                        } catch (MmsException e) {
                            Log.e(TAG, "Can't resend mms. + uri="+uri);
                        }
                        // Make sure this thread isn't over the limits in message count
                        Recycler.getMmsRecycler().deleteOldMessagesByThreadId(getApplicationContext(), threadId);
                        mCellMgr.unregister();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message: threadId=" + mThreadId, e);
            }
            finish();
            break;
        case MENU_VIEW_REPORT:
            showDeliveryReport();
            break;
        case android.R.id.home:
            if (mDirMode) {
                Intent it = new Intent(this, FolderViewList.class);
                it.putExtra("floderview_key", mHomeBox);
                finish();
                startActivity(it);
            } else {
                //open what?
                finish();
            }
            break;
        case MENU_CALL_RECIPIENT:
            dialRecipient(false);
            break;
        case MENU_CALL_RECIPIENT_BY_VT:
            dialRecipient(true);
            break;
        case MENU_COPY_TO_SDCARD: 
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(MmsPlayerActivity.this, getString(R.string.Insert_sdcard), Toast.LENGTH_LONG).show();
                return false;
            }

            final long iMsgId = mMsgId;
            Intent i = new Intent(MmsPlayerActivity.this, MultiSaveActivity.class);
            i.putExtra("msgid", iMsgId);
                /// M: @{

                //add for attachment enhance
                //IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
                
                if (mMmsAttachmentEnhancePlugin != null) {
                    mMmsAttachmentEnhancePlugin.setSaveAttachIntent(i,IMmsAttachmentEnhance.MMS_SAVE_ALL_ATTACHMENT);
                }
                /// @}
            startActivityForResult(i,0);
            return true;
        case MENU_LOCK:
            lockMessage(true);
            break;
        case MENU_UNLOCK:
            lockMessage(false);
            break;
        default:
            return false;
        // add for cmcc dir ui mode end
        }
        return false;
    }

    private void confirmToDeleteMessage(final Uri msgUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setMessage(mLocked ? R.string.confirm_delete_locked_message :
            R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        /// M: fix bug ALPS00351620; for requery searchactivity.
                                        SearchActivity.setNeedRequery();
                                        SqliteWrapper.delete(MmsPlayerActivity.this, getContentResolver(),
                                                msgUri, null, null);
                                        dialog.dismiss();
                                        Intent mIntent = new Intent();
                                        mIntent.putExtra("delete_flag",true);
                                        setResult(RESULT_OK, mIntent);
                                        finish();
                                    }                                          
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private final OnCreateContextMenuListener mTextMenuCreateListener =
        new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            TextMenuClickListener l = new TextMenuClickListener();
            int itemId = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
            //cmcc dir mode code begin
            if (mDirMode) {
                if (itemId == 0) {
                    //the header did not response
                    return;
                }
                itemId--;// list view has a header now.
            }
            //cmcc dir mode code end
            Log.i(TAG, "getItem Id: " + itemId);
            MmsPlayerActivityItemData data = (MmsPlayerActivityItemData) mListAdapter.getItem(itemId);
            if (data != null) {
                addCallAndContactMenuItems(menu, l, data.getText());
            } else {
                Log.i(TAG, "getItem null");
            }
            
        }
    };
    
    private final class TextMenuClickListener implements MenuItem.OnMenuItemClickListener {
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
            case MENU_COPY_MESSAGE_TEXT:
                ClipboardManager clip =
                    (ClipboardManager)MmsPlayerActivity.this.getSystemService(mContext.CLIPBOARD_SERVICE);
                clip.setText(mCopyText); 
                return true;

            case MENU_ADD_TO_BOOKMARK:
                if (mURLs.size() == 1) {
                    Browser.saveBookmark(mContext, null, mURLs.get(0));
                } else if (mURLs.size() > 1) {
                    CharSequence[] items = new CharSequence[mURLs.size()];
                    for (int i = 0; i < mURLs.size(); i++) {
                        items[i] = mURLs.get(i);
                    }
                    new AlertDialog.Builder(mContext)
                        .setTitle(R.string.menu_add_to_bookmark)
                        .setIcon(R.drawable.ic_dialog_menu_generic)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Browser.saveBookmark(mContext, null, mURLs.get(which));
                                }
                            })
                        .show();
                }
                return true;

            case MENU_ADD_ADDRESS_TO_CONTACTS: 
                Intent mAddContactIntent = item.getIntent();
                startActivityForResult(mAddContactIntent, REQUEST_CODE_ADD_CONTACT);
                return true;

            // add for select text copy
            case MENU_SELECT_TEXT:
                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext)
                                                    .setPositiveButton(R.string.yes, null);
                LayoutInflater factory = LayoutInflater.from(dialog.getContext());
                final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
                EditText contentSelector = (EditText)textEntryView.findViewById(R.id.content_selector);
                contentSelector.setText(mCopyText);
                dialog.setTitle(R.string.select_text)
                    .setView(textEntryView)
                    .show();
                return true;

            default:
                return false;
            }
        }
    }


    private void addCallAndContactMenuItems(
            ContextMenu menu, TextMenuClickListener l, CharSequence text) {
        // Add all possible links in the address & message
        StringBuilder textToSpannify = new StringBuilder();
        textToSpannify.append(text);

        SpannableString msg = new SpannableString(textToSpannify.toString());
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris =
            MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));
        mURLs.clear();
        menu.setHeaderTitle(R.string.message_options);
        if (text != null && text.length() > 0) {
            menu.add(0, MENU_COPY_MESSAGE_TEXT, 0, R.string.copy_message_text)
                    .setOnMenuItemClickListener(l);
            // add for select text copy
            menu.add(0, MENU_SELECT_TEXT, 0, R.string.select_text)
                    .setOnMenuItemClickListener(l);
            mCopyText = (String) text;
        }
        while (uris.size() > 0) {
            String uriString = uris.remove(0);
            // Remove any dupes so they don't get added to the menu multiple times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }

            int sep = uriString.indexOf(":");
            String prefix = null;
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                if ("mailto".equalsIgnoreCase(prefix) || "tel".equalsIgnoreCase(prefix)) {
                    uriString = uriString.substring(sep + 1);
                }
            }
            boolean addToContacts = false;
            if ("mailto".equalsIgnoreCase(prefix)) {
                String sendEmailString = mContext.getString(
                        R.string.menu_send_email).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("mailto:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                Log.i(TAG, "send email number: " + sendEmailString);
                menu.add(0, MENU_SEND_EMAIL, 0, sendEmailString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
                addToContacts = !MessageUtils.haveEmailContact(uriString, mContext);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                String callBackString = mContext.getString(
                        R.string.menu_call_back).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                Log.i(TAG, "call back number: " + callBackString);
                menu.add(0, MENU_CALL_BACK, 0, callBackString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
                
                if (text != null && text.toString().replaceAll("\\-", "").contains(uriString)) {
                    String sendSmsString = mContext.getString(
                        R.string.menu_send_sms).replace("%s", uriString);
                    Intent intentSms = new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:" + uriString));
                    intentSms.setClassName(mContext, "com.android.mms.ui.SendMessageToActivity");
                    intentSms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Log.i(TAG, "send sms number: " + sendSmsString);
                    /// M: disable when non-default sms
                    if (MmsConfig.isSmsEnabled(this)) {
                        menu.add(0, MENU_SEND_SMS, 0, sendSmsString)
                            .setOnMenuItemClickListener(l)
                            .setIntent(intentSms);
                    }
                }
                addToContacts = !isNumberInContacts(uriString);
            } else {
                //add URL to book mark
                if (mURLs.size() <= 0) {
                    menu.add(0, MENU_ADD_TO_BOOKMARK, 0, R.string.menu_add_to_bookmark)
                    .setOnMenuItemClickListener(l);
                }
                mURLs.add(uriString);
            }
            if (addToContacts) {
                Intent intent = ConversationList.createAddContactIntent(uriString);
                String addContactString = mContext.getString(
                        R.string.menu_add_address_to_contacts).replace("%s", uriString);
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, addContactString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
            }
        }
    }
    
    private boolean isNumberInContacts(String phoneNumber) {
        return Contact.get(phoneNumber, true).existsInDatabase();
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged(MessageListAdapter adapter);
    }

    //add for cmcc dir ui begin
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mDirMode) {
            return true;
        }

        if (!mParseMsgUriSuccess || mNumber == null || mNumber.length() <= 0) {
            return true;
        }
        // M: fix bug ALPS00351962
        if (mContactList == null) {
            Log.d(TAG, "onPrepareOptionsMenu mContactList == null1");
            mContactList = ContactList.getByNumbers(mNumber, false, true);
            if (mContactList == null) {
                Log.d(TAG, "onPrepareOptionsMenu mContactList == null2");
                return true;
            }
        }

        //mContactList = ContactList.getByNumbers(mNumber, false, true);
        int count  = 0;
        mIndex = -1;
        for (Contact contact : mContactList) {
            if (!contact.existsInDatabase() && MessageUtils.canAddToContacts(contact)) {
                mIndex = count;
                Log.d(TAG, "not in contact[number:" + contact.getNumber() + ",name:" + contact.getName());
                break;
            }
            count++;
        }
        boolean menuAddExist = (menu.findItem(MENU_ADD_CONTACT) != null);
        if (mIndex != -1/*ICS has no add multi recipients ui*/) {
            if (!menuAddExist) {
                menu.add(R.id.slidegroup, MENU_ADD_CONTACT, 1, R.string.menu_add_to_contacts);
            }
        } else {
            menu.removeItem(MENU_ADD_CONTACT);
        }

        if (mIsSmsEnabled) {
            if (mLocked) {
                if (menu.findItem(MENU_LOCK) != null) {
                    menu.removeItem(MENU_LOCK);
                }
                if (menu.findItem(MENU_UNLOCK) == null) {
                    menu.add(0, MENU_UNLOCK, 0, R.string.menu_unlock);
                }
            } else {
                if (menu.findItem(MENU_UNLOCK) != null) {
                    menu.removeItem(MENU_UNLOCK);
                }
                if (menu.findItem(MENU_LOCK) == null) {
                    menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
                }
            }
        }
        return true;
    }

    private void parseMsgUri() {
        Log.d(TAG, "uri:" + mMsgUri.toString());
        Cursor c = null;
        try {
            c = SqliteWrapper.query(this, getContentResolver(), mMsgUri,
                            PDU_PROJECTION, null, null, null);
            if (c == null || c.getCount() != 1) {
                Log.e(TAG, "count is not 1!!");
                mParseMsgUriSuccess = false;
                return;
            }
            c.moveToFirst();
            mMsgBox = c.getInt(PDU_COLUMN_MESSAGE_BOX);
            mThreadId = c.getLong(PDU_COLUMN_THREAD_ID);
            mMsgId = c.getLong(PDU_COLUMN_ID);
            mSubject = c.getString(PDU_COLUMN_SUBJECT);
            mDeliveryReport = c.getInt(COLUMN_MMS_DELIVERY_REPORT);
            mReadReport = c.getInt(COLUMN_MMS_READ_REPORT);
            mDateLong = c.getLong(COLUMN_MMS_DATE) * 1000L;
            mSimId = c.getInt(COLUMN_MMS_SIM_ID);
            mLocked = c.getInt(COLUMN_MMS_LOCKED) > 0;
            if (!TextUtils.isEmpty(mSubject)) {
                EncodedStringValue v = new EncodedStringValue(
                        c.getInt(PDU_COLUMN_SUBJECT_CHARSET),
                        PduPersister.getBytes(mSubject));
                mSubject = v.getString();
            }
            Log.d(TAG, "msgbox:" + mMsgBox + ",threadId:" + mThreadId + ",subject:" + mSubject);
            // get the mms related numbers
            PduPersister persister = PduPersister.getPduPersister(this);
            MultimediaMessagePdu pdu = (MultimediaMessagePdu)persister.load(mMsgUri);
            if (mMsgBox == Mms.MESSAGE_BOX_INBOX) {
                mNumber = pdu.getFrom().getString();
                Log.d(TAG, "this mms is from:" + mNumber);
            } else {
                EncodedStringValue numbers[] = pdu.getTo();
                mNumber = EncodedStringValue.concat(numbers);
                Log.d(TAG, "this mms is to:" + mNumber);
            }

            //check whether this mms was sent fail.
            if (mMsgBox == Mms.MESSAGE_BOX_OUTBOX) {
                checkSendFail();
            }
            mParseMsgUriSuccess = true;
        } catch(MmsException e) {
            Log.e(TAG, "MmsException:" + e.toString());
        } catch(ClassCastException e) {
            Log.e(TAG, "ClassCastException:" + e.toString());
        } finally {
            if (c != null) {
                c.close();
            }
        }
        // fill mms header view
        String date;
        date = MessageUtils.formatTimeStampString(this, mDateLong);
        if (mMsgBox == Mms.MESSAGE_BOX_INBOX) {
            //mName = getString(R.string.via_without_time_for_send) + ": " + mName;
            date = String.format(getString(R.string.received_on), date);
        } else {
            //mName = getString(R.string.via_without_time_for_recieve) + ": " + mName;
            date = String.format(getString(R.string.sent_on), date);
        }
        //mRecipient.setText("xx");//do this later.
        mDate.setText(date);
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            formatSimStatus();
        } else {
            mByCard.setVisibility(View.GONE);
        }
        mLockedInd.setVisibility(mLocked ? View.VISIBLE : View.GONE);
        //mark this mms as readed.
        markMmsRead();
        //update notification
        if (mMsgBox == Mms.MESSAGE_BOX_INBOX) {
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(this, mThreadId, false);
        }
    }

    private void formatSimStatus() {
        SpannableStringBuilder buffer = new SpannableStringBuilder();
        int simInfoStart = buffer.length();
        CharSequence simInfo = getSimInfo(this, mSimId);
        if (simInfo.length() > 0) {
            buffer.append(getString(R.string.by_card));
            simInfoStart = buffer.length();
            buffer.append(" ");
            buffer.append(simInfo);
        }
        int color = getResources().getColor(R.color.timestamp_color);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        buffer.setSpan(colorSpan, 0, simInfoStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mByCard.setText(buffer);
    }


    private CharSequence getSimInfo(Context context, int simId) {
        MmsLog.d(TAG, "getSimInfo simId = " + simId);
        //add this code for more safty
        if (simId == -1) {
            return "";
        }
        //get sim info
        EncapsulatedSimInfoManager simInfo = EncapsulatedSimInfoManager.getSIMInfoById(context, simId);
        if (null != simInfo) {
            String displayName = simInfo.getDisplayName();

            MmsLog.d(TAG, "SIMInfo simId=" + simInfo.getSimId() + " mDisplayName=" + displayName);

            if (null == displayName) {
                return "";
            }

            SpannableStringBuilder buf = new SpannableStringBuilder();
            buf.append("   ");
            /// M: Code analyze 007, For fix bug ALPS00238454, modify sim card
            // indicator max string length. @{
            if (displayName.length() < 20) {
                buf.append(displayName);
            } else {
                buf.append(displayName.substring(0,8) + "..." + displayName.substring(displayName.length() - 8,
                     displayName.length()));
                /// @}
            }
            buf.append("  ");

            //set background image
            int colorRes = (simInfo.getSlot() >= 0) ? simInfo.getSimBackgroundLightRes()
                 : EncapsulatedR.drawable.sim_background_locked;
            Drawable drawable = context.getResources().getDrawable(colorRes);
            buf.setSpan(new EncapsulatedBackgroundImageSpan(colorRes, drawable), 
                0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //set simInfo color
            int color = context.getResources().getColor(R.color.siminfo_color);
            buf.setSpan(new ForegroundColorSpan(color), 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return buf;
        }
        return "";
    }



    private void markMmsRead() {
        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);
                SqliteWrapper.update(getApplicationContext(), getContentResolver(), mMsgUri, values, null, null);
            }
        }).start();
    }

    private void checkSendFail() {
        long msgId = ContentUris.parseId(mMsgUri);
        Log.d(TAG, "mms id:" + msgId);
        Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        uriBuilder.appendQueryParameter("message", String.valueOf(msgId));

        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                uriBuilder.build(), null, null, null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    int errorType = cursor.getInt(cursor.getColumnIndexOrThrow(PendingMessages.ERROR_TYPE));
                    if (errorType == MmsSms.ERR_TYPE_GENERIC_PERMANENT) {
                        mShowResend = true;
                    } else {
                        mShowResend = false;
                    }
                } else {
                    Log.e(TAG, "query result not 1.!!");
                }
            } finally {
                cursor.close();
            }
        } else {
            Log.d(TAG, "query PendingMessages get null!");
        }
    }

    private void forwardMms() {
        SendReq sendReq = new SendReq();
        String subject = getString(R.string.forward_prefix);
        if (mSubject != null) {
            subject += mSubject;
        }
        sendReq.setSubject(new EncodedStringValue(subject));
        sendReq.setBody(mSlideshow.makeCopy());

        Uri uri = null;
        try {
            PduPersister persister = PduPersister.getPduPersister(this);
            // Copy the parts of the message here.
            uri = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to copy message: " + mMsgUri, e);
            MmsApp.getToastHandler().sendEmptyMessage(MmsApp.MSG_MMS_CAN_NOT_SAVE);
            return;
        }
        
        Intent intent = new Intent();
        intent.putExtra("forwarded_message", true);
        intent.putExtra("msg_uri", uri);
        intent.putExtra("subject", subject);
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        startActivity(intent);
    }

    private void addToContact() {
        //int count = mContactList.size();
        //switch(count) {
        //case 0:
        //    Log.e(TAG, "add contact, mCount == 0!");
        //    break;
        //case 1:
        if (mIndex != -1) {
            Intent intent = ConversationList.createAddContactIntent(mContactList.get(mIndex).getNumber());
            startActivity(intent);
        }
            //MessageUtils.addNumberOrEmailtoContact(mNumber, 0, this);
        //    break;
        //default:
            //MultiRecipientsActivity.setContactList(mContactList);
            //final Intent i = new Intent(getApplicationContext(), MultiRecipientsActivity.class);
            //startActivity(i);
        //    break;
        //}
    }

    public void onClickDetails(View v) {
        //show mms details info.
        String messageDetails = MessageUtils.getMmsDetail(this, mMsgUri, mSlideshow.getCurrentSlideshowSize(), mMsgBox);
        new AlertDialog.Builder(this)
                .setTitle(R.string.message_details_title)
                .setMessage(messageDetails)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .show();
    }

    private void showDeliveryReport() {
        // delivery report activity is partially shown, do not clear the cache.
        // or the back activity may set ImageView to null, and image disappear.
        mClearCache = false;
        Intent intent = new Intent(this, DeliveryReportActivity.class);
        intent.putExtra("message_id", mMsgId);
        intent.putExtra("message_type", "mms");
        startActivity(intent);
    }

    // We don't want to show the "call" option unless there is only one
    // recipient and it's a phone number.
    private boolean isRecipientCallable() {
        if (mContactList == null) {
            if (mNumber != null) {
                mContactList = ContactList.getByNumbers(mNumber, true, false);
            } else {
                MmsLog.e(TAG, "isRecipientCallable(): no number with msgUri=" + mMsgUri);
                return false;
            }
        }
        return (mContactList.size() == 1 && !mContactList.containsEmail());
    }

    private void dialRecipient(boolean isVideoCall) {
        if (isRecipientCallable()) {
            String number = mContactList.get(0).getNumber();
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            if (isVideoCall) {
                dialIntent.putExtra("com.android.phone.extra.video", true);
            }
            startActivity(dialIntent);
        }
    }
    //add for cmcc dir ui end
    
    /**
     * Looks to see if there are any valid parts of the attachment that can be copied to a SD card.
     * @param msgId
     */
    private boolean haveSomethingToCopyToSDCard(long msgId) {
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(MmsPlayerActivity.this, mMessageUri);
        } catch (MmsException e) {
            Log.e(TAG, "SlideshowModel.getPduBody MmsException:" + e.toString());
        }
        
        if (body == null) {
            return false;
        }

        boolean result = false;
        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (EncapsulatedContentType.isImageType(type) || EncapsulatedContentType.isVideoType(type) ||
                    EncapsulatedContentType.isAudioType(type) || "application/ogg".equalsIgnoreCase(type)) {
                result = true;
                break;
            }

            // add for vcard
            if (FileAttachmentModel.isSupportedFile(part)) {
                result = true;
                break;
            }
        }

        /// M: Justify weather there are attachments in parts but not in slides
        // SlideshowModel mSlideShowModel = mWorkingMessage.getSlideshow();
        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                //OP01
                SlideshowModel mSlideShow = null;
                try{
                     mSlideShow = SlideshowModel.createFromPduBody(this, body);
                }
                catch (MmsException e){
                     MmsLog.e(TAG, "Create from pdubody exception!");
                }

                if (mSlideShow != null) {
                    MmsLog.i(TAG, "mSlideShowModel != null");
                    if (mSlideShow.getAttachFiles() != null) {
                        MmsLog.i(TAG, "mSlideShowModel.getAttachFiles() != null");
                        if (mSlideShow.getAttachFiles().size() != 0) {
                            result = true;
                        }
                    }
                }
            }
        }

        return result;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean succeeded = false;
        if (data != null && data.hasExtra("multi_save_result")) {
            succeeded = data.getBooleanExtra("multi_save_result", false);
            final int resId = succeeded ? R.string.copy_to_sdcard_success : R.string.copy_to_sdcard_fail;
            Toast.makeText(MmsPlayerActivity.this, resId, Toast.LENGTH_SHORT).show();
        }
        return;
    }

    public void onUpdate(Contact updated) {
        MmsLog.d(TAG, "onUpdate,number and name:" + updated.getNumber() + "," + updated.getName());
        if ((mContactList != null && mContactList.size() > 0) &&
                (updated.getNumber().equals(mContactList.get(0).getNumber()))) {
            updateRecipient();
        }
    }

    private void updateRecipient() {
        mContactList = ContactList.getByNumbers(mNumber, true, false);
        //update name
        if (mContactList.size() == 1) {
            mName = mContactList.get(0).getName();
        } else {
            mName = mContactList.getFirstName(null);
        }
        Log.d(TAG, "mName:" + mName);
        if (mMsgBox == Mms.MESSAGE_BOX_INBOX) {
            mName = getString(R.string.via_without_time_for_send) + ": " + mName;
        } else {
            mName = getString(R.string.via_without_time_for_recieve) + ": " + mName;
        }
        final String showString = mName;
        runOnUiThread(new Runnable() {
            public void run() {
                mRecipient.setText(showString);
            }
        });
    }
    
    

    private void initPlugin(Context context) {
        mMmsTextSizeAdjustPlugin = (IMmsTextSizeAdjust)MmsPluginManager.getMmsPluginObject(
                MmsPluginManager.MMS_PLUGIN_TYPE_TEXT_SIZE_ADJUST);
        if (mMmsTextSizeAdjustPlugin != null) {
            mMmsTextSizeAdjustPlugin.init(this, this);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = false;
        if (mMmsTextSizeAdjustPlugin != null) {
            ret = mMmsTextSizeAdjustPlugin.dispatchTouchEvent(ev);
        }
        if (!ret) {
            ret = super.dispatchTouchEvent(ev);
        }
        return ret;
    }


    public void setTextSize(float size) {

        if (mListAdapter != null) {
            mListAdapter.setTextSize(size);
        }
        if (mListView != null) {
            int count = mListView.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = mListView.getChildAt(i);
                if (view != null && view instanceof MmsPlayerActivityItem) {
                    MmsPlayerActivityItem item = (MmsPlayerActivityItem)view;
                    item.setTextSize(size);
                }
            }
        }
    }    

    private void lockMessage(final boolean lock) {

        final ContentValues values = new ContentValues(1);
        values.put("locked", lock ? 1 : 0);

        mLocked = lock;
        new Thread(new Runnable() {
            public void run() {
                getContentResolver().update(mMsgUri, values, null, null);
                runOnUiThread(new Runnable() {

                    public void run() {
                        mLockedInd.setVisibility(lock ? View.VISIBLE : View.GONE);
                    }
                });
            }
        }, "lockMessage").start();
    }

    private void asyncLoadMms() {
        mAsyncDialog.clearPendingProgressDialog();
        mAsyncDialog.runAsync(new Runnable() {

            @Override
            public void run() {
                // backupGroud load...
                // add for cmcc dir ui begin
                if (mDirMode) {
                    mMsgUri = mMessageUri;
                    parseMsgUri();
                }
                try {
                    // body = SlideshowModel.getPduBody(MmsPlayerActivity.this,
                    // msg);
                    mSlideshow = SlideshowModel.createFromMessageUri(MmsPlayerActivity.this,
                            mMessageUri);
                } catch (MmsException e) {
                    return;
                }
                // add for cmcc dir ui end
            }
        }, new Runnable() {

            @Override
            public void run() {
                // after load finish

                initListAdapter(mMessageUri);
                setListTextSize();
                if (mDirMode && mParseMsgUriSuccess) {
                    updateRecipient();
                }
                invalidateOptionsMenu();
            }
        }, R.string.sync_mms_to_db);
    }

    private void setListTextSize() {
        if (MmsConfig.getAdjustFontSizeEnabled()) {
            float textSize = MessageUtils.getPreferenceValueFloat(this, SettingListActivity.TEXT_SIZE, 18);
            setTextSize(textSize);
        }
    }
}
