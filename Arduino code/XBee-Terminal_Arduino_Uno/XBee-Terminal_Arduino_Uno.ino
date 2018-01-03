#include <adk.h>

#if defined(LED_BUILTIN)
#define LED LED_BUILTIN // Use built in LED
#else
#define LED 13 // Set to something here that makes sense for your board.
#endif

#define XBEE_BUFFER_SIZE 64
#define ADK_BUFFER_SIZE 64

// Satisfy IDE, which only needs to see the include statment in the ino.
#ifdef dobogusinclude
#include <spi4teensy3.h>
#include <SPI.h>
#endif

#include <SoftwareSerial.h>

USB Usb;
ADK adk(&Usb, "XBEECOM", // Manufacturer Name
        "XBEECOM", // Model Name
        "Android XBee bridge", // Description (user-visible string)
        "1.0", // Version
        "https://play.google.com/store/apps/details?id=no.daffern.xbeecommunication", // URL (web page to visit if no installed apps support the accessory)
        "123456789"); // Serial Number (optional)

uint32_t timer;
bool connected;

//Arduino due has one Serial port, so use software serial on ports 5 and 6
SoftwareSerial altSerial(5, 6);

//buffers for android and XBee
uint8_t msg[ADK_BUFFER_SIZE];
uint8_t msg2[XBEE_BUFFER_SIZE];

void setup() {
  Serial.begin(57600);

  // Wait for serial port to connect
  #if !defined(__MIPSEL__)
    while (!Serial); 
  #endif

  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }

  altSerial.begin(57600);

  pinMode(LED, OUTPUT);
}

void loop() {

  Usb.Task();

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      digitalWrite(LED, HIGH);
      Serial.print(F("\r\nConnected to accessory"));
    }

    //read from android
    uint16_t len = ADK_BUFFER_SIZE;
    uint8_t rcode = adk.RcvData(&len, msg);
    
    if (rcode && rcode != hrNAK) {//something went wrong with the USB connection

      Serial.print(F("\r\nData rcv: "));
      Serial.print(rcode, HEX);

    } else if (len > 0) {//else write to XBEE

      Serial.print("\nreceived from adk: ");
      Serial.print(len);

      altSerial.write(msg, len);
    }

    //read from XBee
    uint8_t available = altSerial.available();
    if (available > 0) {
      uint8_t leng =  altSerial.readBytes(msg2, XBEE_BUFFER_SIZE);
      Serial.print("\nreceived from xbee: ");
      Serial.print(leng);

      //write to android
      if (leng > 0) {
        uint8_t rcode1 = adk.SndData(leng, msg2);
        if (rcode1 && rcode1 != hrNAK) {

          Serial.print(F("\r\nData snd: "));
          Serial.print(rcode1, HEX);
        }
      }
    }

  } else {
    if (connected) {
      connected = false;
      Serial.print(F("\r\nDisconnected from accessory"));
      digitalWrite(LED, LOW);
    }
  }
}
