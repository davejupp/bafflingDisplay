# bafflingDisplay
Bafang ebike controller display application for Android phones

Initial commit:
Coded almost entirely by Gemini, This app aims to be able to replace Bafang displays
(Including those with separate buttons) while additionally aiming to eventually support 
Bluetooth BLE. I'd quite like to get some mapping on the actual app but my primary
goal is just a functional fully working display.

I believe that Bluetooth support would allow for:
Handlebar mounted bluetooth controls for gear changes
(i.e. we just pretend to be an audio player and intercept the next/previous buttons)
Phone in bag, gear changes still work. I've never played that much with BLE so I'm
attempting the USB part first as the protocol should be identical.

I'd also ultimately like to add support for the open source bbs-fw so settings can be
changed on the fly.