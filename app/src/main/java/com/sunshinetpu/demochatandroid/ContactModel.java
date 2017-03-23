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


        Contact contact1 = new Contact("fsi1@192.168.6.240");
        Contact contact2 = new Contact("fsi2@192.168.6.240");

        //FSI is for group chat
        Contact contact3 = new Contact("FSI");

        //MediaChat is for testing video call between 2 clients.
        Contact contact4 = new Contact("MediaChat");

        mContacts.add(contact1);
        mContacts.add(contact2);
        mContacts.add(contact3);
        mContacts.add(contact4);
    }

    public List<Contact> getContacts()
    {
        return mContacts;
    }

}
