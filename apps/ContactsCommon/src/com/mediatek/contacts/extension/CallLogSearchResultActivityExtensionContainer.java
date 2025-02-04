/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
package com.mediatek.contacts.extension;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.mediatek.contacts.ext.CallLogSearchResultActivityExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class CallLogSearchResultActivityExtensionContainer extends CallLogSearchResultActivityExtension {

    private static final String TAG = "CallLogSearchResultActivityExtensionContainer";

    private LinkedList<CallLogSearchResultActivityExtension> mSubExtensionList;

    public void add(CallLogSearchResultActivityExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<CallLogSearchResultActivityExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(CallLogSearchResultActivityExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void onCreate(Activity activity) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onCreate(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onCreate(), activity = " + activity);
        Iterator<CallLogSearchResultActivityExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onCreate(activity);
        }
    }

    public void onDestroy() {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onDestroy(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onDestroy()");
        Iterator<CallLogSearchResultActivityExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onDestroy();
        }
    }

    public boolean onListItemClick(ListView l, View v, int position, long id) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onListItemClick(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "onListItemClick(), listView = " + l
                + ", view = " + v + ", position = " + position + ", id = " + id);
        Iterator<CallLogSearchResultActivityExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallLogSearchResultActivityExtension extension = iterator.next();
            if (extension.onListItemClick(l, v, position, id)) {
                return true;
            }
        }
        return false;
    }
}
