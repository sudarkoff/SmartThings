/**
 *  Alert on Power Consumption
 *
 *  Copyright 2014 George Sudarkoff
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.time.*

definition(
  name: "Better Laundry Monitor",
  namespace: "com.sudarkoff",
  author: "George Sudarkoff",
  description: "Using a switch with powerMonitor capability, monitor the laundry cycle and alert when it's done.",
  category: "Green Living",
  iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn.png",
  iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn@2x.png")


preferences {
  section ("When this device stops drawing power") {
    input "meter", "capability.powerMeter", multiple: false, required: true
  }

  section ("Send this message") {
    input "message", "text", title: "Notification message", description: "Laudry is done!", required: true
  }

  section (title: "Notification method") {
    input "sendPushMessage", "bool", title: "Send a push notification?"
  }

  section (title: "Additionally", hidden: hideOptionsSection(), hideable: true) {
    input "phone", "phone", title: "Send a text message to:", required: false
    input "switches", "capability.switch", title: "Turn on this switch", required:false, multiple:true
    input "hues", "capability.colorControl", title: "Turn these hue bulbs", required:false, multiple:true
    input "color", "enum", title: "This color", required: false, multiple:false, options: ["White", "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
    input "lightLevel", "enum", title: "This light Level", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"

  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(meter, "power", handler)
}

def handler(evt) {
  def latestPower = meter.currentValue("power")
  log.trace "Power: ${latestPower}W"

  if (!state.cycleOn && latestPower > 8) {
    state.cycleOn = true
    state.cycleStart = now()
    log.trace "Cycle started."
  }
  // If the washer stops drawing power, the cycle is complete, send notification.
  else if (state.cycleOn && latestPower == 0) {
    send(message)
    lightAlert(evt)
    state.cycleOn = false
    state.cycleEnd = now()
    duration = state.cycleEnd - state.cycleStart
    log.trace "Cycle ended after ${duration} minutes."
  }
}

private lightAlert(evt) {
  def hueColor = 0
  def saturation = 100

  if (hues) {
      switch(color) {
          case "White":
              hueColor = 52
              saturation = 19
              break;
          case "Daylight":
              hueColor = 53
              saturation = 91
              break;
          case "Soft White":
              hueColor = 23
              saturation = 56
              break;
          case "Warm White":
              hueColor = 20
              saturation = 80 //83
              break;
          case "Blue":
              hueColor = 70
              break;
          case "Green":
              hueColor = 39
              break;
          case "Yellow":
              hueColor = 25
              break;
          case "Orange":
              hueColor = 10
              break;
          case "Purple":
              hueColor = 75
              break;
          case "Pink":
              hueColor = 83
              break;
          case "Red":
              hueColor = 100
              break;
      }

      state.previous = [:]

      hues.each {
          state.previous[it.id] = [
              "switch": it.currentValue("switch"),
              "level" : it.currentValue("level"),
              "hue": it.currentValue("hue"),
              "saturation": it.currentValue("saturation")
          ]
      }

      log.debug "current values = $state.previous"

      def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
      log.debug "new value = $newValue"

      if (switches) {
          switches*.on()
      }
      hues*.setColor(newValue)
  }
}

private send(msg) {
  if (sendPushMessage) {
    sendPush(msg)
  }

  if (phone) {
    sendSms(phone, msg)
  }

  log.debug msg
}

private hideOptionsSection() {
    (phone || switches || hues || color || lightLevel) ? false : true
}

