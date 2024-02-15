/**
 *  MIT License
 *  Copyright 2022 Jonathan Bradshaw (jb@nrgup.net)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
metadata {
    definition(
        name: 'ESPHome ratGDO',
        namespace: 'esphome',
        author: 'Jonathan Bradshaw, Adrian Caramaliu',
        singleThreaded: true,
        importUrl: 'https://raw.githubusercontent.com/bradsjm/hubitat-drivers/main/ESPHome/ESPHome-ratGDO.groovy') {

        capability 'Actuator'
        capability 'Sensor'
        capability 'Refresh'
        capability 'Initialize'
        capability 'Signal Strength'
        capability 'Door Control'
        capability 'Garage Door Control'
        capability "Contact Sensor"
        capability 'Switch'
        capability 'Lock'
        capability 'MotionSensor'
        capability 'Pushable Button'

        // attribute populated by ESPHome API Library automatically
        attribute 'dryContactLight', 'enum', [ 'open', 'closed' ]
        attribute 'dryContactOpen', 'enum', [ 'open', 'closed' ]
        attribute 'dryContactClose', 'enum', [ 'open', 'closed' ]
        attribute 'learn', 'enum', [ 'on', 'off' ]
        attribute 'motor', 'enum', [ 'idle', 'running' ]
        attribute 'networkStatus', 'enum', [ 'connecting', 'online', 'offline' ]
        attribute 'obstruction', 'enum', [ 'present', 'not present' ]
        attribute 'openings', 'number'
        attribute 'position', 'number'      
        
        command 'learnOn'
        command 'learnOff'
    }

    preferences {
        input name: 'ipAddress',    // required setting for API library
                type: 'text',
                title: 'Device IP Address',
                required: true

        input name: 'password',     // optional setting for API library
                type: 'text',
                title: 'Device Password <i>(if required)</i>',
                required: false

        input name: 'cover',        // allows the user to select which cover entity to use
            type: 'enum',
            title: 'ESPHome Door Entity',
            required: state.covers?.size() > 0,
            options: state.covers?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'light', // allows the user to select which light entity to use
            type: 'enum',
            title: 'ESPHome Light Entity',
            required: state.lights?.size() > 0,
            options: state.lights?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'lock', // allows the user to select which light entity to use
            type: 'enum',
            title: 'ESPHome Lock Entity',
            required: state.locks?.size() > 0,
            options: state.locks?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'motionsensor', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Motion Sensor Entity',
            required: state.sensors?.size() > 0,
            options: state.sensors?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'obstructionsensor', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Obstruction Sensor Entity',
            required: state.sensors?.size() > 0,
            options: state.sensors?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'button', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Button',
            required: state.sensors?.size() > 0,
            options: state.sensors?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'motor', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Motor Entity',
            required: state.sensors?.size() > 0,
            options: state.sensors?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'learn', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Learn Entity',
            required: state.switches?.size() > 0,
            options: state.switches?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'drycontactlight', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Dry Contact Light Entity',
            required: state.sensors?.size() > 0,
            options: state.sensors?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'drycontactopen', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Dry Contact Open Entity',
            required: state.sensors?.size() > 0,
            options: state.sensors?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'drycontactclose', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome DryContactClose Entity',
            required: state.sensors?.size() > 0,
            options: state.sensors?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'openings', // allows the user to select which sensor entity to use
            type: 'enum',
            title: 'ESPHome Openings Entity',
            required: state.others?.size() > 0,
            options: state.others?.collectEntries { k, v -> [ k, v.name ] }

        input name: 'logEnable',    // if enabled the library will log debug details
                type: 'bool',
                title: 'Enable Debug Logging',
                required: false,
                defaultValue: false

        input name: 'logTextEnable',
              type: 'bool',
              title: 'Enable descriptionText logging',
              required: false,
              defaultValue: true
    }
}

public void initialize() {
    // API library command to open socket to device, it will automatically reconnect if needed
    openSocket()

    if (logEnable) {
        runIn(1800, 'logsOff')
    }
}

public void installed() {
    log.info "${device} driver installed"
}

public void logsOff() {
    espHomeSubscribeLogs(LOG_LEVEL_INFO, false) // disable device logging
    device.updateSetting('logEnable', false)
    log.info "${device} debug logging disabled"
}

public void updated() {
    log.info "${device} driver configuration updated"
    initialize()
}

public void uninstalled() {
    closeSocket('driver uninstalled') // make sure the socket is closed when uninstalling
    log.info "${device} driver uninstalled"
}

// driver commands
public void open() {
    String doorState = device.currentValue('door')
    // API library cover command, entity key for the cover is required
    if (logTextEnable) { log.info "${device} open" }
    espHomeCoverCommand(key: settings.cover as Long, position: 1.0)
}

public void close() {
    String doorState = device.currentValue('door')
    // API library cover command, entity key for the cover is required
    if (logTextEnable) { log.info "${device} close" }
    espHomeCoverCommand(key: settings.cover as Long, position: 0.0)
}

public void on() {
    if (logTextEnable) { log.info "${device} on" }
    espHomeLightCommand(key: settings.light as Long, state: true)
}

public void off() {
    if (logTextEnable) { log.info "${device} off" }
    espHomeLightCommand(key: settings.light as Long, state: false)
}

public void lock() {
    if (logTextEnable) { log.info "${device} locked" }
    espHomeLockCommand(key: settings.lock as Long, lockCommand: LOCK_LOCK)
}

public void unlock() {
    if (logTextEnable) { log.info "${device} unlocked" }
    espHomeLockCommand(key: settings.lock as Long, lockCommand: LOCK_UNLOCK)
}

public void learnOn() {
    if (logTextEnable) { log.info "${device} on" }
    espHomeSwitchCommand(key: settings.learn as Long, state: true)
}

public void learnOff() {
    if (logTextEnable) { log.info "${device} off" }
    espHomeSwitchCommand(key: settings.learn as Long, state: false)
}


public void refresh() {
    log.info "${device} refresh"
    state.clear()
    state.requireRefresh = true
    espHomeDeviceInfoRequest()
}

// the parse method is invoked by the API library when messages are received
public void parse(Map message) {
    if (logEnable) { log.debug "ESPHome received: ${message}" }

    switch (message.type) {
        case 'device':
            // Device information
            break

        case 'entity':
            // This will populate the UI dropdowns with all the entities
            // discovered and the entity key which is required when sending commands
            if (message.platform == 'cover') {
                state.covers = (state.covers ?: [:]) + [ (message.key as String): message ]
                if (!settings.cover && (message.name == "Door")) {
                    device.updateSetting('cover', message.key as String)
                }
                return
            }

            if (message.platform == 'binary') {
                state.sensors = (state.sensors ?: [:]) + [ (message.key as String): message ]
                if (!settings.motionsensor && (message.name == "Motion")) {
                    device.updateSetting('motionsensor', message.key as String)
                }
                if (!settings.obstructionsensor && (message.name == "Obstruction")) {
                    device.updateSetting('obstructionsensor', message.key as String)
                }
                if (!settings.button && (message.name == "Button")) {
                    device.updateSetting('button', message.key as String)
                }
                if (!settings.motor && (message.name == "Motor")) {
                    device.updateSetting('motor', message.key as String)
                }
                if (!settings.drycontactlight && (message.name == "Dry contact light")) {
                    device.updateSetting('drycontactlight', message.key as String)
                }
                if (!settings.drycontactopen && (message.name == "Dry contact open")) {
                    device.updateSetting('drycontactopen', message.key as String)
                }
                if (!settings.drycontactclose && (message.name == "Dry contact close")) {
                    device.updateSetting('drycontactclose', message.key as String)
                }
                return
            }

            if (message.platform == 'light') {
                state.lights = (state.lights ?: [:]) + [ (message.key as String): message ]
                if (!settings.light && (message.name == "Light")) {
                    device.updateSetting('light', message.key as String)
                }
                return
            }

            if (message.platform == 'switch') {
                state.switches = (state.switches ?: [:]) + [ (message.key as String): message ]
                if (!settings.learn && (message.name == "Learn")) {
                    device.updateSetting('learn', message.key as String)
                }
                return
            }

            if (message.platform == 'lock') {
                state.locks = (state.locks ?: [:]) + [ (message.key as String): message ]
                if (!settings.lock && (message.name == "Lock remotes")) {
                    device.updateSetting('lock', message.key as String)
                }
                return
            }

            if (message.platform == 'sensor') {
                state.others = (state.others ?: [:]) + [ (message.key as String): message ]
                if (!settings.openings && (message.name == "Openings")) {
                    device.updateSetting('openings', message.key as String)
                }
                if (message.deviceClass == 'signal_strength') {
                    state['signalStrength'] = message.key
                }
                return
            }
            break

        case 'state':
            String type = message.isDigital ? 'digital' : 'physical'
            // Check if the entity key matches the message entity key received to update device state
            if (settings.cover as Long == message.key) {
                String value
                String contact
                switch (message.currentOperation) {
                    case COVER_OPERATION_IDLE:
                        value = message.position > 0 ? 'open' : 'closed'
                        contact = value
                        break
                    case COVER_OPERATION_IS_OPENING:
                        value = 'opening'
                        break
                    case COVER_OPERATION_IS_CLOSING:
                        value = 'closing'
                        break
                }
                if (device.currentValue('door') != value) {
                    descriptionText = "${device} door is ${value}"
                    sendEvent(name: 'door', value: value, type: type, descriptionText: descriptionText)
                    if (logTextEnable) { log.info descriptionText }
                }
                if ((contact != null) && (device.currentValue('contact') != contact)) {
                    descriptionText = "${device} contact is ${value}"
                    sendEvent(name: 'contact', value: contact, type: type, descriptionText: descriptionText)
                    if (logTextEnable) { log.info descriptionText }
                }
                if (message.position != null) {
                position = Math.round(message.position * 100)
                    if (device.currentValue('position') != position) {
                        descriptionText = "${device} position is ${position}"
                        sendEvent(name: 'position', value: position, type: type, descriptionText: descriptionText)
                        if (logTextEnable) { log.info descriptionText }
                    }
                }
                return
            }
        
            if (settings.motionsensor as Long == message.key) {
                String value = message.state ? 'active' : 'inactive'
                if (device.currentValue('motion') != value) {
                    sendEvent([
                        name: 'motion',
                        value: value,
                        descriptionText: "Motion is ${value}"
                    ])
                }
                return
            }

            if (settings.obstructionsensor as Long == message.key) {
                String value = message.state ? 'present' : 'not present'
                if (device.currentValue('obstruction') != value) {
                    sendEvent([
                        name: 'obstruction',
                        value: value,
                        descriptionText: "Obstruction is ${value}"
                    ])
                }
                return
            }

            if (settings.motor as Long == message.key) {
                String value = message.state ? 'running' : 'idle'
                if (device.currentValue('motor') != value) {
                    sendEvent([
                        name: 'motor',
                        value: value,
                        descriptionText: "Motor is ${value}"
                    ])
                }
                return
            }

            if (settings.learn as Long == message.key) {
                String value = message.state ? 'on' : 'off'
                if (device.currentValue('learning') != value) {
                    sendEvent([
                        name: 'learn',
                        value: value,
                        descriptionText: "Learn is ${value}"
                    ])
                }
                return
            }

            if (settings.light as Long == message.key) {
                String value = message.state ? 'on' : 'off'
                if (device.currentValue('switch') != value) {
                    sendEvent(name: 'switch', value: value, type: type, descriptionText: "Light is ${value} (${type})")
                }
                return
            }

            if (settings.lock as Long == message.key) {
                String value = message.state == 1 ? 'locked' : 'unlocked'
                if (device.currentValue('lock') != value) {
                    sendEvent(name: 'lock', value: value, type: type, descriptionText: "Lock is ${value} (${type})")
                }
                return
            }

            if (settings.openings as Long == message.key) {
                int value = message.state as int
                if (device.currentValue('openings') != value) {
                    sendEvent(name: 'openings', value: value, type: type, descriptionText: "Lock is ${value} (${type})")
                }
                return
            }

            if (settings.drycontactlight as Long == message.key) {
                String value = message.state == 1 ? 'closed' : 'open'
                if (device.currentValue('dryContactLight') != value) {
                    sendEvent(name: 'dryContactLight', value: value, type: type, descriptionText: "Dry Contact Light is ${value} (${type})")
                }
                return
            }

            if (settings.drycontactopen as Long == message.key) {
                String value = message.state == 1 ? 'closed' : 'open'
                if (device.currentValue('dryContactOpen') != value) {
                    sendEvent(name: 'dryContactOpen', value: value, type: type, descriptionText: "Dry Contact Open is ${value} (${type})")
                }
                return
            }

            if (settings.drycontactclose as Long == message.key) {
                String value = message.state == 1 ? 'closed' : 'open'
                if (device.currentValue('dryContactClose') != value) {
                    sendEvent(name: 'dryContactClose', value: value, type: type, descriptionText: "Dry Contact Close is ${value} (${type})")
                }
                return
            }

            if (settings.button as Long == message.key && message.hasState) {
                if (message.state) {
                    descriptionText = "Button 1 pushed (${type})"
                    sendEvent(name: "pushed", value: 1, type: type, descriptionText: descriptionText, isStateChange: true)
                    if (logTextEnable) { log.info descriptionText }
                }
                return
            }
        
            if (state.signalStrength as Long == message.key && message.hasState) {
                Integer rssi = Math.round(message.state as Float)
                String unit = 'dBm'
                if (device.currentValue('rssi') != rssi) {
                    descriptionText = "${device} rssi is ${rssi}"
                    sendEvent(name: 'rssi', value: rssi, unit: unit, type: type, descriptionText: descriptionText)
                    if (logTextEnable) { log.info descriptionText }
                }
                return
            }
            break
    }
}

// Put this line at the end of the driver to include the ESPHome API library helper
#include esphome.espHomeApiHelper