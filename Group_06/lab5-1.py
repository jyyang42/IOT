import machine
import ssd1306
import time

import network
import urequests
import json
import socket



# return AM/PM time 
def returnAmPmString(temphour):
    ampm = "AM"
    if (temphour >= 12):
        temphour = temphour - 12
        if (temphour == 0):
            temphour = 12
        ampm = "PM"
    return ampm

def http_get(url):
    _, _, host, path = url.split('/', 3)
    addr = socket.getaddrinfo(host, 80)[0][-1]
    s = socket.socket()
    s.connect(addr)
    s.send(bytes('GET /%s HTTP/1.0\r\nHost: %s\r\n\r\n' % (path, host), 'utf8'))
    while True:
        data = s.recv(100)
        if data:
            print(str(data, 'utf8'), end='')
        else:
            break
    s.close()

#Configure Wifi
sta_if = network.WLAN(network.STA_IF)
ap_if = network.WLAN(network.AP_IF)

# Defining i2c and oled
i2c = machine.I2C(scl=machine.Pin(5), sda=machine.Pin(4))
oled = ssd1306.SSD1306_I2C(128, 32,i2c)

#Network Info
print(sta_if.active())


#Try to connect to Columbia Wifi
sta_if.active(True)
sta_if.connect('Columbia University',' ')

#Wait for the ESP8266 to be connected to wifi
print("Waiting to be connected!")
while(sta_if.isconnected()==False):
    pass
print("Connected to Wifi!")

print(sta_if.ifconfig())



# print time 
rtc = machine.RTC()
rtc.datetime((2017, 9, 26, 3, 16, 58, 0, 0))
print(rtc.datetime())


# GET IP address of the Huzzah 
import socket
addr = socket.getaddrinfo(sta_if.ifconfig()[0], 80)[0][-1]



# setting parameters for the socket 
s = socket.socket()
s.bind(addr)
s.listen(1)
# if within 0.5 seconds no request, pass and keep listenning
s.settimeout(0.5)

print('listening on', addr)

# Define States: 
 



state=0
while True:
    
    # State0: Turn on displace
    if(state==0):
        oled.fill(0)
        oled.show()
    
    # State1:Show Hello
    elif(state==1):
        oled.fill(0)
        oled.text("Hello!", 30, 0)
        oled.show()
        
    # State 2 Show time 
    elif(state==2):
        RealTime = rtc.datetime()
        oled.fill(0)

        hourRT = RealTime[4]
        minuteRT = RealTime[5]
        secondRT = RealTime[6]
        ampm= returnAmPmString(hourRT)
        oled.text(("%d:%02d:%02d "+ ampm) % (hourRT%12, minuteRT, secondRT), 30, 0)
        oled.show()
        print(RealTime)
        
    # State 3 Show Message 
    elif(state==3):
            oled.fill(0)
            oled.text(msg, 30, 0)
            oled.show()
    try:
        
        # try to accept request from socket 
        cl, addr = s.accept()
        print('client connected from', addr)
        msg=cl.recv(4096)
        
        # print message
        print(msg)
        msg=msg.split(b'\r\n\r\n') #converts it into list
        data=msg[-1] #gets unsplited data
        formatteddata=data.decode("utf-8") #byte to string conversion
        msgtype, msg=formatteddata.split("=",1) #split it using "=" sign
        print("Type:",msgtype,"Msg:",msg)
        
        
        # Depending on the message assign which state are we.
        if (msgtype=="OnDisplay"):
            state=1
        elif(msgtype=="OffDisplay"):
            state=0
        elif(state==1 and msgtype=="DisplayTime"):
            state=2
            rtc.datetime((int(msg[4:8]), int(msg[2:4]), int(msg[0:2]), 0, int(msg[8:10]), int(msg[10:12]), 0, 0))
            # print(msg[0:2])
            # print(msg[2:4])
            # print(msg[4:8])
            # print(msg[8:10])
            # print(msg[10:12])
        elif(state==1 and msgtype=="message"):
            state=3
            msg=msg.replace("+"," ")
            
        resp12= "HTTP/1.1 200 OK\r\nContent-Type: application/text\r\nContent-Length: 0\r\n\r\n{''}"
        cl.send(resp12)
        cl.close()
    except:
        # if within 0.5 seconds no request, pass and keep listenning
        pass


