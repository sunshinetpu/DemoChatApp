# DemoChatApp
Demo Android chat app using XMPP server OpenFire.
Features:
- Chat between 2 client (send texts, share images and transfer video).
- Group chat (only send texts). Messages from friends currently are display as from one person. 
- Voice/video call between 2 clients using webRTC.

To Run app you have to install openFire server 
https://www.igniterealtime.org/projects/openfire/

Then open file RoosterConnection and change the server host, server name to yours.


Currently use hard code for jabberd Id in the contact list. Replace them with your new jabberId 

Server page:

- user: admin, password: admin
- user: fsi1, password: fsi1
- user: fsi2, password: fsi2

You can download demo apk file (no proguard) here:
https://drive.google.com/file/d/0B2prAz-G4KUeVU9vQWlEaGtLbmM/view?usp=sharing

For more options in creating MediaConstraints you should check out this file:
https://chromium.googlesource.com/external/webrtc/stable/talk/+/master/app/webrtc/mediaconstraintsinterface.h
