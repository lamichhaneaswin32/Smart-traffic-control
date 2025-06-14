const int pirPins[2]    = {6, 7};       // NS, EW PIR sensors
const int greenPins[2]  = {10, 13};     // NS, EW Green LEDs
const int yellowPins[2] = {11, A0};     // NS, EW Yellow LEDs
const int redPins[2]    = {12, A1};     // NS, EW Red LEDs

int vehicleCounts[2];         // For NS and EW
int greenTimes[2];            // Duration for green phase

int currentDirection = 0;     // 0: NS, 1: EW
int previousDirection = -1;   // Direction just completed
int redBlinkCount = 0;
bool redBlinkState = false;
unsigned long lastBlinkTime = 0;

int phase = 0;                // 0 = Green, 1 = Yellow, 2 = Red
unsigned long startTime = 0;
const unsigned long yellowDuration = 5000;

void setup() {
  Serial.begin(9600);

  for (int i = 0; i < 2; i++) {
    pinMode(pirPins[i], INPUT);
    pinMode(greenPins[i], OUTPUT);
    pinMode(yellowPins[i], OUTPUT);
    pinMode(redPins[i], OUTPUT);
  }

  getVehicleCounts();
  calculateGreenTimes();
  startGreenPhase(currentDirection);
}

void loop() {
  unsigned long currentTime = millis();

  if (phase == 0 && currentTime - startTime >= greenTimes[currentDirection]) {
    startYellowPhase(currentDirection);
  } else if (phase == 1 && currentTime - startTime >= yellowDuration) {
    startRedPhase(currentDirection);

    previousDirection = currentDirection;
    redBlinkCount = vehicleCounts[previousDirection];
    
    currentDirection = (currentDirection + 1) % 2;
    getVehicleCounts();
    calculateGreenTimes();
    startGreenPhase(currentDirection);
  }

  handleRedBlinking();
  delay(200);
}

void getVehicleCounts() {
  for (int i = 0; i < 2; i++) {
    vehicleCounts[i] = digitalRead(pirPins[i]) == HIGH ? 5 : 0;
  }
}

void calculateGreenTimes() {
  for (int i = 0; i < 2; i++) {
    greenTimes[i] = vehicleCounts[i] > 0 ? 30000 : 15000;
  }
}

void startGreenPhase(int dir) {
  phase = 0;
  startTime = millis();

  digitalWrite(greenPins[dir], HIGH);
  digitalWrite(yellowPins[dir], LOW);
  digitalWrite(redPins[dir], LOW);

  int other = (dir + 1) % 2;
  digitalWrite(greenPins[other], LOW);
  digitalWrite(yellowPins[other], LOW);
  digitalWrite(redPins[other], HIGH);
}

void startYellowPhase(int dir) {
  phase = 1;
  startTime = millis();

  digitalWrite(greenPins[dir], LOW);
  digitalWrite(yellowPins[dir], HIGH);
  digitalWrite(redPins[dir], LOW);
}

void startRedPhase(int dir) {
  phase = 2;

  digitalWrite(greenPins[dir], LOW);
  digitalWrite(yellowPins[dir], LOW);
  digitalWrite(redPins[dir], HIGH);
}

void handleRedBlinking() {
  if (previousDirection == -1 || redBlinkCount <= 0) return;

  unsigned long currentTime = millis();

  if (currentTime - lastBlinkTime >= 500) {
    redBlinkState = !redBlinkState;
    digitalWrite(redPins[previousDirection], redBlinkState ? HIGH : LOW);
    lastBlinkTime = currentTime;

    if (!redBlinkState) {
      redBlinkCount--;
    }

    if (redBlinkCount <= 0) {
      digitalWrite(redPins[previousDirection], HIGH);
      previousDirection = -1;
    }
  }
}