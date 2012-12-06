Embedded-Android-Home-Automation
===============================


Android Tablet and ATmega32 with Ethernet client-server system to home automation with support of Infrared control and LIRC configuration files.

The need on this projec erased due to situation when no satisfactory support of Android for IR control.


There is projects based on idea:
http://www.freepatentsonline.com/6931231.html
to play wav files with over phone connector but my Android MID 7" Tablet has limited bandwidth while required up to 20 KHz to make 36 KHz or 38 KHz carrier. 

Short Guide.

There are two parts.

1) Android based client;

2) Embedded device based server connected to LAN.
Based on:
a) my previous project Android Home Automation placed on GIT
https://github.com/yamukha/Android-Home-Automation
b) Ethernet ATmega32/644 Experimentierboard from disign by Ulrich Radig
http://www.ulrichradig.de/home/index.php/avr/eth_m32_ex

Features:
Embedded part used instead of PC of  Android Home Automation project.
https://github.com/yamukha/Android-Home-Automation/tree/master/UDP_HA_Server 
It provides new feature of infra red remote control.

Android part. 
Android Home Automation code extended to read LIRC raw codes for IR control from USB flash.
Default path:
/udisk/lircd.conf 
It can be changed to other on 
/sdacard
or
/LocalDisk/dowload
by recompiling java source.

For more understanding of raw format read:
http://winlirc.sourceforge.net/technicaldetails.html
You can find some lircd.conf files for of different remotes in:
http://lircconfig.commandir.com/lircd.conf/
As well as you can clone IR device by examining it using LIRC or WinLIRC:

http://www.lirc.org/
http://winlirc.sourceforge.net/

Embedded based board and Android Tablet communicate over Ethernet by UPD.

At start user should "ping" server by IP and get interface (to get list of commands)

If there are no response in defined format Android shows "Timeout!".

If requested server has implementation for remote controlling it sends in response list of commands.

After this user can invoke commands remotely from Android application.

If server (embedded device) has IR support and Android read successfully lircd.conf file user can sent IR command with 
IR remote sub-menu options (like mute, power on, volume up, channel down etc).

Virtual buttons "+" and "-" used to select command from received list.
While << and >> used for selecting IR commands.

By default first command is "ping".

Predefined server IP is 192.168.1.34

By soft keyboard user can change it to other one.

Next future planes:
- provide support RC5/RC6 and other not raw codes configuration LIRC files.
- derive a separate project for IR control with nice user interface
- provide connectivity over LAN using LIRC port for controlling KMPlayer, Dscaler, Foobar2000 etc.

Some pictures system in work:
https://plus.google.com/photos/113583868025097187752/albums/5817323723859125057