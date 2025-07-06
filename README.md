# bafflingDisplay
Bafang ebike controller display application for Android phones

Uses the Bafang UART protocol and a programming cable for the serial interface.
I'd eventually like to update this to use bluetooth BLE for both display communication
and additional peripherals.

I haven't been able to find a specific Android app for this functionality, I'm not
at all an experienced Android programmer, and I have no idea how Bafang communication
works. That said, it all looks pretty simple with the exception of any reasonable 
start/end message tokens.

Initial commit:
I wanted to see how far I could get using entirely Gemini, 6 months ago I tried the same 
thing and it almost exclusively suggested obsolete or deprecated solutions. Now (July 25)
it's coding competent, reasonable Compose UI designs. It still requires a lot of hand 
holding to actually complete a job, but the work it does is surprisingly excellent.

So V1 is Coded almost entirely by Gemini. I'd quite like to get some mapping on the actual app but my primary
goal is just a functional fully working display for the moment, then move to BLE, then
add modularity. Or just mapping/fitness tracking. It'd also obviously be awesome to include 
ANT/ANT+ comms and fitness app mapping but these are far off goals that will likely 
never happen unless there's community support (I'm just quite lazy!)

I believe that Bluetooth support would allow for:
Handlebar mounted bluetooth controls for gear changes
(i.e. we just pretend to be an audio player and intercept the next/previous buttons)
Phone in bag, gear changes still work. I've never played that much with BLE so I'm
attempting the USB part first as the protocol should be identical. But it does seem
likely that some USB HID device could be made to work even if it wasn't a really cheap 
BT audio controller

I'd also ultimately like to add support for the open source bbs-fw so settings can be
changed on the fly. And detection of original vs bbs-hd firmware for setting the 
appropriate types. This should be pretty trivial as it's just a config screen and a whole
load of config writes. AFAIK.

Any input, suggestions, PRs, etc would obviously be welcome. This project is pre-alpha 
(it doesn't do anything useful) so if you know nothing about Bafang but are familiar with 
android, I'd appreciate feedback on any stupid mistakes I made. 

Have fun riding today!