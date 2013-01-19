#include "mbed.h"
#include "MPU6050.h"
#include <string>

using namespace std;

Serial pc(USBTX, USBRX);
Serial zigbee(p13, p14);
MPU6050 mpu(p9, p10);

float conversionFactor = 57.2957795;
float pitchFilterConst = 0.0001;
float rollFilterConst = 0.00004;
float KP = 1.0;
float KI = 0.0;
float KIlim = 0.5;
float KD = 0.2;
float KY = 0.2;
float pitch = 0;
float roll = 0;
float pitchError = 0;
float rollError = 0;
float loopTime = 0;
float accPitch = 0;
float accRoll = 0;
float gyroPitch = 0;
float gyroRoll = 0;
float gyroYaw = 0;
float pitchErrorIntegral = 0;
float rollErrorIntegral = 0;
float pitchFilterTime = 0;
float rollFilterTime = 0;
float Command[4];
float gyrodata[3];
float accdata[3];
float gyrozero[3];
float rate[3];

int Output[4];
int PWMmin = 1000;
int PWMmax = 2000;
int maxAngle = 30;
int maxRate = 180;
int throttleCommand = 0;
int pitchCommand = 0;
int rollCommand = 0;
int yawCommand = 0;
int Ab, Bb, Cb, Db;
int loopCount = 0;
int nodataTimeout = 0;

PwmOut motors[4]= {PwmOut(p21),PwmOut(p22),PwmOut(p23),PwmOut(p24)};

DigitalOut Boot(LED1);
DigitalOut SerialStatus(LED2);

Timer t;

