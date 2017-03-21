package com.sunshinetpu.demochatandroid;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gakwaya on 4/16/2016.
 */
public class ContactModel {

    private static ContactModel sContactModel;
    private List<Contact> mContacts;

    public static ContactModel get(Context context)
    {
        if(sContactModel == null)
        {
            sContactModel = new ContactModel(context);
        }
        return  sContactModel;
    }

    private ContactModel(Context context)
    {
        mContacts = new ArrayList<>();
        populateWithInitialContacts(context);

    }

    private void populateWithInitialContacts(Context context)
    {
        //Create the Foods and add them to the list;


        Contact contact1 = new Contact("minh2@192.168.8.182");
        Contact contact2 = new Contact("sunshinetpu@192.168.8.182");
        Contact contact3 = new Contact("minh3@192.168.8.182");
        Contact contact4 = new Contact("FSI");
        Contact contact5 = new Contact("MediaChat");
        mContacts.add(contact1);
        mContacts.add(contact2);
        mContacts.add(contact3);
        mContacts.add(contact4);
        mContacts.add(contact5);
    }

    public List<Contact> getContacts()
    {
        return mContacts;
    }

}
