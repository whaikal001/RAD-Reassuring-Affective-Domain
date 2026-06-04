#include "additional.h"

#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

const int buzzer = 16;  //buzzer pin

#define right_Sensor 27
#define left_Sensor 12

int buttonState1 = 0;
int buttonState2 = 0;
int buttonState3 = 0;

#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels

#define SDA_1 21
#define SCL_1 22

#define SDA_2 33
#define SCL_2 32
// Declaration for an SSD1306 display connected to I2C (SDA, SCL pins)
// The pins for I2C are defined by the Wire-library. 
// On an arduino UNO:       A4(SDA), A5(SCL)
// On an arduino MEGA 2560: 20(SDA), 21(SCL)
// On an arduino LEONARDO:   2(SDA),  3(SCL), ...
#define OLED_RESET     -1 // Reset pin # (or -1 if sharing Arduino reset pin)
#define SCREEN_ADDRESS 0x3C ///< See datasheet for Address; 0x3D for 128x64, 0x3C for 128x32
Adafruit_SSD1306 oled1(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire,-1,400000,100000);//important to create this
Adafruit_SSD1306 oled2(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire1,-1,400000,100000);//important to create this

#define NUMFLAKES     10 // Number of snowflakes in the animation example

#define LOGO_HEIGHT   16
#define LOGO_WIDTH    16

String serialBuffer = "";
bool blinkMode = true;
unsigned long lastBlinkAt = 0;
const unsigned long blinkIntervalMs = 3000;

void handleSerialInput();
void processCommand(String command);
void handleEmotionCommand(String payload);
void showFaceByName(String faceName);
void sad_Face();





void setup() {

  pinMode(left_Sensor, INPUT);
  pinMode(right_Sensor, INPUT);

  pinMode(buzzer, OUTPUT);

  Serial.begin(115200);

  Wire.begin(SDA_1, SCL_1);//I2C number 1 for the firts robot eye and all other sensors.
  Wire1.begin(SDA_2, SCL_2);//I2C number 2 for the second oled robot eye.

  oled1.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS);
  oled2.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS);

  normal_Face();
  Serial.println("PBOT_READY");

  delay(20);

}




void loop() {
  handleSerialInput();

  if (blinkMode && (millis() - lastBlinkAt >= blinkIntervalMs)) {
    kelip();
    lastBlinkAt = millis();
  }
}

void handleSerialInput()
{
  while (Serial.available() > 0) {
    char incoming = (char)Serial.read();

    if (incoming == '\r') {
      continue;
    }

    if (incoming == '\n') {
      serialBuffer.trim();
      if (serialBuffer.length() > 0) {
        processCommand(serialBuffer);
      }
      serialBuffer = "";
      continue;
    }

    serialBuffer += incoming;
  }
}

void processCommand(String command)
{
  String raw = command;
  command.trim();
  String upper = command;
  upper.toUpperCase();

  if (upper == "PING") {
    Serial.println("PONG");
    return;
  }

  if (upper == "HELP") {
    Serial.println("OK:EMOTION:<name>[:intensity]|FACE:<name>|BLINK:ON|BLINK:OFF|PING");
    return;
  }

  if (upper.startsWith("BLINK:")) {
    String value = upper.substring(6);
    value.trim();
    blinkMode = (value == "ON");
    if (!blinkMode) {
      normal_Face();
    }
    Serial.println(blinkMode ? "OK:BLINK_ON" : "OK:BLINK_OFF");
    return;
  }

  if (upper.startsWith("EMOTION:")) {
    String payload = command.substring(8);
    handleEmotionCommand(payload);
    return;
  }

  if (upper.startsWith("FACE:")) {
    String face = command.substring(5);
    showFaceByName(face);
    Serial.print("OK:FACE:");
    Serial.println(face);
    return;
  }

  Serial.print("ERR:UNKNOWN_COMMAND:");
  Serial.println(raw);
}

