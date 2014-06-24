/**
 *  Working From Home
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
    name: "Working From Home",
    namespace: "sudarkoff.com",
    author: "George Sudarkoff",
    description: "If after a particular time of day certain people/cars are still present, trigger a \"Working From Home\" mode.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@2x.png"
)

preferences {
    page (name: "selectPhrase")
}

def selectPhrase() {
    def configured = settings.wfhPhrase
    dynamicPage(name: "selectPhrase", title: "Configure", install: true, uninstall: true) {
        section ("Who?") {
            input "people", "capability.presenceSensor", title: "When these people", multiple: true, required: true
        }
        section ("When?") {
            input "timeOfDay", "time", title: "Still at home after", required: true
        }
        section (title: "More Options", hidden: hideOptionsSection(), hideable: true) {
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
                options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }

        def phrases = location.helloHome?.getPhrases()*.label
        if (phrases) {
            phrases.sort()
            section("Action") {
                input "wfhPhrase", "enum", title: "Perform action", required: true, options: phrases 
            }
        }
    }
}

def installed() {
    initialize()
    subscribe(app)
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    schedule(timeOfDay, "checkPresence")
}

def checkPresence() {
    if (daysOk && modeOk) {
        if (people.latestValue("presence") == "present") {
            location.helloHome.execute(settings.wfhPhrase)
        }
    }
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

