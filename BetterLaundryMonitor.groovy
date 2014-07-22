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
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png")


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

  section (title: "More Options", hidden: hideOptionsSection(), hideable: true) {
    input "phone", "phone", title: "Additionally, also send a text message to:", required: false
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
    state.cycleOn = false
    state.cycleEnd = now()
    duration = state.cycleEnd - state.cycleStart
    log.trace "Cycle ended after ${duration} minutes."
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
    (phone) ? false : true
}

