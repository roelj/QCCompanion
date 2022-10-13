/*
 * Copyright (C) 2022  Roel Janssen <roel@roelj.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.roelj.qccompanion;

import android.media.midi.MidiReceiver;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QuadCortexMidiReceiver extends MidiReceiver {

    private List<MidiCommandProcessor> callback_list;

    public QuadCortexMidiReceiver() {
        callback_list = new ArrayList<>();
    }

    @Override
    public void onSend(byte[] bytes, int offset, int count, long timestamp) throws IOException {
        Log.i("QuadCortexMidiReceiver", String.format("Received %d bytes.", bytes.length));
        Log.i("QuadCortexMidiReceiver", String.format("Offset %d, count %d.", offset, count));

        int channel_index = bytes[offset + 1];
        int value = bytes[offset + 2];
        for (MidiCommandProcessor callback : callback_list) callback.processMidiCommand(channel_index, value);
    }

    public void registerCallback(MidiCommandProcessor object) {
        callback_list.add(object);
    }

}