void handleEmotionCommand(String payload)
{
  payload.trim();

  int firstSeparator = payload.indexOf(':');
  String emotion = payload;
  int intensity = 5;

  if (firstSeparator >= 0) {
    emotion = payload.substring(0, firstSeparator);
    String intensityValue = payload.substring(firstSeparator + 1);
    intensityValue.trim();
    if (intensityValue.length() > 0) {
      intensity = intensityValue.toInt();
    }
  }

  emotion.trim();
  emotion.toLowerCase();

  if (emotion == "happy" || emotion == "joy") {
    happy_Face();
    emotion = "happy";
  } else if (emotion == "sad" || emotion == "sadness" || emotion == "lonely" || emotion == "loneliness" || emotion == "hopeless" || emotion == "depressed") {
    sad_Face();
    emotion = "sad";
  } else if (emotion == "angry" || emotion == "anger" || emotion == "frustrated") {
    angry_Face();
    emotion = "angry";
  } else if (emotion == "sus" || emotion == "anxious" || emotion == "anxiety" || emotion == "stressed" || emotion == "stress" || emotion == "worried" || emotion == "panic" || emotion == "confused") {
    sus_Face();
    emotion = "sus";
  } else if (emotion == "sleep" || emotion == "sleepy" || emotion == "exhaustion" || emotion == "exhausted" || emotion == "tired" || emotion == "fatigue") {
    sleepy_Face();
    emotion = "sleep";
  } else if (emotion == "normal" || emotion == "neutral" || emotion == "calm" || emotion == "relaxed") {
    normal_Face();
    emotion = "normal";
  } else {
    normal_Face();
    emotion = "normal";
  }

  if (intensity >= 8) {
    tone(buzzer, 880, 120);
  } else if (intensity >= 5) {
    tone(buzzer, 660, 80);
  }

  blinkMode = false;
  Serial.print("OK:EMOTION:");
  Serial.print(emotion);
  Serial.print(":");
  Serial.println(intensity);
}

void showFaceByName(String faceName)
{
  faceName.trim();
  faceName.toLowerCase();

  if (faceName == "angry") {
    angry_Face();
  } else if (faceName == "happy") {
    happy_Face();
  } else if (faceName == "normal") {
    normal_Face();
  } else if (faceName == "left") {
    left_Face();
  } else if (faceName == "right") {
    right_Face();
  } else if (faceName == "sus") {
    sus_Face();
  } else if (faceName == "confuse") {
    confuse_Face();
  } else if (faceName == "close") {
    close_Face();
  } else if (faceName == "sleepy") {
    sleepy_Face();
  } else if (faceName == "sad") {
    sad_Face();
  } else {
    normal_Face();
  }

  blinkMode = false;
}


//////////////////////////////////////////////////////////////////////////////////////
void kelip()
{
//close eye
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_closeeye1, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_closeeye1, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(200);

 oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
 
}



void testdrawbitmap() {
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_angryeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_angryeye2, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_downeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_downeye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_upeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_upeye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

      oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_righteye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_righteye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

    oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

      oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_lefteye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_lefteye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

    oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

   oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_happyeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_happyeye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

 oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

      oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_suseye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_suseye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

    oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

    oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sadeye1, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sadeye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

       oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

    oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sadeye2, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sadeye2, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  
 oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

   oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled1.display();
  oled2.display();
    oled1.setRotation(1);
  oled2.setRotation(1);
  delay(500);

   oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled1.display();
  oled2.display();
    oled1.setRotation(2);
  oled2.setRotation(2);
  delay(500);

   oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled1.display();
  oled2.display();
    oled1.setRotation(3);
  oled2.setRotation(3);
  delay(500);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled1.display();
  oled2.display();
    oled1.setRotation(0);
  oled2.setRotation(0);
  delay(500);

   
 oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sharinganeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sharinganeye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  
 oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

 
 oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sleepeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sleepeye, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);

  //animate(logo_bmp, LOGO_WIDTH, LOGO_HEIGHT);
}


void angry_Face()
{
    oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_angryeye, 128, 64, 1);
    oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_angryeye2, 128, 64, 1);
    oled1.display();
    oled2.display();
   
}

void happy_Face()
{
   oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_happyeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_happyeye, 128, 64, 1);
  oled1.display();
  oled2.display();

}

void normal_Face()
{
   oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
    oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_normaleye, 128, 64, 1);
    oled1.display();
    oled2.display();
   
}

void leftwink_Face()
{
oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_happyeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_closeeye, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void rightwink_Face()
{
oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_closeeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_happyeye, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void sus_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_suseye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_suseye, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void left_Face()
{
    oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_lefteye, 128, 64, 1);
    oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_lefteye, 128, 64, 1);
    oled1.display();
    oled2.display();
}

void right_Face()
{
     oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_righteye, 128, 64, 1);
    oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_righteye, 128, 64, 1);
    oled1.display();
    oled2.display();
}

void confuse_Face()
{
   oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_confuseeye, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void sharingan_Face()
{

 oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sharinganeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sharinganeye, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void close_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_closeeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_closeeye, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void wificonf_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_wificonf, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_wificonf, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void Gamemode_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_Gamemode, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_Gamemode, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void rightans_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_rightans, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_rightans, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void wrongans_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_wrongans, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_wrongans, 128, 64, 1);
  oled1.display();
  oled2.display();
}

void target_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_target, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_angryeye2, 128, 64, 1);
  oled1.display();
  oled2.display();
}



