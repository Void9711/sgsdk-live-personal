#!/usr/bin/env groovy

//
// Return environment names that can be used in a choices parameter.
//

def call(filter = []) {
    def choices = [
        'dev',
    ]
    choices -= filter
    return choices.join('\n')
}
