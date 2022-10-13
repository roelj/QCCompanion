QCCompanion
===========

This Android application acts as a MIDI receiver and an audio player
(over USB) for the NeuralDSP Quad Cortex.  Note that this app is not
a NeuralDSP product, but simply a hobby project by an unaffiliated
third-party.

## How to set up the Quad Cortex

The QCCompanion expects to receive MIDI signals on channel 1.  The following
table shows the MIDI you have to configure under `...` -> `Preset MIDI Out`:

| FOOTSWITCH | TYPE | CHANNEL | CC#    | VALUE   |
|------------|------|---------|--------|---------|
| A          | CC   | 1       | 1      | 1       |
| B          | CC   | 1       | 1      | 2       |
| C          | CC   | 1       | 1      | 3       |
| D          | CC   | 1       | 1      | 4       |
| E          | CC   | 1       | 1      | 5       |
| F          | CC   | 1       | 1      | 6       |
| G          | CC   | 1       | 1      | 7       |
| H          | CC   | 1       | 1      | 8       |

Also make sure that `MIDI over USB` is turned on under
`...` -> `Settings` -> `MIDI Settings`.

## Assigning audio tracks

The Android application has eight buttons (A to H) that mirror the footswitches
on the Quad Cortex. Tap one of the buttons and select an audio track. The buttons
that have an audio file assigned will turn green.

When the Quad Cortex is connected to your phone via USB, a toggle a footswitch on
the Quad Cortex and the corresponding button in the app will turn red, and a timer
appears displaying the seconds the track is playing.
 