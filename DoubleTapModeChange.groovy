/**
 *  Double Tap Mode Switch
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

definition(
    name: "Double Tap Mode Switch",
    namespace: "sudarkoff.com",
    author: "George Sudarkoff",
    description: "Execute a 'Hello, Home' phrase when an existing switch is tapped twice in a row.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@2x.png"
)

preferences {
    page (name: "configApp")
}

def configApp() {
    dynamicPage(name: "configApp", install: true, uninstall: true) {
        section ("When this switch is double-tapped...") {
            input "master", "capability.switch", required: true
        }

        def phrases = location.helloHome?.getPhrases()*.label
        if (phrases) {
            phrases.sort()
            section("Perform this actions...") {
                input "onPhrase", "enum", title: "ON action", required: false, options: phrases
                input "offPhrase", "enum", title: "OFF action", required: false, options: phrases
            }
        }

        section (title: "More Options", hidden: hideOptionsSection(), hideable: true) {
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
                options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values:["Yes","No"]], required: false
            input "phone", "phone", title: "Send a Text Message?", required: false
        }
    }
}

def installed()
{
    subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def updated()
{
    unsubscribe()
    subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {
    log.info evt.value

    if (daysOk && modeOk) {
        // use Event rather than DeviceState because we may be changing DeviceState to only store changed values
        def recentStates = master.eventsSince(new Date(now() - 4000), [all:true, max: 10]).findAll{it.name == "switch"}
        log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"

        if (evt.isPhysical()) {
            if (evt.value == "on" && lastTwoStatesWere("on", recentStates, evt)) {
                log.debug "detected two taps, execute ON phrase"
                location.helloHome.execute(settings.onPhrase)
                def message = "${location.name} executed ${settings.onPhrase}"
                send(message)
            } else if (evt.value == "off" && lastTwoStatesWere("off", recentStates, evt)) {
                log.debug "detected two taps, execute OFF phrase"
                location.helloHome.execute(settings.offPhrase)
                def message = "${location.name} executed ${settings.offPhrase}"
                send(message)
            }
        }
        else {
            log.trace "Skipping digital on/off event"
        }
    }
}

private lastTwoStatesWere(value, states, evt) {
    def result = false
    if (states) {
        log.trace "unfiltered: [${states.collect{it.dateCreated + ':' + it.value}.join(', ')}]"
        def onOff = states.findAll { it.isPhysical() || !it.type }
        log.trace "filtered:   [${onOff.collect{it.dateCreated + ':' + it.value}.join(', ')}]"

        // This test was needed before the change to use Event rather than DeviceState. It should never pass now.
        if (onOff[0].date.before(evt.date)) {
            log.warn "Last state does not reflect current event, evt.date: ${evt.dateCreated}, state.date: ${onOff[0].dateCreated}"
            result = evt.value == value && onOff[0].value == value
        }
        else {
            result = onOff.size() > 1 && onOff[0].value == value && onOff[1].value == value
        }
    }
    result
}

private send(msg) {
    if (sendPushMessage != "No") {
        sendPush(msg)
    }

    if (phone) {
        sendSms(phone, msg)
    }

    log.debug msg
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    result
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    result
}

private hideOptionsSection() {
    (days || modes) ? false : true
}

