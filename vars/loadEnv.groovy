#!/usr/bin/env groovy

//
// Load environment settings from resources as a Map.
//

import groovy.json.JsonOutput

def loadYaml(String resourcePath, String name, defaults = null) {
    def resource = libraryResource "${resourcePath}/${name}.yml"
    return readYaml(text: resource, defaults: defaults)
}

def printMap(String name, map) {
    def json = JsonOutput.toJson(map)
    json = JsonOutput.prettyPrint(json)
    echo "[loadEnv] ${name}\n${json}"
}

def call(String resourcePath, String name, boolean debug = false) {
    def map = loadYaml(resourcePath, name)
    if (debug) {
        printMap(name, map)
    }
    return map
}
