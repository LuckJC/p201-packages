/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.contacts.common.list;

import com.android.contacts.common.list.ContactListAdapter.ContactQuery;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.preference.ContactsPreferences;
import com.mediatek.contacts.ExtensionManager;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.SearchSnippetColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A cursor adapter for the {@link ContactsContract.Contacts#CONTENT_TYPE} content type.
 */
public class DefaultContactListAdapter extends ContactListAdapter {

    public static final char SNIPPET_START_MATCH = '\u0001';
    public static final char SNIPPET_END_MATCH = '\u0001';
    public static final String SNIPPET_ELLIPSIS = "\u2026";
    public static final int SNIPPET_MAX_TOKENS = 5;
    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     
     *   CR ID: ALPS00112614
     *   Descriptions: only show phone contact if it's from sms
     */
    private boolean mOnlyShowPhoneContacts = false;

    public ProfileAndContactsLoader mSDNLoader = null;

    public boolean isOnlyShowPhoneContacts() {
        return mOnlyShowPhoneContacts;
    }

    public void setOnlyShowPhoneContacts(boolean showPhoneContacts) {
        mOnlyShowPhoneContacts = showPhoneContacts;
    }
    /*
     * Bug Fix by Mediatek End.
     */

    public static final String SNIPPET_ARGS = SNIPPET_START_MATCH + "," + SNIPPET_END_MATCH + ","
            + SNIPPET_ELLIPSIS + "," + SNIPPET_MAX_TOKENS;

    public DefaultContactListAdapter(Context context) {
        super(context);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        if (loader instanceof ProfileAndContactsLoader) {
            /** M: New Feature SDN @{ */
            mSDNLoader = (ProfileAndContactsLoader) loader;
            /** @} */
            ((ProfileAndContactsLoader) loader).setLoadProfile(shouldIncludeProfile());
        }

        ContactListFilter filter = getFilter();
        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (TextUtils.isEmpty(query)) {
                // Regardless of the directory, we don't want anything returned,
                // so let's just send a "nothing" query to the local directory.
                loader.setUri(Contacts.CONTENT_URI);
                loader.setProjection(getProjection(false));
                loader.setSelection("0");
            } else {
                Builder builder = Contacts.CONTENT_FILTER_URI.buildUpon();
                builder.appendPath(query);      // Builder will encode the query
                builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(directoryId));
                if (directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE) {
                    builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                            String.valueOf(getDirectoryResultLimit(getDirectoryById(directoryId))));
                }
                builder.appendQueryParameter(SearchSnippetColumns.SNIPPET_ARGS_PARAM_KEY,
                        SNIPPET_ARGS);
                builder.appendQueryParameter(SearchSnippetColumns.DEFERRED_SNIPPETING_KEY,"1");
                loader.setUri(builder.build());
                loader.setProjection(getProjection(true));
            }
        } else {
            configureUri(loader, directoryId, filter);
            loader.setProjection(getProjection(false));
            configureSelection(loader, directoryId, filter);
        }

        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     
         *   CR ID: ALPS00112614
         *   Descriptions: only show phone contact if it's from sms
         */
        if (isOnlyShowPhoneContacts()) {
            configureOnlyShowPhoneContactsSelection(loader, directoryId, filter);
        }
        /*
         * Bug Fix by Mediatek End.
         */
        
        String sortOrder;
        if (getSortOrder() == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
            sortOrder = Contacts.SORT_KEY_PRIMARY;
        } else {
            sortOrder = Contacts.SORT_KEY_ALTERNATIVE;
        }

        loader.setSortOrder(sortOrder);
    }

    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     
     *   CR ID: ALPS00112614
     *   Descriptions: only show phone contact if it's from sms
     */
    private void configureOnlyShowPhoneContactsSelection(CursorLoader loader, long directoryId,
            ContactListFilter filter) {
        if (filter == null) {
            return;
        }

        if (directoryId != Directory.DEFAULT) {
            return;
        }

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<String>();
        
        selection.append(Contacts.INDICATE_PHONE_SIM + "= ?");
        selectionArgs.add("-1");
        
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }
    /*
     * Bug Fix by Mediatek End.
     */

    protected void configureUri(CursorLoader loader, long directoryId, ContactListFilter filter) {
        Uri uri = Contacts.CONTENT_URI;
        if (filter != null && filter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
            String lookupKey = getSelectedContactLookupKey();
            if (lookupKey != null) {
                uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey);
            } else {
                uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, getSelectedContactId());
            }
        }

        if (directoryId == Directory.DEFAULT && isSectionHeaderDisplayEnabled()) {
            uri = ContactListAdapter.buildSectionIndexerUri(uri);
        }

        // The "All accounts" filter is the same as the entire contents of Directory.DEFAULT
        if (filter != null
                && filter.filterType != ContactListFilter.FILTER_TYPE_CUSTOM
                && filter.filterType != ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
            final Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT));
            /**
             * M: Change Feature: <br>
             * As Local Phone account contains null account and Phone Account,
             * the Account Query Parameter could not meet this requirement. So,
             * We should keep to query contacts with selection. @{
             */
            /*
             * if (filter.filterType == ContactListFilter.FILTER_TYPE_ACCOUNT) {
             * filter.addAccountQueryParameterToUrl(builder); }
             */
            /** @} */
            uri = builder.build();
        }

        loader.setUri(uri);
    }

    /**
     * M: New Feature SDN <br>
     * Origin code: <br>
     * private void configureSelection(<br>
     * @{
     */
    protected void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter) {
        if (filter == null) {
            return;
        }

        if (directoryId != Directory.DEFAULT) {
            return;
        }

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<String>();

        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS: {
                // We have already added directory=0 to the URI, which takes care of this
                // filter
                /** M: New Feature SDN @{ */
                selection.append(RawContacts.IS_SDN_CONTACT + " < 1");
                /** @} */
                break;
            }
            case ContactListFilter.FILTER_TYPE_SINGLE_CONTACT: {
                // We have already added the lookup key to the URI, which takes care of this
                // filter
                break;
            }
            case ContactListFilter.FILTER_TYPE_STARRED: {
                selection.append(Contacts.STARRED + "!=0");
                break;
            }
            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY: {
                selection.append(Contacts.HAS_PHONE_NUMBER + "=1");
                /** M: New Feature SDN @{ */
                selection.append(" AND " + RawContacts.IS_SDN_CONTACT + " < 1");
                /** @} */
                break;
            }
            case ContactListFilter.FILTER_TYPE_CUSTOM: {
                selection.append(Contacts.IN_VISIBLE_GROUP + "=1");
                if (isCustomFilterForPhoneNumbersOnly()) {
                    selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                }
                /** M: New Feature SDN @{ */
                selection.append(" AND " + RawContacts.IS_SDN_CONTACT + " < 1");
                /** @} */
                break;
            }
            case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                // We use query parameters for account filter, so no selection to add here.
                /** M: Change Feature: As Local Phone account contains null account and Phone
                 * Account, the Account Query Parameter could not meet this requirement. So,
                 * We should keep to query contacts with selection. @{ */
                if (AccountType.ACCOUNT_TYPE_LOCAL_PHONE.equals(filter.accountType)) {
                    selection.append("EXISTS ("
                                    + "SELECT DISTINCT " + RawContacts.CONTACT_ID
                                    + " FROM view_raw_contacts"
                                    + " WHERE ( ");
                    selection.append(RawContacts.IS_SDN_CONTACT + " < 1 AND ");
                    selection.append(RawContacts.CONTACT_ID + " = " + "view_contacts."
                            + Contacts._ID
                                    + " AND (" + RawContacts.ACCOUNT_TYPE + " IS NULL "
                                    + " AND " + RawContacts.ACCOUNT_NAME + " IS NULL "
                                    + " AND " +  RawContacts.DATA_SET + " IS NULL "
                                    + " OR " + RawContacts.ACCOUNT_TYPE + "=? "
                                    + " AND " + RawContacts.ACCOUNT_NAME + "=? ");
                } else {
                    selection.append("EXISTS ("
                                    + "SELECT DISTINCT " + RawContacts.CONTACT_ID
                                    + " FROM view_raw_contacts"
                                    + " WHERE ( ");
                    selection.append(RawContacts.IS_SDN_CONTACT + " < 1 AND ");
                    selection.append(RawContacts.CONTACT_ID + " = " + "view_contacts."
                            + Contacts._ID
                                    + " AND (" + RawContacts.ACCOUNT_TYPE + "=?"
                                    + " AND " + RawContacts.ACCOUNT_NAME + "=?");
                }
                selectionArgs.add(filter.accountType);
                selectionArgs.add(filter.accountName);
                if (filter.dataSet != null) {
                    selection.append(" AND " + RawContacts.DATA_SET + "=? )");
                    selectionArgs.add(filter.dataSet);
                } else {
                    selection.append(" AND " +  RawContacts.DATA_SET + " IS NULL )");
                }
                selection.append("))");
                /** @} */
                break;
            }
        }
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        final ContactListItemView view = (ContactListItemView)itemView;

        view.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);

        if (isSelectionVisible()) {
            view.setActivated(isSelectedContact(partition, cursor));
        }

        bindSectionHeaderAndDivider(view, position, cursor);
        
        /*
         * New Feature by Mediatek Begin.
         *   Original Android's code:
         *     
         *   CR ID: ALPS00308657
         *   Descriptions: RCS
         */
        int i = cursor.getColumnIndex(Contacts._ID);
        long contactId = cursor.getLong(i);

        boolean pulginStatus = ExtensionManager.getInstance().getContactDetailExtension()
                .canSetExtensionIcon(contactId, ExtensionManager.COMMD_FOR_RCS);
        view.removeExtentionTextView();
        view.setExtentionIcon(pulginStatus, contactId);
        /*
         * New Feature by Mediatek End.
         */
       
        if (isQuickContactEnabled()) {
            bindQuickContact(view, partition, cursor, ContactQuery.CONTACT_PHOTO_ID,
                    ContactQuery.CONTACT_PHOTO_URI, ContactQuery.CONTACT_ID,
                    ContactQuery.CONTACT_LOOKUP_KEY);
        } else {
            if (getDisplayPhotos()) {
                bindPhoto(view, partition, cursor);
            }
        }

        bindName(view, cursor);
        bindPresenceAndStatusMessage(view, cursor);

        ///M: for SNS plugin @{
        if (ExtensionManager.getInstance().getContactDetailExtension()
                .checkPluginSupport(ExtensionManager.COMMD_FOR_SNS)) {
            Drawable icon = ExtensionManager
                    .getInstance()
                    .getContactListExtension()
                    .getPresenceIcon(cursor, ContactQuery.STATUS_RES_PACKAGE,
                            ContactQuery.STATUS_ICON,
                            ExtensionManager.COMMD_FOR_SNS);
            String status = ExtensionManager
                    .getInstance()
                    .getContactListExtension()
                    .getStatusString(cursor, ContactQuery.STATUS_RES_PACKAGE,
                            ContactQuery.CONTACT_CONTACT_STATUS,
                            ExtensionManager.COMMD_FOR_SNS);
            if (icon != null) {
                view.setPresence(icon);
            }
            if (status != null) {
                view.setStatus(status);
            }
        }
        ///@}

        if (isSearchMode()) {
            bindSearchSnippet(view, cursor);
        } else {
            view.setSnippet(null);
        }
    }

    private boolean isCustomFilterForPhoneNumbersOnly() {
        // TODO: this flag should not be stored in shared prefs.  It needs to be in the db.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
                ContactsPreferences.PREF_DISPLAY_ONLY_PHONES_DEFAULT);
    }

    /** M: The following lines are provided and maintained by Mediatek Inc. @{ */

    // New Feature for SDN
    @Override
    public void updateIndexer(Cursor cursor) {
        super.updateIndexer(cursor);
        ContactsSectionIndexer sectionIndexer = (ContactsSectionIndexer) this.getIndexer();
        if (mSDNLoader != null) {
            if (mSDNLoader.hasSdnContact()) {
                sectionIndexer.setSdnHeader("SDN", mSDNLoader.getSdnContactCount());
            }
        }
    }

    /** M: The previous lines are provided and maintained by Mediatek Inc. @} */
}
