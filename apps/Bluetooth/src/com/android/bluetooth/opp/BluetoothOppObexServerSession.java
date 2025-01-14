/*
 * Copyright (c) 2008-2009, Motorola, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Motorola, Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.bluetooth.opp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.android.bluetooth.R;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.webkit.MimeTypeMap;

import javax.obex.HeaderSet;
import javax.obex.ObexTransport;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.ServerSession;
import java.lang.Integer;
import java.util.ArrayList;


/**
 * This class runs as an OBEX server
 */
public class BluetoothOppObexServerSession extends ServerRequestHandler implements
        BluetoothOppObexSession {

    private static final String TAG = "[Bluetooth.OPP]BtOppObexServer";
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;

    private ObexTransport mTransport;

    private Context mContext;

    private Handler mCallback = null;

    /* status when server is blocking for user/auto confirmation */
    private boolean mServerBlocking = true;

    /* the current transfer info */
    private BluetoothOppShareInfo mInfo;

    /* info id when we insert the record */
    private int mLocalShareInfoId;

    private int mAccepted = BluetoothShare.USER_CONFIRMATION_PENDING;

    private boolean mInterrupted = false;

    private ServerSession mSession;

    private long mTimestamp;

    private BluetoothOppReceiveFileInfo mFileInfo;

    private WakeLock mWakeLock;

    private WakeLock mPartialWakeLock;

    boolean mTimeoutMsgSent = false;

    private BluetoothOppTransferActivity.UpdataProgress mUp;
    private ArrayList<Integer> mStopTaskId;

    public BluetoothOppObexServerSession(Context context, ObexTransport transport) {
        mContext = context;
        mTransport = transport;
        PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, TAG);
        mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mStopTaskId = new ArrayList<Integer>();
    }

    public void unblock() {
        mServerBlocking = false;
    }

    /**
     * Called when connection is accepted from remote, to retrieve the first
     * Header then wait for user confirmation
     */
    public void preStart() {
        if (D) Log.d(TAG, "acquire full WakeLock");
        mWakeLock.acquire();
        try {
            if (D) Log.d(TAG, "Create ServerSession with transport " + mTransport.toString());
            mSession = new ServerSession(mTransport, this, null);
        } catch (IOException e) {
            Log.e(TAG, "Create server session error" + e);
        }
    }

    /**
     * Called from BluetoothOppTransfer to start the "Transfer"
     */
    public void start(Handler handler, int numShares) {
        if (D) Log.d(TAG, "Start!");
        mCallback = handler;

    }

    /**
     * Called from BluetoothOppTransfer to cancel the "Transfer" Otherwise,
     * server should end by itself.
     */
    public void stop() {
        /*
         * TODO now we implement in a tough way, just close the socket.
         * maybe need nice way
         */
        if (D) Log.d(TAG, "Stop!");
        mInterrupted = true;
        if (mSession != null) {
            try {
                mSession.close();
                if(mTransport != null) {
                    mTransport.close();
                }
                try {
                    if (mFileInfo != null && mFileInfo.mOutputStream != null) {
                        mFileInfo.mOutputStream.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "exception while closing ouputstream");
                }
            } catch (IOException e) {
                Log.e(TAG, "close mTransport error" + e);
            }
        }
        mCallback = null;
        mSession = null;
    }

    public void addShare(BluetoothOppShareInfo info) {
        if (D) Log.d(TAG, "addShare for id " + info.mId);
        mInfo = info;
        //mFileInfo = processShareInfo();
    }

    @Override
    public int onPut(Operation op) {
        if (D) Log.d(TAG, "onPut " + op.toString());
        HeaderSet request;
        String name, mimeType;
        Long length;

        int obexResponse = ResponseCodes.OBEX_HTTP_OK;

        /**
         * For multiple objects, reject further objects after user deny the
         * first one
         */
        if (mAccepted == BluetoothShare.USER_CONFIRMATION_DENIED) {
            Log.i(TAG, "onput--, USER_CONFIRMATION_DENIED");
            return ResponseCodes.OBEX_HTTP_FORBIDDEN;
        }

        String destination;
        if (mTransport instanceof BluetoothOppRfcommTransport) {
            destination = ((BluetoothOppRfcommTransport)mTransport).getRemoteAddress();
        } else {
            destination = "FF:FF:FF:00:00:00";
        }
        boolean isWhitelisted = BluetoothOppManager.getInstance(mContext).
                isWhitelisted(destination);

        try {
            boolean pre_reject = false;

            request = op.getReceivedHeader();
            if (V) Constants.logHeader(request);
            name = (String)request.getHeader(HeaderSet.NAME);
            length = (Long)request.getHeader(HeaderSet.LENGTH);
            mimeType = (String)request.getHeader(HeaderSet.TYPE);

            if (length == 0) {
                if (D) Log.w(TAG, "length is 0, reject the transfer");
                pre_reject = true;
                obexResponse = ResponseCodes.OBEX_HTTP_LENGTH_REQUIRED;
            }

            if (name == null || name.equals("")) {
                if (D) Log.w(TAG, "name is null or empty, reject the transfer");
                pre_reject = true;
                obexResponse = ResponseCodes.OBEX_HTTP_BAD_REQUEST;
            }

            if (!pre_reject) {
                /* first we look for Mimetype in Android map */
                String extension, type;
                int dotIndex = name.lastIndexOf(".");
                if (dotIndex < 0 && mimeType == null) {
                    if (D) Log.w(TAG, "There is no file extension or mime type," +
                            "reject the transfer");
                    pre_reject = true;
                    obexResponse = ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                } else {
                    extension = name.substring(dotIndex + 1).toLowerCase();
                    MimeTypeMap map = MimeTypeMap.getSingleton();
                    type = map.getMimeTypeFromExtension(extension);
                    if (V) Log.v(TAG, "Mimetype guessed from extension " + extension + " is " + type);
                    if (type != null) {
                        mimeType = type;

                    } else {
                        if (mimeType == null) {
                            if (D) Log.w(TAG, "Can't get mimetype, reject the transfer");
                            pre_reject = true;
                            obexResponse = ResponseCodes.OBEX_HTTP_UNSUPPORTED_TYPE;
                        }
                    }
                    if (mimeType != null) {
                        mimeType = mimeType.toLowerCase();
                    }
                }
            }

            // Reject policy: anything outside the "white list" plus unspecified
            // MIME Types. Also reject everything in the "black list".
            if (!pre_reject
                    && (mimeType == null
                            || (!isWhitelisted && !Constants.mimeTypeMatches(mimeType,
                                    Constants.ACCEPTABLE_SHARE_INBOUND_TYPES))
                            || Constants.mimeTypeMatches(mimeType,
                                    Constants.UNACCEPTABLE_SHARE_INBOUND_TYPES))) {
                if (D) Log.w(TAG, "mimeType is null or in unacceptable list, reject the transfer");
                pre_reject = true;
                obexResponse = ResponseCodes.OBEX_HTTP_UNSUPPORTED_TYPE;
            }

            if (pre_reject && obexResponse != ResponseCodes.OBEX_HTTP_OK) {
                // some bad implemented client won't send disconnect
                Log.i(TAG, "onput --, obexResponse != ResponseCodes.OBEX_HTTP_OK");
                return obexResponse;
            }

        } catch (IOException e) {
            Log.e(TAG, "onput--, get getReceivedHeaders error " + e);
            return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
        }

        ContentValues values = new ContentValues();

        values.put(BluetoothShare.FILENAME_HINT, name);
        values.put(BluetoothShare.TOTAL_BYTES, length.intValue());
        values.put(BluetoothShare.MIMETYPE, mimeType);

        values.put(BluetoothShare.DESTINATION, destination);

        values.put(BluetoothShare.DIRECTION, BluetoothShare.DIRECTION_INBOUND);
        values.put(BluetoothShare.TIMESTAMP, mTimestamp);

        boolean needConfirm = true;
        /** It's not first put if !serverBlocking, so we auto accept it */
        if (!mServerBlocking) {
            values.put(BluetoothShare.USER_CONFIRMATION,
                    BluetoothShare.USER_CONFIRMATION_AUTO_CONFIRMED);
            needConfirm = false;
        }

        if (isWhitelisted) {
            values.put(BluetoothShare.USER_CONFIRMATION,
                    BluetoothShare.USER_CONFIRMATION_HANDOVER_CONFIRMED);
            needConfirm = false;

        }

        Uri contentUri = null;
        synchronized (this) {
            if (mWakeLock.isHeld()) {
                mPartialWakeLock.acquire();
                mWakeLock.release();
            }
            mServerBlocking = true;

            contentUri = mContext.getContentResolver().insert(BluetoothShare.CONTENT_URI, values);
            mLocalShareInfoId = Integer.parseInt(contentUri.getPathSegments().get(1));

            if (needConfirm) {
                Intent in = new Intent(BluetoothShare.INCOMING_FILE_CONFIRMATION_REQUEST_ACTION);
                in.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
                mContext.sendBroadcast(in);
            }

            if (V)Log.v(TAG, "insert contentUri: " + contentUri);
            if (V)Log.v(TAG, "mLocalShareInfoId = " + mLocalShareInfoId);

            if (V)Log.v(TAG, "acquire partial WakeLock");

            try {

                while (mServerBlocking) {
                    wait(1000);
                    Log.d(TAG, "while loop when serverblocking");
                    if (mCallback != null && !mTimeoutMsgSent) {
                        mCallback.sendMessageDelayed(mCallback
                                .obtainMessage(BluetoothOppObexSession.MSG_CONNECT_TIMEOUT),
                                BluetoothOppObexSession.SESSION_TIMEOUT);
                        mTimeoutMsgSent = true;
                        if (V) Log.v(TAG, "MSG_CONNECT_TIMEOUT sent");
                    }
                }
            } catch (InterruptedException e) {
                if (V) Log.v(TAG, "Interrupted in onPut blocking");
            }
        }
        if (D) Log.d(TAG, "Server unblocked ");
        synchronized (this) {
            if (mCallback != null && mTimeoutMsgSent) {
                mCallback.removeMessages(BluetoothOppObexSession.MSG_CONNECT_TIMEOUT);
            }
        }

        /* we should have mInfo now */

        /*
         * TODO check if this mInfo match the one that we insert before server
         * blocking? just to make sure no error happens
         */
        if (mInfo.mId != mLocalShareInfoId) {
            Log.e(TAG, "Unexpected error!");
        }
        mAccepted = mInfo.mConfirm;

        if (V) Log.v(TAG, "after confirm: userAccepted=" + mAccepted);
        int status = BluetoothShare.STATUS_SUCCESS;

        if (mAccepted == BluetoothShare.USER_CONFIRMATION_CONFIRMED
                || mAccepted == BluetoothShare.USER_CONFIRMATION_AUTO_CONFIRMED
                || mAccepted == BluetoothShare.USER_CONFIRMATION_HANDOVER_CONFIRMED) {
            /* Confirm or auto-confirm */
            mFileInfo = processShareInfo();

            if (mFileInfo.mFileName == null) {
                status = mFileInfo.mStatus;
                /* TODO need to check if this line is correct */
                mInfo.mStatus = mFileInfo.mStatus;
                Constants.updateShareStatus(mContext, mInfo.mId, status);
                obexResponse = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;

            }

            if (mFileInfo.mFileName != null) {

                ContentValues updateValues = new ContentValues();
                contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + mInfo.mId);
                updateValues.put(BluetoothShare._DATA, mFileInfo.mFileName);
                updateValues.put(BluetoothShare.STATUS, BluetoothShare.STATUS_RUNNING);
                mContext.getContentResolver().update(contentUri, updateValues, null, null);

                status = receiveFile(mFileInfo, op);
                /*
                 * TODO map status to obex response code
                 */
                if (status != BluetoothShare.STATUS_SUCCESS) {
                    obexResponse = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
                }
                Constants.updateShareStatus(mContext, mInfo.mId, status);
            }

            if (status == BluetoothShare.STATUS_SUCCESS) {
                Message msg = Message.obtain(mCallback, BluetoothOppObexSession.MSG_SHARE_COMPLETE);
                msg.obj = mInfo;
                msg.sendToTarget();
            } else {
                if (mCallback != null) {
                    Message msg = Message.obtain(mCallback,
                            BluetoothOppObexSession.MSG_SESSION_ERROR);
                    mInfo.mStatus = status;
                    msg.obj = mInfo;
                    msg.sendToTarget();
                }
            }
        } else if (mAccepted == BluetoothShare.USER_CONFIRMATION_DENIED
                || mAccepted == BluetoothShare.USER_CONFIRMATION_TIMEOUT) {
            /* user actively deny the inbound transfer */
            /*
             * Note There is a question: what's next if user deny the first obj?
             * Option 1 :continue prompt for next objects
             * Option 2 :reject next objects and finish the session
             * Now we take option 2:
             */

            Log.i(TAG, "Rejected incoming request");
            if (mFileInfo.mFileName != null) {
                try {
                    mFileInfo.mOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "error close file stream");
                }
                new File(mFileInfo.mFileName).delete();
            }
            // set status as local cancel
            status = BluetoothShare.STATUS_CANCELED;
            Constants.updateShareStatus(mContext, mInfo.mId, status);
            obexResponse = ResponseCodes.OBEX_HTTP_FORBIDDEN;

            Message msg = Message.obtain(mCallback);
            msg.what = BluetoothOppObexSession.MSG_SHARE_INTERRUPTED;
            mInfo.mStatus = status;
            msg.obj = mInfo;
            msg.sendToTarget();
        }
        Log.i(TAG, "onput --");
        return obexResponse;
    }

    private int receiveFile(BluetoothOppReceiveFileInfo fileInfo, Operation op) {
        Log.i(TAG, "receiverFile ++, fileName = " + fileInfo.mFileName);
        /*
         * implement receive file
         */
        int status = -1;
        BufferedOutputStream bos = null;

        InputStream is = null;
        boolean error = false;
        try {
            is = op.openInputStream();
        } catch (IOException e1) {
            Log.e(TAG, "Error when openInputStream");
            status = BluetoothShare.STATUS_OBEX_DATA_ERROR;
            error = true;
        }

        Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + mInfo.mId);

        ContentValues updateValues;
        if (!error) {
            updateValues = new ContentValues();
            updateValues.put(BluetoothShare._DATA, fileInfo.mFileName);
            mContext.getContentResolver().update(contentUri, updateValues, null, null);
        }

        int position = 0;
        if (!error) {
            bos = new BufferedOutputStream(fileInfo.mOutputStream, 0x10000);
        }

        if (!error) {
            int outputBufferSize = op.getMaxPacketSize();
            byte[] b = new byte[outputBufferSize];
            int readLength = 0;
            long timestamp = 0;
            long preTime = System.currentTimeMillis();
            try {
                Log.i(TAG, "mInfo.mDestination = " + mInfo.mDestination);
                while ((!mInterrupted) && (position != fileInfo.mLength)) {

                    if (V) timestamp = System.currentTimeMillis();

                    readLength = is.read(b);

                    if (readLength == -1) {
                        if (D) Log.d(TAG, "Receive file reached stream end at position" + position);
                        break;
                    }

                    bos.write(b, 0, readLength);
                    position += readLength;

                    if (V) {
                        Log.v(TAG, "Receive file position = " + position + " readLength "
                                + readLength + " bytes took "
                                + (System.currentTimeMillis() - timestamp) + " ms");
                    }

                    long currentTime = System.currentTimeMillis();
                    if (mCallback != null && !mStopTaskId.contains(mInfo.mId) && currentTime-preTime > 500) {
                        preTime = currentTime;

                        BluetoothOppNotification.NotificationItem item = new BluetoothOppNotification.NotificationItem();
                        item.id = mInfo.mId;
                        item.direction = BluetoothShare.DIRECTION_INBOUND;
                        item.totalCurrent = position;
                        item.totalTotal = ((Long)fileInfo.mLength).intValue();
                        item.timeStamp = mInfo.mTimestamp;
                        item.description = mContext.getString(R.string.notification_receiving, fileInfo.mFileName);;
                        item.handoverInitiated = mInfo.mConfirm == BluetoothShare.USER_CONFIRMATION_HANDOVER_CONFIRMED;
                        item.destination = mInfo.mDestination;
                        //item.obexSession = mObexSession;

                        Object[] params = new Object[] {item};
                        Message msg = Message.obtain(mCallback);
                        msg.what = BluetoothOppObexSession.MSG_UPDATE_PROGRESS;
                        msg.obj = params;
                        msg.sendToTarget();

                        if (mUp != null) {
                            mUp.updateProgress(((Long)fileInfo.mLength).intValue(), position);
                        }

                    }

                }

                updateValues = new ContentValues();
                updateValues.put(BluetoothShare.CURRENT_BYTES, position);
                mContext.getContentResolver().update(contentUri, updateValues,
                        null, null);
            } catch (IOException e1) {
                Log.e(TAG, "Error when receiving file");
                /* OBEX Abort packet received from remote device */
                if ("Abort Received".equals(e1.getMessage())) {
                    status = BluetoothShare.STATUS_CANCELED;
                } else {
                    status = BluetoothShare.STATUS_OBEX_DATA_ERROR;
                }
                error = true;
            }
        }

        if (mInterrupted) {
            if (D) Log.d(TAG, "receiving file interrupted by user.");
            status = BluetoothShare.STATUS_CANCELED;
        } else {
            if (position == fileInfo.mLength) {
                if (D) Log.d(TAG, "Receiving file completed for " + fileInfo.mFileName);
                status = BluetoothShare.STATUS_SUCCESS;
            } else {
                if (D) Log.d(TAG, "Reading file failed at " + position + " of " + fileInfo.mLength);
                if (status == -1) {
                    status = BluetoothShare.STATUS_UNKNOWN_ERROR;
                }
            }
        }

        if (bos != null) {
            try {
                bos.close();
            } catch (IOException e) {
                Log.e(TAG, "Error when closing stream after send");
            }
        }
        return status;
    }

    private BluetoothOppReceiveFileInfo processShareInfo() {
        if (D) Log.d(TAG, "processShareInfo() " + mInfo.mId);
        BluetoothOppReceiveFileInfo fileInfo = BluetoothOppReceiveFileInfo.generateFileInfo(
                mContext, mInfo.mId);
        if (V) {
            Log.v(TAG, "Generate BluetoothOppReceiveFileInfo:");
            Log.v(TAG, "filename  :" + fileInfo.mFileName);
            Log.v(TAG, "length    :" + fileInfo.mLength);
            Log.v(TAG, "status    :" + fileInfo.mStatus);
        }
        return fileInfo;
    }

    @Override
    public int onConnect(HeaderSet request, HeaderSet reply) {

        if (D) Log.d(TAG, "onConnect");
        if (V) Constants.logHeader(request);
        Long objectCount = null;
        try {
            byte[] uuid = (byte[])request.getHeader(HeaderSet.TARGET);
            if (V) Log.v(TAG, "onConnect(): uuid =" + Arrays.toString(uuid));
            if(uuid != null) {
                 return ResponseCodes.OBEX_HTTP_NOT_ACCEPTABLE;
            }

            objectCount = (Long) request.getHeader(HeaderSet.COUNT);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }
        String destination;
        if (mTransport instanceof BluetoothOppRfcommTransport) {
            destination = ((BluetoothOppRfcommTransport)mTransport).getRemoteAddress();
        } else {
            destination = "FF:FF:FF:00:00:00";
        }
        boolean isHandover = BluetoothOppManager.getInstance(mContext).
                isWhitelisted(destination);
        if (isHandover) {
            // Notify the handover requester file transfer has started
            Intent intent = new Intent(Constants.ACTION_HANDOVER_STARTED);
            if (objectCount != null) {
                intent.putExtra(Constants.EXTRA_BT_OPP_OBJECT_COUNT, objectCount.intValue());
            } else {
                intent.putExtra(Constants.EXTRA_BT_OPP_OBJECT_COUNT,
                        Constants.COUNT_HEADER_UNAVAILABLE);
            }
            intent.putExtra(Constants.EXTRA_BT_OPP_ADDRESS, destination);
            mContext.sendBroadcast(intent, Constants.HANDOVER_STATUS_PERMISSION);
        }
        mTimestamp = System.currentTimeMillis();
        return ResponseCodes.OBEX_HTTP_OK;
    }

    @Override
    public void onDisconnect(HeaderSet req, HeaderSet resp) {
        if (D) Log.d(TAG, "onDisconnect");
        resp.responseCode = ResponseCodes.OBEX_HTTP_OK;
    }

    private synchronized void releaseWakeLocks() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mPartialWakeLock.isHeld()) {
            mPartialWakeLock.release();
        }
    }

    @Override
    public void onClose() {
        if (V) Log.v(TAG, "release WakeLock");
        releaseWakeLocks();

        /* onClose could happen even before start() where mCallback is set */
        if (mCallback != null) {
            Message msg = Message.obtain(mCallback);
            msg.what = BluetoothOppObexSession.MSG_SESSION_COMPLETE;
            msg.obj = mInfo;
            msg.sendToTarget();
        }
    }

    public void registerCb(BluetoothOppTransferActivity.UpdataProgress up) {
        Log.i(TAG, "registerCb++ ");
        mUp = up;
    }

    public void unRegisterCb(BluetoothOppTransferActivity.UpdataProgress up) {
        Log.i(TAG, "unRegisterCb");
        if(mUp != null && mUp == up) {
            mUp = null;
        }
    }

    public void notifyStopTaskId(int id) {
        Log.i(TAG, "notifyStopTaskId::id = " + id);
        if(mStopTaskId != null) {
            mStopTaskId.add(id);
        }
    }
}
