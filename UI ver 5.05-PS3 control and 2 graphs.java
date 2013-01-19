import procontroll.*;
import java.io.*;
import processing.serial.*;

ControllIO controll;
ControllDevice joystick;
ControllStick stick0;
ControllStick stick1;
ControllStick stick2;
ControllStick stick3;
ControllCoolieHat cooliehat;
ControllButton TriangleButton;
ControllButton CircleButton;
ControllButton CrossButton;
ControllButton SquareButton;
ControllButton L1Button;
ControllButton R1Button;
ControllButton L2Button;
ControllButton R2Button;
ControllButton SelectButton;
ControllButton LeftStickButton;
ControllButton RightStickButton;
ControllButton StartButton;
ControllButton PSButton;

// Globals
int WindowWidth      = 1900;
int WindowHeight     = 1000;
boolean SaveToFile   = true;  // Dumps data to c:\\output.txt in a comma seperated format (easy to import into Excel)
boolean EnableDataFilter = true;  // Enables simple filter to help smooth out data.

int throttle;
int roll;
int pitch;
int yaw;
boolean controllerEnable = false;

cDataArray Data1    = new cDataArray(200);
cDataArray Data2    = new cDataArray(200);
cDataArray Data3    = new cDataArray(200);
cDataArray Data4    = new cDataArray(200);
cDataArray Data5    = new cDataArray(200);
cDataArray Data6    = new cDataArray(200);
cDataArray Data7    = new cDataArray(200);
cDataArray Data8    = new cDataArray(200);
cDataArray Data9    = new cDataArray(200);
cDataArray Data10    = new cDataArray(200);
cDataArray Data11    = new cDataArray(200);
cDataArray Data12    = new cDataArray(200);
cGraph LeftGraph         = new cGraph(10, 190, ((WindowWidth/2)-20), 800);
cGraph RightGraph        = new cGraph(((WindowWidth/2)+10), 190, ((WindowWidth/2)-20), 800);
Serial zigbee;

void setup(){
  size(WindowWidth, WindowHeight, P2D);

  println(Serial.list());
  //zigbee = new Serial(this, Serial.list()[0], 115200);
  zigbee = new Serial(this, "COM9", 115200);
  
  controll = ControllIO.getInstance(this);
  
  joystick = controll.getDevice("MotioninJoy Virtual Game Controller");         // Select controler
  
  stick0 = joystick.getStick("X axis Y axis");                                  // Left stick
  stick0.setTolerance(0.05f);
  stick0.setMultiplier(100);
  
  stick1 = joystick.getStick("Y Rotation Z Rotation");                          // Right stick
  stick1.setTolerance(0.05f);
  stick1.setMultiplier(100);
  
  stick2 = joystick.getStick("Z Axis X Rotation");                              // L2 R2
  stick2.setTolerance(0.05f);
  stick2.setMultiplier(100);
  
  stick3 = joystick.getStick("Slider Dial");                                    // Controller rotation
  stick3.setTolerance(0.05f);
  stick3.setMultiplier(100);
  
  cooliehat = joystick.getCoolieHat(20);
  cooliehat.setMultiplier(100);
  
  // This draws the graph key info
  strokeWeight(1.5);
  stroke(255, 0, 0);     line(20, 875, 35, 875);
  stroke(0, 255, 0);     line(20, 895, 35, 895);
  stroke(0, 0, 255);     line(20, 915, 35, 915);
  stroke(255, 255, 0);   line(20, 935, 35, 935);
  stroke(255, 0, 255);   line(20, 955, 35, 955);
  stroke(0, 255, 255);   line(20, 975, 35, 975);

  stroke(255, 0, 0);     line(980, 875, 995, 875);
  stroke(0, 255, 0);     line(980, 895, 995, 895);
  stroke(0, 0, 255);     line(980, 915, 995, 915);
  stroke(255, 255, 0);   line(980, 935, 995, 935);
  stroke(255, 0, 255);   line(980, 955, 995, 955);
  stroke(0, 255, 255);   line(980, 975, 995, 975);
  
  fill(0);
  text("Data 1", 40, 880);
  text("Data 2", 40, 900);
  text("Data 3", 40, 920);
  text("Data 4", 40, 940);
  text("Data 5", 40, 960);
  text("Data 6", 40, 980);

  text("Data 7", 1000, 880);
  text("Data 8", 1000, 900);
  text("Data 9", 1000, 920);
  text("Data 10", 1000, 940);
  text("Data 11", 1000, 960);
  text("Data 12", 1000, 980);
  
  text("MOTOR OUTPUT POWER",((WindowWidth/4)-100),830);
  text("QUADRACOPTER ANGLE",((3*(WindowWidth/4))-100),830);
  
  rectMode(CORNERS);
  
  if (SaveToFile)
  {
    // This clears deletes the old file each time the app restarts
    byte[] tmpChars = {'\r', '\n'};
    saveBytes("c:\\output.txt", tmpChars);
  }
  zigbee.write("[0,128,128,128],[0,0,0,0]\n\r");
}