void sleepy_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sleepy3, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sleepy3, 128, 64, 1);
  oled1.display();
  oled2.display();

  delay(1000);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_closeeye1, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_closeeye1, 128, 64, 1);
  oled1.display();
  oled2.display();

  delay(100);

  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sleepy3, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sleepy3, 128, 64, 1);
  oled1.display();
  oled2.display();

  delay(1000);


  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_closeeye1, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_closeeye1, 128, 64, 1);
  oled1.display();
  oled2.display();

  delay(100);

   oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sleepy3, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_closeeye, 128, 64, 1);
  oled1.display();
  oled2.display();

  delay(1000);

}

//////////////////////////////////////////////////////////////////////////////////////
// NEW EMOTION FUNCTIONS - Mapped to Backend EmotionalHistory states
//////////////////////////////////////////////////////////////////////////////////////

// Map to backend "sadness" emotion

// Map to backend "stress" emotion - confused eyes with shaking animation
void stress_Face()
{
  for (int i = 0; i < 3; i++) {
    oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
      (oled1.width()  - 128 ) / 2,
      (oled1.height() - 64) / 2,
      epd_bitmap_confuseeye, 128, 64, 1);
    oled2.drawBitmap(
      (oled2.width()  - 128 ) / 2,
      (oled2.height() - 64) / 2,
      epd_bitmap_confuseeye, 128, 64, 1);
    oled1.display();
    oled2.display();
    delay(200);
    
    oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
      (oled1.width()  - 128 ) / 2 + 2,
      (oled1.height() - 64) / 2,
      epd_bitmap_confuseeye, 128, 64, 1);
    oled2.drawBitmap(
      (oled2.width()  - 128 ) / 2 - 2,
      (oled2.height() - 64) / 2,
      epd_bitmap_confuseeye, 128, 64, 1);
    oled1.display();
    oled2.display();
    delay(200);
  }
}

// Map to backend "anxiety" emotion - rapid blinking with worried eyes
void anxiety_Face()
{
  for (int i = 0; i < 5; i++) {
    oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
      (oled1.width()  - 128 ) / 2,
      (oled1.height() - 64) / 2,
      epd_bitmap_suseye, 128, 64, 1);
    oled2.drawBitmap(
      (oled2.width()  - 128 ) / 2,
      (oled2.height() - 64) / 2,
      epd_bitmap_suseye, 128, 64, 1);
    oled1.display();
    oled2.display();
    delay(100);
    
    oled1.clearDisplay();
    oled2.clearDisplay();
    oled1.drawBitmap(
      (oled1.width()  - 128 ) / 2,
      (oled1.height() - 64) / 2,
      epd_bitmap_closeeye1, 128, 64, 1);
    oled2.drawBitmap(
      (oled2.width()  - 128 ) / 2,
      (oled2.height() - 64) / 2,
      epd_bitmap_closeeye1, 128, 64, 1);
    oled1.display();
    oled2.display();
    delay(50);
  }
}

// Map to backend "loneliness" emotion - sad eyes looking down
void lonely_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sadeye2, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sadeye2, 128, 64, 1);
  oled1.display();
  oled2.display();
  delay(500);
  
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_downeye, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_downeye, 128, 64, 1);
  oled1.display();
  oled2.display();
}

// Map to backend "exhaustion" emotion
void exhausted_Face()
{
  sleepy_Face();
}

//////////////////////////////////////////////////////////////////////////////////////
// EMOTION HANDLER - Receives emotion string from backend and displays matching face
//////////////////////////////////////////////////////////////////////////////////////

void showEmotion(String emotion)
{
  emotion.toLowerCase();
  
  if (emotion == "joy" || emotion == "happy") {
    happy_Face();
  } 
  else if (emotion == "anger" || emotion == "angry") {
    angry_Face();
  }
  else if (emotion == "sadness" || emotion == "sad") {
    sad_Face();
  }
  else if (emotion == "stress" || emotion == "stressed") {
    stress_Face();
  }
  else if (emotion == "anxiety" || emotion == "anxious") {
    anxiety_Face();
  }
  else if (emotion == "exhaustion" || emotion == "tired" || emotion == "exhausted") {
    exhausted_Face();
  }
  else if (emotion == "loneliness" || emotion == "lonely") {
    lonely_Face();
  }
  else if (emotion == "confusion" || emotion == "confused") {
    confuse_Face();
  }
  else if (emotion == "surprise" || emotion == "surprised") {
    sus_Face();
  }
  else {
    normal_Face();
  }
}

void sad_Face()
{
  oled1.clearDisplay();
  oled2.clearDisplay();
  oled1.drawBitmap(
    (oled1.width()  - 128 ) / 2,
    (oled1.height() - 64) / 2,
    epd_bitmap_sadeye1, 128, 64, 1);
  oled2.drawBitmap(
    (oled2.width()  - 128 ) / 2,
    (oled2.height() - 64) / 2,
    epd_bitmap_sadeye, 128, 64, 1);
  oled1.display();
  oled2.display();
}