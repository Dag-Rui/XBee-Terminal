#include <adk.h>

#ifdef dobogusinclude
#include <spi4teensy3.h>
#include <SPI.h>
#endif

//The LED on Arduino Due is connected to pin 13
#define LED 13 

#define XBEE_BUFFER_SIZE 64
#define ADK_BUFFER_SIZE 64

//USB class used communicate via USB
USBHost usb;

//Initialize ADK (object used to communicate with an Android device)
ADK adk(&usb, "XBEECOM", // Manufacturer Name
        "XBEECOM", // Model Name
        "Android XBee bridge", // Description (user-visible string)
        "1.0", // Version
        "https://play.google.com/store/apps/details?id=no.daffern.xbeecommunication", // URL (web page to visit if no installed apps support the accessory)
        "123456789"); // Serial Number (optional)

bool connected;

//Buffers for messages (Android to XBee and XBee to Android)
uint8_t adkBuffer[ADK_BUFFER_SIZE];
uint8_t xBeeBuffer[XBEE_BUFFER_SIZE];

void setup() {
  //Initialize serial port 0 (pins tx0 and rx0)
  Serial.begin(57600);

  //Initialize serial port 1 (pins tx1 and rx1)
  Serial1.begin(57600);

  //enables global interrupts (may not be needed)
  cpu_irq_enable();

  //Delay 200ms to wait for the serial ports to initialize
  delay(200);

  //Initialize the LED pin
  pinMode(LED, OUTPUT);

}

void loop() {
  //USB task needs to run once every iteration. 
  //This function handles lower level USB connection and communication
  usb.Task();

  //Turn on LED if Android device is connected
  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      digitalWrite(LED, HIGH);
    }

    //Read from USB and transmit to XBee if anything was read
    uint32_t adkLength = 0;
    uint32_t rcode = adk.read(&adkLength, ADK_BUFFER_SIZE, adkBuffer);
    
    if (adkLength > 0) {
      Serial1.write(adkBuffer, adkLength);
    }

    //Read from the XBee serial port and write bytes read to USB
    uint8_t available = Serial1.available();
    if (available > 0) {
      
      uint8_t xBeeLength =  Serial1.readBytes(xBeeBuffer, XBEE_BUFFER_SIZE);
      uint32_t rcode1 = adk.write(xBeeLength, xBeeBuffer);
      
    }

  } else {
    //Disable LED if USB is diconnected
    if (connected) {
      connected = false;
      digitalWrite(LED, LOW);
    }
  }
}