void draw()
{
  // We need to read in all the avilable data so graphing doesn't lag behind
  while (zigbee.available() >1){
    processSerialData();
  }
  
  stroke(0);
  strokeWeight(1);
  if(controllerEnable){
    throttle = round(map(stick0.getY(),100,-100,0,255));
    yaw = round(map(stick0.getX(),-100,100,0,255));
    pitch = round(map(stick1.getX(),100,-100,0,255));
    roll = round(map(stick1.getY(),-100,100,0,255));
    fill(0,255,0);
  }else{
    throttle = 0;
    yaw = pitch = roll = 128;
    fill(255,0,0);
  }
  rect(1750,960,1850,980);
  fill(0);
  
  if(zigbee.available() == 0){
    zigbee.write("[" + throttle + "," + pitch + "," + roll+ "," + yaw + "],[0,0,0,0]\n\r");
  }

  strokeWeight(1);
  fill(255, 255, 255);
  LeftGraph.drawGraphBox();
  RightGraph.drawGraphBox();
  fill(0);
  text("75%",15,205);
  line(10,210,((WindowWidth/2)-10),210);
  text("50%",15,405);
  line(10,410,((WindowWidth/2)-10),410);
  text("25%",15,605);
  line(10,610,((WindowWidth/2)-10),610);
  text("0°",((WindowWidth/2)+15),405);
  line(((WindowWidth/2)+10),410,(WindowWidth-10),410);
  text("-10°",((WindowWidth/2)+15),494);
  line(((WindowWidth/2)+10),499,(WindowWidth-10),499);
  text("10°",((WindowWidth/2)+15),316);
  line(((WindowWidth/2)+10),321,(WindowWidth-10),321);
  text("-20°",((WindowWidth/2)+15),583);
  line(((WindowWidth/2)+10),588,(WindowWidth-10),588);
  text("20°",((WindowWidth/2)+15),227);
  line(((WindowWidth/2)+10),232,(WindowWidth-10),232);
  text("-30°",((WindowWidth/2)+15),672);
  line(((WindowWidth/2)+10),677,(WindowWidth-10),677);
  text("30°",((WindowWidth/2)+15),138);
  line(((WindowWidth/2)+10),143,(WindowWidth-10),143);
  text("-40°",((WindowWidth/2)+15),761);
  line(((WindowWidth/2)+10),766,(WindowWidth-10),766);
  text("40°",((WindowWidth/2)+15),49);
  line(((WindowWidth/2)+10),54,(WindowWidth-10),54);
  
  strokeWeight(1.5);
  stroke(255, 0, 0);
  LeftGraph.drawLine(Data1, 0, 1024);
  stroke(0, 255, 0);
  LeftGraph.drawLine(Data2, 0, 1024);
  stroke(0, 0, 255);
  LeftGraph.drawLine(Data3, 0, 1024);
  stroke(255, 255, 0);
  LeftGraph.drawLine(Data4, 0, 1024);
  stroke(255, 0, 255);
  LeftGraph.drawLine(Data5, 0, 1024);
  stroke(0, 255, 255);
  LeftGraph.drawLine(Data6, 0, 1024);

  stroke(255, 0, 0);
  RightGraph.drawLine(Data7, 0, 1024);
  stroke(0, 255, 0);
  RightGraph.drawLine(Data8, 0, 1024);
  stroke(0, 0, 255);
  RightGraph.drawLine(Data9, 0, 1024);
  stroke(255, 255, 0);
  RightGraph.drawLine(Data10, 0, 1024);
  stroke(255, 0, 255);
  RightGraph.drawLine(Data11, 0, 1024);
  stroke(0, 255, 255);
  RightGraph.drawLine(Data12, 0, 1024);
}

