///////////////////////////////////////////////////////////////////////////////
//FILE:          OverlayArduino.java
//PROJECT:       Micro-Manager
//SUBSYSTEM:     OverlayArduino plugin
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ikuo Obataya
//
// COPYRIGHT:    JPK Instruments AG, 2018
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
// Source codes are based on MultiChannelShading plugin by Kurt Thorn, Nico Stuurman
//
package org.micromanager.overlayarduino;

/**
 *
 */
public class OverlayArduino implements org.micromanager.api.MMProcessorPlugin {
   public static final String menuName = "Overlay Arduino Input";
   public static final String tooltipDescription = "Write tags and/or overlay Arduino Digital Input";

   public static String versionNumber = "0.3";

   public static Class<?> getProcessorClass() {
	   return OverlayArduinoProcessor.class;
   }

   @Override
   public String getDescription() {
      return tooltipDescription;
   }

   @Override
   public String getInfo() {
      return tooltipDescription;
   }

   @Override
   public String getVersion() {
      return versionNumber;
   }

   @Override
   public String getCopyright() {
      return "JPK Instruments AG 2018";
   }
   
}
