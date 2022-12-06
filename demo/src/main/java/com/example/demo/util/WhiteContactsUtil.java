package com.example.demo.util;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.example.demo.util.ContactEntity;

import java.util.ArrayList;
import java.util.Objects;

import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.util.Log;
import android.widget.Toast;

public class WhiteContactsUtil {

    private static final String TAG = WhiteContactsUtil.class.getSimpleName();

    /**
     * 添加联系人到本机
     *
     * @param context
     * @param contact
     * @return
     */
    public static boolean addContact(Context context, ContactEntity contact) {
        try {
            // 向data表插入电话数据
            String current_mobile_number = contact.getPhone();
            String current_name = contact.getName();
            String wl_num = contact.getWl_num();
            //wl-phone：白名单电话号码；如果为空，则表示删除该位置号码
            if (null == current_mobile_number || current_mobile_number.isEmpty()) {
                deleteContact(context, current_name);
                return false;
            }

            //避免重复添加
            ArrayList<ContactEntity> contactEntities = getContacts(context);
            for (ContactEntity entity : contactEntities) {
                if (entity.getPhone().equals(current_mobile_number) && entity.getName().equals(current_name)) {
                    return false;
                }
            }


            ContentValues values = new ContentValues();

            // 下面的操作会根据RawContacts表中已有的rawContactId使用情况自动生成新联系人的rawContactId
            //Uri rawContactUri = context.getContentResolver().insert(RawContacts.CONTENT_URI, values);
            //            long rawContactId = Long.parseLong(wl_num);

            Uri uri = context.getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);
            long rawContactId = ContentUris.parseId(uri);

            //id，这里用传过来的序号当id

            if (null != wl_num && !wl_num.isEmpty()) {
                values.clear();
                values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                values.put(StructuredName.DATA_SET, wl_num);
                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            }

            //电话号码
            if (null != current_mobile_number) {
                values.clear();
                values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
                values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, current_mobile_number);
                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            }

            // 向data表插入姓名数据
            String name = contact.getName();
            if (null != current_name) {
                values.clear();
                values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                values.put(StructuredName.DISPLAY_NAME, name);
                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            }


            // 向data表插入Email数据
            //            String email = contact.getEmail();
            //            if (null != email) {
            //                values.clear();
            //                values.put(Data.RAW_CONTACT_ID, rawContactId);
            //                values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            //                values.put(Email.DATA, email);
            //                values.put(Email.TYPE, Email.TYPE_WORK);
            //                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            //            }

            // 向data表插入备注信息
            //            String describe = contact.getDescribe();
            //            if (null != describe) {
            //                values.clear();
            //                values.put(Data.RAW_CONTACT_ID, rawContactId);
            //                values.put(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE);
            //                values.put(Note.NOTE, describe);
            //                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            //            }

            // 向data表插入头像数据
            //            Bitmap sourceBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            //            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            //            // 将Bitmap压缩成PNG编码，质量为100%存储
            //            sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            //            byte[] avatar = os.toByteArray();
            //            values.put(Data.RAW_CONTACT_ID, rawContactId);
            //            values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            //            values.put(Photo.PHOTO, avatar);
            //            context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 获取所有联系人
     *
     * @param context
     * @return
     */
    public static ArrayList<ContactEntity> getContacts(Context context) {
        Cursor cursor = null;
        ArrayList<ContactEntity> contactEntitiesList = new ArrayList<>();
        try {
            cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
            while (cursor.moveToNext()) {
                int i_name = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String displayName = cursor.getString(i_name);

                int i_number = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(i_number);

                int i_raw_contact_id = cursor.getColumnIndex(Data.RAW_CONTACT_ID);
                String raw_contact_id = cursor.getString(i_raw_contact_id);

                String data1 = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA_SET));

                ContactEntity contactEntity = new ContactEntity();
                contactEntity.setName(displayName);
                contactEntity.setPhone(number);
                //                contactEntity.setWl_num(raw_contact_id);
                contactEntitiesList.add(contactEntity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contactEntitiesList;
    }

    /**
     * 获取sos号码
     */
    public static void getSOSContactsList(Context context) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                null, null, null, null);

        while (cursor.moveToNext()) {
            //            String rawContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts._ID));
            //
            //            Log.i("RAW_CONTACTS _ID", rawContactId);

            //得到rawcontact对应的contactId

            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
            Log.i(TAG, contactId);


            //同步contactId查询contact表，添加一个selector值进行过滤
            //            String[] selection_args = new String[]{"name_search"};
            //            Cursor c = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,
            //                    ContactsContract.Contacts.DISPLAY_NAME + "=?", selection_args, null);

            String dn = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));


            //            String deleted = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.DELETED));


            //            Log.i("RAW_CONTACTS DELETED", deleted);
        }
    }


    /**
     * 删除联系人
     */
    public static void deleteContact(Context context, String name) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        if (!name.equals("")) {
            context.getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI,
                    ContactsContract.Contacts.DISPLAY_NAME + "=?", new String[]{name});
        }
    }

}
