// The source for the Android application can be found at the following link: https://github.com/Lauszus/ArduinoBlinkLED
// The code for the Android application is heavily based on this guide: http://allaboutee.com/2011/12/31/arduino-adk-board-blink-an-led-with-your-phone-code-and-explanation/ by Miguel
#include <adk.h>

//
// CAUTION! WARNING! ATTENTION! VORSICHT! ADVARSEL! ¡CUIDADO! ВНИМАНИЕ!
//
// Pin 13 is occupied by the SCK pin on various Arduino boards,
// including Uno, Duemilanove, etc., so use a different pin for those boards.
//
// CAUTION! WARNING! ATTENTION! VORSICHT! ADVARSEL! ¡CUIDADO! ВНИМАНИЕ!
//
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
ADK adk(&Usb, "TKJElectronics", // Manufacturer Name
        "ArduinoBlinkLED", // Model Name
        "Example sketch for the USB Host Shield", // Description (user-visible string)
        "1.0", // Version
        "http://www.tkjelectronics.dk/uploads/ArduinoBlinkLED.apk", // URL (web page to visit if no installed apps support the accessory)
        "123456789"); // Serial Number (optional)

uint32_t timer;
bool connected;



SoftwareSerial altSerial(5, 6);
void setup() {
  Serial.begin(57600);
#if !defined(__MIPSEL__)
  while (!Serial); // Wait for serial port to connect
#endif
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }

  //altSerial.begin(9600);
  //altSerial.println("Hello World");
  altSerial.begin(57600);
  //Serial1.print("hello");

  pinMode(LED, OUTPUT);
  //Serial.print("\r\nArduino Blink LED Started");
}

uint8_t msg[ADK_BUFFER_SIZE];
uint8_t msg2[XBEE_BUFFER_SIZE];

void loop() {

  Usb.Task();
  

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      digitalWrite(LED, HIGH);
      Serial.print(F("\r\nConnected to accessory"));
    }

    uint16_t len = ADK_BUFFER_SIZE;
    uint8_t rcode = adk.RcvData(&len, msg);
    if (rcode && rcode != hrNAK) {

      Serial.print(F("\r\nData rcv: "));
      Serial.print(rcode, HEX);

    } else if (len > 0) {

/*
      Serial.print(F("\r\nReceived from usb: "));
      Serial.print(msg[0]);
*/
      Serial.print("\nreceived from adk: ");
      Serial.print(len);

      altSerial.write(msg, len);
    }

    uint8_t available = altSerial.available();
    if (available > 0) {
      uint8_t leng =  altSerial.readBytes(msg2, XBEE_BUFFER_SIZE);
      Serial.print("\nreceived from xbee: ");
      Serial.print(leng);
/*
      Serial.print(F("\r\nReceived from XBee: "));
      Serial.print(msg2[0]);
*/
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
