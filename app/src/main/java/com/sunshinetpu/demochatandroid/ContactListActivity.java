package com.sunshinetpu.demochatandroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ContactListActivity extends AppCompatActivity {

    private static final String TAG = "ContactListActivity";

    private RecyclerView mContactsRecyclerView;
    private ContactAdapter mAdapter;
    private String mCurrentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        mContactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_recycler_view);
        mContactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        ContactModel model = ContactModel.get(getBaseContext());
        List<Contact> contacts = model.getContacts();

        mAdapter = new ContactAdapter(contacts);
        mContactsRecyclerView.setAdapter(mAdapter);
        mCurrentUserId = getIntent().getStringExtra("id");
    }



    private class ContactHolder extends RecyclerView.ViewHolder
    {
        private TextView contactTextView;
        private Contact mContact;
        public ContactHolder ( View itemView)
        {
            super(itemView);

            contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Inside here we start the chat activity
                    if(mContact.getJid().equals("MediaChat")){
                        Intent intent = new Intent(ContactListActivity.this,WebRTCChatActivity.class);
                        intent.putExtra("id",mCurrentUserId);
                        startActivity(intent);
                    }else {
                        Intent intent = new Intent(ContactListActivity.this
                                , Chat2Activity.class);
                        intent.putExtra("EXTRA_CONTACT_JID", mContact.getJid());
                        startActivity(intent);
                    }

                }
            });
        }


        public void bindContact( Contact contact)
        {
            mContact = contact;
            if (mContact == null)
            {
                Log.d(TAG,"Trying to work on a null Contact object ,returning.");
                return;
            }
            contactTextView.setText(mContact.getJid());

        }
    }


    private class ContactAdapter extends RecyclerView.Adapter<ContactHolder>
    {
        private List<Contact> mContacts;

        public ContactAdapter( List<Contact> contactList)
        {
            mContacts = contactList;
        }

        @Override
        public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater
                    .inflate(R.layout.list_item_contact, parent,
                            false);
            return new ContactHolder(view);
        }

        @Override
        public void onBindViewHolder(ContactHolder holder, int position) {
            Contact contact = mContacts.get(position);
            holder.bindContact(contact);

        }

        @Override
        public int getItemCount() {
            return mContacts.size();
        }
    }
}
