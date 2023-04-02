// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.apps.facemeshgpu;

import android.os.Bundle;
import android.util.Log;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.TextView;
import android.widget.ImageView;

/** Main activity of MediaPipe face mesh app. */
public class MainActivity extends com.google.mediapipe.apps.basic.MainActivity {
  private static final String TAG = "MainActivity";

  private static final String INPUT_NUM_FACES_SIDE_PACKET_NAME = "num_faces";
  private static final String OUTPUT_LANDMARKS_STREAM_NAME = "multi_face_landmarks";
  // Max number of faces to detect/process.
  private static final int NUM_FACES = 1;

  private float ry1, ry2, ary1, ary2, aly1, aly2, ratio_r, ratio_l;
  private TextView tv_eye_blink;
  private ImageView imgv;

  private boolean eye_blinked, eye_open;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    AndroidPacketCreator packetCreator = processor.getPacketCreator();
    Map<String, Packet> inputSidePackets = new HashMap<>();
    inputSidePackets.put(INPUT_NUM_FACES_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_FACES));
    processor.setInputSidePackets(inputSidePackets);
    tv_eye_blink = findViewById(R.id.tv_eye_blink);
    imgv = findViewById(R.id.imageView);

    eye_open = true;
    eye_blinked = true;

    // To show verbose logging, run:
    // adb shell setprop log.tag.MainActivity VERBOSE
          
    processor.addPacketCallback(
        OUTPUT_LANDMARKS_STREAM_NAME,
        (packet) -> {
          List<NormalizedLandmarkList> multiFaceLandmarks = PacketGetter.getProtoVector(packet,
              NormalizedLandmarkList.parser());

          ry1 = multiFaceLandmarks.get(0).getLandmarkList().get(5).getY() * 1920f;
          ry2 = multiFaceLandmarks.get(0).getLandmarkList().get(4).getY() * 1920f;

          ary1 = multiFaceLandmarks.get(0).getLandmarkList().get(373).getY() * 1920f;
          ary2 = multiFaceLandmarks.get(0).getLandmarkList().get(386).getY() * 1920f;
          
          aly1 = multiFaceLandmarks.get(0).getLandmarkList().get(163).getY() * 1920f;
          aly2 = multiFaceLandmarks.get(0).getLandmarkList().get(160).getY() * 1920f;

          ratio_r = (ary1 - ary2) / (ry2 - ry1);
          ratio_l = (aly1 - aly2) / (ry2 - ry1);

          if(ratio_r < 0.7 && ratio_l < 0.7){
            if(eye_blinked){
              tv_eye_blink.setText("Eyes is blinked");
              imgv.setImageDrawable(this.getResources().getDrawable(R.drawable.eyes_close));
              eye_blinked = false;
              eye_open = true;
            }
          }
          else{
            if(eye_open){
              tv_eye_blink.setText("Eyes is open");
              imgv.setImageDrawable(this.getResources().getDrawable(R.drawable.eyes_open));
              eye_blinked = true;
              eye_open = false;
            }
          }
        });
  }

  private static String getMultiFaceLandmarksDebugString(
      List<NormalizedLandmarkList> multiFaceLandmarks) {
    if (multiFaceLandmarks.isEmpty()) {
      return "No face landmarks";
    }
    String multiFaceLandmarksStr = "Number of faces detected: " + multiFaceLandmarks.size() + "\n";
    int faceIndex = 0;
    for (NormalizedLandmarkList landmarks : multiFaceLandmarks) {
      multiFaceLandmarksStr += "\t#Face landmarks for face[" + faceIndex + "]: " + landmarks.getLandmarkCount() + "\n";
      int landmarkIndex = 0;
      for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
        multiFaceLandmarksStr += "\t\tLandmark ["
            + landmarkIndex
            + "]: ("
            + landmark.getX()
            + ", "
            + landmark.getY()
            + ", "
            + landmark.getZ()
            + ")\n";
        ++landmarkIndex;
      }
      ++faceIndex;
    }
    return multiFaceLandmarksStr;
  }
}
