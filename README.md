Android app that allows for wireless text and voice communication using XBee DigiMesh radio modules (Only tested with XBee-PRO DigiMesh 2.4). The XBee modules must be configured to communicate with each other and must be in API mode. The XBee module communicates with the Android device through USB.

It uses this library: https://github.com/felHR85/UsbSerial for USB serial communication with the XBee(For Android devices with USB Host capability). The USB accessory API is also implemented allowing the Android device to connect to for example an Arduino Due which then acts as the serial converter to the XBee module. (Arduino Due and Arduino Uno code is included in the "Arduino Code" folder.)

Google Play: https://play.google.com/store/apps/details?id=no.daffern.xbeecommunication