// This reads in one set of the data from the serial port
void processSerialData(){
  String rx = zigbee.readStringUntil('*');
  if (rx != null) {
    float[] RX = float(splitTokens(rx, ",*"));
    
    for (int i = 0; i < 4; i++){
      if(RX[i] < 1000){
          RX[i] = 1000;
      }else if(RX[i] > 2000){
        RX[i] = 2000;
      }
    }
    
    Data1.addVal(map(RX[0],1000,2000,0,800));
    Data2.addVal(map(RX[1],1000,2000,0,800));
    Data3.addVal(map(RX[2],1000,2000,0,800));
    Data4.addVal(map(RX[3],1000,2000,0,800));
    Data5.addVal(0);
    Data6.addVal(0);
    Data7.addVal(map(RX[4],-45,45,0,800));
    Data8.addVal(map(RX[5],-45,45,0,800));
    Data9.addVal(0);
    Data10.addVal(0);
    Data11.addVal(0);
    Data12.addVal(0);

    if (SaveToFile){  // Dump data to a file if needed
      String tempStr;
      tempStr = Data1 + "," + Data2 + "," + Data3 + "," + Data4 + "," + Data5 + "," + Data6 + "," + Data7 + "," + Data8 + "," + Data9 + "," + Data10 + "," + Data11 + "," + Data12 + "\r\n";
      FileWriter file;

      try{  
        file = new FileWriter("c:\\output.txt", true); //bool tells to append
        file.write(tempStr, 0, tempStr.length()); //(string, start char, end char)
        file.close();
      }  
      catch(Exception e){  
        println("Error: Can't open file!");
      }
    }
  }
  zigbee.write("[" + throttle + "," + pitch + "," + roll+ "," + yaw + "],[0,0,0,0]\n\r");
  print("[" + throttle + "," + pitch + "," + roll+ "," + yaw + "]\n\r");
}

// This class helps mangage the arrays of data I need to keep around for graphing.
class cDataArray{
  float[] m_data;
  int m_maxSize;
  int m_startIndex = 0;
  int m_endIndex = 0;
  int m_curSize;
  
  cDataArray(int maxSize){
    m_maxSize = maxSize;
    m_data = new float[maxSize];
  }
  
  void addVal(float val){
    
    if (EnableDataFilter && (m_curSize != 0)){
      int indx;
      
      if (m_endIndex == 0)
        indx = m_maxSize-1;
      else
        indx = m_endIndex - 1;
      
      m_data[m_endIndex] = getVal(indx)*.5 + val*.5;
    }
    else{
      m_data[m_endIndex] = val;
    }
    
    m_endIndex = (m_endIndex+1)%m_maxSize;
    if (m_curSize == m_maxSize){
      m_startIndex = (m_startIndex+1)%m_maxSize;
    }
    else{
      m_curSize++;
    }
  }
  
  float getVal(int index){
    return m_data[(m_startIndex+index)%m_maxSize];
  }
  
  int getCurSize(){
    return m_curSize;
  }
  
  int getMaxSize(){
    return m_maxSize;
  }
}

// This class takes the data and helps graph it
class cGraph
{
  float m_gWidth, m_gHeight;
  float m_gLeft, m_gBottom, m_gRight, m_gTop;
  
  cGraph(float x, float y, float w, float h)
  {
    m_gWidth     = w;
    m_gHeight    = h;
    m_gLeft      = x;
    m_gBottom    = WindowHeight - y;
    m_gRight     = x + w;
    m_gTop       = WindowHeight - y - h;
  }
  
  void drawGraphBox()
  {
    stroke(0, 0, 0);
    rectMode(CORNERS);
    rect(m_gLeft, m_gBottom, m_gRight, m_gTop);
  }
  
  void drawLine(cDataArray data, float minRange, float maxRange)
  {
    float graphMultX = m_gWidth/data.getMaxSize();
    float graphMultY = m_gHeight/(maxRange-minRange);
    
    for(int i=0; i<data.getCurSize()-1; ++i)
    {
      float x0 = i*graphMultX+m_gLeft;
      float y0 = m_gBottom-((data.getVal(i)-minRange)*graphMultY);
      float x1 = (i+1)*graphMultX+m_gLeft;
      float y1 = m_gBottom-((data.getVal(i+1)-minRange)*graphMultY);
      line(x0, y0, x1, y1);
    }
  }
}
//(1750,960,1850,980)
void mousePressed(){
    if(mouseX >= 1750 && mouseY >= 960 && mouseX <= 1850 && mouseY <= 980){
        controllerEnable = !controllerEnable;
    }
  }