int main(){
    // Setup serial ports
    pc.baud(115200);
    zigbee.baud(115200);

    
//    zigbee.printf("Uplink Established\n\r");
     //Setup PWM period
    for(int i = 0; i < 4; i++){
        motors[i].period_ms(20);
        motors[i].pulsewidth_us(1000);
        }

    pc.printf("\n\rPlace Quadracopter on level surface and wait for calibration to complete");

    // Take 10 data points from the gyro and flash LED1 while doing so 
    for(int p = 0; p < 11; p++){
        pc.printf(".");
        Boot = !Boot;
        wait(0.5);
        mpu.getGyro(gyrodata);
        for(int j = 0; j < 3; j++)
            gyrozero[j] += gyrodata[j];
    } 
    // Average 10 data points taken in previous loop
    for (int j = 0; j < 3; ++j)
        gyrozero[j] = gyrozero[j] / 10;

    pc.printf("\n\r");

    Boot = 1;
    pc.printf("Boot Complete\n\r");
    
    while(1){
        // Start loop timer
        t.start();
        
        /*while(zigbee.readable() == 0){
            SerialStatus = 0;
            nodataTimeout++;
            zigbee.printf("-1,-1,-1,-1,-1,-1,-1*");
            pc.printf("%d\n\r", nodataTimeout);
            wait(0.02);
            if(nodataTimeout >= 100){
                for(int i = 0; i < 4; i++)
                    motors[i].pulsewidth_us(1000);
            }
        }*/

        // Read data from serial port
        char tmp[1024];
        zigbee.scanf("%s",&tmp);
        sscanf(tmp, "[%03i,%03i,%03i,%03i],[%d,%d,%d,%d]", &throttleCommand, &pitchCommand, &rollCommand, &yawCommand, &Ab, &Bb, &Cb, &Db);
        
        if(zigbee.readable() == 1){
            nodataTimeout = 0;
            SerialStatus = !SerialStatus;
            
        }

        // Get Gyro and accelerometer form MPU-6050
        mpu.getGyro(gyrodata);
        mpu.getAccelero(accdata);

        // Convert gyro data to degrees/sec
        for(int j = 0; j < 3; j++)
            rate[j] = (gyrodata[j] - gyrozero[j])*conversionFactor;
        
        // Calculate angle from accelerometer
        accPitch = conversionFactor * (atan(accdata[1]/accdata[2]));
        accRoll = conversionFactor * (atan(accdata[0]/accdata[2]));

        // Calculate angle from gyro
        gyroPitch += float(rate[1]*loopTime);
        gyroRoll += float(rate[0]*loopTime);

        // Calculate complementary filter time constatnt for pitch
        pitchFilterTime = pitchFilterConst/(loopTime + pitchFilterConst);
        // Pitch complementary filter
        pitch = pitchFilterTime*(pitch + gyroPitch);
        pitch += (1 - pitchFilterTime)*(accPitch);
        // Pitch error calculation
        pitchError = float(((pitchCommand-128)/128) * maxAngle - pitch);
        // Pitch error intergration and limmiter
        /*pitchErrorIntegral += pitchError*loopTime;
        if(pitchErrorIntegral >= KIlim){
            pitchErrorIntegral = KIlim;
        }
        if(pitchErrorIntegral <= -KIlim){
            pitchErrorIntegral = -KIlim;
        }
        */
        
        // Calculate complementary filter time constatnt for roll
        rollFilterTime = rollFilterConst/(loopTime+rollFilterConst);
        // Roll complementary filter
        roll = rollFilterTime*(roll+ gyroRoll);
        roll += (1- rollFilterTime)*(accRoll);
        // Roll error calculation
        rollError = float(((rollCommand-128)/128) * maxAngle - roll);
        // Roll error intergration and limmiter
        /*rollErrorIntegral += rollError*loopTime;
        if(rollErrorIntegral >= KIlim){
            rollErrorIntegral = KIlim;
        }
        if(rollErrorIntegral <= -KIlim){
            rollErrorIntegral = -KIlim;
        }
        */
        
        // Front PID
        Command[0] = -(pitchError * KP);
        Command[0] -= pitchErrorIntegral * KI;
        Command[0] += rate[1] * KD;
        Command[0] -= float((((yawCommand - 128) / 128.0) * maxRate - rate[2]) * KY);
        // Rear PID
        Command[2] = pitchError * KP;
        Command[2] += pitchErrorIntegral * KI;
        Command[2] -= rate[1] * KD;
        Command[2] -= float((((yawCommand - 128) / 128.0) * maxRate - rate[2]) * KY);
        // Right PID
        Command[1] = -(rollError * KP);
        Command[1] -= rollErrorIntegral * KI;
        Command[1] += rate[0] * KD;
        Command[1] += float((((yawCommand - 128) / 128.0) * maxRate - rate[2]) * KY);
        // Left PID
        Command[3] = rollError * KP;
        Command[3] += rollErrorIntegral * KI;
        Command[3] -= rate[0] * KD;
        Command[3] += float((((yawCommand - 128) / 128.0) * maxRate - rate[2]) * KY);

        // Prepair output values
        for(int i = 0; i < 4; i++)
            Output[i] = int((((Command[i] + throttleCommand) * 3.921568627)+1000)+ 0.5);

        // Apply limmits to output values
        for(int i = 0; i < 4; i++){
            if(Output[i] > PWMmax){
                Output[i] = PWMmax;
            }
            if(Output[i] < PWMmin){
                Output[i] = PWMmin;
            }
        }

        // Output PWM signals
        for(int i = 0; i < 4; i++)
            motors[i].pulsewidth_us(Output[i]);

        zigbee.printf("%d,%d,%d,%d,%f,%f,%f*", Output[0], Output[1], Output[2], Output[3], pitch, roll, rate[2]);
        
        // Debug
        //pc.printf("% d,% d,% d,% d,% f,% f,% f,% f,% f,% f,\n\r", Output[0], Output[1], Output[2], Output[3], Command[0], Command[1], Command[2], Command[3], pitch, roll);

        // Stop, read and reset loop timer
        t.stop();
        loopTime = t.read_ms();
        t.reset();
    }
}